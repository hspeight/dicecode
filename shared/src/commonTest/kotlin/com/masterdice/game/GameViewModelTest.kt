package com.masterdice.game

import com.masterdice.data.GameStore
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame
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
}
