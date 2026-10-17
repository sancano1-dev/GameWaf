package com.woofbattle.app

import com.woofbattle.engine.GameEngine
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SessionTest {

    @Test
    fun `session survives a JSON round trip as saved in SavedStateHandle`() {
        var game = Settings(playerCount = 3, shortGame = true).gameConfig().let { GameEngine.newGame(it, 5) }
        repeat(4) { game = GameEngine.playRound(game).state }
        val session = Session(Mode.TOGETHER, listOf(0, 1, 2), game, listOf(com.woofbattle.app.ui.Breed.PUG, com.woofbattle.app.ui.Breed.HUSKY, com.woofbattle.app.ui.Breed.CORGI))

        val text = Json.encodeToString(Session.serializer(), session)
        assertEquals(session, Json.decodeFromString(Session.serializer(), text))
    }

    @Test
    fun `settings map to engine config`() {
        assertNull(Settings(shortGame = false).gameConfig().roundLimit)
        assertEquals(30, Settings(playerCount = 4, shortGame = true, shortRounds = 30).gameConfig().roundLimit)
        assertEquals(4, Settings(playerCount = 4).gameConfig().playerCount)
    }

    @Test
    fun `every sound theme synthesizes every sound`() {
        SoundTheme.entries.forEach { theme ->
            Sfx.entries.forEach { sfx ->
                val pcm = Synth.render(theme, sfx)
                assert(pcm.size > Synth.RATE / 50) { "$theme/$sfx too short" }
                assert(pcm.any { it > 1000 }) { "$theme/$sfx is silent" }
            }
        }
    }

    @Test
    fun `every language has every label`() {
        Lang.entries.forEach { lang ->
            val s = Strings.of(lang)
            listOf(s.woof, s.players, s.gameLength, s.sound, s.soundStyle, s.language, s.parentArea, s.chooseDog, s.privacyTitle, s.privacyText).forEach { assert(it.isNotBlank()) }
            assertEquals(com.woofbattle.app.ui.Breed.entries.size, s.breedNames.size)
        }
    }
}
