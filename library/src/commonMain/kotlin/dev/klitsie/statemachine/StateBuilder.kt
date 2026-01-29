package dev.klitsie.statemachine

import kotlin.reflect.KClass

/**
 * Definition of a single state, including its transitions and side effects.
 */
data class StateDefinition<State : Any, CurrentState : State, Effect : Any, Event : Any>(
	val clazz: KClass<out State>,
	val parent: KClass<out State>?,
	val sideEffects: List<SideEffect<State, CurrentState, Event>>,
	val transitions: Map<KClass<out Event>, StateTransition<State, CurrentState, Effect, Event>>,
)

/**
 * Builder for defining a single state.
 */
@StateDsl
class StateBuilder<State : Any, CurrentState : State, Effect : Any, Event : Any>(
	private val clazz: KClass<CurrentState>,
	private val parent: KClass<out State>,
) :
	SideEffectBuilder<State, CurrentState, Event> by DefaultSideEffectBuilder(),
	TransitionBuilder<State, CurrentState, Effect, Event> by DefaultTransitionBuilder() {

	/**
	 * Defines a transition for when an event of type [E] is received.
	 */
	inline fun <reified E : Event> onEvent(
		noinline transition: EffectHandler<CurrentState, Effect>.(state: CurrentState, event: E) -> State,
	) {
		onEvent(E::class, transition)
	}

	/**
	 * Builds the [StateDefinition].
	 */
	fun build(): StateDefinition<State, CurrentState, Effect, Event> {
		return StateDefinition(
			clazz = clazz,
			parent = parent,
			sideEffects = buildSideEffects(),
			transitions = buildTransitions(),
		)
	}

}
