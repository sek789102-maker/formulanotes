package com.sek789102.formulabook.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.sek789102.formulabook.data.model.ThemeMode

val BlueWhiteLightColorScheme = lightColorScheme(
    primary = BluePrimaryLight,
    onPrimary = OnBluePrimaryLight,
    primaryContainer = BluePrimaryContainerLight,
    onPrimaryContainer = OnBluePrimaryContainerLight,
    secondary = SkySecondaryLight,
    onSecondary = OnSkySecondaryLight,
    secondaryContainer = SkySecondaryContainerLight,
    onSecondaryContainer = OnSkySecondaryContainerLight,
    tertiary = PeriwinkleTertiaryLight,
    onTertiary = OnPeriwinkleTertiaryLight,
    tertiaryContainer = PeriwinkleTertiaryContainerLight,
    onTertiaryContainer = OnPeriwinkleTertiaryContainerLight,
    background = SkyBackgroundLight,
    onBackground = InkOnSurfaceLight,
    surface = PureSurfaceLight,
    onSurface = InkOnSurfaceLight,
    surfaceVariant = SoftSurfaceVariantLight,
    onSurfaceVariant = MutedOnSurfaceVariantLight,
    surfaceContainerLowest = PureSurfaceLight,
    surfaceContainerLow = SkyBackgroundLight,
    surfaceContainer = SoftSurfaceVariantLight,
    surfaceContainerHigh = SurfaceContainerHighLight,
    surfaceContainerHighest = BluePrimaryContainerLight,
    outline = SlateOutlineLight,
    outlineVariant = PaperLineLight,
    error = RedErrorLight,
    onError = OnRedErrorLight,
    errorContainer = RedErrorContainerLight,
    onErrorContainer = OnRedErrorContainerLight
)

val BlueWhiteDarkColorScheme = darkColorScheme(
    primary = SkyPrimaryDark,
    onPrimary = OnSkyPrimaryDark,
    primaryContainer = BluePrimaryContainerDark,
    onPrimaryContainer = OnBluePrimaryContainerDark,
    secondary = IceSecondaryDark,
    onSecondary = OnIceSecondaryDark,
    secondaryContainer = SkySecondaryContainerDark,
    onSecondaryContainer = OnSkySecondaryContainerDark,
    tertiary = PeriwinkleTertiaryDark,
    onTertiary = OnPeriwinkleTertiaryDark,
    tertiaryContainer = PeriwinkleTertiaryContainerDark,
    onTertiaryContainer = OnPeriwinkleTertiaryContainerDark,
    background = MidnightBackgroundDark,
    onBackground = IceOnSurfaceDark,
    surface = MidnightSurfaceDark,
    onSurface = IceOnSurfaceDark,
    surfaceVariant = MidnightSurfaceVariantDark,
    onSurfaceVariant = MutedOnSurfaceVariantDark,
    surfaceContainerLowest = MidnightBackgroundDark,
    surfaceContainerLow = MidnightSurfaceDark,
    surfaceContainer = MidnightSurfaceVariantDark,
    surfaceContainerHigh = MidnightSurfaceHighDark,
    outline = NavyOutlineDark,
    outlineVariant = PaperLineDark,
    error = CoralErrorDark,
    onError = OnCoralErrorDark,
    errorContainer = CoralErrorContainerDark,
    onErrorContainer = OnCoralErrorContainerDark
)

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(Dimens.Radius8),
    small = RoundedCornerShape(Dimens.Radius12),
    medium = RoundedCornerShape(Dimens.Radius16),
    large = RoundedCornerShape(Dimens.Radius24),
    extraLarge = RoundedCornerShape(Dimens.Radius24)
)

@Composable
fun resolveDarkTheme(themeMode: ThemeMode): Boolean = when (themeMode) {
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
}

@Composable
fun SmutNoteTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val darkTheme = resolveDarkTheme(themeMode)
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> BlueWhiteDarkColorScheme
        else -> BlueWhiteLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
