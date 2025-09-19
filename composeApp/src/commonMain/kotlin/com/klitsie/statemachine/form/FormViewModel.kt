package com.klitsie.statemachine.form

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.klitsie.statemachine.domain.FetchFormDataUseCase
import com.klitsie.statemachine.domain.SaveFormDataUseCase
import com.klitsie.statemachine.state.stateMachine
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
				FormState.FormLoaded(event.value)
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
		state<FormState.FormLoaded> {
			onEvent<FormEvent.Update> { state, event ->
				state.copy(value = event.value)
			}
			onEvent<FormEvent.Save> { state, _ ->
				FormState.SavingForm(value = state.value)
			}
		}
		state<FormState.SavingForm> {
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
				FormState.SavingFailure(state.value, event.error)
			}
		}
		state<FormState.Success> {
			onEvent<FormEvent.Reset> { _, _ ->
				FormState.LoadingFormData(simulateLoadingFailure = true)
			}
		}
		state<FormState.SavingFailure> {
			onEvent<FormEvent.Save> { state, _ ->
				FormState.SavingForm(state.value)
			}
			onEvent<FormEvent.Update> { _, event ->
				FormState.FormLoaded(event.value)
			}
		}
	}

	val viewState = formStateMachine.state.map { state ->
		when (state) {
			is FormState.LoadingFormData -> FormViewState.Loading
			is FormState.FormLoadingFailure -> FormViewState.Failure
			is FormState.FormLoaded -> FormViewState.FormInput(
				value = state.value,
			)

			is FormState.SavingForm -> FormViewState.FormInput(
				value = state.value,
				showLoading = true,
			)

			is FormState.SavingFailure -> FormViewState.FormInput(
				value = state.value,
				inputErrorMessage = "Field cannot be blank!",
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
