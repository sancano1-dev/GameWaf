package com.woofbattle.engine

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

const val MIN_VALUE = 1
const val MAX_VALUE = 6
const val COPIES_PER_VALUE = 6
const val DECK_SIZE = (MAX_VALUE - MIN_VALUE + 1) * COPIES_PER_VALUE // 36
const val DEFAULT_SHORT_GAME_ROUNDS = 20
const val SAFETY_CAP_ROUNDS = 500

/** One card. [id] is unique within a deck (0..35) so the UI can key animations on it. */
@Serializable
data class Card(val id: Int, val value: Int) {
    init {
        require(value in MIN_VALUE..MAX_VALUE) { "card value $value out of range" }
    }
}

/** A seat at the table. `pile[0]` is the top (next card to flip); new cards go to the end. */
@Serializable
data class Player(val id: Int, val pile: List<Card>) {
    val cardCount: Int get() = pile.size
    val isEliminated: Boolean get() = pile.isEmpty()
}

/**
 * @param roundLimit null = classic game (play until one player holds every card);
 *   otherwise "short game": stop after this many rounds, most cards wins.
 * @param safetyCap hard stop for runaway games; most cards wins.
 */
@Serializable
data class GameConfig(
    val playerCount: Int = 2,
    val roundLimit: Int? = null,
    val safetyCap: Int = SAFETY_CAP_ROUNDS,
) {
    init {
        require(playerCount in 2..4) { "2 to 4 players supported" }
        require(roundLimit == null || roundLimit > 0) { "roundLimit must be positive" }
        require(safetyCap > 0) { "safetyCap must be positive" }
    }
}

enum class EndReason { ALL_CARDS, ROUND_LIMIT, SAFETY_CAP }

@Serializable
sealed interface GameStatus {
    @Serializable
    @SerialName("playing")
    data object Playing : GameStatus

    /** [winners] has more than one id only when several players tie for most cards. */
    @Serializable
    @SerialName("finished")
    data class Finished(val winners: List<Int>, val reason: EndReason) : GameStatus
}

@Serializable
data class GameState(
    val config: GameConfig,
    val players: List<Player>,
    val round: Int = 0,
    val rngState: Long,
    val status: GameStatus = GameStatus.Playing,
) {
    val activePlayers: List<Player> get() = players.filter { !it.isEliminated }
    val totalCards: Int get() = players.sumOf { it.cardCount }
    val isFinished: Boolean get() = status is GameStatus.Finished
}

/** Everything that happened during one round, in order, so the UI can animate it step by step. */
sealed interface RoundEvent {
    /** Cards flipped face-up (the opening flip, and the face-up card of every battle). */
    data class Reveal(val cards: Map<Int, Card>) : RoundEvent

    /** Several players tied for the highest card: "WOOF BATTLE!". */
    data class WoofBattle(val players: List<Int>) : RoundEvent

    /** Hidden cards placed by battling players before their next face-up card. */
    data class FaceDown(val cards: Map<Int, Card>) : RoundEvent

    /** Tied players with no cards left: they lose the battle. */
    data class Forfeit(val players: List<Int>) : RoundEvent

    /** [winner] takes every card on the table (already shuffled, added to the bottom of the pile). */
    data class Collect(val winner: Int, val cards: List<Card>) : RoundEvent

    /** Nobody could continue the battle: every player takes back the cards they put down. */
    data class Returned(val cards: Map<Int, List<Card>>) : RoundEvent

    /** Players who reached zero cards this round. */
    data class Eliminated(val players: List<Int>) : RoundEvent

    data class GameOver(val status: GameStatus.Finished) : RoundEvent
}

data class RoundResult(
    val state: GameState,
    val events: List<RoundEvent>,
    /** Winner of the trick, or null when the table was returned (see [RoundEvent.Returned]). */
    val trickWinner: Int?,
)
