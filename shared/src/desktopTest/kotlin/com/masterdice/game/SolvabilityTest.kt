package com.masterdice.game

import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Exhaustive minimax over every distinct deal structure: what is the minimum
 * number of submitted guesses that guarantees a win, under optimal play?
 *
 * Feedback only depends on the "similarity graph" of the 4 deal dice (edge =
 * shares colour or value), so all C(36,4) deals collapse into a handful of
 * classes keyed by adjacency up to relabeling.
 */
class SolvabilityTest {

    private val allDice: List<Die> =
        DieColour.entries.flatMap { c -> (1..6).map { v -> Die(c, v) } }

    // All 24 ordered triples of distinct deal positions (index-encoded i*16+j*4+k,
    // where i is the die in column 0, j in column 1, k in column 2).
    private val candidates: List<Int> = (0..3).flatMap { i ->
        (0..3).flatMap { j -> (0..3).map { k -> i * 16 + j * 4 + k } }
    }.filter { code ->
        val i = code / 16; val j = code / 4 % 4; val k = code % 4
        i != j && i != k && j != k
    }

    private val fullMask: Long = candidates.fold(0L) { m, c -> m or (1L shl c) }

    private fun similar(a: Die, b: Die): Boolean =
        a.colour == b.colour || a.value == b.value

    /**
     * Feedback signature of candidate [c] against guess [g] under adjacency
     * [adj]. 0 = all green (win); otherwise per-column G(0)/A(1)/N(2), base 3.
     */
    private fun sig(g: Int, c: Int, adj: Array<BooleanArray>): Int {
        var s = 0
        var place = 1
        var t = g; var u = c
        repeat(3) {
            val gd = t % 4; val cd = u % 4
            s += place * when {
                gd == cd -> 0
                adj[gd][cd] -> 1
                else -> 2
            }
            place *= 3; t /= 4; u /= 4
        }
        return s
    }

    /** Minimax: guesses needed to force a win from candidate set [mask]. */
    private fun worstCase(mask: Long, adj: Array<BooleanArray>, memo: HashMap<Long, Int>): Int {
        if (mask.countOneBits() == 1) return 1
        memo[mask]?.let { return it }
        var best = Int.MAX_VALUE
        for (g in candidates) {
            val classes = HashMap<Int, Long>()
            var m = mask
            while (m != 0L) {
                val bit = m and -m
                m -= bit
                val c = bit.countTrailingZeroBits()
                val s = sig(g, c, adj)
                if (s == 0) continue // g was the truth — won with this guess
                classes[s] = (classes[s] ?: 0L) or bit
            }
            // A guess that partitions nothing gains no information — skip it.
            if (classes.size == 1 && classes.values.single() == mask) continue
            var worst = 0
            for (sub in classes.values) {
                val w = worstCase(sub, adj, memo)
                if (w > worst) worst = w
                if (worst >= best - 1) break // can't improve on best
            }
            if (1 + worst < best) best = 1 + worst
        }
        if (best == Int.MAX_VALUE) best = ROWS + 1
        memo[mask] = best
        return best
    }

    @Test
    fun computeMinimaxForEveryDealStructure() {
        // One representative per relabeling-equivalence class of the deal's
        // similarity graph; canonical key over all 24 permutations of S4.
        val classes = LinkedHashMap<String, Array<BooleanArray>>()
        val dealCount = HashMap<String, Int>()

        for (a in 0 until allDice.size) for (b in a + 1 until allDice.size)
            for (c in b + 1 until allDice.size) for (d in c + 1 until allDice.size) {
                val dice = listOf(allDice[a], allDice[b], allDice[c], allDice[d])
                val adj = Array(4) { i -> BooleanArray(4) { j -> similar(dice[i], dice[j]) } }
                var bestKey: String? = null
                for (p in permutations4()) {
                    val key = buildString(16) {
                        for (i in 0..3) for (j in 0..3)
                            append(if (adj[p[i]][p[j]]) '1' else '0')
                    }
                    if (bestKey == null || key < bestKey!!) bestKey = key
                }
                classes.putIfAbsent(bestKey!!, adj)
                dealCount[bestKey!!] = (dealCount[bestKey] ?: 0) + 1
            }

        val results = classes.map { (key, adj) ->
            Triple(key, dealCount[key]!!, worstCase(fullMask, adj, HashMap()))
        }.sortedBy { it.third }

        val max = results.maxOf { it.third }
        val report = buildString {
            appendLine("Deal similarity-graph classes: ${results.size}")
            for ((key, count, w) in results) {
                appendLine("  graph=$key deals=$count worstCaseGuesses=$w")
            }
            appendLine("MAX worst-case guesses to force a win: $max")
        }
        print(report)
        val workDir = java.io.File(System.getProperty("user.dir"))
        val root = if (workDir.name == "shared") workDir.parentFile else workDir
        java.io.File(root, "tmp").mkdirs()
        java.io.File(root, "tmp/solvability.txt").writeText(report)
        assertTrue(max <= ROWS, "Game must be winnable within $ROWS rows")
    }

    private fun permutations4(): List<IntArray> {
        val out = ArrayList<IntArray>()
        fun perm(a: IntArray, l: Int) {
            if (l == a.size) { out += a.copyOf(); return }
            for (i in l until a.size) {
                val t = a[l]; a[l] = a[i]; a[i] = t
                perm(a, l + 1)
                val u = a[l]; a[l] = a[i]; a[i] = u
            }
        }
        perm(intArrayOf(0, 1, 2, 3), 0)
        return out
    }
}
