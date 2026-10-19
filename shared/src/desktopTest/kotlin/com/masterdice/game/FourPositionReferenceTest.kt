package com.masterdice.game
import java.math.BigInteger
import kotlin.test.Test
import kotlin.test.assertEquals

private fun independent(dice: List<Die>): Int {
    val candidates = buildList {
        for (a in dice) for (b in dice) for (c in dice) for (d in dice) {
            if (setOf(a,b,c,d).size == 4) add(listOf(a,b,c,d))
        }
    }
    val masks = candidates.map { guess ->
        Array(21) { BigInteger.ZERO }.also { row ->
            candidates.forEachIndexed { index, secret ->
                val f = evaluate(guess, secret)
                val sig = f.count { it == Feedback.GREEN } * 5 + f.count { it == Feedback.AMBER }
                row[sig] = row[sig].setBit(index)
            }
        }
    }
    val branches = masks.maxOf { row -> row.take(20).count { it.signum() != 0 } }
    val capacity = IntArray(7).also { cap ->
        cap[1] = 1
        for (d in 2..6) cap[d] = minOf(120, 1 + branches * cap[d-1])
    }
    val memo = mutableMapOf<Pair<BigInteger, Int>, Boolean>()
    fun feasible(state: BigInteger, depth: Int): Boolean {
        if (state.bitCount() == 1) return true
        if (depth <= 1 || state.bitCount() > capacity[depth]) return false
        val key = state to depth
        memo[key]?.let { return it }
        val options = masks.map { row ->
            row.take(20).map { it.and(state) }.filter { it.signum() != 0 }
        }.filter { parts -> parts.none { it == state || it.bitCount() > capacity[depth-1] } }
            .map { it.sortedByDescending { x -> x.bitCount() } }
            .sortedBy { it.firstOrNull()?.bitCount() ?: 0 }
        val result = options.any { parts -> parts.all { feasible(it, depth-1) } }
        memo[key] = result
        return result
    }
    val full = BigInteger.ONE.shiftLeft(120).subtract(BigInteger.ONE)
    return (1..6).first { feasible(full,it) }
}

class FourPositionReferenceTest {
    @Test
    fun arbitraryWidthReferenceAgreesWithoutRootSymmetryOrSequentialShortcut() {
        for (case in fourPositionCases) {
            val dice = case.ids.map { Die(DieColour.entries[it / 6], it % 6 + 1) }
            assertEquals(case.worst, independent(dice), "Reference graph ${case.graph}")
        }
    }
}
