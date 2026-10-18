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

    /** Exact search over all 24 legal guesses, including guesses outside the candidate set. */
    private class CountOnlyMinimax(dice: List<Die>) {
        private val guesses = buildList {
            for (a in dice.indices) for (b in dice.indices) for (c in dice.indices) {
                if (a != b && a != c && b != c) add(listOf(dice[a], dice[b], dice[c]))
            }
        }
        private val signatures = guesses.map { guess ->
            guesses.map { secret ->
                // Independently encode the rules rather than relying on the engine evaluator.
                var greens = 0
                var ambers = 0
                guess.zip(secret).forEach { (g, s) ->
                    if (g == s) greens++
                    else if (g.colour == s.colour || g.value == s.value) ambers++
                }
                greens * 4 + ambers
            }
        }
        private val memo = mutableMapOf<Int, Int>()

        fun worstCase(): Int = solve((1 shl guesses.size) - 1)

        private fun solve(candidates: Int): Int {
            // Knowing the secret still costs one final winning submission.
            if (candidates.countOneBits() == 1) return 1
            memo[candidates]?.let { return it }
            var best = guesses.size
            for (g in guesses.indices) {
                val parts = IntArray(13)
                for (secret in guesses.indices) {
                    val bit = 1 shl secret
                    if (candidates and bit == 0) continue
                    val response = signatures[g][secret]
                    if (response != 12) parts[response] = parts[response] or bit
                }
                // A non-winning unchanged branch cannot improve an optimal strategy.
                if (parts.any { it == candidates }) continue
                var worstContinuation = 0
                for (part in parts) {
                    if (part == 0) continue
                    worstContinuation = maxOf(worstContinuation, solve(part))
                    if (1 + worstContinuation >= best) break
                }
                best = minOf(best, 1 + worstContinuation)
            }
            memo[candidates] = best
            return best
        }
    }
}
