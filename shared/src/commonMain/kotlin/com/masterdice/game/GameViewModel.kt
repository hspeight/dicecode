package com.masterdice.game

import androidx.lifecycle.ViewModel
import com.masterdice.data.GameStore
import kotlin.random.Random
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class GameViewModel(
    private val store: GameStore?,
    private val random: Random = Random.Default,
) : ViewModel() {

    private val _state = MutableStateFlow(store?.load() ?: newGame(random.nextLong()))
    val state: StateFlow<GameState> = _state.asStateFlow()

    fun dispatch(intent: GameIntent) {
        val next = reduce(_state.value, intent)
        if (next !== _state.value) {
            _state.value = next
            store?.save(next)
        }
    }

    fun newGame() {
        dispatch(GameIntent.NewGame(random.nextLong()))
    }
}
