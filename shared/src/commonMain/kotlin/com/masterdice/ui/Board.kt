package com.masterdice.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import com.masterdice.game.Die
import com.masterdice.game.Feedback
import com.masterdice.game.GameIntent
import com.masterdice.game.GameState
import com.masterdice.game.GameStatus
import com.masterdice.game.ROWS
import com.masterdice.game.Slot
import com.masterdice.game.isLegalMove
import com.masterdice.platform.SoundFx
import com.masterdice.resources.Res
import com.masterdice.resources.enter_key
import androidx.compose.ui.layout.ContentScale
import org.jetbrains.compose.resources.painterResource

internal val WellShape = RoundedCornerShape(10.dp)

// Geometry of the baked board/strip artwork per level (source pixels; the
// bundled resources are 0.6x downscales — identical fractions). The render
// page tmp/board_render.html computes the same numbers: margin 98, well
// pitch 532, row pitch 531.8, die 500, 576px peg track after a 64px gap,
// deal strip the full board width with evenly spaced dice.
internal data class BoardSpec(
    val imageWidthPx: Float,
    val boardAspect: Float,
    val wellPx: Float,
    val colsPx: FloatArray,
    val trackCxPx: Float,
    val rowTopPx: Float,
    val rowPitchPx: Float,
    val dealAspect: Float,
    val dealDieTopPx: Float,
    val dealColsPx: FloatArray,
    val dealWellPx: Float = wellPx,
)

internal fun boardSpec(levelIndex: Int): BoardSpec = when (levelIndex) {
    2 -> boardSpec(0).copy(
        dealColsPx = floatArrayOf(60f, 530f, 1000f, 1470f, 1940f),
        dealDieTopPx = 150f,
        dealWellPx = 400f,
    )
    // board_l1.png + deal_l1.png: 2932x5482, 4 wells/row, 5-die strip.
    1 -> BoardSpec(
        imageWidthPx = 2932f,
        boardAspect = 5482f / 2932f,
        wellPx = 500f,
        colsPx = floatArrayOf(98f, 630f, 1162f, 1694f),
        trackCxPx = 2546f,
        rowTopPx = 98f,
        rowPitchPx = 531.8f,
        dealAspect = 700f / 2932f,
        dealDieTopPx = 100f,
        dealColsPx = floatArrayOf(72f, 644f, 1216f, 1788f, 2360f),
    )
    // board_4x10.png + deal_row.png: 2400x5482, 3 wells/row, 4-die strip.
    else -> BoardSpec(
        imageWidthPx = 2400f,
        boardAspect = 5482f / 2400f,
        wellPx = 500f,
        colsPx = floatArrayOf(98f, 630f, 1162f),
        trackCxPx = 2014f,
        rowTopPx = 98f,
        rowPitchPx = 531.8f,
        dealAspect = 700f / 2400f,
        dealDieTopPx = 100f,
        dealColsPx = floatArrayOf(80f, 660f, 1240f, 1820f),
    )
}

@Composable
internal fun BoxScope.GuessRow(
    state: GameState,
    spec: BoardSpec,
    row: Int,
    boardW: Dp,
    cell: Dp,
    dieSize: Dp,
    enterSize: Dp,
    dd: DragDropState,
    onIntent: (GameIntent) -> Unit,
    feedbackShown: (Int) -> Boolean,
    showMatchIndicator: Boolean,
    onEmptyTap: (Slot.Cell) -> Unit,
) {
    val isActive = state.status == GameStatus.PLAYING && row == state.activeRow
    val isSubmitted = row < state.guesses.size

    // Artwork well positions in board-width fractions.
    val colX = spec.colsPx.map { boardW * (it / spec.imageWidthPx) }
    val rowTop =
        boardW * ((spec.rowTopPx + (ROWS - 1 - row) * spec.rowPitchPx) / spec.imageWidthPx)
    // Center-x of the baked peg track that follows the guess columns.
    val trackCx = boardW * (spec.trackCxPx / spec.imageWidthPx)

    for (col in 0 until state.level.cols) {
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
            alpha = 1f,
            highlighted = highlighted,
            feedback = if (showFeedback) feedback else null,
            showMatchIndicator = showMatchIndicator,
            description = cellDescription(
                state, row, col, die,
                if (showMatchIndicator && showFeedback) feedback else null,
            ),
            draggable = isActive && die != null && !draggingAway,
            state = state,
            onIntent = onIntent,
            onTap = { SoundFx.tap(); onIntent(GameIntent.TapMove(slot)) },
            onEmptyTap = { onEmptyTap(slot) },
            modifier = Modifier.absoluteOffset(x = colX[col], y = rowTop),
        )
    }

    // Column 4: enter cell on the active row, feedback pegs on submitted rows.
    // The cell is sized to the enter key and centered on the peg track.
    Box(
        modifier = Modifier
            .absoluteOffset(
                x = trackCx - enterSize / 2,
                y = rowTop + (cell - enterSize) / 2,
            )
            .size(enterSize),
        contentAlignment = Alignment.Center,
    ) {
        when {
            isActive -> EnterCell(state, enterSize, onIntent)
            isSubmitted -> {
                val feedback = state.guesses[row].feedback
                val greens = feedback.count { it == Feedback.GREEN }
                val ambers = feedback.count { it == Feedback.AMBER }
                Box(
                    modifier = Modifier
                        .size(enterSize)
                        .clip(RoundedCornerShape(5.dp))
                        .background(
                            MasterDiceColors.Board.copy(alpha = 0.55f),
                        )
                        .border(
                            1.dp,
                            MasterDiceColors.SubmittedPanelBorder,
                            RoundedCornerShape(5.dp),
                        )
                        .semantics {
                            contentDescription =
                                "Row ${row + 1}: $greens exact, $ambers partial matches"
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        val sorted = feedback.sortedBy { it.ordinal }
                        val top = sorted.take((sorted.size + 1) / 2)
                        for (pegs in listOf(top, sorted.drop(top.size))) {
                            if (pegs.isNotEmpty()) {
                                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                    for (f in pegs) FeedbackPeg(f)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeedbackPeg(f: Feedback) {
    Box(
        modifier = Modifier
            .size(6.dp)
            .background(
                when (f) {
                    Feedback.GREEN -> MasterDiceColors.GreenBorder
                    Feedback.AMBER -> MasterDiceColors.AmberBorder
                    Feedback.NONE ->
                        MasterDiceColors.Ivory.copy(alpha = 0.18f)
                },
                CircleShape,
            ),
    )
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
    alpha: Float,
    highlighted: Boolean,
    feedback: Feedback?,
    showMatchIndicator: Boolean,
    description: String,
    draggable: Boolean,
    state: GameState,
    onIntent: (GameIntent) -> Unit,
    onTap: () -> Unit,
    onEmptyTap: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    // The drag gesture lives on the persistent well (keyed on slot only) so
    // recomposition mid-drag — the die leaving the slot, highlight changes —
    // can never cancel the gesture coroutine.
    val currentState by rememberUpdatedState(state)
    val currentDie by rememberUpdatedState(die)
    val currentDraggable by rememberUpdatedState(draggable)
    val currentActive by rememberUpdatedState(active)
    val currentOnIntent by rememberUpdatedState(onIntent)
    val currentOnTap by rememberUpdatedState(onTap)
    val currentOnEmptyTap by rememberUpdatedState(onEmptyTap)

    // Positional feedback is exposed visually and through semantics only in debug mode.
    val shownFeedback = if (showMatchIndicator) feedback else null

    // Tray cells draw their wells to match the die corner radius; grid cells
    // sit on the board artwork's wells and draw nothing underneath.
    val isTray = slot is Slot.Tray
    val shape = if (isTray) RoundedCornerShape(dieSize * 0.18f) else WellShape

    // Colored outer glow behind submitted-cell borders (Stitch feedback rings).
    val glowModifier = when (shownFeedback) {
        Feedback.GREEN -> Modifier.shadow(
            8.dp, shape, clip = false,
            ambientColor = MasterDiceColors.GreenBorder,
            spotColor = MasterDiceColors.GreenBorder.copy(alpha = 0.4f),
        )
        Feedback.AMBER -> Modifier.shadow(
            8.dp, shape, clip = false,
            ambientColor = MasterDiceColors.AmberBorder,
            spotColor = MasterDiceColors.AmberBorder.copy(alpha = 0.4f),
        )
        else -> Modifier
    }

    val borderModifier = when {
        shownFeedback == Feedback.GREEN ->
            Modifier.border(4.dp, MasterDiceColors.GreenBorder, shape)
        shownFeedback == Feedback.AMBER ->
            Modifier.border(2.5.dp, MasterDiceColors.AmberBorder, shape)
        shownFeedback == Feedback.NONE ->
            Modifier.border(1.dp, MasterDiceColors.Ivory.copy(alpha = 0.15f), shape)
        highlighted ->
            Modifier.border(2.dp, MasterDiceColors.Brass, shape)
        // Active-row cells get a dashed outline in the socket pass below.
        else -> Modifier
    }

    Box(
        modifier = modifier
            .size(cell)
            .then(glowModifier)
            .then(
                if (slot is Slot.Cell) Modifier.testTag("cell-${slot.row}-${slot.col}")
                else Modifier.testTag("tray-${(slot as Slot.Tray).index}")
            )
            .semantics { contentDescription = description }
            .onGloballyPositioned {
                // Grow the drop target to the die's footprint when the cell
                // is deliberately undersized (tray cells sit under full-size
                // dice); grid cells keep exact bounds (ratio = 1 → no inflate).
                val inflate = it.size.width / 2f *
                    (dieSize / cell - 1f).coerceAtLeast(0f)
                dd.register(slot, it, inflate)
            }
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
                                    SoundFx.tap()
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
            }
            // Tap = quick move: tray die fills the first free cell, cell die
            // returns to the tray. Tapping an empty active cell shows the
            // hint bubble. Drags never reach here — they need slop.
            .pointerInput(slot) {
                detectTapGestures {
                    if (dd.session != null) return@detectTapGestures
                    if (currentDraggable) {
                        currentOnTap()
                    } else if (slot is Slot.Cell && currentActive &&
                        currentDie == null
                    ) {
                        currentOnEmptyTap()
                    }
                }
            },
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(borderModifier)
                .drawWithContent {
                    drawContent()
                    // "+" hint = empty drop target in the active row.
                    if (slot is Slot.Cell && active && die == null &&
                        !highlighted && shownFeedback == null
                    ) {
                        drawDropHint()
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            if (die != null) {
                // requiredSize lets the die overflow a smaller cell box
                // (tray cells sit at 0.87 under the full-size die).
                DieView(
                    die = die,
                    size = dieSize,
                    alpha = alpha,
                    modifier = Modifier.requiredSize(dieSize),
                )
            }
        }
    }
}

@Composable
private fun EnterCell(state: GameState, size: Dp, onIntent: (GameIntent) -> Unit) {
    val enabled = state.canSubmit
    Box(
        modifier = Modifier
            .size(size)
            .testTag("enter")
            .semantics {
                contentDescription =
                    if (enabled) "Submit guess" else "Submit guess, disabled until the row is full"
            }
            .pointerInput(enabled) {
                if (enabled) {
                    detectTapGestures {
                        SoundFx.submit()
                        onIntent(GameIntent.Submit)
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(Res.drawable.enter_key),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = if (enabled) 1f else 0.3f },
        )
    }
}

/** The dealt dice, seated in the standalone deal-row wells. */
@Composable
internal fun DiceRow(
    state: GameState,
    spec: BoardSpec,
    boardW: Dp,
    cell: Dp,
    dd: DragDropState,
    onIntent: (GameIntent) -> Unit,
) {
    val colX = spec.dealColsPx.map { boardW * (it / spec.imageWidthPx) }
    val rowTop = boardW * (spec.dealDieTopPx / spec.imageWidthPx)
    // The die art is a rounded square covering an inscribed 0.87 square;
    // dice-row cells sit fully behind the die, centered on the wells.
    val diceCell = cell * 0.87f
    val inset = (cell - diceCell) / 2

    for (i in 0 until state.level.traySize) {
        val slot = Slot.Tray(i)
        val die = state.tray[i]
        val draggingAway = dd.session?.from == slot
        val highlighted = dd.session?.let { isLegalMove(state, it.from, slot) } == true
        val canDrag = state.status == GameStatus.PLAYING && die != null && !draggingAway

        CellWell(
            slot = slot,
            die = if (draggingAway) null else die,
            cell = diceCell,
            dieSize = cell,
            dd = dd,
            active = state.status == GameStatus.PLAYING,
            alpha = 1f,
            highlighted = highlighted,
            feedback = null,
            showMatchIndicator = true,
            description = "Tray slot ${i + 1}: " +
                (die?.let { "${colourName(it.colour)} ${it.value}" } ?: "empty"),
            draggable = canDrag,
            state = state,
            onIntent = onIntent,
            onTap = { SoundFx.tap(); onIntent(GameIntent.TapMove(slot)) },
            modifier = Modifier.absoluteOffset(
                x = colX[i] + inset,
                y = rowTop + inset,
            ),
        )
    }
}

/** Centered brass "+" drop hint for empty cells on the active row. */
private fun DrawScope.drawDropHint() {
    val arm = size.minDimension * 0.13f
    val t = 1.5.dp.toPx()
    val c = MasterDiceColors.Brass.copy(alpha = 0.55f)
    drawLine(c, Offset(center.x - arm, center.y), Offset(center.x + arm, center.y), t)
    drawLine(c, Offset(center.x, center.y - arm), Offset(center.x, center.y + arm), t)
}