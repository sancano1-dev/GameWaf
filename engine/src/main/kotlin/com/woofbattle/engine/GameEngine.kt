package com.woofbattle.engine

import kotlinx.serialization.json.Json

/**
 * Pure, deterministic rules for Woof Battle. All functions are side-effect free: they take a
 * [GameState] and return a new one. Randomness comes only from the RNG state stored in the game.
 */
object GameEngine {

    /** The 36-card deck: values 1..6, six copies each, ids 0..35. */
    fun fullDeck(): List<Card> = (0 until DECK_SIZE).map { Card(id = it, value = it / COPIES_PER_VALUE + MIN_VALUE) }

    /** Shuffles a full deck with [seed] and deals it round-robin (2p: 18 each, 3p: 12, 4p: 9). */
    fun newGame(config: GameConfig, seed: Long): GameState {
        val rng = SeededRng(seed)
        val deck = fullDeck().toMutableList()
        rng.shuffle(deck)
        val piles = List(config.playerCount) { mutableListOf<Card>() }
        deck.forEachIndexed { i, card -> piles[i % config.playerCount] += card }
        return GameState(
            config = config,
            players = piles.mapIndexed { id, pile -> Player(id, pile) },
            rngState = rng.state,
        )
    }

    /**
     * Builds a game from explicit card values (top of each pile first). Intended for tests and
     * demos; card ids are assigned sequentially. The total need not be 36.
     */
    fun fromPiles(
        piles: List<List<Int>>,
        config: GameConfig = GameConfig(playerCount = piles.size),
        seed: Long = 0L,
        round: Int = 0,
    ): GameState {
        require(piles.size == config.playerCount) { "pile count must match playerCount" }
        var nextId = 0
        val players = piles.mapIndexed { id, values -> Player(id, values.map { Card(nextId++, it) }) }
        return GameState(config = config, players = players, round = round, rngState = seed)
    }

    /** Plays one full round (opening flip plus any chained Woof Battles) and checks end conditions. */
    fun playRound(state: GameState): RoundResult {
        check(!state.isFinished) { "game is already finished" }

        val rng = SeededRng(state.rngState)
        val piles = state.players.map { ArrayDeque(it.pile) }
        val putDown = state.players.associate { it.id to mutableListOf<Card>() }
        val events = mutableListOf<RoundEvent>()
        val startedWithCards = state.players.filter { !it.isEliminated }.map { it.id }

        fun take(id: Int): Card = piles[id].removeFirst().also { putDown.getValue(id) += it }

        // Opening flip: every player still in the game shows their top card.
        var faceUp: Map<Int, Card> = startedWithCards.associateWith { take(it) }
        events += RoundEvent.Reveal(faceUp)

        var trickWinner: Int? = null
        while (true) {
            val best = faceUp.values.maxOf { it.value }
            val leaders = faceUp.filterValues { it.value == best }.keys.sorted()
            if (leaders.size == 1) {
                trickWinner = leaders.single()
                break
            }

            // WOOF BATTLE between the tied players only.
            events += RoundEvent.WoofBattle(leaders)
            val canPlay = leaders.filter { piles[it].isNotEmpty() }
            val forfeits = leaders - canPlay.toSet()
            if (forfeits.isNotEmpty()) events += RoundEvent.Forfeit(forfeits)

            when (canPlay.size) {
                0 -> break // nobody can continue: table is returned below
                1 -> {
                    trickWinner = canPlay.single()
                    break
                }
            }

            // One hidden card each, except a player down to their last card plays it face-up.
            val faceDown = canPlay.filter { piles[it].size >= 2 }.associateWith { take(it) }
            if (faceDown.isNotEmpty()) events += RoundEvent.FaceDown(faceDown)
            faceUp = canPlay.associateWith { take(it) }
            events += RoundEvent.Reveal(faceUp)
        }

        if (trickWinner != null) {
            val won = putDown.values.flatten().toMutableList()
            rng.shuffle(won) // shuffling won cards prevents endless repeating games
            piles[trickWinner].addAll(won)
            events += RoundEvent.Collect(trickWinner, won.toList())
        } else {
            val returned = putDown.filterValues { it.isNotEmpty() }.mapValues { (id, cards) ->
                cards.toMutableList().also { rng.shuffle(it); piles[id].addAll(it) }.toList()
            }
            events += RoundEvent.Returned(returned)
        }

        val players = state.players.map { it.copy(pile = piles[it.id].toList()) }
        val eliminated = startedWithCards.filter { players[it].isEliminated }
        if (eliminated.isNotEmpty()) events += RoundEvent.Eliminated(eliminated)

        val round = state.round + 1
        val status = evaluateStatus(state.config, players, round)
        if (status is GameStatus.Finished) events += RoundEvent.GameOver(status)

        val next = state.copy(players = players, round = round, rngState = rng.state, status = status)
        check(next.totalCards == state.totalCards) { "cards were lost or duplicated" }
        return RoundResult(next, events, trickWinner)
    }

    /** Plays rounds until the game ends. Used by tests and simulations. */
    fun playToEnd(state: GameState): GameState {
        var s = state
        while (!s.isFinished) s = playRound(s).state
        return s
    }

    internal fun evaluateStatus(config: GameConfig, players: List<Player>, round: Int): GameStatus {
        val total = players.sumOf { it.cardCount }
        val alive = players.filter { !it.isEliminated }
        if (alive.size == 1 && alive.single().cardCount == total) {
            return GameStatus.Finished(listOf(alive.single().id), EndReason.ALL_CARDS)
        }
        val reason = when {
            config.roundLimit != null && round >= config.roundLimit -> EndReason.ROUND_LIMIT
            round >= config.safetyCap -> EndReason.SAFETY_CAP
            else -> return GameStatus.Playing
        }
        val most = players.maxOf { it.cardCount }
        return GameStatus.Finished(players.filter { it.cardCount == most }.map { it.id }, reason)
    }
}

/** JSON round-trip for saving a game (rotation / process death). */
object GameStateCodec {
    private val json = Json { ignoreUnknownKeys = true }

    fun encode(state: GameState): String = json.encodeToString(GameState.serializer(), state)

    fun decode(text: String): GameState = json.decodeFromString(GameState.serializer(), text)
}
