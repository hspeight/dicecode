package com.masterdice.game

import java.io.File

internal data class FiveDieStructure(val graph: Int, val diceIds: List<Int>, val dealSets: Int)

/** Exhaustive 36-choose-5 enumeration, not a random sample. */
internal fun enumerateFiveDieStructures(): List<FiveDieStructure> {
    val canonical = IntArray(1024)
    // Enumerate all five-vertex similarity graphs once, canonicalized by vertex permutations.
    val permutations = mutableListOf<IntArray>()
    fun permute(prefix: List<Int>) {
        if (prefix.size == 5) { permutations += prefix.toIntArray(); return }
        for (i in 0..4) if (i !in prefix) permute(prefix + i)
    }
    permute(emptyList())
    val pairs = (0..4).flatMap { i -> (i + 1..4).map { j -> i to j } }
    val edge = Array(5) { IntArray(5) }
    pairs.forEachIndexed { bit, (i,j) -> edge[i][j] = bit; edge[j][i] = bit }
    for (graph in 0 until 1024) {
        canonical[graph] = permutations.minOf { p ->
            var key = 0
            pairs.forEachIndexed { bit, (i,j) ->
                if (graph and (1 shl edge[p[i]][p[j]]) != 0) key = key or (1 shl bit)
            }
            key
        }
    }
    val reps = sortedMapOf<Int, List<Int>>()
    val counts = mutableMapOf<Int, Int>()
    var total = 0
    for (a in 0..31) for (b in a+1..32) for (c in b+1..33) for (d in c+1..34) for (e in d+1..35) {
        val ids = listOf(a,b,c,d,e)
        var graph = 0
        pairs.forEachIndexed { bit, (i,j) ->
            if (ids[i]/6 == ids[j]/6 || ids[i]%6 == ids[j]%6) graph = graph or (1 shl bit)
        }
        val key = canonical[graph]
        reps.putIfAbsent(key, ids)
        counts[key] = (counts[key] ?: 0) + 1
        total++
    }
    check(total == 376_992)
    return reps.map { (key, ids) -> FiveDieStructure(key, ids, counts.getValue(key)) }
}

fun main(args: Array<String>) {
    val started = System.nanoTime()
    val structures = enumerateFiveDieStructures()
    val total = structures.sumOf { it.dealSets }
    println("Enumerated $total five-die sets, ${structures.size} realizable structures")
    val output = File(args[0]).also { it.mkdirs() }
    val audit = mutableListOf("graph,representative,dealSets,exactWorstCase,states,milliseconds")
    val histogram = sortedMapOf<Int, Int>()
    for ((key, ids, count) in structures) {
        val dice = ids.map { Die(DieColour.entries[it/6], it%6+1) }
        val solver = FourPositionMinimax(dice)
        val start = System.nanoTime()
        val result = solver.worstCase()
        check(result <= ROWS) { "Structure $key cannot be guaranteed within ten guesses" }
        val ms = (System.nanoTime()-start)/1_000_000
        val line = "$key,\"${ids.joinToString(" ")}\",$count,$result,${solver.states},$ms"
        println(line)
        audit += line
        File(output, "structures.csv").writeText(audit.joinToString("\n")+"\n")
        histogram[result] = (histogram[result] ?: 0) + count
    }
    val summary = "dealSets=$total\nstructures=${structures.size}\nhistogram=$histogram\nelapsedMs=${(System.nanoTime()-started)/1_000_000}\n"
    File(output, "summary.txt").writeText(summary)
    println(summary)
}
