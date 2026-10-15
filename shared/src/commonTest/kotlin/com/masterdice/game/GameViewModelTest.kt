package com.masterdice.game

import com.masterdice.data.GameStore
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue
import okio.fakefilesystem.FakeFileSystem

class GameViewModelTest {

    @Test
    fun restoresSavedGameFromStore() {
        val fs = FakeFileSystem()
        val store = GameStore("/saves", fs)
        val saved = newGame(123L).let { s ->
            reduce(s, GameIntent.Move(Slot.Tray(0), Slot.Cell(0, 0)))
        }
        store.save(saved)

        val vm = GameViewModel(GameStore("/saves", fs), Random(0))
        assertEquals(saved, vm.state.value)
    }

    @Test
    fun startsNewGameWhenStoreEmpty() {
        val fs = FakeFileSystem()
        val vm = GameViewModel(GameStore("/saves", fs), Random(5))
        assertEquals(GameStatus.PLAYING, vm.state.value.status)
        assertEquals(0, vm.state.value.activeRow)
    }

    @Test
    fun dispatchSavesChangedState() {
        val fs = FakeFileSystem()
        val store = GameStore("/saves", fs)
        val vm = GameViewModel(store, Random(5))

        vm.dispatch(GameIntent.Move(Slot.Tray(0), Slot.Cell(0, 0)))
        assertEquals(vm.state.value, store.load())
    }

    @Test
    fun illegalDispatchLeavesSameInstanceAndDoesNotSave() {
        val fs = FakeFileSystem()
        val vm = GameViewModel(GameStore("/saves", fs), Random(5))
        val before = vm.state.value

        vm.dispatch(GameIntent.Move(Slot.Tray(0), Slot.Tray(1)))
        assertSame(before, vm.state.value)

        // Submit on an unfilled row is also a no-op.
        vm.dispatch(GameIntent.Submit)
        assertSame(before, vm.state.value)
    }

    @Test
    fun newGameProducesFreshState() {
        val vm = GameViewModel(null, Random(5))
        val before = vm.state.value
        vm.newGame()
        assertNotSame(before, vm.state.value)
        assertEquals(GameStatus.PLAYING, vm.state.value.status)
    }

    // ---------- scoring ----------

    /** Wins the current game outright by placing every solution die. */
    private fun GameViewModel.playWin() {
        val state = state.value
        state.solution.forEachIndexed { col, die ->
            dispatch(GameIntent.Move(Slot.Tray(state.deal.indexOf(die)), Slot.Cell(0, col)))
        }
        dispatch(GameIntent.Submit)
    }

    @Test
    fun winningGameRecordsAndPersistsBestScore() {
        val fs = FakeFileSystem()
        val vm = GameViewModel(GameStore("/saves", fs), Random(5))
        vm.playWin()

        assertEquals(GameStatus.WON, vm.state.value.status)
        val expected = scoreWin(LEVELS[0], 1)
        assertEquals(expected, vm.stats.value.bestScores[0])
        assertEquals(expected, vm.stats.value.totalScore)
        assertTrue(vm.newBest.value)

        // Stats persist across store/ViewModel instances.
        val vm2 = GameViewModel(GameStore("/saves", fs), Random(0))
        assertEquals(expected, vm2.stats.value.bestScores[0])
        assertEquals(expected, vm2.stats.value.totalScore)
    }

    /** Loses on purpose: every submitted row contains the decoy. */
    private fun GameViewModel.playLosingGame() {
        val deal = state.value.deal
        val decoy = deal.first { it !in state.value.solution }
        val fillers = state.value.solution.take(2)
        repeat(ROWS) {
            (listOf(decoy) + fillers).forEachIndexed { col, die ->
                dispatch(
                    GameIntent.Move(
                        Slot.Tray(state.value.deal.indexOf(die)),
                        Slot.Cell(state.value.activeRow, col),
                    ),
                )
            }
            dispatch(GameIntent.Submit)
        }
    }

    @Test
    fun weakerScoreDoesNotOverwriteBest() {
        val vm = GameViewModel(null, Random(5))
        vm.playWin() // 150, becomes best
        vm.newGame()
        assertFalse(vm.newBest.value)

        // Losing pays at most 24 — it must not overwrite the 150 best,
        // but the total still grows by the loss credit.
        vm.playLosingGame()
        assertEquals(GameStatus.LOST, vm.state.value.status)
        assertEquals(150, vm.stats.value.bestScores[0])
        assertEquals(
            150 + scoreLoss(LEVELS[0], vm.state.value.guesses),
            vm.stats.value.totalScore,
        )
        assertFalse(vm.newBest.value)
    }

    @Test
    fun unscoredGameDoesNotRecord() {
        val vm = GameViewModel(null, Random(5))
        vm.dispatch(
            GameIntent.NewGameWithDeal(
                listOf(
                    Die(DieColour.RED, 1), Die(DieColour.BLUE, 2), Die(DieColour.GREEN, 3),
                ),
                Die(DieColour.ORANGE, 4),
            ),
        )
        vm.playWin()

        assertEquals(GameStatus.WON, vm.state.value.status)
        assertTrue(vm.stats.value.bestScores.isEmpty())
        assertEquals(0, vm.stats.value.totalScore)
        assertFalse(vm.newBest.value)
    }
}
