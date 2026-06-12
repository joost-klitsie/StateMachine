package dev.klitsie.statemachine.compiler.plugin

import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCompilerApi::class)
class StateMachineDslCheckerTest {

	@Test
	fun `valid state machine DSL compiles without errors`() {
		val kotlinSource = loadSourceFile("valid_dsl.txt")

		val result = compile(kotlinSource)

		assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
		assertTrue(
			result.messages.isEmpty() || !result.messages.contains("Nesting error"),
			"Expected no invalid nesting errors. Messages: ${'$'}{result.messages}",
		)
	}

	@Test
	fun `nested state compiles without errors when correctly structured`() {
		val kotlinSource = loadSourceFile("nested_state_correct.txt")

		val result = compile(kotlinSource)
		assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
	}

	@Test
	fun `nested state skipped intermediate parent compiles without errors`() {
		val kotlinSource = loadSourceFile("nested_state_skipped_parent.txt")

		val result = compile(kotlinSource)
		assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
	}

	@Test
	fun `invalid nesting reports an error`() {
		val kotlinSource = loadSourceFile("invalid_nesting.txt")

		val result = compile(kotlinSource, pluginOptionsMap = mapOf("errorOnInvalidNesting" to "true"))
		assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
		assertTrue(
			result.messages.contains("Nesting error"),
			"Expected INVALID_NESTING error. Messages: ${result.messages}",
		)
	}

	@Test
	fun `invalid nesting reports a warning`() {
		val kotlinSource = loadSourceFile("invalid_nesting.txt")

		val result = compile(kotlinSource, pluginOptionsMap = mapOf("errorOnInvalidNesting" to "false"))
		assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
		assertTrue(
			result.messages.contains("Nesting error"),
			"Expected INVALID_NESTING warning. Messages: ${result.messages}",
		)
	}

	@Test
	fun `missing leaf reports an error`() {
		val kotlinSource = loadSourceFile("missing_leaf.txt")

		val result = compile(kotlinSource, pluginOptionsMap = mapOf("errorOnMissingLeaf" to "true"))
		assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
		assertTrue(
			result.messages.contains("Missing states in"),
			"Expected MISSING_LEAF error. Messages: ${result.messages}",
		)
	}

	@Test
	fun `missing leaf reports a warning`() {
		val kotlinSource = loadSourceFile("missing_leaf.txt")

		val result = compile(kotlinSource, pluginOptionsMap = mapOf("errorOnMissingLeaf" to "false"))
		assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
		assertTrue(
			result.messages.contains("Missing states in"),
			"Expected MISSING_LEAF warning. Messages: ${result.messages}",
		)
	}

	@Test
	fun `duplicate state reports an error`() {
		val kotlinSource = loadSourceFile("duplicate_state.txt")

		val result = compile(kotlinSource, pluginOptionsMap = mapOf("errorOnDuplicateState" to "true"))
		assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
		assertTrue(
			result.messages.contains("Duplicate state"),
			"Expected DUPLICATE_STATE error. Messages: ${result.messages}",
		)
	}

	@Test
	fun `duplicate state reports a warning`() {
		val kotlinSource = loadSourceFile("duplicate_state.txt")

		val result = compile(kotlinSource, pluginOptionsMap = mapOf("errorOnDuplicateState" to "false"))
		assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
		assertTrue(
			result.messages.contains("Duplicate state"),
			"Expected DUPLICATE_STATE warning. Messages: ${result.messages}",
		)
	}

	@Test
	fun `incomplete nested reports an error`() {
		val kotlinSource = loadSourceFile("incomplete_nested.txt")

		val result = compile(kotlinSource, pluginOptionsMap = mapOf("errorOnIncompleteNestedState" to "true"))
		assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
		assertTrue(
			result.messages.contains("is incomplete"),
			"Expected INCOMPLETE_NESTED error. Messages: ${result.messages}",
		)
	}

	@Test
	fun `incomplete nested reports a warning`() {
		val kotlinSource = loadSourceFile("incomplete_nested.txt")

		val result = compile(kotlinSource, pluginOptionsMap = mapOf("errorOnIncompleteNestedState" to "false"))
		assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
		assertTrue(
			result.messages.contains("is incomplete"),
			"Expected INCOMPLETE_NESTED warning. Messages: ${result.messages}",
		)
	}

	@Test
	fun `unused event reports an error`() {
		val kotlinSource = loadSourceFile("unused_event.txt")

		val result = compile(kotlinSource, pluginOptionsMap = mapOf("errorOnUnusedEvent" to "true"))
		assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
		assertTrue(
			result.messages.contains("is never handled"),
			"Expected UNUSED_EVENT error. Messages: ${result.messages}",
		)
	}

	@Test
	fun `unused event reports a warning`() {
		val kotlinSource = loadSourceFile("unused_event.txt")

		val result = compile(kotlinSource, pluginOptionsMap = mapOf("errorOnUnusedEvent" to "false"))
		assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
		assertTrue(
			result.messages.contains("is never handled"),
			"Expected UNUSED_EVENT warning. Messages: ${result.messages}",
		)
	}

	@Test
	fun `used event compiles without errors`() {
		val kotlinSource = loadSourceFile("used_event.txt")

		val result = compile(kotlinSource, pluginOptionsMap = mapOf("errorOnUnusedEvent" to "true"))
		assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
		assertTrue(
			!result.messages.contains("is never handled"),
			"Expected no unused event error. Messages: ${result.messages}",
		)
	}

	private fun loadSourceFile(fileName: String): SourceFile {
		val resourceUrl = this::class.java.getResource("/$fileName")
			?: error("Test file not found: '$fileName'.")
		val virtualFileName = fileName.replace(".txt", ".kt")

		return SourceFile.kotlin(virtualFileName, resourceUrl.readText())
	}

	@Test
	fun `invalid nesting in extension function reports an error`() {
		val kotlinSource = loadSourceFile("extension_invalid_nesting.txt")

		val result = compile(kotlinSource, pluginOptionsMap = mapOf("errorOnInvalidNesting" to "true"))
		assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
		assertTrue(
			result.messages.contains("Nesting error"),
			"Expected INVALID_NESTING error. Messages: ${result.messages}",
		)
	}

	@Test
	fun `duplicate state in extension function reports an error`() {
		val kotlinSource = loadSourceFile("extension_duplicate.txt")

		val result = compile(kotlinSource, pluginOptionsMap = mapOf("errorOnDuplicateState" to "true"))
		assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
		assertTrue(
			result.messages.contains("Duplicate state"),
			"Expected DUPLICATE_STATE error. Messages: ${result.messages}",
		)
	}

	private fun compile(
		vararg sourceFiles: SourceFile,
		pluginOptionsMap: Map<String, String> = emptyMap(),
	): JvmCompilationResult {
		return KotlinCompilation().apply {
			sources = sourceFiles.toList()
			compilerPluginRegistrars = listOf(StateMachinePluginRegistrar())
			commandLineProcessors = listOf(StateMachineCommandLineProcessor())
			pluginOptions = pluginOptionsMap.map {
				com.tschuchort.compiletesting.PluginOption(
					"dev.klitsie.statemachine.compiler.plugin",
					it.key,
					it.value,
				)
			}
			inheritClassPath = true
			jvmTarget = "21"
			messageOutputStream = System.out
		}.compile()
	}
}
