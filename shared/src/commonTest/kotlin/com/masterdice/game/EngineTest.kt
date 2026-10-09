package com.masterdice.game

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class EngineTest {

    // ---------- setup ----------

    @Test
    fun newGameInvariantsHoldAcrossManySeeds() {
        for (seed in 0L until 200L) {
            val state = newGame(seed)

            assertEquals(GUESS_SIZE, state.solution.size, "seed $seed")
            assertEquals(TRAY_SIZE, state.deal.size, "seed $seed")
            assertEquals(state.deal, state.tray, "seed $seed")
            assertEquals(List(GUESS_SIZE) { null }, state.activeCells, "seed $seed")
            assertTrue(state.guesses.isEmpty(), "seed $seed")
            assertEquals(GameStatus.PLAYING, state.status, "seed $seed")
            assertEquals(0, state.activeRow, "seed $seed")

            // No two dice in the deal are identical.
            assertEquals(TRAY_SIZE, state.deal.toSet().size, "seed $seed")

            // Deal contains exactly the three solution dice plus one decoy
            // that is not identical to any solution die.
            state.solution.forEach { assertTrue(it in state.deal, "seed $seed") }
            val decoys = state.deal.filter { it !in state.solution }
            assertEquals(1, decoys.size, "seed $seed")

            state.deal.forEach { assertTrue(it.value in 1..6, "seed $seed") }
        }
    }

    @Test
    fun sameSeedProducesSameGame() {
        assertEquals(newGame(42L), newGame(42L))
        assertNotEquals(newGame(1L), newGame(2L))
    }

    // ---------- legal moves ----------

    @Test
    fun trayToEmptyActiveCellMovesDie() {
        val state = newGame(7L)
        val die = state.tray[1]!!
        val next = reduce(state, GameIntent.Move(Slot.Tray(1), Slot.Cell(0, 2)))

        assertNull(next.tray[1])
        assertEquals(die, next.activeCells[2])
    }

    @Test
    fun activeCellToEmptyTrayMovesDieBack() {
        var state = newGame(7L)
        state = reduce(state, GameIntent.Move(Slot.Tray(0), Slot.Cell(0, 1)))
        val die = state.activeCells[1]!!

        // Slot.Tray(0) is the slot emptied by the first move.
        val next = reduce(state, GameIntent.Move(Slot.Cell(0, 1), Slot.Tray(0)))
        assertNull(next.activeCells[1])
        assertEquals(die, next.tray[0])
    }

    @Test
    fun cellToEmptyCellIsPlainMove() {
        var state = newGame(7L)
        state = reduce(state, GameIntent.Move(Slot.Tray(0), Slot.Cell(0, 0)))
        val die = state.activeCells[0]!!

        val next = reduce(state, GameIntent.Move(Slot.Cell(0, 0), Slot.Cell(0, 2)))
        assertNull(next.activeCells[0])
        assertEquals(die, next.activeCells[2])
    }

    @Test
    fun cellToOccupiedCellSwaps() {
        var state = newGame(7L)
        state = reduce(state, GameIntent.Move(Slot.Tray(0), Slot.Cell(0, 0)))
        state = reduce(state, GameIntent.Move(Slot.Tray(1), Slot.Cell(0, 2)))
        val a = state.activeCells[0]!!
        val b = state.activeCells[2]!!

        val next = reduce(state, GameIntent.Move(Slot.Cell(0, 0), Slot.Cell(0, 2)))
        assertEquals(b, next.activeCells[0])
        assertEquals(a, next.activeCells[2])
    }

    // ---------- illegal moves: every one returns the same instance ----------

    private fun filledState(): GameState {
        var state = newGame(7L)
        state = reduce(state, GameIntent.Move(Slot.Tray(0), Slot.Cell(0, 0)))
        state = reduce(state, GameIntent.Move(Slot.Tray(1), Slot.Cell(0, 1)))
        state = reduce(state, GameIntent.Move(Slot.Tray(2), Slot.Cell(0, 2)))
        return state
    }

    @Test
    fun illegalMovesReturnSameInstance() {
        val empty = newGame(7L)
        val filled = filledState()

        // tray -> tray
        assertSame(empty, reduce(empty, GameIntent.Move(Slot.Tray(0), Slot.Tray(1))))
        // tray -> occupied active cell
        assertSame(filled, reduce(filled, GameIntent.Move(Slot.Tray(3), Slot.Cell(0, 0))))
        // active cell -> occupied tray slot (tray still has deal[3])
        assertSame(filled, reduce(filled, GameIntent.Move(Slot.Cell(0, 0), Slot.Tray(3))))
        // from an empty slot
        assertSame(empty, reduce(empty, GameIntent.Move(Slot.Cell(0, 0), Slot.Tray(0))))
        // from == to
        assertSame(empty, reduce(empty, GameIntent.Move(Slot.Tray(0), Slot.Tray(0))))
        // non-active row as source and as target
        assertSame(empty, reduce(empty, GameIntent.Move(Slot.Cell(5, 0), Slot.Tray(0))))
        assertSame(empty, reduce(empty, GameIntent.Move(Slot.Tray(0), Slot.Cell(5, 0))))
        // out-of-range indices
        assertSame(empty, reduce(empty, GameIntent.Move(Slot.Tray(9), Slot.Cell(0, 0))))
        assertSame(empty, reduce(empty, GameIntent.Move(Slot.Tray(0), Slot.Cell(0, 5))))
        assertSame(empty, reduce(empty, GameIntent.Move(Slot.Cell(0, 0), Slot.Cell(-1, 0))))
    }

    @Test
    fun movesFromSubmittedRowAreIllegal() {
        var state = wonInOneGuess()
        // submitted row 0 as source
        assertSame(state, reduce(state, GameIntent.Move(Slot.Cell(0, 0), Slot.Tray(0))))
        assertSame(state, reduce(state, GameIntent.Move(Slot.Tray(0), Slot.Cell(0, 0))))
    }

    @Test
    fun movesAfterWonAndLostAreNoOps() {
        val won = wonInOneGuess()
        assertSame(won, reduce(won, GameIntent.Move(Slot.Tray(0), Slot.Cell(1, 0))))
        assertSame(won, reduce(won, GameIntent.Submit))

        val lost = playLoss()
        assertSame(lost, reduce(lost, GameIntent.Move(Slot.Tray(0), Slot.Cell(0, 0))))
        assertSame(lost, reduce(lost, GameIntent.Submit))
    }

    // ---------- submit ----------

    @Test
    fun canSubmitOnlyWhenRowIsFull() {
        var state = newGame(7L)
        assertFalse(state.canSubmit)
        assertSame(state, reduce(state, GameIntent.Submit))

        state = reduce(state, GameIntent.Move(Slot.Tray(0), Slot.Cell(0, 0)))
        state = reduce(state, GameIntent.Move(Slot.Tray(1), Slot.Cell(0, 1)))
        assertFalse(state.canSubmit)

        state = reduce(state, GameIntent.Move(Slot.Tray(2), Slot.Cell(0, 2)))
        assertTrue(state.canSubmit)
    }

    @Test
    fun nonFinalSubmitResetsTrayAndAdvancesRow() {
        var state = losingGuess()
        state = reduce(state, GameIntent.Submit)

        assertEquals(1, state.guesses.size)
        assertEquals(1, state.activeRow)
        assertEquals(state.deal, state.tray)
        assertEquals(List(GUESS_SIZE) { null }, state.activeCells)
        assertEquals(GameStatus.PLAYING, state.status)
    }

    @Test
    fun threeGreensWins() {
        val state = wonInOneGuess()
        assertEquals(GameStatus.WON, state.status)
        assertEquals(1, state.guesses.size)
        assertTrue(state.guesses[0].feedback.all { it == Feedback.GREEN })
    }

    @Test
    fun tenthNonWinningRowLoses() {
        val state = playLoss()
        assertEquals(GameStatus.LOST, state.status)
        assertEquals(ROWS, state.guesses.size)
    }

    // ---------- evaluate ----------

    private fun d(colour: DieColour, value: Int) = Die(colour, value)

    @Test
    fun evaluateWorkedExample() {
        // PRD: solution Red 4, Blue 2, Green 6.
        val solution = listOf(d(DieColour.RED, 4), d(DieColour.BLUE, 2), d(DieColour.GREEN, 6))

        assertEquals(
            listOf(Feedback.NONE, Feedback.NONE, Feedback.GREEN),
            evaluate(
                listOf(d(DieColour.BLUE, 2), d(DieColour.RED, 4), d(DieColour.GREEN, 6)),
                solution,
            ),
        )
        assertEquals(
            listOf(Feedback.GREEN, Feedback.AMBER, Feedback.GREEN),
            evaluate(
                listOf(d(DieColour.RED, 4), d(DieColour.ORANGE, 2), d(DieColour.GREEN, 6)),
                solution,
            ),
        )
    }

    @Test
    fun evaluateAllOutcomes() {
        val solution = listOf(d(DieColour.RED, 4))
        assertEquals(listOf(Feedback.GREEN), evaluate(listOf(d(DieColour.RED, 4)), solution))
        // amber: colour only, value only
        assertEquals(listOf(Feedback.AMBER), evaluate(listOf(d(DieColour.RED, 1)), solution))
        assertEquals(listOf(Feedback.AMBER), evaluate(listOf(d(DieColour.BLUE, 4)), solution))
        assertEquals(listOf(Feedback.NONE), evaluate(listOf(d(DieColour.BLUE, 1)), solution))
    }

    // ---------- helpers ----------

    /** Fills the active row with the solution dice in order, then submits. */
    private fun wonInOneGuess(): GameState {
        var state = newGame(7L)
        for (col in 0 until GUESS_SIZE) {
            val trayIndex = state.tray.indexOf(state.solution[col])
            state = reduce(state, GameIntent.Move(Slot.Tray(trayIndex), Slot.Cell(0, col)))
        }
        return reduce(state, GameIntent.Submit)
    }

    /** Fills the active row with a guess that cannot be all-green. */
    private fun losingGuess(): GameState {
        var state = newGame(7L)
        val decoy = state.deal.first { it !in state.solution }
        val others = state.deal.filter { it != decoy }

        var trayIndex = state.tray.indexOf(decoy)
        state = reduce(state, GameIntent.Move(Slot.Tray(trayIndex), Slot.Cell(state.activeRow, 0)))
        state = reduce(
            state,
            GameIntent.Move(Slot.Tray(state.tray.indexOf(others[0])), Slot.Cell(state.activeRow, 1)),
        )
        state = reduce(
            state,
            GameIntent.Move(Slot.Tray(state.tray.indexOf(others[1])), Slot.Cell(state.activeRow, 2)),
        )
        return state
    }

    private fun playLoss(): GameState {
        var state = newGame(7L)
        repeat(ROWS) {
            // Rebuild the same non-winning guess each row: decoy in column 0
            // can never be GREEN, so the game can never be won this way.
            val decoy = state.deal.first { it !in state.solution }
            val others = state.deal.filter { it != decoy }
            val row = state.activeRow
            state = reduce(state, GameIntent.Move(Slot.Tray(state.tray.indexOf(decoy)), Slot.Cell(row, 0)))
            state = reduce(state, GameIntent.Move(Slot.Tray(state.tray.indexOf(others[0])), Slot.Cell(row, 1)))
            state = reduce(state, GameIntent.Move(Slot.Tray(state.tray.indexOf(others[1])), Slot.Cell(row, 2)))
            state = reduce(state, GameIntent.Submit)
        }
        return state
    }
}
