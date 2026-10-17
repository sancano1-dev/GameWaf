package com.woofbattle.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.woofbattle.app.Lang
import com.woofbattle.app.Settings
import com.woofbattle.app.SoundTheme
import com.woofbattle.app.Strings

/**
 * Parent settings, reached only through the 3-second hold gate. Changes to players and game
 * length apply to the next game; sound, auto-play and language apply immediately.
 */
@Composable
fun ParentArea(
    settings: Settings,
    strings: Strings,
    inGame: Boolean,
    onChange: ((Settings) -> Settings) -> Unit,
    onChooseSoundTheme: (SoundTheme) -> Unit,
    onQuitGame: () -> Unit,
    onClose: () -> Unit,
) {
    BackHandler(onBack = onClose)
    Box(
        Modifier
            .fillMaxSize()
            .background(Cream)
            .clickable(interactionSource = null, indication = null) { /* block taps to the game below */ },
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(Modifier.widthIn(max = 720.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(strings.parentArea, fontSize = 30.sp, fontWeight = FontWeight.Black, color = Cocoa, modifier = Modifier.weight(1f))
                BigIconButton(onClose, Color(0xFF3FB560), "close") { iconCheck(Color.White) }
            }

            Section(strings.players) {
                (2..4).forEach { n ->
                    Chip(settings.playerCount == n, { onChange { it.copy(playerCount = n) } }) {
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                            repeat(n) { i ->
                                Canvas(Modifier.size(18.dp)) { drawCircle(PlayerColors[i]) }
                            }
                            Spacer(Modifier.size(6.dp))
                            Text("$n", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Cocoa)
                        }
                    }
                }
            }

            Section(strings.gameLength) {
                Chip(!settings.shortGame, { onChange { it.copy(shortGame = false) } }) {
                    Text("∞  ${strings.classic}", fontSize = 18.sp, color = Cocoa, textAlign = TextAlign.Center, modifier = Modifier.widthIn(max = 220.dp))
                }
                Settings.SHORT_ROUND_OPTIONS.forEach { rounds ->
                    Chip(settings.shortGame && settings.shortRounds == rounds, { onChange { it.copy(shortGame = true, shortRounds = rounds) } }) {
                        Text("$rounds ${strings.rounds}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Cocoa)
                    }
                }
            }

            Section(strings.sound) {
                listOf(true, false).forEach { on ->
                    Chip(settings.soundOn == on, { onChange { it.copy(soundOn = on) } }) {
                        Canvas(Modifier.size(48.dp)) { iconSpeaker(Cocoa, on) }
                    }
                }
            }

            Section(strings.soundStyle) {
                SoundTheme.entries.forEach { theme ->
                    Chip(settings.soundTheme == theme, { onChooseSoundTheme(theme) }) {
                        Canvas(Modifier.size(76.dp)) { drawSoundThemeIcon(theme) }
                    }
                }
            }

            Section(strings.autoPlay) {
                listOf(false, true).forEach { on ->
                    Chip(settings.autoPlay == on, { onChange { it.copy(autoPlay = on) } }) {
                        Canvas(Modifier.size(44.dp)) { if (on) iconPlay(Cocoa) else iconClose(Cocoa) }
                    }
                }
            }

            Section(strings.language) {
                Lang.entries.forEach { lang ->
                    Chip(settings.lang == lang, { onChange { it.copy(lang = lang) } }) {
                        Text(lang.nativeName, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Cocoa)
                    }
                }
            }

            if (inGame) {
                Row(
                    Modifier
                        .sizeIn(minHeight = MinTouch)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF4D96FF))
                        .clickable(role = Role.Button, onClick = onQuitGame)
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Canvas(Modifier.size(48.dp)) { iconHouse(Color.White) }
                    Text(strings.quitGame, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
            Column(
                Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.7f), RoundedCornerShape(20.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(strings.privacyTitle, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Cocoa)
                Text(strings.privacyText, fontSize = 15.sp, color = Cocoa)
                val context = LocalContext.current
                val version = remember {
                    runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull()
                }
                Text("Woof Battle ${version ?: ""}", fontSize = 12.sp, color = Cocoa.copy(alpha = 0.6f))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(Modifier.widthIn(max = 720.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Cocoa)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            content()
        }
    }
}

@Composable
private fun Chip(selected: Boolean, onClick: () -> Unit, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Box(
        Modifier
            .sizeIn(minWidth = MinTouch, minHeight = MinTouch)
            .clip(shape)
            .background(if (selected) Color(0xFFFFD43B) else Color.White)
            .border(if (selected) 4.dp else 2.dp, if (selected) Color(0xFFFF8A3D) else Color(0x22000000), shape)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) { content() }
}

/** Picture for a sound style: little pup, big dog, or a silly bouncing dog; music notes = "tap to hear". */
private fun DrawScope.drawSoundThemeIcon(theme: SoundTheme) {
    val w = size.width
    val h = size.height
    when (theme) {
        SoundTheme.PUPPY -> drawBreed(Breed.PUG, w * 0.42f, h * 0.98f, h * 0.55f)
        SoundTheme.BIG_DOG -> drawBreed(Breed.GOLDEN, w * 0.42f, h * 0.98f, h * 0.92f)
        SoundTheme.SILLY -> rotate(-12f, Offset(w * 0.42f, h * 0.7f)) {
            drawBreed(Breed.POODLE, w * 0.42f, h * 0.9f, h * 0.8f, wag = 25f, happy = true)
        }
    }
    // music note
    val note = Color(0xFFFF8A3D)
    val x = w * 0.86f
    drawCircle(note, w * 0.07f, Offset(x - w * 0.05f, h * 0.36f))
    drawLine(note, Offset(x + w * 0.015f, h * 0.36f), Offset(x + w * 0.015f, h * 0.08f), w * 0.035f)
    drawLine(note, Offset(x + w * 0.015f, h * 0.08f), Offset(x + w * 0.11f, h * 0.15f), w * 0.035f)
    if (theme == SoundTheme.SILLY) {
        drawArc(note, 200f, 220f, false, topLeft = Offset(w * 0.02f, h * 0.05f), size = androidx.compose.ui.geometry.Size(w * 0.2f, w * 0.2f), style = Stroke(w * 0.03f))
    }
}
