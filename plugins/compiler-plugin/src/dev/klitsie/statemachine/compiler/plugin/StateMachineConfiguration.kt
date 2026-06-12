package dev.klitsie.statemachine.compiler.plugin

import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey

data class StateMachineConfiguration(
	val errorOnMissingLeaf: Boolean,
	val errorOnDuplicateState: Boolean,
	val errorOnInvalidNesting: Boolean,
	val errorOnIncompleteNestedState: Boolean,
	val errorOnUnusedEvent: Boolean,
)

fun CompilerConfiguration.toConfiguration() = StateMachineConfiguration(
	errorOnMissingLeaf = this[StateMachineConfigurationKeys.ErrorOnMissingLeaf, false],
	errorOnDuplicateState = this[StateMachineConfigurationKeys.ErrorOnDuplicateState, true],
	errorOnInvalidNesting = this[StateMachineConfigurationKeys.ErrorOnInvalidNesting, true],
	errorOnIncompleteNestedState = this[StateMachineConfigurationKeys.ErrorOnIncompleteNestedState, true],
	errorOnUnusedEvent = this[StateMachineConfigurationKeys.ErrorOnUnusedEvent, false],
)

fun StateMachineConfiguration.missingLeafDiagnostics() =
	if (errorOnMissingLeaf) FirPluginDiagnostics.MissingLeafStatesError else FirPluginDiagnostics.MissingLeafStatesWarning

fun StateMachineConfiguration.duplicateStateDiagnostics() =
	if (errorOnDuplicateState) FirPluginDiagnostics.DuplicateStateDeclarationError else FirPluginDiagnostics.DuplicateStateDeclarationWarning

fun StateMachineConfiguration.invalidNestingDiagnostics() =
	if (errorOnInvalidNesting) FirPluginDiagnostics.InvalidNestingError else FirPluginDiagnostics.InvalidNestingWarning

fun StateMachineConfiguration.incompleteNestedDiagnostics() =
	if (errorOnIncompleteNestedState) FirPluginDiagnostics.IncompleteNestedStateError else FirPluginDiagnostics.IncompleteNestedStateWarning

fun StateMachineConfiguration.unusedEventDiagnostics() =
	if (errorOnUnusedEvent) FirPluginDiagnostics.UnusedEventDefinitionError else FirPluginDiagnostics.UnusedEventDefinitionWarning


object StateMachineConfigurationKeys {
	val Enabled = CompilerConfigurationKey<Boolean>("enabled")
	val ErrorOnMissingLeaf = CompilerConfigurationKey<Boolean>("errorOnMissingLeaf")
	val ErrorOnDuplicateState = CompilerConfigurationKey<Boolean>("errorOnDuplicateState")
	val ErrorOnInvalidNesting = CompilerConfigurationKey<Boolean>("errorOnInvalidNesting")
	val ErrorOnIncompleteNestedState =
		CompilerConfigurationKey<Boolean>("errorOnIncompleteNestedState")
	val ErrorOnUnusedEvent = CompilerConfigurationKey<Boolean>("errorOnUnusedEvent")
}
