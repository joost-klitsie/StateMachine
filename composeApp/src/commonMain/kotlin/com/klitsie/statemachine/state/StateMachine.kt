package com.klitsie.statemachine.state

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

interface StateMachine<out State : Any, in Event : Any> {

	val state: StateFlow<State>

	fun onEvent(event: Event)

}

internal class DefaultStateMachine<out State : Any, in Event : Any>(
	private val scope: CoroutineScope,
	private val initialState: State,
	private val started: SharingStarted = SharingStarted.WhileSubscribed(5000),
	private val definition: StateMachineDefinition<State, Event>,
) : StateMachine<State, Event> {

	private val events = Channel<Event>()

	override val state by lazy {
		var lastState: State = initialState
		events.receiveAsFlow()
			.runningFold(initial = { initialState }) { state, event ->
				definition.states[state::class]
					?.transitions[event::class]
					?.transition(state, event)
					?: state
			}
			.onEach { lastState = it }
			.stateIn(
				scope = scope,
				started = started,
				initialValue = initialState
			)
	}

	override fun onEvent(event: Event) {
		scope.launch {
			events.send(event)
		}
	}

	private fun <T, R> Flow<T>.runningFold(
		initial: () -> R,
		operation: suspend (accumulator: R, value: T) -> R
	): Flow<R> {
		return flow {
			var accumulator: R = initial()
			emit(accumulator)
			collect { value ->
				accumulator = operation(accumulator, value)
				emit(accumulator)
			}
		}
	}

}
