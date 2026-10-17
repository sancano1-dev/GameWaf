package com.woofbattle.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GameEngineTest {

    private fun GameState.values(player: Int) = players[player].pile.map { it.value }
    private fun RoundResult.battles() = events.filterIsInstance<RoundEvent.WoofBattle>()

    // ---------- Dealing ----------

    @Test
    fun `deck has 36 cards, six of each value`() {
        val deck = GameEngine.fullDeck()
        assertEquals(36, deck.size)
        assertEquals(36, deck.map { it.id }.toSet().size)
        (1..6).forEach { v -> assertEquals(6, deck.count { it.value == v }) }
    }

    @Test
    fun `deals all cards evenly for 2, 3 and 4 players`() {
        mapOf(2 to 18, 3 to 12, 4 to 9).forEach { (players, each) ->
            val state = GameEngine.newGame(GameConfig(playerCount = players), seed = 42)
            assertEquals(players, state.players.size)
            state.players.forEach { assertEquals(each, it.cardCount) }
            val all = state.players.flatMap { it.pile }
            assertEquals(GameEngine.fullDeck().toSet(), all.toSet())
            assertEquals(GameStatus.Playing, state.status)
        }
    }

    @Test
    fun `dealing is deterministic per seed`() {
        val config = GameConfig(playerCount = 3)
        assertEquals(GameEngine.newGame(config, 7), GameEngine.newGame(config, 7))
        assertNotEquals(GameEngine.newGame(config, 7).players, GameEngine.newGame(config, 8).players)
    }

    // ---------- Normal trick ----------

    @Test
    fun `highest card wins and takes cards to the bottom`() {
        val state = GameEngine.fromPiles(listOf(listOf(5, 1), listOf(3, 2)))
        val result = GameEngine.playRound(state)

        assertEquals(0, result.trickWinner)
        assertEquals(1, result.state.values(0).first()) // untouched card stays on top
        assertEquals(listOf(3, 5), result.state.values(0).drop(1).sorted())
        assertEquals(listOf(2), result.state.values(1))
        assertEquals(1, result.state.round)
        assertTrue(result.battles().isEmpty())
        val collect = result.events.filterIsInstance<RoundEvent.Collect>().single()
        assertEquals(listOf(3, 5), collect.cards.map { it.value }.sorted())
    }

    @Test
    fun `everyone flips at once with four players`() {
        val state = GameEngine.fromPiles(listOf(listOf(2, 1), listOf(6, 1), listOf(3, 1), listOf(1, 1)))
        val result = GameEngine.playRound(state)
        val reveal = result.events.first() as RoundEvent.Reveal
        assertEquals(setOf(0, 1, 2, 3), reveal.cards.keys)
        assertEquals(1, result.trickWinner)
        assertEquals(5, result.state.players[1].cardCount)
    }

    // ---------- Ties ----------

    @Test
    fun `two-way tie - only tied players battle and winner takes everything`() {
        val state = GameEngine.fromPiles(
            listOf(listOf(4, 1, 6, 2), listOf(4, 2, 3, 2), listOf(2, 5, 5)),
        )
        val result = GameEngine.playRound(state)

        assertEquals(listOf(0, 1), result.battles().single().players)
        val faceDown = result.events.filterIsInstance<RoundEvent.FaceDown>().single()
        assertEquals(setOf(0, 1), faceDown.cards.keys) // player 2 is not involved
        assertEquals(0, result.trickWinner)
        assertEquals(8, result.state.players[0].cardCount) // 1 left + 7 won
        assertEquals(listOf(2), result.state.values(1))
        assertEquals(listOf(5, 5), result.state.values(2))
    }

    @Test
    fun `three-way tie`() {
        val state = GameEngine.fromPiles(listOf(listOf(5, 1, 2), listOf(5, 1, 6), listOf(5, 1, 3)))
        val result = GameEngine.playRound(state)

        assertEquals(listOf(0, 1, 2), result.battles().single().players)
        assertEquals(1, result.trickWinner)
        assertEquals(9, result.state.players[1].cardCount)
        assertIs<GameStatus.Finished>(result.state.status)
    }

    @Test
    fun `tie between two of three leaders after a three-way tie narrows the battle`() {
        val state = GameEngine.fromPiles(
            listOf(listOf(5, 1, 6, 1, 2), listOf(5, 1, 6, 1, 4), listOf(5, 1, 3, 1, 6)),
        )
        val result = GameEngine.playRound(state)
        val battles = result.battles()
        assertEquals(listOf(0, 1, 2), battles[0].players)
        assertEquals(listOf(0, 1), battles[1].players)
        assertEquals(1, result.trickWinner)
        assertEquals(listOf(1, 6), result.state.values(2)) // player 2 left the second battle
    }

    @Test
    fun `chained ties repeat until broken`() {
        val state = GameEngine.fromPiles(listOf(listOf(3, 1, 4, 1, 6), listOf(3, 1, 4, 1, 2)))
        val result = GameEngine.playRound(state)

        assertEquals(2, result.battles().size)
        assertEquals(0, result.trickWinner)
        assertEquals(10, result.state.players[0].cardCount)
        assertEquals(GameStatus.Finished(listOf(0), EndReason.ALL_CARDS), result.state.status)
    }

    // ---------- Running out mid-battle ----------

    @Test
    fun `player with one card left uses it face-up`() {
        val state = GameEngine.fromPiles(listOf(listOf(3, 5), listOf(3, 1, 4, 2)))
        val result = GameEngine.playRound(state)

        val faceDown = result.events.filterIsInstance<RoundEvent.FaceDown>().single()
        assertEquals(setOf(1), faceDown.cards.keys) // player 0 placed no hidden card
        val battleReveal = result.events.filterIsInstance<RoundEvent.Reveal>()[1]
        assertEquals(5, battleReveal.cards.getValue(0).value)
        assertEquals(0, result.trickWinner)
        assertEquals(5, result.state.players[0].cardCount)
        assertEquals(listOf(2), result.state.values(1))
    }

    @Test
    fun `player with exactly two cards places one hidden and one face-up`() {
        val state = GameEngine.fromPiles(listOf(listOf(3, 1, 2), listOf(3, 1, 6, 4)))
        val result = GameEngine.playRound(state)
        assertEquals(setOf(0, 1), result.events.filterIsInstance<RoundEvent.FaceDown>().single().cards.keys)
        assertEquals(1, result.trickWinner)
        assertTrue(result.state.players[0].isEliminated)
    }

    @Test
    fun `player with no cards at all loses the battle`() {
        val state = GameEngine.fromPiles(listOf(listOf(3), listOf(3, 1, 2)))
        val result = GameEngine.playRound(state)

        assertEquals(listOf(0), result.events.filterIsInstance<RoundEvent.Forfeit>().single().players)
        assertTrue(result.events.none { it is RoundEvent.FaceDown }) // nothing more to play
        assertEquals(1, result.trickWinner)
        assertEquals(listOf(1, 2), result.state.values(1).take(2))
        assertEquals(4, result.state.players[1].cardCount)
        assertEquals(GameStatus.Finished(listOf(1), EndReason.ALL_CARDS), result.state.status)
    }

    @Test
    fun `remaining battlers continue when one tied player forfeits`() {
        val state = GameEngine.fromPiles(listOf(listOf(6), listOf(6, 1, 5), listOf(6, 1, 4)))
        val result = GameEngine.playRound(state)

        assertEquals(listOf(0), result.events.filterIsInstance<RoundEvent.Forfeit>().single().players)
        assertEquals(1, result.trickWinner)
        assertEquals(7, result.state.players[1].cardCount)
        assertEquals(listOf(0, 2), result.events.filterIsInstance<RoundEvent.Eliminated>().single().players)
    }

    @Test
    fun `when every tied player is out of cards the table is returned`() {
        val state = GameEngine.fromPiles(listOf(listOf(3), listOf(3), listOf(1, 2)))
        val result = GameEngine.playRound(state)

        assertNull(result.trickWinner)
        val returned = result.events.filterIsInstance<RoundEvent.Returned>().single()
        assertEquals(setOf(0, 1, 2), returned.cards.keys)
        assertEquals(listOf(3), result.state.values(0))
        assertEquals(listOf(3), result.state.values(1))
        assertEquals(listOf(2, 1), result.state.values(2)) // own card back at the bottom
        assertEquals(GameStatus.Playing, result.state.status)
        assertEquals(1, result.state.round)
    }

    // ---------- Elimination ----------

    @Test
    fun `player with zero cards is eliminated and skipped afterwards`() {
        val state = GameEngine.fromPiles(listOf(listOf(6, 6), listOf(1), listOf(2, 3)))
        val first = GameEngine.playRound(state)

        assertEquals(listOf(1), first.events.filterIsInstance<RoundEvent.Eliminated>().single().players)
        assertTrue(first.state.players[1].isEliminated)
        assertEquals(GameStatus.Playing, first.state.status)
        assertEquals(listOf(0, 2), first.state.activePlayers.map { it.id })

        val second = GameEngine.playRound(first.state)
        val reveal = second.events.first() as RoundEvent.Reveal
        assertEquals(setOf(0, 2), reveal.cards.keys)
    }

    // ---------- End conditions ----------

    @Test
    fun `classic game ends when one player holds every card`() {
        val result = GameEngine.playRound(GameEngine.fromPiles(listOf(listOf(6), listOf(1))))
        assertEquals(GameStatus.Finished(listOf(0), EndReason.ALL_CARDS), result.state.status)
        assertIs<RoundEvent.GameOver>(result.events.last())
    }

    @Test
    fun `short game ends after N rounds and most cards wins`() {
        val config = GameConfig(playerCount = 2, roundLimit = 2)
        val state = GameEngine.fromPiles(listOf(listOf(6, 6, 1, 1), listOf(1, 1, 2, 2)), config)
        val afterOne = GameEngine.playRound(state).state
        assertEquals(GameStatus.Playing, afterOne.status)
        val afterTwo = GameEngine.playRound(afterOne).state
        assertEquals(GameStatus.Finished(listOf(0), EndReason.ROUND_LIMIT), afterTwo.status)
    }

    @Test
    fun `short game with equal piles has several winners`() {
        val config = GameConfig(playerCount = 3, roundLimit = 1)
        val state = GameEngine.fromPiles(listOf(listOf(6, 1), listOf(1, 1, 1, 1), listOf(2, 1, 1)), config)
        val end = GameEngine.playRound(state).state
        assertEquals(GameStatus.Finished(listOf(0), EndReason.ROUND_LIMIT), end.status)

        val tieState = GameEngine.fromPiles(listOf(listOf(6, 1), listOf(1, 1, 1, 1, 1), listOf(2, 1)), config)
        val tieEnd = GameEngine.playRound(tieState).state
        assertEquals(GameStatus.Finished(listOf(0, 1), EndReason.ROUND_LIMIT), tieEnd.status)
    }

    @Test
    fun `default short game lasts at most 20 rounds`() {
        repeat(50) { seed ->
            val end = GameEngine.playToEnd(
                GameEngine.newGame(GameConfig(playerCount = 4, roundLimit = DEFAULT_SHORT_GAME_ROUNDS), seed.toLong()),
            )
            assertTrue(end.round <= 20)
            val status = end.status as GameStatus.Finished
            if (status.reason == EndReason.ROUND_LIMIT) assertEquals(20, end.round)
        }
    }

    @Test
    fun `safety cap stops a game at 500 rounds`() {
        val state = GameEngine.fromPiles(listOf(listOf(5, 1, 1), listOf(2, 1)), round = 499)
        val result = GameEngine.playRound(state)
        assertEquals(500, result.state.round)
        assertEquals(GameStatus.Finished(listOf(0), EndReason.SAFETY_CAP), result.state.status)
    }

    @Test
    fun `safety cap is applied through full play`() {
        val config = GameConfig(playerCount = 2, safetyCap = 3)
        val end = GameEngine.playToEnd(GameEngine.newGame(config, seed = 1))
        assertEquals(3, end.round)
        assertEquals(EndReason.SAFETY_CAP, (end.status as GameStatus.Finished).reason)
    }

    @Test
    fun `cannot play a finished game`() {
        val end = GameEngine.playRound(GameEngine.fromPiles(listOf(listOf(6), listOf(1)))).state
        assertFailsWith<IllegalStateException> { GameEngine.playRound(end) }
    }

    // ---------- Whole-game properties ----------

    @Test
    fun `random games always conserve cards and finish within the cap`() {
        for (players in 2..4) {
            repeat(300) { seed ->
                var s = GameEngine.newGame(GameConfig(playerCount = players), seed.toLong())
                while (!s.isFinished) {
                    s = GameEngine.playRound(s).state
                    assertEquals(DECK_SIZE, s.totalCards)
                    assertEquals(DECK_SIZE, s.players.flatMap { it.pile }.map { it.id }.toSet().size)
                }
                assertTrue(s.round <= SAFETY_CAP_ROUNDS)
                val status = s.status as GameStatus.Finished
                val most = s.players.maxOf { it.cardCount }
                status.winners.forEach { assertEquals(most, s.players[it].cardCount) }
                if (status.reason == EndReason.ALL_CARDS) assertEquals(DECK_SIZE, s.players[status.winners.single()].cardCount)
            }
        }
    }

    @Test
    fun `games are fully deterministic and survive serialization`() {
        val start = GameEngine.newGame(GameConfig(playerCount = 3), seed = 99)
        var a = start
        repeat(10) { a = GameEngine.playRound(a).state }

        val restored = GameStateCodec.decode(GameStateCodec.encode(a))
        assertEquals(a, restored)
        assertEquals(GameEngine.playToEnd(a), GameEngine.playToEnd(restored))
        assertEquals(GameEngine.playToEnd(start), GameEngine.playToEnd(GameEngine.newGame(GameConfig(playerCount = 3), 99)))

        val finished = GameEngine.playToEnd(start)
        assertEquals(finished, GameStateCodec.decode(GameStateCodec.encode(finished)))
        assertFalse(GameStateCodec.encode(finished).isBlank())
    }
}
