package com.masterdice.game

import kotlin.math.roundToInt

/**
 * A difficulty level: `cols` hidden dice, `traySize` dealt dice
 * (`traySize - cols` decoys). `par` is the skill target (~90th percentile of a
 * consistency player); `bound` is the guaranteed-solve limit shown to the
 * player as "always solvable in N" — null while unproven (universal bound is
 * `traySize` guesses).
 */
data class Level(
    val index: Int,
    val cols: Int,
    val traySize: Int,
    val par: Int,
    val bound: Int?,
    val base: Int,
) {
    val decoys: Int get() = traySize - cols

    /** Points added per guess under par (25% of base). */
    val bonus: Int get() = (base / 4f).roundToInt()
}

val LEVELS = listOf(
    Level(0, cols = 3, traySize = 4, par = 3, bound = 4, base = 100),
    Level(1, cols = 4, traySize = 5, par = 3, bound = 5, base = 150),
    Level(2, cols = 3, traySize = 5, par = 4, bound = 5, base = 200),
    Level(3, cols = 3, traySize = 6, par = 4, bound = 6, base = 250),
    Level(4, cols = 4, traySize = 6, par = 4, bound = 6, base = 350),
    Level(5, cols = 4, traySize = 7, par = 4, bound = null, base = 500),
    Level(6, cols = 4, traySize = 8, par = 5, bound = null, base = 700),
)

/** Win in [guesses] guesses: base plus a bonus per guess under par. A first
 *  guess is pure luck, so `max(guesses, 2)` caps it at guess-2 credit. */
fun scoreWin(level: Level, guesses: Int): Int =
    level.base + level.bonus * maxOf(0, level.par - maxOf(guesses, 2))

/**
 * The strongest single guess: most greens, then most ambers, then earliest.
 * Returns (greens, ambers) — (0, 0) when there are no guesses.
 */
fun bestRow(guesses: List<Guess>): Pair<Int, Int> {
    var best = Pair(0, 0)
    for (guess in guesses) {
        val greens = guess.feedback.count { it == Feedback.GREEN }
        val ambers = guess.feedback.count { it == Feedback.AMBER }
        if (greens > best.first || (greens == best.first && ambers > best.second)) {
            best = Pair(greens, ambers)
        }
    }
    return best
}

/** Loss credit: 10% of base per best-row green, ambers at 40%. */
fun scoreLoss(level: Level, guesses: List<Guess>): Int {
    val (greens, ambers) = bestRow(guesses)
    return (level.base * 0.10 * (greens + 0.4 * ambers)).roundToInt()
}

/** Points for a finished game, or null while still playing. */
fun gameScore(state: GameState, level: Level = LEVELS[0]): Int? = when (state.status) {
    GameStatus.PLAYING -> null
    GameStatus.WON -> scoreWin(level, state.guesses.size)
    GameStatus.LOST -> scoreLoss(level, state.guesses)
}
