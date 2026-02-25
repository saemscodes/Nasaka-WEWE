package org.briarproject.briar.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// iOS System Colors (Light & Dark)
val iOSBlueLight = Color(0xFF007AFF)
val iOSBlueDark = Color(0xFF0A84FF)

val iOSGreenLight = Color(0xFF34C759)
val iOSGreenDark = Color(0xFF30D158)

val iOSRedLight = Color(0xFFFF3B30)
val iOSRedDark = Color(0xFFFF453A)

val iOSOrangeLight = Color(0xFFFF9500)
val iOSOrangeDark = Color(0xFFFF9F0A)

val iOSGrayLight = Color(0xFF8E8E93)
val iOSGrayDark = Color(0xFF8E8E93)

val iOSGroupedBGLight = Color(0xFFF2F2F7)
val iOSGroupedBGDark = Color(0xFF000000)

private val DarkColorScheme = darkColorScheme(
    primary = iOSBlueDark,
    secondary = iOSGrayDark,
    tertiary = iOSBlueDark,
    background = Color.Black,
    surface = Color(0xFF1C1C1E),
    error = iOSRedDark,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = iOSBlueLight,
    secondary = iOSGrayLight,
    tertiary = iOSBlueLight,
    background = iOSGroupedBGLight,
    surface = Color.White,
    error = iOSRedLight,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onTertiary = Color.White,
    onBackground = Color.Black,
    onSurface = Color.Black
)

/**
 * Standard spacing units based on 4dp grid
 */
object NasakaSpacing {
    val xSmall = 4.dp
    val small = 8.dp
    val medium = 16.dp
    val large = 24.dp
    val xLarge = 32.dp
}

@Composable
fun NasakaWeweTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
