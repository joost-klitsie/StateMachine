package dev.klitsie.statemachine

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.view.WindowCompat

class MainActivity : ComponentActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		enableEdgeToEdge()
		super.onCreate(savedInstanceState)

		setContent {
			val view = LocalView.current
			LaunchedEffect(Unit) {
				val insetsController = window?.let { WindowCompat.getInsetsController(it, view) }
				insetsController?.isAppearanceLightStatusBars = true
				insetsController?.isAppearanceLightNavigationBars = true
			}
			App()
		}
	}
}

@Preview
@Composable
fun AppAndroidPreview() {
	App()
}
