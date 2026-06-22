package dev.klitsie.statemachine.form.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.klitsie.statemachine.form.FormEffect
import dev.klitsie.statemachine.form.FormEvent
import dev.klitsie.statemachine.form.FormState
import dev.klitsie.statemachine.form.FormViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormScreen(
	onBack: () -> Unit,
) {
	val viewModel = viewModel { FormViewModel() }
	val state by viewModel.stateMachine.collectAsStateWithLifecycle()
	val focusManager = LocalFocusManager.current

	LaunchedEffect(viewModel.stateMachine) {
		viewModel.stateMachine.consumeEffects { effect ->
			when (effect) {
				FormEffect.ClearFocus -> focusManager.clearFocus()
			}
		}
	}

	Scaffold(
		topBar = {
			TopAppBar(
				title = { Text("Form Example") },
				navigationIcon = {
					Button(onClick = onBack) {
						Text("Back")
					}
				},
			)
		},
	) { padding ->
		Box(
			modifier = Modifier
				.fillMaxSize()
				.padding(padding),
			contentAlignment = Alignment.Center,
		) {
			when (val s = state) {
				is FormState.Input -> {
					Column(horizontalAlignment = Alignment.CenterHorizontally) {
						TextField(
							value = s.input,
							onValueChange = { viewModel.stateMachine.send(FormEvent.UpdateInput(it)) },
							label = { Text("Enter something (max 10 chars)") },
							isError = s.error != null,
							supportingText = s.error?.let { { Text(it) } },
						)
						Spacer(modifier = Modifier.height(16.dp))
						Button(
							onClick = { viewModel.stateMachine.send(FormEvent.Submit) },
							enabled = s is FormState.Input.PendingSubmit,
						) {
							Text("Submit")
						}
					}
				}

				FormState.Submitting -> {
					CircularProgressIndicator()
				}

				is FormState.Success -> {
					Column(horizontalAlignment = Alignment.CenterHorizontally) {
						Text(s.result)
						Spacer(modifier = Modifier.height(16.dp))
						Button(onClick = { viewModel.stateMachine.send(FormEvent.Reset) }) {
							Text("Reset")
						}
					}
				}
			}
		}
	}
}
