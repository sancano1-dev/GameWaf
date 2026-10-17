package com.woofbattle.app.ui

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

enum class Ears { FLOPPY, LONG, FOLDED, BIG_POINTY, POINTY, POM }
enum class Tail { WAVY, LONG, CURLY, POMPOM, STUB }
enum class FacePattern { PLAIN, BLAZE, DARK_MASK, WHITE_MASK, SPOTS }

/** Manga ink: every shape gets a bold dark outline. */
val Ink = Color(0xFF1E1410)

/**
 * Eight original dogs inspired by breeds children love, drawn in an original shōnen-manga style:
 * bold ink outlines, cel shading, spiky fur tufts, big shiny anime eyes and a confident grin.
 * Each one has its own main colour so a toddler can say "the pink one", "the orange one".
 */
enum class Breed(
    val body: Color,
    val ear: Color,
    val muzzle: Color,
    val chest: Color,
    val accent: Color,
    val collar: Color,
    /** Background of the dog's tile in the picker. */
    val card: Color,
    val ears: Ears,
    val tail: Tail,
    val face: FacePattern,
    /** Anime iris colour. */
    val iris: Color,
    /** Number of spiky fur tufts on top of the head. */
    val tufts: Int,
    val bodyWidth: Float = 0.56f,
    val legLength: Float = 0.2f,
    val tongue: Boolean = false,
    val saddle: Boolean = false,
    val fluffy: Boolean = false,
) {
    GOLDEN(Color(0xFFF7B733), Color(0xFFD48A12), Color(0xFFFFE3A6), Color(0xFFFFE3A6), Color(0xFFD48A12), Color(0xFFE63946), Color(0xFFFFF1C9), Ears.FLOPPY, Tail.WAVY, FacePattern.PLAIN, Color(0xFFC77B1A), 3, tongue = true),
    DALMATIAN(Color(0xFFFFFFFF), Color(0xFF26201E), Color(0xFFFFFFFF), Color(0xFFF2F2F0), Color(0xFF26201E), Color(0xFF3A86FF), Color(0xFFDDEBFF), Ears.FLOPPY, Tail.WAVY, FacePattern.SPOTS, Color(0xFF2F6FDB), 2),
    BEAGLE(Color(0xFFDE9A4E), Color(0xFF7A4A22), Color(0xFFFFFFFF), Color(0xFFFFFFFF), Color(0xFF3A2A20), Color(0xFF2DC653), Color(0xFFE3F6E6), Ears.LONG, Tail.WAVY, FacePattern.BLAZE, Color(0xFF8A5426), 2, tongue = true, saddle = true),
    PUG(Color(0xFFEBD3A3), Color(0xFF3A2E2A), Color(0xFF3A2E2A), Color(0xFFF5E6C8), Color(0xFF3A2E2A), Color(0xFFFF5D8F), Color(0xFFFFE2EC), Ears.FOLDED, Tail.CURLY, FacePattern.DARK_MASK, Color(0xFF5A3A22), 1, legLength = 0.17f),
    CORGI(Color(0xFFF5862F), Color(0xFFF5862F), Color(0xFFFFFFFF), Color(0xFFFFFFFF), Color(0xFFFFFFFF), Color(0xFF4361EE), Color(0xFFFFE6D3), Ears.BIG_POINTY, Tail.STUB, FacePattern.BLAZE, Color(0xFFA0662A), 3, legLength = 0.12f, tongue = true),
    POODLE(Color(0xFFFF9EC7), Color(0xFFFF86B8), Color(0xFFFFD1E5), Color(0xFFFFB6D6), Color(0xFFFFC2DD), Color(0xFF9B5DE5), Color(0xFFF7E3FF), Ears.POM, Tail.POMPOM, FacePattern.PLAIN, Color(0xFF9B5DE5), 0, fluffy = true),
    HUSKY(Color(0xFF7189AB), Color(0xFF566C8C), Color(0xFFFFFFFF), Color(0xFFFFFFFF), Color(0xFFFFFFFF), Color(0xFFFF9F1C), Color(0xFFDDF2FF), Ears.POINTY, Tail.CURLY, FacePattern.WHITE_MASK, Color(0xFF3FA7F5), 4),
    DACHSHUND(Color(0xFFA0552E), Color(0xFF5E2F17), Color(0xFFD08A5E), Color(0xFFD08A5E), Color(0xFF5E2F17), Color(0xFFFFD60A), Color(0xFFF5E6D6), Ears.LONG, Tail.LONG, FacePattern.PLAIN, Color(0xFF7A3E1C), 2, bodyWidth = 0.98f, legLength = 0.1f),
}

/** The dogs on the 1..6 cards (index = value - 1). Size, not breed, decides who wins. */
val CardBreeds = listOf(Breed.PUG, Breed.CORGI, Breed.BEAGLE, Breed.DALMATIAN, Breed.HUSKY, Breed.GOLDEN)

private val ShadowTint = Color(0xFF3A3A7A)

/** Cel-shading tone: a cool, darker version of [c] (works for white dogs too). */
private fun shade(c: Color) = lerp(c, ShadowTint, 0.22f)

/**
 * Draws [breed] sitting, paws on [groundY], centred on [cx], fitting an [s]-by-[s] box.
 * [wag] swings the tail in degrees; [happy] closes the eyes in a big grin; [aura] adds a
 * flickering power aura behind the dog ([phase] animates it). [bark] (0..1) opens the mouth in a
 * shout; above 0.6 the eyes squeeze shut "> <" like a manga yell.
 */
fun DrawScope.drawBreed(
    breed: Breed,
    cx: Float,
    groundY: Float,
    s: Float,
    wag: Float = 0f,
    happy: Boolean = false,
    aura: Boolean = false,
    phase: Float = 0f,
    bark: Float = 0f,
) {
    val b = breed
    val legH = b.legLength
    val bodyY = legH * 0.5f + 0.2f
    val headY = bodyY + 0.42f
    val tuftTop = when (b.tufts) {
        0 -> 0f
        1 -> 0.39f
        else -> 0.47f
    }
    val earTop = maxOf(
        tuftTop,
        when (b.ears) {
            Ears.BIG_POINTY -> 0.46f
            Ears.POINTY -> 0.4f
            else -> if (b.fluffy) 0.42f else 0.32f
        },
    )
    val widthUnits = maxOf(b.bodyWidth + 0.25f, 0.95f)
    val u = minOf(s / (headY + earTop), s / widthUnits)
    val line = 0.024f * u

    fun o(x: Float, y: Float) = Offset(cx + x * u, groundY - y * u)
    fun ovalPath(x: Float, y: Float, w: Float, h: Float) = Path().apply {
        addOval(Rect(o(x - w / 2, y + h / 2), Size(w * u, h * u)))
    }
    fun circlePath(x: Float, y: Float, r: Float) = Path().apply { addOval(Rect(o(x, y), r * u)) }
    fun polyPath(vararg pts: Pair<Float, Float>) = Path().apply {
        pts.forEachIndexed { i, (x, y) -> val q = o(x, y); if (i == 0) moveTo(q.x, q.y) else lineTo(q.x, q.y) }
        close()
    }
    /** Fill, optional cel-shade crescent on the lower right, then the ink outline. */
    fun ink(path: Path, c: Color, shaded: Boolean = false, outline: Float = line) {
        drawPath(path, c)
        if (shaded) {
            clipPath(path) {
                drawPath(path, shade(c))
                translate(-0.06f * u, -0.05f * u) { drawPath(path, c) }
            }
        }
        drawPath(path, Ink, style = Stroke(outline, join = StrokeJoin.Round))
    }
    fun inkOval(c: Color, x: Float, y: Float, w: Float, h: Float, shaded: Boolean = false) = ink(ovalPath(x, y, w, h), c, shaded)
    fun dot(c: Color, x: Float, y: Float, r: Float) = drawCircle(c, r * u, o(x, y))

    val bw = b.bodyWidth

    // ---- Power aura (behind everything)
    if (aura) drawAura(o(0f, (headY + earTop) * 0.5f), 0.6f * u, (headY + earTop) * 0.62f * u, b.collar, phase)

    // ---- Tail
    val tailBase = o(bw / 2 - 0.05f, bodyY - 0.02f)
    rotate(wag, pivot = tailBase) {
        when (b.tail) {
            Tail.WAVY, Tail.LONG -> {
                val p = Path().apply {
                    moveTo(tailBase.x, tailBase.y)
                    val c1 = o(bw / 2 + 0.22f, bodyY - 0.02f)
                    val end = o(bw / 2 + 0.15f, bodyY + if (b.tail == Tail.LONG) 0.22f else 0.32f)
                    quadraticTo(c1.x, c1.y, end.x, end.y)
                }
                val w = if (b.tail == Tail.LONG) 0.06f else 0.095f
                drawPath(p, Ink, style = Stroke(w * u + line * 2, cap = StrokeCap.Round))
                drawPath(p, b.body, style = Stroke(w * u, cap = StrokeCap.Round))
            }
            Tail.CURLY -> {
                val c = o(bw / 2 + 0.03f, bodyY + 0.12f)
                val r = 0.1f * u
                val w = (if (b == Breed.HUSKY) 0.085f else 0.06f) * u
                drawCircle(Ink, r, c, style = Stroke(w + line * 2))
                drawCircle(b.body, r, c, style = Stroke(w))
                if (b == Breed.HUSKY) drawCircle(Color.White, 0.03f * u, Offset(c.x + r, c.y))
            }
            Tail.POMPOM -> {
                val endX = bw / 2 + 0.12f
                val endY = bodyY + 0.26f
                drawLine(Ink, tailBase, o(endX, endY), 0.04f * u + line * 2, StrokeCap.Round)
                drawLine(b.body, tailBase, o(endX, endY), 0.04f * u, StrokeCap.Round)
                ink(circlePath(endX, endY, 0.1f), b.accent)
            }
            Tail.STUB -> inkOval(b.body, bw / 2 - 0.02f, bodyY - 0.02f, 0.14f, 0.14f)
        }
    }

    // ---- Body
    if (b.fluffy) {
        listOf(-0.22f to 0.0f, 0.22f to 0.0f, -0.16f to 0.15f, 0.16f to 0.15f, 0f to -0.12f, -0.2f to -0.12f, 0.2f to -0.12f).forEach { (dx, dy) ->
            ink(circlePath(dx, bodyY + dy, 0.13f), b.accent)
        }
    }
    val bodyPath = ovalPath(0f, bodyY, bw, 0.42f)
    ink(bodyPath, b.body, shaded = true)
    if (b.saddle) {
        clipPath(bodyPath) { drawPath(ovalPath(0f, bodyY + 0.12f, bw * 0.95f, 0.24f), b.accent) }
        drawPath(bodyPath, Ink, style = Stroke(line))
    }
    inkOval(b.chest, 0f, bodyY - 0.03f, 0.26f, 0.3f)
    if (b.face == FacePattern.SPOTS) {
        dot(b.accent, -0.19f, bodyY + 0.05f, 0.045f)
        dot(b.accent, 0.2f, bodyY - 0.07f, 0.035f)
        dot(b.accent, 0.15f, bodyY + 0.12f, 0.03f)
    }

    // ---- Legs and paws
    val legXs = if (bw > 0.8f) listOf(-0.36f, -0.22f, 0.22f, 0.36f) else listOf(-0.1f, 0.1f)
    val pawColor = if (b.face == FacePattern.DARK_MASK) b.chest else b.muzzle
    legXs.forEach { lx ->
        val leg = Path().apply {
            addRoundRect(RoundRect(Rect(o(lx - 0.055f, legH + 0.06f), Size(0.11f * u, (legH + 0.06f) * u)), CornerRadius(0.05f * u)))
        }
        ink(leg, b.body)
        inkOval(pawColor, lx, 0.035f, 0.15f, 0.075f)
    }

    // ---- Collar with tag
    val collar = Path().apply {
        addRoundRect(RoundRect(Rect(o(-0.2f, bodyY + 0.15f), Size(0.4f * u, 0.065f * u)), CornerRadius(0.033f * u)))
    }
    ink(collar, b.collar)
    inkOval(Color(0xFFFFD43B), 0f, bodyY + 0.05f, 0.08f, 0.08f)

    // ---- Spiky fur tufts (behind the head, which covers their base)
    if (b.tufts > 0) {
        // one continuous zig-zag crown of flame-like spikes, wide at the base
        val n = b.tufts + 2
        val spread = 0.1f + 0.035f * n
        val pts = mutableListOf(-spread to headY + 0.12f)
        for (i in 0 until n) {
            val t = (i + 0.5f) / n
            val x = -spread + 2 * spread * t
            val center = 1 - abs(t - 0.5f) * 2
            val tall = if (b.tufts == 1) 0.3f + 0.08f * center else 0.3f + 0.16f * center
            pts += (x + 0.16f * (t - 0.5f)) to headY + tall // tips sweep outward like flames
            if (i < n - 1) pts += (x + spread / n) to headY + 0.22f
        }
        pts += spread to headY + 0.12f
        ink(polyPath(*pts.toTypedArray()), if (b == Breed.HUSKY) b.ear else b.body)
    }

    // ---- Ears behind the head
    when (b.ears) {
        Ears.FLOPPY -> listOf(-1f, 1f).forEach { side ->
            rotate(side * -16f, pivot = o(side * 0.24f, headY + 0.14f)) { inkOval(b.ear, side * 0.27f, headY - 0.03f, 0.16f, 0.32f) }
        }
        Ears.LONG -> listOf(-1f, 1f).forEach { side ->
            rotate(side * -6f, pivot = o(side * 0.25f, headY + 0.14f)) { inkOval(b.ear, side * 0.28f, headY - 0.1f, 0.16f, 0.44f) }
        }
        Ears.BIG_POINTY -> listOf(-1f, 1f).forEach { side ->
            ink(polyPath(side * 0.08f to headY + 0.2f, side * 0.34f to headY + 0.46f, side * 0.31f to headY + 0.06f), b.ear)
            drawPath(polyPath(side * 0.15f to headY + 0.2f, side * 0.3f to headY + 0.37f, side * 0.28f to headY + 0.12f), Color(0xFFFFB3C1))
        }
        Ears.POINTY -> listOf(-1f, 1f).forEach { side ->
            ink(polyPath(side * 0.1f to headY + 0.22f, side * 0.3f to headY + 0.4f, side * 0.3f to headY + 0.07f), b.ear)
            drawPath(polyPath(side * 0.16f to headY + 0.2f, side * 0.27f to headY + 0.32f, side * 0.27f to headY + 0.12f), Color(0xFFFFB3C1))
        }
        Ears.POM -> listOf(-1f, 1f).forEach { side ->
            listOf(0.05f to 0.09f, -0.07f to 0.1f, -0.19f to 0.09f).forEach { (dy, r) ->
                ink(circlePath(side * 0.31f, headY + dy, r), b.ear)
            }
        }
        Ears.FOLDED -> Unit
    }

    // ---- Head
    val headW = if (b == Breed.PUG) 0.68f else 0.6f
    if (b.fluffy) {
        listOf(-0.1f to 0.3f, 0.1f to 0.3f, 0f to 0.37f, 0f to 0.27f).forEach { (dx, dy) ->
            ink(circlePath(dx, headY + dy, 0.11f), b.accent)
        }
    }
    val headPath = ovalPath(0f, headY, headW, 0.58f)
    ink(headPath, b.body, shaded = true)
    clipPath(headPath) {
        when (b.face) {
            FacePattern.BLAZE -> drawPath(Path().apply {
                addRoundRect(RoundRect(Rect(o(-0.045f, headY + 0.3f), Size(0.09f * u, 0.32f * u)), CornerRadius(0.045f * u)))
            }, Color.White)
            FacePattern.WHITE_MASK -> {
                drawPath(ovalPath(0f, headY - 0.1f, 0.54f, 0.38f), Color.White)
                drawPath(Path().apply {
                    addRoundRect(RoundRect(Rect(o(-0.04f, headY + 0.3f), Size(0.08f * u, 0.34f * u)), CornerRadius(0.04f * u)))
                }, Color.White)
            }
            FacePattern.DARK_MASK -> {
                drawPath(ovalPath(-0.12f, headY + 0.03f, 0.2f, 0.19f), b.accent.copy(alpha = 0.85f))
                drawPath(ovalPath(0.12f, headY + 0.03f, 0.2f, 0.19f), b.accent.copy(alpha = 0.85f))
                listOf(0.17f, 0.21f).forEach { wy ->
                    drawArc(Ink.copy(alpha = 0.5f), 200f, 140f, false, topLeft = o(-0.1f, headY + wy + 0.04f), size = Size(0.2f * u, 0.1f * u), style = Stroke(line * 0.8f, cap = StrokeCap.Round))
                }
            }
            FacePattern.SPOTS -> {
                dot(b.accent, 0.15f, headY + 0.16f, 0.04f)
                dot(b.accent, -0.18f, headY - 0.06f, 0.03f)
                dot(b.accent, 0.04f, headY + 0.25f, 0.025f)
            }
            FacePattern.PLAIN -> Unit
        }
    }
    drawPath(headPath, Ink, style = Stroke(line))
    if (b.ears == Ears.FOLDED) listOf(-1f, 1f).forEach { side ->
        rotate(side * 25f, pivot = o(side * 0.24f, headY + 0.2f)) { inkOval(b.ear, side * 0.26f, headY + 0.17f, 0.17f, 0.13f) }
    }

    // ---- Anime eyes with determined brows
    listOf(-1f, 1f).forEach { side ->
        val ex = side * 0.12f
        val shouting = bark > 0.6f
        if (shouting) {
            // squeezed "> <" eyes, pointing toward the nose
            val tipX = ex - side * 0.045f
            drawLine(Ink, o(ex + side * 0.05f, headY + 0.08f), o(tipX, headY + 0.03f), line * 1.6f, StrokeCap.Round)
            drawLine(Ink, o(tipX, headY + 0.03f), o(ex + side * 0.05f, headY - 0.02f), line * 1.6f, StrokeCap.Round)
        } else if (happy) {
            val arc = Path().apply {
                val a = o(ex - 0.06f, headY + 0.01f); val c = o(ex, headY + 0.1f); val e = o(ex + 0.06f, headY + 0.01f)
                moveTo(a.x, a.y); quadraticTo(c.x, c.y, e.x, e.y)
            }
            drawPath(arc, Ink, style = Stroke(line * 1.4f, cap = StrokeCap.Round))
        } else {
            ink(ovalPath(ex, headY + 0.03f, 0.13f, 0.16f), Color.White)
            val irisPath = ovalPath(ex + side * 0.005f, headY + 0.02f, 0.1f, 0.13f)
            drawPath(irisPath, b.iris)
            clipPath(irisPath) {
                drawRect(lerp(b.iris, Ink, 0.45f), topLeft = o(ex - 0.06f, headY + 0.09f), size = Size(0.12f * u, 0.05f * u))
            }
            drawPath(ovalPath(ex + side * 0.005f, headY + 0.015f, 0.05f, 0.07f), Ink)
            dot(Color.White, ex + 0.025f, headY + 0.055f, 0.024f)
            dot(Color.White, ex - 0.02f, headY - 0.01f, 0.011f)
            drawArc(Ink, 200f, 140f, false, topLeft = o(ex - 0.07f, headY + 0.115f), size = Size(0.14f * u, 0.1f * u), style = Stroke(line * 1.3f, cap = StrokeCap.Round))
        }
        // eyebrow: inner end lower = determined; raised when happy
        drawLine(
            Ink,
            o(side * 0.05f, headY + if (bark > 0.05f) 0.12f else if (happy) 0.17f else 0.14f),
            o(side * 0.19f, headY + if (bark > 0.05f) 0.21f else if (happy) 0.17f else 0.2f),
            line * 1.5f,
            StrokeCap.Round,
        )
    }
    // manga blush: little hatch lines on each cheek
    listOf(-1f, 1f).forEach { side ->
        for (k in 0..2) {
            val x = side * (0.17f + k * 0.03f)
            drawLine(Color(0xFFFF6F8A), o(x - 0.012f, headY - 0.1f), o(x + 0.012f, headY - 0.06f), line * 0.8f, StrokeCap.Round)
        }
    }

    // ---- Muzzle, nose, grin with a little fang
    inkOval(b.muzzle, 0f, headY - 0.12f, if (b == Breed.PUG) 0.36f else 0.32f, 0.22f)
    ink(ovalPath(0f, headY - 0.06f, 0.13f, 0.085f), Ink)
    dot(Color.White.copy(alpha = 0.7f), -0.03f, headY - 0.045f, 0.016f)
    val mouthInk = if (b.face == FacePattern.DARK_MASK) Color(0xFFB8A79A) else Ink
    if (bark > 0.05f) {
        // big shouting mouth, opening with [bark]
        val open = 0.08f + 0.16f * bark.coerceAtMost(1f)
        val mouth = ovalPath(0f, headY - 0.13f - open / 2, 0.22f, open)
        drawPath(mouth, Color(0xFF6E1626))
        clipPath(mouth) { drawPath(ovalPath(0f, headY - 0.13f - open * 0.95f, 0.16f, open * 0.6f), Color(0xFFFF6F8A)) }
        drawPath(mouth, Ink, style = Stroke(line, join = StrokeJoin.Round))
        listOf(-1f, 1f).forEach { side ->
            ink(polyPath(side * 0.085f to headY - 0.13f, side * 0.05f to headY - 0.13f, side * 0.07f to headY - 0.13f - open * 0.35f), Color.White, outline = line * 0.6f)
        }
    } else if (happy || b.tongue) {
        val grin = Path().apply {
            val a = o(-0.09f, headY - 0.12f); val c = o(0f, headY - 0.26f); val e = o(0.09f, headY - 0.12f)
            moveTo(a.x, a.y); quadraticTo(c.x, c.y, e.x, e.y); close()
        }
        drawPath(grin, Color(0xFF7A1E2C))
        clipPath(grin) { drawPath(ovalPath(0f, headY - 0.22f, 0.1f, 0.08f), Color(0xFFFF6F8A)) }
        drawPath(grin, mouthInk, style = Stroke(line, join = StrokeJoin.Round))
        ink(polyPath(0.035f to headY - 0.125f, 0.065f to headY - 0.125f, 0.05f to headY - 0.165f), Color.White, outline = line * 0.7f)
    } else {
        val smirk = Path().apply {
            val a = o(-0.08f, headY - 0.13f); val c1 = o(-0.04f, headY - 0.18f); val m = o(0f, headY - 0.1f)
            val c2 = o(0.05f, headY - 0.19f); val e = o(0.1f, headY - 0.11f)
            moveTo(a.x, a.y); quadraticTo(c1.x, c1.y, m.x, m.y); quadraticTo(c2.x, c2.y, e.x, e.y)
        }
        drawPath(smirk, mouthInk, style = Stroke(line * 1.1f, cap = StrokeCap.Round))
        ink(polyPath(0.045f to headY - 0.14f, 0.07f to headY - 0.135f, 0.058f to headY - 0.17f), Color.White, outline = line * 0.6f)
    }
}

/** Flickering flame-like power aura centred on [c] with radii [rx]/[ry]. */
fun DrawScope.drawAura(c: Offset, rx: Float, ry: Float, color: Color, phase: Float) {
    listOf(1.0f to color.copy(alpha = 0.55f), 0.8f to Color.White.copy(alpha = 0.75f)).forEach { (scale, col) ->
        val p = Path()
        val points = 22
        for (i in 0..points * 2) {
            val a = PI * i / points - PI / 2
            val spike = if (i % 2 == 0) 1.1f + 0.08f * sin(phase * 9f + i * 1.7f) else 0.86f
            val lift = if (sin(a) < 0) 1f + 0.25f * (-sin(a)).toFloat() else 1f // taller flames on top
            val x = c.x + (rx * scale * spike * cos(a)).toFloat()
            val y = c.y + (ry * scale * spike * lift * sin(a)).toFloat()
            if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
        }
        p.close()
        drawPath(p, col)
    }
}

/** Manga speed lines radiating from [c], between [inner] and [outer] radius. */
fun DrawScope.drawSpeedLines(c: Offset, inner: Float, outer: Float, color: Color, count: Int = 48, seed: Int = 0) {
    val r = kotlin.random.Random(seed)
    repeat(count) { i ->
        val a = 2 * PI * (i + r.nextFloat() * 0.6f) / count
        val start = inner * (0.85f + r.nextFloat() * 0.4f)
        val w = (0.004f + r.nextFloat() * 0.01f) * outer
        val dir = Offset(cos(a).toFloat(), sin(a).toFloat())
        val perp = Offset(-dir.y, dir.x)
        val s0 = c + dir * start
        val e = c + dir * outer
        val p = Path().apply {
            moveTo(s0.x, s0.y)
            lineTo(e.x + perp.x * w, e.y + perp.y * w)
            lineTo(e.x - perp.x * w, e.y - perp.y * w)
            close()
        }
        drawPath(p, color)
    }
}

/** Halftone screentone: dots that grow toward the top of [rect]. */
fun DrawScope.drawHalftone(rect: Rect, color: Color, step: Float) {
    var y = rect.top + step / 2
    var row = 0
    while (y < rect.bottom) {
        val t = 1f - (y - rect.top) / rect.height
        val r = step * 0.38f * t
        if (r > 0.4f) {
            var x = rect.left + step / 2 + if (row % 2 == 1) step / 2 else 0f
            while (x < rect.right) {
                drawCircle(color, r, Offset(x, y))
                x += step
            }
        }
        y += step
        row++
    }
}
