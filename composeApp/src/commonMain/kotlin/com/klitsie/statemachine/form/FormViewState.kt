package com.klitsie.statemachine.form

sealed interface FormViewState {

	data object Loading : FormViewState
	data object Failure : FormViewState
	data class FormInput(
		val value: String,
		val showErrorDialog: Boolean = false,
		val showLoading: Boolean = false,
	) : FormViewState

	data object Success : FormViewState

}
