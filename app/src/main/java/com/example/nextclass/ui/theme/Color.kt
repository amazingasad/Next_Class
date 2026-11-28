package com.example.nextclass.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * One light or dark side of a palette. The look follows a 70 / 25 / 5 split:
 * [base] covers about 70% of the screen, [surface] (with [sunken]) about 25% for cards, the dock
 * and sheets, and [accent] about 5% for live state and the main action. [ink], [muted] and
 * [line] are the neutral text and rule colors that sit on top.
 */
@Immutable
class Tones(
    val base: Color,
    val surface: Color,
    val sunken: Color,
    val ink: Color,
    val muted: Color,
    val line: Color,
    val accent: Color,
    val onAccent: Color,
    val accentText: Color,
)

@Immutable
class PaletteSpec(
    val name: String,
    val blurb: String,
    val light: Tones,
    val dark: Tones,
)

object NcPalettes {

    /** Warm paper and sand with a brick-red accent. The default. */
    private val Paper = PaletteSpec(
        name = "Paper",
        blurb = "Warm paper, sand and brick red",
        light = Tones(
            base = Color(0xFFF3EFE6),
            surface = Color(0xFFE7E1D4),
            sunken = Color(0xFFDDD6C6),
            ink = Color(0xFF1C1A16),
            muted = Color(0xFF675F53),
            line = Color(0xFFC9C0AF),
            accent = Color(0xFFC8402A),
            onAccent = Color(0xFFFFFFFF),
            accentText = Color(0xFFB03722),
        ),
        dark = Tones(
            base = Color(0xFF14120F),
            surface = Color(0xFF211E18),
            sunken = Color(0xFF2B2720),
            ink = Color(0xFFF2EDE3),
            muted = Color(0xFFA59C8B),
            line = Color(0xFF4A4337),
            accent = Color(0xFFF0653F),
            onAccent = Color(0xFF1C0C06),
            accentText = Color(0xFFFF8661),
        ),
    )

    /** Deep green neutrals with a burnt-orange accent. */
    private val Forest = PaletteSpec(
        name = "Forest",
        blurb = "Moss, sage and burnt orange",
        light = Tones(
            base = Color(0xFFEDF1EA),
            surface = Color(0xFFD9E3D5),
            sunken = Color(0xFFCDDAC8),
            ink = Color(0xFF13241A),
            muted = Color(0xFF53665A),
            line = Color(0xFFB6C6B3),
            accent = Color(0xFFB4501B),
            onAccent = Color(0xFFFFFFFF),
            accentText = Color(0xFF9A4313),
        ),
        dark = Tones(
            base = Color(0xFF0D1410),
            surface = Color(0xFF18241C),
            sunken = Color(0xFF213127),
            ink = Color(0xFFE6EEE8),
            muted = Color(0xFF93A598),
            line = Color(0xFF38493E),
            accent = Color(0xFFE8863F),
            onAccent = Color(0xFF1C0E03),
            accentText = Color(0xFFF39C57),
        ),
    )

    /** Cool slate with a single amber accent. */
    private val Harbor = PaletteSpec(
        name = "Harbor",
        blurb = "Slate, steel and amber",
        light = Tones(
            base = Color(0xFFEEF1F5),
            surface = Color(0xFFDBE1EA),
            sunken = Color(0xFFCED6E1),
            ink = Color(0xFF101A2C),
            muted = Color(0xFF526077),
            line = Color(0xFFB9C2D0),
            accent = Color(0xFFA5600A),
            onAccent = Color(0xFFFFFFFF),
            accentText = Color(0xFF8D5008),
        ),
        dark = Tones(
            base = Color(0xFF0C111A),
            surface = Color(0xFF172030),
            sunken = Color(0xFF202B3D),
            ink = Color(0xFFE5EAF2),
            muted = Color(0xFF93A0B5),
            line = Color(0xFF364359),
            accent = Color(0xFFF2B24A),
            onAccent = Color(0xFF1B1203),
            accentText = Color(0xFFF6C474),
        ),
    )

    val all: List<PaletteSpec> = listOf(Paper, Forest, Harbor)

    fun get(id: Int): PaletteSpec = all[id.coerceIn(0, all.lastIndex)]

    fun tones(id: Int, dark: Boolean): Tones = get(id).let { if (dark) it.dark else it.light }

    val ErrorLight = Color(0xFFB42318)
    val ErrorDark = Color(0xFFF97066)
}

/** Resolves the saved mode (0 system, 1 light, 2 dark) against the phone's current setting. */
fun resolveDark(mode: Int, systemDark: Boolean): Boolean = when (mode) {
    1 -> false
    2 -> true
    else -> systemDark
}
