import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	alias(libs.plugins.kotlin.jvm)
	alias(libs.plugins.klitsie.publishing)
}

group = "dev.klitsie.statemachine"

stateMachinePublishing {
	artifactId = "compiler-plugin"
	name = "State Machine Compiler Plugin"
	description = "Checks the safety of any statemachine that is written with the library."
}

sourceSets {
	main {
		java.setSrcDirs(listOf("src"))
		resources.setSrcDirs(listOf("resources"))
	}
}

dependencies {
	compileOnly(libs.kotlin.compiler.embeddable)
}

kotlin {
	compilerOptions {
		optIn.add("org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi")
		optIn.add("org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI")
	}
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.compilerOptions {
	freeCompilerArgs.set(listOf("-Xcontext-parameters"))
}
