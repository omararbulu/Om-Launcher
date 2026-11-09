package com.omararbulu.omlauncher

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.lerp

/**
 * Un momento del día. Las medidas están en las unidades del diseño (390 x 844)
 * y se escalan al tamaño real de la pantalla al dibujar.
 */
data class Scene(
    val bands: List<Color>, // 5 de cielo, 5 de mar, orilla y suelo (12)
    val sunX: Float,
    val sunY: Float,
    val sunR: Float,
    val sun: Color,
    val moon: Float,
    val stars: Float,
    val cliff: Color,
    val dark: Float,
    val accent: Color,
) {
    val ink: Ink get() = if (dark > 0.5f) LightInk else DarkInk
    val micInk: Color get() = Color(0xFF1E2436).let { if (dark > 0.5f) it else Color(0xFF2E333B) }
    val paper: Paper get() = if (dark > 0.5f) NightPaper else DayPaper
}

private fun colors(vararg hex: Long): List<Color> = hex.map { Color(it) }

private val NIGHT = Scene(
    bands = colors(
        0xFF1E2436, 0xFF252C40, 0xFF2C334A, 0xFF343A52, 0xFF3E4360,
        0xFF1A2030, 0xFF2F3850, 0xFF161B29, 0xFF2F3850, 0xFF121624,
        0xFF1D1C22, 0xFF0F1117,
    ),
    sunX = 288f, sunY = 640f, sunR = 24f, sun = Color(0xFFF7E4D2),
    moon = 1f, stars = 1f, cliff = Color(0xFF15141A), dark = 1f, accent = Color(0xFFE3D6C0),
)

private val DAWN = Scene(
    bands = colors(
        0xFFB9B4CC, 0xFFCDBFD0, 0xFFE3C6C6, 0xFFF0C9B4, 0xFFF6D2A8,
        0xFF8E93AE, 0xFFB7B4C6, 0xFF7C82A0, 0xFFB7B4C6, 0xFF6B7190,
        0xFF6F6170, 0xFF2E2F3A,
    ),
    sunX = 290f, sunY = 550f, sunR = 22f, sun = Color(0xFFFBE3B8),
    moon = 0f, stars = 0f, cliff = Color(0xFF5E5262), dark = 0f, accent = Color(0xFFF6D2A8),
)

private val MORNING = Scene(
    bands = colors(
        0xFFB7CDE3, 0xFFC6D7E6, 0xFFD7E2EA, 0xFFE4EAEE, 0xFFEEF0EE,
        0xFF6D93B8, 0xFFA7C0D6, 0xFF5C83AA, 0xFFA7C0D6, 0xFF4E7299,
        0xFF9A8A76, 0xFF2E333B,
    ),
    sunX = 292f, sunY = 352f, sunR = 22f, sun = Color(0xFFFFF6E0),
    moon = 0f, stars = 0f, cliff = Color(0xFF7E705F), dark = 0f, accent = Color(0xFFEEF0EE),
)

private val NOON = Scene(
    bands = colors(
        0xFF9FC3E6, 0xFFB4D1EC, 0xFFC9DEF1, 0xFFDCE9F4, 0xFFEAF2F7,
        0xFF4C8AC0, 0xFF93BEDF, 0xFF3D78AE, 0xFF93BEDF, 0xFF2F679C,
        0xFFB39C7C, 0xFF2E333B,
    ),
    sunX = 276f, sunY = 176f, sunR = 26f, sun = Color(0xFFFFF9E8),
    moon = 0f, stars = 0f, cliff = Color(0xFF927E62), dark = 0f, accent = Color(0xFFEAF2F7),
)

private val AFTERNOON = Scene(
    bands = colors(
        0xFFC3CDD8, 0xFFCDD2D8, 0xFFDED4CC, 0xFFEACDBA, 0xFFF2BFA2,
        0xFF7F92AE, 0xFFA9B7C9, 0xFF6C7F9C, 0xFFA9B7C9, 0xFF5E7090,
        0xFF8A7868, 0xFF2E333B,
    ),
    sunX = 288f, sunY = 500f, sunR = 26f, sun = Color(0xFFF7E4D2),
    moon = 0f, stars = 0f, cliff = Color(0xFF6E5F53), dark = 0f, accent = Color(0xFFF2BFA2),
)

private val SUNSET = AFTERNOON.copy(sunY = 552f)

private val KEYFRAMES: List<Pair<Float, Scene>> = listOf(
    0f to NIGHT,
    5.3f to NIGHT,
    6.3f to DAWN,
    8.5f to MORNING,
    12f to NOON,
    14.5f to NOON,
    17.2f to AFTERNOON,
    18.2f to SUNSET,
    19.2f to NIGHT,
    24f to NIGHT,
)

private fun lerpF(a: Float, b: Float, t: Float) = a + (b - a) * t
private fun smooth(t: Float) = t * t * (3f - 2f * t)

private fun mix(a: Scene, b: Scene, t: Float) = Scene(
    bands = a.bands.indices.map { lerp(a.bands[it], b.bands[it], t) },
    sunX = lerpF(a.sunX, b.sunX, t),
    sunY = lerpF(a.sunY, b.sunY, t),
    sunR = lerpF(a.sunR, b.sunR, t),
    sun = lerp(a.sun, b.sun, t),
    moon = lerpF(a.moon, b.moon, t),
    stars = lerpF(a.stars, b.stars, t),
    cliff = lerp(a.cliff, b.cliff, t),
    dark = lerpF(a.dark, b.dark, t),
    accent = lerp(a.accent, b.accent, t),
)

/** Escena para una hora del día (0 a 24, con decimales). */
fun sceneAt(hour: Float): Scene {
    for (i in 0 until KEYFRAMES.size - 1) {
        val (h0, s0) = KEYFRAMES[i]
        val (h1, s1) = KEYFRAMES[i + 1]
        if (hour >= h0 && hour <= h1) {
            val t = if (h1 > h0) (hour - h0) / (h1 - h0) else 0f
            return mix(s0, s1, smooth(t))
        }
    }
    return NIGHT
}

private val BAND_HEIGHTS = floatArrayOf(300f, 80f, 70f, 60f, 40f, 24f, 10f, 30f, 8f, 40f, 60f)

private val STARS = listOf(
    Triple(60f, 70f, 1.6f), Triple(150f, 40f, 1.1f), Triple(220f, 250f, 1.6f),
    Triple(330f, 300f, 1.1f), Triple(100f, 330f, 1.1f), Triple(260f, 60f, 1.2f),
    Triple(40f, 210f, 1f), Triple(355f, 110f, 1.3f),
)

private val MoonColor = Color(0xFFE9E4D6)

@Composable
fun Landscape(scene: Scene, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val sy = size.height / 844f
        val sx = size.width / 390f
        var y = 0f
        fun band(i: Int) {
            val h = BAND_HEIGHTS[i] * sy
            drawRect(scene.bands[i], Offset(0f, y), Size(size.width, h + 1f))
            y += h
        }
        for (i in 0..4) band(i)
        // La luna y las estrellas esperan a que el cielo esté oscuro: a media luz se ven como manchas grises.
        val night = ((scene.dark - 0.5f) * 2f).coerceIn(0f, 1f)
        val starAlpha = scene.stars * night
        val moonAlpha = scene.moon * night
        if (starAlpha > 0.01f) {
            STARS.forEach { (x, yy, r) ->
                drawCircle(MoonColor.copy(alpha = starAlpha), r * sy, Offset(x * sx, yy * sy))
            }
        }
        if (moonAlpha > 0.01f) {
            drawCircle(MoonColor.copy(alpha = moonAlpha), 18f * sy, Offset(298f * sx, 158f * sy))
        }
        drawCircle(scene.sun, scene.sunR * sy, Offset(scene.sunX * sx, scene.sunY * sy))
        for (i in 5..10) band(i)
        drawRect(scene.bands[11], Offset(0f, y), Size(size.width, size.height - y))
        val cliff = Path().apply {
            moveTo(0f, 618f * sy)
            lineTo(68f * sx, 628.4f * sy)
            lineTo(119f * sx, 675.2f * sy)
            lineTo(170f * sx, 722f * sy)
            lineTo(0f, 722f * sy)
            close()
        }
        drawPath(cliff, scene.cliff)
    }
}
