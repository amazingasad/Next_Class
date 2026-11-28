package com.example.nextclass.widget

import android.content.Context
import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.glance.color.ColorProvider
import androidx.glance.unit.ColorProvider as ProviderType
import com.example.nextclass.data.local.AppPreferences
import com.example.nextclass.ui.theme.NcPalettes

/**
 * Widget colors, taken from the palette chosen in Settings. [mode] 0 follows the system, 1 is
 * always light, 2 is always dark. The accent is reserved for the live and highlighted state.
 */
internal class WidgetPalette(private val mode: Int, context: Context? = null, dynamic: Boolean = false) {

    private val paletteId = context?.let { AppPreferences(it).paletteId } ?: 0
    private val day = NcPalettes.tones(paletteId, dark = false)
    private val night = NcPalettes.tones(paletteId, dark = true)

    private fun pick(light: Color, dark: Color): ProviderType = when (mode) {
        1 -> ColorProvider(day = light, night = light)
        2 -> ColorProvider(day = dark, night = dark)
        else -> ColorProvider(day = light, night = dark)
    }

    private val useDynamic = dynamic && context != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    private val lightScheme: ColorScheme? = if (useDynamic) dynamicLightColorScheme(context!!) else null
    private val darkScheme: ColorScheme? = if (useDynamic) dynamicDarkColorScheme(context!!) else null

    private fun pick(
        light: Color,
        dark: Color,
        dayDynamic: (ColorScheme) -> Color,
        nightDynamic: (ColorScheme) -> Color,
    ): ProviderType {
        val l = lightScheme
        val d = darkScheme
        return if (l != null && d != null) pick(dayDynamic(l), nightDynamic(d)) else pick(light, dark)
    }

    val surface = pick(day.surface, night.surface, { it.surface }, { it.surface })
    val hairline = pick(day.line, night.line, { it.outlineVariant }, { it.outlineVariant })
    val ink = pick(day.ink, night.ink, { it.onSurface }, { it.onSurface })
    val muted = pick(day.muted, night.muted, { it.onSurfaceVariant }, { it.onSurfaceVariant })
    val inkFaded = pick(
        day.ink.copy(alpha = 0.4f), night.ink.copy(alpha = 0.4f),
        { it.onSurface.copy(alpha = 0.4f) }, { it.onSurface.copy(alpha = 0.4f) },
    )
    val mutedFaded = pick(
        day.muted.copy(alpha = 0.33f), night.muted.copy(alpha = 0.33f),
        { it.onSurfaceVariant.copy(alpha = 0.33f) }, { it.onSurfaceVariant.copy(alpha = 0.33f) },
    )
    val sage = pick(day.muted, night.muted, { it.tertiary }, { it.tertiary })
    val vermilion = pick(day.accent, night.accent, { it.primary }, { it.primary })
    val vermilionText = pick(day.accentText, night.accentText, { it.primary }, { it.primary })
    val focusTint = pick(
        day.accent.copy(alpha = 0.10f), night.accent.copy(alpha = 0.18f),
        { it.primary.copy(alpha = 0.10f) }, { it.primary.copy(alpha = 0.18f) },
    )
    val clear = pick(Color(0x00000000), Color(0x00000000))
}
