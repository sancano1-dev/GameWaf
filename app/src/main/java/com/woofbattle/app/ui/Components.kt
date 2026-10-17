package com.woofbattle.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

val PlayerColors = listOf(Color(0xFFFF6B6B), Color(0xFF4D96FF), Color(0xFF3FB560), Color(0xFFFFA62B))
val Cream = Color(0xFFFFF6E5)
val Cocoa = Color(0xFF4A2F1B)

/** Minimum touch target for anything a child can press. */
val MinTouch = 96.dp

@Composable
fun WoofTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(primary = Color(0xFFFF8A3D), background = Cream, surface = Color.White, onSurface = Cocoa),
        content = content,
    )
}

/** Big rounded button that shows a Canvas-drawn icon instead of text. */
@Composable
fun BigIconButton(
    onClick: () -> Unit,
    color: Color,
    description: String,
    modifier: Modifier = Modifier,
    size: Dp = MinTouch,
    icon: DrawScope.() -> Unit,
) {
    Box(
        modifier
            .size(size)
            .shadow(6.dp, CircleShape)
            .clip(CircleShape)
            .background(color)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description }
            .padding(size * 0.22f),
    ) {
        Canvas(Modifier.fillMaxSize()) { icon() }
    }
}

/**
 * Parent gate: must be held for 3 seconds (a ring fills up). A quick tap does nothing, so small
 * children rarely get in.
 */
@Composable
fun ParentGateButton(onUnlock: () -> Unit, description: String, modifier: Modifier = Modifier) {
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    Box(
        modifier
            .size(MinTouch)
            .semantics { contentDescription = description }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown()
                    val job = scope.launch { progress.animateTo(1f, tween(HOLD_MS, easing = LinearEasing)) }
                    val released = withTimeoutOrNull(HOLD_MS.toLong()) { waitForUpOrCancellation() }
                    job.cancel()
                    if (released == null && progress.value > 0.95f) {
                        onUnlock()
                        waitForUpOrCancellation()
                    }
                    scope.launch { progress.snapTo(0f) }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(64.dp)) {
            drawCircle(Color.White.copy(alpha = 0.7f))
            val stroke = size.minDimension * 0.1f
            drawArc(
                Color(0xFFFF8A3D), -90f, 360f * progress.value, false,
                topLeft = Offset(stroke / 2, stroke / 2), size = Size(size.width - stroke, size.height - stroke),
                style = Stroke(stroke),
            )
            inset(size.minDimension * 0.25f) { iconGear(Color(0xFF9A8878)) }
        }
    }
}

private const val HOLD_MS = 3000

/** A card that flips (3D, around Y) from its back to its face when [faceUp] becomes true. */
@Composable
fun PlayingCard(value: Int, faceUp: Boolean, width: Dp, height: Dp, modifier: Modifier = Modifier, highlight: Color? = null) {
    val rotation by animateFloatAsState(if (faceUp) 0f else 180f, tween(450), label = "flip")
    Box(
        modifier
            .size(width, height)
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 14f * density
            },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawRoundRect(Color.Black.copy(alpha = 0.18f), topLeft = Offset(size.width * 0.03f, size.height * 0.025f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.width * 0.1f))
            if (rotation > 90f) {
                drawCardBack()
            } else {
                drawCardFace(value)
                highlight?.let {
                    drawRoundRect(it, cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.width * 0.1f), style = Stroke(size.width * 0.07f))
                }
            }
        }
    }
}

/** A face-down pile drawn as a stack whose height reflects the number of cards. */
@Composable
fun PileStack(count: Int, cardWidth: Dp, cardHeight: Dp, modifier: Modifier = Modifier) {
    val layers = when {
        count <= 0 -> 0
        else -> (count + 3) / 4 // 1..9 layers for up to 36 cards
    }
    val step = cardHeight * 0.035f
    Box(modifier.size(cardWidth + step * 9, cardHeight + step * 9), contentAlignment = Alignment.BottomStart) {
        if (layers == 0) {
            Canvas(Modifier.size(cardWidth, cardHeight)) {
                drawRoundRect(Color.Black.copy(alpha = 0.12f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.width * 0.1f), style = Stroke(size.width * 0.04f))
            }
        }
        for (i in 0 until layers) {
            Canvas(
                Modifier
                    .padding(start = step * i, bottom = step * i)
                    .size(cardWidth, cardHeight),
            ) { drawCardBack() }
        }
    }
}

/** Small numeric badge for parents (children compare stack heights instead). */
@Composable
fun CountBadge(count: Int, modifier: Modifier = Modifier) {
    Box(
        modifier
            .sizeIn(minWidth = 28.dp, minHeight = 22.dp)
            .background(Color.White.copy(alpha = 0.85f), RoundedCornerShape(11.dp))
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text("$count", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Cocoa)
    }
}
