package dev.klitsie.statemachine.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.klitsie.statemachine.stateMachine

class OverviewViewModel : ViewModel() {
	val stateMachine = stateMachine<OverviewState, Nothing, OverviewEvent>(
		scope = viewModelScope,
		initialState = OverviewState.ShowOverview,
	) {
		state<OverviewState.ShowOverview> {
			onEvent<OverviewEvent.SelectExample> { _, event ->
				OverviewState.ShowExample(event.example)
			}
		}
		state<OverviewState.ShowExample> {
			onEvent<OverviewEvent.BackToOverview> { _, _ ->
				OverviewState.ShowOverview
			}
		}
	}
}
