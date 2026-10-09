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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
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
    var showHelp by remember { mutableStateOf(false) }
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
            ) {
                BasicText(
                    text = "Master Dice",
                    style = TextStyle(
                        color = MasterDiceColors.Ivory,
                        fontSize = 22.sp,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    modifier = Modifier.align(Alignment.Center),
                )
                HelpButton(
                    onClick = { showHelp = true },
                    modifier = Modifier.align(Alignment.CenterEnd),
                )
            }

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

        if (showHelp) {
            HelpPanel(onDismiss = { showHelp = false })
        }
    }
}

@Composable
private fun HelpButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(32.dp)
            .testTag("help")
            .semantics { contentDescription = "Show game rules" }
            .clip(RoundedCornerShape(50))
            .background(MasterDiceColors.Brass.copy(alpha = 0.25f))
            .border(1.dp, MasterDiceColors.Brass.copy(alpha = 0.6f), RoundedCornerShape(50))
            .pointerInput(Unit) { detectTapGestures { onClick() } },
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = "?",
            style = TextStyle(
                color = MasterDiceColors.Brass,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
    }
}

@Composable
private fun androidx.compose.foundation.layout.BoxScope.HelpPanel(onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("helpPanel")
            .background(MasterDiceColors.Board.copy(alpha = 0.82f))
            .pointerInput(Unit) { detectTapGestures { onDismiss() } },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 32.dp)
                .clip(WellShape)
                .background(MasterDiceColors.Board)
                .border(1.dp, MasterDiceColors.Brass, WellShape)
                .padding(20.dp)
                // Swallow taps on the card itself so it doesn't dismiss.
                .pointerInput(Unit) { detectTapGestures { } },
        ) {
            BasicText(
                text = "How to play",
                style = TextStyle(
                    color = MasterDiceColors.Ivory,
                    fontSize = 20.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                ),
            )
            Spacer(Modifier.height(12.dp))
            RuleText("Three dice hide in the brass tiles. The tray holds those three plus one decoy — work out which three belong, and in what order.")
            Spacer(Modifier.height(8.dp))
            RuleText("Drag dice from the tray into the row, then tap the check mark. You have $ROWS guesses.")
            Spacer(Modifier.height(12.dp))
            RuleLine("✓", MasterDiceColors.GreenBorder, "Colour and value match that column's hidden die")
            RuleLine("~", MasterDiceColors.AmberBorder, "Colour or value matches — not both")
            RuleLine("·", MasterDiceColors.Ivory.copy(alpha = 0.4f), "Neither matches")
            Spacer(Modifier.height(16.dp))
            BasicText(
                text = "Got it",
                style = TextStyle(
                    color = MasterDiceColors.Board,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                ),
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .semantics { contentDescription = "Close rules" }
                    .clip(WellShape)
                    .background(MasterDiceColors.Brass)
                    .pointerInput(Unit) { detectTapGestures { onDismiss() } }
                    .padding(horizontal = 24.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun RuleText(text: String) {
    BasicText(
        text = text,
        style = TextStyle(
            color = MasterDiceColors.Ivory.copy(alpha = 0.85f),
            fontSize = 14.sp,
        ),
    )
}

@Composable
private fun RuleLine(marker: String, color: androidx.compose.ui.graphics.Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        BasicText(
            text = marker,
            style = TextStyle(color = color, fontSize = 15.sp, fontWeight = FontWeight.Bold),
            modifier = Modifier.width(20.dp),
        )
        BasicText(
            text = text,
            style = TextStyle(
                color = MasterDiceColors.Ivory.copy(alpha = 0.85f),
                fontSize = 14.sp,
            ),
        )
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
                    .shadow(4.dp, WellShape, clip = false)
                    .drawBehind { drawBrassTile() }
                    .border(1.dp, MasterDiceColors.TileEdge, WellShape),
                contentAlignment = Alignment.Center,
            ) {
                if (revealed) {
                    DieView(die, dieSize)
                } else {
                    BasicText(
                        text = "?",
                        style = TextStyle(
                            color = MasterDiceColors.TileGlyph,
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

    Row(
        modifier = rowPanelModifier(isActive, isSubmitted),
        horizontalArrangement = Arrangement.spacedBy(gap),
    ) {
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

    // Colored outer glow behind submitted-cell borders (Stitch feedback rings).
    val glowModifier = when (feedback) {
        Feedback.GREEN -> Modifier.shadow(
            8.dp, WellShape, clip = false,
            ambientColor = MasterDiceColors.GreenBorder,
            spotColor = MasterDiceColors.GreenBorder.copy(alpha = 0.4f),
        )
        Feedback.AMBER -> Modifier.shadow(
            8.dp, WellShape, clip = false,
            ambientColor = MasterDiceColors.AmberBorder,
            spotColor = MasterDiceColors.AmberBorder.copy(alpha = 0.4f),
        )
        else -> Modifier
    }

    val borderModifier = when {
        feedback == Feedback.GREEN ->
            Modifier.border(4.dp, MasterDiceColors.GreenBorder, WellShape)
        feedback == Feedback.AMBER ->
            Modifier.border(2.5.dp, MasterDiceColors.AmberBorder, WellShape)
        feedback == Feedback.NONE ->
            Modifier.border(1.dp, MasterDiceColors.Ivory.copy(alpha = 0.15f), WellShape)
        highlighted ->
            Modifier.border(2.dp, MasterDiceColors.Brass, WellShape)
        // Empty active cells get a dashed outline (drawn in the socket pass
        // below); filled active cells keep a subtle brass edge.
        active && die != null ->
            Modifier.border(1.dp, MasterDiceColors.Brass.copy(alpha = 0.35f), WellShape)
        else -> Modifier
    }

    // Outer box is unclipped so the feedback badge can overhang the corner.
    Box(
        modifier = Modifier
            .size(cell)
            .then(glowModifier)
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
                .drawBehind {
                    drawSocket(
                        tray = slot is Slot.Tray,
                        empty = die == null,
                        dimmed = dimmed,
                        dashed = slot is Slot.Cell && active && die == null &&
                            !highlighted && feedback == null,
                    )
                }
                .then(borderModifier),
            contentAlignment = Alignment.Center,
        ) {
            if (die != null) {
                DieView(die = die, size = dieSize, dimmed = dimmed)
            }
        }
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
            .then(
                if (enabled) {
                    Modifier
                        .shadow(
                            12.dp, WellShape, clip = false,
                            ambientColor = MasterDiceColors.EnterLight,
                            spotColor = MasterDiceColors.EnterLight.copy(alpha = 0.7f),
                        )
                        .drawBehind {
                            val corner = CornerRadius(10.dp.toPx())
                            drawRoundRect(
                                brush = Brush.verticalGradient(
                                    listOf(
                                        MasterDiceColors.EnterLight,
                                        MasterDiceColors.EnterMid,
                                        MasterDiceColors.EnterDark,
                                    ),
                                ),
                                cornerRadius = corner,
                            )
                            clipPath(roundRectPath(size, corner)) {
                                // Inset top highlight (CSS inset_0_1px_1px
                                // rgba(255,255,255,.6)).
                                drawRect(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = 0.6f),
                                            Color.Transparent,
                                        ),
                                        startY = 0f,
                                        endY = 2.dp.toPx(),
                                    ),
                                    size = Size(size.width, 2.dp.toPx()),
                                )
                            }
                        }
                        .border(1.dp, MasterDiceColors.EnterEdge, WellShape)
                } else {
                    Modifier
                        .clip(WellShape)
                        .background(MasterDiceColors.Well.copy(alpha = 0.45f))
                },
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
                color = if (enabled) MasterDiceColors.Ivory
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
            .drawBehind { drawWalnutTray() }
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

private val RowPanelShape = RoundedCornerShape(12.dp)

/** Panel chrome for the active and submitted guess rows (Stitch row 04/03). */
private fun rowPanelModifier(active: Boolean, submitted: Boolean): Modifier = when {
    active -> Modifier
        .shadow(
            15.dp, RowPanelShape, clip = false,
            ambientColor = MasterDiceColors.Brass,
            spotColor = MasterDiceColors.Brass.copy(alpha = 0.35f),
        )
        .background(MasterDiceColors.RowPanel.copy(alpha = 0.95f), RowPanelShape)
        .border(2.dp, MasterDiceColors.Brass, RowPanelShape)
    submitted -> Modifier
        .background(MasterDiceColors.SubmittedPanel.copy(alpha = 0.85f), RowPanelShape)
        .border(1.dp, MasterDiceColors.SubmittedPanelBorder, RowPanelShape)
    else -> Modifier
}

private fun roundRectPath(size: Size, corner: CornerRadius): Path =
    Path().apply { addRoundRect(RoundRect(0f, 0f, size.width, size.height, corner)) }

/**
 * Recessed socket (CSS .cell-well / .cell-well.empty-deeper / .walnut-groove).
 * Radial felt gradient plus clipped inner shadows; tray wells get the carved
 * walnut groove instead. [dashed] draws the brass "tap to place" outline for
 * empty cells on the active row.
 */
private fun DrawScope.drawSocket(
    tray: Boolean,
    empty: Boolean,
    dimmed: Boolean,
    dashed: Boolean,
) {
    val w = size.width
    val h = size.height
    val corner = CornerRadius(10.dp.toPx())
    val shapePath = roundRectPath(size, corner)

    if (tray) {
        // Carved walnut groove — always the deeper look.
        drawRoundRect(
            brush = Brush.verticalGradient(
                0f to MasterDiceColors.GrooveDark,
                0.5f to MasterDiceColors.GrooveMid,
                1f to MasterDiceColors.GrooveLight,
            ),
            cornerRadius = corner,
        )
    } else if (empty) {
        drawRoundRect(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0f to MasterDiceColors.SocketEmptyLight,
                    0.8f to MasterDiceColors.SocketEmptyMid,
                    1f to MasterDiceColors.SocketEmptyDeep,
                ),
                center = Offset(w * 0.5f, h * 0.2f),
                radius = w,
            ),
            cornerRadius = corner,
        )
    } else {
        drawRoundRect(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0f to MasterDiceColors.SocketLight,
                    0.75f to MasterDiceColors.SocketMid,
                    1f to MasterDiceColors.SocketDeep,
                ),
                center = Offset(w * 0.5f, h * 0.15f),
                radius = w * 1.1f,
            ),
            cornerRadius = corner,
        )
    }

    val deep = tray || empty
    clipPath(shapePath) {
        // Top inner shadow: a broad fade plus a harder lip right at the edge.
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Black.copy(alpha = if (deep) 0.85f else 0.6f),
                    Color.Transparent,
                ),
                startY = 0f,
                endY = h * (if (deep) 0.45f else 0.35f),
            ),
        )
        drawRect(
            color = Color.Black.copy(alpha = if (deep) 0.95f else 0.9f),
            size = Size(w, 1.5.dp.toPx()),
        )
        // Bottom inner highlight.
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Transparent,
                    Color.White.copy(alpha = if (deep) 0.08f else 0.12f),
                ),
                startY = h * 0.8f,
                endY = h,
            ),
            topLeft = Offset(0f, h * 0.8f),
            size = Size(w, h * 0.2f),
        )
        // Preserve the old alpha dimming on non-active future rows.
        if (dimmed) {
            drawRect(color = MasterDiceColors.Board.copy(alpha = 0.5f))
        }
    }

    // Faint outer bottom highlight (CSS 0 1px 1px rgba(255,255,255,.05)).
    drawLine(
        color = Color.White.copy(alpha = if (tray) 0.08f else 0.05f),
        start = Offset(corner.x, h + 0.5f.dp.toPx()),
        end = Offset(w - corner.x, h + 0.5f.dp.toPx()),
        strokeWidth = 1.dp.toPx(),
        cap = StrokeCap.Round,
    )

    if (dashed) {
        val inset = 0.75.dp.toPx()
        drawRoundRect(
            color = MasterDiceColors.Brass.copy(alpha = 0.5f),
            topLeft = Offset(inset, inset),
            size = Size(w - inset * 2, h - inset * 2),
            cornerRadius = CornerRadius(corner.x - inset),
            style = Stroke(
                width = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(
                    floatArrayOf(4.dp.toPx(), 3.dp.toPx()),
                ),
            ),
        )
    }
}

/**
 * Brass mystery tile (CSS .brass-tile): 145° brass gradient with a bevel —
 * bright inner lip on top, dark inner shadow on the bottom.
 */
private fun DrawScope.drawBrassTile() {
    val w = size.width
    val h = size.height
    val corner = CornerRadius(10.dp.toPx())

    // CSS 145deg sweeps from the upper-left toward the lower-right.
    drawRoundRect(
        brush = Brush.linearGradient(
            colorStops = MasterDiceColors.BrassTileStops,
            start = Offset(0f, 0f),
            end = Offset(w, h),
        ),
        cornerRadius = corner,
    )
    clipPath(roundRectPath(size, corner)) {
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.White.copy(alpha = 0.7f), Color.Transparent),
                startY = 0f,
                endY = 1.5.dp.toPx(),
            ),
            size = Size(w, 1.5.dp.toPx()),
        )
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f)),
                startY = h - 3.dp.toPx(),
                endY = h,
            ),
            topLeft = Offset(0f, h - 3.dp.toPx()),
            size = Size(w, 3.dp.toPx()),
        )
    }
}

/** Walnut tray strip (CSS .walnut-tray) with carved top rim and dark base. */
private fun DrawScope.drawWalnutTray() {
    val w = size.width
    val h = size.height
    drawRect(
        brush = Brush.verticalGradient(
            0f to MasterDiceColors.TrayTop,
            0.35f to MasterDiceColors.Tray,
            1f to MasterDiceColors.TrayBottom,
        ),
    )
    // Carved lip: top rim, then a fading inner highlight just beneath it.
    val rim = 3.dp.toPx()
    drawRect(color = MasterDiceColors.TrayRimTop, size = Size(w, rim))
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color.White.copy(alpha = 0.2f), Color.Transparent),
            startY = rim,
            endY = rim + 2.dp.toPx(),
        ),
        topLeft = Offset(0f, rim),
        size = Size(w, 2.dp.toPx()),
    )
    // Bottom inner shadow + dark base rim.
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)),
            startY = h - 4.dp.toPx(),
            endY = h,
        ),
        topLeft = Offset(0f, h - 4.dp.toPx()),
        size = Size(w, 4.dp.toPx()),
    )
    drawRect(
        color = MasterDiceColors.TrayRimBottom,
        topLeft = Offset(0f, h - 2.dp.toPx()),
        size = Size(w, 2.dp.toPx()),
    )
}
