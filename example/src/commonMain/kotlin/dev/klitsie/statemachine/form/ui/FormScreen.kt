package dev.klitsie.statemachine.form.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.klitsie.statemachine.form.FormEvent
import dev.klitsie.statemachine.form.FormViewModel
import dev.klitsie.statemachine.form.FormViewState

@Composable
fun FormScreen(
	viewModel: FormViewModel,
) {
	val state by viewModel.viewState.collectAsStateWithLifecycle()

	Scaffold(
		modifier = Modifier.fillMaxSize()
	) {
		Box(
			modifier = Modifier
				.fillMaxSize()
		) {
			AnimatedContent(
				targetState = state,
				contentKey = { state::class.simpleName },
				transitionSpec = { fadeIn() togetherWith fadeOut() },
			) { state ->
				when (state) {
					FormViewState.Loading -> FormLoadingScreen()

					FormViewState.Failure -> FormLoadingFailureScreen(
						onRetry = { viewModel.onEvent(FormEvent.Retry) },
					)

					is FormViewState.FormInput -> FormInputScreen(
						state = state,
						onValueChanged = {
							viewModel.onEvent(
								FormEvent.Update(
									it
								)
							)
						},
						onSave = { viewModel.onEvent(FormEvent.Save) },
					)

					FormViewState.Success -> FormInputSuccessScreen(
					)
				}
			}
			Button(
				onClick = { viewModel.onEvent(FormEvent.Reset) },
				modifier = Modifier
					.safeContentPadding()
					.padding(16.dp)
					.align(Alignment.TopEnd),
			) {
				Text("Reset demo")
			}
		}
	}
}
