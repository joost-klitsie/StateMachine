package dev.klitsie.statemachine.nested

sealed interface NestedExampleState {

	data object Pending : NestedExampleState
	data class Loading(val id: String, val shouldFail: Boolean) : NestedExampleState

	sealed interface InputName : NestedExampleState {

		val username: String

		data class Pending(override val username: String) : InputName
		data class Confirm(override val username: String) : InputName

	}

	sealed interface LoadingFailed : NestedExampleState {
		data object UserIsAnIdiot : LoadingFailed
		data object UserLockedOut : LoadingFailed
		data class Retryable(val error: Throwable, val id: String) : LoadingFailed
	}

}

sealed interface NestedExampleEffect {

	data object Close : NestedExampleEffect

}

sealed interface NestedExampleEvent {

	data class StartLoading(val id: String) : NestedExampleEvent
	data class LoadingResult(val result: Result<String>) : NestedExampleEvent
	data class UpdateName(val newValue: String) : NestedExampleEvent

	data object Retry : NestedExampleEvent
	data object Reset : NestedExampleEvent
	data object Close : NestedExampleEvent

}
