package com.klitsie.statemachine.form.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
			supportingText = state.inputErrorMessage?.let { errorMessage ->
				@Composable {
					Text(errorMessage)
				}
			},
			isError = state.inputErrorMessage != null,
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
	}
}
