package org.balise.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Night = Color(0xFF061226)
val DeepBlue = Color(0xFF0B1F4D)
val CardBlue = Color(0xFF10285A)
val Line = Color(0xFF2A4580)
val Muted = Color(0xFFB7C3DE)
val Orange = Color(0xFFFF7A1A)
val OrangeLight = Color(0xFFFFE7D3)

private val BaliseColors = darkColorScheme(
    primary = Orange,
    onPrimary = DeepBlue,
    primaryContainer = Color(0xFF1C3A7A),
    onPrimaryContainer = Color.White,
    secondary = Orange,
    onSecondary = DeepBlue,
    secondaryContainer = Color(0xFF3B2410),
    onSecondaryContainer = OrangeLight,
    tertiary = Orange,
    onTertiary = DeepBlue,
    background = Night,
    onBackground = Color.White,
    surface = Night,
    onSurface = Color.White,
    surfaceVariant = CardBlue,
    onSurfaceVariant = Muted,
    surfaceContainer = CardBlue,
    surfaceContainerHigh = CardBlue,
    surfaceContainerHighest = CardBlue,
    outline = Line,
    error = Color(0xFFE5484D),
    onError = Color.White
)

@Composable
fun BaliseTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = BaliseColors, content = content)
}
