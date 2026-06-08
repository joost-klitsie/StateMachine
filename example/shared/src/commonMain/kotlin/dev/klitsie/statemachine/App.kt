package dev.klitsie.statemachine

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.klitsie.statemachine.nested.NestedExampleEffect
import dev.klitsie.statemachine.nested.NestedExampleEvent
import dev.klitsie.statemachine.nested.NestedExampleState
import dev.klitsie.statemachine.nested.NestedExampleViewModel

@Composable
fun App(onClose: () -> Unit = {}) {
	MaterialTheme {
		val viewModel = viewModel { NestedExampleViewModel() }
		val state by viewModel.stateMachine.collectAsStateWithLifecycle()

		LaunchedEffect(viewModel.stateMachine) {
			viewModel.stateMachine.consumeEffects { effect ->
				when (effect) {
					NestedExampleEffect.Close -> onClose()
				}
			}
		}

		Box(
			modifier = Modifier
				.fillMaxSize(),
			contentAlignment = Alignment.Center,
		) {
			when (val state = state) {
				NestedExampleState.Pending -> Button(
					onClick = {
						viewModel.stateMachine.send(
							NestedExampleEvent.StartLoading("1"),
						)
					},
				) {
					Text("Start loading!")
				}

				is NestedExampleState.Loading -> CircularProgressIndicator()

				is NestedExampleState.InputName -> Column {
					TextField(
						value = state.username,
						onValueChange = { viewModel.stateMachine.send(NestedExampleEvent.UpdateName(it)) },
					)
				}

				is NestedExampleState.LoadingFailed.Retryable -> Column {
					Text("Oh no")
					Spacer(modifier = Modifier.height(16.dp))
					Button(
						onClick = {
							viewModel.stateMachine.send(NestedExampleEvent.Retry)
						},
					) {
						Text("Retry")
					}
					Button(
						onClick = {
							viewModel.stateMachine.send(NestedExampleEvent.Close)
						},
					) {
						Text("Close")
					}

				}

				NestedExampleState.LoadingFailed.UserIsAnIdiot -> Column {
					Text("Oh no, you are an idiot")
					Spacer(modifier = Modifier.height(16.dp))
					Button(
						onClick = {
							viewModel.stateMachine.send(NestedExampleEvent.Close)
						},
					) {
						Text("Close")
					}

				}

				NestedExampleState.LoadingFailed.UserLockedOut -> Column {
					Text("Oh no, you are locked out!")
					Spacer(modifier = Modifier.height(16.dp))
					Button(
						onClick = {
							viewModel.stateMachine.send(NestedExampleEvent.Close)
						},
					) {
						Text("Close")
					}

				}
			}
		}
	}
}
