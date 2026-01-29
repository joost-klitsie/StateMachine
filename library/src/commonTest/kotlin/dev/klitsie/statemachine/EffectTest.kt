package dev.klitsie.statemachine

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * A test suite for validating the behavior of a state machine in regard to handling
 * and consuming side effects across multiple scenarios.
 *
 * This class specifically tests the `stateMachine` implementation to ensure that:
 * - Effects can be triggered and collected correctly, even when triggered in bulk or from
 *   multiple threads.
 * - Effects are consumed only once across different coroutine scopes.
 * - Effects handling can resume properly after cancellation.
 * - It is possible to trigger effects and update states simultaneously.
 *
 * Each test validates specific behavior related to the state machine's effect handling
 * capabilities by simulating various edge cases and verifying state transitions and effect consumption.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EffectTest {

	@Test
	fun `Check we can trigger multiple effects and can collect them from from multiple threads`() = runTest {
		val expectedEffects = List(1000) { TestEffect.Count(it + 1) }
		val machine = stateMachine<TestState, TestEffect, TestEvent>(
			scope = backgroundScope,
			initialState = TestState.StateA("I am a value"),
			started = SharingStarted.Eagerly,
		) {
			state<TestState.StateA> {
				onEvent<TestEvent.MoveForward> { _, _ ->
					repeat(expectedEffects.size) {
						trigger(TestEffect.Count(it + 1))
					}
					trigger(TestEffect.Close)
				}
			}
		}

		val consumedEventsAndJobs = List(10) { mutableListOf<TestEffect>() }
			.associateWith { actualEffects ->
				backgroundScope.launch {
					withContext(Dispatchers.Default) {
						machine.consumeEffects { effect ->
							when (effect) {
								TestEffect.Close -> cancel()
								is TestEffect.Count -> actualEffects.add(effect)
							}
						}
					}
				}
			}

		// Make sure consuming started before sending event
		advanceTimeBy(10)
		machine.send(TestEvent.MoveForward)

		consumedEventsAndJobs.values.joinAll()

		consumedEventsAndJobs.keys.forEach { actualEffects ->
			assertContentEquals(expectedEffects, actualEffects)
		}

		assertEquals(
			TestState.StateA(value = "I am a value"),
			machine.value,
		)
	}

	@Test
	fun `Check we correctly consume effects once`() = runTest {
		val machine = stateMachine<TestState, TestEffect, TestEvent>(
			scope = backgroundScope,
			initialState = TestState.StateA("I am a value"),
			started = SharingStarted.Eagerly,
		) {
			state<TestState.StateA> {
				onEvent<TestEvent.Close> { _, _ ->
					trigger(TestEffect.Close)
				}
			}
		}

		var effectBeforeCancel: TestEffect? = null

		val job1 = backgroundScope.launch {
			machine.consumeEffects {
				effectBeforeCancel = it
				cancel()
			}
		}
		var effectAfterCancel: TestEffect? = null
		backgroundScope.launch {
			advanceUntilIdle()
			job1.join()
			advanceTimeBy(1)
			machine.consumeEffects {
				effectAfterCancel = it
			}
		}

		machine.send(TestEvent.Close)
		advanceTimeBy(4)

		assertEquals(TestEffect.Close, effectBeforeCancel)
		assertNull(effectAfterCancel)

		assertEquals(
			TestState.StateA(value = "I am a value"),
			machine.value,
		)
	}

	@Test
	fun `Check we can continue consuming effects after cancellation`() = runTest {
		val machine = stateMachine<TestState, TestEffect, TestEvent>(
			scope = backgroundScope,
			initialState = TestState.StateA("I am a value"),
			started = SharingStarted.Eagerly,
		) {
			state<TestState.StateA> {
				onEvent<TestEvent.Close> { _, _ ->
					trigger(TestEffect.Count(1))
					trigger(TestEffect.Count(2))
				}
			}
		}

		var effectBeforeCancel: TestEffect? = null

		val job1 = backgroundScope.launch {
			machine.consumeEffects {
				effectBeforeCancel = it
				cancel()
			}
		}
		var effectAfterCancel: TestEffect? = null
		backgroundScope.launch {
			advanceUntilIdle()
			job1.join()
			advanceTimeBy(1)
			machine.consumeEffects {
				effectAfterCancel = it
			}
		}

		machine.send(TestEvent.Close)
		advanceTimeBy(4)

		assertEquals(TestEffect.Count(1), effectBeforeCancel)
		assertEquals(TestEffect.Count(2), effectAfterCancel)

		assertEquals(
			TestState.StateA(value = "I am a value"),
			machine.value,
		)
	}

	@Test
	fun `Check we consume effects only 1 time`() = runTest {
		val machine = stateMachine<TestState, TestEffect, TestEvent>(
			scope = backgroundScope,
			initialState = TestState.StateA("I am a value"),
			started = SharingStarted.Eagerly,
		) {
			state<TestState.StateA> {
				onEvent<TestEvent.Close> { _, _ ->
					trigger(TestEffect.Close)
				}
			}
		}

		var effect1: TestEffect? = null
		var effect2: TestEffect? = null

		backgroundScope.launch {
			machine.consumeEffects {
				effect1 = it
			}
		}

		machine.send(TestEvent.Close)
		advanceTimeBy(1)

		backgroundScope.launch {
			machine.consumeEffects {
				effect2 = it
			}
		}
		advanceTimeBy(1)

		assertEquals(TestEffect.Close, effect1)
		assertNull(effect2)

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

		var effect: TestEffect? = null
		backgroundScope.launch {
			machine.consumeEffects { effect = it }
		}

		advanceTimeBy(1)
		assertEquals(TestEffect.Close, effect)

		assertEquals(
			TestState.StateB(value = "I am a value"),
			machine.value,
		)
	}


}
