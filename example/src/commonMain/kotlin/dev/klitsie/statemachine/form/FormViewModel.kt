package dev.klitsie.statemachine.form

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.klitsie.statemachine.domain.FetchFormDataUseCase
import dev.klitsie.statemachine.domain.SaveFormDataUseCase
import dev.klitsie.statemachine.stateMachine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class FormViewModel(
	private val fetchFormDataUseCase: FetchFormDataUseCase,
	private val saveFormDataUseCase: SaveFormDataUseCase,
) : ViewModel() {

	private val formStateMachine = stateMachine<FormState, FormEvent>(
		scope = viewModelScope,
		initialState = FormState.LoadingFormData(simulateLoadingFailure = true),
	) {
		sideEffect { state ->
			println("Current state: ${state::class.simpleName}")
		}
		onEvent<FormEvent.Reset> { _, _ ->
			FormState.LoadingFormData(simulateLoadingFailure = true)
		}
		state<FormState.LoadingFormData> {
			sideEffect { state ->
				fetchFormDataUseCase.run(simulateFailure = state.simulateLoadingFailure)
					.fold(
						onSuccess = FormEvent::LoadingSuccess,
						onFailure = FormEvent::Failure,
					)
					.also(::onEvent)
			}
			onEvent<FormEvent.LoadingSuccess> { _, event ->
				FormState.WithData.PendingInput(event.value)
			}
			onEvent<FormEvent.Failure> { _, event ->
				FormState.FormLoadingFailure(event.error)
			}
		}
		state<FormState.FormLoadingFailure> {
			onEvent<FormEvent.Retry> { _, _ ->
				FormState.LoadingFormData()
			}
		}
		nestedState<FormState.WithData> {
			state<FormState.WithData.PendingInput> {
				onEvent<FormEvent.Update> { state, event ->
					state.copy(value = event.value)
				}
				onEvent<FormEvent.Save> { state, _ ->
					FormState.WithData.SavingForm(value = state.value)
				}
			}
			state<FormState.WithData.SavingForm> {
				sideEffect { state ->
					saveFormDataUseCase.run(state.value)
						.fold(
							onSuccess = { FormEvent.SavingSuccess },
							onFailure = FormEvent::Failure,
						)
						.also(::onEvent)
				}
				onEvent<FormEvent.SavingSuccess> { _, _ ->
					FormState.Success
				}
				onEvent<FormEvent.Failure> { state, event ->
					FormState.WithData.SavingFailure(state.value, event.error)
				}
			}
			state<FormState.WithData.SavingFailure> {
				onEvent<FormEvent.Save> { state, _ ->
					FormState.WithData.SavingForm(state.value)
				}
				onEvent<FormEvent.Update> { _, event ->
					FormState.WithData.PendingInput(event.value)
				}
			}
		}

		state<FormState.Success>()
	}

	val viewState = formStateMachine.state.map { state ->
		when (state) {
			is FormState.LoadingFormData -> FormViewState.Loading
			is FormState.FormLoadingFailure -> FormViewState.Failure
			is FormState.WithData -> FormViewState.FormInput(
				value = state.value,
				inputErrorMessage = (state as? FormState.WithData.SavingFailure)?.error?.message,
				showLoading = state is FormState.WithData.SavingForm,
			)

			FormState.Success -> FormViewState.Success
		}
	}.stateIn(
		scope = viewModelScope,
		started = SharingStarted.WhileSubscribed(5000),
		initialValue = FormViewState.Loading,
	)

	fun onEvent(formEvent: FormEvent) {
		formStateMachine.onEvent(formEvent)
	}

}
