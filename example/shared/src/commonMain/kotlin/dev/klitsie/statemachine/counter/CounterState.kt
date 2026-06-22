package dev.klitsie.statemachine.counter

sealed interface CounterState {
	data object Inactive : CounterState
	data class Active(val count: Int) : CounterState
}

sealed interface CounterEvent {
	data object Start : CounterEvent
	data object Increment : CounterEvent
	data object Decrement : CounterEvent
	data object Reset : CounterEvent
	data class NotifyMilestone(val count: Int) : CounterEvent
}

sealed interface CounterEffect {
	data class ShowToast(val message: String) : CounterEffect
}
