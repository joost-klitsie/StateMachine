package dev.klitsie.statemachine

interface EffectHandler<out State : Any, Effect : Any> {

	fun trigger(effect: Effect): State

}
