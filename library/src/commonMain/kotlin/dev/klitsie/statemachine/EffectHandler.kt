package dev.klitsie.statemachine

/**
 * Scope for handling effects within a state transition.
 */
@StateDsl
interface EffectHandler<out State : Any, Effect : Any> {

	/**
	 * Triggers an [effect] and returns the current [State].
	 */
	fun trigger(effect: Effect): State

}
