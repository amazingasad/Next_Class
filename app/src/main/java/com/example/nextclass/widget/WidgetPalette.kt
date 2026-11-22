package com.example.nextclass.widget

import android.content.Context
import android.os.Build
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.glance.color.ColorProvider

/**
 * Widget colors. [mode] 0 follows the system, 1 is always light, 2 is always dark.
 * Vermilion is reserved for the live and highlighted state.
 */
internal class WidgetPalette(private val mode: Int, context: Context? = null, dynamic: Boolean = false) {

    private fun pick(day: Color, night: Color) = when (mode) {
        1 -> ColorProvider(day = day, night = day)
        2 -> ColorProvider(day = night, night = night)
        else -> ColorProvider(day = day, night = night)
    }

    private val useDynamic = dynamic && context != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    private val light = if (useDynamic) dynamicLightColorScheme(context!!) else null
    private val dark = if (useDynamic) dynamicDarkColorScheme(context!!) else null

    private fun pick(day: Color, night: Color, dayDynamic: (androidx.compose.material3.ColorScheme) -> Color, nightDynamic: (androidx.compose.material3.ColorScheme) -> Color): ColorProvider {
        val l = light
        val d = dark
        return if (l != null && d != null) pick(dayDynamic(l), nightDynamic(d)) else pick(day, night)
    }

    val surface = pick(Color(0xFFFFFFFF), Color(0xFF18181B), { it.surface }, { it.surface })
    val hairline = pick(Color(0xFFDADAD6), Color(0xFF34343A), { it.outlineVariant }, { it.outlineVariant })
    val ink = pick(Color(0xFF111113), Color(0xFFF4F4F2), { it.onSurface }, { it.onSurface })
    val muted = pick(Color(0xFF6B6B66), Color(0xFFA1A1A8), { it.onSurfaceVariant }, { it.onSurfaceVariant })
    val inkFaded = pick(Color(0x66111113), Color(0x66F4F4F2), { it.onSurface.copy(alpha = 0.4f) }, { it.onSurface.copy(alpha = 0.4f) })
    val mutedFaded = pick(Color(0x556B6B66), Color(0x55A1A1A8), { it.onSurfaceVariant.copy(alpha = 0.33f) }, { it.onSurfaceVariant.copy(alpha = 0.33f) })
    val sage = pick(Color(0xFF5E7568), Color(0xFF8FA89A), { it.tertiary }, { it.tertiary })
    val vermilion = pick(Color(0xFFEA580C), Color(0xFFEA580C), { it.primary }, { it.primary })
    val vermilionText = pick(Color(0xFFC2410C), Color(0xFFFB7A3A), { it.primary }, { it.primary })
    val focusTint = pick(Color(0x14EA580C), Color(0x26EA580C), { it.primary.copy(alpha = 0.10f) }, { it.primary.copy(alpha = 0.18f) })
    val clear = pick(Color(0x00000000), Color(0x00000000))
}
