package dev.klitsie.statemachine

import kotlin.reflect.KClass

/**
 * Represents a transition from one state to another triggered by an event.
 */
data class StateTransition<State : Any, CurrentState : State, Effect : Any, Event : Any>(
	val transition: EffectHandler<CurrentState, Effect>.(CurrentState, Event) -> State,
)

/**
 * Builder for defining transitions between states.
 */
@StateDsl
interface TransitionBuilder<State : Any, CurrentState : State, Effect : Any, Event : Any> {

	/**
	 * Defines a transition for the given [eventClass].
	 */
	fun <E : Event> onEvent(
		eventClass: KClass<E>,
		transition: EffectHandler<CurrentState, Effect>.(CurrentState, E) -> State,
	)

	/**
	 * Builds the map of defined transitions.
	 */
	fun buildTransitions(): Map<KClass<out Event>, StateTransition<State, CurrentState, Effect, Event>>

}

internal class DefaultTransitionBuilder<State : Any, CurrentState : State, Effect : Any, Event : Any> :
	TransitionBuilder<State, CurrentState, Effect, Event> {

	private var transitions = mapOf<KClass<out Event>, StateTransition<State, CurrentState, Effect, Event>>()

	override fun <E : Event> onEvent(
		eventClass: KClass<E>,
		transition: EffectHandler<CurrentState, Effect>.(state: CurrentState, event: E) -> State,
	) {
		@Suppress("UNCHECKED_CAST")
		val stateTransition = StateTransition(transition) as StateTransition<State, CurrentState, Effect, Event>
		transitions = transitions
			.plus(eventClass to stateTransition)
	}

	override fun buildTransitions() = transitions

}
