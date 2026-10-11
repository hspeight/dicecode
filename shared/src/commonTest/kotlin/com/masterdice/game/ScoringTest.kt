package com.masterdice.game

import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ScoringTest {

    private val l1 = LEVELS[0] // 3 cols, 4 tray: par 3, bound 4, base 100, bonus 25

    private fun guess(vararg feedback: Feedback) =
        Guess(dice = List(feedback.size) { Die(DieColour.RED, it + 1) }, feedback.toList())

    // ---------- win ----------

    @Test
    fun winAtParPaysBase() {
        assertEquals(100, scoreWin(l1, 3))
    }

    @Test
    fun winUnderParPaysBonusPerGuess() {
        assertEquals(125, scoreWin(l1, 2))
    }

    @Test
    fun firstGuessWinScoresSameAsGuessTwo() {
        assertEquals(scoreWin(l1, 2), scoreWin(l1, 1))
    }

    @Test
    fun overParWinLosesBonusPerGuess() {
        assertEquals(75, scoreWin(l1, 4))
        assertEquals(50, scoreWin(l1, 5))
    }

    @Test
    fun overParWinIsFloored() {
        assertEquals(40, scoreWin(l1, 6))
        assertEquals(40, scoreWin(l1, 10))
        for (level in LEVELS) {
            assertEquals(level.floor, scoreWin(level, 10), "level ${level.index}")
        }
    }

    @Test
    fun bonusIsQuarterBase() {
        for (level in LEVELS) {
            assertEquals(
                (level.base / 4f).roundToInt(), level.bonus,
                "level ${level.index}",
            )
        }
    }

    // ---------- best row ----------

    @Test
    fun bestRowPrefersMostGreens() {
        val guesses = listOf(
            guess(Feedback.AMBER, Feedback.AMBER, Feedback.AMBER), // 0G 3A
            guess(Feedback.GREEN, Feedback.NONE, Feedback.NONE),   // 1G 0A — best
        )
        assertEquals(Pair(1, 0), bestRow(guesses))
    }

    @Test
    fun bestRowTieBreaksOnAmbers() {
        val guesses = listOf(
            guess(Feedback.GREEN, Feedback.NONE, Feedback.NONE),   // 1G 0A
            guess(Feedback.GREEN, Feedback.AMBER, Feedback.NONE),  // 1G 1A — best
        )
        assertEquals(Pair(1, 1), bestRow(guesses))
    }

    @Test
    fun bestRowReturnsEarliestOnFullTie() {
        val guesses = listOf(
            guess(Feedback.GREEN, Feedback.AMBER, Feedback.NONE), // row 0 — best
            guess(Feedback.GREEN, Feedback.AMBER, Feedback.NONE), // same, later
        )
        // Both tie; score is identical either way, but the helper must not throw.
        assertEquals(Pair(1, 1), bestRow(guesses))
    }

    @Test
    fun bestRowEmptyGuesses() {
        assertEquals(Pair(0, 0), bestRow(emptyList()))
    }

    // ---------- loss ----------

    @Test
    fun lossPaysBestRowCredit() {
        val guesses = listOf(
            guess(Feedback.NONE, Feedback.NONE, Feedback.NONE),
            guess(Feedback.GREEN, Feedback.GREEN, Feedback.AMBER), // 2G 1A → 24
        )
        assertEquals(24, scoreLoss(l1, guesses))
    }

    @Test
    fun lossWithNoMatchesPaysZero() {
        val guesses = List(3) { guess(Feedback.NONE, Feedback.NONE, Feedback.NONE) }
        assertEquals(0, scoreLoss(l1, guesses))
    }

    @Test
    fun bestLossNeverBeatsWorstWin() {
        for (level in LEVELS) {
            val maxLossGuesses = listOf(
                Guess(
                    dice = List(level.cols) { Die(DieColour.RED, it + 1) },
                    feedback = List(level.cols - 1) { Feedback.GREEN } + Feedback.AMBER,
                ),
            )
            assertTrue(
                scoreLoss(level, maxLossGuesses) < scoreWin(level, 10),
                "level ${level.index}",
            )
        }
    }

    // ---------- gameScore ----------

    @Test
    fun gameScoreNullWhilePlaying() {
        assertNull(gameScore(newGame(1L)))
    }

    @Test
    fun gameScoreUsesLevelFromState() {
        val won = newGame(1L).copy(
            status = GameStatus.WON,
            guesses = listOf(
                guess(Feedback.NONE, Feedback.NONE, Feedback.NONE),
                guess(Feedback.GREEN, Feedback.GREEN, Feedback.GREEN),
            ),
        )
        assertEquals(scoreWin(LEVELS[0], 2), gameScore(won))
    }
}
