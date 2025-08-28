package com.klitsie.statemachine.form

sealed interface FormState {

    data class LoadingFormData(
        val simulateLoadingFailure: Boolean = false,
    ) : FormState

    data class FormLoadingFailure(val error: Throwable) : FormState

    data class FormLoaded(val value: String) : FormState
    data class SavingForm(val value: String) : FormState
    data class SavingFailure(
        val value: String,
        val error: Throwable,
    ) : FormState

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
