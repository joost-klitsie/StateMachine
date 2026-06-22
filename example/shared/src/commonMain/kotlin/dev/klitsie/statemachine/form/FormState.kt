package dev.klitsie.statemachine.form

sealed interface FormState {
    val input: String
    val error: String?

    sealed interface Input : FormState {
        data class PendingInput(override val input: String, override val error: String? = null) : Input
        data class PendingSubmit(override val input: String, override val error: String? = null) : Input
    }

    data object Submitting : FormState {
        override val input: String = ""
        override val error: String? = null
    }

    data class Success(val result: String) : FormState {
        override val input: String = ""
        override val error: String? = null
    }
}

sealed interface FormEvent {
    data class UpdateInput(val input: String) : FormEvent
    data object Submit : FormEvent
    data class SubmitResult(val result: Result<String>) : FormEvent
    data object Reset : FormEvent
}

sealed interface FormEffect {
    data object ClearFocus : FormEffect
}
