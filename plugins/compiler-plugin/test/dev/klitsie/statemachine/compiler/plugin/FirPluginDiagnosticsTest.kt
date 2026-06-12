package dev.klitsie.statemachine.compiler.plugin

import org.junit.Test
import kotlin.test.assertEquals

class FirPluginDiagnosticsTest {

	@Test
	fun `getRendererFactory returns StateMachineDiagnosticRendererFactory`() {
		val factory = FirPluginDiagnostics.getRendererFactory()
		assertEquals(StateMachineDiagnosticRendererFactory, factory)
	}
}
