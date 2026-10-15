package com.masterdice.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runComposeUiTest
import com.masterdice.game.Die
import com.masterdice.game.Feedback
import com.masterdice.game.GameState
import com.masterdice.game.GameStatus
import com.masterdice.game.newGame
import com.masterdice.game.reduce
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class GameScreenTest {

    private fun ComposeUiTest.centerOf(tag: String): Offset =
        onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot.center

    /**
     * Drags gradually (10 interpolated steps with event and main-clock time
     * advancing between them) so recompositions happen mid-gesture — the same
     * conditions that break a drag hosted on the die itself.
     */
    private fun ComposeUiTest.drag(
        fromTag: String,
        toTag: String,
        assertOverlayMidDrag: Boolean = false,
    ) {
        val from = centerOf(fromTag)
        val to = centerOf(toTag)
        onRoot().performTouchInput { down(from) }
        val steps = 10
        for (i in 1..steps) {
            val t = i.toFloat() / steps
            val p = Offset(
                from.x + (to.x - from.x) * t,
                from.y + (to.y - from.y) * t,
            )
            onRoot().performTouchInput {
                moveTo(p)
                advanceEventTime(16)
            }
            mainClock.advanceTimeBy(16)
            waitForIdle()
            if (assertOverlayMidDrag && i == steps / 2) {
                onNodeWithTag("dragOverlay").fetchSemanticsNode()
            }
        }
        onRoot().performTouchInput { up() }
        waitForIdle()
    }

    /** Drags `dice` into row [row] (tray slot resolved against [state]). */
    private fun ComposeUiTest.playDice(state: () -> GameState, row: Int, dice: List<Die>) {
        dice.forEachIndexed { col, die ->
            val trayIndex = state().tray.indexOf(die)
            assertTrue(trayIndex >= 0, "die $die not in tray")
            drag("tray-$trayIndex", "cell-$row-$col", assertOverlayMidDrag = col == 0)
        }
    }

    @Test
    fun fullGamePlayedThroughUi() = runComposeUiTest {
        var state by mutableStateOf(newGame(7L))
        setContent {
            GameScreen(
                state = state,
                onIntent = { intent -> state = reduce(state, intent) },
                onNewGame = {},
            )
        }
        waitForIdle()

        // Enter is a no-op while the row is empty.
        onNodeWithTag("enter").performClick()
        waitForIdle()
        assertEquals(0, state.activeRow)

        // Submit a deliberately wrong guess: decoy in column 0 can't be green.
        val decoy = state.deal.first { it !in state.solution }
        val others = state.deal.filter { it != decoy }
        playDice({ state }, row = 0, dice = listOf(decoy, others[0], others[1]))

        onNodeWithTag("enter").performClick()
        waitForIdle()

        assertEquals(1, state.activeRow)
        assertEquals(state.deal, state.tray)
        assertTrue(state.guesses[0].feedback.any { it != Feedback.GREEN })

        // Now drag the solution dice into place and win.
        playDice({ state }, row = 1, dice = state.solution)
        onNodeWithTag("enter").performClick()
        waitForIdle()

        assertEquals(GameStatus.WON, state.status)
        onNodeWithTag("endPanel").fetchSemanticsNode()
        onNodeWithTag("newGame").fetchSemanticsNode()
    }

    @Test
    fun illegalDropSnapsBack() = runComposeUiTest {
        var state by mutableStateOf(newGame(7L))
        setContent {
            GameScreen(
                state = state,
                onIntent = { intent -> state = reduce(state, intent) },
                onNewGame = {},
            )
        }
        waitForIdle()

        // Dropping a tray die onto a non-active row is illegal; the overlay
        // is asserted mid-drag inside drag(), then the die returns.
        drag("tray-0", "cell-5-0", assertOverlayMidDrag = true)
        mainClock.advanceTimeBy(500) // let the return animation finish
        waitForIdle()
        assertFailsWith<AssertionError> {
            onNodeWithTag("dragOverlay").fetchSemanticsNode()
        }

        assertEquals(newGame(7L), state)
        assertEquals(state.deal, state.tray)
    }

    @Test
    fun swapDiceWithinActiveRow() = runComposeUiTest {
        var state by mutableStateOf(newGame(7L))
        setContent {
            GameScreen(
                state = state,
                onIntent = { intent -> state = reduce(state, intent) },
                onNewGame = {},
            )
        }
        waitForIdle()

        drag("tray-0", "cell-0-0")
        drag("tray-1", "cell-0-2")
        val a = state.activeCells[0]!!
        val b = state.activeCells[2]!!

        drag("cell-0-0", "cell-0-2")
        assertEquals(b, state.activeCells[0])
        assertEquals(a, state.activeCells[2])

        // Move a cell die back to an empty tray slot.
        drag("cell-0-0", "tray-0")
        assertEquals(b, state.tray[0])
        assertEquals(null, state.activeCells[0])
    }

    @Test
    fun tapMovesDiceBetweenTrayAndRow() = runComposeUiTest {
        var state by mutableStateOf(newGame(7L))
        setContent {
            GameScreen(
                state = state,
                onIntent = { intent -> state = reduce(state, intent) },
                onNewGame = {},
            )
        }
        waitForIdle()

        // Taps fill the active row left to right.
        val dieA = state.tray[0]!!
        val dieB = state.tray[2]!!
        onNodeWithTag("tray-0").performClick()
        onNodeWithTag("tray-2").performClick()
        waitForIdle()
        assertEquals(dieA, state.activeCells[0])
        assertEquals(dieB, state.activeCells[1])

        // Tapping an active-row die sends it back to its own tray slot.
        onNodeWithTag("cell-0-0").performClick()
        waitForIdle()
        assertEquals(null, state.activeCells[0])
        assertEquals(dieA, state.tray[0])
    }

    @Test
    fun emptyActiveCellTapShowsHint() = runComposeUiTest {
        var state by mutableStateOf(newGame(7L))
        setContent {
            GameScreen(
                state = state,
                onIntent = { intent -> state = reduce(state, intent) },
                onNewGame = {},
            )
        }
        waitForIdle()

        // Freeze the clock so the hint's auto-dismiss timer can't fire
        // before the assertions (waitForIdle would advance past it).
        mainClock.autoAdvance = false
        onNodeWithTag("cell-0-1").performClick()
        mainClock.advanceTimeByFrame()
        onNodeWithTag("cellHint").fetchSemanticsNode()
        onNodeWithTag("trayPointer").fetchSemanticsNode()
        mainClock.autoAdvance = true
    }

    @Test
    fun repeatedGuessShowsWarning() = runComposeUiTest {
        var state by mutableStateOf(newGame(7L))
        setContent {
            GameScreen(
                state = state,
                onIntent = { intent -> state = reduce(state, intent) },
                onNewGame = {},
            )
        }
        waitForIdle()

        // First fill is not a duplicate — no warning before submitting.
        val dice = listOf(state.tray[0]!!, state.tray[1]!!, state.tray[2]!!)
        playDice({ state }, row = 0, dice = dice)
        assertFailsWith<AssertionError> {
            onNodeWithTag("repeatWarning").fetchSemanticsNode()
        }
        onNodeWithTag("enter").performClick()
        waitForIdle()

        // Replaying the same combination warns once the row is full.
        playDice({ state }, row = 1, dice = dice)
        onNodeWithTag("repeatWarning").fetchSemanticsNode()

        // Dismissing lets the duplicate submit anyway.
        onNodeWithTag("repeatWarningDismiss").performClick()
        waitForIdle()
        assertFailsWith<AssertionError> {
            onNodeWithTag("repeatWarning").fetchSemanticsNode()
        }
        onNodeWithTag("enter").performClick()
        waitForIdle()
        assertEquals(2, state.activeRow)
    }
}
