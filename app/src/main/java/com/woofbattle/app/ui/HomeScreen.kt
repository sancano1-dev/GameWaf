package com.woofbattle.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.woofbattle.app.Mode
import com.woofbattle.app.Settings
import com.woofbattle.app.Strings

@Composable
fun HomeScreen(
    settings: Settings,
    strings: Strings,
    onStart: (mode: Mode, players: Int) -> Unit,
    onToggleSound: () -> Unit,
    onOpenParent: () -> Unit,
    onChooseDog: (Breed) -> Unit = {},
    initialPicking: Mode? = null,
    initialChoosingDog: Boolean = false,
) {
    val wag = rememberInfiniteTransition(label = "wag")
    val t by wag.animateFloat(0f, 1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "t")
    /** Mode whose player-count picker is open, or null for the main menu. */
    var picking by rememberSaveable { mutableStateOf(initialPicking) }
    var choosingDog by rememberSaveable { mutableStateOf(initialChoosingDog) }
    BackHandler(enabled = picking != null) { picking = null }

    if (choosingDog) {
        DogPicker(settings.favoriteDog, strings, onSelect = onChooseDog, onDone = { choosingDog = false })
        return
    }

    val fav = settings.favoriteDog
    BoxWithConstraints(Modifier.fillMaxSize()) {
        DogParkBackground()
        val landscape = maxWidth > maxHeight

        // Header: my dog (tap to change it) above a bone-shaped title plate.
        val header: @Composable (Dp) -> Unit = { dogSize ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier
                        .size(max(dogSize, MinTouch))
                        .clickable(role = Role.Button) { choosingDog = true }
                        .semantics { contentDescription = strings.chooseDog },
                ) {
                    Canvas(Modifier.fillMaxSize()) {
                        val s = size.minDimension
                        drawCircle(fav.card, s * 0.47f)
                        drawCircle(fav.collar.copy(alpha = 0.5f), s * 0.47f, style = Stroke(s * 0.035f))
                        drawOval(Color.Black.copy(alpha = 0.1f), topLeft = Offset(s * 0.22f, s * 0.83f), size = Size(s * 0.56f, s * 0.07f))
                        drawBreed(fav, s / 2, s * 0.87f, s * 0.72f, wag = (t - 0.5f) * 50f)
                        // "change" badge
                        val bc = Offset(s * 0.84f, s * 0.84f)
                        drawCircle(Color.White, s * 0.13f, bc)
                        drawCircle(fav.collar, s * 0.13f, bc, style = Stroke(s * 0.02f))
                        inset(bc.x - s * 0.08f, bc.y - s * 0.08f, size.width - bc.x - s * 0.08f, size.height - bc.y - s * 0.08f) { iconReplay(fav.collar) }
                    }
                }
                Box(Modifier.size(width = 280.dp, height = 70.dp), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.fillMaxSize()) {
                        drawBonePlate(Rect(0f, size.height * 0.06f, size.width, size.height), Color.Black.copy(alpha = 0.08f))
                        drawBonePlate(Rect(0f, 0f, size.width, size.height * 0.94f), Color.White)
                    }
                    Text("Woof Battle", fontSize = 30.sp, fontWeight = FontWeight.Black, color = Cocoa, modifier = Modifier.padding(bottom = 4.dp))
                }
            }
        }

        val tileW: Dp
        val tileH: Dp
        if (landscape) {
            tileW = min(maxWidth * 0.46f, 440.dp)
            tileH = ((maxHeight - 56.dp) / 3.25f).coerceAtLeast(MinTouch)
        } else {
            tileW = min(maxWidth - 40.dp, 460.dp)
            tileH = ((maxHeight - 420.dp) / 3.3f).coerceIn(MinTouch, 170.dp)
        }

        val tiles: @Composable () -> Unit = {
            when (picking) {
                Mode.VS_CPU -> (1..3).forEach { dogs ->
                    PickTile(PlayerColors[0], tileW, tileH, "1 vs $dogs", { onStart(Mode.VS_CPU, dogs + 1) }) { drawVsDogs(fav, dogs, t) }
                }
                Mode.TOGETHER -> (2..4).forEach { kids ->
                    PickTile(PlayerColors[1], tileW, tileH, "$kids", { onStart(Mode.TOGETHER, kids) }) { drawTogether(kids) }
                }
                else -> {
                    PickTile(PlayerColors[0], tileW, tileH, null, { picking = Mode.VS_CPU }, "play against dogs") { drawVsDogs(fav, 1, t) }
                    PickTile(PlayerColors[1], tileW, tileH, null, { picking = Mode.TOGETHER }, "play together") { drawTogether(2) }
                    PickTile(PlayerColors[2], tileW, tileH, null, { onStart(Mode.DEMO, settings.playerCount) }, "watch a demo") {
                        val h = size.height
                        drawBreed(Breed.CORGI, size.width * 0.18f, h * 0.98f, h * 0.85f, wag = (t - 0.5f) * 40f)
                        inset(size.width * 0.38f, h * 0.12f, size.width * 0.38f, h * 0.12f) { iconPlay(Color.White) }
                        drawBreed(Breed.POODLE, size.width * 0.82f, h * 0.98f, h * 0.85f, wag = (0.5f - t) * 40f)
                    }
                }
            }
        }

        val headerDog = if (landscape) min(maxHeight * 0.5f, 260.dp) else min(maxWidth * 0.5f, 240.dp)
        if (landscape) {
            Row(Modifier.fillMaxSize().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                    header(headerDog)
                }
                Column(Modifier.fillMaxHeight().padding(end = 88.dp), verticalArrangement = Arrangement.SpaceEvenly) { tiles() }
            }
        } else {
            Column(
                Modifier.fillMaxSize().padding(top = 28.dp, bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly,
            ) {
                header(headerDog)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { tiles() }
            }
        }

        if (picking != null) {
            BigIconButton({ picking = null }, Color.White.copy(alpha = 0.9f), "back", Modifier.align(Alignment.TopStart).padding(8.dp)) {
                iconBack(Cocoa)
            }
        }
        BigIconButton(
            onToggleSound, Color.White.copy(alpha = 0.85f), strings.sound,
            Modifier.align(if (picking != null) Alignment.BottomStart else Alignment.TopStart).padding(8.dp),
        ) { iconSpeaker(Cocoa, settings.soundOn) }
        ParentGateButton(onOpenParent, strings.parentArea, Modifier.align(Alignment.TopEnd).padding(8.dp))
    }
}

/**
 * Big coloured tile with a stitched border and a dog tag, like a dog bed. [label] is a small
 * corner hint for parents (children go by the picture).
 */
@Composable
private fun PickTile(color: Color, width: Dp, height: Dp, label: String?, onClick: () -> Unit, description: String = label ?: "", art: DrawScope.() -> Unit) {
    val shape = RoundedCornerShape(32.dp)
    Box(
        Modifier
            .padding(4.dp)
            .size(width = max(width, MinTouch), height = max(height, MinTouch))
            .shadow(8.dp, shape)
            .clip(shape)
            .background(color)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val m = 7.dp.toPx()
            drawPaw(Offset(size.width * 0.9f, size.height * 0.78f), size.height * 0.5f, Color.White.copy(alpha = 0.12f))
            drawPaw(Offset(size.width * 0.08f, size.height * 0.22f), size.height * 0.3f, Color.White.copy(alpha = 0.1f))
            drawRoundRect(
                Color.White.copy(alpha = 0.7f), topLeft = Offset(m, m), size = Size(size.width - 2 * m, size.height - 2 * m),
                cornerRadius = CornerRadius(26.dp.toPx()),
                style = Stroke(3.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f))),
            )
        }
        Canvas(Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 12.dp)) { art() }
        if (label != null) {
            Text(
                label,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                modifier = Modifier.align(Alignment.TopEnd).padding(10.dp)
                    .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(12.dp)).padding(horizontal = 8.dp, vertical = 2.dp),
            )
        }
    }
}

/** My dog (left, on a cushion) facing [dogs] computer dogs (right). */
private fun DrawScope.drawVsDogs(fav: Breed, dogs: Int, t: Float) {
    val h = size.height
    val left = size.width * 0.2f
    drawOval(Color.White.copy(alpha = 0.35f), topLeft = Offset(left - h * 0.45f, h * 0.82f), size = Size(h * 0.9f, h * 0.18f))
    drawBreed(fav, left, h * 0.95f, h * 0.92f, wag = (t - 0.5f) * 40f)
    val others = Breed.entries.filter { it != fav }
    val area = size.width * 0.54f
    val step = area / dogs
    val dogSize = minOf(h * 0.8f, step * 1.05f)
    for (i in 0 until dogs) {
        val cx = size.width * 0.46f + step * (i + 0.5f)
        drawBreed(others[(i * 3 + 1) % others.size], cx, h * 0.95f, dogSize, wag = (0.5f - t) * 30f)
    }
}

/** 2–4 children in the seat colours. */
private fun DrawScope.drawTogether(players: Int) {
    val h = size.height
    val step = size.width / players
    for (i in 0 until players) {
        val r = minOf(h * 0.36f, step * 0.44f)
        drawCircle(Color.White.copy(alpha = 0.35f), r, Offset(step * (i + 0.5f), h * 0.5f))
        drawKidFace(Offset(step * (i + 0.5f), h * 0.5f), r * 0.86f, PlayerColors[i].copy(alpha = 0.95f))
    }
}
