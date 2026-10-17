package com.woofbattle.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.min
import com.woofbattle.app.GameUi
import com.woofbattle.engine.GameStatus
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

private class Confetti(rnd: Random) {
    val x = rnd.nextFloat()
    val y0 = rnd.nextFloat()
    val speed = 0.12f + rnd.nextFloat() * 0.25f
    val sway = 1f + rnd.nextFloat() * 3f
    val phase = rnd.nextFloat() * 6.28f
    val spin = 90f + rnd.nextFloat() * 300f
    val color = (PlayerColors + ValueAccents)[rnd.nextInt(PlayerColors.size + ValueAccents.size)]
    val w = 0.6f + rnd.nextFloat() * 0.8f
}

/** Celebration: confetti, the winner(s) wearing a crown, a happy dancing dog, replay and home buttons. */
@Composable
fun VictoryOverlay(ui: GameUi, onReplay: () -> Unit, onHome: () -> Unit) {
    val status = ui.game.status as? GameStatus.Finished ?: return
    var time by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        val start = withFrameMillis { it }
        while (true) withFrameMillis { time = (it - start) / 1000f }
    }
    val confetti = remember { Random(3).let { r -> List(140) { Confetti(r) } } }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(Color(0xCC2B1B10))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { /* block taps */ },
        contentAlignment = Alignment.Center,
    ) {
        val unit = min(maxWidth, maxHeight)
        Canvas(Modifier.fillMaxSize()) {
            // manga victory rays slowly turning behind everything
            rotate(time * 8f, Offset(size.width / 2, size.height * 0.45f)) {
                drawSpeedLines(Offset(size.width / 2, size.height * 0.45f), size.minDimension * 0.18f, size.maxDimension, Color(0xFFFFD43B).copy(alpha = 0.28f), count = 40, seed = 11)
            }
            confetti.forEach { p ->
                val y = (((p.y0 + time * p.speed) % 1.15f) - 0.08f) * size.height
                val x = p.x * size.width + sin(time * p.sway + p.phase) * 24.dp.toPx()
                val piece = 9.dp.toPx() * p.w
                rotate(time * p.spin, Offset(x, y)) {
                    drawRect(p.color, topLeft = Offset(x - piece / 2, y - piece * 0.3f), size = Size(piece, piece * 0.6f))
                }
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(unit * 0.03f)) {
            Row(horizontalArrangement = Arrangement.spacedBy(unit * 0.04f)) {
                status.winners.forEach { seat ->
                    val bob = abs(sin(time * 4f + seat)) * 10f
                    Box(
                        Modifier.size(unit * 0.22f),
                        contentAlignment = Alignment.Center,
                    ) {
                        Canvas(Modifier.fillMaxSize()) {
                            val r = size.minDimension * 0.4f
                            val c = Offset(size.width / 2, size.height * 0.58f - bob)
                            drawCircle(PlayerColors[seat], r * 1.05f, c)
                            drawBreed(ui.dogOf(seat), c.x, c.y + r * 0.85f, r * 1.6f, happy = true)
                            drawCrown(Rect(c.x - r * 0.55f, c.y - r * 1.55f, c.x + r * 0.55f, c.y - r * 0.95f), Color(0xFFFFD43B))
                        }
                    }
                }
            }
            Canvas(Modifier.size(unit * 0.36f)) {
                val hop = abs(sin(time * 5f)) * size.height * 0.08f
                drawBreed(
                    ui.dogOf(status.winners.first()),
                    cx = size.width / 2,
                    groundY = size.height * 0.98f - hop,
                    s = size.height * 0.9f,
                    wag = sin(time * 14f) * 28f,
                    happy = true,
                    aura = true,
                    phase = time,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(unit * 0.08f)) {
                val btn = max(MinTouch, unit * 0.2f)
                BigIconButton(onReplay, Color(0xFF3FB560), "replay", size = btn) { iconReplay(Color.White) }
                BigIconButton(onHome, Color(0xFF4D96FF), "home", size = btn) { iconHouse(Color.White) }
            }
        }
    }
}
