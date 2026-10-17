package com.woofbattle.app

import android.content.Context
import com.woofbattle.app.ui.Breed
import com.woofbattle.engine.DEFAULT_SHORT_GAME_ROUNDS
import com.woofbattle.engine.GameConfig
import java.util.Locale

enum class Lang(val nativeName: String) {
    EN("English"),
    FR("Français"),
    ES("Español"),
    NL("Nederlands"),
}

/** Sound styles a parent can pick. All are synthesized, see [Synth]. */
enum class SoundTheme { PUPPY, BIG_DOG, SILLY }

data class Settings(
    val playerCount: Int = 2,
    val shortGame: Boolean = false,
    val shortRounds: Int = DEFAULT_SHORT_GAME_ROUNDS,
    val soundOn: Boolean = true,
    val autoPlay: Boolean = false,
    val soundTheme: SoundTheme = SoundTheme.BIG_DOG,
    /** The child's own dog, picked on the home screen. */
    val favoriteDog: Breed = Breed.GOLDEN,
    val lang: Lang = Lang.EN,
) {
    fun gameConfig() = GameConfig(
        playerCount = playerCount,
        roundLimit = if (shortGame) shortRounds else null,
    )

    companion object {
        val SHORT_ROUND_OPTIONS = listOf(10, 20, 30)
    }
}

/** Settings live only on the device (SharedPreferences, excluded from backup). */
class SettingsRepository(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    fun load(): Settings = Settings(
        playerCount = prefs.getInt("players", 2).coerceIn(2, 4),
        shortGame = prefs.getBoolean("short", false),
        shortRounds = prefs.getInt("shortRounds", DEFAULT_SHORT_GAME_ROUNDS),
        soundOn = prefs.getBoolean("sound", true),
        autoPlay = prefs.getBoolean("auto", false),
        favoriteDog = prefs.getString("dog", null)?.let { runCatching { Breed.valueOf(it) }.getOrNull() } ?: Breed.GOLDEN,
        soundTheme = prefs.getString("soundTheme", null)?.let { runCatching { SoundTheme.valueOf(it) }.getOrNull() } ?: SoundTheme.BIG_DOG,
        lang = prefs.getString("lang", null)?.let { runCatching { Lang.valueOf(it) }.getOrNull() } ?: systemLang(),
    )

    fun save(s: Settings) {
        prefs.edit()
            .putInt("players", s.playerCount)
            .putBoolean("short", s.shortGame)
            .putInt("shortRounds", s.shortRounds)
            .putBoolean("sound", s.soundOn)
            .putBoolean("auto", s.autoPlay)
            .putString("soundTheme", s.soundTheme.name)
            .putString("dog", s.favoriteDog.name)
            .putString("lang", s.lang.name)
            .apply()
    }

    private fun systemLang(): Lang = when (Locale.getDefault().language) {
        "fr" -> Lang.FR
        "es" -> Lang.ES
        "nl" -> Lang.NL
        else -> Lang.EN
    }
}

/** The handful of labels in the app (parent area mostly). Children never need to read them. */
data class Strings(
    val woof: String,
    val players: String,
    val gameLength: String,
    val classic: String,
    val short: String,
    val rounds: String,
    val sound: String,
    val soundStyle: String,
    val autoPlay: String,
    val language: String,
    val parentArea: String,
    val holdHint: String,
    val quitGame: String,
    val cards: String,
    val chooseDog: String,
    val breedNames: List<String>,
    val privacyTitle: String,
    val privacyText: String,
) {
    fun breedName(b: Breed) = breedNames[b.ordinal]

    companion object {
        fun of(lang: Lang): Strings = when (lang) {
            Lang.EN -> Strings("WOOF!", "Players in demo", "Game length", "Until one dog has all cards", "Short game", "rounds", "Sound", "Sound style", "Auto-play (demo)", "Language", "Parents", "Hold 3 seconds", "Back to start", "cards", "Choose your dog",
                listOf("Golden", "Dalmatian", "Beagle", "Pug", "Corgi", "Poodle", "Husky", "Dachshund"),
                "Privacy",
                "Woof Battle collects no personal data. It has no internet access, no ads, no analytics and no in-app purchases. Settings and the current game are stored only on this device and are deleted when the app is uninstalled.")
            Lang.FR -> Strings("OUAF !", "Joueurs en démo", "Durée de la partie", "Jusqu'à ce qu'un chien ait toutes les cartes", "Partie courte", "tours", "Son", "Style de sons", "Jeu automatique (démo)", "Langue", "Parents", "Appuyer 3 secondes", "Retour à l'accueil", "cartes", "Choisis ton chien",
                listOf("Golden", "Dalmatien", "Beagle", "Carlin", "Corgi", "Caniche", "Husky", "Teckel"),
                "Confidentialité",
                "Woof Battle ne collecte aucune donnée personnelle. Pas d'accès à internet, pas de publicité, pas de statistiques, pas d'achats intégrés. Les réglages et la partie en cours restent uniquement sur cet appareil et sont supprimés quand l'application est désinstallée.")
            Lang.ES -> Strings("¡GUAU!", "Jugadores en demo", "Duración", "Hasta que un perro tenga todas las cartas", "Partida corta", "rondas", "Sonido", "Estilo de sonidos", "Juego automático (demo)", "Idioma", "Padres", "Mantén 3 segundos", "Volver al inicio", "cartas", "Elige tu perro",
                listOf("Golden", "Dálmata", "Beagle", "Carlino", "Corgi", "Caniche", "Husky", "Salchicha"),
                "Privacidad",
                "Woof Battle no recoge ningún dato personal. No tiene acceso a internet, ni anuncios, ni estadísticas, ni compras. Los ajustes y la partida en curso solo se guardan en este dispositivo y se borran al desinstalar la aplicación.")
            Lang.NL -> Strings("WOEF!", "Spelers in demo", "Speelduur", "Tot één hond alle kaarten heeft", "Kort spel", "rondes", "Geluid", "Geluidsstijl", "Automatisch spelen (demo)", "Taal", "Ouders", "3 seconden vasthouden", "Terug naar start", "kaarten", "Kies je hond",
                listOf("Golden", "Dalmatiër", "Beagle", "Mopshond", "Corgi", "Poedel", "Husky", "Teckel"),
                "Privacy",
                "Woof Battle verzamelt geen persoonsgegevens. Geen internettoegang, geen reclame, geen statistieken en geen aankopen in de app. Instellingen en het huidige spel blijven alleen op dit apparaat en worden verwijderd als de app wordt verwijderd.")
        }
    }
}
