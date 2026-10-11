package com.masterdice.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.masterdice.data.GameStore
import com.masterdice.game.GameViewModel
import com.masterdice.platform.defaultStorageDir

@Composable
fun App() {
    val viewModel: GameViewModel = viewModel {
        GameViewModel(GameStore(defaultStorageDir()))
    }
    val state by viewModel.state.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val newBest by viewModel.newBest.collectAsState()
    GameScreen(
        state = state,
        onIntent = viewModel::dispatch,
        onNewGame = viewModel::newGame,
        bestScore = stats.bestScores[state.levelIndex],
        newBest = newBest,
    )
}
