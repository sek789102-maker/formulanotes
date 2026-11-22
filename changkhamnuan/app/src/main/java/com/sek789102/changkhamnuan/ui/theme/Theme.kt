package com.sek789102.changkhamnuan.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sek789102.changkhamnuan.R
import com.sek789102.changkhamnuan.data.ThemeMode

val PlexThai = FontFamily(
    Font(R.font.ibm_plex_sans_thai_regular, FontWeight.Normal),
    Font(R.font.ibm_plex_sans_thai_medium, FontWeight.Medium),
    Font(R.font.ibm_plex_sans_thai_semibold, FontWeight.SemiBold),
    Font(R.font.ibm_plex_sans_thai_bold, FontWeight.Bold)
)

// Construction yellow + charcoal. Yellow fills (buttons, bars); dark gold for text accents so
// everything stays readable in bright sunlight on site.
private val Light = lightColorScheme(
    primary = Color(0xFFFFC21A), onPrimary = Color(0xFF241A00),
    primaryContainer = Color(0xFFFFE7A1), onPrimaryContainer = Color(0xFF251A00),
    secondary = Color(0xFF7A5600), onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDC7A), onSecondaryContainer = Color(0xFF261A00),
    tertiary = Color(0xFF3A3A3A), onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE6E1D8), onTertiaryContainer = Color(0xFF1E1B16),
    background = Color(0xFFFFFBF2), onBackground = Color(0xFF1E1B16),
    surface = Color(0xFFFFFBF2), onSurface = Color(0xFF1E1B16),
    surfaceVariant = Color(0xFFEDE1CF), onSurfaceVariant = Color(0xFF4E4639),
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFFFF6E2),
    surfaceContainer = Color(0xFFFBF0D9), surfaceContainerHigh = Color(0xFFF5EAD2), surfaceContainerHighest = Color(0xFFEFE4CC),
    outline = Color(0xFF807667), outlineVariant = Color(0xFFD2C5B4),
    error = Color(0xFFBA1A1A), onError = Color.White, errorContainer = Color(0xFFFFDAD6), onErrorContainer = Color(0xFF410002)
)

// Near-black "night site" look with glowing construction yellow (default theme).
private val Dark = darkColorScheme(
    primary = Color(0xFFFFC61F), onPrimary = Color(0xFF1C1400),
    primaryContainer = Color(0xFF3D2F06), onPrimaryContainer = Color(0xFFFFE7A1),
    secondary = Color(0xFFFFC61F), onSecondary = Color(0xFF1C1400),
    secondaryContainer = Color(0xFF3A2E0E), onSecondaryContainer = Color(0xFFFFDF9E),
    tertiary = Color(0xFFD0C6B6), onTertiary = Color(0xFF353026),
    tertiaryContainer = Color(0xFF2E2A22), onTertiaryContainer = Color(0xFFEDE1CF),
    background = Color(0xFF0B0B0B), onBackground = Color(0xFFF2F0EC),
    surface = Color(0xFF0B0B0B), onSurface = Color(0xFFF2F0EC),
    surfaceVariant = Color(0xFF26231D), onSurfaceVariant = Color(0xFFBDB6A8),
    surfaceContainerLowest = Color(0xFF060606), surfaceContainerLow = Color(0xFF151515),
    surfaceContainer = Color(0xFF1B1B1B), surfaceContainerHigh = Color(0xFF232323), surfaceContainerHighest = Color(0xFF2D2D2D),
    outline = Color(0xFF8E8676), outlineVariant = Color(0xFF3A352B),
    error = Color(0xFFFFB4AB), onError = Color(0xFF690005), errorContainer = Color(0xFF93000A), onErrorContainer = Color(0xFFFFDAD6)
)

private fun s(size: Int, line: Int, w: FontWeight) = TextStyle(fontFamily = PlexThai, fontWeight = w, fontSize = size.sp, lineHeight = line.sp)

private val Type = Typography(
    headlineMedium = s(26, 36, FontWeight.Bold),
    headlineSmall = s(22, 30, FontWeight.SemiBold),
    titleLarge = s(19, 28, FontWeight.SemiBold),
    titleMedium = s(16, 24, FontWeight.SemiBold),
    titleSmall = s(14, 21, FontWeight.SemiBold),
    bodyLarge = s(16, 25, FontWeight.Normal),
    bodyMedium = s(14, 21, FontWeight.Normal),
    bodySmall = s(12, 18, FontWeight.Normal),
    labelLarge = s(14, 20, FontWeight.Medium),
    labelMedium = s(12, 17, FontWeight.Medium),
    labelSmall = s(11, 16, FontWeight.Medium)
)

@Composable
fun isDark(mode: ThemeMode) = when (mode) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

@Composable
fun AppTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isDark(mode)) Dark else Light,
        typography = Type,
        shapes = Shapes(
            small = RoundedCornerShape(10.dp), medium = RoundedCornerShape(14.dp), large = RoundedCornerShape(20.dp)
        ),
        content = content
    )
}
