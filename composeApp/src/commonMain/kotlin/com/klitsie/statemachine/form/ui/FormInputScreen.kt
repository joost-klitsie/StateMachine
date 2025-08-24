package com.klitsie.statemachine.form.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.klitsie.statemachine.form.FormViewState

@Composable
fun FormInputScreen(
	state: FormViewState.FormInput,
	onValueChanged: (String) -> Unit,
	onSave: () -> Unit,
	onConsumeFailure: () -> Unit,
	onSuccess: () -> Unit,
	onFailure: (Throwable) -> Unit,
) {
	Column(
		modifier = Modifier
			.fillMaxSize(),
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = spacedBy(16.dp, Alignment.CenterVertically),
	) {
		OutlinedTextField(
			value = state.value,
			onValueChange = onValueChanged,
		)
		Button(onClick = onSave) {
			AnimatedVisibility(state.showLoading) {
				CircularProgressIndicator(
					modifier = Modifier
						.padding(end = 8.dp)
						.size(20.dp),
					color = LocalContentColor.current,
				)
			}
			Text("Save")
		}
		AnimatedVisibility(
			state.showLoading,
			enter = fadeIn(animationSpec = tween(220, delayMillis = 150)) + expandVertically(),
			exit = fadeOut() + shrinkVertically(animationSpec = tween(220, delayMillis = 150)),
		) {
			Row(horizontalArrangement = spacedBy(16.dp)) {
				Button(onClick = onSuccess) {
					Text("Simulate Success")
				}
				Button(onClick = { onFailure(Throwable("Simulated exception")) }) {
					Text("Simulate Failure")
				}
			}
		}
		if (state.showErrorDialog) {
			AlertDialog(
				onDismissRequest = {},
				confirmButton = {
					Button(onClick = onSave) {
						Text("Retry")
					}
				},
				dismissButton = {
					TextButton(onClick = onConsumeFailure) {
						Text("Cancel")
					}
				},
				title = { Text("Failure!") },
				text = { Text("Something went wrong :(") }
			)
		}
	}
}
