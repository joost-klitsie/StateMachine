package dev.klitsie.statemachine.overview

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverviewScreen(
	onSelectExample: (Example) -> Unit,
) {
	Scaffold(
		topBar = {
			TopAppBar(title = { Text("StateMachine Examples") })
		},
	) { padding ->
		Column(
			modifier = Modifier
				.fillMaxSize()
				.padding(padding),
			horizontalAlignment = Alignment.CenterHorizontally,
			verticalArrangement = Arrangement.Center,
		) {
			Button(
				onClick = { onSelectExample(Example.Counter) },
				modifier = Modifier.fillMaxWidth(0.8f),
			) {
				Text("Counter Example (Side Effects)")
			}
			Spacer(modifier = Modifier.height(16.dp))
			Button(
				onClick = { onSelectExample(Example.Form) },
				modifier = Modifier.fillMaxWidth(0.8f),
			) {
				Text("Form Example (Transitions)")
			}
			Spacer(modifier = Modifier.height(16.dp))
			Button(
				onClick = { onSelectExample(Example.Loading) },
				modifier = Modifier.fillMaxWidth(0.8f),
			) {
				Text("Generic Loading Example")
			}
			Spacer(modifier = Modifier.height(16.dp))
			Button(
				onClick = { onSelectExample(Example.Pagination) },
				modifier = Modifier.fillMaxWidth(0.8f),
			) {
				Text("Pagination Example")
			}
		}
	}
}
