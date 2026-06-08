package dev.klitsie.statemachine.compiler.plugin

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.*

@Suppress("unused") // Used via reflection.
class StateMachineGradlePlugin : KotlinCompilerPluginSupportPlugin {
	override fun apply(target: Project) {
		target.extensions.create("stateMachineCompiler", StatemachineCompilerExtension::class.java)
	}

	override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean {
		val project = kotlinCompilation.target.project
		val versionString = project.plugins
			.filterIsInstance<KotlinBasePlugin>()
			.firstOrNull()
			?.pluginVersion
			?: throw GradleException("Kotlin plugin is not applied to project :${project.name}")

		// Strip suffixes like "-RC" or "-Beta" to safely parse the numbers
		val cleanVersion = versionString.substringBefore('-')
		val parts = cleanVersion.split('.').mapNotNull { it.toIntOrNull() }

		val major = parts.getOrNull(0) ?: 0
		val minor = parts.getOrNull(1) ?: 0

		if (major < 2 || (major == 2 && minor < 4)) {
			val errorMessage =
				"The StateMachine compiler plugin requires Kotlin 2.4.0 or higher. The current project is using Kotlin $versionString."
			throw GradleException(errorMessage)
		}

		return true
	}

	override fun getCompilerPluginId(): String = "dev.klitsie.statemachine.compiler.plugin"

	override fun getPluginArtifact() = SubpluginArtifact(
		groupId = "dev.klitsie.statemachine",
		artifactId = "compiler-plugin",
		version = BuildConfig.PLUGIN_VERSION,
	)

	override fun applyToCompilation(
		kotlinCompilation: KotlinCompilation<*>,
	): Provider<List<SubpluginOption>> {
		val project = kotlinCompilation.target.project

		return project.provider {
			val extension = project.extensions.getByType(StatemachineCompilerExtension::class.java)

			listOf(
				SubpluginOption(OPTION_ENABLED, extension.enabled.get().toString()),
				SubpluginOption(
					OPTION_ERROR_ON_MISSING_LEAF,
					extension.errorOnMissingLeaf.get().toString(),
				),
				SubpluginOption(
					OPTION_ERROR_ON_DUPLICATE_STATE,
					extension.errorOnDuplicateState.get().toString(),
				),
				SubpluginOption(
					OPTION_ERROR_ON_INVALID_NESTING,
					extension.errorOnInvalidNesting.get().toString(),
				),
				SubpluginOption(
					OPTION_ERROR_ON_INCOMPLETE_NESTED_STATE,
					extension.errorOnIncompleteNestedState.get().toString(),
				),
				SubpluginOption(
					OPTION_ERROR_ON_UNUSED_EVENT,
					extension.errorOnUnusedEvent.get().toString(),
				),
			)
		}
	}

	companion object {

		const val OPTION_ENABLED = "enabled"
		const val OPTION_ERROR_ON_MISSING_LEAF = "errorOnMissingLeaf"
		const val OPTION_ERROR_ON_DUPLICATE_STATE = "errorOnDuplicateState"
		const val OPTION_ERROR_ON_INVALID_NESTING = "errorOnInvalidNesting"
		const val OPTION_ERROR_ON_INCOMPLETE_NESTED_STATE = "errorOnIncompleteNestedState"
		const val OPTION_ERROR_ON_UNUSED_EVENT = "errorOnUnusedEvent"
	}
}
