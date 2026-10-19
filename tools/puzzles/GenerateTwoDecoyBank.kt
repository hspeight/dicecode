package com.masterdice.game

import java.io.File
import kotlin.random.Random

/** Run with the common-test CountOnlyMinimax helper on the JVM. Writes deterministic candidate rows and audit data. */
fun main(args: Array<String>) {
    val random = Random(20261019)
    val seen = mutableSetOf<List<Int>>()
    val results = mutableMapOf<Int, Int>()
    val rows = mutableListOf<String>()
    val audit = mutableListOf("id,encoded,similarityGraph,exactWorstCase")
    val histogram = sortedMapOf<Int, Int>()
    val started = System.nanoTime()
    while (rows.size < 1000) {
        val ids = (0 until 36).shuffled(random).take(5)
        if (!seen.add(ids.sorted())) continue
        val dice = ids.map { Die(DieColour.entries[it / 6], it % 6 + 1) }
        val key = similarityKey(dice)
        val worst = results.getOrPut(key) {
            val begin = System.nanoTime()
            val solver = CountOnlyMinimax(dice)
            val value = solver.worstCase()
            println("graph=$key exact=$value states=${solver.states} ms=${(System.nanoTime()-begin)/1_000_000}")
            value
        }
        check(worst <= ROWS) { "Candidate exceeds ten guesses" }
        val encoded = ids.joinToString("") { it.toString(36).padStart(2, '0') }
        audit += "${rows.size},$encoded,$key,$worst"
        rows += encoded
        histogram[worst] = (histogram[worst] ?: 0) + 1
    }
    val out = File(args[0]).also { it.mkdirs() }
    File(out, "bank.txt").writeText(rows.joinToString("\n") + "\n")
    File(out, "audit.csv").writeText(audit.joinToString("\n") + "\n")
    File(out, "summary.txt").writeText("seed=20261019\nsize=${rows.size}\nstructures=${results.size}\nhistogram=$histogram\nelapsedMs=${(System.nanoTime()-started)/1_000_000}\n")
    println(File(out, "summary.txt").readText())
}
