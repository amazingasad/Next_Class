package com.example.nextclass.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
class NcExtras(
    val live: Color,
    val liveText: Color,
    val upcoming: Color,
    val hairline: Color,
    val muted: Color,
)

private val LocalNcExtras = staticCompositionLocalOf<NcExtras> {
    error("NextClassTheme is missing")
}

object NcTheme {
    val extras: NcExtras
        @Composable
        @ReadOnlyComposable
        get() = LocalNcExtras.current
}

private val LightScheme = lightColorScheme(
    primary = NcPalette.SignalVermilion,
    onPrimary = Color.White,
    primaryContainer = NcPalette.SunkenLight,
    onPrimaryContainer = NcPalette.InkLight,
    secondary = NcPalette.SageLight,
    onSecondary = Color.White,
    secondaryContainer = NcPalette.SunkenLight,
    onSecondaryContainer = NcPalette.InkLight,
    tertiary = NcPalette.SageLight,
    onTertiary = Color.White,
    tertiaryContainer = NcPalette.SunkenLight,
    onTertiaryContainer = NcPalette.InkLight,
    background = NcPalette.OffWhite,
    onBackground = NcPalette.InkLight,
    surface = NcPalette.PureWhite,
    onSurface = NcPalette.InkLight,
    surfaceVariant = NcPalette.SunkenLight,
    onSurfaceVariant = NcPalette.MutedLight,
    surfaceTint = Color.Transparent,
    inverseSurface = NcPalette.InkLight,
    inverseOnSurface = NcPalette.OffWhite,
    error = NcPalette.ErrorLight,
    onError = Color.White,
    errorContainer = Color(0xFFFDECEA),
    onErrorContainer = Color(0xFF7A1710),
    outline = Color(0xFFA3A39D),
    outlineVariant = NcPalette.LineLight,
    scrim = Color.Black,
    surfaceBright = NcPalette.PureWhite,
    surfaceDim = Color(0xFFE9E9E5),
    surfaceContainerLowest = NcPalette.PureWhite,
    surfaceContainerLow = NcPalette.PureWhite,
    surfaceContainer = NcPalette.PureWhite,
    surfaceContainerHigh = NcPalette.SunkenLight,
    surfaceContainerHighest = NcPalette.SunkenLight,
)

private val DarkScheme = darkColorScheme(
    primary = NcPalette.SignalVermilion,
    onPrimary = Color.White,
    primaryContainer = NcPalette.SunkenDark,
    onPrimaryContainer = NcPalette.InkDark,
    secondary = NcPalette.SageDark,
    onSecondary = NcPalette.Obsidian,
    secondaryContainer = NcPalette.SunkenDark,
    onSecondaryContainer = NcPalette.InkDark,
    tertiary = NcPalette.SageDark,
    onTertiary = NcPalette.Obsidian,
    tertiaryContainer = NcPalette.SunkenDark,
    onTertiaryContainer = NcPalette.InkDark,
    background = NcPalette.Obsidian,
    onBackground = NcPalette.InkDark,
    surface = NcPalette.Charcoal,
    onSurface = NcPalette.InkDark,
    surfaceVariant = NcPalette.SunkenDark,
    onSurfaceVariant = NcPalette.MutedDark,
    surfaceTint = Color.Transparent,
    inverseSurface = NcPalette.InkDark,
    inverseOnSurface = NcPalette.Obsidian,
    error = NcPalette.ErrorDark,
    onError = NcPalette.Obsidian,
    errorContainer = Color(0xFF3B1512),
    onErrorContainer = Color(0xFFFDECEA),
    outline = Color(0xFF6E6E76),
    outlineVariant = NcPalette.LineDark,
    scrim = Color.Black,
    surfaceBright = NcPalette.SunkenDark,
    surfaceDim = NcPalette.Obsidian,
    surfaceContainerLowest = NcPalette.Obsidian,
    surfaceContainerLow = NcPalette.Charcoal,
    surfaceContainer = NcPalette.Charcoal,
    surfaceContainerHigh = NcPalette.SunkenDark,
    surfaceContainerHighest = NcPalette.SunkenDark,
)

@Composable
fun NextClassTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val scheme = if (darkTheme) DarkScheme else LightScheme
    val extras = NcExtras(
        live = NcPalette.SignalVermilion,
        liveText = if (darkTheme) NcPalette.VermilionTextDark else NcPalette.VermilionTextLight,
        upcoming = if (darkTheme) NcPalette.SageDark else NcPalette.SageLight,
        // 1dp hairline: outlineVariant at 35% alpha.
        hairline = scheme.outlineVariant.copy(alpha = 0.35f),
        muted = scheme.onSurfaceVariant,
    )
    CompositionLocalProvider(LocalNcExtras provides extras) {
        MaterialTheme(
            colorScheme = scheme,
            typography = NcTypography,
            content = content,
        )
    }
}
