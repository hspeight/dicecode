package com.masterdice.game

import kotlin.random.Random

sealed interface GameIntent {
    data class Move(val from: Slot, val to: Slot) : GameIntent
    data object Submit : GameIntent
    data class NewGame(val seed: Long) : GameIntent
}

fun newGame(seed: Long): GameState {
    val random = Random(seed)

    fun randomDie(): Die = Die(
        colour = DieColour.entries[random.nextInt(DieColour.entries.size)],
        value = random.nextInt(1, 7),
    )

    val solution = mutableListOf<Die>()
    while (solution.size < GUESS_SIZE) {
        val die = randomDie()
        if (die !in solution) solution += die
    }

    var decoy = randomDie()
    while (decoy in solution) decoy = randomDie()

    val deal = (solution + decoy).shuffled(random)

    return GameState(
        solution = solution,
        deal = deal,
        tray = deal,
        activeCells = List(GUESS_SIZE) { null },
        guesses = emptyList(),
        status = GameStatus.PLAYING,
        seed = seed,
    )
}

fun isLegalMove(state: GameState, from: Slot, to: Slot): Boolean {
    if (state.status != GameStatus.PLAYING) return false
    if (from == to) return false
    if (state.dieAt(from) == null) return false

    val row = state.activeRow
    return when {
        // Tray -> empty cell in the active row.
        from is Slot.Tray && to is Slot.Cell ->
            from.index in 0 until TRAY_SIZE &&
                to.row == row && to.col in 0 until GUESS_SIZE &&
                state.dieAt(to) == null

        // Cell in the active row -> empty tray slot.
        from is Slot.Cell && to is Slot.Tray ->
            from.row == row && from.col in 0 until GUESS_SIZE &&
                to.index in 0 until TRAY_SIZE &&
                state.dieAt(to) == null

        // Cell -> cell within the active row (swap; empty target = plain move).
        from is Slot.Cell && to is Slot.Cell ->
            from.row == row && to.row == row &&
                from.col in 0 until GUESS_SIZE && to.col in 0 until GUESS_SIZE

        else -> false
    }
}

fun evaluate(guess: List<Die>, solution: List<Die>): List<Feedback> =
    guess.mapIndexed { col, die ->
        val header = solution[col]
        val colourMatch = die.colour == header.colour
        val valueMatch = die.value == header.value
        when {
            colourMatch && valueMatch -> Feedback.GREEN
            colourMatch || valueMatch -> Feedback.AMBER
            else -> Feedback.NONE
        }
    }

fun reduce(state: GameState, intent: GameIntent): GameState = when (intent) {
    is GameIntent.NewGame -> newGame(intent.seed)

    is GameIntent.Move -> {
        if (!isLegalMove(state, intent.from, intent.to)) return state
        applyMove(state, intent.from, intent.to)
    }

    GameIntent.Submit -> {
        if (!state.canSubmit) return state
        val dice = state.activeCells.map { it!! }
        val feedback = evaluate(dice, state.solution)
        val won = feedback.all { it == Feedback.GREEN }
        val guess = Guess(dice = dice, feedback = feedback)
        state.copy(
            guesses = state.guesses + guess,
            tray = state.deal,
            activeCells = List(GUESS_SIZE) { null },
            status = when {
                won -> GameStatus.WON
                state.guesses.size + 1 >= ROWS -> GameStatus.LOST
                else -> GameStatus.PLAYING
            },
        )
    }
}

private fun applyMove(state: GameState, from: Slot, to: Slot): GameState {
    val tray = state.tray.toMutableList()
    val cells = state.activeCells.toMutableList()

    fun get(slot: Slot): Die? = when (slot) {
        is Slot.Tray -> tray[slot.index]
        is Slot.Cell -> cells[slot.col]
    }

    fun set(slot: Slot, die: Die?) {
        when (slot) {
            is Slot.Tray -> tray[slot.index] = die
            is Slot.Cell -> cells[slot.col] = die
        }
    }

    val moving = get(from)
    set(from, get(to))
    set(to, moving)

    return state.copy(tray = tray, activeCells = cells)
}
