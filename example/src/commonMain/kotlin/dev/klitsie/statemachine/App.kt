package dev.klitsie.statemachine

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.klitsie.statemachine.nested.ExampleEvent
import dev.klitsie.statemachine.nested.NestedExampleState
import dev.klitsie.statemachine.nested.NestedExampleViewModel
import org.jetbrains.compose.ui.tooling.preview.Preview

@Preview
@Composable
fun App(onClose: () -> Unit = {}) {
	MaterialTheme {

		val viewModel = viewModel { NestedExampleViewModel() }
		val state by viewModel.stateMachine.collectAsStateWithLifecycle()

		when (val state = state) {
			NestedExampleState.Pending -> Button(onClick = {
				viewModel.stateMachine.onEvent(
					ExampleEvent.StartLoading("1")
				)
			}) {
				Text("Start loading!")
			}

			is NestedExampleState.Loading -> CircularProgressIndicator()

			is NestedExampleState.InputName -> Column {
				TextField(
					value = state.username,
					onValueChange = { viewModel.stateMachine.onEvent(ExampleEvent.UpdateName(it)) })
			}

			is NestedExampleState.LoadingFailed.Retryable -> Column {
				Text("Oh no")
				Spacer(modifier = Modifier.height(16.dp))
				Button(onClick = {
					viewModel.stateMachine.onEvent(ExampleEvent.Retry)
				}) {
					Text("Retry")
				}
				Button(onClick = {
					viewModel.stateMachine.onEvent(ExampleEvent.Close)
				}) {
					Text("Close")
				}

			}

			NestedExampleState.LoadingFailed.UserIsAnIdiot -> Column {
				Text("Oh no, you are an idiot")
				Spacer(modifier = Modifier.height(16.dp))
				Button(onClick = {
					viewModel.stateMachine.onEvent(ExampleEvent.Close)
				}) {
					Text("Close")
				}

			}

			NestedExampleState.LoadingFailed.UserLockedOut -> Column {
				Text("Oh no, you are locked out!")
				Spacer(modifier = Modifier.height(16.dp))
				Button(onClick = {
					viewModel.stateMachine.onEvent(ExampleEvent.Close)
				}) {
					Text("Close")
				}

			}

			NestedExampleState.CloseScreen -> LaunchedEffect(Unit) {
				onClose()
			}
		}
	}
}
