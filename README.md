[![Maven Central](https://img.shields.io/maven-central/v/dev.klitsie.statemachine/statemachine-core)](https://central.sonatype.com/artifact/dev.klitsie.statemachine/statemachine-core)
[![Kotlin](https://img.shields.io/badge/kotlin-2.2.21-blue.svg?logo=kotlin)](http://kotlinlang.org)
[![Platform](https://img.shields.io/badge/platform-Android%20%7C%20iOS%20%7C%20JVM%20%7C%20JS%20%7C%20WasmJS-lightgrey.svg)](#)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

# **Kotlin Multiplatform State Machine**

A lightweight, type-safe, and highly performant Finite State Machine (FSM) library built for **Kotlin Multiplatform**.
Designed specifically to handle complex logic transitions with a clean DSL, while maintaining a tiny footprint.

## **🚀 Key Features**

* **Multiplatform First:** Native support for Android, iOS, JVM, JS and Wasm-JS.
* **Presentation-Layer Focused:** The machine is optimized to live within a CoroutineScope (like viewModelScope). Any
  work or side-effects are automatically cancelled when the scope is cleared.
* **Implements StateFlow:** The machine itself implements StateFlow\<S\>, allowing it to be collected directly. By
  default, it uses `SharingStarted.WhileSubscribed(5.seconds)`.
* **One-Time Effects:** Exposes an Effect flow for one-time events (like navigation or toasts) that shouldn't be part of
  the persistent state.
* **Lifecycle-Aware Side Effects:** Use the sideEffect { } block to launch work that starts on entry and cancels on
  exit, similar to Compose's LaunchedEffect.
* **Restart Control with Keys:** Provide a key \= { } to sideEffect to prevent restarts when the state data changes but
  the logical identity remains the same.
* **Type-Safe DSL:** Leverage Kotlin's sealed classes for compile-time safety.
* **Wasm Optimized:** Zero-dependency core, ideal for web applications.

## **📦 Installation**

Add the dependency to your commonMain source set:

```kotlin
kotlin {
	sourceSets {
		commonMain.dependencies {
			implementation("dev.klitsie.statemachine:statemachine-core:0.2.0")
		}
	}
}
```

## **🛠️ Usage**

### **1\. Define your Graph**

```kotlin
sealed interface QuizState {
	data object Idle : QuizState
	sealed interface InProgress : QuizState {
		data class Playing(val questionId: String, val score: Int) : InProgress
		data object Loading : QuizState
	}
	data class Finished(val finalScore: Int) : QuizState
}

sealed interface QuizEffect {
	data object PlayCorrectSound : QuizEffect
	data object PlayWrongSound : QuizEffect
}

sealed interface QuizEvent {
	data object Start : QuizEvent
	data object Cancel : QuizEvent
	data class Answer(val isCorrect: Boolean) : QuizEvent
	data object TimeOut : QuizEvent
	data class NextQuestion(val id: String) : QuizEvent
}
```

### **2\. Configure the Machine**

```kotlin
val machine = stateMachine<QuizState, QuizEffect, QuizEvent>(
	scope = viewModelScope, // All work is tied to this scope's lifecycle
	initialState = QuizState.Idle,
) {
	// Runs again on every state change in the machine
	sideEffect { state ->
		try {
			println("Entered state: $state")
			awaitCancellation()
		} finally {
			println("Exited state: $state")
		}
	}

	// Top-level state definition
	state<QuizState.Idle> {
		onEvent<QuizEvent.Start> { _, _ ->
			QuizState.InProgress.Loading
		}
	}

	// Define a sub-graph for states sharing common logic
	nestedState<QuizState.InProgress> {
		// Shared handler: allows 'Cancel' from anywhere within InProgress (Loading or Playing)
		onEvent<QuizEvent.Cancel> { _, _ -> QuizState.Idle }

		state<QuizState.InProgress.Loading> {
			// sideEffect starts when Loading is entered and cancels when exited
			sideEffect {
				val data = repository.fetchQuestions()
				// You can send events from within sideEffects
				send(QuizEvent.NextQuestion(data.first().id))
			}

			onEvent<QuizEvent.NextQuestion> { _, event ->
				QuizState.InProgress.Playing(questionId = event.id, score = 0)
			}
		}

		state<QuizState.InProgress.Playing> {
			// key: Only restarts if questionId changes. 
			// If the state changes (e.g., score updates) but ID is same, timer keeps running.
			sideEffect(key = { it.questionId }) {
				delay(30.seconds)
				send(QuizEvent.TimeOut)
			}

			onEvent<QuizEvent.Answer> { state, event ->
				if (event.isCorrect) {
					// Triggers a one-time event on the 'effects' flow
					trigger(QuizEffect.PlayCorrectSound)
					// Transitions to a new state instance with updated score
					state.copy(score = state.score + 10)
				} else {
					trigger(QuizEffect.PlayWrongSound)
					state // Return current state to skip transition
				}
			}

			onEvent<QuizEvent.TimeOut> { state, _ ->
				QuizState.Finished(finalScore = state.score)
			}
		}
	}
}
```

### **3\. Observe State and Effects**

```kotlin
// The machine IS a StateFlow
lifecycleScope.launch {
	machine.collect { currentState ->
		updateUI(currentState)
	}
}

// Observe one-time effects
lifecycleScope.launch {
	machine.effects.collect { effect ->
		when (effect) {
			QuizEffect.PlayCorrectSound -> audioPlayer.play("success.mp3")
			QuizEffect.PlayWrongSound -> audioPlayer.play("error.mp3")
		}
	}
}
```

### **🎨 Compose Integration**

```kotlin
@Composable
@Composable
fun QuizScreen(
	viewModel: QuizViewModel,
	audioPlayer: AudioPlayer,
) {

	// Lifecycle-aware collection using the machine's StateFlow implementation
	val state by viewModel.machine.collectAsStateWithLifecycle()

	LaunchedEffect(viewModel.machine.effects) {
		viewModel.machine.effects.collect { effect ->
			when (effect) {
				QuizEffect.PlayCorrectSound -> audioPlayer.play("success.mp3")
				QuizEffect.PlayWrongSound -> audioPlayer.play("error.mp3")
			}
		}
	}

	when (val s = state) {
		is QuizState.Idle -> StartButton(onClick = { viewModel.machine.send(QuizEvent.Start) })
		is QuizState.InProgress.Loading -> LoadingSpinner()
		is QuizState.InProgress.Playing -> ScoreBoard(s.score)
		is QuizState.Finished -> ResultsScreen(s.finalScore)
	}
}
```

## **🌐 Wasm-JS & JS Specifics**

This library is designed for the modern web. It avoids reflection and heavy JVM APIs, keeping your bundle small and your
logic fast across both Wasm-JS and JS targets.

## **📄 License**

This project is licensed under the **Apache License, Version 2.0**.  
Copyright 2026 klitsie.dev

Licensed under the Apache License, Version 2.0 (the "License");  
you may not use this file except in compliance with the License.  
You may obtain a copy of the License at

[http://www.apache.org/licenses/LICENSE-2.0\](http://www.apache.org/licenses/LICENSE-2.0)

Maintained by [klitsie.dev](https://www.google.com/search?q=https://klitsie.dev)
