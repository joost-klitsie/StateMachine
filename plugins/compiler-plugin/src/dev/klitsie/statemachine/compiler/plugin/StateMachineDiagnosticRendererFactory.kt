package dev.klitsie.statemachine.compiler.plugin

import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.rendering.CommonRenderers

object StateMachineDiagnosticRendererFactory : BaseDiagnosticRendererFactory() {
	override val MAP by KtDiagnosticFactoryToRendererMap("StateMachinePlugin") {
		it.put(
			FirPluginDiagnostics.MissingLeafStatesError,
			"Missing states in {1}: {0}",
			CommonRenderers.STRING,
			CommonRenderers.STRING,
		)
		it.put(
			FirPluginDiagnostics.MissingLeafStatesWarning,
			"Missing states in {1}: {0}",
			CommonRenderers.STRING,
			CommonRenderers.STRING,
		)
		it.put(
			FirPluginDiagnostics.DuplicateStateDeclarationError,
			"Duplicate state declaration: {0}",
			CommonRenderers.STRING,
		)
		it.put(
			FirPluginDiagnostics.DuplicateStateDeclarationWarning,
			"Duplicate state declaration: {0}",
			CommonRenderers.STRING,
		)
		it.put(
			FirPluginDiagnostics.InvalidNestingError,
			"Nesting error: {0} must be inside {1}",
			CommonRenderers.STRING,
			CommonRenderers.STRING,
		)
		it.put(
			FirPluginDiagnostics.InvalidNestingWarning,
			"Nesting error: {0} must be inside {1}",
			CommonRenderers.STRING,
			CommonRenderers.STRING,
		)
		it.put(
			FirPluginDiagnostics.IncompleteNestedStateError,
			"Nested state {0} is incomplete",
			CommonRenderers.STRING,
		)
		it.put(
			FirPluginDiagnostics.IncompleteNestedStateWarning,
			"Nested state {0} is incomplete",
			CommonRenderers.STRING,
		)
		it.put(
			FirPluginDiagnostics.UnusedEventDefinitionError,
			"Event definition {0} is never handled by onEvent.",
			CommonRenderers.STRING,
		)
		it.put(
			FirPluginDiagnostics.UnusedEventDefinitionWarning,
			"Event definition {0} is never handled by onEvent.",
			CommonRenderers.STRING,
		)
	}
}
