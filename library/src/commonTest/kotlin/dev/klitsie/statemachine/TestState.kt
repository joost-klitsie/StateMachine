package dev.klitsie.statemachine

sealed interface TestState {

	data class StateA(val value: String) : TestState
	data class StateB(val value: String) : TestState
	data class StateC(val value: String) : TestState

	sealed interface NestedState : TestState {

		val nestedValue: String

		data class NestedStateA(override val nestedValue: String) : NestedState
		data class NestedStateB(override val nestedValue: String) : NestedState

	}

}

sealed interface TestEffect {

	data object Close : TestEffect
	data class Count(val count: Int) : TestEffect

}

sealed interface TestEvent {

	data object MoveForward : TestEvent
	data class Append(val value: String) : TestEvent
	data object Close : TestEvent

}
