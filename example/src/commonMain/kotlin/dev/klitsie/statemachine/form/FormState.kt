package dev.klitsie.statemachine.form

sealed interface FormState {

	data class LoadingFormData(
		val simulateLoadingFailure: Boolean = false,
	) : FormState

	data class FormLoadingFailure(val error: Throwable) : FormState

	sealed interface WithData : FormState {
		val value: String

		data class PendingInput(override val value: String) : WithData
		data class SavingForm(override val value: String) : WithData
		data class SavingFailure(
			override val value: String,
			val error: Throwable,
		) : WithData

	}

	data object Success : FormState

}

sealed interface FormEvent {

	data class Failure(val error: Throwable) : FormEvent
	data class LoadingSuccess(val value: String) : FormEvent
	data object Retry : FormEvent
	data class Update(val value: String) : FormEvent
	data object SavingSuccess : FormEvent
	data object Save : FormEvent
	data object Reset : FormEvent

}
