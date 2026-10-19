package com.masterdice.game

/** Exact adaptive search for three positions and four or five distinct dealt dice. */
internal class CountOnlyMinimax(private val dice: List<Die>, private val stateBudget: Int = 2_000_000) {
    class BudgetExceeded : Exception("Exact minimax state budget exceeded")
    private val guesses = buildList {
        for (a in dice.indices) for (b in dice.indices) for (c in dice.indices) {
            if (a != b && a != c && b != c) add(intArrayOf(a, b, c))
        }
    }
    private val responseMasks = Array(guesses.size) { g ->
        LongArray(13).also { masks ->
            for (s in guesses.indices) {
                var greens = 0
                var ambers = 0
                for (col in 0..2) {
                    val a = dice[guesses[g][col]]
                    val b = dice[guesses[s][col]]
                    if (a == b) greens++
                    else if (a.colour == b.colour || a.value == b.value) ambers++
                }
                val response = greens * 4 + ambers
                masks[response] = masks[response] or (1L shl s)
            }
        }
    }
    private val memo = mutableMapOf<Long, Int>()
    var states: Int = 0
        private set

    init { require(dice.size in 4..5 && dice.distinct().size == dice.size) }
    fun worstCase(): Int = solve((1L shl guesses.size) - 1)

    private fun solve(candidates: Long): Int {
        if (candidates.countOneBits() == 1) return 1 // final winning submission
        memo[candidates]?.let { return it }
        if (++states > stateBudget) throw BudgetExceeded()
        var best = candidates.countOneBits() // sequential candidate guesses always terminate
        for (masks in responseMasks) {
            // All-green has no continuation. Every other outcome must shrink the set.
            val parts = (0 until 12).map { masks[it] and candidates }.filter { it != 0L }
            if (parts.any { it == candidates }) continue
            var worst = 0
            for (part in parts.sortedByDescending { it.countOneBits() }) {
                worst = maxOf(worst, solve(part))
                if (1 + worst >= best) break
            }
            best = minOf(best, 1 + worst)
            if (best == 2) break // multiple candidates cannot be guaranteed in one guess
        }
        memo[candidates] = best
        return best
    }
}

/** Canonical similarity graph; equality and colour-or-value adjacency determine all feedback. */
internal fun similarityKey(dice: List<Die>): Int {
    val permutation = dice.indices.toMutableList()
    var best = Int.MAX_VALUE
    fun visit(pos: Int) {
        if (pos == dice.size) {
            var key = 0
            var bit = 0
            for (i in dice.indices) for (j in i + 1 until dice.size) {
                val a = dice[permutation[i]]
                val b = dice[permutation[j]]
                if (a.colour == b.colour || a.value == b.value) key = key or (1 shl bit)
                bit++
            }
            best = minOf(best, key)
            return
        }
        for (i in pos until dice.size) {
            val temp = permutation[pos]; permutation[pos] = permutation[i]; permutation[i] = temp
            visit(pos + 1)
            val back = permutation[pos]; permutation[pos] = permutation[i]; permutation[i] = back
        }
    }
    visit(0)
    return best
}
