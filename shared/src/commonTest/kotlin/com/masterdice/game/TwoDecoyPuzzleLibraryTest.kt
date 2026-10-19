package com.masterdice.game

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class TwoDecoyPuzzleLibraryTest {
    @Test
    fun bankHasOneThousandUniqueFiveDieDeals() {
        val bank = TwoDecoyPuzzleLibrary.puzzles
        assertEquals(1000, bank.size)
        assertEquals((0 until 1000).toList(), bank.map { it.id })
        val deals = bank.map { it.solution + it.decoys }
        assertEquals(1000, deals.map { d -> d.map { it.colour.ordinal * 6 + it.value }.sorted() }.distinct().size)
        bank.forEach {
            assertEquals(3, it.solution.size)
            assertEquals(2, it.decoys.size)
            assertEquals(5, (it.solution + it.decoys).distinct().size)
        }
        assertTrue(deals.any { d -> d.map { it.colour }.distinct().size < 5 })
        assertTrue(deals.any { d -> d.map { it.value }.distinct().size < 5 })
    }

    @Test
    fun exactCountOnlyMinimaxValidatesEveryEntry() {
        val byStructure = mutableMapOf<Int, Int>()
        val histogram = mutableMapOf<Int, Int>()
        for (puzzle in TwoDecoyPuzzleLibrary.puzzles) {
            val dice = puzzle.solution + puzzle.decoys
            val worst = byStructure.getOrPut(similarityKey(dice)) { CountOnlyMinimax(dice).worstCase() }
            assertTrue(worst <= ROWS, "Puzzle ${puzzle.id} exceeds ten guesses")
            assertTrue(worst <= TwoDecoyPuzzleLibrary.GUARANTEED_GUESSES)
            histogram[worst] = (histogram[worst] ?: 0) + 1
        }
        assertEquals(mapOf(4 to 832, 5 to 154, 6 to 14), histogram)
        assertEquals(17, byStructure.size)
        assertEquals(6, LEVELS[TwoDecoyPuzzleLibrary.LEVEL_INDEX].bound)
    }

    @Test
    fun seededDrawsAreStableAndDoNotRepeatWithinACycle() {
        var used = emptyList<Int>()
        repeat(1000) { turn ->
            val draw = TwoDecoyPuzzleLibrary.draw(turn.toLong(), used)
            assertEquals(draw, TwoDecoyPuzzleLibrary.draw(turn.toLong(), used))
            assertTrue(draw.puzzle.id !in used)
            used = draw.usedPuzzleIds
        }
        assertEquals(1000, used.distinct().size)
        val next = TwoDecoyPuzzleLibrary.draw(2000L, used)
        assertEquals(listOf(next.puzzle.id), next.usedPuzzleIds)
    }

    @Test
    fun switchingLevelsKeepsHistoriesSeparateAndPreservesOriginalBankIds() {
        val original = newGame(10L)
        val twoDecoys = reduce(original, GameIntent.NewGame(11L, TwoDecoyPuzzleLibrary.LEVEL_INDEX))
        assertEquals(original.usedPuzzleIds, twoDecoys.usedPuzzleIds)
        assertEquals(listOf(twoDecoys.puzzleId!!), twoDecoys.usedPuzzleIdsByLevel.getValue(2))
        val next = reduce(twoDecoys, GameIntent.NewGame(12L, 2))
        assertNotEquals(twoDecoys.puzzleId, next.puzzleId)
        assertEquals(2, next.usedPuzzleIdsByLevel[2]?.size)
        val back = reduce(next, GameIntent.NewGame(13L, 0))
        assertEquals(2, back.usedPuzzleIds.size)
        assertEquals(next.usedPuzzleIdsByLevel, back.usedPuzzleIdsByLevel)
        val again = reduce(back, GameIntent.NewGame(14L, 2))
        assertEquals(3, again.usedPuzzleIdsByLevel[2]?.size)
        assertEquals(back.usedPuzzleIds, again.usedPuzzleIds)
        assertEquals(listOf(Die(DieColour.BLUE, 2), Die(DieColour.ORANGE, 1), Die(DieColour.TEAL, 3)),
            PuzzleLibrary.puzzles[0].solution)
    }

    @Test
    fun engineUsesFixedSelectedDiceAndReturnsBothDecoysToTray() {
        val state = newGame(77L, 2)
        val puzzle = TwoDecoyPuzzleLibrary.puzzles[state.puzzleId!!]
        assertEquals(puzzle.solution, state.solution)
        assertEquals((puzzle.solution + puzzle.decoys).toSet(), state.deal.toSet())
        var played = state
        val wrong = listOf(puzzle.decoys[0], puzzle.decoys[1], puzzle.solution[0])
        wrong.forEachIndexed { col, die ->
            played = reduce(played, GameIntent.Move(
                Slot.Tray(played.tray.indexOf(die)), Slot.Cell(0, col),
            ))
        }
        played = reduce(played, GameIntent.Submit)
        assertEquals(state.deal, played.tray)
        assertEquals(state.solution, played.solution)
        assertEquals(GameStatus.PLAYING, played.status)
        val debug = reduce(played, GameIntent.NewGameWithDeal(puzzle.solution, puzzle.decoys, 2))
        assertEquals(played.usedPuzzleIdsByLevel, debug.usedPuzzleIdsByLevel)
    }
}
