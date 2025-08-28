package com.klitsie.statemachine.form

sealed interface FormViewState {

	data object Loading: FormViewState
	data object Failure: FormViewState
	data class FormInput(
		val value: String,
        val inputErrorMessage: String? = null,
		val showLoading: Boolean = false,
	): FormViewState
	data object Success: FormViewState

}
