package com.example.nextclass.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

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

private fun schemeFor(t: Tones, dark: Boolean): ColorScheme {
    val error = if (dark) NcPalettes.ErrorDark else NcPalettes.ErrorLight
    val onError = if (dark) t.base else Color.White
    return if (dark) {
        darkColorScheme(
            primary = t.accent,
            onPrimary = t.onAccent,
            primaryContainer = t.sunken,
            onPrimaryContainer = t.ink,
            secondary = t.muted,
            onSecondary = t.base,
            secondaryContainer = t.sunken,
            onSecondaryContainer = t.ink,
            tertiary = t.muted,
            onTertiary = t.base,
            tertiaryContainer = t.sunken,
            onTertiaryContainer = t.ink,
            background = t.base,
            onBackground = t.ink,
            surface = t.surface,
            onSurface = t.ink,
            surfaceVariant = t.sunken,
            onSurfaceVariant = t.muted,
            surfaceTint = Color.Transparent,
            inverseSurface = t.ink,
            inverseOnSurface = t.base,
            error = error,
            onError = onError,
            errorContainer = Color(0xFF3B1512),
            onErrorContainer = Color(0xFFFDECEA),
            outline = t.muted,
            outlineVariant = t.line,
            scrim = Color.Black,
            surfaceBright = t.sunken,
            surfaceDim = t.base,
            surfaceContainerLowest = t.base,
            surfaceContainerLow = t.surface,
            surfaceContainer = t.surface,
            surfaceContainerHigh = t.sunken,
            surfaceContainerHighest = t.sunken,
        )
    } else {
        lightColorScheme(
            primary = t.accent,
            onPrimary = t.onAccent,
            primaryContainer = t.sunken,
            onPrimaryContainer = t.ink,
            secondary = t.muted,
            onSecondary = Color.White,
            secondaryContainer = t.sunken,
            onSecondaryContainer = t.ink,
            tertiary = t.muted,
            onTertiary = Color.White,
            tertiaryContainer = t.sunken,
            onTertiaryContainer = t.ink,
            background = t.base,
            onBackground = t.ink,
            surface = t.surface,
            onSurface = t.ink,
            surfaceVariant = t.sunken,
            onSurfaceVariant = t.muted,
            surfaceTint = Color.Transparent,
            inverseSurface = t.ink,
            inverseOnSurface = t.base,
            error = error,
            onError = onError,
            errorContainer = Color(0xFFFDECEA),
            onErrorContainer = Color(0xFF7A1710),
            outline = t.muted,
            outlineVariant = t.line,
            scrim = Color.Black,
            surfaceBright = t.surface,
            surfaceDim = t.sunken,
            surfaceContainerLowest = t.base,
            surfaceContainerLow = t.surface,
            surfaceContainer = t.surface,
            surfaceContainerHigh = t.sunken,
            surfaceContainerHighest = t.sunken,
        )
    }
}

/**
 * @param themeMode 0 follows the phone, 1 is always light, 2 is always dark.
 * @param paletteId index into [NcPalettes.all].
 * @param dynamicColor use Material You colors from the wallpaper instead of the palette.
 */
@Composable
fun NextClassTheme(
    themeMode: Int = 0,
    paletteId: Int = 0,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val dark = resolveDark(themeMode, isSystemInDarkTheme())
    val useDynamic = dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val tones = remember(paletteId, dark) { NcPalettes.tones(paletteId, dark) }
    val scheme = when {
        useDynamic && dark -> dynamicDarkColorScheme(context)
        useDynamic -> dynamicLightColorScheme(context)
        else -> remember(tones, dark) { schemeFor(tones, dark) }
    }
    val extras = if (useDynamic) {
        NcExtras(
            live = scheme.primary,
            liveText = scheme.primary,
            upcoming = scheme.onSurface,
            hairline = scheme.outlineVariant.copy(alpha = 0.6f),
            muted = scheme.onSurfaceVariant,
        )
    } else {
        NcExtras(
            live = tones.accent,
            liveText = tones.accentText,
            upcoming = tones.ink,
            hairline = tones.line,
            muted = tones.muted,
        )
    }
    CompositionLocalProvider(LocalNcExtras provides extras) {
        MaterialTheme(
            colorScheme = scheme,
            typography = NcTypography,
            content = content,
        )
    }
}
