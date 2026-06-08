@file:Suppress("UnstableApiUsage")

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
	alias(libs.plugins.kotlinMultiplatform)
	alias(libs.plugins.androidMultiplatformLibrary)
	alias(libs.plugins.klitsie.publishing)
	alias(libs.plugins.kotlinxKover)
}

stateMachinePublishing {
	artifactId = "statemachine-core"
	name = "State Machine Core"
	description = "A simple yet powerful state machine built on top of Kotlin Coroutines"
}

kotlin {
	compilerOptions {
		apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_2)
		languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_2)
	}
	androidLibrary {
		namespace = "dev.klitsie.statemachine"
		compileSdk = libs.versions.android.compileSdk.get().toInt()
		minSdk = libs.versions.android.minSdk.get().toInt()

		optimization {
			minify = false
		}
		compilerOptions {
			jvmTarget.set(JvmTarget.JVM_11)
		}

		packaging {
			resources {
				excludes += "/META-INF/{AL2.0,LGPL2.1}"
			}
		}

	}

	jvm()

	@OptIn(ExperimentalWasmDsl::class)
	wasmJs {
		browser {
		}
		binaries.executable()
	}
	js {
		browser {
		}
		binaries.executable()
	}
	listOf(
		iosX64(),
		iosArm64(),
		iosSimulatorArm64(),
		macosArm64(),
	).forEach { iosTarget ->
		iosTarget.binaries.framework {
			baseName = "statemachine"
			isStatic = true
		}
	}

	sourceSets {
		commonMain.dependencies {
			implementation(libs.kotlinx.coroutinesCore)
		}
		commonTest.dependencies {
			implementation(libs.kotlin.test)
			implementation(libs.kotlinx.coroutinesTest)
		}
		jvmMain.dependencies {
			implementation(libs.kotlinx.coroutinesSwing)
		}
	}
}
