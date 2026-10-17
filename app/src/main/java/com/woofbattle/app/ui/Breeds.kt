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

enum class Ears { FLOPPY, LONG, FOLDED, BIG_POINTY, POINTY, POM }
enum class Tail { WAVY, LONG, CURLY, POMPOM, STUB }
enum class FacePattern { PLAIN, BLAZE, DARK_MASK, WHITE_MASK, SPOTS }

/**
 * Eight original, caricatured dogs (big heads, big eyes) inspired by breeds children love.
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
    val bodyWidth: Float = 0.56f,
    val legLength: Float = 0.2f,
    val eye: Color = BreedInk,
    val tongue: Boolean = false,
    val saddle: Boolean = false,
    val fluffy: Boolean = false,
) {
    GOLDEN(Color(0xFFF2B33D), Color(0xFFD18F1F), Color(0xFFFFE0A3), Color(0xFFFFE0A3), Color(0xFFD18F1F), Color(0xFFE63946), Color(0xFFFFF1C9), Ears.FLOPPY, Tail.WAVY, FacePattern.PLAIN, tongue = true),
    DALMATIAN(Color(0xFFFFFFFF), Color(0xFF2B2B2B), Color(0xFFFFFFFF), Color(0xFFF4F4F2), Color(0xFF2B2B2B), Color(0xFF3A86FF), Color(0xFFDDEBFF), Ears.FLOPPY, Tail.WAVY, FacePattern.SPOTS),
    BEAGLE(Color(0xFFD9984F), Color(0xFF7A4A22), Color(0xFFFFFFFF), Color(0xFFFFFFFF), Color(0xFF3A2A20), Color(0xFF2DC653), Color(0xFFE3F6E6), Ears.LONG, Tail.WAVY, FacePattern.BLAZE, tongue = true, saddle = true),
    PUG(Color(0xFFE9D2A6), Color(0xFF3A2E2A), Color(0xFF3A2E2A), Color(0xFFF3E3C3), Color(0xFF3A2E2A), Color(0xFFFF5D8F), Color(0xFFFFE2EC), Ears.FOLDED, Tail.CURLY, FacePattern.DARK_MASK, legLength = 0.17f),
    CORGI(Color(0xFFF08A3C), Color(0xFFF08A3C), Color(0xFFFFFFFF), Color(0xFFFFFFFF), Color(0xFFFFFFFF), Color(0xFF4361EE), Color(0xFFFFE6D3), Ears.BIG_POINTY, Tail.STUB, FacePattern.BLAZE, legLength = 0.12f, tongue = true),
    POODLE(Color(0xFFFF9EC7), Color(0xFFFF86B8), Color(0xFFFFD1E5), Color(0xFFFFB6D6), Color(0xFFFFC2DD), Color(0xFF9B5DE5), Color(0xFFF7E3FF), Ears.POM, Tail.POMPOM, FacePattern.PLAIN, fluffy = true),
    HUSKY(Color(0xFF7D8FA8), Color(0xFF5F708A), Color(0xFFFFFFFF), Color(0xFFFFFFFF), Color(0xFFFFFFFF), Color(0xFFFF9F1C), Color(0xFFDDF2FF), Ears.POINTY, Tail.CURLY, FacePattern.WHITE_MASK, eye = Color(0xFF3FA7F5)),
    DACHSHUND(Color(0xFF9A512C), Color(0xFF5E2F17), Color(0xFFC9845A), Color(0xFFC9845A), Color(0xFF5E2F17), Color(0xFFFFD60A), Color(0xFFF5E6D6), Ears.LONG, Tail.LONG, FacePattern.PLAIN, bodyWidth = 0.98f, legLength = 0.1f),
}

private val BreedInk = Color(0xFF2B1B10)
private val InnerEar = Color(0xFFFFB3C1)
private val Cheek = Color(0x66FF7A8A)

/**
 * Draws [breed] sitting, paws on [groundY], centred on [cx], about [s] tall (ears included).
 * [wag] swings the tail in degrees; [happy] closes the eyes in a smile and shows the tongue.
 */
fun DrawScope.drawBreed(breed: Breed, cx: Float, groundY: Float, s: Float, wag: Float = 0f, happy: Boolean = false) {
    val b = breed
    val legH = b.legLength
    val bodyY = legH * 0.5f + 0.2f
    val headY = bodyY + 0.42f
    val earTop = when (b.ears) {
        Ears.BIG_POINTY -> 0.46f
        Ears.POINTY -> 0.38f
        else -> if (b.fluffy) 0.4f else 0.3f
    }
    val widthUnits = maxOf(b.bodyWidth + 0.25f, 0.95f)
    val u = minOf(s / (headY + earTop), s / widthUnits) // normalise: every breed fits an s-by-s box

    fun o(x: Float, y: Float) = Offset(cx + x * u, groundY - y * u)
    fun oval(c: Color, x: Float, y: Float, w: Float, h: Float) = drawOval(c, topLeft = o(x - w / 2, y + h / 2), size = Size(w * u, h * u))
    fun circle(c: Color, x: Float, y: Float, r: Float) = drawCircle(c, r * u, o(x, y))
    fun path(c: Color, vararg pts: Pair<Float, Float>) {
        val p = Path()
        pts.forEachIndexed { i, (x, y) -> val q = o(x, y); if (i == 0) p.moveTo(q.x, q.y) else p.lineTo(q.x, q.y) }
        p.close()
        drawPath(p, c)
    }

    val bw = b.bodyWidth
    val tailBase = o(bw / 2 - 0.05f, bodyY - 0.02f)

    // ---- Tail (behind the body)
    rotate(wag, pivot = tailBase) {
        when (b.tail) {
            Tail.WAVY, Tail.LONG -> {
                val p = Path().apply {
                    moveTo(tailBase.x, tailBase.y)
                    val c1 = o(bw / 2 + 0.2f, bodyY - 0.02f)
                    val end = o(bw / 2 + 0.14f, bodyY + if (b.tail == Tail.LONG) 0.22f else 0.3f)
                    quadraticTo(c1.x, c1.y, end.x, end.y)
                }
                drawPath(p, b.body, style = Stroke((if (b.tail == Tail.LONG) 0.06f else 0.09f) * u, cap = StrokeCap.Round))
            }
            Tail.CURLY -> {
                val r = 0.075f
                val c = o(bw / 2 + 0.02f, bodyY + 0.12f)
                drawCircle(b.body, (r + 0.035f) * u, c, style = Stroke((if (b.fluffy || b == Breed.HUSKY) 0.09f else 0.06f) * u))
                if (b == Breed.HUSKY) drawCircle(Color.White, 0.03f * u, Offset(c.x + r * u, c.y))
            }
            Tail.POMPOM -> {
                val end = o(bw / 2 + 0.12f, bodyY + 0.26f)
                drawLine(b.body, tailBase, end, 0.04f * u, StrokeCap.Round)
                drawCircle(b.accent, 0.1f * u, end)
                drawCircle(b.body, 0.075f * u, Offset(end.x - 0.02f * u, end.y - 0.02f * u))
            }
            Tail.STUB -> circle(b.body, bw / 2 - 0.02f, bodyY - 0.02f, 0.07f)
        }
    }

    // ---- Body
    if (b.fluffy) {
        listOf(-0.22f to 0.0f, 0.22f to 0.0f, -0.16f to 0.15f, 0.16f to 0.15f, 0f to 0.2f, 0f to -0.12f, -0.2f to -0.12f, 0.2f to -0.12f).forEach { (dx, dy) ->
            circle(b.accent, dx, bodyY + dy, 0.13f)
        }
    }
    oval(b.body, 0f, bodyY, bw, 0.42f)
    if (b.saddle) oval(b.accent, 0f, bodyY + 0.1f, bw * 0.92f, 0.22f)
    oval(b.chest, 0f, bodyY - 0.03f, 0.26f, 0.3f)
    if (b.face == FacePattern.SPOTS) {
        circle(b.accent, -0.19f, bodyY + 0.05f, 0.045f)
        circle(b.accent, 0.2f, bodyY - 0.07f, 0.035f)
        circle(b.accent, 0.15f, bodyY + 0.12f, 0.03f)
        circle(b.accent, -0.12f, bodyY - 0.13f, 0.028f)
    }

    // ---- Legs and paws
    val legXs = if (bw > 0.8f) listOf(-0.36f, -0.22f, 0.22f, 0.36f) else listOf(-0.1f, 0.1f)
    legXs.forEach { lx ->
        drawRoundRect(b.body, topLeft = o(lx - 0.055f, legH + 0.06f), size = Size(0.11f * u, (legH + 0.06f) * u), cornerRadius = CornerRadius(0.05f * u))
        oval(if (b == Breed.PUG) b.chest else b.muzzle.takeIf { b.face != FacePattern.DARK_MASK } ?: b.chest, lx, 0.03f, 0.14f, 0.07f)
    }

    // ---- Collar
    drawRoundRect(b.collar, topLeft = o(-0.2f, bodyY + 0.15f), size = Size(0.4f * u, 0.065f * u), cornerRadius = CornerRadius(0.033f * u))
    circle(Color(0xFFFFD43B), 0f, bodyY + 0.05f, 0.04f)

    // ---- Ears behind the head
    when (b.ears) {
        Ears.FLOPPY -> listOf(-1f, 1f).forEach { side ->
            rotate(side * -16f, pivot = o(side * 0.24f, headY + 0.14f)) { oval(b.ear, side * 0.27f, headY - 0.03f, 0.16f, 0.32f) }
        }
        Ears.LONG -> listOf(-1f, 1f).forEach { side ->
            rotate(side * -6f, pivot = o(side * 0.25f, headY + 0.14f)) { oval(b.ear, side * 0.28f, headY - 0.1f, 0.16f, 0.44f) }
        }
        Ears.BIG_POINTY -> listOf(-1f, 1f).forEach { side ->
            path(b.ear, side * 0.08f to headY + 0.2f, side * 0.33f to headY + 0.46f, side * 0.31f to headY + 0.06f)
            path(InnerEar, side * 0.14f to headY + 0.2f, side * 0.3f to headY + 0.38f, side * 0.28f to headY + 0.12f)
        }
        Ears.POINTY -> listOf(-1f, 1f).forEach { side ->
            path(b.ear, side * 0.1f to headY + 0.22f, side * 0.27f to headY + 0.38f, side * 0.29f to headY + 0.08f)
            path(InnerEar, side * 0.15f to headY + 0.21f, side * 0.25f to headY + 0.31f, side * 0.26f to headY + 0.13f)
        }
        Ears.POM -> listOf(-1f, 1f).forEach { side ->
            circle(b.ear, side * 0.3f, headY + 0.05f, 0.09f)
            circle(b.ear, side * 0.32f, headY - 0.07f, 0.1f)
            circle(b.ear, side * 0.29f, headY - 0.19f, 0.09f)
        }
        Ears.FOLDED -> Unit // drawn on top of the head below
    }

    // ---- Head
    val headW = if (b == Breed.PUG) 0.68f else 0.6f
    if (b.fluffy) {
        listOf(-0.1f, 0f, 0.1f).forEach { dx -> circle(b.accent, dx, headY + 0.3f, 0.11f) }
        circle(b.accent, 0f, headY + 0.36f, 0.1f)
    }
    oval(b.body, 0f, headY, headW, 0.58f)

    // Face patterns
    when (b.face) {
        FacePattern.BLAZE -> {
            drawRoundRect(Color.White, topLeft = o(-0.045f, headY + 0.28f), size = Size(0.09f * u, 0.3f * u), cornerRadius = CornerRadius(0.045f * u))
        }
        FacePattern.WHITE_MASK -> {
            oval(Color.White, 0f, headY - 0.1f, 0.52f, 0.36f)
            drawRoundRect(Color.White, topLeft = o(-0.04f, headY + 0.26f), size = Size(0.08f * u, 0.3f * u), cornerRadius = CornerRadius(0.04f * u))
            circle(Color.White, -0.12f, headY + 0.12f, 0.035f)
            circle(Color.White, 0.12f, headY + 0.12f, 0.035f)
        }
        FacePattern.DARK_MASK -> {
            circle(b.accent.copy(alpha = 0.85f), -0.12f, headY + 0.03f, 0.085f)
            circle(b.accent.copy(alpha = 0.85f), 0.12f, headY + 0.03f, 0.085f)
            // forehead wrinkles
            listOf(0.17f, 0.21f).forEach { wy ->
                drawArc(b.ear.copy(alpha = 0.45f), 200f, 140f, false, topLeft = o(-0.1f, headY + wy + 0.04f), size = Size(0.2f * u, 0.1f * u), style = Stroke(0.018f * u, cap = StrokeCap.Round))
            }
        }
        FacePattern.SPOTS -> {
            circle(b.accent, 0.15f, headY + 0.16f, 0.04f)
            circle(b.accent, -0.18f, headY - 0.06f, 0.03f)
            circle(b.accent, 0.04f, headY + 0.24f, 0.025f)
        }
        FacePattern.PLAIN -> Unit
    }
    if (b.ears == Ears.FOLDED) listOf(-1f, 1f).forEach { side ->
        rotate(side * 25f, pivot = o(side * 0.24f, headY + 0.2f)) { oval(b.ear, side * 0.26f, headY + 0.17f, 0.17f, 0.13f) }
    }

    // Eyes (big, caricature)
    listOf(-0.12f, 0.12f).forEach { ex ->
        if (happy) {
            val arc = Path().apply {
                val a = o(ex - 0.055f, headY + 0.02f)
                val c = o(ex, headY + 0.09f)
                val e = o(ex + 0.055f, headY + 0.02f)
                moveTo(a.x, a.y); quadraticTo(c.x, c.y, e.x, e.y)
            }
            drawPath(arc, BreedInk, style = Stroke(0.026f * u, cap = StrokeCap.Round))
        } else {
            circle(Color.White, ex, headY + 0.03f, 0.068f)
            circle(b.eye, ex, headY + 0.025f, 0.052f)
            if (b.eye != BreedInk) circle(BreedInk, ex, headY + 0.025f, 0.025f)
            circle(Color.White, ex + 0.02f, headY + 0.05f, 0.02f)
            circle(Color.White, ex - 0.018f, headY + 0.005f, 0.009f)
        }
    }
    circle(Cheek, -0.21f, headY - 0.07f, 0.045f)
    circle(Cheek, 0.21f, headY - 0.07f, 0.045f)

    // Muzzle, nose, mouth, tongue
    oval(b.muzzle, 0f, headY - 0.12f, if (b == Breed.PUG) 0.36f else 0.32f, 0.22f)
    oval(BreedInk, 0f, headY - 0.06f, 0.13f, 0.085f)
    circle(Color.White.copy(alpha = 0.55f), -0.025f, headY - 0.045f, 0.015f)
    val mouthColor = if (b == Breed.PUG) Color(0xFF14100E) else BreedInk
    val mouth = Path().apply {
        val a = o(-0.08f, headY - 0.13f)
        val c1 = o(-0.04f, headY - 0.18f)
        val m = o(0f, headY - 0.1f)
        val c2 = o(0.04f, headY - 0.18f)
        val e = o(0.08f, headY - 0.13f)
        moveTo(a.x, a.y); quadraticTo(c1.x, c1.y, m.x, m.y); quadraticTo(c2.x, c2.y, e.x, e.y)
    }
    if (b.tongue || happy) oval(Color(0xFFFF6F8A), 0f, headY - 0.18f, 0.08f, 0.1f)
    drawPath(mouth, if (b == Breed.PUG) Color(0xFF8C7A6E) else mouthColor, style = Stroke(0.018f * u, cap = StrokeCap.Round))
}
