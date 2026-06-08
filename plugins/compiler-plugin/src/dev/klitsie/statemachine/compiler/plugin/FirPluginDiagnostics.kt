package dev.klitsie.statemachine.compiler.plugin

import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory

object FirPluginDiagnostics : KtDiagnosticsContainer() {

	val MissingLeafStatesError by error2<PsiElement, String, String>(
		SourceElementPositioningStrategies.DEFAULT,
	)
	val MissingLeafStatesWarning by warning2<PsiElement, String, String>(
		SourceElementPositioningStrategies.DEFAULT,
	)

	val DuplicateStateDeclarationError by error1<PsiElement, String>(
		SourceElementPositioningStrategies.DEFAULT,
	)
	val DuplicateStateDeclarationWarning by warning1<PsiElement, String>(
		SourceElementPositioningStrategies.DEFAULT,
	)

	val UnusedEventDefinitionError by error1<PsiElement, String>(
		SourceElementPositioningStrategies.DEFAULT,
	)
	val UnusedEventDefinitionWarning by warning1<PsiElement, String>(
		SourceElementPositioningStrategies.DEFAULT,
	)

	val InvalidNestingError by error2<PsiElement, String, String>(
		SourceElementPositioningStrategies.DEFAULT,
	)
	val InvalidNestingWarning by warning2<PsiElement, String, String>(
		SourceElementPositioningStrategies.DEFAULT,
	)

	val IncompleteNestedStateError by error1<PsiElement, String>(
		SourceElementPositioningStrategies.DEFAULT,
	)
	val IncompleteNestedStateWarning by warning1<PsiElement, String>(
		SourceElementPositioningStrategies.DEFAULT,
	)

	override fun getRendererFactory(): BaseDiagnosticRendererFactory =
		StateMachineDiagnosticRendererFactory

}
