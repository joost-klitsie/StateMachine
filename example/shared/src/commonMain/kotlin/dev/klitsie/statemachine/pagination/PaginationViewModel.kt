package dev.klitsie.statemachine.pagination

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.klitsie.statemachine.stateMachine
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

class PaginationViewModel : ViewModel() {

	private val pageSize = 20
	private val totalItems = 60 // Simulate end of list after 3 pages

	val stateMachine = stateMachine<PaginationState, Nothing, PaginationEvent>(
		scope = viewModelScope,
		initialState = PaginationState.LoadingInitial,
	) {
		state<PaginationState.LoadingInitial> {
			sideEffect {
				loadPage(0)
			}

			onEvent<PaginationEvent.PageLoaded> { _, event ->
				if (event.isLastPage) {
					PaginationState.Data.EndOfList(event.items)
				} else {
					PaginationState.Data.Idle(event.items)
				}
			}

			onEvent<PaginationEvent.PageLoadFailed> { _, event ->
				PaginationState.ErrorInitial(event.error)
			}
		}

		state<PaginationState.ErrorInitial> {
			onEvent<PaginationEvent.Retry> { _, _ ->
				PaginationState.LoadingInitial
			}
		}

		nestedState<PaginationState.Data> {
			state<PaginationState.Data.Idle> {
				onEvent<PaginationEvent.LoadMore> { state, _ ->
					PaginationState.Data.LoadingMore(state.items)
				}
			}

			state<PaginationState.Data.LoadingMore> {
				sideEffect { state ->
					loadPage(state.items.size / pageSize)
				}

				onEvent<PaginationEvent.PageLoaded> { state, event ->
					val newItems = state.items + event.items
					if (event.isLastPage) {
						PaginationState.Data.EndOfList(newItems)
					} else {
						PaginationState.Data.Idle(newItems)
					}
				}

				onEvent<PaginationEvent.PageLoadFailed> { state, event ->
					PaginationState.Data.ErrorLoadingMore(state.items, event.error)
				}
			}

			state<PaginationState.Data.ErrorLoadingMore> {
				onEvent<PaginationEvent.Retry> { state, _ ->
					PaginationState.Data.LoadingMore(state.items)
				}
			}

			state<PaginationState.Data.EndOfList>()
		}
	}

	private suspend fun loadPage(page: Int) {
		delay(1500.milliseconds) // Simulate network delay

		// Simulate occasional failure
		if (page > 0 && kotlin.random.Random.nextInt(3) == 0) {
			stateMachine.send(PaginationEvent.PageLoadFailed(Exception("Failed to load page $page")))
			return
		}

		val start = page * pageSize
		val end = minOf(start + pageSize, totalItems)
		val items = (start until end).map { "Item $it" }
		val isLastPage = end >= totalItems

		stateMachine.send(PaginationEvent.PageLoaded(items, isLastPage))
	}
}
