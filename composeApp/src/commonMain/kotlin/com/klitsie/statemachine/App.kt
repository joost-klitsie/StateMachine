package com.klitsie.statemachine

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.klitsie.statemachine.form.ui.FormScreen
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
	MaterialTheme {
		FormScreen(viewModel = viewModel())
	}
}
