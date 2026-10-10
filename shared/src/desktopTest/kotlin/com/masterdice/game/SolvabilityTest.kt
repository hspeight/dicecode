package com.masterdice.game

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Exact minimax for "guesses to force a win" under the game's feedback rules.
 *
 * Feedback decomposes per column: a response constrains column j's die to a
 * subset ({g}, {x similar to g}, or {x dissimilar to g}). So every consistent
 * candidate set is the set of matchings of a per-column allowed matrix —
 * the state is just c small bitmasks, not a subset of candidates.
 *
 * Universal upper bound: n = c + d guesses always suffice (probe column j
 * with die (t+j) mod n for t = 0..n-2, then submit). So a structure whose
 * minimax reaches n proves the worst case is exactly n.
 */
class SolvabilityTest {

    private val allDice: List<Die> =
        DieColour.entries.flatMap { c -> (1..6).map { v -> Die(c, v) } }

    private fun similar(a: Die, b: Die): Boolean =
        a.colour == b.colour || a.value == b.value

    /** Adjacency matrix for a deal given as grid cell indices colour*6+value-1. */
    private fun adjacency(cells: IntArray): Array<BooleanArray> =
        Array(cells.size) { i ->
            BooleanArray(cells.size) { j ->
                i != j && similar(allDice[cells[i]], allDice[cells[j]])
            }
        }

    /**
     * Minimax solver for one deal structure. State: allowed[j] = n-bit mask of
     * dice still possible in column j. Throws [BudgetExceeded] when the state
     * budget runs out; [upperBound] then holds the best bound found so far.
     */
    class BudgetExceeded : Exception()

    inner class Solver(val n: Int, val c: Int, val adj: Array<BooleanArray>) {
        private val memo = HashMap<Long, Int>()
        var states = 0
        var budget = Int.MAX_VALUE
        var upperBound = n

        private fun pack(allowed: IntArray): Long {
            var k = 0L
            for (j in 0 until c) k = k or (allowed[j].toLong() shl (8 * j))
            return k
        }

        /** Columns in ascending allowed-count order for faster matching search. */
        private fun hasMatching(allowed: IntArray): Boolean {
            val order = (0 until c).sortedBy { allowed[it].countOneBits() }
            fun dfs(t: Int, used: Int): Boolean {
                if (t == c) return true
                var m = allowed[order[t]] and used.inv()
                while (m != 0) {
                    val b = m and -m; m -= b
                    if (dfs(t + 1, used or b)) return true
                }
                return false
            }
            return dfs(0, 0)
        }

        private fun countMatchings(allowed: IntArray, limit: Int): Int {
            val order = (0 until c).sortedBy { allowed[it].countOneBits() }
            var count = 0
            fun dfs(t: Int, used: Int) {
                if (count >= limit) return
                if (t == c) { count++; return }
                var m = allowed[order[t]] and used.inv()
                while (m != 0) {
                    val b = m and -m; m -= b
                    dfs(t + 1, used or b)
                }
            }
            dfs(0, 0)
            return count
        }

        /**
         * Worst-case continuation if we play guess [g] from [allowed].
         * MAX_VALUE if the adversary can stall (a response leaves the state
         * unchanged), otherwise the max solve() over response classes.
         */
        private fun evalGuess(allowed: IntArray, g: IntArray, best: Int): Int {
            // Per-column response masks: G = {g_j}, A = similar, N = dissimilar.
            // Only nonempty options participate — sparse graphs have few As.
            val options = Array(c) { IntArray(3) }
            val nOpts = IntArray(c)
            for (j in 0 until c) {
                val gbit = 1 shl g[j]
                if (allowed[j] and gbit != 0) options[j][nOpts[j]++] = gbit
                var a = 0; var o = 0
                var m = allowed[j] and gbit.inv()
                while (m != 0) {
                    val b = m and -m; m -= b
                    if (adj[g[j]][b.countTrailingZeroBits()]) a = a or b else o = o or b
                }
                if (a != 0) options[j][nOpts[j]++] = a
                if (o != 0) options[j][nOpts[j]++] = o
            }
            var worst = 0
            val child = IntArray(c)
            val seen = HashSet<Long>()
            fun dfs(j: Int, allG: Boolean) {
                if (worst >= best - 1) return
                if (j == c) {
                    if (allG) return // winning response — branch closed
                    if (child.contentEquals(allowed)) {
                        worst = Int.MAX_VALUE - 1 // adversary can stall
                        return
                    }
                    val key = pack(child)
                    if (!seen.add(key)) return
                    if (!hasMatching(child)) return
                    val w = solve(child)
                    if (w > worst) worst = w
                    return
                }
                for (o in 0 until nOpts[j]) {
                    child[j] = options[j][o]
                    dfs(j + 1, allG && child[j] == (1 shl g[j]))
                }
            }
            dfs(0, true)
            return if (worst == Int.MAX_VALUE - 1) Int.MAX_VALUE else worst
        }

        fun solve(allowed: IntArray): Int {
            if (countMatchings(allowed, 2) == 1) return 1
            memo[pack(allowed)]?.let { return it }
            if (++states > budget) throw BudgetExceeded()
            var best = n
            // Empty graph: all dice symmetric — WLOG first guess (0,1,...,c-1).
            val isRoot = allowed.all { it == (1 shl n) - 1 }
            val symmetric = adj.all { row -> row.all { !it } }

            // Guess pool: any ordered c-tuple of distinct deal dice.
            val g = IntArray(c)
            fun dfs(j: Int, used: Int) {
                if (j == c) {
                    val w = evalGuess(allowed, g, best)
                    if (w != Int.MAX_VALUE && 1 + w < best) {
                        best = 1 + w
                        if (best < upperBound) upperBound = best
                    }
                    return
                }
                var m = if (isRoot && symmetric) 1 shl j
                else (1 shl n) - 1 and used.inv()
                while (m != 0 && best > 2) {
                    val b = m and -m; m -= b
                    g[j] = b.countTrailingZeroBits()
                    dfs(j + 1, used or b)
                }
            }
            dfs(0, 0)

            memo[pack(allowed)] = best
            return best
        }
    }

    /**
     * All deal-structure classes of size n with at most [maxEdges] adjacency
     * edges, enumerated as n-subsets of the 6x6 grid (cells = colour*6+value-1),
     * deduped to graph-isomorphism classes. First cell fixed to (0,0) — grid
     * symmetry acts transitively on cells.
     */
    /**
     * Canonical form of an n-vertex similarity graph: minimal packed adjacency
     * over all n! relabelings (n <= 8, so 40320 perms of <=28 edges).
     */
    private fun canonical(a: Array<BooleanArray>, n: Int): Long {
        var best = -1L
        val perm = IntArray(n) { it }
        fun pack(p: IntArray): Long {
            var k = 0L
            var bit = 0
            for (i in 0 until n) for (j in i + 1 until n) {
                if (a[p[i]][p[j]]) k = k or (1L shl bit)
                bit++
            }
            return k
        }
        fun permute(l: Int) {
            if (l == n) {
                val k = pack(perm)
                if (best < 0 || k < best) best = k
                return
            }
            for (i in l until n) {
                val tmp = perm[l]; perm[l] = perm[i]; perm[i] = tmp
                permute(l + 1)
                val u = perm[l]; perm[l] = perm[i]; perm[i] = u
            }
        }
        permute(0)
        return best
    }

    private fun sparseStructures(n: Int, maxEdges: Int): List<IntArray> {
        val leaves = ArrayList<IntArray>()
        val cells = IntArray(n)
        cells[0] = 0

        fun edgeDelta(x: Int, k: Int): Int {
            var e = 0
            for (i in 0 until k) if (similar(allDice[x], allDice[cells[i]])) e++
            return e
        }

        fun dfs(pos: Int, next: Int, edges: Int) {
            if (pos == n) {
                leaves += cells.copyOf()
                return
            }
            for (x in next until 36) {
                val d = edgeDelta(x, pos)
                if (edges + d > maxEdges) continue
                cells[pos] = x
                dfs(pos + 1, x + 1, edges + d)
            }
        }
        dfs(1, 1, 0)

        // Bucket by a cheap invariant, then dedupe within buckets by canonical form.
        val byInvariant = leaves.groupBy { sel ->
            val a = adjacency(sel)
            val deg = IntArray(n) { i -> (0 until n).count { a[i][it] } }.sorted()
            "$deg"
        }
        val byKey = LinkedHashMap<Long, IntArray>()
        for (bucket in byInvariant.values) {
            for (sel in bucket) {
                val key = canonical(adjacency(sel), n)
                if (key !in byKey) byKey[key] = sel
            }
        }
        return byKey.values.toList()
    }

    /**
     * Deal structures to evaluate for deal size n: ALL sparse classes (the
     * likeliest worst cases — every amber edge only adds information) plus a
     * dense representative. Cells are 6x6-grid indices; matching cells (i,i)
     * give an empty graph.
     */
    private fun structures(n: Int): List<Pair<String, IntArray>> {
        val match = IntArray(minOf(n, 6)) { it * 6 + it }
        val list = mutableListOf<Pair<String, IntArray>>()
        when {
            n <= 6 -> {
                list += "empty" to match
                list += "K$n" to IntArray(n) { it }                       // one colour, values 1..n
            }
            n == 7 -> {
                // Minimum realizable edges is 2 — enumerate up to 5 exhaustively.
                for ((i, cells) in sparseStructures(n, 5).withIndex()) {
                    list += "sparse$i" to cells
                }
                list += "K6+1" to match + intArrayOf(6)
            }
            else -> { // n == 8: minimum realizable edges is 4
                for ((i, cells) in sparseStructures(n, 4).withIndex()) {
                    list += "sparse$i" to cells
                }
                // Two disjoint cherries (4 edges, two components + isolates).
                list += "2cherries" to intArrayOf(0, 5, 30, 8, 10, 14, 19, 28)
                // 4-edge path on 5 dice + 3 isolates.
                list += "path4" to intArrayOf(0, 1, 13, 14, 26, 9, 22, 35)
                // 5 edges: two V's sharing nothing.
                list += "cherries" to match + intArrayOf(1, 15)
                // Dense: two same-colour groups of four + densest clique rep.
                list += "K4+K4" to intArrayOf(0, 1, 2, 3, 6, 7, 8, 9)
                list += "K6+2" to match + intArrayOf(6, 7)
            }
        }
        return list
    }

    @Test
    fun guaranteedGuessCounts() {
        // Heavy exhaustive computation — opt in with -Dsolvability.full=true.
        if (System.getProperty("solvability.full") != "true") return

        // Sanity: reproduce the previously computed exhaustive results.
        runCase(3, 4, "empty") { result -> assertEquals(4, result) }
        runCase(3, 5, "empty") { result -> assertEquals(5, result) }

        val table = listOf(3 to 1, 3 to 2, 3 to 3, 3 to 4, 3 to 5,
            4 to 1, 4 to 2, 4 to 3, 4 to 4,
            5 to 1, 5 to 2, 5 to 3,
            6 to 1, 6 to 2)

        val workDir = java.io.File(System.getProperty("user.dir"))
        val root = if (workDir.name == "shared") workDir.parentFile else workDir
        java.io.File(root, "tmp").mkdirs()
        val outFile = java.io.File(root, "tmp/guarantee_table.txt")

        val pool = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors().coerceAtLeast(2)
        )
        // Per-row results: name -> exact value, or name -> -upperBound if the
        // state budget was exceeded (upperBound <= n still applies).
        val rows = ConcurrentHashMap<Int, ConcurrentHashMap<String, Int>>()
        val stateBudget = 500_000

        fun flush() {
            val sb = StringBuilder("cols decoys  n  worst  (per structure; * = budget-capped, shows bound)\n")
            for ((ri, pair) in table.withIndex()) {
                val (cols, decoys) = pair
                val res = rows[ri] ?: continue
                var lo = 0; var hi = 0; var allExact = true
                val per = StringBuilder()
                for ((name, v) in res.toSortedMap()) {
                    if (v >= 0) { if (v > lo) lo = v; if (v > hi) hi = v; per.append(" $name=$v") }
                    else { allExact = false; if (-v > hi) hi = -v; per.append(" $name<=${-v}*") }
                }
                val summary = if (allExact) "$lo" else "$lo..$hi"
                sb.appendLine("%4d %6d %3d  %6s   %s".format(cols, decoys, cols + decoys, summary, per))
            }
            outFile.writeText(sb.toString())
        }

        try {
            for ((ri, pair) in table.withIndex()) {
                val (cols, decoys) = pair
                val size = cols + decoys
                val res = rows.getOrPut(ri) { ConcurrentHashMap() }
                val futures = structures(size).map { (name, cells) ->
                    pool.submit {
                        val solver = Solver(size, cols, adjacency(cells))
                        solver.budget = stateBudget
                        try {
                            res[name] = solver.solve(IntArray(cols) { (1 shl size) - 1 })
                        } catch (e: BudgetExceeded) {
                            res[name] = -solver.upperBound
                        }
                        flush()
                    }
                }
                futures.forEach { it.get() }
            }
        } finally {
            pool.shutdown()
            pool.awaitTermination(1, TimeUnit.MINUTES)
        }
        print(outFile.readText())
        assertTrue(true)
    }

    /**
     * Worst-case guesses under count-only feedback (match indicators hidden):
     * the player learns only how many columns were GREEN/AMBER/NONE, not
     * which. Candidates are ordered c-tuples of distinct deal dice; state is a
     * bitmask over them.
     */
    private fun countOnlySolve(c: Int, cells: IntArray): Int {
        val dice = cells.map { allDice[it] }
        val n = dice.size
        val cand = mutableListOf<IntArray>()
        for (a in 0 until n) for (b in 0 until n) for (d in 0 until n) {
            if (c != 3 || a == b || a == d || b == d) continue
            cand += intArrayOf(a, b, d)
        }
        val full = (1L shl cand.size) - 1
        val memo = HashMap<Long, Int>()

        fun sig(g: IntArray, t: IntArray): Int {
            var greens = 0; var ambers = 0
            for (j in 0 until c) {
                if (g[j] == t[j]) greens++
                else if (similar(dice[g[j]], dice[t[j]])) ambers++
            }
            return greens * 10 + ambers
        }

        fun solve(mask: Long): Int {
            if (mask.countOneBits() == 1) return 1
            memo[mask]?.let { return it }
            var best = Int.MAX_VALUE
            for (g in cand) {
                val parts = HashMap<Int, Long>()
                var m = mask
                while (m != 0L) {
                    val b = m and -m; m -= b
                    val s = sig(g, cand[b.countTrailingZeroBits()])
                    parts[s] = (parts[s] ?: 0L) or b
                }
                if (parts.size == 1) continue // zero-information guess
                var worst = 0
                for ((s, sub) in parts) {
                    if (s >= c * 10) continue // all-green: the guess wins
                    val w = solve(sub)
                    if (w > worst) worst = w
                }
                if (1 + worst < best) best = 1 + worst
            }
            if (best == Int.MAX_VALUE) best = ROWS + 1
            memo[mask] = best
            return best
        }
        return solve(full)
    }

    @Test
    fun countOnlyFeedbackBound() {
        // All realizable 4-die structure classes (any 4-die deal has <=6 edges).
        val workDir = java.io.File(System.getProperty("user.dir"))
        val root = if (workDir.name == "shared") workDir.parentFile else workDir
        val sb = StringBuilder("structure -> worst guesses (count-only feedback)\n")
        var max = 0
        for ((i, cells) in sparseStructures(4, 6).withIndex()) {
            val w = countOnlySolve(3, cells)
            if (w > max) max = w
            sb.appendLine("class$i $w  ${cells.toList()}")
        }
        sb.appendLine("MAX worst-case guesses: $max")
        java.io.File(root, "tmp/count_bound.txt").writeText(sb.toString())
        assertTrue(max <= ROWS)
    }

    private fun runCase(cols: Int, size: Int, structure: String, check: (Int) -> Unit) {
        val cells = structures(size).first { it.first == structure }.second
        val solver = Solver(size, cols, adjacency(cells))
        check(solver.solve(IntArray(cols) { (1 shl size) - 1 }))
    }
}
