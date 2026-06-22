package dev.klitsie.statemachine.loading

sealed interface LoadingState<out T> {

	data object Loading : LoadingState<Nothing>
	sealed interface Success<out T> : LoadingState<T> {
		val value: T

		data class Pending<out T>(override val value: T) : Success<T>
		data class Refreshing<out T>(override val value: T) : Success<T>
		data class RefreshFailed<out T>(
			override val value: T,
			val error: Throwable,
		) : Success<T>
	}

	data class Failure(val error: Throwable) : LoadingState<Nothing>

}

sealed interface LoadingEvent<out T> {

	data class LoadingResult<T>(val value: Result<T>) : LoadingEvent<T>
	data object Retry : LoadingEvent<Nothing>
	data object Refresh : LoadingEvent<Nothing>
	data object Reset : LoadingEvent<Nothing>

}
