package com.klitsie.statemachine.state

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.SharingStarted
import kotlin.reflect.KClass

fun <State : Any, Event : Any> stateMachine(
	scope: CoroutineScope,
	initialState: State,
	started: SharingStarted = SharingStarted.WhileSubscribed(5000),
	builder: StateMachineBuilder<State, Event>.() -> Unit,
): StateMachine<State, Event> {
	val stateMachineDefinition = StateMachineBuilder<State, Event>()
		.apply(builder)
		.build()

	return DefaultStateMachine(
		scope = scope,
		initialState = initialState,
		started = started,
		definition = stateMachineDefinition,
	)

}

@DslMarker
annotation class StateDsl

data class StateMachineDefinition<State : Any, Event : Any>(
	val states: Map<KClass<out State>, StateDefinition<State, State, Event>>,
)

@StateDsl
class StateMachineBuilder<State : Any, Event : Any> {

	var states = emptyMap<KClass<out State>, StateDefinition<State, State, Event>>()
		private set

	inline fun <reified S : State> state(
		noinline stateBuilder: StateBuilder<State, S, Event>.() -> Unit = {},
	) {
		state(S::class, stateBuilder)
	}

	fun <S : State> state(
		stateClass: KClass<S>,
		stateBuilder: StateBuilder<State, S, Event>.() -> Unit = {},
	) {
		@Suppress("UNCHECKED_CAST")
		val stateHolder = StateBuilder<State, S, Event>().apply(stateBuilder)
			.build() as StateDefinition<State, State, Event>
		states = states
			.plus(stateClass to stateHolder)
	}

	fun build(): StateMachineDefinition<State, Event> {
		return StateMachineDefinition(
			states = states,
		)
	}

}

