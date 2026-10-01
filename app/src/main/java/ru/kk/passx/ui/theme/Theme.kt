package ru.kk.passx.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val GlassmorphismDarkColorScheme = darkColorScheme(
    primary = CyanPrimary,
    onPrimary = Color.Black,
    secondary = IndigoSecondary,
    onSecondary = Color.White,
    background = OLEDBackground,
    onBackground = Color(0xFFF8FAFC),
    surface = GlassSurface,
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = GlassSurfaceVariant,
    onSurfaceVariant = Color(0xFF94A3B8),
    error = RoseError
)

@Composable
fun PassXTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = GlassmorphismDarkColorScheme,
        typography = Typography,
        content = content
    )
}
