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

    @Test
    fun statsRoundTrip() {
        val fs = FakeFileSystem()
        val store = GameStore("/saves", fs)

        assertEquals(GameStore.GameStats(), store.loadStats())

        store.saveStats(GameStore.GameStats(mapOf(0 to 125, 4 to 526)))
        assertEquals(mapOf(0 to 125, 4 to 526), store.loadStats().bestScores)

        // A second store on the same fs sees the same file.
        assertEquals(mapOf(0 to 125, 4 to 526), GameStore("/saves", fs).loadStats().bestScores)
    }

    @Test
    fun corruptStatsLoadsDefault() {
        val fs = FakeFileSystem()
        fs.createDirectories("/saves".toPath())
        fs.write("/saves/stats.json".toPath()) { writeUtf8("{ not json !!!") }
        assertEquals(GameStore.GameStats(), GameStore("/saves", fs).loadStats())
    }

    @Test
    fun settingsRoundTrip() {
        val fs = FakeFileSystem()
        val store = GameStore("/saves", fs)

        assertEquals(GameStore.Settings(), store.loadSettings())

        store.saveSettings(GameStore.Settings(soundEnabled = false))
        assertEquals(false, store.loadSettings().soundEnabled)

        // A second store on the same fs sees the same file.
        assertEquals(false, GameStore("/saves", fs).loadSettings().soundEnabled)
    }

    @Test
    fun corruptSettingsLoadsDefault() {
        val fs = FakeFileSystem()
        fs.createDirectories("/saves".toPath())
        fs.write("/saves/settings.json".toPath()) { writeUtf8("{ not json !!!") }
        assertEquals(GameStore.Settings(), GameStore("/saves", fs).loadSettings())
    }
}
