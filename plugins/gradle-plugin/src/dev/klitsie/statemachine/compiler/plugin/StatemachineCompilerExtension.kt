package dev.klitsie.statemachine.compiler.plugin

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property

open class StatemachineCompilerExtension(objectFactory: ObjectFactory) {

	val enabled: Property<Boolean> = objectFactory.property(Boolean::class.java).convention(true)
	val errorOnMissingLeaf: Property<Boolean> =
		objectFactory.property(Boolean::class.java).convention(false)
	val errorOnDuplicateState: Property<Boolean> =
		objectFactory.property(Boolean::class.java).convention(true)
	val errorOnInvalidNesting: Property<Boolean> =
		objectFactory.property(Boolean::class.java).convention(true)
	val errorOnIncompleteNestedState: Property<Boolean> =
		objectFactory.property(Boolean::class.java).convention(false)
	val errorOnUnusedEvent: Property<Boolean> =
		objectFactory.property(Boolean::class.java).convention(false)

}
