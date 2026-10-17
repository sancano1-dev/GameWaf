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
import kotlin.test.assertNull
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

    /** The app opens on the menu. */
    private fun launch() {
        rule.mainClock.autoAdvance = false // time only moves when advance() says so
        advance(300)
        assertEquals(Screen.HOME, ui.screen, "app should open on the menu")
    }

    /** Menu tile -> player count -> one dog per human player -> play. */
    private fun startGame(tile: String, option: String, vararg dogs: String) {
        tap(tile)
        tap(option)
        assertEquals(Screen.DOGS, ui.screen, "choosing dogs comes right after the game type")
        rule.onNodeWithText("Choose your dog").assertIsDisplayed()
        assertEquals(dogs.size, ui.pending!!.picks.size, "one dog slot per human player")
        dogs.forEach { tap(it) }
        tap("play")
        assertEquals(Screen.GAME, ui.screen)
        advance(2500) // shuffle and deal
    }

    /** Lets the round play out, checks it waits on the table, then taps the screen to go on. */
    private fun finishRound() {
        var waited = 0
        while (ui.game?.awaitingContinue != true && waited < 30_000) {
            advance(500); waited += 500
        }
        assertTrue(ui.game!!.awaitingContinue, "a finished round should wait for a tap")
        tap("next round")
        advance(3000)
    }

    @Test
    fun launch_opens_the_menu() {
        launch()
        assertTrue(exists("play against dogs"))
        assertTrue(exists("play together"))
        assertTrue(exists("watch a demo"))
        assertFalse(exists("Husky"), "no dog picker before a game type is chosen")
    }

    @Test
    fun menu_tiles_open_their_player_count_pickers() {
        launch()
        tap("play against dogs")
        listOf("1 vs 1", "1 vs 2", "1 vs 3").forEach { assertTrue(exists(it), "missing $it") }
        tap("back")
        tap("play together")
        listOf("2", "3", "4").forEach { assertTrue(exists(it), "missing $it") }
        tap("back")
        assertTrue(exists("play against dogs"))
    }

    @Test
    fun vs_dogs_asks_for_exactly_one_dog() {
        launch()
        tap("play against dogs")
        tap("1 vs 3")
        assertEquals(Screen.DOGS, ui.screen)
        assertEquals(listOf<Breed?>(null), ui.pending!!.picks)
        assertTrue(exists("player 1"))
        assertFalse(exists("player 2"))

        tap("play")
        assertEquals(Screen.DOGS, ui.screen, "play does nothing until the dog is chosen")

        tap("Corgi")
        tap("Husky") // changing your mind replaces the pick
        assertEquals(listOf<Breed?>(Breed.HUSKY), ui.pending!!.picks)
        tap("play")
        assertEquals(Screen.GAME, ui.screen)
        val g = ui.game!!
        assertEquals(4, g.game.players.size)
        assertEquals(Breed.HUSKY, g.dogOf(0))
        assertEquals(4, g.session.dogs.distinct().size, "computer dogs get other breeds")
        assertEquals(Breed.HUSKY, ui.settings.favoriteDog)
        advance(2500)
        assertEquals(listOf(9, 9, 9, 9), ui.game!!.pileCounts)
    }

    @Test
    fun each_child_picks_a_different_dog() {
        launch()
        tap("play together")
        tap("3")
        assertEquals(listOf<Breed?>(null, null, null), ui.pending!!.picks)
        assertEquals(0, ui.pending!!.active)

        tap("Golden")
        assertEquals(1, ui.pending!!.active, "next player's turn")
        tap("Golden") // already taken by player 1
        assertEquals(listOf(Breed.GOLDEN, null, null), ui.pending!!.picks, "the same dog cannot be picked twice")
        tap("play")
        assertEquals(Screen.DOGS, ui.screen, "can't start until every child has a dog")

        tap("Pug")
        tap("Dalmatian")
        assertEquals(listOf(Breed.GOLDEN, Breed.PUG, Breed.DALMATIAN), ui.pending!!.picks)

        // player 2 changes their mind: tap their slot, then a free dog
        tap("player 2")
        tap("Poodle")
        assertEquals(listOf(Breed.GOLDEN, Breed.POODLE, Breed.DALMATIAN), ui.pending!!.picks)
        tap("player 3")
        tap("Golden") // still taken by player 1
        assertEquals(Breed.DALMATIAN, ui.pending!!.picks[2])

        tap("play")
        val g = ui.game!!
        assertEquals(listOf(0, 1, 2), g.session.humans)
        assertEquals(listOf(Breed.GOLDEN, Breed.POODLE, Breed.DALMATIAN), g.session.dogs)
    }

    @Test
    fun back_from_dog_picker_returns_to_menu() {
        launch()
        tap("play together")
        tap("2")
        tap("Beagle")
        tap("back")
        assertEquals(Screen.HOME, ui.screen)
        assertNull(ui.pending)
        assertTrue(exists("play together"))
    }

    @Test
    fun rotation_keeps_the_dog_choices() {
        launch()
        tap("play together")
        tap("2")
        tap("Corgi")
        rule.activityRule.scenario.recreate()
        advance(500)
        assertEquals(Screen.DOGS, ui.screen)
        assertEquals(listOf(Breed.CORGI, null), ui.pending!!.picks)
    }

    @Test
    fun demo_starts_without_choosing_dogs() {
        launch()
        tap("watch a demo")
        assertEquals(Screen.GAME, ui.screen)
        advance(20000, step = 250)
        assertTrue(ui.game!!.game.round >= 2, "demo should keep flipping on its own")
    }

    @Test
    fun one_vs_one_against_the_computer() {
        launch()
        startGame("play against dogs", "1 vs 1", "Corgi")
        val g = ui.game!!
        assertEquals(2, g.game.players.size)
        assertEquals(listOf(0), g.session.humans)
        assertEquals(Breed.CORGI, g.dogOf(0))
        assertEquals(listOf(18, 18), ui.game!!.pileCounts)

        tap("seat 0")
        finishRound()
        assertTrue(ui.game!!.game.round >= 1, "tapping my flip zone should play a round")
        assertEquals(DECK_SIZE, ui.game!!.game.totalCards)
        tap("seat 1") // the computer's zone is not tappable for the child
    }

    @Test
    fun four_children_on_one_phone_need_everyone_to_tap() {
        launch()
        startGame("play together", "4", "Golden", "Husky", "Pug", "Poodle")
        assertEquals(listOf(0, 1, 2, 3), ui.game!!.session.humans)
        assertEquals(listOf(Breed.GOLDEN, Breed.HUSKY, Breed.PUG, Breed.POODLE), ui.game!!.session.dogs)
        tap("seat 0"); tap("seat 1"); tap("seat 2")
        advance(3000)
        assertEquals(0, ui.game!!.game.round, "round must wait for the 4th player")
        tap("seat 3")
        finishRound()
        assertTrue(ui.game!!.game.round >= 1)
    }

    @Test
    fun every_round_waits_for_a_tap_on_the_screen() {
        launch()
        startGame("play against dogs", "1 vs 1", "Beagle")
        tap("seat 0")
        var waited = 0
        while (ui.game?.awaitingContinue != true && waited < 30_000) {
            advance(500); waited += 500
        }
        assertTrue(ui.game!!.awaitingContinue)
        assertTrue(ui.game!!.table.isNotEmpty(), "the round's cards stay on the table")

        advance(10_000)
        assertTrue(ui.game!!.awaitingContinue, "nothing moves on without a tap")
        assertEquals(0, ui.game!!.game.round)

        tap("next round")
        advance(3000)
        assertFalse(ui.game!!.awaitingContinue)
        assertTrue(ui.game!!.table.isEmpty(), "after the tap the cards go to the winner")
        assertEquals(1, ui.game!!.game.round)

        tap("seat 0")
        finishRound()
        assertEquals(2, ui.game!!.game.round)
    }

    @Test
    @Config(qualifiers = "w411dp-h1500dp-xxhdpi") // tall enough to show every parent setting
    fun parent_gate_needs_a_three_second_hold_and_settings_apply() {
        launch()
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
    fun short_game_reaches_victory_then_replay_keeps_the_dogs() {
        launch()
        vm.updateSettings { it.copy(shortGame = true, shortRounds = 3, autoPlay = true) }
        startGame("play together", "2", "Dachshund", "Corgi")
        var waited = 0
        while (ui.game?.showVictory != true && waited < 120_000) {
            advance(2000, step = 500); waited += 2000
        }
        assertIs<GameStatus.Finished>(ui.game!!.game.status)
        advance(500) // let the victory screen draw
        assertTrue(exists("replay") && exists("home"))

        tap("replay")
        assertEquals(0, ui.game!!.game.round)
        assertEquals(listOf(Breed.DACHSHUND, Breed.CORGI), ui.game!!.session.dogs, "replay keeps everyone's dog")
        vm.updateSettings { it.copy(autoPlay = false) }
        advance(2500)
        vm.goHome()
        advance(300)
        assertEquals(Screen.HOME, ui.screen)
    }

    @Test
    fun back_is_ignored_in_game_and_rotation_keeps_the_game() {
        launch()
        startGame("play against dogs", "1 vs 2", "Pug")
        tap("seat 0")
        finishRound()
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
