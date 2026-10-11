package com.masterdice.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.graphics.toAwtImage
import com.masterdice.game.Die
import com.masterdice.game.Feedback
import com.masterdice.game.GameIntent
import com.masterdice.game.GameState
import com.masterdice.game.ROWS
import com.masterdice.game.Slot
import com.masterdice.game.evaluate
import com.masterdice.game.newGame
import com.masterdice.game.reduce
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test

/** Renders representative states to PNGs under <repo>/tmp for visual review. */
@OptIn(ExperimentalTestApi::class)
class ScreenshotTest {

    private fun tmpDir(): File {
        val workDir = File(System.getProperty("user.dir"))
        val root = if (workDir.name == "shared") workDir.parentFile else workDir
        return File(root, "tmp").apply { mkdirs() }
    }

    private fun move(state: GameState, from: Slot, to: Slot): GameState =
        reduce(state, GameIntent.Move(from, to))

    private fun place(state: GameState, row: Int, dice: List<Die>): GameState {
        var s = state
        dice.forEachIndexed { col, die ->
            s = move(s, Slot.Tray(s.tray.indexOf(die)), Slot.Cell(row, col))
        }
        return s
    }

    /** A guess that can't win: a non-solution die in column 0. */
    private fun losingDice(state: GameState): List<Die> {
        val decoy = state.deal.first { it !in state.solution }
        val others = state.deal.filter { it != decoy }
        return listOf(decoy, others[0], others[1])
    }

    /** Finds a seed whose deal admits a guess with all three feedback types. */
    private fun mixedGuess(): Pair<Long, List<Die>> {
        for (seed in 0L..2000L) {
            val state = newGame(seed)
            for (a in state.deal) for (b in state.deal) for (c in state.deal) {
                if (a == b || a == c || b == c) continue
                val dice = listOf(a, b, c)
                val fb = evaluate(dice, state.solution).toSet()
                if (fb == setOf(Feedback.GREEN, Feedback.AMBER, Feedback.NONE)) {
                    return seed to dice
                }
            }
        }
        error("no seed with mixed feedback found")
    }

    private fun midGameState(): GameState {
        val (seed, guess) = mixedGuess()
        var state = place(newGame(seed), 0, guess)
        state = reduce(state, GameIntent.Submit)
        // Row 1 in progress: two dice placed.
        state = place(state, 1, listOf(state.solution[0], losingDice(state)[1]))
        return state
    }

    private fun wonState(): GameState {
        val (seed, guess) = mixedGuess()
        var state = place(newGame(seed), 0, guess)
        state = reduce(state, GameIntent.Submit)
        // Row 1: the solution dice in order, straight from the refilled tray.
        state = place(state, 1, state.solution)
        return reduce(state, GameIntent.Submit)
    }

    private fun lostState(): GameState {
        var state = newGame(7L)
        repeat(ROWS) {
            state = place(state, state.activeRow, losingDice(state))
            state = reduce(state, GameIntent.Submit)
        }
        return state
    }

    private suspend fun androidx.compose.ui.test.ComposeUiTest.capture(
        state: GameState,
        name: String,
    ) {
        var s by mutableStateOf(state)
        setContent {
            GameScreen(state = s, onIntent = { s = reduce(s, it) }, onNewGame = {})
        }
        waitForIdle()
        // Let the newest-row feedback animation settle.
        mainClock.advanceTimeBy(2000)
        waitForIdle()
        ImageIO.write(onRoot().captureToImage().toAwtImage(), "png", File(tmpDir(), name))
    }

    @Test
    fun captureMidGame() = runComposeUiTest {
        capture(midGameState(), "midgame.png")
    }

    @Test
    fun captureWon() = runComposeUiTest {
        capture(wonState(), "won.png")
    }

    @Test
    fun captureLost() = runComposeUiTest {
        capture(lostState(), "lost.png")
    }

    @Test
    fun captureDealPicker() = runComposeUiTest {
        var s by mutableStateOf(midGameState())
        setContent {
            GameScreen(state = s, onIntent = { s = reduce(s, it) }, onNewGame = {})
        }
        onNodeWithTag("debugDeal").performClick()
        waitForIdle()
        onNodeWithTag("debugDealPanel").fetchSemanticsNode()
        ImageIO.write(onRoot().captureToImage().toAwtImage(), "png", File(tmpDir(), "dealpicker.png"))
    }

    @Test
    fun captureHelp() = runComposeUiTest {
        var s by mutableStateOf(midGameState())
        setContent {
            GameScreen(state = s, onIntent = { s = reduce(s, it) }, onNewGame = {})
        }
        onNodeWithTag("help").performClick()
        waitForIdle()
        onNodeWithTag("helpPanel").fetchSemanticsNode()
        ImageIO.write(onRoot().captureToImage().toAwtImage(), "png", File(tmpDir(), "help.png"))
    }

    @Test
    fun captureSettings() = runComposeUiTest {
        var s by mutableStateOf(midGameState())
        setContent {
            GameScreen(state = s, onIntent = { s = reduce(s, it) }, onNewGame = {})
        }
        onNodeWithTag("settings").performClick()
        waitForIdle()
        onNodeWithTag("settingsPanel").fetchSemanticsNode()
        ImageIO.write(onRoot().captureToImage().toAwtImage(), "png", File(tmpDir(), "settings.png"))
    }

    @Test
    fun captureIndicatorsHidden() = runComposeUiTest {
        var s by mutableStateOf(midGameState())
        setContent {
            GameScreen(state = s, onIntent = { s = reduce(s, it) }, onNewGame = {})
        }
        // Indicators default to off; the board shows count-only peg feedback.
        ImageIO.write(
            onRoot().captureToImage().toAwtImage(), "png",
            File(tmpDir(), "indicators-hidden.png"),
        )
    }
}
