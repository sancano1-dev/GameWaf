package com.woofbattle.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.woofbattle.app.ui.Breed
import com.woofbattle.engine.Card
import com.woofbattle.engine.GameEngine
import com.woofbattle.engine.GameState
import com.woofbattle.engine.RoundEvent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

enum class Mode { VS_CPU, TOGETHER, DEMO }

/** DOGS = "Choose your dog", shown after picking a game type, one dog per human player. */
enum class Screen { HOME, DOGS, GAME }

/** A game being set up: each human player picks a different dog before it starts. */
@Serializable
data class PendingGame(
    val mode: Mode,
    val players: Int,
    /** One entry per human player, in seat order; null = not chosen yet. */
    val picks: List<Breed?>,
    /** Which player is choosing now. */
    val active: Int = 0,
) {
    val complete: Boolean get() = picks.none { it == null }
}

/** What must survive rotation and process death: the mode, which seats are human, the engine state. */
@Serializable
data class Session(
    val mode: Mode,
    val humans: List<Int>,
    val game: GameState,
    /** Dog shown for each seat; seat 0 is the child's favourite. */
    val dogs: List<Breed> = emptyList(),
)

data class TableCard(val card: Card, val owner: Int, val faceUp: Boolean)

data class GameUi(
    val session: Session,
    /** Pile sizes as currently shown (they shrink as cards are flipped during the animation). */
    val pileCounts: List<Int>,
    /** Cards on the table, in the order they were put down. */
    val table: List<TableCard> = emptyList(),
    /** Human seats that already tapped their flip zone this round. */
    val ready: Set<Int> = emptySet(),
    val animating: Boolean = false,
    val woof: Boolean = false,
    /** Incremented to trigger a screen shake. */
    val shakeTick: Int = 0,
    val battlers: Set<Int> = emptySet(),
    val trickWinner: Int? = null,
    /** When set, every table card slides to this seat's pile. */
    val collectTo: Int? = null,
    /** When true, table cards slide back to their owners (nobody could finish the battle). */
    val returning: Boolean = false,
    /** Start-of-game shuffle and deal animation; taps are ignored meanwhile. */
    val dealing: Boolean = false,
    /** Round decided: cards stay on the table until someone taps the screen to go on. */
    val awaitingContinue: Boolean = false,
) {
    val game: GameState get() = session.game
    val showVictory: Boolean get() = game.isFinished && !animating
    fun isHuman(seat: Int) = seat in session.humans
    fun dogOf(seat: Int): Breed = session.dogs.getOrNull(seat) ?: Breed.entries[seat % Breed.entries.size]
}

data class AppUi(
    val screen: Screen = Screen.HOME,
    val settings: Settings = Settings(),
    val game: GameUi? = null,
    val pending: PendingGame? = null,
)

class GameViewModel(app: Application, private val handle: SavedStateHandle) : AndroidViewModel(app) {

    private val settingsRepo = SettingsRepository(app)
    @Volatile private var sounds: SoundPlayer? = null
    private var roundJob: Job? = null
    private var autoJob: Job? = null
    /** Completed by [onContinueTap] while a finished round waits on the table. */
    private var continueSignal: CompletableDeferred<Unit>? = null

    private val _ui = MutableStateFlow(restore())
    val ui: StateFlow<AppUi> = _ui.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            sounds = SoundPlayer(app).also {
                it.enabled = _ui.value.settings.soundOn
                it.theme = _ui.value.settings.soundTheme
            }
        }
        scheduleAutoFlip()
    }

    // ---------- Navigation ----------

    /**
     * Starts a fresh, randomly shuffled game. [players] is the total number of seats: in
     * [Mode.VS_CPU] that is the child plus players - 1 computer dogs (2 = one-against-one).
     */
    /**
     * The child picked a game type and player count. Demo starts at once; otherwise every human
     * player first chooses a dog (1 for "vs dogs", one per child for "together").
     */
    fun chooseGame(mode: Mode, players: Int) {
        if (mode == Mode.DEMO) return startGame(mode, players)
        val humans = if (mode == Mode.VS_CPU) 1 else players
        val pending = PendingGame(mode, players, List(humans) { null })
        savePending(pending)
        handle[KEY_SCREEN] = Screen.DOGS.name
        _ui.update { it.copy(screen = Screen.DOGS, pending = pending) }
        play(Sfx.TAP)
    }

    /** Tap on a player's slot: that player chooses (or changes) their dog next. */
    fun selectPicker(player: Int) {
        val p = _ui.value.pending ?: return
        if (player !in p.picks.indices) return
        updatePending(p.copy(active = player))
    }

    /** The active player taps a dog. A dog already taken by another player cannot be chosen. */
    fun pickDog(dog: Breed) {
        val p = _ui.value.pending ?: return
        val owner = p.picks.indexOf(dog)
        if (owner >= 0 && owner != p.active) {
            play(Sfx.SAD)
            return
        }
        val picks = p.picks.toMutableList().also { it[p.active] = dog }
        val nextActive = picks.indices.firstOrNull { picks[it] == null && it > p.active }
            ?: picks.indices.firstOrNull { picks[it] == null }
            ?: p.active
        updatePending(p.copy(picks = picks, active = nextActive))
        play(Sfx.BARK)
    }

    /** Green play button: everyone has a dog, start the game. */
    fun startPendingGame() {
        val p = _ui.value.pending ?: return
        if (!p.complete) return
        startGame(p.mode, p.players, p.picks.filterNotNull())
    }

    /** Back from Choose your dog: return to the menu. */
    fun cancelDogPicker() {
        savePending(null)
        handle[KEY_SCREEN] = Screen.HOME.name
        _ui.update { it.copy(screen = Screen.HOME, pending = null) }
    }

    private fun updatePending(p: PendingGame) {
        savePending(p)
        _ui.update { it.copy(pending = p) }
    }

    private fun savePending(p: PendingGame?) {
        if (p == null) handle.remove<String>(KEY_PENDING)
        else handle[KEY_PENDING] = json.encodeToString(PendingGame.serializer(), p)
    }

    /**
     * Starts a fresh, randomly shuffled game. [humanDogs] are the dogs the human players chose, in
     * seat order; the other seats get different random breeds.
     */
    fun startGame(mode: Mode, players: Int = _ui.value.settings.playerCount, humanDogs: List<Breed> = emptyList()) {
        cancelJobs()
        val count = players.coerceIn(2, 4)
        if (count != _ui.value.settings.playerCount) updateSettings { it.copy(playerCount = count) }
        val config = _ui.value.settings.gameConfig()
        val humans = when (mode) {
            Mode.VS_CPU -> listOf(0)
            Mode.TOGETHER -> (0 until config.playerCount).toList()
            Mode.DEMO -> emptyList()
        }
        val game = GameEngine.newGame(config, seed = System.nanoTime())
        val dogs = dogsFor(config.playerCount, humanDogs)
        humanDogs.firstOrNull()?.let { first -> if (first != _ui.value.settings.favoriteDog) updateSettings { it.copy(favoriteDog = first) } }
        val session = Session(mode, humans, game, dogs)
        savePending(null)
        persist(session, Screen.GAME)
        _ui.update { it.copy(screen = Screen.GAME, pending = null, game = GameUi(session, game.players.map { 0 }, dealing = true)) }
        play(Sfx.TAP)
        roundJob = viewModelScope.launch {
            repeat(SHUFFLE_SOUNDS) {
                play(Sfx.FLIP)
                delay(SHUFFLE_STEP_MS)
            }
            _ui.update { app -> app.copy(game = app.game?.copy(dealing = false, pileCounts = game.players.map { p -> p.cardCount })) }
            play(Sfx.PLACE)
            scheduleAutoFlip()
        }
    }

/** Human seats get their chosen dogs (or the favourite); the others get different random breeds. */
    private fun dogsFor(players: Int, chosen: List<Breed>): List<Breed> {
        val first = chosen.distinct().take(players).ifEmpty { listOf(_ui.value.settings.favoriteDog) }
        return first + (Breed.entries - first.toSet()).shuffled().take(players - first.size)
    }

    fun replay() {
        val g = _ui.value.game ?: return startGame(Mode.VS_CPU)
        // same players, same dogs
        startGame(g.session.mode, g.game.players.size, g.session.dogs.take(g.session.humans.size))
    }

    fun goHome() {
        cancelJobs()
        handle.remove<String>(KEY_SESSION)
        handle[KEY_SCREEN] = Screen.HOME.name
        _ui.update { it.copy(screen = Screen.HOME, game = null) }
    }

    fun updateSettings(transform: (Settings) -> Settings) {
        val next = transform(_ui.value.settings)
        settingsRepo.save(next)
        sounds?.enabled = next.soundOn
        sounds?.theme = next.soundTheme
        if (next.autoPlay) continueSignal?.complete(Unit) // auto-play also skips the wait
        _ui.update { it.copy(settings = next) }
        scheduleAutoFlip()
    }

    /** Parent picked a sound style: select it and play its bark so they can hear it (even if muted). */
    fun chooseSoundTheme(theme: SoundTheme) {
        updateSettings { it.copy(soundTheme = theme) }
        sounds?.play(Sfx.BARK, force = true)
    }

    // ---------- Playing ----------

    /** A human seat tapped its flip zone. The round starts once every active human is ready. */
    fun onFlipTap(seat: Int) {
        val g = _ui.value.game ?: return
        if (g.animating || g.dealing || g.game.isFinished || !g.isHuman(seat) || seat in g.ready) return
        if (g.game.players[seat].isEliminated) return
        play(Sfx.TAP)
        val ready = g.ready + seat
        _ui.update { it.copy(game = g.copy(ready = ready)) }
        if (activeHumans(g).all { it in ready }) startRound()
    }

    /** Tap anywhere after a round: collect the cards and get ready for the next round. */
    fun onContinueTap() {
        val signal = continueSignal ?: return
        if (_ui.value.game?.awaitingContinue != true) return
        play(Sfx.TAP)
        signal.complete(Unit)
    }

    private fun activeHumans(g: GameUi) = g.session.humans.filter { !g.game.players[it].isEliminated }

    /** Plays automatically when nobody needs to tap: demo mode, auto-play, or only CPUs left. */
    private fun scheduleAutoFlip() {
        autoJob?.cancel()
        val app = _ui.value
        val g = app.game ?: return
        if (app.screen != Screen.GAME || g.animating || g.dealing || g.game.isFinished) return
        val auto = app.settings.autoPlay || activeHumans(g).isEmpty()
        if (!auto) return
        autoJob = viewModelScope.launch {
            delay(AUTO_DELAY_MS)
            startRound()
        }
    }

    private fun startRound() {
        val g = _ui.value.game ?: return
        if (g.animating || g.dealing || g.game.isFinished) return
        autoJob?.cancel()
        val result = GameEngine.playRound(g.game)
        val nextSession = g.session.copy(game = result.state)
        persist(nextSession, Screen.GAME) // saved before animating, so process death never loses a round

        roundJob = viewModelScope.launch {
            val counts = g.pileCounts.toMutableList()
            fun set(transform: GameUi.() -> GameUi) = _ui.update { app -> app.copy(game = app.game?.transform()) }

            // Children keep the result on screen until they tap; demos and CPU-only games go on alone.
            val waitForTap = !_ui.value.settings.autoPlay && activeHumans(g).isNotEmpty()
            suspend fun holdTable() {
                if (waitForTap) {
                    val signal = CompletableDeferred<Unit>()
                    continueSignal = signal
                    set { copy(awaitingContinue = true) }
                    signal.await()
                    continueSignal = null
                    set { copy(awaitingContinue = false) }
                } else {
                    delay(AUTO_CONTINUE_MS)
                }
            }

            set { copy(animating = true, ready = emptySet()) }
            for (event in result.events) {
                when (event) {
                    is RoundEvent.Reveal -> {
                        event.cards.keys.forEach { counts[it]-- }
                        set { copy(pileCounts = counts.toList(), table = table + event.cards.map { (id, c) -> TableCard(c, id, faceUp = false) }) }
                        delay(380)
                        play(Sfx.FLIP)
                        val ids = event.cards.values.map { it.id }.toSet()
                        set { copy(table = table.map { if (it.card.id in ids) it.copy(faceUp = true) else it }) }
                        delay(900)
                    }
                    is RoundEvent.WoofBattle -> {
                        play(Sfx.BARK)
                        set { copy(woof = true, shakeTick = shakeTick + 1, battlers = event.players.toSet()) }
                        delay(1600)
                        set { copy(woof = false) }
                        delay(200)
                    }
                    is RoundEvent.FaceDown -> {
                        event.cards.keys.forEach { counts[it]-- }
                        play(Sfx.PLACE)
                        set { copy(pileCounts = counts.toList(), table = table + event.cards.map { (id, c) -> TableCard(c, id, faceUp = false) }) }
                        delay(650)
                    }
                    is RoundEvent.Forfeit -> {
                        play(Sfx.SAD)
                        delay(600)
                    }
                    is RoundEvent.Collect -> {
                        play(Sfx.WIN)
                        set { copy(trickWinner = event.winner, battlers = emptySet()) }
                        delay(600)
                        holdTable()
                        set { copy(collectTo = event.winner) }
                        delay(750)
                        set { copy(table = emptyList(), collectTo = null, pileCounts = result.state.players.map { it.cardCount }) }
                    }
                    is RoundEvent.Returned -> {
                        set { copy(battlers = emptySet()) }
                        holdTable()
                        set { copy(returning = true) }
                        delay(750)
                        set { copy(table = emptyList(), returning = false, pileCounts = result.state.players.map { it.cardCount }) }
                    }
                    is RoundEvent.Eliminated -> {
                        play(Sfx.SAD)
                        delay(500)
                    }
                    is RoundEvent.GameOver -> play(Sfx.VICTORY)
                }
            }
            set {
                copy(
                    session = nextSession, pileCounts = result.state.players.map { it.cardCount },
                    table = emptyList(), animating = false, trickWinner = null, battlers = emptySet(),
                    collectTo = null, returning = false, awaitingContinue = false,
                )
            }
            scheduleAutoFlip()
        }
    }

    // ---------- Persistence ----------

    private fun persist(session: Session, screen: Screen) {
        handle[KEY_SESSION] = json.encodeToString(Session.serializer(), session)
        handle[KEY_SCREEN] = screen.name
    }

    private fun restore(): AppUi {
        val settings = settingsRepo.load()
        val session = handle.get<String>(KEY_SESSION)?.let {
            runCatching { json.decodeFromString(Session.serializer(), it) }.getOrNull()
        }
        val pending = handle.get<String>(KEY_PENDING)?.let {
            runCatching { json.decodeFromString(PendingGame.serializer(), it) }.getOrNull()
        }
        val saved = handle.get<String>(KEY_SCREEN)?.let { runCatching { Screen.valueOf(it) }.getOrNull() }
        val screen = when {
            saved == Screen.GAME && session != null -> Screen.GAME
            saved == Screen.DOGS && pending != null -> Screen.DOGS
            else -> Screen.HOME // fresh launch opens on the menu
        }
        return AppUi(
            screen = screen,
            settings = settings,
            game = session?.let { GameUi(it, it.game.players.map { p -> p.cardCount }) },
            pending = pending.takeIf { screen == Screen.DOGS },
        )
    }

    private fun play(sfx: Sfx) = sounds?.play(sfx)

    private fun cancelJobs() {
        continueSignal = null
        roundJob?.cancel()
        autoJob?.cancel()
    }

    override fun onCleared() {
        sounds?.release()
    }

    private companion object {
        const val KEY_SESSION = "session"
        const val KEY_SCREEN = "screen"
        const val KEY_PENDING = "pending"
        const val AUTO_DELAY_MS = 1100L
        const val AUTO_CONTINUE_MS = 900L
        const val SHUFFLE_SOUNDS = 5
        const val SHUFFLE_STEP_MS = 360L
        val json = Json { ignoreUnknownKeys = true }
    }
}
