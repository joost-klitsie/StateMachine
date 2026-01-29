package dev.klitsie.statemachine

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlin.reflect.KClass

/**
 * A State Machine that manages [State], handles [Event]s to transition between states,
 * and triggers [Effect]s as side outputs.
 *
 * It extends [StateFlow], providing the current state as a flow.
 *
 * @param State The base type for all possible states in this machine.
 * @param Effect The type for side effects triggered by transitions or states.
 * @param Event The type for events that can be sent to the state machine.
 */
@OptIn(ExperimentalForInheritanceCoroutinesApi::class)
interface StateMachine<out State : Any, Effect : Any, in Event : Any> : StateFlow<State> {

	/**
	 * Sends an [event] to the state machine to trigger a transition.
	 */
	fun send(event: Event)

	/**
	 * Collects and consumes [Effect]s triggered by the state machine. Effects will be consumed if
	 * there is at least one consumer. There is no replay cache, so effects will be consumed as they are produced.
	 * If there is no consumer, effects will be queued until there is one. All concurrent consumers will receive
	 * all effects.
	 *
	 * @param block A callback that will be invoked for each effect.
	 */
	suspend fun consumeEffects(block: (Effect) -> Unit)

}

internal class DefaultStateMachine<out State : Any, Effect : Any, in Event : Any>(
	private val scope: CoroutineScope,
	private val initialState: State,
	private val started: SharingStarted = SharingStarted.WhileSubscribed(5000),
	private val definition: StateMachineDefinition<State, State, Effect, Event>,
) : StateMachine<State, Effect, Event>, EffectHandler<State, Effect> {

	private val events = Channel<Event>()

	private val state by lazy {
		events.receiveAsFlow()
			.handleEvents()
			.distinctUntilChanged()
			.handleSideEffects()
			.stateIn(
				scope = scope,
				started = started,
				initialValue = initialState,
			)
	}

	private val effects = MutableStateFlow(emptyList<Effect>())

	override val value: State by state::value
	override suspend fun collect(collector: FlowCollector<State>) = state.collect(collector)
	override val replayCache: List<State> by state::replayCache

	override fun send(event: Event) {
		scope.launch {
			events.send(event)
		}
	}

	override suspend fun consumeEffects(block: (effect: Effect) -> Unit) {
		// Make sure we are using the same context as the parent to resolve most concurrency issues in the horrid case
		// consumeEffects is called off the main thread.
		withContext(scope.coroutineContext.minusKey(Job.Key)) {
			effects.collect { currentEffects ->
				val firstEffect = currentEffects.firstOrNull() ?: return@collect

				// Launch so we handle events for all consumers.
				launch {
					block(firstEffect)
					effects.update { old ->
						// Only drop if effects didn't change in the meantime. In this way, the first consumer will
						// actually drop the effect while other consumers will still receive the effect
						when (old) {
							currentEffects -> old.drop(1)
							else -> old
						}
					}
					yield()
				}
			}
		}
	}

	override fun trigger(effect: Effect) = value.also {
		effects.update { it + effect }
	}

	private fun Flow<Event>.handleEvents(): Flow<State> {
		var lastState: State = initialState
		return runningFold(initial = { lastState }) { state, event ->
			state.getSelfAndAncestors()
				.firstNotNullOfOrNull { definition -> definition.transitions[event::class] }
				?.transition(this@DefaultStateMachine, state, event)
				?: state
		}
			.onEach { lastState = it }
	}

	/**
	 * Same as runningFold, but we can pass the initial state inside a lambda. This will help us when the collection
	 * is cancelled and restarted, to make sure we continue where we left off, instead of resetting totally.
	 */
	private fun <T, R> Flow<T>.runningFold(
		initial: () -> R,
		operation: suspend (accumulator: R, value: T) -> R,
	): Flow<R> {
		return flow {
			var accumulator: R = initial()
			emit(accumulator)
			collect { value ->
				accumulator = operation(accumulator, value)
				emit(accumulator)
			}
		}
	}

	/**
	 * Collect the definition of the state and all nested parents
	 */
	private fun State.getSelfAndAncestors(): List<StateDefinition<State, in State, Effect, Event>> = buildList {
		definition.states[this@getSelfAndAncestors::class]?.let { state ->
			add(state)
			var clazz = state.parent
			while (clazz != null) {
				clazz = definition.nestedStates[clazz]?.let { nestedState ->
					add(nestedState.self)
					nestedState.self.parent
				}
			}
		}

		// Finally add the base definition of the state machine to the list
		add(definition.self)
	}

	private fun Flow<State>.handleSideEffects(): Flow<State> = SideEffectsHandler(this)

	private inner class SideEffectsHandler(
		private val upstream: Flow<State>,
	) : Flow<State> {

		override suspend fun collect(collector: FlowCollector<State>) {
			coroutineScope {
				var sideEffectJobs = mapOf<JobKey<State>, Job>()

				upstream.collect { state ->

					// Collect all side effects we wish to run right now
					// We will make sure these are ordered root first and child last
					val sideEffects = buildMap {
						state.getSelfAndAncestors().reversed().forEach { stateDefinition ->
							stateDefinition.sideEffects.forEachIndexed { index, sideEffect ->
								put(
									JobKey(stateDefinition.clazz, index, sideEffect.key(state)),
									sideEffect.effect,
								)
							}
						}
					}

					// Cancel any old nested side effects, youngest jobs first
					// In this case we need to reverse the entries of the existing
					// Side effect jobs because younger side effects are added to the
					// end of the map
					sideEffectJobs.minus(sideEffects.keys)
						.values
						.reversed()
						.forEach { it.cancelAndJoin() }

					// Start non-existing nested side effects, outer (parents) first
					// We store these in the sideEffectJobs. Our sideEffects
					// Are already correctly ordered, so here we can use a simple
					// mapValues operation to either return an existing job or
					// run a new one in the right order
					sideEffectJobs = sideEffects.mapValues { (jobKey, sideEffect) ->
						sideEffectJobs.getOrElse(jobKey) {
							launch { sideEffect(this@DefaultStateMachine, state) }
						}
					}

					// Continue the flow like nothing happened :)
					collector.emit(state)
				}
			}
		}

	}

	private data class JobKey<State : Any>(
		val clazz: KClass<out State>,
		val index: Int,
		val key: Any,
	)

}
