package com.masterdice.game

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PuzzleLibraryTest {
    @Test
    fun bankContainsOneThousandDistinctValidDeals() {
        val bank = PuzzleLibrary.puzzles
        assertEquals(PuzzleLibrary.BANK_SIZE, bank.size)
        assertEquals(PuzzleLibrary.BANK_SIZE, bank.map { it.id }.toSet().size)
        val dealKeys = bank.map { puzzle ->
            (puzzle.solution + puzzle.decoy).map { it.colour.ordinal * 6 + it.value }.sorted()
        }
        assertEquals(PuzzleLibrary.BANK_SIZE, dealKeys.toSet().size)
        bank.forEach { puzzle ->
            val deal = puzzle.solution + puzzle.decoy
            assertEquals(3, puzzle.solution.size)
            assertEquals(4, deal.distinct().size)
            assertTrue(deal.all { it.value in 1..6 })
        }
    }

    @Test
    fun cyclicProbeStrategySolvesEverySecretOrderInEveryPuzzle() {
        for (puzzle in PuzzleLibrary.puzzles) {
            val deal = puzzle.solution + puzzle.decoy
            val probes = (0 until 3).map { turn ->
                (0 until 3).map { column -> deal[(turn + column) % 4] }
            }
            for (first in deal.indices) for (second in deal.indices) for (third in deal.indices) {
                if (first == second || first == third || second == third) continue
                val secret = listOf(deal[first], deal[second], deal[third])
                val responses = probes.map { evaluate(it, secret) }
                val inferred = (0 until 3).map { column ->
                    val greenRow = responses.indexOfFirst { it[column] == Feedback.GREEN }
                    if (greenRow >= 0) probes[greenRow][column]
                    else deal[(3 + column) % 4]
                }
                assertEquals(secret, inferred, "Puzzle ${puzzle.id}, secret ${secret.joinToString()}")
            }
        }
    }

    @Test
    fun selectionDoesNotRepeatBeforeExhaustionAndThenStartsANewCycle() {
        var used = emptyList<Int>()
        val draws = ArrayList<Int>(PuzzleLibrary.BANK_SIZE)
        repeat(PuzzleLibrary.BANK_SIZE) { turn ->
            val draw = PuzzleLibrary.draw(turn.toLong(), used)
            assertTrue(draw.puzzle.id !in used, "Repeated puzzle at draw $turn")
            draws += draw.puzzle.id
            used = draw.usedPuzzleIds
        }
        assertEquals(PuzzleLibrary.BANK_SIZE, draws.toSet().size)
        val next = PuzzleLibrary.draw(2_000L, used)
        assertEquals(1, next.usedPuzzleIds.size)
    }

    @Test
    fun engineCarriesPuzzleHistoryIntoTheNextGame() {
        val first = newGame(10L)
        val next = reduce(first, GameIntent.NewGame(11L))
        assertTrue(first.puzzleId != next.puzzleId)
        assertEquals(2, next.usedPuzzleIds.size)
        assertEquals(listOf(first.puzzleId, next.puzzleId), next.usedPuzzleIds)
    }

    @Test
    fun selectedPuzzleAndHistoryAreStableForASeed() {
        val first = PuzzleLibrary.draw(9876L, listOf(1, 2, 3))
        val again = PuzzleLibrary.draw(9876L, listOf(1, 2, 3))
        assertEquals(first, again)
        val state = newGame(9876L, usedPuzzleIds = listOf(1, 2, 3))
        assertEquals(first.puzzle.id, state.puzzleId)
        assertEquals(first.usedPuzzleIds, state.usedPuzzleIds)
    }
}
