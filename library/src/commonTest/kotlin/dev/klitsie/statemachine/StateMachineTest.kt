package dev.klitsie.statemachine

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.*
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class StateMachineTest {

	@Test
	fun `Check transitioning to different states`() = runTest {
		val machine = stateMachine<TestState, TestEffect, TestEvent>(
			scope = backgroundScope,
			initialState = TestState.StateA("I am a value"),
		) {
			state<TestState.StateA> {
				onEvent<TestEvent.MoveForward> { state, _ ->
					TestState.NestedState.NestedStateA(nestedValue = state.value)
				}
			}
			nestedState<TestState.NestedState> {
				state<TestState.NestedState.NestedStateA> {
					onEvent<TestEvent.MoveForward> { state, _ ->
						TestState.NestedState.NestedStateB(nestedValue = state.nestedValue)
					}
				}
				state<TestState.NestedState.NestedStateB> {
					onEvent<TestEvent.MoveForward> { state, _ ->
						TestState.StateB(value = state.nestedValue)
					}
				}
			}
			state<TestState.StateB>()
		}

		val expected = listOf(
			TestState.StateA(value = "I am a value"),
			TestState.NestedState.NestedStateA(nestedValue = "I am a value"),
			TestState.NestedState.NestedStateB(nestedValue = "I am a value"),
			TestState.StateB(value = "I am a value"),
		)

		val actual = backgroundScope.async {
			machine.take(expected.count()).toList()
		}

		advanceTimeBy(1.milliseconds)
		machine.send(TestEvent.MoveForward)
		advanceTimeBy(1.milliseconds)
		machine.send(TestEvent.MoveForward)
		advanceTimeBy(1.milliseconds)
		machine.send(TestEvent.MoveForward)

		assertContentEquals(expected, actual.await())
	}

	@Test
	fun `Check transitioning to different states from side effects`() = runTest {
		val machine = stateMachine<TestState, TestEffect, TestEvent>(
			scope = backgroundScope,
			initialState = TestState.StateA("I am a value"),
		) {
			state<TestState.StateA> {
				sideEffect { send(TestEvent.MoveForward) }
				onEvent<TestEvent.MoveForward> { state, _ ->
					TestState.NestedState.NestedStateA(nestedValue = state.value)
				}
			}
			nestedState<TestState.NestedState> {
				sideEffect { send(TestEvent.MoveForward) }
				state<TestState.NestedState.NestedStateA> {
					onEvent<TestEvent.MoveForward> { state, _ ->
						TestState.NestedState.NestedStateB(nestedValue = state.nestedValue)
					}
				}
				state<TestState.NestedState.NestedStateB> {
					onEvent<TestEvent.MoveForward> { state, _ ->
						TestState.StateB(value = state.nestedValue)
					}
				}
			}
			state<TestState.StateB>()
		}

		val expected = listOf(
			TestState.StateA(value = "I am a value"),
			TestState.NestedState.NestedStateA(nestedValue = "I am a value"),
			TestState.NestedState.NestedStateB(nestedValue = "I am a value"),
			TestState.StateB(value = "I am a value"),
		)

		val actual = backgroundScope.async {
			machine.take(expected.count()).toList()
		}

		assertContentEquals(expected, actual.await())
	}

	@Test
	fun `Check children will handle events before parents`() = runTest {
		val machine = stateMachine<TestState, TestEffect, TestEvent>(
			scope = backgroundScope,
			initialState = TestState.NestedState.NestedStateA("I am a value"),
		) {
			onEvent<TestEvent.MoveForward> { state, _ ->
				TestState.StateA(
					value = when (state) {
						is TestState.StateA -> state.value
						is TestState.StateB -> state.value
						is TestState.StateC -> state.value
						is TestState.NestedState -> state.nestedValue
					},
				)
			}
			nestedState<TestState.NestedState> {
				onEvent<TestEvent.MoveForward> { state, _ ->
					TestState.StateB(value = state.nestedValue)
				}
				state<TestState.NestedState.NestedStateA> {
					onEvent<TestEvent.MoveForward> { state, _ ->
						TestState.StateC(value = state.nestedValue)
					}
				}
				state<TestState.NestedState.NestedStateB>()
			}
			state<TestState.StateA>()
			state<TestState.StateB>()
			state<TestState.StateC>()
		}

		val expected = listOf(
			TestState.NestedState.NestedStateA(nestedValue = "I am a value"),
			TestState.StateC(value = "I am a value"),
		)

		val actual = backgroundScope.async {
			machine.take(expected.count()).toList()
		}

		advanceTimeBy(1.milliseconds)
		machine.send(TestEvent.MoveForward)

		assertContentEquals(expected, actual.await())
	}

	@Test
	fun `Check parents will handle events before state machine`() = runTest {
		val machine = stateMachine<TestState, TestEffect, TestEvent>(
			scope = backgroundScope,
			initialState = TestState.NestedState.NestedStateA("I am a value"),
		) {
			onEvent<TestEvent.MoveForward> { state, _ ->
				TestState.StateA(
					value = when (state) {
						is TestState.StateA -> state.value
						is TestState.StateB -> state.value
						is TestState.StateC -> state.value
						is TestState.NestedState -> state.nestedValue
					},
				)
			}
			nestedState<TestState.NestedState> {
				onEvent<TestEvent.MoveForward> { state, _ ->
					TestState.StateB(value = state.nestedValue)
				}
				state<TestState.NestedState.NestedStateA>()
				state<TestState.NestedState.NestedStateB>()
			}
			state<TestState.StateA>()
			state<TestState.StateB>()
			state<TestState.StateC>()
		}

		val expected = listOf(
			TestState.NestedState.NestedStateA(nestedValue = "I am a value"),
			TestState.StateB(value = "I am a value"),
		)

		val actual = backgroundScope.async {
			machine.take(expected.count()).toList()
		}

		advanceTimeBy(1.milliseconds)
		machine.send(TestEvent.MoveForward)

		assertContentEquals(expected, actual.await())
	}

	@Test
	fun `Check parents will handle events if no defined state handles it`() = runTest {
		val machine = stateMachine<TestState, TestEffect, TestEvent>(
			scope = backgroundScope,
			initialState = TestState.NestedState.NestedStateA("I am a value"),
		) {
			onEvent<TestEvent.MoveForward> { state, _ ->
				TestState.StateA(
					value = when (state) {
						is TestState.StateA -> state.value
						is TestState.StateB -> state.value
						is TestState.StateC -> state.value
						is TestState.NestedState -> state.nestedValue
					},
				)
			}
			nestedState<TestState.NestedState> {
				state<TestState.NestedState.NestedStateA>()
				state<TestState.NestedState.NestedStateB>()
			}
			state<TestState.StateA>()
			state<TestState.StateB>()
			state<TestState.StateC>()
		}

		val expected = listOf(
			TestState.NestedState.NestedStateA(nestedValue = "I am a value"),
			TestState.StateA(value = "I am a value"),
		)

		val actual = backgroundScope.async {
			machine.take(expected.count()).toList()
		}

		advanceTimeBy(1.milliseconds)
		machine.send(TestEvent.MoveForward)

		assertContentEquals(expected, actual.await())
	}

	@Test
	fun `Check we ignore unhandled events`() = runTest {
		val machine = stateMachine<TestState, TestEffect, TestEvent>(
			scope = backgroundScope,
			initialState = TestState.NestedState.NestedStateA("I am a value"),
		) {
			nestedState<TestState.NestedState> {
				state<TestState.NestedState.NestedStateA>()
				state<TestState.NestedState.NestedStateB>()
			}
			state<TestState.StateA>()
			state<TestState.StateB>()
			state<TestState.StateC>()
		}


		advanceTimeBy(1.milliseconds)
		machine.send(TestEvent.MoveForward)
		advanceTimeBy(1.milliseconds)

		assertEquals(
			TestState.NestedState.NestedStateA(nestedValue = "I am a value"),
			machine.value,
		)
	}

	@Test
	fun `Check we can trigger effects and we can trigger multiple`() = runTest {
		val machine = stateMachine<TestState, TestEffect, TestEvent>(
			scope = backgroundScope,
			initialState = TestState.StateA("I am a value"),
			started = SharingStarted.Eagerly,
		) {
			state<TestState.StateA> {
				onEvent<TestEvent.Close> { _, _ ->
					trigger(TestEffect.Close)
					trigger(TestEffect.Close)
				}
			}
		}
		val expectedEffects = listOf(TestEffect.Close, TestEffect.Close)
		val actualEffects = backgroundScope.async { machine.effect.take(expectedEffects.count()).toList() }

		machine.send(TestEvent.Close)
		advanceUntilIdle()

		assertContentEquals(expectedEffects, actualEffects.await())

		assertEquals(
			TestState.StateA(value = "I am a value"),
			machine.value,
		)
	}

	@Test
	fun `Check we can trigger effects and update state`() = runTest {
		val machine = stateMachine<TestState, TestEffect, TestEvent>(
			scope = backgroundScope,
			initialState = TestState.StateA("I am a value"),
			started = SharingStarted.Eagerly,
		) {
			state<TestState.StateA> {
				onEvent<TestEvent.Close> { state, _ ->
					trigger(TestEffect.Close)
					TestState.StateB(value = state.value)
				}
			}
		}

		machine.send(TestEvent.Close)
		advanceUntilIdle()

		assertEquals(TestEffect.Close, machine.effect.first())

		assertEquals(
			TestState.StateB(value = "I am a value"),
			machine.value,
		)
	}

	@Test
	fun `Check work is cancelled when machine gets inactive after default 5 seconds`() = runTest {
		var sideEffectJob: Job? = null

		val machine = stateMachine<TestState, TestEffect, TestEvent>(
			scope = backgroundScope,
			initialState = TestState.StateA("I am a value"),
			started = SharingStarted.WhileSubscribed(5.seconds),
		) {
			state<TestState.StateA> {
				sideEffect {
					coroutineScope {
						sideEffectJob = launch { delay(Long.MAX_VALUE) }
					}
				}
			}
		}

		advanceUntilIdle()
		assertNull(sideEffectJob)

		val observeJob = backgroundScope.launch { machine.collect() }
		advanceTimeBy(1.milliseconds)

		assertTrue { assertNotNull(sideEffectJob).isActive }

		observeJob.cancel()
		advanceTimeBy(4.seconds)

		// Job should still be active
		assertTrue { assertNotNull(sideEffectJob).isActive }

		advanceTimeBy(2.seconds)

		// Now job should be cancelled
		assertTrue { assertNotNull(sideEffectJob).isCancelled }
	}

	@Test
	fun `Check state machine picks up at last state after restarting and jobs restart as well`() = runTest {
		var sideEffectCount = 0

		val machine = stateMachine<TestState, TestEffect, TestEvent>(
			scope = backgroundScope,
			initialState = TestState.StateA("I am a value"),
			started = SharingStarted.WhileSubscribed(),
		) {
			state<TestState.StateA> {
				onEvent<TestEvent.MoveForward> { state, _ ->
					TestState.StateB(value = state.value)
				}
			}
			state<TestState.StateB> {
				sideEffect {
					sideEffectCount++
				}
			}
		}

		val observeJob = backgroundScope.launch { machine.collect() }
		machine.send(TestEvent.MoveForward)

		advanceTimeBy(1.milliseconds)
		assertEquals(1, sideEffectCount)
		assertEquals(TestState.StateB("I am a value"), machine.value)

		observeJob.cancel()
		// Check we keep state
		advanceTimeBy(1.milliseconds)
		assertEquals(TestState.StateB("I am a value"), machine.value)

		// Restart work
		backgroundScope.launch { machine.collect() }
		advanceTimeBy(1.milliseconds)

		// Check we kept the state and launched the side effect again
		assertEquals(2, sideEffectCount)
		assertEquals(TestState.StateB("I am a value"), machine.value)
	}

	@Test
	fun `Check we suspend events while state machine is inactive`() = runTest {
		val machine = stateMachine<TestState, TestEffect, TestEvent>(
			scope = backgroundScope,
			initialState = TestState.StateA("I am a value"),
			started = SharingStarted.WhileSubscribed(),
		) {
			state<TestState.StateA> {
				onEvent<TestEvent.MoveForward> { state, _ ->
					TestState.StateB(value = state.value)
				}
			}
			state<TestState.StateB> {
				onEvent<TestEvent.Append> { state, event ->
					state.copy(value = state.value + event.value)
				}
			}
		}

		machine.send(TestEvent.MoveForward)
		advanceTimeBy(1.milliseconds)

		// Check we didn't change state yet as collection didn't start
		assertEquals(TestState.StateA("I am a value"), machine.value)
		val observeJob = backgroundScope.launch { machine.collect() }
		advanceTimeBy(1.milliseconds)

		// Check we moved state after collection started
		assertEquals(TestState.StateB("I am a value"), machine.value)

		observeJob.cancel()
		// Check we keep state
		advanceTimeBy(1.milliseconds)
		assertEquals(TestState.StateB("I am a value"), machine.value)

		// Send a bunch of events
		machine.send(TestEvent.Append("-"))
		machine.send(TestEvent.Append("-"))
		machine.send(TestEvent.Append("-"))
		advanceTimeBy(1.milliseconds)
		machine.send(TestEvent.Append("-"))
		advanceTimeBy(1.milliseconds)

		// Make sure nothing happened yet
		assertEquals(TestState.StateB("I am a value"), machine.value)

		// Restart work
		backgroundScope.launch { machine.collect() }
		advanceTimeBy(1.milliseconds)

		// Check we kept the events to update after we started to collect
		assertEquals(TestState.StateB("I am a value----"), machine.value)
	}

	@Test
	fun `Check nothing will really happen if new state is the same as last state`() = runTest {
		var sideEffectCounter = 0

		val machine = stateMachine<TestState, TestEffect, TestEvent>(
			scope = backgroundScope,
			initialState = TestState.StateA("I am a value"),
			started = SharingStarted.Eagerly,
		) {
			state<TestState.StateA> {
				sideEffect {
					sideEffectCounter++
				}

				onEvent<TestEvent.Append> { state, event ->
					state.copy(value = state.value + event.value)
				}
			}
		}

		val expectedStates = listOf(TestState.StateA("I am a value"))
		val actualStates = mutableListOf<TestState>()

		backgroundScope.launch { machine.toList(actualStates) }

		advanceTimeBy(1.milliseconds)
		machine.send(TestEvent.Append(""))
		advanceTimeBy(1.milliseconds)

		assertContentEquals(expectedStates, actualStates)
		assertEquals(1, sideEffectCounter)
	}

	@Test
	fun `Check all side effects will run on every state change if no key is given`() = runTest {
		var machineSideEffectCounter = 0
		var stateASideEffectCounter = 0
		var nestedSideEffectCounter = 0
		var nestedStateASideEffectCounter = 0
		var nestedStateBSideEffectCounter = 0

		val machine = stateMachine<TestState, TestEffect, TestEvent>(
			scope = backgroundScope,
			initialState = TestState.StateA("I am a value"),
			started = SharingStarted.Eagerly,
		) {
			sideEffect {
				machineSideEffectCounter++
			}
			state<TestState.StateA> {
				sideEffect {
					stateASideEffectCounter++
				}

				onEvent<TestEvent.MoveForward> { state, _ ->
					TestState.NestedState.NestedStateA(nestedValue = state.value)
				}
			}
			nestedState<TestState.NestedState> {
				sideEffect {
					nestedSideEffectCounter++
				}

				state<TestState.NestedState.NestedStateA> {
					sideEffect {
						nestedStateASideEffectCounter++
					}

					onEvent<TestEvent.MoveForward> { state, _ ->
						TestState.NestedState.NestedStateB(nestedValue = state.nestedValue)
					}
				}
				state<TestState.NestedState.NestedStateB> {
					sideEffect {
						nestedStateBSideEffectCounter++
					}
				}
			}
		}

		val expectedStates = listOf(
			TestState.StateA("I am a value"),
			TestState.NestedState.NestedStateA("I am a value"),
			TestState.NestedState.NestedStateB("I am a value"),
		)

		val actualStates = backgroundScope.async { machine.take(expectedStates.count()).toList() }

		machine.send(TestEvent.MoveForward)
		advanceTimeBy(1.milliseconds)
		machine.send(TestEvent.MoveForward)
		advanceTimeBy(1.milliseconds)

		assertContentEquals(expectedStates, actualStates.await())
		assertEquals(3, machineSideEffectCounter)
		assertEquals(1, stateASideEffectCounter)
		assertEquals(2, nestedSideEffectCounter)
		assertEquals(1, nestedStateASideEffectCounter)
		assertEquals(1, nestedStateBSideEffectCounter)
	}

	@Test
	fun `Check side effects will not run multiple times if key doesn't change`() = runTest {
		var machineSideEffectCounter = 0
		var stateASideEffectCounter = 0
		var nestedSideEffectCounter = 0
		var nestedStateASideEffectCounter = 0
		var nestedStateBSideEffectCounter = 0

		val machine = stateMachine<TestState, TestEffect, TestEvent>(
			scope = backgroundScope,
			initialState = TestState.StateA("I am a value"),
			started = SharingStarted.Eagerly,
		) {
			sideEffect(
				key = { state ->
					// Do a stupid comparison to just show we can calculate the keys
					when (state) {
						is TestState.NestedState -> state.nestedValue
						is TestState.StateA -> state.value
						is TestState.StateB -> state.value
						is TestState.StateC -> state.value
					}
				},
			) {
				machineSideEffectCounter++
			}
			state<TestState.StateA> {
				sideEffect(key = {}) { // Can simply use Unit
					stateASideEffectCounter++
				}

				onEvent<TestEvent.MoveForward> { state, _ ->
					TestState.NestedState.NestedStateA(nestedValue = state.value)
				}
			}
			nestedState<TestState.NestedState> {
				sideEffect(key = {}) { // Can simply use Unit to never run again
					nestedSideEffectCounter++
				}

				state<TestState.NestedState.NestedStateA> {
					sideEffect(key = {}) { // Can simply use Unit
						nestedStateASideEffectCounter++
					}

					onEvent<TestEvent.MoveForward> { state, _ ->
						TestState.NestedState.NestedStateB(nestedValue = state.nestedValue)
					}
				}
				state<TestState.NestedState.NestedStateB> {
					sideEffect(key = { state -> state.nestedValue }) {
						nestedStateBSideEffectCounter++
					}

					onEvent<TestEvent.Append> { state, event ->
						state.copy(nestedValue = state.nestedValue + event.value)
					}
				}
			}
		}

		val expectedStates = listOf(
			TestState.StateA("I am a value"),
			TestState.NestedState.NestedStateA("I am a value"),
			TestState.NestedState.NestedStateB("I am a value"),
			TestState.NestedState.NestedStateB("I am a value!!!"),
		)

		val actualStates = backgroundScope.async { machine.take(expectedStates.count()).toList() }

		machine.send(TestEvent.MoveForward)
		advanceTimeBy(1.milliseconds)
		machine.send(TestEvent.MoveForward)
		advanceTimeBy(1.milliseconds)
		machine.send(TestEvent.Append(""))
		advanceTimeBy(1.milliseconds)
		machine.send(TestEvent.Append("!!!"))
		advanceTimeBy(1.milliseconds)

		assertContentEquals(expectedStates, actualStates.await())
		assertEquals(2, machineSideEffectCounter)
		assertEquals(1, stateASideEffectCounter)
		assertEquals(1, nestedSideEffectCounter)
		assertEquals(1, nestedStateASideEffectCounter)
		assertEquals(2, nestedStateBSideEffectCounter)
	}

	@Test
	fun `Check last state definition takes precedent`() = runTest {
		var firstCounter = 0
		var secondCounter = 0

		stateMachine<TestState, TestEffect, TestEvent>(
			scope = backgroundScope,
			initialState = TestState.StateA("I am a value"),
			started = SharingStarted.Eagerly,
		) {
			state<TestState.StateA> {
				sideEffect { // Can simply use Unit
					firstCounter++
				}
			}

			state<TestState.StateA> {
				sideEffect { // Can simply use Unit
					secondCounter++
				}
			}
		}

		advanceTimeBy(1.milliseconds)

		assertEquals(0, firstCounter)
		assertEquals(1, secondCounter)
	}

	@Test
	fun `Check we can start with an undefined state`() = runTest {
		val machine = stateMachine<TestState, TestEffect, TestEvent>(
			scope = backgroundScope,
			initialState = TestState.StateA("I am a value"),
			started = SharingStarted.Eagerly,
		) {}

		assertEquals(TestState.StateA("I am a value"), machine.value)
	}

	@Test
	fun `Check we can still transition to undefined states`() = runTest {
		val machine = stateMachine<TestState, TestEffect, TestEvent>(
			scope = backgroundScope,
			initialState = TestState.StateA("I am a value"),
			started = SharingStarted.Eagerly,
		) {
			state<TestState.StateA> {
				onEvent<TestEvent.MoveForward> { state, _ ->
					TestState.StateB(state.value)
				}
			}
		}

		machine.send(TestEvent.MoveForward)
		advanceTimeBy(1.milliseconds)

		assertEquals(TestState.StateB("I am a value"), machine.value)

		// Nothing will happen if we send other events
		machine.send(TestEvent.MoveForward)
		advanceTimeBy(1.milliseconds)

		assertEquals(TestState.StateB("I am a value"), machine.value)
	}

	private sealed interface TestState {

		data class StateA(val value: String) : TestState
		data class StateB(val value: String) : TestState
		data class StateC(val value: String) : TestState

		sealed interface NestedState : TestState {

			val nestedValue: String

			data class NestedStateA(override val nestedValue: String) : NestedState
			data class NestedStateB(override val nestedValue: String) : NestedState

		}

	}

	private sealed interface TestEffect {

		data object Close : TestEffect

	}

	private sealed interface TestEvent {

		data object MoveForward : TestEvent
		data class Append(val value: String) : TestEvent
		data object Close : TestEvent

	}

}
