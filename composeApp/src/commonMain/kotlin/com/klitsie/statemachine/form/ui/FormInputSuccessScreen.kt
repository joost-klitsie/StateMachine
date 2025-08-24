package com.klitsie.statemachine.form.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import statemachine.composeapp.generated.resources.Res
import statemachine.composeapp.generated.resources.compose_multiplatform

@Composable
fun FormInputSuccessScreen(onReset: () -> Unit) {
	Column(
		modifier = Modifier
			.fillMaxSize(),
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = spacedBy(16.dp, Alignment.CenterVertically),
	) {
		Image(painterResource(Res.drawable.compose_multiplatform), null)
		Text("Great Success!")
		Button(
			onClick = onReset,
		) {
			Text("Reset!")
		}
	}
}
