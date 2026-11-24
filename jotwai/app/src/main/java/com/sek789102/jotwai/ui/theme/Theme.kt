package com.sek789102.jotwai.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sek789102.jotwai.R
import com.sek789102.jotwai.data.model.ThemeMode

val PlexThai = FontFamily(
    Font(R.font.ibm_plex_sans_thai_regular, FontWeight.Normal),
    Font(R.font.ibm_plex_sans_thai_medium, FontWeight.Medium),
    Font(R.font.ibm_plex_sans_thai_semibold, FontWeight.SemiBold),
    Font(R.font.ibm_plex_sans_thai_bold, FontWeight.Bold)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFFE0A300),
    onPrimary = Color(0xFF1A1206),
    primaryContainer = Color(0xFFFFE7A3),
    onPrimaryContainer = Color(0xFF3A2A00),
    secondary = Color(0xFF7A5A12),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFEBC2),
    onSecondaryContainer = Color(0xFF2A1E00),
    tertiary = Color(0xFF56633A),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFDAE9B4),
    onTertiaryContainer = Color(0xFF151F00),
    background = Color(0xFFFFF9EC),
    onBackground = Color(0xFF241A0B),
    surface = Color(0xFFFFF9EC),
    onSurface = Color(0xFF241A0B),
    surfaceVariant = Color(0xFFF4E6C6),
    onSurfaceVariant = Color(0xFF6B5A3C),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFFF4DC),
    surfaceContainer = Color(0xFFFFF0D2),
    surfaceContainerHigh = Color(0xFFFBE9C4),
    surfaceContainerHighest = Color(0xFFF5E1B6),
    outline = Color(0xFFB08A2E),
    outlineVariant = Color(0xFFE8D29E),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFC21A),
    onPrimary = Color(0xFF1A1206),
    primaryContainer = Color(0xFF5A4206),
    onPrimaryContainer = Color(0xFFFFE7A3),
    secondary = Color(0xFFE8C77A),
    onSecondary = Color(0xFF2A1E00),
    secondaryContainer = Color(0xFF4A3812),
    onSecondaryContainer = Color(0xFFFFEBC2),
    tertiary = Color(0xFFBECD9A),
    onTertiary = Color(0xFF293410),
    tertiaryContainer = Color(0xFF3F4B24),
    onTertiaryContainer = Color(0xFFDAE9B4),
    background = Color(0xFF120C05),
    onBackground = Color(0xFFF6EEDF),
    surface = Color(0xFF120C05),
    onSurface = Color(0xFFF6EEDF),
    surfaceVariant = Color(0xFF3A2C16),
    onSurfaceVariant = Color(0xFFBFB29A),
    surfaceContainerLowest = Color(0xFF0C0803),
    surfaceContainerLow = Color(0xFF1C140A),
    surfaceContainer = Color(0xFF231A0D),
    surfaceContainerHigh = Color(0xFF2C2111),
    surfaceContainerHighest = Color(0xFF372A16),
    outline = Color(0xFF8C7444),
    outlineVariant = Color(0xFF4A3A1E),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

/** Note background colours (index 0 = no colour). Light and dark variants keep text readable. */
object NoteColors {
    val names = listOf(
        R.string.color_default, R.string.color_red, R.string.color_orange, R.string.color_yellow,
        R.string.color_green, R.string.color_teal, R.string.color_blue, R.string.color_indigo,
        R.string.color_purple, R.string.color_pink, R.string.color_brown, R.string.color_gray
    )
    private val lightTones = listOf(
        0xFFFFFFFF, 0xFFFAD4CF, 0xFFFBDDB8, 0xFFFFF1A8, 0xFFDDF0C2, 0xFFC9EEE6,
        0xFFD3E6F8, 0xFFDCDCF8, 0xFFEBD9F5, 0xFFF9D6E6, 0xFFE9DCCB, 0xFFE6E4E1
    )
    private val darkTones = listOf(
        0xFF261E18, 0xFF5C2B29, 0xFF5E3A12, 0xFF5B4E14, 0xFF344A22, 0xFF1E4A45,
        0xFF223F5C, 0xFF33335E, 0xFF472E5B, 0xFF5B2245, 0xFF4A3A2A, 0xFF3C3A38
    )
    val count: Int get() = lightTones.size

    fun light(index: Int): Color = Color(lightTones.getOrElse(index) { lightTones[0] })
    fun dark(index: Int): Color = Color(darkTones.getOrElse(index) { darkTones[0] })
    fun of(index: Int, dark: Boolean): Color = if (dark) dark(index) else light(index)
}

private fun scaledTypography(scale: Float): Typography {
    fun s(size: Int, line: Int, weight: FontWeight): TextStyle =
        TextStyle(fontFamily = PlexThai, fontWeight = weight, fontSize = (size * scale).sp, lineHeight = (line * scale).sp)
    return Typography(
        displaySmall = s(30, 40, FontWeight.Bold),
        headlineLarge = s(28, 38, FontWeight.Bold),
        headlineMedium = s(24, 34, FontWeight.SemiBold),
        headlineSmall = s(21, 30, FontWeight.SemiBold),
        titleLarge = s(19, 28, FontWeight.SemiBold),
        titleMedium = s(16, 24, FontWeight.SemiBold),
        titleSmall = s(14, 21, FontWeight.SemiBold),
        bodyLarge = s(16, 26, FontWeight.Normal),
        bodyMedium = s(14, 22, FontWeight.Normal),
        bodySmall = s(12, 18, FontWeight.Normal),
        labelLarge = s(14, 20, FontWeight.Medium),
        labelMedium = s(12, 17, FontWeight.Medium),
        labelSmall = s(11, 16, FontWeight.Medium)
    )
}

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun isDarkTheme(mode: ThemeMode): Boolean = when (mode) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

@Composable
fun JotwaiTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    dynamicColor: Boolean = false,
    textScale: Float = 1f,
    content: @Composable () -> Unit
) {
    val dark = isDarkTheme(themeMode)
    val context = LocalContext.current
    val colors = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> DarkColors
        else -> LightColors
    }
    val typography = remember(textScale) { scaledTypography(textScale) }
    MaterialTheme(colorScheme = colors, typography = typography, shapes = AppShapes, content = content)
}

