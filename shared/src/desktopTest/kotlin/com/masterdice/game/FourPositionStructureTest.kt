package com.masterdice.game

import kotlin.test.Test
import kotlin.test.assertEquals

class FourPositionStructureTest {
    @Test
    fun exhaustiveEnumerationCoversEveryFiveDieDealAndAllEighteenStructures() {
        val structures = enumerateFiveDieStructures()
        assertEquals(18, structures.size)
        assertEquals(376_992, structures.sumOf { it.dealSets })
        assertEquals(fourPositionCases.associate { it.graph to it.count },
            structures.associate { it.graph to it.dealSets })
        val expected = fourPositionCases.associate { it.graph to it.worst }
        val histogram = mutableMapOf<Int, Int>()
        for (structure in structures) {
            val dice = structure.diceIds.map { Die(DieColour.entries[it / 6], it % 6 + 1) }
            val worst = FourPositionMinimax(dice).worstCase()
            assertEquals(expected[structure.graph], worst)
            histogram[worst] = (histogram[worst] ?: 0) + structure.dealSets
        }
        assertEquals(mapOf(4 to 327_600, 5 to 45_000, 6 to 4_392), histogram)
    }
}
