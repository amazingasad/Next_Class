package com.example.nextclass.widget

import androidx.compose.ui.graphics.Color
import androidx.glance.color.ColorProvider

/**
 * Widget colors. [mode] 0 follows the system, 1 is always light, 2 is always dark.
 * Vermilion is reserved for the live and highlighted state.
 */
internal class WidgetPalette(private val mode: Int) {

    private fun pick(day: Color, night: Color) = when (mode) {
        1 -> ColorProvider(day = day, night = day)
        2 -> ColorProvider(day = night, night = night)
        else -> ColorProvider(day = day, night = night)
    }

    val surface = pick(Color(0xFFFFFFFF), Color(0xFF18181B))
    val hairline = pick(Color(0xFFDADAD6), Color(0xFF34343A))
    val ink = pick(Color(0xFF111113), Color(0xFFF4F4F2))
    val muted = pick(Color(0xFF6B6B66), Color(0xFFA1A1A8))
    val inkFaded = pick(Color(0x66111113), Color(0x66F4F4F2))
    val mutedFaded = pick(Color(0x556B6B66), Color(0x55A1A1A8))
    val sage = pick(Color(0xFF5E7568), Color(0xFF8FA89A))
    val vermilion = pick(Color(0xFFEA580C), Color(0xFFEA580C))
    val vermilionText = pick(Color(0xFFC2410C), Color(0xFFFB7A3A))
    val focusTint = pick(Color(0x14EA580C), Color(0x26EA580C))
    val clear = pick(Color(0x00000000), Color(0x00000000))
}
