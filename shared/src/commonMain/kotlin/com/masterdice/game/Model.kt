package com.masterdice.game

import kotlinx.serialization.Serializable

const val ROWS = 10
const val GUESS_SIZE = 3
const val TRAY_SIZE = 4

const val SAVE_VERSION = 2

@Serializable
enum class DieColour { RED, BLUE, GREEN, ORANGE, PINK, TEAL }

@Serializable
data class Die(val colour: DieColour, val value: Int) {
    init {
        require(value in 1..6) { "Die value must be 1..6, was $value" }
    }
}

@Serializable
sealed interface Slot {
    @Serializable
    data class Tray(val index: Int) : Slot

    @Serializable
    data class Cell(val row: Int, val col: Int) : Slot
}

enum class Feedback { GREEN, AMBER, NONE }

enum class GameStatus { PLAYING, WON, LOST }

@Serializable
data class Guess(val dice: List<Die>, val feedback: List<Feedback>)

@Serializable
data class GameState(
    val version: Int = SAVE_VERSION,
    val solution: List<Die>,
    val deal: List<Die>,
    val tray: List<Die?>,
    val activeCells: List<Die?>,
    val guesses: List<Guess>,
    val status: GameStatus,
    val seed: Long,
) {
    val activeRow: Int get() = guesses.size

    val canSubmit: Boolean
        get() = status == GameStatus.PLAYING && activeCells.all { it != null }

    fun dieAt(slot: Slot): Die? = when (slot) {
        is Slot.Tray -> tray.getOrNull(slot.index)
        is Slot.Cell -> when {
            slot.row == activeRow && slot.col in 0 until GUESS_SIZE ->
                activeCells.getOrNull(slot.col)
            slot.row in guesses.indices && slot.col in 0 until GUESS_SIZE ->
                guesses[slot.row].dice.getOrNull(slot.col)
            else -> null
        }
    }
}
