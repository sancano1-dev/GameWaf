package com.woofbattle.app

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.compose.foundation.layout.fillMaxSize
import com.woofbattle.app.ui.Breed
import com.woofbattle.app.ui.GameScreen
import com.woofbattle.app.ui.drawBreed
import com.woofbattle.app.ui.HomeScreen
import com.woofbattle.app.ui.ParentArea
import com.woofbattle.app.ui.PlayingCard
import com.woofbattle.app.ui.WoofTheme
import com.woofbattle.engine.Card
import com.woofbattle.engine.EndReason
import com.woofbattle.engine.GameConfig
import com.woofbattle.engine.GameEngine
import com.woofbattle.engine.GameStatus
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Renders key screens to PNG (build/screenshots) for visual review without a device.
 * Opt-in: ./gradlew :app:testDebugUnitTest -Pscreenshots
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class ScreenshotTest {

    @get:Rule
    val rule = createComposeRule()

    private val strings = Strings.of(Lang.EN)
    private val settings = Settings(playerCount = 3)

    private fun shoot(name: String, settleMs: Long = 2500, content: @Composable () -> Unit) {
        rule.mainClock.autoAdvance = false
        rule.setContent { WoofTheme { content() } }
        rule.mainClock.advanceTimeBy(settleMs)
        val bmp = rule.onRoot().captureToImage().asAndroidBitmap()
        val dir = File(System.getProperty("screenshotDir") ?: "build/screenshots").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun game(mode: Mode, piles: List<List<Int>>, table: List<TableCard>, transform: (GameUi) -> GameUi = { it }): GameUi {
        val state = GameEngine.fromPiles(piles, GameConfig(playerCount = piles.size))
        val humans = when (mode) {
            Mode.VS_CPU -> listOf(0)
            Mode.TOGETHER -> piles.indices.toList()
            Mode.DEMO -> emptyList()
        }
        return transform(GameUi(Session(mode, humans, state), state.players.map { it.cardCount }, table = table))
    }

    private fun noop(@Suppress("UNUSED_PARAMETER") i: Int) {}

    @Test
    @Config(qualifiers = "w411dp-h891dp-xxhdpi")
    fun home_portrait() = shoot("home_portrait") {
        HomeScreen(settings, strings, { _, _ -> }, {}, {})
    }

    @Test
    @Config(qualifiers = "w891dp-h411dp-xxhdpi")
    fun home_landscape() = shoot("home_landscape") {
        HomeScreen(settings, strings, { _, _ -> }, {}, {})
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-xxhdpi")
    fun home_pick_vs_cpu() = shoot("home_pick_vs_cpu") {
        HomeScreen(settings, strings, { _, _ -> }, {}, {}, initialPicking = Mode.VS_CPU)
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-xxhdpi")
    fun dog_picker_portrait() = shoot("dog_picker_portrait") {
        com.woofbattle.app.ui.DogPicker(Breed.CORGI, strings, {}, {})
    }

    @Test
    @Config(qualifiers = "w891dp-h411dp-xxhdpi")
    fun dog_picker_landscape() = shoot("dog_picker_landscape") {
        com.woofbattle.app.ui.DogPicker(Breed.POODLE, Strings.of(Lang.FR), {}, {})
    }

    @Test
    @Config(qualifiers = "w891dp-h411dp-xxhdpi")
    fun home_pick_together() = shoot("home_pick_together") {
        HomeScreen(settings, strings, { _, _ -> }, {}, {}, initialPicking = Mode.TOGETHER)
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-xxhdpi")
    fun game_shuffling() = shoot("game_shuffling", settleMs = 1300) {
        val ui = game(Mode.VS_CPU, listOf(List(18) { 1 }, List(18) { 1 }), emptyList()) {
            it.copy(dealing = true, pileCounts = listOf(0, 0))
        }
        GameScreen(ui, settings, strings, ::noop, {}, {}, {}, {})
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-xxhdpi")
    fun game_vs_cpu_portrait() = shoot("game_vs_cpu_portrait") {
        val ui = game(
            Mode.VS_CPU, listOf(List(17) { 3 }, List(13) { 2 }),
            listOf(TableCard(Card(100, 2), 0, true), TableCard(Card(101, 6), 1, true)),
        ) { it.copy(trickWinner = 1) }
        GameScreen(ui, settings, strings, ::noop, {}, {}, {}, {})
    }

    @Test
    @Config(qualifiers = "w891dp-h411dp-xxhdpi")
    fun game_together_4p_landscape_woof() = shoot("game_together_4p_landscape_woof", settleMs = 900) {
        val ui = game(
            Mode.TOGETHER, listOf(List(8) { 1 }, List(6) { 1 }, List(9) { 1 }, List(7) { 1 }),
            listOf(
                TableCard(Card(100, 5), 0, true), TableCard(Card(101, 5), 1, true),
                TableCard(Card(102, 1), 2, true), TableCard(Card(103, 3), 3, true),
                TableCard(Card(104, 4), 0, false), TableCard(Card(105, 2), 1, false),
            ),
        ) { it.copy(woof = true, battlers = setOf(0, 1), ready = setOf(2)) }
        GameScreen(ui, settings, strings, ::noop, {}, {}, {}, {})
    }

    @Test
    @Config(qualifiers = "w1280dp-h800dp-mdpi")
    fun game_tablet_3p_waiting() = shoot("game_tablet_3p_waiting") {
        val ui = game(Mode.TOGETHER, listOf(List(12) { 1 }, List(20) { 1 }, List(4) { 1 }), emptyList()) {
            it.copy(ready = setOf(1))
        }
        GameScreen(ui, settings, strings, ::noop, {}, {}, {}, {})
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-xxhdpi")
    fun game_together_4p_portrait() = shoot("game_together_4p_portrait") {
        val ui = game(
            Mode.TOGETHER, listOf(List(9) { 1 }, List(9) { 1 }, List(9) { 1 }, List(9) { 1 }),
            listOf(TableCard(Card(100, 4), 0, true), TableCard(Card(101, 6), 1, true), TableCard(Card(102, 2), 2, true), TableCard(Card(103, 1), 3, true)),
        )
        GameScreen(ui, settings, strings, ::noop, {}, {}, {}, {})
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-xxhdpi")
    fun victory() = shoot("victory") {
        val base = game(Mode.VS_CPU, listOf(List(36) { 1 }, emptyList()), emptyList())
        val ui = base.copy(session = base.session.copy(game = base.game.copy(status = GameStatus.Finished(listOf(0), EndReason.ALL_CARDS))))
        GameScreen(ui, settings, strings, ::noop, {}, {}, {}, {})
    }

    @Test
    @Config(qualifiers = "w411dp-h1500dp-xxhdpi")
    fun parent_area() = shoot("parent_area") {
        ParentArea(settings.copy(lang = Lang.ES, soundTheme = SoundTheme.SILLY), Strings.of(Lang.ES), inGame = true, onChange = {}, onChooseSoundTheme = {}, onQuitGame = {}, onClose = {})
    }

    /** Captures [frames] frames, [stepMs] apart, of an animated scene into build/screenshots/<name>/. */
    private fun frames(name: String, frames: Int, stepMs: Long, content: @Composable () -> Unit) {
        rule.mainClock.autoAdvance = false
        rule.setContent { WoofTheme { content() } }
        rule.mainClock.advanceTimeBy(400)
        val dir = File(System.getProperty("screenshotDir") ?: "build/screenshots", name).apply { mkdirs() }
        repeat(frames) { i ->
            val bmp = rule.onRoot().captureToImage().asAndroidBitmap()
            File(dir, "f%03d.png".format(i)).outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
            rule.mainClock.advanceTimeBy(stepMs)
        }
    }

    @Test
    @Config(qualifiers = "w640dp-h400dp-mdpi")
    fun bark_off_animation() = frames("bark_off", 26, 40) {
        androidx.compose.foundation.layout.Box(Modifier.fillMaxSize().background(com.woofbattle.app.ui.Cream)) {
            com.woofbattle.app.ui.BarkOff(true, listOf(Breed.CORGI, Breed.HUSKY), "WOOF!", Modifier.fillMaxSize())
        }
    }

    @Test
    @Config(qualifiers = "w640dp-h400dp-mdpi")
    fun bark_off_three_dogs() = shoot("bark_off_three", settleMs = 620) {
        androidx.compose.foundation.layout.Box(Modifier.fillMaxSize().background(com.woofbattle.app.ui.Cream)) {
            com.woofbattle.app.ui.BarkOff(true, listOf(Breed.PUG, Breed.POODLE, Breed.DALMATIAN), "¡GUAU!", Modifier.fillMaxSize())
        }
    }

    @Test
    @Config(qualifiers = "w400dp-h400dp-mdpi")
    fun jump_animation() = frames("jump", 27, 40) {
        androidx.compose.foundation.layout.Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color(0xFF4A3426))) {
            com.woofbattle.app.ui.JumpingDog(Breed.GOLDEN, Modifier.fillMaxSize())
        }
    }

    @Test
    @Config(qualifiers = "w1000dp-h300dp-xxhdpi")
    fun all_breeds() = shoot("all_breeds") {
        androidx.compose.foundation.Canvas(androidx.compose.ui.Modifier.fillMaxSize()) {
            val step = size.width / Breed.entries.size
            Breed.entries.forEachIndexed { i, b ->
                drawRect(b.card, topLeft = androidx.compose.ui.geometry.Offset(i * step, 0f), size = androidx.compose.ui.geometry.Size(step, size.height))
                drawBreed(b, step * (i + 0.5f), size.height * 0.92f, minOf(step * 0.92f, size.height * 0.8f), happy = i == 7)
            }
        }
    }

    @Test
    @Config(qualifiers = "w891dp-h300dp-xxhdpi")
    fun all_cards() = shoot("all_cards") {
        Row(Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (1..6).forEach { PlayingCard(it, faceUp = true, width = 126.dp, height = 180.dp) }
            PlayingCard(1, faceUp = false, width = 126.dp, height = 180.dp)
        }
    }
}
