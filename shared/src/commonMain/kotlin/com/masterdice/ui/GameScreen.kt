package com.masterdice.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import com.masterdice.game.Die
import com.masterdice.game.Feedback
import com.masterdice.game.GameIntent
import com.masterdice.game.GameState
import com.masterdice.game.GameStatus
import com.masterdice.game.GUESS_SIZE
import com.masterdice.game.ROWS
import com.masterdice.game.Slot
import com.masterdice.game.TRAY_SIZE
import com.masterdice.game.isLegalMove
import kotlin.math.roundToInt

private val WellShape = RoundedCornerShape(10.dp)

@Composable
fun GameScreen(
    state: GameState,
    onIntent: (GameIntent) -> Unit,
    onNewGame: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dd = rememberDragDropState()

    // Rows whose feedback is already revealed. Seeded with the restored guess
    // count so only guesses submitted during this session animate in.
    var revealedRows by remember { mutableIntStateOf(state.guesses.size) }
    val reveal = remember { Animatable(1f) }
    LaunchedEffect(state.guesses.size) {
        if (state.guesses.size > revealedRows) {
            reveal.snapTo(0f)
            reveal.animateTo(1f, tween(GUESS_SIZE * 150))
            revealedRows = state.guesses.size
        }
    }

    fun feedbackShown(row: Int, col: Int): Boolean =
        row < revealedRows ||
            (row == state.guesses.lastIndex && col < reveal.value * GUESS_SIZE)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MasterDiceColors.Board)
            .safeDrawingPadding()
            .onGloballyPositioned { dd.container = it },
    ) {
        val maxBoardWidth = if (maxWidth > 480.dp) 480.dp else maxWidth
        val gap = 6.dp
        val widthCell = (maxBoardWidth - 16.dp - gap * (TRAY_SIZE - 1)) / TRAY_SIZE
        // 12 cell-rows (header + 10 grid + tray) plus title and spacing.
        val heightCell = (maxHeight - 96.dp - gap * 13) / 12f
        val cell = if (widthCell < heightCell) widthCell else heightCell
        val dieSize = cell * 0.82f

        Column(
            modifier = Modifier
                .width(maxBoardWidth)
                .align(Alignment.Center)
                .padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BasicText(
                text = "Master Dice",
                style = TextStyle(
                    color = MasterDiceColors.Ivory,
                    fontSize = 22.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.SemiBold,
                ),
                modifier = Modifier.padding(vertical = 8.dp),
            )

            HeaderRow(state, cell, dieSize, gap)

            Box {
                Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                    for (row in 0 until ROWS) {
                        GuessRow(
                            state = state,
                            row = row,
                            cell = cell,
                            dieSize = dieSize,
                            gap = gap,
                            dd = dd,
                            onIntent = onIntent,
                            feedbackShown = { col -> feedbackShown(row, col) },
                        )
                    }
                }
                if (state.status != GameStatus.PLAYING) {
                    EndPanel(state, onNewGame, Modifier.matchParentSize())
                }
            }

            Spacer(Modifier.height(gap * 2))

            TrayRow(state, cell, dieSize, gap, dd, onIntent)
        }

        DragOverlay(dd, dieSize)
    }
}

@Composable
private fun HeaderRow(state: GameState, cell: Dp, dieSize: Dp, gap: Dp) {
    Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
        for (col in 0 until GUESS_SIZE) {
            val revealed = state.status != GameStatus.PLAYING
            val die = state.solution[col]
            Box(
                modifier = Modifier
                    .size(cell)
                    .testTag("header-$col")
                    .semantics {
                        contentDescription =
                            if (revealed) "Column ${col + 1} hidden die: ${colourName(die.colour)} ${die.value}"
                            else "Hidden die"
                    }
                    .clip(WellShape)
                    .background(MasterDiceColors.Brass),
                contentAlignment = Alignment.Center,
            ) {
                if (revealed) {
                    DieView(die, dieSize)
                } else {
                    BasicText(
                        text = "?",
                        style = TextStyle(
                            color = MasterDiceColors.Board,
                            fontSize = (cell.value * 0.45f).sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                }
            }
        }
        Spacer(Modifier.size(cell))
    }
}

@Composable
private fun GuessRow(
    state: GameState,
    row: Int,
    cell: Dp,
    dieSize: Dp,
    gap: Dp,
    dd: DragDropState,
    onIntent: (GameIntent) -> Unit,
    feedbackShown: (Int) -> Boolean,
) {
    val isActive = state.status == GameStatus.PLAYING && row == state.activeRow
    val isSubmitted = row < state.guesses.size

    Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
        for (col in 0 until GUESS_SIZE) {
            val slot = Slot.Cell(row, col)
            val die = state.dieAt(slot)
            val draggingAway = dd.session?.from == slot
            val highlighted = dd.session?.let { isLegalMove(state, it.from, slot) } == true

            val feedback = if (isSubmitted) state.guesses[row].feedback[col] else null
            val showFeedback = feedback != null && feedbackShown(col)

            CellWell(
                slot = slot,
                die = if (draggingAway) null else die,
                cell = cell,
                dieSize = dieSize,
                dd = dd,
                active = isActive,
                dimmed = !isActive && !isSubmitted,
                highlighted = highlighted,
                feedback = if (showFeedback) feedback else null,
                description = cellDescription(state, row, col, die, feedback),
                draggable = isActive && die != null && !draggingAway,
                state = state,
                onIntent = onIntent,
            )
        }

        // Column 4: enter cell on the active row, green count on submitted rows.
        when {
            isActive -> EnterCell(state, cell, onIntent)
            isSubmitted -> {
                val greens = state.guesses[row].feedback.count { it == Feedback.GREEN }
                Box(
                    modifier = Modifier
                        .size(cell)
                        .semantics { contentDescription = "Row ${row + 1}: $greens exact matches" },
                    contentAlignment = Alignment.Center,
                ) {
                    BasicText(
                        text = "$greens",
                        style = TextStyle(
                            color = MasterDiceColors.Ivory.copy(alpha = 0.5f),
                            fontSize = (cell.value * 0.35f).sp,
                        ),
                    )
                }
            }
            else -> Spacer(Modifier.size(cell))
        }
    }
}

private fun cellDescription(
    state: GameState,
    row: Int,
    col: Int,
    die: Die?,
    feedback: Feedback?,
): String {
    val base = "Row ${row + 1}, column ${col + 1}"
    if (die == null) return "$base: empty"
    val dieText = "${colourName(die.colour)} ${die.value}"
    return when (feedback) {
        Feedback.GREEN -> "$base: $dieText, exact match"
        Feedback.AMBER -> "$base: $dieText, partial match"
        Feedback.NONE -> "$base: $dieText, no match"
        null -> "$base: $dieText"
    }
}

@Composable
private fun CellWell(
    slot: Slot,
    die: Die?,
    cell: Dp,
    dieSize: Dp,
    dd: DragDropState,
    active: Boolean,
    dimmed: Boolean,
    highlighted: Boolean,
    feedback: Feedback?,
    description: String,
    draggable: Boolean,
    state: GameState,
    onIntent: (GameIntent) -> Unit,
) {
    // The drag gesture lives on the persistent well (keyed on slot only) so
    // recomposition mid-drag — the die leaving the slot, highlight changes —
    // can never cancel the gesture coroutine.
    val currentState by rememberUpdatedState(state)
    val currentDie by rememberUpdatedState(die)
    val currentDraggable by rememberUpdatedState(draggable)
    val currentOnIntent by rememberUpdatedState(onIntent)

    val borderModifier = when {
        feedback == Feedback.GREEN ->
            Modifier.border(4.dp, MasterDiceColors.GreenBorder, WellShape)
        feedback == Feedback.AMBER ->
            Modifier.border(2.5.dp, MasterDiceColors.AmberBorder, WellShape)
        feedback == Feedback.NONE ->
            Modifier.border(1.dp, MasterDiceColors.Ivory.copy(alpha = 0.15f), WellShape)
        highlighted ->
            Modifier.border(2.dp, MasterDiceColors.Brass, WellShape)
        active ->
            Modifier.border(1.dp, MasterDiceColors.Brass.copy(alpha = 0.35f), WellShape)
        else -> Modifier
    }

    // Outer box is unclipped so the feedback badge can overhang the corner.
    Box(
        modifier = Modifier
            .size(cell)
            .then(
                if (slot is Slot.Cell) Modifier.testTag("cell-${slot.row}-${slot.col}")
                else Modifier.testTag("tray-${(slot as Slot.Tray).index}")
            )
            .semantics { contentDescription = description }
            .onGloballyPositioned { dd.register(slot, it) }
            .pointerInput(slot) {
                try {
                    detectDragGestures(
                        onDragStart = {
                            val d = currentDie
                            if (currentDraggable && d != null && dd.session == null) {
                                dd.begin(slot, d)
                            }
                        },
                        onDrag = { change, amount ->
                            val s = dd.session
                            if (s != null && s.from == slot && !s.returning) {
                                dd.moveBy(amount)
                                change.consume()
                            }
                        },
                        onDragEnd = {
                            val s = dd.session
                            if (s != null && s.from == slot && !s.returning) {
                                val target = dd.slotAt(s.offset)
                                if (target != null &&
                                    isLegalMove(currentState, s.from, target)
                                ) {
                                    currentOnIntent(GameIntent.Move(s.from, target))
                                    dd.finish()
                                } else {
                                    dd.startReturn()
                                }
                            }
                        },
                        onDragCancel = {
                            val s = dd.session
                            if (s != null && s.from == slot && !s.returning) {
                                dd.startReturn()
                            }
                        },
                    )
                } finally {
                    // If this gesture coroutine is ever cancelled mid-drag,
                    // return the die instead of leaving the session stuck.
                    val s = dd.session
                    if (s != null && s.from == slot && !s.returning) {
                        dd.startReturn()
                    }
                }
            },
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(WellShape)
                .background(
                    when {
                        dimmed -> MasterDiceColors.Well.copy(alpha = 0.45f)
                        active -> MasterDiceColors.WellActive
                        else -> MasterDiceColors.Well
                    }
                )
                .then(borderModifier),
            contentAlignment = Alignment.Center,
        ) {
            if (die != null) {
                DieView(die = die, size = dieSize, dimmed = dimmed)
            }
        }
        when (feedback) {
            Feedback.GREEN -> FeedbackBadge("✓", MasterDiceColors.GreenBorder, cell)
            Feedback.AMBER -> FeedbackBadge("~", MasterDiceColors.AmberBorder, cell)
            else -> {}
        }
    }
}

@Composable
private fun BoxScope.FeedbackBadge(text: String, color: Color, cell: Dp) {
    val badge = cell * 0.32f
    Box(
        modifier = Modifier
            .align(Alignment.TopStart)
            .offset(x = -badge * 0.2f, y = -badge * 0.2f)
            .size(badge)
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = text,
            style = TextStyle(
                color = MasterDiceColors.Board,
                fontSize = (badge.value * 0.6f).sp,
                fontWeight = FontWeight.Bold,
            ),
        )
    }
}

@Composable
private fun EnterCell(state: GameState, cell: Dp, onIntent: (GameIntent) -> Unit) {
    val enabled = state.canSubmit
    Box(
        modifier = Modifier
            .size(cell)
            .testTag("enter")
            .semantics {
                contentDescription =
                    if (enabled) "Submit guess" else "Submit guess, disabled until the row is full"
            }
            .clip(WellShape)
            .background(
                if (enabled) MasterDiceColors.Brass
                else MasterDiceColors.Well.copy(alpha = 0.45f)
            )
            .pointerInput(enabled) {
                if (enabled) {
                    detectTapGestures { onIntent(GameIntent.Submit) }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = "✓",
            style = TextStyle(
                color = if (enabled) MasterDiceColors.Board
                else MasterDiceColors.Ivory.copy(alpha = 0.25f),
                fontSize = (cell.value * 0.45f).sp,
                fontWeight = FontWeight.Bold,
            ),
        )
    }
}

@Composable
private fun TrayRow(
    state: GameState,
    cell: Dp,
    dieSize: Dp,
    gap: Dp,
    dd: DragDropState,
    onIntent: (GameIntent) -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(WellShape)
            .background(MasterDiceColors.Tray)
            .border(3.dp, MasterDiceColors.TrayRim, WellShape)
            .padding(gap),
        horizontalArrangement = Arrangement.spacedBy(gap),
    ) {
        for (i in 0 until TRAY_SIZE) {
            val slot = Slot.Tray(i)
            val die = state.tray[i]
            val draggingAway = dd.session?.from == slot
            val highlighted = dd.session?.let { isLegalMove(state, it.from, slot) } == true
            val canDrag = state.status == GameStatus.PLAYING && die != null && !draggingAway

            CellWell(
                slot = slot,
                die = if (draggingAway) null else die,
                cell = cell,
                dieSize = dieSize,
                dd = dd,
                active = state.status == GameStatus.PLAYING,
                dimmed = false,
                highlighted = highlighted,
                feedback = null,
                description = "Tray slot ${i + 1}: " +
                    (die?.let { "${colourName(it.colour)} ${it.value}" } ?: "empty"),
                draggable = canDrag,
                state = state,
                onIntent = onIntent,
            )
        }
    }
}

@Composable
private fun EndPanel(state: GameState, onNewGame: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .testTag("endPanel")
            .background(MasterDiceColors.Board.copy(alpha = 0.55f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(MasterDiceColors.Board)
                .border(1.dp, MasterDiceColors.Brass, RoundedCornerShape(12.dp))
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BasicText(
                text = if (state.status == GameStatus.WON)
                    "Solved in ${state.guesses.size} guess${if (state.guesses.size == 1) "" else "es"}"
                else "Out of guesses",
                style = TextStyle(
                    color = MasterDiceColors.Ivory,
                    fontSize = 24.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                ),
            )
            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .testTag("newGame")
                    .semantics { contentDescription = "New game" }
                    .clip(WellShape)
                    .background(MasterDiceColors.Brass)
                    .pointerInput(Unit) { detectTapGestures { onNewGame() } }
                    .padding(horizontal = 24.dp, vertical = 10.dp),
            ) {
                BasicText(
                    text = "New game",
                    style = TextStyle(
                        color = MasterDiceColors.Board,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
        }
    }
}

@Composable
private fun BoxScope.DragOverlay(dd: DragDropState, dieSize: Dp) {
    val session = dd.session ?: return
    val returnOffset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }

    if (session.returning) {
        LaunchedEffect(session) {
            returnOffset.snapTo(session.offset)
            val target = dd.centerOf(session.from) ?: session.offset
            returnOffset.animateTo(target, tween(200))
            dd.finish()
        }
    }

    val pos = if (session.returning) returnOffset.value else session.offset
    val half = dieSize / 2

    Box(
        modifier = Modifier
            .align(Alignment.TopStart)
            .testTag("dragOverlay")
            .offset { IntOffset((pos.x - half.toPx()).roundToInt(), (pos.y - half.toPx()).roundToInt()) }
            .graphicsLayer { scaleX = 1.12f; scaleY = 1.12f }
            .shadow(12.dp, RoundedCornerShape(dieSize * 0.18f)),
    ) {
        DieView(session.die, dieSize)
    }
}
