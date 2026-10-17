package com.woofbattle.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin

/** Seconds since this composable appeared, ticking every frame (drives the manga animations). */
@Composable
private fun rememberSceneTime(): Float {
    var time by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        val start = withFrameMillis { it }
        while (true) withFrameMillis { time = (it - start) / 1000f }
    }
    return time
}

/**
 * Tie scene: the tied dogs lunge at each other and take turns shouting "WOOF!" in jagged manga
 * speech balloons, with anger marks, shaking and speed lines.
 */
@Composable
fun BarkOff(visible: Boolean, dogs: List<Breed>, text: String, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = visible && dogs.isNotEmpty(),
        modifier = modifier,
        enter = scaleIn(spring(dampingRatio = 0.45f, stiffness = 320f), initialScale = 0.6f) + fadeIn(),
        exit = fadeOut(),
    ) {
        val time = rememberSceneTime()
        val measurer = rememberTextMeasurer()
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            drawRect(Color.White.copy(alpha = 0.6f))
            drawSpeedLines(Offset(w / 2, h / 2), size.minDimension * 0.3f, size.maxDimension, Ink.copy(alpha = 0.45f), count = 64, seed = 3)

            val n = dogs.size
            val s = minOf(w / n * 0.8f, h * 0.52f)
            val groundY = h * 0.9f
            class Pose(val cx: Float, val dir: Float, val bark: Float, val dx: Float, val lift: Float)
            val poses = dogs.indices.map { i ->
                val cx = w * (i + 0.5f) / n
                val dir = sign(w / 2 - cx) // lean toward the middle
                // dogs take turns: each bark peaks at a different moment
                val bark = ((sin(2 * PI * 2.0 * time + i * 2 * PI / n) + 1) / 2).toFloat().pow(2)
                val jitter = sin(time * 70f + i * 3f) * s * 0.012f * bark
                Pose(cx, dir, bark, dir * bark * s * 0.12f + jitter, bark * s * 0.05f)
            }
            // balloons first, so each dog covers the tip of its balloon tail
            poses.forEachIndexed { i, p ->
                if (p.bark > 0.25f) {
                    val k = ((p.bark - 0.25f) / 0.75f).coerceIn(0f, 1f)
                    drawSpeechBalloon(
                        measurer, text,
                        center = Offset(p.cx + p.dx + p.dir * s * 0.22f, groundY - s * 1.28f),
                        width = s * (0.75f + 0.25f * k),
                        height = s * (0.42f + 0.12f * k),
                        tailTo = Offset(p.cx + p.dx + p.dir * s * 0.08f, groundY - s * 0.75f),
                        tilt = if (p.dir == 0f) -6f else -p.dir * 6f,
                        seed = i,
                    )
                }
            }
            poses.forEachIndexed { i, p ->
                val pivot = Offset(p.cx + p.dx, groundY - p.lift)
                withTransform({
                    translate(p.dx, -p.lift)
                    rotate(p.dir * (5f + 9f * p.bark), pivot)
                    scale(1f + 0.07f * p.bark, 1f + 0.07f * p.bark, pivot)
                }) {
                    drawBreed(dogs[i], p.cx, groundY, s, bark = p.bark, wag = sin(time * 18f + i) * 20f)
                }
                // little shout lines next to the mouth
                if (p.bark > 0.3f) {
                    val mouth = Offset(p.cx + p.dx, groundY - s * 0.5f)
                    for (k in -1..1) {
                        val a = (if (p.dir == 0f) -PI / 2 else if (p.dir > 0) 0.0 else PI) + k * 0.45
                        val unit = Offset(cos(a).toFloat(), sin(a).toFloat())
                        drawLine(Ink, mouth + unit * (s * 0.28f), mouth + unit * (s * (0.28f + 0.14f * p.bark)), s * 0.018f, StrokeCap.Round)
                    }
                }
                val markX = p.cx + p.dx + if (p.dir == 0f) s * 0.34f else -p.dir * s * 0.34f
                drawAngerMark(Offset(markX, groundY - s * 0.95f), s * (0.13f + 0.06f * p.bark))
            }
        }
    }
}

/**
 * Victory scene: the winner's dog jumps for joy, manga style: squash on landing, stretch in the
 * air, dust puffs, motion lines, twinkling sparkles and a power aura.
 */
@Composable
fun JumpingDog(breed: Breed, modifier: Modifier = Modifier) {
    val time = rememberSceneTime()
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val s = minOf(w, h) * 0.62f
        val cx = w / 2
        val groundY = h * 0.94f
        val period = 1.05f
        val p = (time % period) / period
        val land = 0.2f
        val maxJump = h * 0.3f
        val height: Float
        val sx: Float
        val sy: Float
        if (p < land) {
            val k = sin(p / land * PI).toFloat()
            height = 0f
            sy = 1f - 0.2f * k
            sx = 1f + 0.16f * k
        } else {
            val q = (p - land) / (1f - land)
            height = sin(q * PI).toFloat() * maxJump
            sy = 1f + 0.09f * abs(cos(q * PI)).toFloat()
            sx = 1f / sy
        }
        val air = height / maxJump

        // shadow shrinks as the dog rises
        val sh = s * 0.8f * (1f - 0.45f * air)
        drawOval(Ink.copy(alpha = 0.18f), topLeft = Offset(cx - sh / 2, groundY - sh * 0.06f), size = Size(sh, sh * 0.12f))

        // dust puffs right after landing
        if (p < 0.32f) {
            val k = p / 0.32f
            listOf(-1f, 1f).forEach { side ->
                for (j in 0..2) {
                    val c = Offset(cx + side * s * (0.32f + 0.32f * k + j * 0.07f), groundY - s * (0.03f + 0.05f * j * k))
                    val r = s * (0.07f - 0.015f * j) * (1f - 0.4f * k)
                    drawCircle(Color.White.copy(alpha = 1f - k), r, c)
                    drawCircle(Ink.copy(alpha = 1f - k), r, c, style = Stroke(s * 0.012f))
                }
            }
        }
        // motion lines under the dog while it flies
        if (air > 0.15f) {
            for (j in -1..1) {
                val x = cx + j * s * 0.18f
                val top = groundY - height + s * 0.06f
                drawLine(Ink.copy(alpha = 0.6f * air), Offset(x, top), Offset(x, top + s * 0.25f * air), s * 0.02f, StrokeCap.Round)
            }
        }
        // twinkling sparkles orbiting the dog
        for (j in 0 until 6) {
            val a = time * 1.6f + j * (2 * PI / 6).toFloat()
            val c = Offset(cx + cos(a) * s * 0.78f, groundY - height - s * 0.5f + sin(a) * s * 0.5f)
            val tw = 0.6f + 0.4f * sin(time * 9f + j * 1.3f)
            drawSparkle(c, s * 0.08f * tw, if (j % 2 == 0) Color(0xFFFFD43B) else Color.White)
        }
        val pivot = Offset(cx, groundY - height)
        withTransform({ scale(sx, sy, pivot) }) {
            drawBreed(breed, cx, groundY - height, s, wag = sin(time * 22f) * 32f, happy = true, aura = true, phase = time)
        }
    }
}

/** Jagged manga speech balloon with a tail pointing at [tailTo], holding inked [text]. */
private fun DrawScope.drawSpeechBalloon(
    measurer: TextMeasurer,
    text: String,
    center: Offset,
    width: Float,
    height: Float,
    tailTo: Offset,
    tilt: Float,
    seed: Int,
) {
    rotate(tilt, center) {
        val r = kotlin.random.Random(seed + 40)
        val balloon = Path()
        val points = 18
        for (i in 0 until points * 2) {
            val a = PI * i / points
            val k = if (i % 2 == 0) 1f + r.nextFloat() * 0.12f else 0.8f
            val x = center.x + (width / 2 * k * cos(a)).toFloat()
            val y = center.y + (height / 2 * k * sin(a)).toFloat()
            if (i == 0) balloon.moveTo(x, y) else balloon.lineTo(x, y)
        }
        balloon.close()
        // tail: a sharp wedge from the balloon's edge toward the mouth
        val toward = (tailTo - center).let { it / it.getDistance() }
        val perp = Offset(-toward.y, toward.x)
        val base = center + Offset(toward.x * width * 0.3f, toward.y * height * 0.3f)
        val tail = Path().apply {
            moveTo(base.x + perp.x * width * 0.08f, base.y + perp.y * width * 0.08f)
            lineTo(tailTo.x, tailTo.y)
            lineTo(base.x - perp.x * width * 0.08f, base.y - perp.y * width * 0.08f)
            close()
        }
        val stroke = Stroke(width * 0.03f, join = StrokeJoin.Miter)
        drawPath(tail, Ink, style = stroke)
        drawPath(balloon, Ink, style = stroke)
        drawPath(tail, Color.White)
        drawPath(balloon, Color.White)

        val fontPx = height * 0.42f
        val style = TextStyle(fontSize = (fontPx / density).sp, fontWeight = FontWeight.Black)
        val fill = measurer.measure(text, style.copy(color = Color(0xFFE63946)))
        val outline = measurer.measure(text, style.copy(color = Ink, drawStyle = Stroke(fontPx * 0.14f, join = StrokeJoin.Round)))
        val topLeft = Offset(center.x - fill.size.width / 2f, center.y - fill.size.height / 2f)
        drawText(outline, topLeft = topLeft)
        drawText(fill, topLeft = topLeft)
    }
}

/** Manga "anger vein" mark: four little curved ticks in a cross. */
private fun DrawScope.drawAngerMark(c: Offset, size: Float) {
    val red = Color(0xFFE63946)
    for (q in 0 until 4) {
        rotate(q * 90f + 45f, c) {
            drawArc(red, 200f, 140f, false, topLeft = Offset(c.x - size * 0.35f, c.y - size * 0.9f), size = Size(size * 0.7f, size * 0.7f), style = Stroke(size * 0.22f, cap = StrokeCap.Round))
        }
    }
}

/** Four-pointed twinkle star. */
private fun DrawScope.drawSparkle(c: Offset, r: Float, color: Color) {
    val p = Path().apply {
        moveTo(c.x, c.y - r)
        quadraticTo(c.x, c.y, c.x + r, c.y)
        quadraticTo(c.x, c.y, c.x, c.y + r)
        quadraticTo(c.x, c.y, c.x - r, c.y)
        quadraticTo(c.x, c.y, c.x, c.y - r)
        close()
    }
    drawPath(p, color)
    drawPath(p, Ink, style = Stroke(r * 0.12f, join = StrokeJoin.Round))
}
