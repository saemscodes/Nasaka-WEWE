package org.briarproject.briar.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// CEKA / Nasaka WEWE Palette
val EmeraldPrimary = Color(0xFF1B5E20)
val EmeraldLight = Color(0xFF4C8C4A)
val EmeraldDark = Color(0xFF003300)

val GoldSecondary = Color(0xFFC5A059)
val GoldLight = Color(0xFFF9D187)
val GoldDark = Color(0xFF93722E)

val DarkBackground = Color(0xFF121212)
val SurfaceDark = Color(0xFF1E1E1E)

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldPrimary,
    secondary = GoldSecondary,
    tertiary = EmeraldLight,
    background = DarkBackground,
    surface = SurfaceDark,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color(0xFFE0E0E0),
    onSurface = Color(0xFFE0E0E0)
)

private val LightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    secondary = GoldSecondary,
    tertiary = EmeraldLight,
    background = Color(0xFFF5F5F5),
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color(0xFF121212),
    onSurface = Color(0xFF121212)
)

@Composable
fun NasakaWeweTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography, // We'll define this next
        content = content
    )
}
