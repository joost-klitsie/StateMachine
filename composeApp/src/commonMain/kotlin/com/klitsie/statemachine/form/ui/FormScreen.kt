package com.klitsie.statemachine.form.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.klitsie.statemachine.form.FormEvent
import com.klitsie.statemachine.form.FormViewModel
import com.klitsie.statemachine.form.FormViewState

@Composable
fun FormScreen(
	viewModel: FormViewModel,
) {
	val state by viewModel.viewState.collectAsStateWithLifecycle()

	Scaffold(
		modifier = Modifier.fillMaxSize()
	) {
		AnimatedContent(
			targetState = state,
			contentKey = { state::class.simpleName },
			transitionSpec = { fadeIn() togetherWith fadeOut() },
		) { state ->
			when (state) {
				FormViewState.Loading -> FormLoadingScreen(
					onSuccess = { viewModel.onEvent(FormEvent.LoadingSuccess(it)) },
					onFailure = { viewModel.onEvent(FormEvent.Failure(it)) },
				)

				FormViewState.Failure -> FormLoadingFailureScreen(
					onRetry = { viewModel.onEvent(FormEvent.Retry) },
				)

				is FormViewState.FormInput -> FormInputScreen(
					state = state,
					onValueChanged = { viewModel.onEvent(FormEvent.Update(it)) },
					onSave = { viewModel.onEvent(FormEvent.Save) },
					onConsumeFailure = { viewModel.onEvent(FormEvent.ConsumeFailure) },
					onSuccess = { viewModel.onEvent(FormEvent.SavingSuccess) },
					onFailure = { viewModel.onEvent(FormEvent.Failure(it)) },
				)

				FormViewState.Success -> FormInputSuccessScreen(
					onReset = { viewModel.onEvent(FormEvent.Reset) },
				)
			}
		}
	}
}
