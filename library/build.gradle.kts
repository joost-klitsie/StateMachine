@file:Suppress("UnstableApiUsage")

import com.android.build.api.dsl.androidLibrary
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
	alias(libs.plugins.kotlinMultiplatform)
	alias(libs.plugins.androidMultiplatformLibrary)
	id("maven-publish")
	alias(libs.plugins.vanniktech.mavenPublish)
}

group = "dev.klitsie.statemachine"
version = "0.2.0"

kotlin {
	androidLibrary {
		namespace = "com.klitsie.statemachine"
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
		macosX64(),
		macosArm64()
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
		}
		jvmMain.dependencies {
			implementation(libs.kotlinx.coroutinesSwing)
		}
	}
}

mavenPublishing {
	publishToMavenCentral()

	signAllPublications()

	coordinates(group.toString(), "statemachine-core", version.toString())

	pom {
		name = "State Machine"
		description = "A simple yet powerful state machine, build on top of Kotlin Coroutines"
		inceptionYear = "2026"
		url = "https://github.com/joost-klitsie/StateMachine/"
		licenses {
			license {
				name = "The Apache License, Version 2.0"
				url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
				distribution = "https://www.apache.org/licenses/LICENSE-2.0.txt"
			}
		}
		developers {
			developer {
				id = "joost-klitsie"
				name = "Joost klitsie"
				url = "https://github.com/joost-klitsie"
				email = "j.p.klitsie@gmail.com"
				organization = "Klitsie Development"
				organizationUrl = "https://klitsie.dev"
			}
		}
		scm {
			url = "https://github.com/joost-klitsie/StateMachine/"
			connection = "scm:git:git://github.com/joost-klitsie/StateMachine.git"
			developerConnection = "scm:git:ssh://git@github.com/joost-klitsie/StateMachine.git"
		}
	}
}
