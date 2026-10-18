package com.masterdice.game

import androidx.lifecycle.ViewModel
import com.masterdice.data.GameStore
import com.masterdice.platform.SoundFx
import kotlin.random.Random
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class GameViewModel(
    private val store: GameStore?,
    private val random: Random = Random.Default,
) : ViewModel() {

    private val initialState = store?.load() ?: newGame(random.nextLong()).also { store?.save(it) }
    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<GameState> = _state.asStateFlow()

    private val _stats = MutableStateFlow(store?.loadStats() ?: GameStore.GameStats())
    val stats: StateFlow<GameStore.GameStats> = _stats.asStateFlow()

    // Sound preference — persisted; mirrors into SoundFx for the UI layer.
    private val _soundEnabled = MutableStateFlow(
        (store?.loadSettings()?.soundEnabled ?: true).also { SoundFx.enabled = it },
    )
    val soundEnabled: StateFlow<Boolean> = _soundEnabled.asStateFlow()

    fun setSoundEnabled(enabled: Boolean) {
        _soundEnabled.value = enabled
        SoundFx.enabled = enabled
        store?.saveSettings(GameStore.Settings(soundEnabled = enabled))
        SoundFx.tap() // audible confirm when toggled on
    }

    /** True while the end panel of a record-breaking game is on screen. */
    private val _newBest = MutableStateFlow(false)
    val newBest: StateFlow<Boolean> = _newBest.asStateFlow()

    fun dispatch(intent: GameIntent) {
        val previous = _state.value
        val next = reduce(previous, intent)
        if (next !== previous) {
            if (intent is GameIntent.NewGame || intent is GameIntent.NewGameWithDeal) {
                _newBest.value = false
            }
            _state.value = next
            store?.save(next)
            recordScore(previous, next)
        }
    }

    fun newGame() {
        dispatch(GameIntent.NewGame(random.nextLong()))
    }

    /** On the transition to WON/LOST, bank the score into per-level bests —
     *  unless a debug tool touched the game (`scored == false`). */
    private fun recordScore(previous: GameState, next: GameState) {
        if (previous.status == GameStatus.PLAYING && next.status != GameStatus.PLAYING &&
            next.scored
        ) {
            val level = LEVELS[next.levelIndex]
            val points = gameScore(next, level) ?: return
            val best = _stats.value.bestScores[level.index]
            val improved = best == null || points > best
            _stats.value = _stats.value.copy(
                bestScores = if (improved)
                    _stats.value.bestScores + (level.index to points)
                else _stats.value.bestScores,
                totalScore = _stats.value.totalScore + points,
            )
            if (improved) _newBest.value = true
            store?.saveStats(_stats.value)
        }
    }
}
