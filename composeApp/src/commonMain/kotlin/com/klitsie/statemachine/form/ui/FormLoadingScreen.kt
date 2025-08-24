package com.klitsie.statemachine.form.ui

import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun FormLoadingScreen(
	onSuccess: (String) -> Unit,
	onFailure: (Throwable) -> Unit,
) {
	Column(
		modifier = Modifier
			.fillMaxSize(),
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = spacedBy(16.dp, Alignment.CenterVertically),
	) {
		CircularProgressIndicator()
		Text("Loading...")
		Row(horizontalArrangement = spacedBy(16.dp)) {
			Button(onClick = { onSuccess("Test") }) {
				Text("Simulate Success")
			}
			Button(onClick = { onFailure(Throwable("Simulated exception")) }) {
				Text("Simulate Failure")
			}
		}
	}
}
