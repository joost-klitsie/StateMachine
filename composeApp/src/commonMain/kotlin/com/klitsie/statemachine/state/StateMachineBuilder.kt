package com.klitsie.statemachine.state

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlin.reflect.KClass

inline fun <reified State : Any, Event : Any> stateMachine(
	scope: CoroutineScope,
	initialState: State,
	started: SharingStarted = SharingStarted.WhileSubscribed(5000),
	noinline builder: StateMachineBuilder<State, State, Event>.() -> Unit,
): StateMachine<State, Event> = stateMachine(
	baseType = State::class,
	scope = scope,
	initialState = initialState,
	started = started,
	builder = builder,
)

fun <State : Any, Event : Any> stateMachine(
	baseType: KClass<State>,
	scope: CoroutineScope,
	initialState: State,
	started: SharingStarted = SharingStarted.WhileSubscribed(5000),
	builder: StateMachineBuilder<State, State, Event>.() -> Unit,
): StateMachine<State, Event> {
	val stateMachineDefinition = StateMachineBuilder<State, State, Event>(baseType, null)
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

data class StateMachineDefinition<State : Any, CurrentState : State, Event : Any>(
	val self: StateDefinition<State, CurrentState, Event>,
	val states: Map<KClass<out CurrentState>, StateDefinition<State, CurrentState, Event>>,
	val nestedStates: Map<KClass<out CurrentState>, StateMachineDefinition<State, CurrentState, Event>>,
)

@StateDsl
class StateMachineBuilder<State : Any, CurrentState : State, Event : Any>(
	private val clazz: KClass<out State>,
	private val parent: KClass<out State>?,
) :
	SideEffectBuilder<State, CurrentState, Event> by DefaultSideEffectBuilder(),
	TransitionBuilder<State, CurrentState, Event> by DefaultTransitionBuilder() {

	var states = emptyMap<KClass<out CurrentState>, StateDefinition<State, CurrentState, Event>>()
		private set

	var nestedStates = emptyMap<KClass<out CurrentState>, StateMachineDefinition<State, CurrentState, Event>>()
		private set

	inline fun <reified E : Event> onEvent(
		noinline transition: (CurrentState, E) -> State,
	) {
		onEvent(E::class, transition)
	}

	inline fun <reified S : CurrentState> state(
		noinline stateBuilder: StateBuilder<State, S, Event>.() -> Unit = {},
	) {
		state(S::class, stateBuilder)
	}

	inline fun <reified S : CurrentState> nestedState(
		noinline nestedStateBuilder: StateMachineBuilder<State, S, Event>.() -> Unit,
	) {
		nestedState(S::class, nestedStateBuilder)
	}

	fun <S : CurrentState> state(
		stateClass: KClass<S>,
		stateBuilder: StateBuilder<State, S, Event>.() -> Unit,
	) {
		@Suppress("UNCHECKED_CAST")
		val stateHolder = StateBuilder<State, S, Event>(stateClass, clazz).apply(stateBuilder)
			.build() as StateDefinition<State, CurrentState, Event>
		states = states
			.plus(stateClass to stateHolder)
	}

	fun <S : CurrentState> nestedState(
		stateClass: KClass<out S>,
		stateBuilder: StateMachineBuilder<State, S, Event>.() -> Unit,
	) {
		@Suppress("UNCHECKED_CAST")
		val nestedState = StateMachineBuilder<State, S, Event>(stateClass, clazz)
			.apply(stateBuilder)
			.build() as StateMachineDefinition<State, CurrentState, Event>
		this.nestedStates += nestedState.nestedStates.plus(stateClass to nestedState)
	}

	fun build(): StateMachineDefinition<State, CurrentState, Event> {
		return StateMachineDefinition(
			self = StateDefinition(
				clazz = clazz,
				parent = parent,
				sideEffects = buildSideEffects(),
				transitions = buildTransitions(),
			),
			states = states + buildMap {
				nestedStates.values.forEach { nestedState ->
					putAll(nestedState.states)
				}
			},
			nestedStates = nestedStates + buildMap {
				nestedStates.values.forEach { nestedState ->
					putAll(nestedState.nestedStates)
				}
			},
		)
	}

}

