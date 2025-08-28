package com.klitsie.statemachine.state

import kotlin.reflect.KClass

data class StateTransition<State : Any, CurrentState : State, Event : Any>(
	val transition: (CurrentState, Event) -> State,
)

data class SideEffect<State : Any, in CurrentState : State, out Event : Any>(
    val key: (CurrentState) -> Any,
    val effect: suspend StateMachine<State, Event>.(CurrentState) -> Unit,
)

data class StateDefinition<State : Any, CurrentState : State, Event : Any>(
	val transitions: Map<KClass<out Event>, StateTransition<State, CurrentState, Event>>,
    val sideEffects: List<SideEffect<State, CurrentState, Event>>,
)

@StateDsl
class StateBuilder<State : Any, CurrentState : State, Event : Any> {

	var transitions = mapOf<KClass<out Event>, StateTransition<State, CurrentState, Event>>()
		private set
    var sideEffects: List<SideEffect<State, CurrentState, Event>> = emptyList()
        private set

	inline fun <reified E : Event> onEvent(
		noinline transitionBuilder: (CurrentState, E) -> State,
	) {
		onEvent(E::class, transitionBuilder)
	}

	fun <E : Event> onEvent(eventClass: KClass<E>, transitionBuilder: (CurrentState, E) -> State) {
		@Suppress("UNCHECKED_CAST")
		val stateTransition = StateTransition(transitionBuilder) as StateTransition<State, CurrentState, Event>
		transitions = transitions
			.plus(eventClass to stateTransition)
	}

    fun sideEffect(
        key: (CurrentState) -> Any = { it },
        effect: suspend StateMachine<State, Event>.(CurrentState) -> Unit,
    ) {
        sideEffects += SideEffect(key, effect)
    }

	fun build(): StateDefinition<State, CurrentState, Event> {
		return StateDefinition(
			transitions = transitions,
            sideEffects = sideEffects,
		)
	}

}
