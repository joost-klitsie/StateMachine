package dev.klitsie.statemachine

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlin.reflect.KClass

/**
 * Creates a [StateMachine] with the specified configuration.
 *
 * @param State The base type for all states.
 * @param Effect The type for side effects.
 * @param Event The type for events.
 * @param scope The [CoroutineScope] in which the state machine will run. This scope should ideally
 * be single-threaded to avoid concurrency issues (e.g. `viewModelScope` or a scope with
 * `Dispatchers.Main(.immediate)` or Dispatchers.Default.limitedParallelism(1)).
 * @param initialState The state to start in.
 * @param started When the state machine should start collecting (default: [SharingStarted.WhileSubscribed]).
 * @param builder A DSL builder to define states, transitions, and side effects.
 */
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

/**
 * DSL marker for the StateMachine DSL.
 */
@DslMarker
annotation class StateDsl

/**
 * Definition of a state machine, containing its own state definition and its sub-states.
 */
data class StateMachineDefinition<State : Any, CurrentState : State, Effect : Any, Event : Any>(
	val self: StateDefinition<State, CurrentState, Effect, Event>,
	val states: Map<KClass<out CurrentState>, StateDefinition<State, CurrentState, Effect, Event>>,
	val nestedStates: Map<KClass<out CurrentState>, StateMachineDefinition<State, CurrentState, Effect, Event>>,
)

/**
 * Builder for defining a state machine or a nested state.
 */
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

	/**
	 * Defines a transition for when an event of type [E] is received.
	 */
	inline fun <reified E : Event> onEvent(
		noinline transition: EffectHandler<CurrentState, Effect>.(state: CurrentState, event: E) -> State,
	) {
		onEvent(E::class, transition)
	}

	/**
	 * Defines a sub-state of type [S].
	 */
	inline fun <reified S : CurrentState> state(
		noinline stateBuilder: StateBuilder<State, S, Effect, Event>.() -> Unit = {},
	) {
		state(S::class, stateBuilder)
	}

	/**
	 * Defines a nested state of type [S] that can have its own sub-states.
	 */
	inline fun <reified S : CurrentState> nestedState(
		noinline nestedStateBuilder: StateMachineBuilder<State, S, Effect, Event>.() -> Unit,
	) {
		nestedState(S::class, nestedStateBuilder)
	}

	/**
	 * Defines a sub-state of type [S].
	 */
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

	/**
	 * Defines a nested state of type [S].
	 */
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

	/**
	 * Builds the [StateMachineDefinition].
	 */
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
