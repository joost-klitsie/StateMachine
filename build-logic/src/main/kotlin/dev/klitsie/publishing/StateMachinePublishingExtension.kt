package dev.klitsie.publishing

import org.gradle.api.provider.Property

interface StateMachinePublishingExtension {
	val name: Property<String>
	val description: Property<String>
	val artifactId: Property<String>
}
