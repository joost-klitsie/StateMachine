package dev.klitsie.statemachine.loading

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoadingScreen(
	onBack: () -> Unit,
) {
	val viewModel = viewModel { LoadingViewModel() }
	val state by viewModel.stateMachine.collectAsStateWithLifecycle()

	Scaffold(
		topBar = {
			TopAppBar(
				title = { Text("Generic Loading Example") },
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
				LoadingState.Loading -> CircularProgressIndicator()

				is LoadingState.Failure -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
					Text("Error: ${s.error.message}")
					Spacer(modifier = Modifier.height(16.dp))
					Button(onClick = { viewModel.stateMachine.send(LoadingEvent.Retry) }) {
						Text("Retry")
					}
				}

				is LoadingState.Success -> {
					Column(horizontalAlignment = Alignment.CenterHorizontally) {
						Text("Data: ${s.value}")

						when (s) {
							is LoadingState.Success.Refreshing -> {
								Spacer(modifier = Modifier.height(8.dp))
								CircularProgressIndicator(modifier = Modifier.size(24.dp))
								Text("Refreshing...")
							}

							is LoadingState.Success.RefreshFailed -> {
								Spacer(modifier = Modifier.height(8.dp))
								Text("Refresh failed: ${s.error.message}", color = MaterialTheme.colorScheme.error)
								Button(onClick = { viewModel.stateMachine.send(LoadingEvent.Retry) }) {
									Text("Try Refresh Again")
								}
							}

							is LoadingState.Success.Pending -> {
								Spacer(modifier = Modifier.height(16.dp))
								Button(onClick = { viewModel.stateMachine.send(LoadingEvent.Refresh) }) {
									Text("Refresh")
								}
							}
						}

						Spacer(modifier = Modifier.height(8.dp))
						Button(onClick = { viewModel.stateMachine.send(LoadingEvent.Reset) }) {
							Text("Reset")
						}
					}
				}
			}
		}
	}
}
