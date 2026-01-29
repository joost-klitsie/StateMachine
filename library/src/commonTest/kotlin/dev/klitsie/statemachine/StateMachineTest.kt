package dev.klitsie.statemachine

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds

/**
 * Unit tests for verifying the functionality of a state machine implementation.
 *
 * This test class ensures that various behaviors of the state machine, such as
 * state transitions, side effects, and event handling, function as expected.
 * The state machine under test operates with the [TestState], [TestEffect], and
 * [TestEvent] sealed interfaces as its core logic components.
 *
 * The tests use structured concurrency to simulate state transitions and
 * perform assertions on the expected outcomes.
 *
 * Test cases included:
 * 1. Verifying no actions occur if a new state is the same as the previous state.
 * 2. Confirming that the last declared definition of a state takes precedence over earlier definitions.
 * 3. Ensuring the state machine can start with an undefined state configuration.
 * 4. Checking transitions to undefined states and confirming behavior when events are sent in these states.
 * 5. Verifying that the state machine resumes from its last state and side effects restart upon restarting.
 *
 * The tests make use of coroutines and time manipulation for simulating asynchronous state transitions
 * and timing behavior.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class StateMachineTest {

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


}
