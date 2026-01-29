package dev.klitsie.statemachine

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds

/**
 * A test class for verifying the behavior of the state machine that
 * transitions between different states based on events and optionally
 * handles side effects during state changes. This test suite evaluates
 * multiple scenarios to ensure correctness of state transitions, event
 * handling precedence, and handling of unhandled or deferred events.
 *
 * The tested state machine supports the following features:
 *
 * - Transitioning between states based on events.
 * - Handling nested states within the parent states.
 * - Processing side effects within states.
 * - Defining precedence between parent and child states.
 * - Ignoring unhandled events unless explicitly handled.
 * - Suspending event handling when the state machine is inactive.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EventTest {

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

}
