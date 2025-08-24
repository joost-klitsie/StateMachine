package com.klitsie.statemachine.form.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun FormLoadingFailureScreen(onRetry: () -> Unit) {
	Column(
		modifier = Modifier
			.fillMaxSize()
			.background(color = MaterialTheme.colorScheme.errorContainer),
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = spacedBy(16.dp, Alignment.CenterVertically),
	) {
		Text(
			"Loading failed :(",
			color = MaterialTheme.colorScheme.onErrorContainer,
		)
		Button(
			onClick = onRetry,
			colors = ButtonDefaults.buttonColors().copy(
				containerColor = MaterialTheme.colorScheme.error,
				contentColor = MaterialTheme.colorScheme.onError,
			)
		) {
			Text("Retry!")
		}
	}
}
