package dev.klitsie.publishing

import com.vanniktech.maven.publish.MavenPublishBaseExtension
import libs
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.create

@Suppress("unused")
class StateMachinePublishingPlugin : Plugin<Project> {
	override fun apply(project: Project) = with(project) {
		val extension = extensions.create<StateMachinePublishingExtension>("stateMachinePublishing")
		pluginManager.apply("com.vanniktech.maven.publish")

		group = "dev.klitsie.statemachine"
		version = project.libs.versions.statemachine.get()

		project.afterEvaluate {
			extensions.configure<MavenPublishBaseExtension> {
				publishToMavenCentral()
				signAllPublications()
				coordinates(group.toString(), extension.artifactId.get(), version.toString())
				pom {
					// .set() inherently accepts lazy Properties.
					// It will resolve these values safely without afterEvaluate.
					name.set(extension.name)
					description.set(extension.description)
					inceptionYear.set("2026")
					url.set("https://github.com/joost-klitsie/StateMachine/")
					licenses {
						license {
							name.set("The Apache License, Version 2.0")
							url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
							distribution.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
						}
					}
					developers {
						developer {
							id.set("joost-klitsie")
							name.set("Joost klitsie")
							url.set("https://github.com/joost-klitsie")
							email.set("j.p.klitsie@gmail.com")
							organization.set("Klitsie Development")
							organizationUrl.set("https://klitsie.dev")
						}
					}
					scm {
						url.set("https://github.com/joost-klitsie/StateMachine/")
						connection.set("scm:git:git://github.com/joost-klitsie/StateMachine.git")
						developerConnection.set("scm:git:ssh://git@github.com/joost-klitsie/StateMachine.git")
					}
				}
			}
		}
		val groupProvider = project.provider { project.group.toString() }
		val versionProvider = project.provider { project.version.toString() }
		tasks.configureEach {
			if (name.contains("publish", ignoreCase = true)) {
				doFirst {
					if (!extension.name.isPresent) throw GradleException("stateMachinePublishing.name is missing")
					if (!extension.description.isPresent) throw GradleException("stateMachinePublishing.description is missing")
					if (!extension.artifactId.isPresent) throw GradleException("stateMachinePublishing.artifactId is missing")
					if (groupProvider.get().isEmpty()) throw GradleException("project.group is missing")
					if (versionProvider.get().isEmpty()) throw GradleException("project.version is missing")
				}
			}
		}
	}
}
