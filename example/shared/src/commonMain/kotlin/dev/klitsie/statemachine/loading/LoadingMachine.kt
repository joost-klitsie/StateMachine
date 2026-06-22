package dev.klitsie.statemachine.loading

import dev.klitsie.statemachine.StateMachine
import dev.klitsie.statemachine.stateMachine
import kotlinx.coroutines.CoroutineScope

/**
 * A generic loading state machine that handles initial loading, refreshing, and retrying.
 */
fun <T> loadingMachine(
	scope: CoroutineScope,
	loadData: suspend () -> Result<T>,
): StateMachine<LoadingState<T>, Unit, LoadingEvent<T>> = stateMachine<LoadingState<T>, Unit, LoadingEvent<T>>(
	scope = scope,
	initialState = LoadingState.Loading,
) {
	// Shared Reset
	onEvent<LoadingEvent.Reset> { _, _ ->
		LoadingState.Loading
	}

	state<LoadingState.Loading> {
		sideEffect {
			send(LoadingEvent.LoadingResult(loadData()))
		}

		onEvent<LoadingEvent.LoadingResult<T>> { _, event ->
			event.value.fold(
				onSuccess = { LoadingState.Success.Pending(it) },
				onFailure = { LoadingState.Failure(it) },
			)
		}
	}

	nestedState<LoadingState.Success<T>> {
		state<LoadingState.Success.Pending<T>> {
			onEvent<LoadingEvent.Refresh> { state, _ ->
				LoadingState.Success.Refreshing(state.value)
			}
		}

		state<LoadingState.Success.Refreshing<T>> {
			sideEffect {
				send(LoadingEvent.LoadingResult(loadData()))
			}

			onEvent<LoadingEvent.LoadingResult<T>> { state, event ->
				event.value.fold(
					onSuccess = { LoadingState.Success.Pending(it) },
					onFailure = { LoadingState.Success.RefreshFailed(state.value, it) },
				)
			}
		}

		state<LoadingState.Success.RefreshFailed<T>> {
			onEvent<LoadingEvent.Retry> { state, _ ->
				LoadingState.Success.Refreshing(state.value)
			}
		}
	}

	state<LoadingState.Failure> {
		onEvent<LoadingEvent.Retry> { _, _ ->
			LoadingState.Loading
		}
	}
}
