package com.woofbattle.app.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.min

/* Simple icons drawn on Canvas, so the app needs no icon library or image assets. */

private val DrawScope.u: Float get() = min(size.width, size.height)
private val DrawScope.c: Offset get() = Offset(size.width / 2, size.height / 2)

fun DrawScope.iconHouse(color: Color) {
    val s = u
    val o = Offset(c.x - s / 2, c.y - s / 2)
    val roof = Path().apply {
        moveTo(o.x + s * 0.1f, o.y + s * 0.5f); lineTo(o.x + s * 0.5f, o.y + s * 0.12f); lineTo(o.x + s * 0.9f, o.y + s * 0.5f)
    }
    drawPath(roof, color, style = Stroke(s * 0.1f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    val body = Path().apply {
        moveTo(o.x + s * 0.22f, o.y + s * 0.45f); lineTo(o.x + s * 0.22f, o.y + s * 0.88f); lineTo(o.x + s * 0.78f, o.y + s * 0.88f); lineTo(o.x + s * 0.78f, o.y + s * 0.45f)
    }
    drawPath(body, color, style = Stroke(s * 0.1f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    drawRect(color, topLeft = Offset(o.x + s * 0.42f, o.y + s * 0.62f), size = Size(s * 0.16f, s * 0.26f))
}

fun DrawScope.iconReplay(color: Color) {
    val s = u
    val r = s * 0.32f
    drawArc(color, startAngle = -60f, sweepAngle = 300f, useCenter = false, topLeft = Offset(c.x - r, c.y - r), size = Size(r * 2, r * 2), style = Stroke(s * 0.11f, cap = StrokeCap.Round))
    val tip = Path().apply {
        val ax = c.x + r * 0.5f
        val ay = c.y - r * 0.87f
        moveTo(ax + s * 0.2f, ay - s * 0.02f); lineTo(ax - s * 0.06f, ay - s * 0.16f); lineTo(ax - s * 0.02f, ay + s * 0.15f); close()
    }
    drawPath(tip, color)
}

fun DrawScope.iconSpeaker(color: Color, on: Boolean) {
    val s = u
    val o = Offset(c.x - s / 2, c.y - s / 2)
    val body = Path().apply {
        moveTo(o.x + s * 0.12f, o.y + s * 0.38f); lineTo(o.x + s * 0.3f, o.y + s * 0.38f); lineTo(o.x + s * 0.52f, o.y + s * 0.18f)
        lineTo(o.x + s * 0.52f, o.y + s * 0.82f); lineTo(o.x + s * 0.3f, o.y + s * 0.62f); lineTo(o.x + s * 0.12f, o.y + s * 0.62f); close()
    }
    drawPath(body, color, style = Stroke(s * 0.04f, join = StrokeJoin.Round))
    drawPath(body, color)
    if (on) {
        listOf(0.16f, 0.28f).forEach { rr ->
            val r = s * rr
            drawArc(color, -45f, 90f, false, topLeft = Offset(o.x + s * 0.52f - r, c.y - r), size = Size(r * 2, r * 2), style = Stroke(s * 0.07f, cap = StrokeCap.Round))
        }
    } else {
        val a = Offset(o.x + s * 0.64f, o.y + s * 0.36f)
        val b = Offset(o.x + s * 0.9f, o.y + s * 0.64f)
        drawLine(color, a, b, s * 0.08f, StrokeCap.Round)
        drawLine(color, Offset(a.x, b.y), Offset(b.x, a.y), s * 0.08f, StrokeCap.Round)
    }
}

fun DrawScope.iconGear(color: Color) {
    val s = u
    for (i in 0 until 8) {
        rotate(i * 45f, c) {
            drawRect(color, topLeft = Offset(c.x - s * 0.08f, c.y - s * 0.44f), size = Size(s * 0.16f, s * 0.2f))
        }
    }
    drawCircle(color, s * 0.31f, c)
    drawCircle(Color.White, s * 0.13f, c)
}

fun DrawScope.iconPlay(color: Color) {
    val s = u
    val p = Path().apply {
        moveTo(c.x - s * 0.22f, c.y - s * 0.32f); lineTo(c.x + s * 0.32f, c.y); lineTo(c.x - s * 0.22f, c.y + s * 0.32f); close()
    }
    drawPath(p, color, style = Stroke(s * 0.08f, join = StrokeJoin.Round))
    drawPath(p, color)
}

fun DrawScope.iconBack(color: Color) {
    val s = u
    drawLine(color, Offset(c.x + s * 0.3f, c.y), Offset(c.x - s * 0.28f, c.y), s * 0.12f, StrokeCap.Round)
    val head = Path().apply {
        moveTo(c.x - s * 0.02f, c.y - s * 0.28f); lineTo(c.x - s * 0.3f, c.y); lineTo(c.x - s * 0.02f, c.y + s * 0.28f)
    }
    drawPath(head, color, style = Stroke(s * 0.12f, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

fun DrawScope.iconCheck(color: Color) {
    val s = u
    val p = Path().apply {
        moveTo(c.x - s * 0.3f, c.y); lineTo(c.x - s * 0.08f, c.y + s * 0.22f); lineTo(c.x + s * 0.32f, c.y - s * 0.24f)
    }
    drawPath(p, color, style = Stroke(s * 0.13f, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

fun DrawScope.iconClose(color: Color) {
    val s = u
    drawLine(color, Offset(c.x - s * 0.28f, c.y - s * 0.28f), Offset(c.x + s * 0.28f, c.y + s * 0.28f), s * 0.12f, StrokeCap.Round)
    drawLine(color, Offset(c.x - s * 0.28f, c.y + s * 0.28f), Offset(c.x + s * 0.28f, c.y - s * 0.28f), s * 0.12f, StrokeCap.Round)
}

/** A smiling child's face in [color] (used for "human" seats and the home tiles). */
fun DrawScope.drawKidFace(center: Offset, radius: Float, color: Color) {
    drawCircle(Ink, radius * 1.06f, center)
    drawCircle(Color(0xFFFFD9B5), radius, center)
    drawArc(color, 180f, 180f, true, topLeft = Offset(center.x - radius * 1.02f, center.y - radius * 1.05f), size = Size(radius * 2.04f, radius * 1.3f))
    drawCircle(Color(0xFF2B1B10), radius * 0.1f, Offset(center.x - radius * 0.35f, center.y + radius * 0.05f))
    drawCircle(Color(0xFF2B1B10), radius * 0.1f, Offset(center.x + radius * 0.35f, center.y + radius * 0.05f))
    drawArc(Color(0xFF2B1B10), 20f, 140f, false, topLeft = Offset(center.x - radius * 0.4f, center.y - radius * 0.05f), size = Size(radius * 0.8f, radius * 0.7f), style = Stroke(radius * 0.1f, cap = StrokeCap.Round))
    drawCircle(Color(0x55FF7A8A), radius * 0.14f, Offset(center.x - radius * 0.6f, center.y + radius * 0.35f))
    drawCircle(Color(0x55FF7A8A), radius * 0.14f, Offset(center.x + radius * 0.6f, center.y + radius * 0.35f))
}

fun DrawScope.drawCrown(rect: Rect, color: Color) {
    val p = Path().apply {
        moveTo(rect.left, rect.bottom)
        lineTo(rect.left, rect.top + rect.height * 0.3f)
        lineTo(rect.left + rect.width * 0.25f, rect.top + rect.height * 0.6f)
        lineTo(rect.left + rect.width * 0.5f, rect.top)
        lineTo(rect.left + rect.width * 0.75f, rect.top + rect.height * 0.6f)
        lineTo(rect.right, rect.top + rect.height * 0.3f)
        lineTo(rect.right, rect.bottom)
        close()
    }
    drawPath(p, color)
    drawPath(p, Color(0xFFB7791F), style = Stroke(rect.width * 0.04f, join = StrokeJoin.Round))
}
