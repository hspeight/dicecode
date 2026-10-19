package com.masterdice.game

import kotlin.random.Random

/** Shared decoder and without-replacement draw for immutable banks. */
internal class PuzzleBank(
    encoded: String,
    decoyCount: Int,
    private val size: Int,
    solutionSize: Int = 3,
) {
    init { require(solutionSize > 0 && decoyCount > 0 && size > 0) }
    val puzzles: List<ValidatedPuzzle> by lazy {
        encoded.trim().lines().mapIndexed { id, row ->
            require(row.length == (solutionSize + decoyCount) * 2) { "Invalid puzzle bank row $id" }
            val dice = row.chunked(2).map { code ->
                val value = code.toInt(36)
                require(value in 0 until DieColour.entries.size * 6)
                Die(DieColour.entries[value / 6], value % 6 + 1)
            }
            require(dice.distinct().size == dice.size) { "Puzzle $id repeats a die" }
            ValidatedPuzzle(id, dice.take(solutionSize), dice[solutionSize], dice.drop(solutionSize + 1))
        }.also {
            require(it.size == size) { "Expected $size puzzles" }
            require(it.map { p -> (p.solution + p.decoys).map { d -> d.colour.ordinal * 6 + d.value }.sorted() }.distinct().size == size) {
                "Puzzle bank repeats a deal set"
            }
        }
    }

    fun draw(seed: Long, usedPuzzleIds: List<Int>): PuzzleDraw {
        val used = usedPuzzleIds.filter { it in 0 until size }.distinct()
        val available = if (used.size == size) (0 until size).toList()
            else (0 until size).filter { it !in used }
        val puzzle = puzzles[available[Random(seed).nextInt(available.size)]]
        return PuzzleDraw(puzzle, if (used.size == size) listOf(puzzle.id) else used + puzzle.id)
    }
}
