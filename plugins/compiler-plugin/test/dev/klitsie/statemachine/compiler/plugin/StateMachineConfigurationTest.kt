package dev.klitsie.statemachine.compiler.plugin

import org.jetbrains.kotlin.config.CompilerConfiguration
import org.junit.Test
import kotlin.test.assertEquals

@OptIn(CompilerConfiguration.Internals::class)
class StateMachineConfigurationTest {

	@Test
	fun `toConfiguration maps values correctly with defaults`() {
		val compilerConfig = CompilerConfiguration()
		val config = compilerConfig.toConfiguration()

		assertEquals(false, config.errorOnMissingLeaf)
		assertEquals(true, config.errorOnDuplicateState)
		assertEquals(true, config.errorOnInvalidNesting)
		assertEquals(true, config.errorOnIncompleteNestedState)
		assertEquals(false, config.errorOnUnusedEvent)
	}

	@Test
	fun `toConfiguration maps explicit values correctly`() {
		val compilerConfig = CompilerConfiguration().apply {
			put(StateMachineConfigurationKeys.ErrorOnMissingLeaf, true)
			put(StateMachineConfigurationKeys.ErrorOnDuplicateState, false)
			put(StateMachineConfigurationKeys.ErrorOnInvalidNesting, false)
			put(StateMachineConfigurationKeys.ErrorOnIncompleteNestedState, false)
			put(StateMachineConfigurationKeys.ErrorOnUnusedEvent, true)
		}
		val config = compilerConfig.toConfiguration()

		assertEquals(true, config.errorOnMissingLeaf)
		assertEquals(false, config.errorOnDuplicateState)
		assertEquals(false, config.errorOnInvalidNesting)
		assertEquals(false, config.errorOnIncompleteNestedState)
		assertEquals(true, config.errorOnUnusedEvent)
	}

	@Test
	fun `missingLeafDiagnostics returns correct diagnostic`() {
		val errorConfig = StateMachineConfiguration(
			errorOnMissingLeaf = true,
			errorOnDuplicateState = false,
			errorOnInvalidNesting = false,
			errorOnIncompleteNestedState = false,
			errorOnUnusedEvent = false,
		)
		val warningConfig = errorConfig.copy(errorOnMissingLeaf = false)

		assertEquals(FirPluginDiagnostics.MissingLeafStatesError, errorConfig.missingLeafDiagnostics())
		assertEquals(FirPluginDiagnostics.MissingLeafStatesWarning, warningConfig.missingLeafDiagnostics())
	}

	@Test
	fun `duplicateStateDiagnostics returns correct diagnostic`() {
		val errorConfig = StateMachineConfiguration(
			errorOnMissingLeaf = false,
			errorOnDuplicateState = true,
			errorOnInvalidNesting = false,
			errorOnIncompleteNestedState = false,
			errorOnUnusedEvent = false,
		)
		val warningConfig = errorConfig.copy(errorOnDuplicateState = false)

		assertEquals(FirPluginDiagnostics.DuplicateStateDeclarationError, errorConfig.duplicateStateDiagnostics())
		assertEquals(FirPluginDiagnostics.DuplicateStateDeclarationWarning, warningConfig.duplicateStateDiagnostics())
	}

	@Test
	fun `invalidNestingDiagnostics returns correct diagnostic`() {
		val errorConfig = StateMachineConfiguration(
			errorOnMissingLeaf = false,
			errorOnDuplicateState = false,
			errorOnInvalidNesting = true,
			errorOnIncompleteNestedState = false,
			errorOnUnusedEvent = false,
		)
		val warningConfig = errorConfig.copy(errorOnInvalidNesting = false)

		assertEquals(FirPluginDiagnostics.InvalidNestingError, errorConfig.invalidNestingDiagnostics())
		assertEquals(FirPluginDiagnostics.InvalidNestingWarning, warningConfig.invalidNestingDiagnostics())
	}

	@Test
	fun `incompleteNestedDiagnostics returns correct diagnostic`() {
		val errorConfig = StateMachineConfiguration(
			errorOnMissingLeaf = false,
			errorOnDuplicateState = false,
			errorOnInvalidNesting = false,
			errorOnIncompleteNestedState = true,
			errorOnUnusedEvent = false,
		)
		val warningConfig = errorConfig.copy(errorOnIncompleteNestedState = false)

		assertEquals(FirPluginDiagnostics.IncompleteNestedStateError, errorConfig.incompleteNestedDiagnostics())
		assertEquals(FirPluginDiagnostics.IncompleteNestedStateWarning, warningConfig.incompleteNestedDiagnostics())
	}

	@Test
	fun `unusedEventDiagnostics returns correct diagnostic`() {
		val errorConfig = StateMachineConfiguration(
			errorOnMissingLeaf = false,
			errorOnDuplicateState = false,
			errorOnInvalidNesting = false,
			errorOnIncompleteNestedState = false,
			errorOnUnusedEvent = true,
		)
		val warningConfig = errorConfig.copy(errorOnUnusedEvent = false)

		assertEquals(FirPluginDiagnostics.UnusedEventDefinitionError, errorConfig.unusedEventDiagnostics())
		assertEquals(FirPluginDiagnostics.UnusedEventDefinitionWarning, warningConfig.unusedEventDiagnostics())
	}
}
