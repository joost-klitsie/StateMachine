package dev.klitsie.statemachine.compiler.plugin

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirFunctionCallChecker
import org.jetbrains.kotlin.fir.declarations.getSealedClassInheritors
import org.jetbrains.kotlin.fir.declarations.utils.isSealed
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.toResolvedFunctionSymbol
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.toConeTypeProjection
import org.jetbrains.kotlin.fir.types.type
import org.jetbrains.kotlin.name.ClassId

data class DslNode(
	val type: ConeKotlinType,
	val isNested: Boolean,
	val source: KtSourceElement?,
	val children: List<DslNode> = emptyList(),
) {
	fun collectAllClassIds(): List<ClassId> =
		listOfNotNull(type.classId) + children.flatMap { it.collectAllClassIds() }
}

data class StateHierarchyNode(
	val type: ConeKotlinType,
	val symbol: FirRegularClassSymbol?,
	val isSealed: Boolean,
	val children: List<StateHierarchyNode>,
) {
	fun getAllLeaves(): List<ConeKotlinType> =
		if (!isSealed) listOf(type) else children.flatMap { it.getAllLeaves() }
}

class StateMachineDslChecker(
	private val configuration: StateMachineConfiguration,
) : FirFunctionCallChecker(mppKind = MppCheckerKind.Common) {

	companion object {
		// Blacklist of behavior-related DSL methods that must never be deep-checked.
		// This stops the compiler from recursively evaluating external functions inside side effects or event handlers.
		private val NON_STATE_DSL_METHODS = setOf("onEvent", "sideEffect", "stateMachine")
	}

	context(context: CheckerContext, reporter: DiagnosticReporter)
	override fun check(expression: FirFunctionCall) {
		val fqName =
			expression.calleeReference.toResolvedFunctionSymbol()?.callableId?.asSingleFqName()
				?.asString() ?: return
		if (fqName != "dev.klitsie.statemachine.stateMachine" && fqName != "stateMachine") return

		val rootStateType =
			expression.typeArguments.getOrNull(0)?.toConeTypeProjection()?.type ?: return
		val eventType =
			expression.typeArguments.getOrNull(2)?.toConeTypeProjection()?.type ?: return

		val anonymousFunction =
			expression.arguments.firstNotNullOfOrNull { it as? FirAnonymousFunctionExpression }
				?: return
		val block = anonymousFunction.anonymousFunction.body ?: return

		val declaredNodes = parseDslBlock(block)
		val hierarchy = buildStateHierarchy(rootStateType, context.session) ?: return

		val allDeclaredClassIds = declaredNodes.flatMap { it.collectAllClassIds() }.toSet()

		validateDuplicates(declaredNodes, configuration)
		validateCompletenessRecursive(
			declaredNodes,
			allDeclaredClassIds,
			hierarchy,
			hierarchy,
			expression.source,
			configuration,
		)
		validateUnusedEvents(expression, block, eventType, configuration)
		validateNestingStructure(declaredNodes, hierarchy, hierarchy, configuration)
	}

	@OptIn(SymbolInternals::class)
	private fun buildStateHierarchy(
		type: ConeKotlinType,
		session: FirSession,
	): StateHierarchyNode? {
		val symbol = type.toRegularClassSymbol(session) ?: return null
		val children = if (symbol.isSealed) {
			symbol.fir.getSealedClassInheritors(session).mapNotNull {
				val sub =
					session.symbolProvider.getClassLikeSymbolByClassId(it) as? FirRegularClassSymbol
				buildStateHierarchy(sub?.defaultType() ?: return@mapNotNull null, session)
			}
		} else emptyList()
		return StateHierarchyNode(type, symbol, symbol.isSealed, children)
	}

	@OptIn(SymbolInternals::class)
	private fun parseDslBlock(
		block: FirBlock,
		visited: MutableSet<FirNamedFunctionSymbol> = mutableSetOf(),
	): List<DslNode> {
		val nodes = mutableListOf<DslNode>()
		for (stmt in block.statements) {
			// Account for implicit returns of the function call
			val call = (stmt as? FirFunctionCall)
				?: ((stmt as? FirReturnExpression)?.result as? FirFunctionCall)
				?: continue

			val symbol = call.calleeReference.toResolvedFunctionSymbol() as? FirNamedFunctionSymbol
				?: continue
			val name = symbol.callableId.callableName.asString()

			if (name == "state") {
				val type =
					call.typeArguments.firstOrNull()?.toConeTypeProjection()?.type ?: continue
				nodes.add(DslNode(type, false, call.source))
			} else if (name == "nestedState") {
				val type =
					call.typeArguments.firstOrNull()?.toConeTypeProjection()?.type ?: continue
				val subBlockNodes = mutableListOf<DslNode>()

				call.arguments.forEach { arg ->
					if (arg is FirAnonymousFunctionExpression) {
						arg.anonymousFunction.body?.let {
							subBlockNodes.addAll(
								parseDslBlock(
									it,
									visited,
								),
							)
						}
					}
				}
				nodes.add(DslNode(type, true, call.source, subBlockNodes))
			} else if (name !in NON_STATE_DSL_METHODS) {
				// 1. Follow any lambda blocks passed to this function (e.g., apply { }, let { })
				call.arguments.forEach { arg ->
					if (arg is FirAnonymousFunctionExpression) {
						arg.anonymousFunction.body?.let { nodes.addAll(parseDslBlock(it, visited)) }
					}
				}

				// 2. Follow the actual body of the custom extension function (e.g. `whatever()`)
				// Using `visited` prevents infinite loops if the user wrote recursive functions.
				if (visited.add(symbol)) {
					symbol.fir.body?.let { funcBody ->
						nodes.addAll(parseDslBlock(funcBody, visited))
					}
				}
			}
		}
		return nodes
	}

	context(context: CheckerContext, reporter: DiagnosticReporter)
	private fun validateDuplicates(nodes: List<DslNode>, configuration: StateMachineConfiguration) {
		val seen = mutableSetOf<ClassId>()
		fun check(n: List<DslNode>) {
			n.forEach { node ->
				node.type.classId?.let {
					if (!seen.add(it)) reporter.reportOn(
						node.source,
						configuration.duplicateStateDiagnostics(),
						it.shortClassName.asString(),
						context,
					)
				}
				check(node.children)
			}
		}
		check(nodes)
	}

	context(context: CheckerContext, reporter: DiagnosticReporter)
	private fun validateCompletenessRecursive(
		nodes: List<DslNode>,
		allDeclaredClassIds: Set<ClassId>,
		hierarchyRoot: StateHierarchyNode,
		currentHierarchy: StateHierarchyNode,
		rootSource: KtSourceElement?,
		configuration: StateMachineConfiguration,
	) {
		val declaredTypes = nodes.map { it.type.classId }.toSet()

		// A state is missing if it's a valid child in the hierarchy,
		// it was not declared in THIS block, AND it was not declared ANYWHERE else.
		// (If it was declared elsewhere, it's an INVALID_NESTING error, not a missing state)
		val missing = currentHierarchy.children.filter {
			it.type.classId !in declaredTypes && it.type.classId !in allDeclaredClassIds
		}

		if (missing.isNotEmpty()) {
			val scopeName =
				currentHierarchy.type.classId?.shortClassName?.asString() ?: "StateMachine"
			val errorSource =
				rootSource // Explicitly use the root block source here instead of nodes.firstOrNull()
			reporter.reportOn(
				errorSource,
				configuration.missingLeafDiagnostics(),
				missing.joinToString { it.type.classId?.shortClassName?.asString() ?: "?" },
				scopeName,
				context,
			)
		}

		nodes.forEach { node ->
			// We search from hierarchyRoot so we can still recurse into misplaced states
			val nodeHierarchy = findNodeInHierarchy(hierarchyRoot, node.type)
			if (node.isNested && nodeHierarchy != null) {
				if (node.children.isEmpty()) {
					reporter.reportOn(
						node.source,
						configuration.incompleteNestedDiagnostics(),
						node.type.classId?.shortClassName?.asString() ?: "?",
						context,
					)
				}

				validateCompletenessRecursive(
					node.children,
					allDeclaredClassIds,
					hierarchyRoot,
					nodeHierarchy,
					node.source ?: rootSource,
					configuration,
				)
			}
		}
	}

	context(context: CheckerContext, reporter: DiagnosticReporter)
	private fun validateNestingStructure(
		nodes: List<DslNode>,
		hierarchyRoot: StateHierarchyNode,
		expectedParent: StateHierarchyNode,
		configuration: StateMachineConfiguration,
	) {
		for (node in nodes) {
			// 1. Is this node a direct child of the expected parent hierarchy block?
			val isDirectChild = expectedParent.children.any { it.type.isSameType(node.type) }

			if (!isDirectChild) {
				val correctParent = findParentInHierarchy(hierarchyRoot, node.type)
				val correctParentName = correctParent?.type?.classId?.shortClassName?.asString()
					?: expectedParent.type.classId?.shortClassName?.asString() ?: "?"

				reporter.reportOn(
					node.source,
					configuration.invalidNestingDiagnostics(),
					node.type.classId?.shortClassName?.asString() ?: "?",
					correctParentName,
					context,
				)
			}

			val nodeHierarchy = findNodeInHierarchy(hierarchyRoot, node.type) ?: continue
			validateNestingStructure(node.children, hierarchyRoot, nodeHierarchy, configuration)
		}
	}

	private fun findNodeInHierarchy(
		current: StateHierarchyNode,
		type: ConeKotlinType,
	): StateHierarchyNode? {
		if (current.type.isSameType(type)) return current
		return current.children.firstNotNullOfOrNull { findNodeInHierarchy(it, type) }
	}

	private fun findParentInHierarchy(
		current: StateHierarchyNode,
		target: ConeKotlinType,
	): StateHierarchyNode? {
		if (current.children.any { it.type.isSameType(target) }) return current
		return current.children.firstNotNullOfOrNull { findParentInHierarchy(it, target) }
	}

	context(context: CheckerContext, reporter: DiagnosticReporter)
	@OptIn(SymbolInternals::class)
	private fun validateUnusedEvents(
		root: FirFunctionCall,
		block: FirBlock,
		eventType: ConeKotlinType,
		configuration: StateMachineConfiguration,
	) {
		val eventSymbol = eventType.toRegularClassSymbol(context.session) ?: return
		val allEvents =
			if (eventSymbol.isSealed) eventSymbol.fir.getSealedClassInheritors(context.session)
				.mapNotNull { context.session.symbolProvider.getClassLikeSymbolByClassId(it)?.classId }
				.toSet() else emptySet()
		val used = mutableSetOf<ClassId>()

		fun scan(b: FirBlock, visited: MutableSet<FirNamedFunctionSymbol>) {
			for (stmt in b.statements) {
				val call = (stmt as? FirFunctionCall)
					?: ((stmt as? FirReturnExpression)?.result as? FirFunctionCall)
					?: continue

				val symbol =
					call.calleeReference.toResolvedFunctionSymbol() as? FirNamedFunctionSymbol
						?: continue
				val name = symbol.callableId.callableName.asString()

				if (name == "onEvent") {
					call.typeArguments.firstOrNull()
						?.toConeTypeProjection()?.type?.classId?.let { used.add(it) }
				} else if (name !in NON_STATE_DSL_METHODS) {
					// Scan inner lambdas
					call.arguments.forEach { arg ->
						if (arg is FirAnonymousFunctionExpression) {
							arg.anonymousFunction.body?.let { scan(it, visited) }
						}
					}
					// Scan custom extension function body
					if (visited.add(symbol)) {
						symbol.fir.body?.let { scan(it, visited) }
					}
				}
			}
		}
		scan(block, mutableSetOf())

		(allEvents - used).forEach {
			reporter.reportOn(
				root.source,
				configuration.unusedEventDiagnostics(),
				it.shortClassName.asString(),
				context,
			)
		}
	}

	private fun ConeKotlinType.toRegularClassSymbol(session: FirSession) =
		classId?.let { session.symbolProvider.getClassLikeSymbolByClassId(it) as? FirRegularClassSymbol }

	private fun ConeKotlinType.isSameType(other: ConeKotlinType) = classId == other.classId

}
