package dev.klitsie.statemachine.compiler.plugin

import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.expression.ExpressionCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirFunctionCallChecker
import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter

class StateMachinePluginRegistrar : CompilerPluginRegistrar() {
	override val pluginId: String = "dev.klitsie.statemachine.compiler.plugin"
	override val supportsK2: Boolean = true

	override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
		val enabled = configuration[StateMachineConfigurationKeys.Enabled, true]
		if (!enabled) return
		val configuration = configuration.toConfiguration()
		FirExtensionRegistrarAdapter.registerExtension(StateMachineFirRegistrar(configuration))
	}


}

class StateMachineFirRegistrar(
	private val configuration: StateMachineConfiguration,
) : FirExtensionRegistrar() {
	override fun ExtensionRegistrarContext.configurePlugin() {
		+{ session: FirSession -> StateMachineCheckersRegistrar(session, configuration) }
	}
}

class StateMachineCheckersRegistrar(session: FirSession, configuration: StateMachineConfiguration) :
	FirAdditionalCheckersExtension(session) {
	override val expressionCheckers: ExpressionCheckers = object : ExpressionCheckers() {
		override val functionCallCheckers: Set<FirFunctionCallChecker> =
			setOf(StateMachineDslChecker(configuration))
	}
}
