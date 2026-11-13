package com.geezerfit.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Navy = Color(0xFF1F4E79)   // Dave
private val Forest = Color(0xFF375623) // Lesa

/** user: "dave", "lesa", or null for the neutral home screen. */
@Composable
fun GeezerFitTheme(user: String? = null, content: @Composable () -> Unit) {
    val primary = when (user) {
        "lesa" -> Forest
        else -> Navy
    }
    MaterialTheme(colorScheme = lightColorScheme(primary = primary), content = content)
}
