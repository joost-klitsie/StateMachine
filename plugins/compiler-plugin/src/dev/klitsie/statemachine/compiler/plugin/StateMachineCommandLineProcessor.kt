package dev.klitsie.statemachine.compiler.plugin

import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration

@OptIn(ExperimentalCompilerApi::class)
class StateMachineCommandLineProcessor : CommandLineProcessor {
	override val pluginId: String = "dev.klitsie.statemachine.compiler.plugin"

	override val pluginOptions: Collection<AbstractCliOption> = listOf(
		CliOption("enabled", "true/false", "Turn the plugin on or off", false),
		CliOption("errorOnMissingLeaf", "true/false", "Error on missing leaf, otherwise give a warning", false),
		CliOption("errorOnDuplicateState", "true/false", "Error on duplicate, otherwise give a warning", false),
		CliOption("errorOnInvalidNesting", "true/false", "Error on invalid nesting, otherwise give a warning", false),
		CliOption("errorOnIncompleteNestedState", "true/false", "Error on incomplete, otherwise give a warning", false),
		CliOption("errorOnUnusedEvent", "true/false", "Error on unused event, otherwise give a warning", false),
	)

	override fun processOption(
		option: AbstractCliOption,
		value: String,
		configuration: CompilerConfiguration,
	) {
		when (option.optionName) {
			"enabled" -> configuration.put(StateMachineConfigurationKeys.Enabled, value.toBoolean())
			"errorOnMissingLeaf" -> configuration.put(
				StateMachineConfigurationKeys.ErrorOnMissingLeaf,
				value.toBoolean(),
			)

			"errorOnDuplicateState" -> configuration.put(
				StateMachineConfigurationKeys.ErrorOnDuplicateState,
				value.toBoolean(),
			)

			"errorOnInvalidNesting" -> configuration.put(
				StateMachineConfigurationKeys.ErrorOnInvalidNesting,
				value.toBoolean(),
			)

			"errorOnIncompleteNestedState" -> configuration.put(
				StateMachineConfigurationKeys.ErrorOnIncompleteNestedState,
				value.toBoolean(),
			)

			"errorOnUnusedEvent" -> configuration.put(
				StateMachineConfigurationKeys.ErrorOnUnusedEvent,
				value.toBoolean(),
			)
		}
	}
}
