package com.klitsie.statemachine.form

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.klitsie.statemachine.state.stateMachine
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class FormViewModel : ViewModel() {

	private val formStateMachine = stateMachine<FormState, FormEvent>(
		scope = MainScope(),
		initialState = FormState.LoadingFormData,
	) {
		state<FormState.LoadingFormData> {
			onEvent<FormEvent.LoadingSuccess> { _, event ->
				FormState.FormLoaded(event.value)
			}
			onEvent<FormEvent.Failure> { _, event ->
				FormState.FormLoadingFailure(event.error)
			}
		}
		state<FormState.FormLoadingFailure> {
			onEvent<FormEvent.Retry> { _, _ ->
				FormState.LoadingFormData
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
			onEvent<FormEvent.SavingSuccess> { _, _ ->
				FormState.Success
			}
			onEvent<FormEvent.Failure> { state, event ->
				FormState.SavingFailure(state.value, event.error)
			}
		}
		state<FormState.Success> {
			onEvent<FormEvent.Reset> { _, _ ->
				FormState.LoadingFormData
			}
		}
		state<FormState.SavingFailure> {
			onEvent<FormEvent.Save> { state, _ ->
				FormState.SavingForm(state.value)
			}
			onEvent<FormEvent.ConsumeFailure> { state, _ ->
				FormState.FormLoaded(state.value)
			}
		}
	}

	val viewState = formStateMachine.state.map { state ->
		when (state) {
			FormState.LoadingFormData -> FormViewState.Loading
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
				showErrorDialog = true,
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
