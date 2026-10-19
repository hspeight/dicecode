package com.masterdice.game

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class FourPositionPuzzleLibraryTest {
    @Test
    fun bankHasOneThousandUniqueFourPositionDeals() {
        val bank = FourPositionPuzzleLibrary.puzzles
        assertEquals(1000, bank.size)
        assertEquals((0 until 1000).toList(), bank.map { it.id })
        val deals = bank.map { it.solution + it.decoys }
        assertEquals(1000, deals.map { d -> d.map { it.colour.ordinal * 6 + it.value }.sorted() }.distinct().size)
        bank.forEach {
            assertEquals(4, it.solution.size)
            assertEquals(1, it.decoys.size)
            assertEquals(5, (it.solution + it.decoys).distinct().size)
        }
        assertTrue(deals.any { it.map { d -> d.colour }.distinct().size < 5 })
        assertTrue(deals.any { it.map { d -> d.value }.distinct().size < 5 })
    }

    @Test
    fun everyBankEntryHasAnExactSixGuessOrBetterCountOnlyStrategy() {
        val exact = mutableMapOf<Int, Int>()
        val histogram = mutableMapOf<Int, Int>()
        for (puzzle in FourPositionPuzzleLibrary.puzzles) {
            val dice = puzzle.solution + puzzle.decoys
            val worst = exact.getOrPut(similarityKey(dice)) { FourPositionMinimax(dice).worstCase() }
            assertTrue(worst <= FourPositionPuzzleLibrary.GUARANTEED_GUESSES)
            assertTrue(worst <= ROWS)
            histogram[worst] = (histogram[worst] ?: 0) + 1
        }
        assertEquals(mapOf(4 to 857, 5 to 134, 6 to 9), histogram)
        assertEquals(17, exact.size)
        assertEquals(6, LEVELS[FourPositionPuzzleLibrary.LEVEL_INDEX].bound)
    }

    @Test
    fun deterministicDrawsDoNotRepeatUntilAllOneThousandAreUsed() {
        var used = emptyList<Int>()
        repeat(1000) { turn ->
            val draw = FourPositionPuzzleLibrary.draw(turn.toLong(), used)
            assertEquals(draw, FourPositionPuzzleLibrary.draw(turn.toLong(), used))
            assertTrue(draw.puzzle.id !in used)
            used = draw.usedPuzzleIds
        }
        assertEquals(1000, used.distinct().size)
        val next = FourPositionPuzzleLibrary.draw(3000L, used)
        assertEquals(listOf(next.puzzle.id), next.usedPuzzleIds)
    }

    @Test
    fun allThreeBanksKeepIndependentHistoryWhenSwitching() {
        var state = newGame(1L)
        state = reduce(state, GameIntent.NewGame(2L, 2))
        val original = state.usedPuzzleIds
        val twoDecoys = state.usedPuzzleIdsByLevel.getValue(2)
        state = reduce(state, GameIntent.NewGame(3L, 1))
        val fourPositionId = state.puzzleId
        assertEquals(original, state.usedPuzzleIds)
        assertEquals(twoDecoys, state.usedPuzzleIdsByLevel.getValue(2))
        assertEquals(listOf(fourPositionId!!), state.usedPuzzleIdsByLevel.getValue(1))
        state = reduce(state, GameIntent.NewGame(4L, 0))
        state = reduce(state, GameIntent.NewGame(5L, 2))
        state = reduce(state, GameIntent.NewGame(6L, 1))
        assertNotEquals(fourPositionId, state.puzzleId)
        assertEquals(2, state.usedPuzzleIds.size)
        assertEquals(2, state.usedPuzzleIdsByLevel.getValue(1).size)
        assertEquals(2, state.usedPuzzleIdsByLevel.getValue(2).size)
    }

    @Test
    fun engineUsesFourSecretDiceAndRefillsTheFixedFiveDieDeal() {
        var state = newGame(77L, 1)
        val puzzle = FourPositionPuzzleLibrary.puzzles[state.puzzleId!!]
        assertEquals(puzzle.solution, state.solution)
        assertEquals((puzzle.solution + puzzle.decoys).toSet(), state.deal.toSet())
        val deal = state.deal
        // Repeating a deliberately wrong ordered guess must remain legal.
        repeat(2) {
            val wrong = puzzle.decoys + puzzle.solution.take(3)
            wrong.forEachIndexed { col, die ->
                state = reduce(state, GameIntent.Move(
                    Slot.Tray(state.tray.indexOf(die)), Slot.Cell(state.activeRow, col),
                ))
            }
            state = reduce(state, GameIntent.Submit)
            assertEquals(deal, state.tray)
            assertEquals(puzzle.solution, state.solution)
            assertEquals(GameStatus.PLAYING, state.status)
        }
        assertEquals(state.guesses[0], state.guesses[1])
        puzzle.solution.forEachIndexed { col, die ->
            state = reduce(state, GameIntent.Move(
                Slot.Tray(state.tray.indexOf(die)), Slot.Cell(state.activeRow, col),
            ))
        }
        state = reduce(state, GameIntent.Submit)
        assertEquals(GameStatus.WON, state.status)
        assertEquals(3, state.guesses.size)
        assertEquals(4, state.guesses.last().feedback.size)
        val debug = reduce(state, GameIntent.NewGameWithDeal(puzzle.solution, puzzle.decoys, 1))
        assertEquals(state.usedPuzzleIdsByLevel, debug.usedPuzzleIdsByLevel)
    }
}
