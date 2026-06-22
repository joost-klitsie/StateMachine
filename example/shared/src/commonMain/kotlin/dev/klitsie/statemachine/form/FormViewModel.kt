package dev.klitsie.statemachine.form

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.klitsie.statemachine.stateMachine
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

class FormViewModel : ViewModel() {

	val stateMachine = stateMachine<FormState, FormEffect, FormEvent>(
		scope = viewModelScope,
		initialState = FormState.Input.PendingInput(""),
	) {
		nestedState<FormState.Input> {
			onEvent<FormEvent.UpdateInput> { _, event ->
				val input = event.input
				val error = if (input.length > 10) "Too long!" else null

				if (input.isEmpty() || error != null) {
					FormState.Input.PendingInput(input, error)
				} else {
					FormState.Input.PendingSubmit(input)
				}
			}

			state<FormState.Input.PendingInput>()

			state<FormState.Input.PendingSubmit> {
				onEvent<FormEvent.Submit> { state, _ ->
					trigger(FormEffect.ClearFocus)
					FormState.Submitting
				}
			}
		}

		state<FormState.Submitting> {
			sideEffect {
				delay(2.seconds) // Simulate network
				send(FormEvent.SubmitResult(Result.success("Form submitted successfully!")))
			}

			onEvent<FormEvent.SubmitResult> { _, event ->
				event.result.fold(
					onSuccess = { FormState.Success(it) },
					onFailure = { FormState.Input.PendingSubmit("", it.message) },
				)
			}
		}

		state<FormState.Success> {
			onEvent<FormEvent.Reset> { _, _ ->
				FormState.Input.PendingInput("")
			}
		}
	}
}
