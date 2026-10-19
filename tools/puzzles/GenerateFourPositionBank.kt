package com.masterdice.game

import java.io.File
import kotlin.random.Random

internal const val FOUR_POSITION_BANK_SEED = 2026101904

internal data class GeneratedFourPositionPuzzle(val encoded: String, val graph: Int, val worst: Int)

/** Deterministic selection; each accepted set is checked with exact 120-secret minimax. */
internal fun generateFourPositionBank(): List<GeneratedFourPositionPuzzle> {
    val random = Random(FOUR_POSITION_BANK_SEED)
    val seen = mutableSetOf<List<Int>>()
    val exactByStructure = mutableMapOf<Int, Int>()
    return buildList {
        while (size < 1000) {
            val ids = (0 until 36).shuffled(random).take(5)
            if (!seen.add(ids.sorted())) continue
            val dice = ids.map { Die(DieColour.entries[it / 6], it % 6 + 1) }
            val graph = similarityKey(dice)
            val worst = exactByStructure.getOrPut(graph) { FourPositionMinimax(dice).worstCase() }
            check(worst <= 6 && worst <= ROWS) { "Candidate is not proved within six guesses" }
            add(GeneratedFourPositionPuzzle(
                ids.joinToString("") { it.toString(36).padStart(2, '0') }, graph, worst,
            ))
        }
    }
}

fun main(args: Array<String>) {
    val started = System.nanoTime()
    val bank = generateFourPositionBank()
    val out = File(args[0]).also { it.mkdirs() }
    File(out, "bank.txt").writeText(bank.joinToString("\n") { it.encoded } + "\n")
    File(out, "audit.csv").writeText("id,encoded,similarityGraph,exactWorstCase\n" +
        bank.mapIndexed { id, p -> "$id,${p.encoded},${p.graph},${p.worst}" }.joinToString("\n") + "\n")
    val histogram = bank.groupingBy { it.worst }.eachCount().toSortedMap()
    File(out, "summary.txt").writeText(
        "seed=$FOUR_POSITION_BANK_SEED\nsize=${bank.size}\nstructures=${bank.map { it.graph }.distinct().size}\n" +
            "histogram=$histogram\nelapsedMs=${(System.nanoTime() - started) / 1_000_000}\n",
    )
    println(File(out, "summary.txt").readText())
}
