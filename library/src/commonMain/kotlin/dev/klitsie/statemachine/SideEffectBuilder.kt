package dev.klitsie.statemachine

@StateDsl
interface SideEffectBuilder<State : Any, CurrentState : State, Event : Any> {

	fun sideEffect(
		key: (CurrentState) -> Any = { it },
		effect: suspend StateMachine<State, Event>.(CurrentState) -> Unit,
	)

	fun buildSideEffects(): List<SideEffect<State, CurrentState, Event>>

}

internal class DefaultSideEffectBuilder<State : Any, CurrentState : State, Event : Any> :
	SideEffectBuilder<State, CurrentState, Event> {

	private var sideEffects: List<SideEffect<State, CurrentState, Event>> = emptyList()

	override fun sideEffect(
		key: (CurrentState) -> Any,
		effect: suspend StateMachine<State, Event>.(CurrentState) -> Unit,
	) {
		sideEffects += SideEffect(key, effect)
	}

	override fun buildSideEffects() = sideEffects

}

data class SideEffect<State : Any, in CurrentState : State, out Event : Any>(
	val key: (CurrentState) -> Any,
	val effect: suspend StateMachine<State, Event>.(CurrentState) -> Unit,
)
