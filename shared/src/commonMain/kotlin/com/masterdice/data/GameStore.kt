package com.masterdice.data

import com.masterdice.game.GameState
import com.masterdice.game.SAVE_VERSION
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.SYSTEM

/**
 * Persists the current game as `game.json` in app storage. Writes go to a temp
 * file that is then renamed so a crash mid-write cannot corrupt the save.
 */
class GameStore(
    dir: String,
    private val fs: FileSystem = FileSystem.SYSTEM,
) {
    private val directory: Path = dir.toPath()
    private val target: Path = directory / "game.json"
    private val tmp: Path = directory / "game.json.tmp"
    private val statsTarget: Path = directory / "stats.json"
    private val statsTmp: Path = directory / "stats.json.tmp"
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun load(): GameState? = try {
        if (!fs.exists(target)) {
            null
        } else {
            val serialized = fs.read(target) { readUtf8() }
            val version = json.parseToJsonElement(serialized).jsonObject["version"]?.jsonPrimitive?.intOrNull
            if (version == SAVE_VERSION) {
                json.decodeFromString(GameState.serializer(), serialized)
            } else {
                delete()
                null
            }
        }
    } catch (_: Exception) {
        null // corrupt or unknown format: discard the save, start a new game
    }

    fun save(state: GameState) {
        if (!fs.exists(directory)) fs.createDirectories(directory)
        fs.write(tmp) { writeUtf8(json.encodeToString(GameState.serializer(), state)) }
        fs.delete(target, mustExist = false)
        fs.atomicMove(tmp, target)
    }

    fun delete() {
        fs.delete(tmp, mustExist = false)
        fs.delete(target, mustExist = false)
    }

    /** Best score per level index across all games ever played. */
    @Serializable
    data class GameStats(val bestScores: Map<Int, Int> = emptyMap())

    fun loadStats(): GameStats = try {
        if (!fs.exists(statsTarget)) {
            GameStats()
        } else {
            json.decodeFromString(
                GameStats.serializer(),
                fs.read(statsTarget) { readUtf8() },
            )
        }
    } catch (_: Exception) {
        GameStats() // corrupt stats: start fresh, the game save is unaffected
    }

    fun saveStats(stats: GameStats) {
        if (!fs.exists(directory)) fs.createDirectories(directory)
        fs.write(statsTmp) { writeUtf8(json.encodeToString(GameStats.serializer(), stats)) }
        fs.delete(statsTarget, mustExist = false)
        fs.atomicMove(statsTmp, statsTarget)
    }
}
