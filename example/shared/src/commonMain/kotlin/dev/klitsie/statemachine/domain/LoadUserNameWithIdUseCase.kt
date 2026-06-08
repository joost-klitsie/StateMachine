package dev.klitsie.statemachine.domain

import kotlinx.coroutines.delay
import kotlin.Result.Companion.failure
import kotlin.Result.Companion.success
import kotlin.time.Duration.Companion.seconds

fun interface LoadUserNameWithIdUseCase {

	suspend fun run(id: String, shouldFail: Boolean): Result<String>

}

internal class DefaultLoadUserNameWithIdUseCase : LoadUserNameWithIdUseCase {

	override suspend fun run(id: String, shouldFail: Boolean): Result<String> {
		delay(1.seconds)
		return if (shouldFail) {
			failure(Exception())
		} else {
			success("Joost")
		}
	}

}
