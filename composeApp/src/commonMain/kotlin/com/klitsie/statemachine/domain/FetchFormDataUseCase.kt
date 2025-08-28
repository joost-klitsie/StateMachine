package com.klitsie.statemachine.domain

import kotlinx.coroutines.delay
import kotlin.Result.Companion.failure
import kotlin.Result.Companion.success
import kotlin.time.Duration.Companion.seconds

fun interface FetchFormDataUseCase {

    suspend fun run(simulateFailure: Boolean): Result<String>

}

internal class DefaultFetchFormDataUseCase : FetchFormDataUseCase {

    override suspend fun run(simulateFailure: Boolean): Result<String> {
        delay(2.seconds)
        return if (simulateFailure) {
            failure(Throwable("Test exception"))
        } else {
            success("Test input")
        }
    }
}
