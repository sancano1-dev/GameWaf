package com.woofbattle.app

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.woofbattle.app.ui.DogPicker
import com.woofbattle.app.ui.GameScreen
import com.woofbattle.app.ui.HomeScreen
import com.woofbattle.app.ui.ParentArea
import com.woofbattle.app.ui.WoofTheme

class MainActivity : ComponentActivity() {

    private val viewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        hideSystemBars()

        setContent {
            WoofTheme {
                val app by viewModel.ui.collectAsStateWithLifecycle()
                val strings = Strings.of(app.settings.lang)
                var parentOpen by rememberSaveable { mutableStateOf(false) }
                val toggleSound = { viewModel.updateSettings { it.copy(soundOn = !it.soundOn) } }

                Box(Modifier.fillMaxSize()) {
                    val game = app.game
                    if (app.screen == Screen.DOGS) {
                        DogPicker(
                            selected = app.settings.favoriteDog,
                            strings = strings,
                            onSelect = viewModel::chooseFavoriteDog,
                            onDone = viewModel::closeDogPicker,
                        )
                    } else if (app.screen == Screen.GAME && game != null) {
                        GameScreen(
                            ui = game,
                            settings = app.settings,
                            strings = strings,
                            onFlip = viewModel::onFlipTap,
                            onToggleSound = toggleSound,
                            onOpenParent = { parentOpen = true },
                            onReplay = viewModel::replay,
                            onHome = viewModel::goHome,
                        )
                    } else {
                        HomeScreen(
                            settings = app.settings,
                            strings = strings,
                            onStart = viewModel::startGame,
                            onToggleSound = toggleSound,
                            onOpenParent = { parentOpen = true },
                            onOpenDogPicker = viewModel::openDogPicker,
                        )
                    }
                    if (parentOpen) {
                        ParentArea(
                            settings = app.settings,
                            strings = strings,
                            inGame = app.screen == Screen.GAME,
                            onChange = viewModel::updateSettings,
                            onChooseSoundTheme = viewModel::chooseSoundTheme,
                            onQuitGame = {
                                parentOpen = false
                                viewModel.goHome()
                            },
                            onClose = { parentOpen = false },
                        )
                    }
                }
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    /** Immersive mode so little fingers don't hit the system bars. */
    private fun hideSystemBars() {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
    }
}
