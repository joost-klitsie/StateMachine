package com.klitsie.statemachine.state

import kotlin.reflect.KClass

data class StateDefinition<State : Any, CurrentState : State, Event : Any>(
	val clazz: KClass<out State>,
	val parent: KClass<out State>?,
	val sideEffects: List<SideEffect<State, CurrentState, Event>>,
	val transitions: Map<KClass<out Event>, StateTransition<State, CurrentState, Event>>,
)

@StateDsl
class StateBuilder<State : Any, CurrentState : State, Event : Any>(
	private val clazz: KClass<CurrentState>,
	private val parent: KClass<out State>,
) :
	SideEffectBuilder<State, CurrentState, Event> by DefaultSideEffectBuilder(),
	TransitionBuilder<State, CurrentState, Event> by DefaultTransitionBuilder() {

	inline fun <reified E : Event> onEvent(
		noinline transition: (CurrentState, E) -> State,
	) {
		onEvent(E::class, transition)
	}

	fun build(): StateDefinition<State, CurrentState, Event> {
		return StateDefinition(
			clazz = clazz,
			parent = parent,
			sideEffects = buildSideEffects(),
			transitions = buildTransitions(),
			sideEffects = sideEffects,
		)
	}

}
