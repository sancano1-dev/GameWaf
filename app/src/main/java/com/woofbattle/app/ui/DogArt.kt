package com.woofbattle.app.ui

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform

enum class EarType { FLOPPY, POINTY }

/** Parameters for one of the six original dog designs. */
data class DogStyle(
    val body: Color,
    val ear: Color,
    val muzzle: Color,
    val chest: Color,
    val earType: EarType,
    val eye: Color = Color(0xFF2B1B10),
    val spots: Color? = null,
    val eyePatch: Color? = null,
    val collar: Color? = null,
    val tongue: Boolean = false,
)

private val Ink = Color(0xFF2B1B10)
private val Blush = Color(0x66FF7A8A)

/** Index = card value - 1. Size on the card grows with value; the designs keep each value recognizable too. */
val DogStyles = listOf(
    // 1: tiny cream pup with pointy ears
    DogStyle(Color(0xFFF7E3C3), Color(0xFFE2B98A), Color(0xFFFFFAF0), Color(0xFFFFFAF0), EarType.POINTY, collar = Color(0xFFFF8FB1)),
    // 2: tan hound with brown floppy ears
    DogStyle(Color(0xFFD08F4F), Color(0xFF6B3E1E), Color(0xFFFFF6E8), Color(0xFFFFF6E8), EarType.FLOPPY, eyePatch = Color(0xFF8A5530)),
    // 3: white spotty dog
    DogStyle(Color(0xFFFDFDFB), Color(0xFF2E2E2E), Color(0xFFFFFFFF), Color(0xFFF1F1EE), EarType.FLOPPY, spots = Color(0xFF2E2E2E), collar = Color(0xFF4D96FF)),
    // 4: golden fluffy dog
    DogStyle(Color(0xFFE7A83E), Color(0xFFC0801F), Color(0xFFF8DDA8), Color(0xFFF8DDA8), EarType.FLOPPY, tongue = true),
    // 5: grey snow dog with blue eyes
    DogStyle(Color(0xFF8792A3), Color(0xFF5E6878), Color(0xFFFFFFFF), Color(0xFFFFFFFF), EarType.POINTY, eye = Color(0xFF3A8DDE), collar = Color(0xFF6BCB77)),
    // 6: huge chocolate dog with red collar
    DogStyle(Color(0xFF7A4A2A), Color(0xFF52301A), Color(0xFFD9B48C), Color(0xFFD9B48C), EarType.FLOPPY, collar = Color(0xFFE63946), tongue = true),
)

val ValueAccents = listOf(
    Color(0xFFFF8FB1), Color(0xFFFF9F45), Color(0xFFFFCF3D),
    Color(0xFF6BCB77), Color(0xFF4D96FF), Color(0xFF9B6BFF),
)

/**
 * Draws a sitting dog whose paws rest on [groundY], horizontally centered on [cx]. The drawing is
 * about [s] tall and 0.8 * [s] wide. [wag] rotates the tail in degrees; [bounce] lifts the head.
 */
fun DrawScope.drawDog(style: DogStyle, cx: Float, groundY: Float, s: Float, wag: Float = 0f, happy: Boolean = false) {
    fun o(x: Float, y: Float) = Offset(cx + x * s, groundY - y * s)
    fun sz(w: Float, h: Float) = Size(w * s, h * s)
    fun ovalAt(color: Color, x: Float, y: Float, w: Float, h: Float) =
        drawOval(color, topLeft = o(x - w / 2, y + h / 2), size = sz(w, h))

    // Tail (behind the body)
    rotate(wag, pivot = o(0.22f, 0.25f)) {
        val tail = Path().apply {
            moveTo(o(0.2f, 0.22f).x, o(0.2f, 0.22f).y)
            quadraticTo(o(0.42f, 0.22f).x, o(0.42f, 0.22f).y, o(0.36f, 0.5f).x, o(0.36f, 0.5f).y)
        }
        drawPath(tail, style.body, style = Stroke(width = 0.09f * s, cap = StrokeCap.Round))
        drawPath(tail, style.ear.copy(alpha = 0.25f), style = Stroke(width = 0.03f * s, cap = StrokeCap.Round))
    }

    // Haunches, body, chest
    ovalAt(style.body, -0.25f, 0.12f, 0.24f, 0.24f)
    ovalAt(style.body, 0.25f, 0.12f, 0.24f, 0.24f)
    ovalAt(style.body, 0f, 0.28f, 0.6f, 0.55f)
    ovalAt(style.chest, 0f, 0.32f, 0.3f, 0.36f)
    style.spots?.let {
        drawCircle(it, 0.05f * s, o(-0.2f, 0.32f))
        drawCircle(it, 0.035f * s, o(0.22f, 0.2f))
        drawCircle(it, 0.04f * s, o(0.17f, 0.42f))
    }

    // Front legs and paws
    listOf(-0.11f, 0.11f).forEach { lx ->
        drawRoundRect(style.body, topLeft = o(lx - 0.06f, 0.3f), size = sz(0.12f, 0.28f), cornerRadius = CornerRadius(0.06f * s))
        ovalAt(style.muzzle, lx, 0.035f, 0.15f, 0.08f)
    }

    // Collar
    style.collar?.let {
        drawRoundRect(it, topLeft = o(-0.17f, 0.5f), size = sz(0.34f, 0.06f), cornerRadius = CornerRadius(0.03f * s))
        drawCircle(Color(0xFFFFD43B), 0.035f * s, o(0f, 0.43f))
    }

    // Head
    val headY = if (happy) 0.7f else 0.68f
    val earBack = style.ear
    if (style.earType == EarType.FLOPPY) {
        listOf(-1f, 1f).forEach { side ->
            withTransform({ rotate(side * -18f, pivot = o(side * 0.2f, headY + 0.15f)) }) {
                ovalAt(earBack, side * 0.24f, headY + 0.0f, 0.15f, 0.32f)
            }
        }
    } else {
        // Upright, rounded ears (shiba / corgi style) set wide on the head
        listOf(-1f, 1f).forEach { side ->
            withTransform({ rotate(side * 24f, pivot = o(side * 0.17f, headY + 0.2f)) }) {
                ovalAt(earBack, side * 0.17f, headY + 0.25f, 0.17f, 0.27f)
                ovalAt(Color(0xFFFFB3C1), side * 0.17f, headY + 0.24f, 0.08f, 0.16f)
            }
        }
    }
    drawCircle(style.body, 0.25f * s, o(0f, headY))
    style.spots?.let {
        drawCircle(it, 0.035f * s, o(0.12f, headY + 0.14f))
        drawCircle(it, 0.025f * s, o(-0.15f, headY - 0.1f))
    }
    style.eyePatch?.let { ovalAt(it, 0.1f, headY + 0.06f, 0.15f, 0.15f) }

    // Eyes
    listOf(-0.1f, 0.1f).forEach { ex ->
        if (happy) {
            val arc = Path().apply {
                moveTo(o(ex - 0.045f, headY + 0.05f).x, o(ex - 0.045f, headY + 0.05f).y)
                quadraticTo(o(ex, headY + 0.11f).x, o(ex, headY + 0.11f).y, o(ex + 0.045f, headY + 0.05f).x, o(ex + 0.045f, headY + 0.05f).y)
            }
            drawPath(arc, Ink, style = Stroke(width = 0.022f * s, cap = StrokeCap.Round))
        } else {
            drawCircle(Ink, 0.042f * s, o(ex, headY + 0.06f))
            if (style.eye != Ink) drawCircle(style.eye, 0.03f * s, o(ex, headY + 0.06f))
            drawCircle(Ink, 0.017f * s, o(ex, headY + 0.06f))
            drawCircle(Color.White, 0.014f * s, o(ex + 0.013f, headY + 0.075f))
        }
    }
    drawCircle(Blush, 0.04f * s, o(-0.17f, headY - 0.04f))
    drawCircle(Blush, 0.04f * s, o(0.17f, headY - 0.04f))

    // Muzzle, nose, mouth, tongue
    ovalAt(style.muzzle, 0f, headY - 0.09f, 0.32f, 0.21f)
    ovalAt(Ink, 0f, headY - 0.03f, 0.12f, 0.08f)
    drawCircle(Color.White.copy(alpha = 0.6f), 0.012f * s, o(-0.015f, headY - 0.015f))
    val mouth = Path().apply {
        moveTo(o(-0.07f, headY - 0.1f).x, o(-0.07f, headY - 0.1f).y)
        quadraticTo(o(-0.035f, headY - 0.14f).x, o(-0.035f, headY - 0.14f).y, o(0f, headY - 0.07f).x, o(0f, headY - 0.07f).y)
        quadraticTo(o(0.035f, headY - 0.14f).x, o(0.035f, headY - 0.14f).y, o(0.07f, headY - 0.1f).x, o(0.07f, headY - 0.1f).y)
    }
    if (style.tongue || happy) ovalAt(Color(0xFFFF6F8A), 0f, headY - 0.14f, 0.07f, 0.08f)
    drawPath(mouth, Ink, style = Stroke(width = 0.016f * s, cap = StrokeCap.Round))
}

/** Card face: one dog standing on a ground line, sized by value, with dice pips in two corners. */
fun DrawScope.drawCardFace(value: Int) {
    val w = size.width
    val h = size.height
    val r = CornerRadius(w * 0.1f)
    val accent = ValueAccents[value - 1]
    drawRoundRect(Color.White, cornerRadius = r)
    drawRoundRect(accent.copy(alpha = 0.18f), topLeft = Offset(w * 0.05f, h * 0.04f), size = Size(w * 0.9f, h * 0.92f), cornerRadius = CornerRadius(w * 0.07f))
    drawRoundRect(accent, cornerRadius = r, style = Stroke(width = w * 0.035f))

    val groundY = h * 0.9f
    val dogSize = h * (0.24f + 0.112f * (value - 1))
    drawOval(Color.Black.copy(alpha = 0.1f), topLeft = Offset(w * 0.5f - dogSize * 0.42f, groundY - dogSize * 0.04f), size = Size(dogSize * 0.84f, dogSize * 0.08f))
    drawDog(DogStyles[value - 1], w * 0.5f, groundY, dogSize)

    val box = w * 0.2f
    drawPips(value, Offset(w * 0.07f, h * 0.05f), box, accent)
    rotate(180f) { drawPips(value, Offset(w * 0.07f, h * 0.05f), box, accent) }
}

private val PipLayout = mapOf(
    1 to listOf(1 to 1),
    2 to listOf(0 to 0, 2 to 2),
    3 to listOf(0 to 0, 1 to 1, 2 to 2),
    4 to listOf(0 to 0, 2 to 0, 0 to 2, 2 to 2),
    5 to listOf(0 to 0, 2 to 0, 1 to 1, 0 to 2, 2 to 2),
    6 to listOf(0 to 0, 0 to 1, 0 to 2, 2 to 0, 2 to 1, 2 to 2),
)

private fun DrawScope.drawPips(value: Int, topLeft: Offset, box: Float, accent: Color) {
    drawRoundRect(Color.White, topLeft = topLeft, size = Size(box, box), cornerRadius = CornerRadius(box * 0.2f))
    drawRoundRect(accent, topLeft = topLeft, size = Size(box, box), cornerRadius = CornerRadius(box * 0.2f), style = Stroke(box * 0.06f))
    val step = box / 3.4f
    PipLayout.getValue(value).forEach { (col, row) ->
        drawCircle(Ink, box * 0.11f, Offset(topLeft.x + box * 0.2f + col * step, topLeft.y + box * 0.2f + row * step))
    }
}

val CardBackColor = Color(0xFF3D5A98)

fun DrawScope.drawCardBack() {
    val w = size.width
    val h = size.height
    drawRoundRect(CardBackColor, cornerRadius = CornerRadius(w * 0.1f))
    drawRoundRect(Color.White.copy(alpha = 0.85f), topLeft = Offset(w * 0.06f, w * 0.06f), size = Size(w * 0.88f, h - w * 0.12f), cornerRadius = CornerRadius(w * 0.07f), style = Stroke(w * 0.03f))
    val paw = Color.White.copy(alpha = 0.3f)
    for (row in 0..2) for (col in 0..1) {
        val x = w * (0.3f + col * 0.4f) + if (row % 2 == 1) w * 0.0f else 0f
        val y = h * (0.2f + row * 0.3f) + if (col == 1) h * 0.12f else 0f
        drawPaw(Offset(x, y), w * 0.22f, paw)
    }
}

/** A paw print centered on [c], about [size] wide. */
fun DrawScope.drawPaw(c: Offset, size: Float, color: Color) {
    drawOval(color, topLeft = Offset(c.x - size * 0.32f, c.y - size * 0.05f), size = Size(size * 0.64f, size * 0.5f))
    listOf(-0.36f to -0.22f, -0.13f to -0.42f, 0.13f to -0.42f, 0.36f to -0.22f).forEach { (dx, dy) ->
        drawCircle(color, size * 0.13f, Offset(c.x + dx * size, c.y + dy * size))
    }
}
