plugins {
	`kotlin-dsl`
}

dependencies {
	implementation(libs.vanniktech.mavenPublish.plugin)
	// https://github.com/gradle/gradle/issues/15383
	compileOnly(files(libs::class.java.superclass.protectionDomain.codeSource.location))
}

gradlePlugin {
	plugins {
		register("stateMachinePublishing") {
			id = libs.plugins.klitsie.publishing.get().pluginId
			implementationClass = "dev.klitsie.publishing.StateMachinePublishingPlugin"
		}
	}
}
