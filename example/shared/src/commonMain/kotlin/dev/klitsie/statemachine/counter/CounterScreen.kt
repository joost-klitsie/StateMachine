package dev.klitsie.statemachine.counter

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CounterScreen(
	onBack: () -> Unit,
) {
	val viewModel = viewModel { CounterViewModel() }
	val state by viewModel.stateMachine.collectAsStateWithLifecycle()
	val snackbarHostState = remember { SnackbarHostState() }

	val scope = rememberCoroutineScope()
	LaunchedEffect(viewModel.stateMachine) {
		viewModel.stateMachine.consumeEffects { effect ->
			when (effect) {
				is CounterEffect.ShowToast -> {
					scope.launch {
						snackbarHostState.showSnackbar(effect.message)
					}
				}
			}
		}
	}

	Scaffold(
		snackbarHost = { SnackbarHost(snackbarHostState) },
		topBar = {
			TopAppBar(
				title = { Text("Counter Example") },
				navigationIcon = {
					Button(onClick = onBack) {
						Text("Back")
					}
				},
			)
		},
	) { padding ->
		Column(
			modifier = Modifier
				.fillMaxSize()
				.padding(padding),
			horizontalAlignment = Alignment.CenterHorizontally,
			verticalArrangement = Arrangement.Center,
		) {
			when (val s = state) {
				is CounterState.Inactive -> {
					Text(
						text = "Counter is inactive",
						style = MaterialTheme.typography.headlineMedium,
					)
					Spacer(modifier = Modifier.height(16.dp))
					Button(onClick = { viewModel.stateMachine.send(CounterEvent.Start) }) {
						Text("Start Counting")
					}
				}

				is CounterState.Active -> {
					Text(
						text = "Count: ${s.count}",
						style = MaterialTheme.typography.headlineLarge,
					)
					Spacer(modifier = Modifier.height(16.dp))
					Row {
						Button(onClick = { viewModel.stateMachine.send(CounterEvent.Decrement) }) {
							Text("-")
						}
						Spacer(modifier = Modifier.width(8.dp))
						Button(onClick = { viewModel.stateMachine.send(CounterEvent.Increment) }) {
							Text("+")
						}
					}
					Spacer(modifier = Modifier.height(8.dp))
					Button(onClick = { viewModel.stateMachine.send(CounterEvent.Reset) }) {
						Text("Reset")
					}
				}
			}
		}
	}
}
