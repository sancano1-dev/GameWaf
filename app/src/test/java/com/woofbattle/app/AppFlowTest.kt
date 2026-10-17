package com.woofbattle.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.woofbattle.app.ui.Breed
import com.woofbattle.engine.DECK_SIZE
import com.woofbattle.engine.GameStatus
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * End-to-end checks of every menu option, driving the real MainActivity and GameViewModel the way
 * a user would (taps, long-press, Back, rotation).
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
class AppFlowTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private val vm get() = ViewModelProvider(rule.activity)[GameViewModel::class.java]
    private val ui get() = vm.ui.value

    /** Lets [ms] of time pass for both the ViewModel (main looper) and Compose animations. */
    private fun advance(ms: Long, step: Long = 100) {
        var left = ms
        while (left > 0) {
            ShadowLooper.idleMainLooper(step, TimeUnit.MILLISECONDS)
            rule.mainClock.advanceTimeBy(step)
            left -= step
        }
        rule.waitForIdle()
    }

    private fun tap(description: String) {
        rule.onNodeWithContentDescription(description).performClick()
        advance(300)
    }

    private fun exists(description: String) = rule.onAllNodesWithContentDescription(description).fetchSemanticsNodes().isNotEmpty()

    /** On launch the child picks their dog first, then reaches the main menu. */
    private fun passDogPicker(dog: String = "Golden") {
        rule.mainClock.autoAdvance = false // time only moves when advance() says so
        advance(300)
        assertEquals(Screen.DOGS, ui.screen, "app should open on Choose your dog")
        rule.onNodeWithText("Choose your dog").assertIsDisplayed()
        tap(dog)
        tap("done")
        assertEquals(Screen.HOME, ui.screen)
    }

    @Test
    fun launch_opens_choose_your_dog_then_menu() {
        passDogPicker("Husky")
        assertEquals(Breed.HUSKY, ui.settings.favoriteDog)
        assertTrue(exists("play against dogs"))
        assertTrue(exists("play together"))
        assertTrue(exists("watch a demo"))
    }

    @Test
    fun menu_tiles_open_their_own_pickers_not_the_dog_picker() {
        passDogPicker()
        tap("play against dogs")
        assertEquals(Screen.HOME, ui.screen)
        listOf("1 vs 1", "1 vs 2", "1 vs 3").forEach { assertTrue(exists(it), "missing $it") }
        assertFalse(exists("Husky"), "dog picker must not open from the red tile")
        tap("back")
        tap("play together")
        listOf("2", "3", "4").forEach { assertTrue(exists(it), "missing $it") }
        assertFalse(exists("Husky"))
        tap("back")
        assertTrue(exists("play against dogs"))
    }

    @Test
    fun my_dog_on_the_menu_reopens_the_dog_picker() {
        passDogPicker()
        tap("Choose your dog")
        assertEquals(Screen.DOGS, ui.screen)
        tap("Pug")
        assertEquals(Breed.PUG, ui.settings.favoriteDog)
        tap("done")
        assertEquals(Screen.HOME, ui.screen)
    }

    @Test
    fun one_vs_one_against_the_computer() {
        passDogPicker("Corgi")
        tap("play against dogs")
        tap("1 vs 1")
        assertEquals(Screen.GAME, ui.screen)
        val g = ui.game!!
        assertEquals(2, g.game.players.size)
        assertEquals(listOf(0), g.session.humans)
        assertEquals(Breed.CORGI, g.dogOf(0))
        assertTrue(g.dealing)
        advance(2500)
        assertFalse(ui.game!!.dealing)
        assertEquals(listOf(18, 18), ui.game!!.pileCounts)

        tap("seat 0")
        advance(8000)
        assertTrue(ui.game!!.game.round >= 1, "tapping my flip zone should play a round")
        assertEquals(DECK_SIZE, ui.game!!.game.totalCards)
        tap("seat 1") // the computer's zone is not tappable for the child
    }

    @Test
    fun one_vs_three_against_the_computer() {
        passDogPicker()
        tap("play against dogs")
        tap("1 vs 3")
        assertEquals(4, ui.game!!.game.players.size)
        assertEquals(4, ui.game!!.session.dogs.distinct().size)
        advance(2500)
        assertEquals(listOf(9, 9, 9, 9), ui.game!!.pileCounts)
    }

    @Test
    fun four_children_on_one_phone_need_everyone_to_tap() {
        passDogPicker()
        tap("play together")
        tap("4")
        assertEquals(listOf(0, 1, 2, 3), ui.game!!.session.humans)
        advance(2500)
        tap("seat 0"); tap("seat 1"); tap("seat 2")
        advance(3000)
        assertEquals(0, ui.game!!.game.round, "round must wait for the 4th player")
        tap("seat 3")
        advance(8000)
        assertTrue(ui.game!!.game.round >= 1)
    }

    @Test
    fun demo_plays_by_itself() {
        passDogPicker()
        tap("watch a demo")
        advance(20000, step = 250)
        assertTrue(ui.game!!.game.round >= 2, "demo should keep flipping on its own")
    }

    @Test
    @Config(qualifiers = "w411dp-h1500dp-xxhdpi") // tall enough to show every parent setting
    fun parent_gate_needs_a_three_second_hold_and_settings_apply() {
        passDogPicker()
        val gate = rule.onNodeWithContentDescription("Parents")
        gate.performClick()
        advance(500)
        assertFalse(exists("close"), "a quick tap must not open the parent area")

        gate.performTouchInput { down(center) }
        advance(3300)
        gate.performTouchInput { up() }
        advance(300)
        assertTrue(exists("close"), "holding 3 s opens the parent area")

        rule.onNodeWithText("Español").performClick()
        advance(200)
        assertEquals(Lang.ES, ui.settings.lang)
        rule.onNodeWithText("Jugadores en demo").assertIsDisplayed()
        rule.onNodeWithText("4").performClick()
        advance(200)
        assertEquals(4, ui.settings.playerCount)
        tap("close")
        assertFalse(exists("close"))
    }

    @Test
    fun short_game_reaches_victory_then_replay_and_home() {
        passDogPicker()
        vm.updateSettings { it.copy(shortGame = true, shortRounds = 3, autoPlay = true) }
        tap("play together")
        tap("2")
        var waited = 0
        while (ui.game?.showVictory != true && waited < 120_000) {
            advance(2000, step = 500); waited += 2000
        }
        val status = ui.game!!.game.status
        assertIs<GameStatus.Finished>(status)
        assertTrue(exists("replay") && exists("home"))

        tap("replay")
        assertEquals(0, ui.game!!.game.round)
        assertEquals(2, ui.game!!.game.players.size)
        vm.updateSettings { it.copy(autoPlay = false) }
        advance(2500)
        vm.goHome()
        advance(300)
        assertEquals(Screen.HOME, ui.screen)
    }

    @Test
    fun back_is_ignored_in_game_and_rotation_keeps_the_game() {
        passDogPicker()
        tap("play against dogs")
        tap("1 vs 2")
        advance(2500)
        tap("seat 0")
        advance(8000)
        val before = ui.game!!.game

        rule.activity.onBackPressedDispatcher.onBackPressed()
        advance(300)
        assertEquals(Screen.GAME, ui.screen, "Back must not leave a game")

        rule.activityRule.scenario.recreate()
        advance(500)
        assertEquals(Screen.GAME, ui.screen)
        assertEquals(before, ui.game!!.game)
    }
}
