package com.masterdice.game

import kotlin.test.Test
import kotlin.test.assertEquals

class FourPositionBankGenerationTest {
    @Test
    fun deterministicGenerationReproducesEveryPublishedPuzzleRow() {
        val expected = FourPositionPuzzleLibrary.puzzles.map { puzzle ->
            (puzzle.solution + puzzle.decoys).joinToString("") {
                (it.colour.ordinal * 6 + it.value - 1).toString(36).padStart(2, '0')
            }
        }
        assertEquals(expected, generateFourPositionBank().map { it.encoded })
    }
}
