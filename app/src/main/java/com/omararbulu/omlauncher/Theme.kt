package com.omararbulu.omlauncher

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight

@OptIn(ExperimentalTextApi::class)
val Outfit = FontFamily(
    Font(R.font.outfit, FontWeight.Light, variationSettings = FontVariation.Settings(FontVariation.weight(300))),
    Font(R.font.outfit, FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.outfit, FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500))),
    Font(R.font.outfit, FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
    Font(R.font.outfit, FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700))),
)

/** Colores de texto según el fondo. */
data class Ink(val main: Color, val soft: Color, val light: Boolean)

val DarkInk = Ink(Color(0xFF262A31), Color(0xFF3A404A), light = false)
val LightInk = Ink(Color(0xFFEDE9E2), Color(0xFFC9C4BC), light = true)

/** Superficie tipo papel para Agenda, cajón y paneles. */
data class Paper(val overlay: Color, val ink: Ink, val muted: Color, val line: Color, val accent: Color)

val DayPaper = Paper(
    overlay = Color(0xF5F4F1EC),
    ink = DarkInk,
    muted = Color(0xFF4E5560),
    line = Color(0xFF9AA0A8),
    accent = Color(0xFF8A5A45),
)
val NightPaper = Paper(
    overlay = Color(0xFB161A26),
    ink = LightInk,
    muted = Color(0xFFB4B0A9),
    line = Color(0xFF5A6070),
    accent = Color(0xFFE3B79A),
)

val SheetBg = Color(0xFFF7F5F1)
val SheetLine = Color(0xFFE2DED8)
val SheetChip = Color(0xFFEAE6E0)
val Charcoal = Color(0xFF262A31)
val Muted = Color(0xFF4E5560)
val Clay = Color(0xFFA8694F)
val DockText = Color(0xFFE8E4DE)
