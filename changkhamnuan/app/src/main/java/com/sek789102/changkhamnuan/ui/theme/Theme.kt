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

// Navy + safety amber: easy to read in sunlight on site.
private val Light = lightColorScheme(
    primary = Color(0xFF1F4E79), onPrimary = Color.White,
    primaryContainer = Color(0xFFD3E4FF), onPrimaryContainer = Color(0xFF001C38),
    secondary = Color(0xFF8A5A00), onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDEA6), onSecondaryContainer = Color(0xFF2C1A00),
    tertiary = Color(0xFF3B6939), onTertiary = Color.White,
    tertiaryContainer = Color(0xFFBCF0B4), onTertiaryContainer = Color(0xFF002204),
    background = Color(0xFFF8F9FC), onBackground = Color(0xFF191C20),
    surface = Color(0xFFF8F9FC), onSurface = Color(0xFF191C20),
    surfaceVariant = Color(0xFFDFE2EB), onSurfaceVariant = Color(0xFF43474E),
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFF2F3F9),
    surfaceContainer = Color(0xFFECEEF4), surfaceContainerHigh = Color(0xFFE6E8EE), surfaceContainerHighest = Color(0xFFE1E2E8),
    outline = Color(0xFF73777F), outlineVariant = Color(0xFFC3C6CF),
    error = Color(0xFFBA1A1A), onError = Color.White, errorContainer = Color(0xFFFFDAD6), onErrorContainer = Color(0xFF410002)
)

private val Dark = darkColorScheme(
    primary = Color(0xFFA2C9FF), onPrimary = Color(0xFF00315C),
    primaryContainer = Color(0xFF004882), onPrimaryContainer = Color(0xFFD3E4FF),
    secondary = Color(0xFFFFBA38), onSecondary = Color(0xFF482A00),
    secondaryContainer = Color(0xFF684300), onSecondaryContainer = Color(0xFFFFDEA6),
    tertiary = Color(0xFFA1D399), onTertiary = Color(0xFF0A390E),
    tertiaryContainer = Color(0xFF235023), onTertiaryContainer = Color(0xFFBCF0B4),
    background = Color(0xFF111318), onBackground = Color(0xFFE1E2E8),
    surface = Color(0xFF111318), onSurface = Color(0xFFE1E2E8),
    surfaceVariant = Color(0xFF43474E), onSurfaceVariant = Color(0xFFC3C6CF),
    surfaceContainerLowest = Color(0xFF0C0E13), surfaceContainerLow = Color(0xFF191C20),
    surfaceContainer = Color(0xFF1D2024), surfaceContainerHigh = Color(0xFF282A2F), surfaceContainerHighest = Color(0xFF33353A),
    outline = Color(0xFF8D9199), outlineVariant = Color(0xFF43474E),
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
