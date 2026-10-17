package com.woofbattle.app

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.woofbattle.app.ui.Breed
import com.woofbattle.app.ui.DogPicker
import com.woofbattle.app.ui.GameScreen
import com.woofbattle.app.ui.HomeScreen
import com.woofbattle.app.ui.MangaTitle
import com.woofbattle.app.ui.WoofTheme
import com.woofbattle.app.ui.drawBreed
import com.woofbattle.app.ui.drawSpeedLines
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
 * Renders the Google Play graphics (icon, feature graphic, phone and tablet screenshots) and the
 * launcher icon layers into build/store. Opt-in: ./gradlew :app:testDebugUnitTest -Pscreenshots
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class ScreenshotTestStoreAssets {

    @get:Rule
    val rule = createComposeRule()

    private val strings = Strings.of(Lang.EN)
    private val settings = Settings(playerCount = 3, favoriteDog = Breed.GOLDEN)
    private val iconOrange = Color(0xFFFFB830)

    private fun save(name: String, settleMs: Long = 1200, content: @Composable () -> Unit) {
        rule.mainClock.autoAdvance = false
        rule.setContent { WoofTheme { content() } }
        rule.mainClock.advanceTimeBy(settleMs)
        val bmp = rule.onRoot().captureToImage().asAndroidBitmap()
        val root = File(System.getProperty("screenshotDir") ?: "build/screenshots").absoluteFile.parentFile ?: File(".")
        val file = File(root, "store/$name.png").apply { parentFile?.mkdirs() }
        file.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun game(mode: Mode, piles: List<List<Int>>, table: List<TableCard>, dogs: List<Breed>, transform: (GameUi) -> GameUi = { it }): GameUi {
        val state = GameEngine.fromPiles(piles, GameConfig(playerCount = piles.size))
        val humans = if (mode == Mode.VS_CPU) listOf(0) else piles.indices.toList()
        return transform(GameUi(Session(mode, humans, state, dogs), state.players.map { it.cardCount }, table = table))
    }

    private fun noop(@Suppress("UNUSED_PARAMETER") i: Int) {}

    private fun DrawScope.iconBackground() {
        drawRect(Brush.radialGradient(listOf(Color(0xFFFFD166), iconOrange, Color(0xFFF58A1F)), center = Offset(size.width / 2, size.height / 2), radius = size.minDimension * 0.75f))
        drawSpeedLines(Offset(size.width / 2, size.height / 2), size.minDimension * 0.2f, size.maxDimension, Color.White.copy(alpha = 0.35f), count = 36, seed = 21)
    }

    // ---------- Launcher icon layers (108dp adaptive canvas, art inside the 66dp safe zone) ----------

    @Test
    @Config(qualifiers = "w432dp-h432dp-mdpi")
    fun launcher_background() = save("launcher/background_432") {
        Canvas(Modifier.fillMaxSize()) { iconBackground() }
    }

    @Test
    @Config(qualifiers = "w432dp-h432dp-mdpi")
    fun launcher_foreground() = save("launcher/foreground_432") {
        Canvas(Modifier.fillMaxSize()) {
            val s = size.minDimension * 0.52f
            drawBreed(Breed.GOLDEN, size.width / 2, size.height * 0.5f + s * 0.5f, s, aura = true, phase = 0.3f)
        }
    }

    // ---------- Play Store listing graphics ----------

    @Test
    @Config(qualifiers = "w512dp-h512dp-mdpi")
    fun play_icon_512() = save("play/icon_512") {
        Canvas(Modifier.fillMaxSize()) {
            iconBackground()
            val s = size.minDimension * 0.72f
            drawBreed(Breed.GOLDEN, size.width / 2, size.height * 0.5f + s * 0.5f, s, aura = true, phase = 0.3f)
        }
    }

    @Test
    @Config(qualifiers = "w1024dp-h500dp-mdpi")
    fun play_feature_graphic() = save("play/feature_graphic_1024x500") {
        Box(Modifier.fillMaxSize()) {
            Canvas(Modifier.fillMaxSize()) {
                iconBackground()
                val h = size.height
                // the Golden (with its aura) goes first so the aura stays behind the others
                val dogs = listOf(Breed.GOLDEN to 0.8f, Breed.CORGI to 0.56f, Breed.HUSKY to 0.62f, Breed.POODLE to 0.5f)
                val xs = listOf(0.73f, 0.56f, 0.88f, 0.98f)
                dogs.forEachIndexed { i, (b, k) ->
                    drawBreed(b, size.width * xs[i], h * 0.96f, h * k, happy = b == Breed.GOLDEN, aura = b == Breed.GOLDEN, phase = 0.4f)
                }
            }
            Column(Modifier.align(Alignment.CenterStart).padding(start = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                MangaTitle("Woof Battle", 86.sp, Color.White, Modifier.graphicsLayer { rotationZ = -4f })
                MangaTitle("WOOF!  OUAF !  ¡GUAU!", 30.sp, Color(0xFFE63946), Modifier.padding(top = 8.dp).graphicsLayer { rotationZ = -4f })
            }
        }
    }

    // ---------- Phone screenshots, 1080 x 1920 ----------

    @Test
    @Config(qualifiers = "w360dp-h640dp-xxhdpi")
    fun phone_1_menu() = save("play/phone_1_menu") {
        HomeScreen(settings, strings, { _, _ -> }, {}, {})
    }

    @Test
    @Config(qualifiers = "w360dp-h640dp-xxhdpi")
    fun phone_2_choose_dogs() = save("play/phone_2_choose_dogs") {
        DogPicker(listOf(Breed.CORGI, null, Breed.POODLE), 1, strings, {}, {}, {}, {})
    }

    @Test
    @Config(qualifiers = "w360dp-h640dp-xxhdpi")
    fun phone_3_game() = save("play/phone_3_game", settleMs = 2500) {
        val ui = game(
            Mode.VS_CPU, listOf(List(17) { 1 }, List(13) { 1 }),
            listOf(TableCard(Card(100, 2), 0, true), TableCard(Card(101, 5), 1, true)),
            listOf(Breed.GOLDEN, Breed.HUSKY),
        ) { it.copy(trickWinner = 1, awaitingContinue = true) }
        GameScreen(ui, settings, strings, ::noop, {}, {}, {}, {})
    }

    @Test
    @Config(qualifiers = "w360dp-h640dp-xxhdpi")
    fun phone_4_woof_battle() = save("play/phone_4_woof_battle", settleMs = 1050) {
        val ui = game(
            Mode.TOGETHER, listOf(List(12) { 1 }, List(14) { 1 }),
            listOf(TableCard(Card(100, 4), 0, true), TableCard(Card(101, 4), 1, true)),
            listOf(Breed.CORGI, Breed.DALMATIAN),
        ) { it.copy(woof = true, battlers = setOf(0, 1)) }
        GameScreen(ui, settings, strings, ::noop, {}, {}, {}, {})
    }

    @Test
    @Config(qualifiers = "w360dp-h640dp-xxhdpi")
    fun phone_5_victory() = save("play/phone_5_victory", settleMs = 1900) {
        val base = game(Mode.VS_CPU, listOf(List(36) { 1 }, emptyList()), emptyList(), listOf(Breed.GOLDEN, Breed.PUG))
        val ui = base.copy(session = base.session.copy(game = base.game.copy(status = GameStatus.Finished(listOf(0), EndReason.ALL_CARDS))))
        GameScreen(ui, settings, strings, ::noop, {}, {}, {}, {})
    }

    // ---------- Tablet screenshots ----------

    @Test
    @Config(qualifiers = "w1280dp-h800dp-xhdpi")
    fun tablet10_1_menu() = save("play/tablet10_1_menu") {
        HomeScreen(settings, strings, { _, _ -> }, {}, {})
    }

    @Test
    @Config(qualifiers = "w1280dp-h800dp-xhdpi")
    fun tablet10_2_four_players_woof() = save("play/tablet10_2_four_players_woof", settleMs = 1050) {
        val ui = game(
            Mode.TOGETHER, listOf(List(8) { 1 }, List(6) { 1 }, List(9) { 1 }, List(7) { 1 }),
            listOf(
                TableCard(Card(100, 5), 0, true), TableCard(Card(101, 5), 1, true),
                TableCard(Card(102, 1), 2, true), TableCard(Card(103, 3), 3, true),
            ),
            listOf(Breed.GOLDEN, Breed.HUSKY, Breed.PUG, Breed.POODLE),
        ) { it.copy(woof = true, battlers = setOf(0, 1)) }
        GameScreen(ui, settings, strings, ::noop, {}, {}, {}, {})
    }

    @Test
    @Config(qualifiers = "w1280dp-h800dp-xhdpi")
    fun tablet10_3_choose_dogs() = save("play/tablet10_3_choose_dogs") {
        DogPicker(listOf(Breed.GOLDEN, Breed.HUSKY, null, Breed.POODLE), 2, strings, {}, {}, {}, {})
    }

    @Test
    @Config(qualifiers = "w960dp-h600dp-xhdpi")
    fun tablet7_1_game() = save("play/tablet7_1_game", settleMs = 2500) {
        val ui = game(
            Mode.TOGETHER, listOf(List(12) { 1 }, List(10) { 1 }, List(11) { 1 }),
            listOf(TableCard(Card(100, 6), 0, true), TableCard(Card(101, 3), 1, true), TableCard(Card(102, 2), 2, true)),
            listOf(Breed.GOLDEN, Breed.BEAGLE, Breed.DALMATIAN),
        ) { it.copy(trickWinner = 0, awaitingContinue = true) }
        GameScreen(ui, settings, strings, ::noop, {}, {}, {}, {})
    }

    @Test
    @Config(qualifiers = "w960dp-h600dp-xhdpi")
    fun tablet7_2_menu() = save("play/tablet7_2_menu") {
        HomeScreen(settings, strings, { _, _ -> }, {}, {})
    }

}
