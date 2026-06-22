package dev.klitsie.statemachine

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import dev.klitsie.statemachine.counter.CounterScreen
import dev.klitsie.statemachine.form.ui.FormScreen
import dev.klitsie.statemachine.loading.LoadingScreen
import dev.klitsie.statemachine.overview.*
import dev.klitsie.statemachine.pagination.PaginationScreen

@Composable
fun App() {
	MaterialTheme {
		val viewModel = viewModel { OverviewViewModel() }
		val state by viewModel.stateMachine.collectAsStateWithLifecycle()

		NavDisplay(
			backStack = state.backStack,
			entryDecorators = listOf(
				rememberSaveableStateHolderNavEntryDecorator(),
				rememberViewModelStoreNavEntryDecorator(),
			),
		) { key ->
			when (key) {
				NavKey.Overview -> NavEntry(key) {
					OverviewScreen(
						onSelectExample = { viewModel.stateMachine.send(OverviewEvent.SelectExample(it)) },
					)
				}

				is NavKey.ExampleDetail -> NavEntry(key) {
					val onBack = { viewModel.stateMachine.send(OverviewEvent.BackToOverview) }
					when (key.example) {
						Example.Counter -> CounterScreen(onBack = onBack)
						Example.Form -> FormScreen(onBack = onBack)
						Example.Loading -> LoadingScreen(onBack = onBack)
						Example.Pagination -> PaginationScreen(onBack = onBack)
					}
				}
			}
		}
	}
}
