package com.woofbattle.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import kotlin.math.abs
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.woofbattle.app.Strings
import kotlin.random.Random

/** Warm gradient sprinkled with paw prints and bones: the "dog park" backdrop of the menus. */
@Composable
fun DogParkBackground(modifier: Modifier = Modifier) {
    val decor = androidx.compose.runtime.remember {
        val r = Random(11)
        List(26) { i -> Triple(Offset(r.nextFloat(), r.nextFloat()), r.nextFloat() * 360f, i % 3 == 0) }
    }
    Canvas(modifier.fillMaxSize()) {
        drawRect(Brush.verticalGradient(listOf(Color(0xFFFFF6E5), Color(0xFFFFE2BF))))
        val unit = size.minDimension * 0.07f
        decor.forEach { (p, angle, bone) ->
            val c = Offset(p.x * size.width, p.y * size.height)
            rotate(angle, c) {
                if (bone) drawBone(c, unit * 1.6f, Color.White.copy(alpha = 0.55f))
                else drawPaw(c, unit, Cocoa.copy(alpha = 0.07f))
            }
        }
    }
}

/** A cartoon bone centred on [c], [length] long. */
fun DrawScope.drawBone(c: Offset, length: Float, color: Color) {
    val t = length * 0.28f
    drawRoundRect(color, topLeft = Offset(c.x - length / 2, c.y - t / 2), size = Size(length, t), cornerRadius = CornerRadius(t / 2))
    listOf(-1f, 1f).forEach { sx ->
        listOf(-1f, 1f).forEach { sy ->
            drawCircle(color, t * 0.62f, Offset(c.x + sx * length / 2, c.y + sy * t * 0.5f))
        }
    }
}

/** Bone-shaped plate filling [rect] (used behind the title). */
fun DrawScope.drawBonePlate(rect: Rect, color: Color, outline: Float = 0f) {
    if (outline > 0f) drawBonePlate(rect.inflate(outline), Ink)
    val t = rect.height * 0.62f
    val r = rect.height * 0.3f
    drawRoundRect(color, topLeft = Offset(rect.left + r, rect.center.y - t / 2), size = Size(rect.width - 2 * r, t), cornerRadius = CornerRadius(t / 2))
    listOf(rect.left + r, rect.right - r).forEach { x ->
        drawCircle(color, r, Offset(x, rect.top + r))
        drawCircle(color, r, Offset(x, rect.bottom - r))
    }
}

/**
 * "Choose your dog" for every human player. The coloured slots at the top show whose turn it is;
 * tapping a dog gives it to that player, and a dog already taken by someone else can't be chosen.
 * The green play button starts the game once everyone has a dog.
 */
@Composable
fun DogPicker(
    picks: List<Breed?>,
    active: Int,
    strings: Strings,
    onPick: (Breed) -> Unit,
    onSelectPlayer: (Int) -> Unit,
    onBack: () -> Unit,
    onPlay: () -> Unit,
) {
    BackHandler(onBack = onBack)
    val wag = rememberInfiniteTransition(label = "wag")
    val t by wag.animateFloat(-1f, 1f, infiniteRepeatable(tween(260), RepeatMode.Reverse), label = "t")
    val complete = picks.none { it == null }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        DogParkBackground()
        val landscape = maxWidth > maxHeight
        val cols = if (landscape) 4 else 2
        val rows = Breed.entries.size / cols
        val header = if (landscape) 104.dp else 200.dp
        val cell = min((maxWidth - 32.dp) / cols, (maxHeight - header - 16.dp) / rows)

        val slots: @Composable () -> Unit = {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                picks.forEachIndexed { i, dog -> PlayerSlot(i, dog, i == active && !complete || picks.size == 1, t) { onSelectPlayer(i) } }
            }
        }
        val playButton: @Composable () -> Unit = {
            BigIconButton(
                onClick = onPlay,
                color = if (complete) Color(0xFF3FB560) else Color(0xFF3FB560).copy(alpha = 0.3f),
                description = "play",
                modifier = Modifier.scale(if (complete) 1f + 0.06f * abs(t) else 1f),
            ) { iconPlay(Color.White) }
        }

        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            if (landscape) {
                Row(Modifier.fillMaxWidth().height(header), verticalAlignment = Alignment.CenterVertically) {
                    BigIconButton(onBack, Color.White.copy(alpha = 0.9f), "back") { iconBack(Cocoa) }
                    MangaTitle(strings.chooseDog, 24.sp, Color(0xFFFF8A3D), Modifier.padding(horizontal = 12.dp))
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { slots() }
                    playButton()
                }
            } else {
                Row(Modifier.fillMaxWidth().height(112.dp), verticalAlignment = Alignment.CenterVertically) {
                    BigIconButton(onBack, Color.White.copy(alpha = 0.9f), "back") { iconBack(Cocoa) }
                    MangaTitle(strings.chooseDog, 26.sp, Color(0xFFFF8A3D), Modifier.weight(1f))
                    playButton()
                }
                Box(Modifier.height(header - 112.dp), contentAlignment = Alignment.Center) { slots() }
            }
            Column(verticalArrangement = Arrangement.Center, modifier = Modifier.fillMaxSize()) {
                Breed.entries.chunked(cols).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        row.forEach { breed ->
                            val owner = picks.indexOf(breed)
                            DogTile(breed, owner, strings.breedName(breed), t, Modifier.size(cell).padding(5.dp)) { onPick(breed) }
                        }
                    }
                }
            }
        }
    }
}

/** A player's slot: their colour with a child face, or the dog they picked. Gold ring = choosing now. */
@Composable
private fun PlayerSlot(player: Int, dog: Breed?, choosing: Boolean, wag: Float, onClick: () -> Unit) {
    val color = PlayerColors[player]
    val scale by animateFloatAsState(if (choosing) 1.12f else 1f, spring(dampingRatio = 0.4f), label = "slot")
    Box(
        Modifier
            .size(MinTouch * 0.86f)
            .scale(scale)
            .clip(CircleShape)
            .background(color)
            .border(if (choosing) 6.dp else 3.dp, if (choosing) Color(0xFFFFC93C) else Ink, CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = "player ${player + 1}" },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize().padding(8.dp)) {
            if (dog == null) {
                drawKidFace(Offset(size.width / 2, size.height / 2), size.minDimension * 0.42f, color.copy(alpha = 0.6f))
            } else {
                drawBreed(dog, size.width / 2, size.height, size.minDimension, happy = true, wag = if (choosing) wag * 20f else 0f)
            }
        }
    }
}

/** One of the 8 dogs. [owner] = index of the player who picked it, or -1 if free. */
@Composable
private fun DogTile(breed: Breed, owner: Int, name: String, wag: Float, modifier: Modifier, onClick: () -> Unit) {
    val taken = owner >= 0
    val scale by animateFloatAsState(if (taken) 1.05f else 1f, spring(dampingRatio = 0.4f), label = "pick")
    val shape = RoundedCornerShape(28.dp)
    val ownerColor = if (taken) PlayerColors[owner] else Ink
    Box(
        modifier
            .scale(scale)
            .shadow(if (taken) 12.dp else 4.dp, shape)
            .clip(shape)
            .background(breed.card)
            .border(if (taken) 7.dp else 3.dp, ownerColor, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = name },
    ) {
        Column(Modifier.fillMaxSize().padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Canvas(Modifier.fillMaxWidth().weight(1f)) {
                val s = minOf(size.width, size.height) * 0.95f
                if (taken) {
                    drawSpeedLines(Offset(size.width / 2, size.height * 0.55f), s * 0.42f, s * 1.2f, ownerColor.copy(alpha = 0.45f), count = 30, seed = breed.ordinal)
                }
                drawOval(Ink.copy(alpha = 0.1f), topLeft = Offset(size.width / 2 - s * 0.35f, size.height - s * 0.07f), size = Size(s * 0.7f, s * 0.08f))
                drawBreed(breed, size.width / 2, size.height * 0.98f, s, wag = if (taken) wag * 25f else 0f, happy = taken, aura = taken, phase = wag)
                if (taken) {
                    // owner badge: the player's colour with a child face
                    val r = s * 0.15f
                    val c = Offset(size.width * 0.12f + r * 0.4f, r * 1.1f)
                    drawCircle(Ink, r * 1.12f, c)
                    drawCircle(ownerColor, r, c)
                    drawKidFace(c, r * 0.8f, ownerColor.copy(alpha = 0.6f))
                }
            }
            Text(name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Cocoa, maxLines = 1)
        }
    }
}

/** Comic-book lettering: bold text with a thick ink outline. */
@Composable
fun MangaTitle(text: String, fontSize: androidx.compose.ui.unit.TextUnit, fill: Color, modifier: Modifier = Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Text(
            text, fontSize = fontSize, fontWeight = FontWeight.Black, color = Ink, textAlign = TextAlign.Center,
            style = androidx.compose.ui.text.TextStyle(drawStyle = androidx.compose.ui.graphics.drawscope.Stroke(width = fontSize.value * 0.32f, join = androidx.compose.ui.graphics.StrokeJoin.Round)),
            modifier = Modifier.clearAndSetSemantics { }, // outline layer only; the fill text carries the label
        )
        Text(text, fontSize = fontSize, fontWeight = FontWeight.Black, color = fill, textAlign = TextAlign.Center)
    }
}
