package com.klitsie.statemachine.domain

import kotlinx.coroutines.delay
import kotlin.Result.Companion.failure
import kotlin.Result.Companion.success
import kotlin.time.Duration.Companion.seconds

fun interface SaveFormDataUseCase {

    suspend fun run(newValue: String): Result<Unit>

}

internal class DefaultSaveFormDataUseCase : SaveFormDataUseCase {

    override suspend fun run(newValue: String): Result<Unit> {
        return if (newValue.isNotEmpty()) {
            delay(1.seconds)
            success(Unit)
        } else {
            failure(Throwable("Test exception"))
        }
    }

}
