package com.masterdice.game

/** Exact depth-feasibility minimax for 120 four-position secrets. No truncated result is accepted. */
internal class FourPositionMinimax(dice: List<Die>, private val budget: Int = 5_000_000) {
    class BudgetExceeded : Exception("Exact four-position minimax budget exceeded")
    private data class Mask(val low: Long, val high: Long) {
        val size: Int get() = low.countOneBits() + high.countOneBits()
        fun intersect(other: Mask) = Mask(low and other.low, high and other.high)
    }
    private val guesses = buildList {
        for (a in dice.indices) for (b in dice.indices) for (c in dice.indices) for (d in dice.indices) {
            if (setOf(a, b, c, d).size == 4) add(intArrayOf(a, b, c, d))
        }
    }
    private val empty = Mask(0, 0)
    private val full = Mask(-1L, (1L shl 56) - 1)
    private val masks = guesses.map { guess ->
        val low = LongArray(21)
        val high = LongArray(21)
        for ((s, secret) in guesses.withIndex()) {
            var greens = 0
            var ambers = 0
            for (col in 0..3) {
                val a = dice[guess[col]]
                val b = dice[secret[col]]
                if (a == b) greens++
                else if (a.colour == b.colour || a.value == b.value) ambers++
            }
            val response = greens * 5 + ambers
            if (s < 64) low[response] = low[response] or (1L shl s)
            else high[response] = high[response] or (1L shl (s - 64))
        }
        Array(21) { Mask(low[it], high[it]) }
    }
    private val capacity = IntArray(11).also { cap ->
        cap[1] = 1
        val branches = masks.maxOf { row -> (0 until 20).count { row[it] != empty } }
        for (depth in 2..10) cap[depth] = minOf(120, 1 + branches * cap[depth - 1])
    }
    private val wins = mutableMapOf<Mask, Int>()
    private val failures = mutableMapOf<Mask, Int>()
    var states = 0
        private set

    init { require(dice.size == 5 && dice.distinct().size == 5) }

    fun worstCase(): Int {
        for (depth in 1..10) if (canSolve(full, depth)) return depth
        return 11 // all depths 1..10 were proved impossible
    }

    private fun canSolve(candidates: Mask, depth: Int): Boolean {
        val count = candidates.size
        if (count <= depth) return true // sequentially submit remaining candidates
        if (depth <= 1 || count > capacity[depth]) return false
        wins[candidates]?.let { if (it <= depth) return true }
        failures[candidates]?.let { if (it >= depth) return false }
        if (++states > budget) throw BudgetExceeded()

        val options = mutableListOf<List<Mask>>()
        for (g in guesses.indices) {
            // At the root, column permutations are symmetric for count feedback:
            // one sorted guess for each of the five possible omitted dice suffices.
            if (candidates == full && !guesses[g].asList().zipWithNext().all { it.first < it.second }) continue
            val parts = (0 until 20).map { masks[g][it].intersect(candidates) }.filter { it != empty }
            if (parts.any { it == candidates || it.size > capacity[depth - 1] }) continue
            options += parts.sortedByDescending { it.size }
        }
        options.sortBy { it.firstOrNull()?.size ?: 0 }
        for (parts in options) {
            if (parts.all { canSolve(it, depth - 1) }) {
                wins[candidates] = minOf(wins[candidates] ?: depth, depth)
                return true
            }
        }
        failures[candidates] = maxOf(failures[candidates] ?: depth, depth)
        return false
    }
}
