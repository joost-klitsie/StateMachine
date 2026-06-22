package dev.klitsie.statemachine.loading

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlin.Result.Companion.failure
import kotlin.Result.Companion.success
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

class LoadingViewModel : ViewModel() {

	val stateMachine = loadingMachine(
		scope = viewModelScope,
		loadData = {
			delay(2.seconds)
			if (Random.nextBoolean()) {
				success("Data loaded with random value: ${Random.nextInt(100)}")
			} else {
				failure(Exception("Loading failed!"))
			}
		},
	)
}
