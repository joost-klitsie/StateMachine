package dev.klitsie.statemachine

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.*
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * A test suite for validating the behavior of a state machine with side effects.
 *
 * This class contains test cases to ensure:
 * - Side effects are triggered as expected on state changes.
 * - Keys are used correctly to prevent duplicate side effect triggers when the state doesn't change meaningfully.
 * - Side effects are properly canceled when the state machine becomes inactive.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SideEffectTest {


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
	fun `Check side effects are cancelled when machine gets inactive after default 5 seconds`() = runTest {
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

		// Now the job should be cancelled
		assertTrue { assertNotNull(sideEffectJob).isCancelled }
	}

}
