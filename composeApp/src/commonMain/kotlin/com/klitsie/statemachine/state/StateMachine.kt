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
	private val definition: StateMachineDefinition<State, Event>,
) : StateMachine<State, Event> {

	private val events = Channel<Event>()

	override val state by lazy {
		events.receiveAsFlow()
			.handleEvents()
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

	private fun Flow<Event>.handleEvents(): Flow<State> {
		var lastState: State = initialState
		return runningFold(initial = { lastState }) { state, event ->
			definition.states[state::class]
				?.transitions[event::class]
				?.transition(state, event)
				?: state
		}
			.distinctUntilChanged()
			.onEach { lastState = it }
	}

	private fun <T, R> Flow<T>.runningFold(
		initial: () -> R,
		operation: suspend (accumulator: R, value: T) -> R
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
					val stateDefinition = definition.states[state::class]
					val sideEffects = buildMap {
						stateDefinition?.sideEffects?.forEachIndexed { index, sideEffect ->
							put(
								JobKey(state::class, index, sideEffect.key(state)),
								sideEffect.effect,
							)
						}
					}

					// Cancel any old side effects we no longer want
					sideEffectJobs.minus(sideEffects.keys)
						.forEach { (_, job) -> job.cancelAndJoin() }

					// Start non-existing nested side effects, outer (parents) first
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
