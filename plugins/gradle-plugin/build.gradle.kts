plugins {
	`kotlin-dsl`
	alias(libs.plugins.kotlin.jvm)
	alias(libs.plugins.gradle.plugin)
	alias(libs.plugins.buildconfig)
	alias(libs.plugins.klitsie.publishing)
}

stateMachinePublishing {
	artifactId = "gradle-plugin"
	name = "State Machine Gradle Plugin"
	description = "Gradle plugin to configure and apply the State Machine compiler plugin."
}

sourceSets {
	main {
		java.setSrcDirs(listOf("src"))
		resources.setSrcDirs(listOf("resources"))
	}
	test {
		java.setSrcDirs(listOf("test"))
		resources.setSrcDirs(listOf("testResources"))
	}
}

dependencies {
	compileOnly(libs.kotlin.gradle.plugin.api)
	// https://github.com/gradle/gradle/issues/15383
	compileOnly(files(libs::class.java.superclass.protectionDomain.codeSource.location))
}

buildConfig {
	packageName("dev.klitsie.statemachine.compiler.plugin")

	buildConfigField("String", "PLUGIN_VERSION", "\"${libs.versions.statemachine.get()}\"")
}

gradlePlugin {
	plugins {
		create("SimplePlugin") {
			id = "dev.klitsie.statemachine.compiler.plugin"
			displayName = "StateMachine Compiler Plugin"
			description = "A compiler plugin to check the safety of state machines"
			implementationClass = "dev.klitsie.statemachine.compiler.plugin.StateMachineGradlePlugin"
		}
	}
}
