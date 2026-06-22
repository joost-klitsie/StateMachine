package dev.klitsie.statemachine.overview

sealed interface Example {
	data object Counter : Example
	data object Form : Example
	data object Loading : Example
	data object Pagination : Example
}

sealed interface NavKey {
	data object Overview : NavKey
	data class ExampleDetail(val example: Example) : NavKey
}

sealed interface OverviewState {
	val backStack: List<NavKey>

	data object ShowOverview : OverviewState {
		override val backStack = listOf(NavKey.Overview)
	}

	data class ShowExample(val example: Example) : OverviewState {
		override val backStack = listOf(NavKey.Overview, NavKey.ExampleDetail(example))
	}
}

sealed interface OverviewEvent {
	data class SelectExample(val example: Example) : OverviewEvent
	data object BackToOverview : OverviewEvent
}
