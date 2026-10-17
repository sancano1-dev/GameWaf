package com.woofbattle.app.ui

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate

val ValueAccents = listOf(
    Color(0xFFFF8FB1), Color(0xFFFF9F45), Color(0xFFFFCF3D),
    Color(0xFF6BCB77), Color(0xFF4D96FF), Color(0xFF9B6BFF),
)

/**
 * Card face, manga style: an ink-framed panel with halftone dots and speed lines, one dog sized
 * by value standing on the ground line, and dice pips in two corners.
 */
fun DrawScope.drawCardFace(value: Int) {
    val w = size.width
    val h = size.height
    val accent = ValueAccents[value - 1]
    val panel = Path().apply {
        addRoundRect(RoundRect(Rect(Offset(w * 0.055f, h * 0.04f), Size(w * 0.89f, h * 0.92f)), CornerRadius(w * 0.07f)))
    }
    drawRoundRect(Color.White, cornerRadius = CornerRadius(w * 0.1f))
    drawPath(panel, accent.copy(alpha = 0.16f))
    clipPath(panel) {
        drawHalftone(Rect(0f, 0f, w, h * 0.55f), accent.copy(alpha = 0.35f), w * 0.07f)
        drawSpeedLines(Offset(w / 2, h * 0.58f), w * 0.42f, h, accent.copy(alpha = 0.3f), count = 26, seed = value)
    }
    drawPath(panel, Ink, style = Stroke(w * 0.018f))
    drawRoundRect(Ink, cornerRadius = CornerRadius(w * 0.1f), style = Stroke(w * 0.03f))

    val groundY = h * 0.9f
    val dogSize = minOf(h * (0.26f + 0.104f * (value - 1)), w * 0.98f)
    drawOval(Ink.copy(alpha = 0.15f), topLeft = Offset(w * 0.5f - dogSize * 0.4f, groundY - dogSize * 0.035f), size = Size(dogSize * 0.8f, dogSize * 0.07f))
    drawBreed(CardBreeds[value - 1], w * 0.5f, groundY, dogSize)

    val box = w * 0.2f
    drawPips(value, Offset(w * 0.08f, h * 0.055f), box, accent)
    rotate(180f) { drawPips(value, Offset(w * 0.08f, h * 0.055f), box, accent) }
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
    drawRoundRect(accent.copy(alpha = 0.35f), topLeft = topLeft, size = Size(box, box), cornerRadius = CornerRadius(box * 0.2f))
    drawRoundRect(Ink, topLeft = topLeft, size = Size(box, box), cornerRadius = CornerRadius(box * 0.2f), style = Stroke(box * 0.08f))
    val step = box / 3.4f
    PipLayout.getValue(value).forEach { (col, row) ->
        drawCircle(Ink, box * 0.115f, Offset(topLeft.x + box * 0.2f + col * step, topLeft.y + box * 0.2f + row * step))
    }
}

val CardBackColor = Color(0xFF3D5A98)

/** Card back: deep blue with speed lines bursting from a central paw, inked like a manga panel. */
fun DrawScope.drawCardBack() {
    val w = size.width
    val h = size.height
    val shape = Path().apply { addRoundRect(RoundRect(Rect(Offset.Zero, size), CornerRadius(w * 0.1f))) }
    drawPath(shape, CardBackColor)
    clipPath(shape) {
        drawSpeedLines(Offset(w / 2, h / 2), w * 0.28f, h, Color.White.copy(alpha = 0.18f), count = 30, seed = 7)
    }
    drawCircle(Color(0xFFFFD43B), w * 0.22f, Offset(w / 2, h / 2))
    drawCircle(Ink, w * 0.22f, Offset(w / 2, h / 2), style = Stroke(w * 0.025f))
    drawPaw(Offset(w / 2, h * 0.53f), w * 0.3f, Ink)
    drawRoundRect(Color.White.copy(alpha = 0.85f), topLeft = Offset(w * 0.06f, w * 0.06f), size = Size(w * 0.88f, h - w * 0.12f), cornerRadius = CornerRadius(w * 0.07f), style = Stroke(w * 0.03f))
    drawPath(shape, Ink, style = Stroke(w * 0.03f))
}

/** A paw print centered on [c], about [size] wide. */
fun DrawScope.drawPaw(c: Offset, size: Float, color: Color) {
    drawOval(color, topLeft = Offset(c.x - size * 0.32f, c.y - size * 0.05f), size = Size(size * 0.64f, size * 0.5f))
    listOf(-0.36f to -0.22f, -0.13f to -0.42f, 0.13f to -0.42f, 0.36f to -0.22f).forEach { (dx, dy) ->
        drawCircle(color, size * 0.13f, Offset(c.x + dx * size, c.y + dy * size))
    }
}
