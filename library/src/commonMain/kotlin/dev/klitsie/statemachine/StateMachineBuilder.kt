package dev.klitsie.statemachine

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlin.reflect.KClass

inline fun <reified State : Any, Effect : Any, Event : Any> stateMachine(
	scope: CoroutineScope,
	initialState: State,
	started: SharingStarted = SharingStarted.WhileSubscribed(5000),
	noinline builder: StateMachineBuilder<State, State, Effect, Event>.() -> Unit,
): StateMachine<State, Effect, Event> = stateMachine(
	baseType = State::class,
	scope = scope,
	initialState = initialState,
	started = started,
	builder = builder,
)

@PublishedApi
internal fun <State : Any, Effect : Any, Event : Any> stateMachine(
	baseType: KClass<State>,
	scope: CoroutineScope,
	initialState: State,
	started: SharingStarted,
	builder: StateMachineBuilder<State, State, Effect, Event>.() -> Unit,
): StateMachine<State, Effect, Event> {
	val stateMachineDefinition = StateMachineBuilder<State, State, Effect, Event>(baseType, null)
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

data class StateMachineDefinition<State : Any, CurrentState : State, Effect : Any, Event : Any>(
	val self: StateDefinition<State, CurrentState, Effect, Event>,
	val states: Map<KClass<out CurrentState>, StateDefinition<State, CurrentState, Effect, Event>>,
	val nestedStates: Map<KClass<out CurrentState>, StateMachineDefinition<State, CurrentState, Effect, Event>>,
)

@StateDsl
class StateMachineBuilder<State : Any, CurrentState : State, Effect : Any, Event : Any>(
	private val clazz: KClass<out State>,
	private val parent: KClass<out State>?,
) :
	SideEffectBuilder<State, CurrentState, Event> by DefaultSideEffectBuilder(),
	TransitionBuilder<State, CurrentState, Effect, Event> by DefaultTransitionBuilder() {

	var states = emptyMap<KClass<out CurrentState>, StateDefinition<State, CurrentState, Effect, Event>>()
		private set

	var nestedStates = emptyMap<KClass<out CurrentState>, StateMachineDefinition<State, CurrentState, Effect, Event>>()
		private set

	inline fun <reified E : Event> onEvent(
		noinline transition: EffectHandler<CurrentState, Effect>.(state: CurrentState, event: E) -> State,
	) {
		onEvent(E::class, transition)
	}

	inline fun <reified S : CurrentState> state(
		noinline stateBuilder: StateBuilder<State, S, Effect, Event>.() -> Unit = {},
	) {
		state(S::class, stateBuilder)
	}

	inline fun <reified S : CurrentState> nestedState(
		noinline nestedStateBuilder: StateMachineBuilder<State, S, Effect, Event>.() -> Unit,
	) {
		nestedState(S::class, nestedStateBuilder)
	}

	fun <S : CurrentState> state(
		stateClass: KClass<S>,
		stateBuilder: StateBuilder<State, S, Effect, Event>.() -> Unit,
	) {
		@Suppress("UNCHECKED_CAST")
		val stateHolder = StateBuilder<State, S, Effect, Event>(stateClass, clazz).apply(stateBuilder)
			.build() as StateDefinition<State, CurrentState, Effect, Event>
		states = states
			.plus(stateClass to stateHolder)
	}

	fun <S : CurrentState> nestedState(
		stateClass: KClass<out S>,
		stateBuilder: StateMachineBuilder<State, S, Effect, Event>.() -> Unit,
	) {
		@Suppress("UNCHECKED_CAST")
		val nestedState = StateMachineBuilder<State, S, Effect, Event>(stateClass, clazz)
			.apply(stateBuilder)
			.build() as StateMachineDefinition<State, CurrentState, Effect, Event>
		this.nestedStates += nestedState.nestedStates.plus(stateClass to nestedState)
	}

	fun build(): StateMachineDefinition<State, CurrentState, Effect, Event> {
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
