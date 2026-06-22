package dev.klitsie.statemachine.pagination

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaginationScreen(onBack: () -> Unit) {
	val viewModel = viewModel { PaginationViewModel() }
	val state by viewModel.stateMachine.collectAsStateWithLifecycle()

	Scaffold(
		topBar = {
			TopAppBar(
				title = { Text("Pagination Example") },
				navigationIcon = {
					IconButton(onClick = onBack) {
						Text("<")
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
				PaginationState.LoadingInitial -> CircularProgressIndicator()
				is PaginationState.ErrorInitial -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
					Text("Error: ${s.error.message}")
					Button(onClick = { viewModel.stateMachine.send(PaginationEvent.Retry) }) {
						Text("Retry")
					}
				}

				is PaginationState.Data -> PaginationList(s, viewModel)
			}
		}
	}
}

@Composable
fun PaginationList(state: PaginationState.Data, viewModel: PaginationViewModel) {
	val listState = rememberLazyListState()

	// Detect when reaching the bottom
	val shouldLoadMore = remember {
		derivedStateOf {
			val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()
			lastVisibleItem != null && lastVisibleItem.index >= listState.layoutInfo.totalItemsCount - 2
		}
	}

	LaunchedEffect(shouldLoadMore.value) {
		if (shouldLoadMore.value && state is PaginationState.Data.Idle) {
			viewModel.stateMachine.send(PaginationEvent.LoadMore)
		}
	}

	LazyColumn(
		state = listState,
		modifier = Modifier.fillMaxSize(),
		contentPadding = PaddingValues(16.dp),
	) {
		items(state.items) { item ->
			Card(
				modifier = Modifier
					.fillMaxWidth()
					.padding(vertical = 8.dp),
			) {
				Text(
					text = item,
					modifier = Modifier.padding(16.dp),
					style = MaterialTheme.typography.bodyLarge,
				)
			}
		}

		item {
			when (state) {
				is PaginationState.Data.LoadingMore -> Box(
					modifier = Modifier
						.fillMaxWidth()
						.padding(16.dp),
					contentAlignment = Alignment.Center,
				) {
					CircularProgressIndicator()
				}

				is PaginationState.Data.ErrorLoadingMore -> Column(
					modifier = Modifier
						.fillMaxWidth()
						.padding(16.dp),
					horizontalAlignment = Alignment.CenterHorizontally,
				) {
					Text("Error loading more: ${state.error.message}", color = MaterialTheme.colorScheme.error)
					Button(onClick = { viewModel.stateMachine.send(PaginationEvent.Retry) }) {
						Text("Retry")
					}
				}

				is PaginationState.Data.EndOfList -> Box(
					modifier = Modifier
						.fillMaxWidth()
						.padding(16.dp),
					contentAlignment = Alignment.Center,
				) {
					Text("End of list reached", style = MaterialTheme.typography.bodyMedium)
				}

				is PaginationState.Data.Idle -> Spacer(modifier = Modifier.height(32.dp))
			}
		}
	}
}
