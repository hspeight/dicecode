package com.masterdice.game

import kotlin.test.Test
import kotlin.test.assertEquals

internal data class FourPositionCase(val graph: Int, val ids: List<Int>, val count: Int, val worst: Int)

internal val fourPositionCases = listOf(
    FourPositionCase(0, listOf(0, 7, 14, 21, 28), 4320, 6),
    FourPositionCase(1, listOf(0, 1, 8, 15, 22), 43200, 5),
    FourPositionCase(3, listOf(0, 1, 6, 14, 21), 64800, 4),
    FourPositionCase(19, listOf(0, 1, 2, 9, 16), 14400, 4),
    FourPositionCase(20, listOf(0, 1, 8, 9, 16), 54000, 4),
    FourPositionCase(21, listOf(0, 1, 6, 8, 15), 43200, 4),
    FourPositionCase(23, listOf(0, 1, 2, 6, 15), 43200, 4),
    FourPositionCase(28, listOf(0, 1, 6, 14, 15), 43200, 4),
    FourPositionCase(54, listOf(0, 1, 6, 7, 14), 3600, 4),
    FourPositionCase(58, listOf(0, 1, 6, 8, 13), 14400, 4),
    FourPositionCase(59, listOf(0, 1, 2, 6, 13), 14400, 4),
    FourPositionCase(183, listOf(0, 1, 2, 3, 10), 1800, 5),
    FourPositionCase(184, listOf(0, 1, 2, 9, 10), 10800, 4),
    FourPositionCase(185, listOf(0, 1, 2, 6, 9), 10800, 4),
    FourPositionCase(191, listOf(0, 1, 2, 3, 6), 3600, 4),
    FourPositionCase(207, listOf(0, 1, 2, 6, 12), 3600, 4),
    FourPositionCase(221, listOf(0, 1, 2, 6, 7), 3600, 4),
    FourPositionCase(1023, listOf(0, 1, 2, 3, 4), 72, 6),
)

class FourPositionMinimaxTest {
    @Test
    fun everyRealizableFiveDieStructureHasItsExactCountOnlyGuarantee() {
        for (case in fourPositionCases) {
            val dice = case.ids.map { Die(DieColour.entries[it / 6], it % 6 + 1) }
            assertEquals(case.graph, similarityKey(dice))
            assertEquals(case.worst, FourPositionMinimax(dice).worstCase(), "Graph ${case.graph}")
        }
        assertEquals(6, LEVELS[1].bound)
    }

    @Test
    fun relabelingDoesNotChangeTheExactGuarantee() {
        for (case in fourPositionCases) {
            val dice = case.ids.reversed().map { Die(DieColour.entries[it / 6], it % 6 + 1) }
            assertEquals(case.worst, FourPositionMinimax(dice).worstCase(), "Relabeled graph ${case.graph}")
        }
    }
}
