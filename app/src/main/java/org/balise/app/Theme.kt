package org.balise.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val DeepBlue = Color(0xFF0B1F4D)
val Orange = Color(0xFFFF7A1A)
val OrangeLight = Color(0xFFFFE7D3)
val Cloud = Color(0xFFF4F6FB)

private val BaliseColors = lightColorScheme(
    primary = DeepBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE4F5),
    onPrimaryContainer = DeepBlue,
    secondary = Orange,
    onSecondary = DeepBlue,
    secondaryContainer = OrangeLight,
    onSecondaryContainer = DeepBlue,
    tertiary = Orange,
    onTertiary = DeepBlue,
    background = Cloud,
    onBackground = DeepBlue,
    surface = Color.White,
    onSurface = DeepBlue,
    surfaceVariant = Color(0xFFE6EAF4),
    onSurfaceVariant = Color(0xFF3B4A6B),
    surfaceContainerHighest = Color(0xFFE8ECF6),
    error = Color(0xFFC62828),
    onError = Color.White
)

@Composable
fun BaliseTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = BaliseColors, content = content)
}
