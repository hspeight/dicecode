package com.masterdice.game

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PuzzleBankMinimaxTest {
    @Test
    fun everyBankDealIsSolvableWithCountOnlyFeedbackWithinFiveGuesses() {
        // Equality plus this labeled similarity graph completely determines feedback.
        val byStructure = mutableMapOf<List<Boolean>, Int>()
        val histogram = mutableMapOf<Int, Int>()
        for (puzzle in PuzzleLibrary.puzzles) {
            val dice = puzzle.solution + puzzle.decoy
            val structure = dice.flatMapIndexed { i, a ->
                dice.drop(i + 1).map { b -> a.colour == b.colour || a.value == b.value }
            }
            val worst = byStructure.getOrPut(structure) { CountOnlyMinimax(dice).worstCase() }
            assertTrue(worst <= ROWS, "Puzzle ${puzzle.id} exceeds the game limit: $worst")
            assertTrue(worst <= 5, "Puzzle ${puzzle.id} exceeds the documented guarantee: $worst")
            histogram[worst] = (histogram[worst] ?: 0) + 1
        }
        assertEquals(mapOf(3 to 390, 4 to 509, 5 to 101), histogram)
        assertEquals(5, LEVELS[0].bound)
    }

    @Test
    fun firstBankDealNeedsFiveGuessesEvenWithOptimalAdaptivePlay() {
        val puzzle = PuzzleLibrary.puzzles.first()
        assertEquals(5, CountOnlyMinimax(puzzle.solution + puzzle.decoy).worstCase())
    }

}
