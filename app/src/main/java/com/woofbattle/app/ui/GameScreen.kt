package com.woofbattle.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.withFrameMillis
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.woofbattle.app.GameUi
import com.woofbattle.app.Mode
import com.woofbattle.app.Settings
import com.woofbattle.app.Strings
import com.woofbattle.app.TableCard
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private val Gold = Color(0xFFFFC93C)

/** Where a seat sits on screen. */
private data class SeatPlacement(val seat: Int, val rotated: Boolean, val barAtTop: Boolean)

@Composable
fun GameScreen(
    ui: GameUi,
    settings: Settings,
    strings: Strings,
    onFlip: (Int) -> Unit,
    onToggleSound: () -> Unit,
    onOpenParent: () -> Unit,
    onReplay: () -> Unit,
    onHome: () -> Unit,
) {
    BackHandler { /* swallow: little hands hit Back a lot; parents leave via the parent area */ }

    BoxWithConstraints(Modifier.fillMaxSize().background(Cream)) {
        val landscape = maxWidth > maxHeight
        val n = ui.game.players.size
        val together = ui.session.mode == Mode.TOGETHER
        val rows: List<List<Int>> = when {
            landscape && n <= 3 -> listOf((0 until n).toList())
            n == 2 -> listOf(listOf(1), listOf(0))
            n == 3 -> listOf(listOf(1, 2), listOf(0))
            else -> listOf(listOf(1, 2), listOf(0, 3))
        }
        val placements = rows.mapIndexed { r, row ->
            row.map { seat ->
                val top = rows.size == 2 && r == 0
                SeatPlacement(seat, rotated = top && together, barAtTop = top && !together)
            }
        }
        val rowH = maxHeight / rows.size
        val cellW = maxWidth / rows.maxOf { it.size }
        val barH = (rowH * 0.36f).coerceIn(MinTouch, 150.dp)
        val cardH = min((rowH - barH) * 0.84f, cellW * 0.36f / 0.7f)
        val cardW = cardH * 0.7f

        val tableAnchors = remember { mutableStateMapOf<Int, Offset>() }
        val pileAnchors = remember { mutableStateMapOf<Int, Offset>() }
        var overlayOrigin by remember { mutableStateOf(Offset.Zero) }

        val shake = remember { Animatable(0f) }
        LaunchedEffect(ui.shakeTick) {
            if (ui.shakeTick > 0) {
                repeat(7) { shake.animateTo(if (it % 2 == 0) 22f else -22f, tween(45)) }
                shake.animateTo(0f, tween(60))
            }
        }

        Box(Modifier.fillMaxSize().graphicsLayer { translationX = shake.value }) {
            Column(Modifier.fillMaxSize()) {
                placements.forEach { row ->
                    Row(Modifier.fillMaxWidth().weight(1f)) {
                        row.forEach { p ->
                            SeatCell(
                                ui = ui,
                                placement = p,
                                cardW = cardW,
                                cardH = cardH,
                                barH = barH,
                                onFlip = onFlip,
                                onTableAnchor = { tableAnchors[p.seat] = it },
                                onPileAnchor = { pileAnchors[p.seat] = it },
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                            )
                        }
                    }
                }
            }

            // All table cards live in one overlay so they can fly between seats.
            Box(Modifier.fillMaxSize().onGloballyPositioned { overlayOrigin = it.boundsInRoot().topLeft }) {
                val density = LocalDensity.current
                val cardWpx = with(density) { cardW.toPx() }
                val cardHpx = with(density) { cardH.toPx() }
                val rotatedSeats = placements.flatten().filter { it.rotated }.map { it.seat }.toSet()
                val perSeat = ui.table.groupBy { it.owner }
                ui.table.forEach { tc ->
                    val mine = perSeat.getValue(tc.owner)
                    val k = mine.indexOf(tc)
                    val dir = if (tc.owner in rotatedSeats) -1f else 1f
                    val slot = tableAnchors[tc.owner] ?: return@forEach
                    val tableCenter = slot + Offset(dir * (k - (mine.size - 1) / 2f) * cardWpx * 0.32f, 0f)
                    val homeCenter = pileAnchors[tc.owner] ?: slot
                    val targetCenter = when {
                        ui.collectTo != null -> pileAnchors[ui.collectTo] ?: tableCenter
                        ui.returning -> homeCenter
                        else -> tableCenter
                    }
                    val toLocal = { c: Offset -> c - overlayOrigin - Offset(cardWpx / 2, cardHpx / 2) }
                    key(tc.card.id) {
                        FlyingCard(
                        tc = tc,
                        start = toLocal(homeCenter),
                        target = toLocal(targetCenter),
                        rotation = if (tc.owner in rotatedSeats) 180f else 0f,
                        shrink = ui.collectTo != null || ui.returning,
                        highlight = if (tc.faceUp && ui.trickWinner == tc.owner) Gold else null,
                        cardW = cardW,
                        cardH = cardH,
                        )
                    }
                }
            }
        }

        // Corner controls (sound + parent gate) at the edges where they disturb play least.
        val controlsAlignTop = rows.size == 1
        BigIconButton(
            onClick = onToggleSound,
            color = Color.White.copy(alpha = 0.8f),
            description = strings.sound,
            size = MinTouch,
            modifier = Modifier.align(if (controlsAlignTop) Alignment.TopStart else Alignment.CenterStart).padding(8.dp),
        ) { iconSpeaker(Cocoa, settings.soundOn) }
        ParentGateButton(
            onUnlock = onOpenParent,
            description = strings.parentArea,
            modifier = Modifier.align(if (controlsAlignTop) Alignment.TopEnd else Alignment.CenterEnd).padding(8.dp),
        )

        if (ui.dealing) ShuffleIntro(cardW, cardH, Modifier.align(Alignment.Center))
        WoofBanner(ui.woof, strings.woof, Modifier.align(Alignment.Center), min(maxWidth, maxHeight) * 0.8f)

        if (ui.showVictory) {
            VictoryOverlay(ui = ui, onReplay = onReplay, onHome = onHome)
        }
    }
}

@Composable
private fun FlyingCard(
    tc: TableCard,
    start: Offset,
    target: Offset,
    rotation: Float,
    shrink: Boolean,
    highlight: Color?,
    cardW: Dp,
    cardH: Dp,
) {
    val pos = remember(tc.card.id) { Animatable(start, Offset.VectorConverter) }
    LaunchedEffect(target) {
        pos.animateTo(target, tween(if (shrink) 650 else 380, easing = FastOutSlowInEasing))
    }
    val scale by animateFloatAsState(if (shrink) 0.4f else 1f, tween(650), label = "collect")
    PlayingCard(
        value = tc.card.value,
        faceUp = tc.faceUp,
        width = cardW,
        height = cardH,
        highlight = highlight,
        modifier = Modifier
            .offset { IntOffset(pos.value.x.roundToInt(), pos.value.y.roundToInt()) }
            .graphicsLayer {
                rotationZ = rotation
                scaleX = scale
                scaleY = scale
            },
    )
}

@Composable
private fun SeatCell(
    ui: GameUi,
    placement: SeatPlacement,
    cardW: Dp,
    cardH: Dp,
    barH: Dp,
    onFlip: (Int) -> Unit,
    onTableAnchor: (Offset) -> Unit,
    onPileAnchor: (Offset) -> Unit,
    modifier: Modifier = Modifier,
) {
    val seat = placement.seat
    val color = PlayerColors[seat]
    val out = ui.game.players[seat].isEliminated && !ui.animating
    Column(
        modifier
            .graphicsLayer { rotationZ = if (placement.rotated) 180f else 0f }
            .alpha(if (out) 0.35f else 1f)
            .padding(4.dp),
    ) {
        val table: @Composable () -> Unit = {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Canvas(
                    Modifier
                        .size(cardW, cardH)
                        .onGloballyPositioned { onTableAnchor(it.boundsInRoot().center) },
                ) {
                    drawRoundRect(
                        color.copy(alpha = 0.35f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.width * 0.1f),
                        style = Stroke(size.width * 0.03f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(18f, 14f))),
                    )
                }
            }
        }
        if (!placement.barAtTop) table()
        SeatBar(ui, seat, color, barH, onFlip, onPileAnchor)
        if (placement.barAtTop) table()
    }
}

@Composable
private fun SeatBar(ui: GameUi, seat: Int, color: Color, barH: Dp, onFlip: (Int) -> Unit, onPileAnchor: (Offset) -> Unit) {
    val human = ui.isHuman(seat)
    val ready = seat in ui.ready
    val eliminated = ui.game.players[seat].isEliminated
    val waiting = human && !ready && !ui.animating && !ui.dealing && !ui.game.isFinished && !eliminated
    val winner = ui.trickWinner == seat
    val battling = seat in ui.battlers

    val pulse = rememberInfiniteTransition(label = "pulse")
    val pulseScale by pulse.animateFloat(1f, 1.12f, infiniteRepeatable(tween(550), RepeatMode.Reverse), label = "p")
    val winBounce by animateFloatAsState(if (winner) 1.06f else 1f, spring(dampingRatio = 0.35f), label = "win")

    val shape = RoundedCornerShape(28.dp)
    BoxWithConstraints(Modifier.fillMaxWidth().height(barH)) {
    val inner = maxWidth - 24.dp
    val pileH = min(barH * 0.66f, inner * 0.42f)
    val circle = min(barH * 0.82f, inner * 0.5f)
    Row(
        Modifier
            .fillMaxSize()
            .scale(winBounce)
            .background(color.copy(alpha = if (waiting) 0.9f else 0.55f), shape)
            .border(if (winner || battling) 6.dp else 0.dp, if (winner) Gold else Color.White, shape)
            .clickable(
                enabled = human && !eliminated,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { onFlip(seat) }
            .semantics { contentDescription = "seat $seat" }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            PileStack(
                count = ui.pileCounts[seat],
                cardWidth = pileH * 0.7f,
                cardHeight = pileH,
                modifier = Modifier.onGloballyPositioned { onPileAnchor(it.boundsInRoot().center) },
            )
            CountBadge(ui.pileCounts[seat])
        }
        Box(
            Modifier
                .size(circle)
                .scale(if (waiting) pulseScale else 1f)
                .background(Color.White.copy(alpha = if (human) 0.95f else 0.6f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.fillMaxSize().padding(circle * 0.12f)) {
                drawBreed(ui.dogOf(seat), size.width / 2, size.height * 0.98f, size.height * 0.95f, happy = winner)
                if (human) {
                    // paw badge = "tap here"; turns into a check once this player is ready
                    val r = size.width * 0.2f
                    val c = Offset(size.width - r * 0.7f, size.height - r * 0.7f)
                    drawCircle(if (ready) Color(0xFF2E9E4F) else color, r, c)
                    drawCircle(Color.White, r, c, style = Stroke(r * 0.15f))
                    if (ready) {
                        inset(c.x - r * 0.75f, c.y - r * 0.75f, size.width - c.x - r * 0.75f, size.height - c.y - r * 0.75f) { iconCheck(Color.White) }
                    } else {
                        drawPaw(Offset(c.x, c.y + r * 0.15f), r * 1.25f, Color.White)
                    }
                }
            }
        }
    }
    }
}

/** Start of game: a deck riffling back and forth in the middle, so children see the cards being mixed. */
@Composable
private fun ShuffleIntro(cardW: Dp, cardH: Dp, modifier: Modifier) {
    var time by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        val start = withFrameMillis { it }
        while (true) withFrameMillis { time = (it - start) / 1000f }
    }
    Canvas(modifier.size(cardW * 3f, cardH * 1.3f)) {
        val w = cardW.toPx()
        val h = cardH.toPx()
        val c = Offset(size.width / 2, size.height / 2)
        for (i in 0 until 10) {
            val side = if (i % 2 == 0) 1f else -1f
            val swing = sin((time * 2.6f + i * 0.12f) * Math.PI).toFloat()
            val x = c.x + side * swing * w * 0.75f
            val y = c.y - i * h * 0.012f
            rotate(side * swing * 14f, Offset(x, y)) {
                translate(x - w / 2, y - h / 2) {
                    drawIntoCardBack(w, h)
                }
            }
        }
    }
}

private fun DrawScope.drawIntoCardBack(w: Float, h: Float) {
    val old = drawContext.size
    drawContext.size = androidx.compose.ui.geometry.Size(w, h)
    drawRoundRect(Color.Black.copy(alpha = 0.15f), topLeft = Offset(w * 0.03f, h * 0.025f), size = androidx.compose.ui.geometry.Size(w, h), cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.1f))
    drawCardBack()
    drawContext.size = old
}

@Composable
private fun WoofBanner(visible: Boolean, text: String, modifier: Modifier, maxSize: Dp) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = scaleIn(spring(dampingRatio = 0.4f, stiffness = 300f)) + fadeIn(),
        exit = scaleOut() + fadeOut(),
    ) {
        val spin = rememberInfiniteTransition(label = "spin")
        val angle by spin.animateFloat(0f, 360f, infiniteRepeatable(tween(6000)), label = "a")
        Box(Modifier.size(min(maxSize, 360.dp)), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize().graphicsLayer { rotationZ = angle }) {
                val c = Offset(size.width / 2, size.height / 2)
                val outer = size.minDimension / 2
                val star = Path()
                val points = 14
                for (i in 0 until points * 2) {
                    val r = if (i % 2 == 0) outer else outer * 0.72f
                    val a = Math.PI * i / points
                    val pt = Offset(c.x + (r * cos(a)).toFloat(), c.y + (r * sin(a)).toFloat())
                    if (i == 0) star.moveTo(pt.x, pt.y) else star.lineTo(pt.x, pt.y)
                }
                star.close()
                drawPath(star, Color(0xFFFFD43B))
                drawPath(star, Color(0xFFFF8A3D), style = Stroke(outer * 0.05f))
            }
            val fontSize = (min(maxSize, 360.dp).value * 0.2f).sp
            Text(text, fontSize = fontSize, fontWeight = FontWeight.Black, color = Color(0xFFE63946))
            Text(
                text,
                fontSize = fontSize,
                fontWeight = FontWeight.Black,
                color = Color.White,
                style = TextStyle(drawStyle = androidx.compose.ui.graphics.drawscope.Stroke(width = 6f)),
            )
        }
    }
}
