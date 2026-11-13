package com.softoolsmith.geezerfit.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Dave: navy (#1F4E79). Lesa: forest green (#375623).
private val DaveColors = lightColorScheme(
    primary = Color(0xFF1F4E79),
    background = Color(0xFFFAFBFD),
    surface = Color(0xFFFAFBFD),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD3E4F8),
    onPrimaryContainer = Color(0xFF001D36),
    secondary = Color(0xFF526070),
    secondaryContainer = Color(0xFFD5E4F7),
    onSecondaryContainer = Color(0xFF0E1D2A),
    tertiaryContainer = Color(0xFFFFDDB8),
    onTertiaryContainer = Color(0xFF2B1700),
    surfaceVariant = Color(0xFFDFE3EB),
    onSurfaceVariant = Color(0xFF42474E),
)

private val LesaColors = lightColorScheme(
    primary = Color(0xFF375623),
    background = Color(0xFFFBFCF7),
    surface = Color(0xFFFBFCF7),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD5E8CB),
    onPrimaryContainer = Color(0xFF0A2000),
    secondary = Color(0xFF55624C),
    secondaryContainer = Color(0xFFD9E7CB),
    onSecondaryContainer = Color(0xFF131F0D),
    tertiaryContainer = Color(0xFFFFDDB8),
    onTertiaryContainer = Color(0xFF2B1700),
    surfaceVariant = Color(0xFFE0E4D6),
    onSurfaceVariant = Color(0xFF43483E),
)

/** user: "dave", "lesa", or null for the neutral home/settings screens (navy). */
@Composable
fun GeezerFitTheme(user: String? = null, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (user == "lesa") LesaColors else DaveColors, content = content)
}
