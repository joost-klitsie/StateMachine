package com.klitsie.statemachine.state

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlin.reflect.KClass

interface StateMachine<out State : Any, in Event : Any> {

	val state: StateFlow<State>

	fun onEvent(event: Event)

}

internal class DefaultStateMachine<out State : Any, in Event : Any>(
	private val scope: CoroutineScope,
	private val initialState: State,
	private val started: SharingStarted = SharingStarted.WhileSubscribed(5000),
	private val definition: StateMachineDefinition<State, State, Event>,
) : StateMachine<State, Event> {

	private val events = Channel<Event>()

	override val state by lazy {
		events.receiveAsFlow()
			.handleEvents()
			.distinctUntilChanged()
			.handleSideEffects()
			.stateIn(
				scope = scope,
				started = started,
				initialValue = initialState
			)
	}

	override fun onEvent(event: Event) {
		scope.launch {
			events.send(event)
		}
	}

	@Suppress("AssignedValueIsNeverRead")
	private fun Flow<Event>.handleEvents(): Flow<State> {
		var lastValue: State = initialState
		return runningFold(lastValue) { state, event ->
			state.getSelfAndAncestors()
				.firstNotNullOfOrNull { definition -> definition.transitions[event::class] }
				?.transition(state, event)
				?: state
		}
			.onEach { lastValue = it }
	}

	/**
	 * Collect the definition of the state and all nested parents
	 */
	private fun State.getSelfAndAncestors(): List<StateDefinition<State, in State, Event>> = buildList {
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

		@Suppress("AssignedValueIsNeverRead")
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
