package dev.klitsie.statemachine.nested

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.klitsie.statemachine.domain.DefaultLoadUserNameWithIdUseCase
import dev.klitsie.statemachine.domain.LoadUserNameWithIdUseCase
import dev.klitsie.statemachine.stateMachine

class NestedExampleViewModel(
	private val loadUserNameWithIdUseCase: LoadUserNameWithIdUseCase = DefaultLoadUserNameWithIdUseCase(),
) : ViewModel() {

	val stateMachine = stateMachine<NestedExampleState, ExampleEvent>(
		scope = viewModelScope,
		initialState = NestedExampleState.Pending,
	) {
		onEvent<ExampleEvent.Reset> { _, _ ->
			NestedExampleState.Pending
		}
		sideEffect { state ->
			println("Current state: $state")
		}
		state<NestedExampleState.Pending> {
			onEvent<ExampleEvent.StartLoading> { _, event ->
				NestedExampleState.Loading(id = event.id, shouldFail = true)
			}
		}
		state<NestedExampleState.Loading> {
			sideEffect { state ->
				loadUserNameWithIdUseCase.run(id = state.id, shouldFail = state.shouldFail)
					.let(ExampleEvent::LoadingResult)
					.also(::onEvent)
			}
			onEvent<ExampleEvent.LoadingResult> { state, event ->
				event.result.fold(
					onSuccess = { value ->
						when (value) {
							"" -> NestedExampleState.InputName.Pending(username = value)
							else -> NestedExampleState.InputName.Confirm(username = value)
						}
					},
					onFailure = {
						when (it.message) {
							"user is an idiot" -> NestedExampleState.LoadingFailed.UserIsAnIdiot
							"user is locked out" -> NestedExampleState.LoadingFailed.UserLockedOut
							else -> NestedExampleState.LoadingFailed.Retryable(error = it, id = state.id)
						}
					}
				)
			}
		}
		nestedState<NestedExampleState.InputName> {
			onEvent<ExampleEvent.UpdateName> { _, event ->
				when (val value = event.newValue) {
					"" -> NestedExampleState.InputName.Pending(username = value)
					else -> NestedExampleState.InputName.Confirm(username = value)
				}
			}

			state<NestedExampleState.InputName.Pending>()
			state<NestedExampleState.InputName.Confirm>()
		}

		nestedState<NestedExampleState.LoadingFailed> {
			onEvent<ExampleEvent.Close> { _, _ ->
				NestedExampleState.CloseScreen
			}
			state<NestedExampleState.LoadingFailed.Retryable> {
				onEvent<ExampleEvent.Retry> { state, _ ->
					NestedExampleState.Loading(id = state.id, shouldFail = false)
				}
			}
			state<NestedExampleState.LoadingFailed.UserIsAnIdiot>()
			state<NestedExampleState.LoadingFailed.UserLockedOut>()
		}
	}

}
