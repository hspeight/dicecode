package com.masterdice.data

import com.masterdice.game.newGame
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem

class GameStoreTest {

    @Test
    fun roundTripRestoresState() {
        val fs = FakeFileSystem()
        val store = GameStore("/saves", fs)
        val state = newGame(99L)

        store.save(state)
        assertEquals(state, store.load())

        // A second store on the same fs sees the same file.
        assertEquals(state, GameStore("/saves", fs).load())
    }

    @Test
    fun missingFileLoadsNull() {
        val fs = FakeFileSystem()
        assertNull(GameStore("/saves", fs).load())
    }

    @Test
    fun corruptFileLoadsNull() {
        val fs = FakeFileSystem()
        fs.createDirectories("/saves".toPath())
        fs.write("/saves/game.json".toPath()) { writeUtf8("{ not json !!!") }
        assertNull(GameStore("/saves", fs).load())
    }

    @Test
    fun versionMismatchLoadsNullAndDeletes() {
        val fs = FakeFileSystem()
        val store = GameStore("/saves", fs)
        store.save(newGame(1L))

        val path = "/saves/game.json".toPath()
        fs.write(path) { writeUtf8("""{"version":999}""") }

        assertNull(store.load())
        assertNull(store.load())
    }
}
