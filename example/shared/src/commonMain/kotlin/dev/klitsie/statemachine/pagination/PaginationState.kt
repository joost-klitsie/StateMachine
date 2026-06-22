package dev.klitsie.statemachine.pagination

sealed interface PaginationState {
	data object LoadingInitial : PaginationState
	data class ErrorInitial(val error: Throwable) : PaginationState

	sealed interface Data : PaginationState {
		val items: List<String>

		data class Idle(override val items: List<String>) : Data
		data class LoadingMore(override val items: List<String>) : Data
		data class ErrorLoadingMore(override val items: List<String>, val error: Throwable) : Data
		data class EndOfList(override val items: List<String>) : Data
	}
}

sealed interface PaginationEvent {
	data class PageLoaded(val items: List<String>, val isLastPage: Boolean) : PaginationEvent
	data class PageLoadFailed(val error: Throwable) : PaginationEvent
	data object LoadMore : PaginationEvent
	data object Retry : PaginationEvent
}
