package dev.klitsie.statemachine.counter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.klitsie.statemachine.stateMachine
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

class CounterViewModel : ViewModel() {

	val stateMachine = stateMachine<CounterState, CounterEffect, CounterEvent>(
		scope = viewModelScope,
		initialState = CounterState.Inactive,
	) {
		state<CounterState.Inactive> {
			onEvent<CounterEvent.Start> { _, _ ->
				CounterState.Active(0)
			}
		}
		state<CounterState.Active> {
			onEvent<CounterEvent.Increment> { state, _ ->
				state.copy(count = state.count + 1)
			}
			onEvent<CounterEvent.Decrement> { state, _ ->
				state.copy(count = state.count - 1)
			}
			onEvent<CounterEvent.Reset> { _, _ ->
				CounterState.Inactive
			}

			onEvent<CounterEvent.NotifyMilestone> { state, event ->
				trigger(CounterEffect.ShowToast("Reached ${event.count}!"))
				state
			}

			// Side effect that triggers when count is a multiple of 10
			sideEffect(key = { it.count }) { state ->
				if (state.count != 0 && state.count % 10 == 0) {
					send(CounterEvent.NotifyMilestone(state.count))
				}
			}

			// Side effect that automatically increments the counter every second
			sideEffect {
				delay(1.seconds)
				send(CounterEvent.Increment)
			}
		}
	}
}
