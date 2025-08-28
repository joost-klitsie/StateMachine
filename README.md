This is an example project showing off a state machine with a simple UI to show off its capabilities.
The link to the supporting article will arrive soon!

The demo shows that you can control the following flow:

1. Load form data, first time with an error we have to handle
2. Manipulate data
3. Save the data with input validation (not empty)
4. Show the success screen

Compared to the previous solution, we now rely on side effects to trigger loading/saving, fire events and transition to
a new state

<img src="docs/state_machine_side_effect_ez-gif.gif" width="250"/>

This is a Kotlin Multiplatform project targeting Android, Web, Desktop (JVM).

* [/composeApp](./composeApp/src) is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
  - [commonMain](./composeApp/src/commonMain/kotlin) is for code that’s common for all targets.
  - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
    For example, if you want to edit the Desktop (JVM) specific part, the [jvmMain](./composeApp/src/jvmMain/kotlin)
    folder is the appropriate location.

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html),
[Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform/#compose-multiplatform),
[Kotlin/Wasm](https://kotl.in/wasm/)…

You can open the web application by running the `:composeApp:wasmJsBrowserDevelopmentRun` Gradle task.
