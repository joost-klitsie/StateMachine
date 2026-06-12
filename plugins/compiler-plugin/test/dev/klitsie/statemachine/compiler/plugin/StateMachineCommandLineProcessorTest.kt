package dev.klitsie.statemachine.compiler.plugin

import org.jetbrains.kotlin.config.CompilerConfiguration
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(CompilerConfiguration.Internals::class)
class StateMachineCommandLineProcessorTest {

	@Test
	fun `test pluginId is correct`() {
		val processor = StateMachineCommandLineProcessor()
		assertEquals("dev.klitsie.statemachine.compiler.plugin", processor.pluginId)
	}

	@Test
	fun `test pluginOptions are defined`() {
		val processor = StateMachineCommandLineProcessor()
		val options = processor.pluginOptions
		assertTrue(options.any { it.optionName == "enabled" })
		assertTrue(options.any { it.optionName == "errorOnMissingLeaf" })
		assertTrue(options.any { it.optionName == "errorOnDuplicateState" })
		assertTrue(options.any { it.optionName == "errorOnInvalidNesting" })
		assertTrue(options.any { it.optionName == "errorOnIncompleteNestedState" })
		assertTrue(options.any { it.optionName == "errorOnUnusedEvent" })
	}

	@Test
	fun `test processOption sets configuration correctly`() {
		val processor = StateMachineCommandLineProcessor()
		val configuration = CompilerConfiguration()

		val options = processor.pluginOptions

		// Test each option with true
		options.forEach { option ->
			processor.processOption(option, "true", configuration)
		}

		assertEquals(true, configuration[StateMachineConfigurationKeys.Enabled])
		assertEquals(true, configuration[StateMachineConfigurationKeys.ErrorOnMissingLeaf])
		assertEquals(true, configuration[StateMachineConfigurationKeys.ErrorOnDuplicateState])
		assertEquals(true, configuration[StateMachineConfigurationKeys.ErrorOnInvalidNesting])
		assertEquals(true, configuration[StateMachineConfigurationKeys.ErrorOnIncompleteNestedState])
		assertEquals(true, configuration[StateMachineConfigurationKeys.ErrorOnUnusedEvent])

		// Test each option with false
		options.forEach { option ->
			processor.processOption(option, "false", configuration)
		}

		assertEquals(false, configuration[StateMachineConfigurationKeys.Enabled])
		assertEquals(false, configuration[StateMachineConfigurationKeys.ErrorOnMissingLeaf])
		assertEquals(false, configuration[StateMachineConfigurationKeys.ErrorOnDuplicateState])
		assertEquals(false, configuration[StateMachineConfigurationKeys.ErrorOnInvalidNesting])
		assertEquals(false, configuration[StateMachineConfigurationKeys.ErrorOnIncompleteNestedState])
		assertEquals(false, configuration[StateMachineConfigurationKeys.ErrorOnUnusedEvent])
	}
}
