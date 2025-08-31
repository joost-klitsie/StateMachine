package com.klitsie.statemachine

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.klitsie.statemachine.domain.DefaultFetchFormDataUseCase
import com.klitsie.statemachine.domain.DefaultSaveFormDataUseCase
import com.klitsie.statemachine.form.FormViewModel
import com.klitsie.statemachine.form.ui.FormScreen
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
	MaterialTheme {
		FormScreen(
			viewModel = viewModel {
				// We don't have dependency injection, but this will do the trick :)
				FormViewModel(
					fetchFormDataUseCase = DefaultFetchFormDataUseCase(),
					saveFormDataUseCase = DefaultSaveFormDataUseCase(),
				)
			},
		)
	}
}
