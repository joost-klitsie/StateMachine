package dev.klitsie.statemachine.nested

sealed interface NestedExampleState {

	data object Pending : NestedExampleState
	data class Loading(val id: String, val shouldFail: Boolean) : NestedExampleState

	sealed interface InputName : NestedExampleState {

		val username: String

		data class Pending(override val username: String) : InputName
		data class Confirm(override val username: String) : InputName

	}

	data object CloseScreen : NestedExampleState

	sealed interface LoadingFailed : NestedExampleState {
		data object UserIsAnIdiot : LoadingFailed
		data object UserLockedOut : LoadingFailed
		data class Retryable(val error: Throwable, val id: String) : LoadingFailed
	}

}

sealed interface ExampleEvent {

	data class StartLoading(val id: String) : ExampleEvent
	data class LoadingResult(val result: Result<String>) : ExampleEvent
	data class UpdateName(val newValue: String) : ExampleEvent

	data object Retry : ExampleEvent
	data object Reset : ExampleEvent
	data object Close : ExampleEvent

}
