package com.klitsie.statemachine.state

import kotlin.reflect.KClass

data class StateTransition<State : Any, CurrentState : State, Event : Any>(
	val transition: (CurrentState, Event) -> State,
)

@StateDsl
interface TransitionBuilder<State : Any, CurrentState : State, Event : Any> {

	fun <E : Event> onEvent(eventClass: KClass<E>, transition: (CurrentState, E) -> State)
	fun buildTransitions(): Map<KClass<out Event>, StateTransition<State, CurrentState, Event>>

}

internal class DefaultTransitionBuilder<State : Any, CurrentState : State, Event : Any> :
	TransitionBuilder<State, CurrentState, Event> {

	private var transitions = mapOf<KClass<out Event>, StateTransition<State, CurrentState, Event>>()

	override fun <E : Event> onEvent(
		eventClass: KClass<E>,
		transition: (CurrentState, E) -> State,
	) {
		@Suppress("UNCHECKED_CAST")
		val stateTransition = StateTransition(transition) as StateTransition<State, CurrentState, Event>
		transitions = transitions
			.plus(eventClass to stateTransition)
	}

	override fun buildTransitions() = transitions

}
