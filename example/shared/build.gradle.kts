import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

plugins {
	alias(libs.plugins.kotlinMultiplatform)
	alias(libs.plugins.androidMultiplatformLibrary)
	alias(libs.plugins.composeMultiplatform)
	alias(libs.plugins.composeCompiler)
	alias(libs.plugins.statemachineCcompiler)
}

stateMachineCompiler {
	enabled = true
}

kotlin {
	androidLibrary {
		namespace = "dev.klitsie.statemachine.example.shared"
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
		outputModuleName.set("composeApp")
		browser {
			val rootDirPath = project.rootDir.path
			val projectDirPath = project.projectDir.path
			commonWebpackConfig {
				outputFileName = "composeApp.js"
				devServer = (devServer ?: KotlinWebpackConfig.DevServer()).apply {
					// Serve sources to debug inside the browser
					static(rootDirPath)
					static(projectDirPath)
				}
			}
		}
		binaries.executable()
	}

	sourceSets {
		commonMain.dependencies {
			implementation(projects.library)
			implementation(compose.runtime)
			implementation(compose.foundation)
			implementation(compose.material3)
			implementation(compose.ui)
			implementation(compose.components.resources)
			implementation(compose.components.uiToolingPreview)
			implementation(libs.androidx.lifecycle.viewmodelCompose)
			implementation(libs.androidx.lifecycle.runtimeCompose)
		}
		commonTest.dependencies {
			implementation(libs.kotlin.test)
		}
		jvmMain.dependencies {
			implementation(compose.desktop.currentOs)
			implementation(libs.kotlinx.coroutinesSwing)
		}
	}
}

compose.desktop {
	application {
		mainClass = "dev.klitsie.statemachine.MainKt"

		nativeDistributions {
			targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
			packageName = "dev.klitsie.statemachine"
			packageVersion = libs.versions.statemachine.get()
		}
	}
}
