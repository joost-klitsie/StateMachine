package dev.klitsie.statemachine

/**
 * Builder for defining side effects that should run when a state is entered.
 */
@StateDsl
interface SideEffectBuilder<State : Any, CurrentState : State, Event : Any> {

	/**
	 * Defines a side effect to run when this state is entered.
	 *
	 * @param key A key to uniquely identify this side effect. If the state changes but the key remains the same,
	 * the side effect will not be restarted.
	 * @param effect The side effect to run. It's executed in a [StateMachine] scope.
	 */
	fun sideEffect(
		key: (CurrentState) -> Any = { it },
		effect: suspend StateMachine<State, *, Event>.(CurrentState) -> Unit,
	)

	/**
	 * Builds the list of defined side effects.
	 */
	fun buildSideEffects(): List<SideEffect<State, CurrentState, Event>>

}

internal class DefaultSideEffectBuilder<State : Any, CurrentState : State, Event : Any> :
	SideEffectBuilder<State, CurrentState, Event> {

	private var sideEffects: List<SideEffect<State, CurrentState, Event>> = emptyList()

	override fun sideEffect(
		key: (CurrentState) -> Any,
		effect: suspend StateMachine<State, *, Event>.(CurrentState) -> Unit,
	) {
		sideEffects += SideEffect(key, effect)
	}

	override fun buildSideEffects() = sideEffects

}

/**
 * Represents a side effect to be executed in a state.
 */
data class SideEffect<State : Any, in CurrentState : State, out Event : Any>(
	val key: (CurrentState) -> Any,
	val effect: suspend StateMachine<State, *, Event>.(CurrentState) -> Unit,
)
