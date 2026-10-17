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
fun DrawScope.drawBonePlate(rect: Rect, color: Color) {
    val t = rect.height * 0.62f
    val r = rect.height * 0.3f
    drawRoundRect(color, topLeft = Offset(rect.left + r, rect.center.y - t / 2), size = Size(rect.width - 2 * r, t), cornerRadius = CornerRadius(t / 2))
    listOf(rect.left + r, rect.right - r).forEach { x ->
        drawCircle(color, r, Offset(x, rect.top + r))
        drawCircle(color, r, Offset(x, rect.bottom - r))
    }
}

/**
 * "Choose your dog": 8 big picture tiles. Tapping one selects it (it barks and jumps);
 * the green check goes back.
 */
@Composable
fun DogPicker(selected: Breed, strings: Strings, onSelect: (Breed) -> Unit, onDone: () -> Unit) {
    BackHandler(onBack = onDone)
    val wag = rememberInfiniteTransition(label = "wag")
    val t by wag.animateFloat(-1f, 1f, infiniteRepeatable(tween(260), RepeatMode.Reverse), label = "t")

    BoxWithConstraints(Modifier.fillMaxSize()) {
        DogParkBackground()
        val landscape = maxWidth > maxHeight
        val cols = if (landscape) 4 else 2
        val rows = Breed.entries.size / cols
        val header = 112.dp
        val cell = min((maxWidth - 32.dp) / cols, (maxHeight - header - 24.dp) / rows)

        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth().height(header), verticalAlignment = Alignment.CenterVertically) {
                BigIconButton(onDone, Color(0xFF3FB560), "done") { iconCheck(Color.White) }
                Text(
                    strings.chooseDog,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = Cocoa,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
                Box(Modifier.size(MinTouch))
            }
            Column(verticalArrangement = Arrangement.Center, modifier = Modifier.fillMaxSize()) {
                Breed.entries.chunked(cols).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        row.forEach { breed ->
                            DogTile(breed, breed == selected, strings.breedName(breed), t, Modifier.size(cell).padding(6.dp)) { onSelect(breed) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DogTile(breed: Breed, selected: Boolean, name: String, wag: Float, modifier: Modifier, onClick: () -> Unit) {
    val scale by animateFloatAsState(if (selected) 1.06f else 1f, spring(dampingRatio = 0.4f), label = "pick")
    val shape = RoundedCornerShape(28.dp)
    Box(
        modifier
            .scale(scale)
            .shadow(if (selected) 12.dp else 4.dp, shape)
            .clip(shape)
            .background(breed.card)
            .border(if (selected) 7.dp else 4.dp, if (selected) Color(0xFFFFC93C) else breed.collar.copy(alpha = 0.6f), shape)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = name },
    ) {
        Column(Modifier.fillMaxSize().padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Canvas(Modifier.fillMaxWidth().weight(1f)) {
                val s = minOf(size.width, size.height) * 0.95f
                drawOval(Color.Black.copy(alpha = 0.08f), topLeft = Offset(size.width / 2 - s * 0.35f, size.height - s * 0.07f), size = Size(s * 0.7f, s * 0.08f))
                drawBreed(breed, size.width / 2, size.height * 0.98f, s, wag = if (selected) wag * 25f else 0f, happy = selected)
                if (selected) {
                    drawCrown(Rect(size.width * 0.06f, size.height * 0.02f, size.width * 0.06f + s * 0.24f, size.height * 0.02f + s * 0.16f), Color(0xFFFFD43B))
                }
            }
            Text(name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Cocoa, maxLines = 1)
        }
    }
}
