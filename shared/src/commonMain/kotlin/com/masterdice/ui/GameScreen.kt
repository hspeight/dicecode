package com.masterdice.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.absoluteOffset
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
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import com.masterdice.game.Die
import com.masterdice.game.DieColour
import com.masterdice.game.Feedback
import com.masterdice.game.GameIntent
import com.masterdice.game.GameState
import com.masterdice.game.GameStatus
import com.masterdice.game.GUESS_SIZE
import com.masterdice.game.LEVELS
import com.masterdice.game.ROWS
import com.masterdice.game.Slot
import com.masterdice.game.TRAY_SIZE
import com.masterdice.game.gameScore
import com.masterdice.game.isLegalMove
import com.masterdice.platform.debugToolsEnabled
import com.masterdice.resources.Res
import com.masterdice.resources.board_4x10
import com.masterdice.resources.deal_tray
import com.masterdice.resources.enter_key
import androidx.compose.ui.layout.ContentScale
import org.jetbrains.compose.resources.painterResource
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private val WellShape = RoundedCornerShape(10.dp)

// board_4x10.png geometry (pixels of the 2367x5483 source artwork; the
// bundled resource is a 0.6x downscale — identical fractions): square wells
// on a uniform grid — 3 guess columns plus the lighter return-key column.
private const val BOARD_ASPECT = 5483f / 2367f
private const val BOARD_WELL_PX = 500f
private val BOARD_COLS_PX = floatArrayOf(98f, 630f, 1162f, 1769f)
private const val BOARD_ROW_TOP_PX = 98f
private const val BOARD_ROW_PITCH_PX = 531.8f

// deal_tray.png is 2292x697; rendered at boardW*2292/2367 wide, its wells land
// on exactly the same grid as the board's.
private const val TRAY_ASPECT = 697f / 2292f
private val TRAY_COLS_PX = floatArrayOf(98f, 630f, 1162f, 1693f)
private const val TRAY_ROW_TOP_PX = 98f

@Composable
fun GameScreen(
    state: GameState,
    onIntent: (GameIntent) -> Unit,
    onNewGame: () -> Unit,
    bestScore: Int? = null,
    newBest: Boolean = false,
    totalScore: Int = 0,
    modifier: Modifier = Modifier,
) {
    val dd = rememberDragDropState()

    // Rows whose feedback is already revealed. Seeded with the restored guess
    // count so only guesses submitted during this session animate in.
    var revealedRows by remember { mutableIntStateOf(state.guesses.size) }
    var showHelp by remember { mutableStateOf(false) }
    var showDealPicker by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showMatchIndicators by remember { mutableStateOf(false) }
    // Slot.Cell that last received an empty-cell tap; drives the hint bubble.
    var hintCell by remember { mutableStateOf<Slot.Cell?>(null) }
    val reveal = remember { Animatable(1f) }
    LaunchedEffect(hintCell) {
        if (hintCell != null) {
            delay(2200)
            hintCell = null
        }
    }
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
            .drawBehind { drawFeltBackground() }
            .safeDrawingPadding()
            .onGloballyPositioned { dd.container = it },
    ) {
        val maxBoardWidth = if (maxWidth > 480.dp) 480.dp else maxWidth
        val gap = 6.dp
        // Board and tray are fixed-aspect artworks scaled from boardW; its
        // width is bound by whichever is tighter — the column width, or the
        // height budget once the header and spacers are paid for.
        val boardW = minOf(
            maxBoardWidth - 16.dp,
            (maxHeight - 80.dp) / (BOARD_ASPECT + 697f / 2367f),
        ) * 0.81f
        val boardH = boardW * BOARD_ASPECT
        // One cell = one artwork well (500px of the 2367px-wide image).
        val cell = boardW * (BOARD_WELL_PX / 2367f)
        // 4dp of well showing on every side. Grid and tray share one size.
        val dieSize = cell - 8.dp
        // 4th-column cell — sized to the enter key, smaller than the well.
        val enterSize = dieSize * 0.9f

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
                    .padding(vertical = 6.dp),
            ) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BasicText(
                            text = "◆",
                            style = TextStyle(
                                color = MasterDiceColors.BrassBright.copy(alpha = 0.6f),
                                fontSize = 10.sp,
                            ),
                        )
                        Spacer(Modifier.width(8.dp))
                        BasicText(
                            text = "Master Dice",
                            style = TextStyle(
                                color = MasterDiceColors.BrassBright,
                                fontSize = 22.sp,
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.sp,
                            ),
                        )
                        Spacer(Modifier.width(8.dp))
                        BasicText(
                            text = "◆",
                            style = TextStyle(
                                color = MasterDiceColors.BrassBright.copy(alpha = 0.6f),
                                fontSize = 10.sp,
                            ),
                        )
                    }
                    val turn = if (state.status == GameStatus.PLAYING)
                        (state.guesses.size + 1).coerceAtMost(ROWS)
                    else state.guesses.size
                    BasicText(
                        text = "GUESS $turn / $ROWS",
                        style = TextStyle(
                            color = MasterDiceColors.Ivory.copy(alpha = 0.55f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 2.sp,
                        ),
                    )
                    BasicText(
                        text = "SCORE $totalScore",
                        style = TextStyle(
                            color = MasterDiceColors.BrassBright.copy(alpha = 0.7f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 2.sp,
                        ),
                        modifier = Modifier.testTag("runningTotal"),
                    )
                }
                if (debugToolsEnabled) {
                    Row(
                        modifier = Modifier.align(Alignment.CenterStart),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        DebugDealButton(onClick = { showDealPicker = true })
                        DebugUndoButton(
                            enabled = state.guesses.isNotEmpty(),
                            onClick = { onIntent(GameIntent.UndoGuess) },
                        )
                    }
                }
                Row(
                    modifier = Modifier.align(Alignment.CenterEnd),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    SettingsButton(onClick = { showSettings = true })
                    HelpButton(onClick = { showHelp = true })
                }
            }

            Box(modifier = Modifier.size(boardW, boardH)) {
                Image(
                    painter = painterResource(Res.drawable.board_4x10),
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.matchParentSize(),
                )
                // Column 4's lighter wells are baked into the artwork at full
                // well size; repaint them at enter-key size.
                Canvas(Modifier.matchParentSize()) {
                    val cellPx = cell.toPx()
                    val keyPx = enterSize.toPx()
                    val left = size.width * (BOARD_COLS_PX[3] / 2367f)
                    val inset = (cellPx - keyPx) / 2f
                    // Same corner radius as the enter key art (~0.21 of size).
                    val radius = CornerRadius(keyPx * 0.21f)
                    for (i in 0 until ROWS) {
                        val top = size.width *
                            ((BOARD_ROW_TOP_PX + i * BOARD_ROW_PITCH_PX) / 2367f)
                        drawRect(
                            color = MasterDiceColors.EnterWellBg,
                            topLeft = Offset(left, top),
                            size = Size(cellPx, cellPx),
                        )
                        drawRoundRect(
                            color = MasterDiceColors.EnterWell,
                            topLeft = Offset(left + inset, top + inset),
                            size = Size(keyPx, keyPx),
                            cornerRadius = radius,
                        )
                    }
                }
                for (row in ROWS - 1 downTo 0) {
                    GuessRow(
                        state = state,
                        row = row,
                        boardW = boardW,
                        cell = cell,
                        dieSize = dieSize,
                        enterSize = enterSize,
                        dd = dd,
                        onIntent = onIntent,
                        feedbackShown = { col -> feedbackShown(row, col) },
                        showMatchIndicator = showMatchIndicators,
                        onEmptyTap = { slot -> hintCell = slot },
                    )
                }
                if (state.status != GameStatus.PLAYING) {
                    EndPanel(
                        state,
                        onNewGame,
                        Modifier.matchParentSize(),
                        bestScore = bestScore,
                        newBest = newBest,
                        totalScore = totalScore,
                    )
                }
            }

            Spacer(Modifier.height(4.dp))
//            Row(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(end = 4.dp),
//                horizontalArrangement = Arrangement.End,
//            ) {
//                BasicText(
//                    text = "DRAG INTO THE ROW",
//                    style = TextStyle(
//                        color = MasterDiceColors.Ivory.copy(alpha = 0.3f),
//                        fontSize = 9.sp,
//                        letterSpacing = 1.sp,
//                    ),
//                )
//            }
//            Spacer(Modifier.height(4.dp))

            TrayRow(state, boardW, cell, dieSize, dd, onIntent)
        }

        DragOverlay(dd, dieSize)

        // Hint bubble above an empty active cell that was tapped, plus a
        // finger bouncing over the deal tray.
        hintCell?.let { slot ->
            dd.centerOf(slot)?.let { center ->
                HintBubble(
                    anchor = center,
                    aboveBy = cell / 2 + 8.dp,
                    onDismiss = { hintCell = null },
                )
            }
            TrayPointer(dd, visible = true)
        }

        if (showHelp) {
            HelpPanel(
                showMatchIndicators = showMatchIndicators,
                onDismiss = { showHelp = false },
            )
        }

        if (showSettings) {
            SettingsPanel(
                showMatchIndicators = showMatchIndicators,
                onToggle = { showMatchIndicators = it },
                onDismiss = { showSettings = false },
            )
        }

        if (showDealPicker) {
            DebugDealPanel(
                onDeal = { solution, decoy ->
                    onIntent(GameIntent.NewGameWithDeal(solution, decoy))
                    showDealPicker = false
                },
                onDismiss = { showDealPicker = false },
            )
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
private fun SettingsButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(32.dp)
            .testTag("settings")
            .semantics { contentDescription = "Open settings" }
            .clip(RoundedCornerShape(50))
            .background(MasterDiceColors.Brass.copy(alpha = 0.25f))
            .border(1.dp, MasterDiceColors.Brass.copy(alpha = 0.6f), RoundedCornerShape(50))
            .pointerInput(Unit) { detectTapGestures { onClick() } },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(15.dp)) {
            val brass = MasterDiceColors.Brass
            val c = center
            val r = size.minDimension / 2f
            // Eight gear teeth as short radial strokes, then the hub ring.
            for (i in 0 until 8) {
                val a = i * kotlin.math.PI / 4
                drawLine(
                    color = brass,
                    start = Offset(
                        (c.x + kotlin.math.cos(a) * r * 0.72f).toFloat(),
                        (c.y + kotlin.math.sin(a) * r * 0.72f).toFloat(),
                    ),
                    end = Offset(
                        (c.x + kotlin.math.cos(a) * r).toFloat(),
                        (c.y + kotlin.math.sin(a) * r).toFloat(),
                    ),
                    strokeWidth = r * 0.32f,
                    cap = StrokeCap.Round,
                )
            }
            drawCircle(color = brass, radius = r * 0.58f, center = c)
            drawCircle(color = MasterDiceColors.Board, radius = r * 0.3f, center = c)
        }
    }
}

@Composable
private fun DebugDealButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(32.dp)
            .testTag("debugDeal")
            .semantics { contentDescription = "Open debug deal picker" }
            .clip(RoundedCornerShape(50))
            .background(MasterDiceColors.RowPanel)
            .border(1.dp, MasterDiceColors.Ivory.copy(alpha = 0.3f), RoundedCornerShape(50))
            .pointerInput(Unit) { detectTapGestures { onClick() } },
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = "DEV",
            style = TextStyle(
                color = MasterDiceColors.Ivory.copy(alpha = 0.55f),
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
            ),
        )
    }
}

@Composable
private fun DebugUndoButton(enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .testTag("debugUndo")
            .semantics { contentDescription = "Undo last guess (debug)" }
            .clip(RoundedCornerShape(50))
            .background(MasterDiceColors.RowPanel)
            .border(1.dp, MasterDiceColors.Ivory.copy(alpha = 0.3f), RoundedCornerShape(50))
            .graphicsLayer { alpha = if (enabled) 1f else 0.35f }
            .pointerInput(enabled) {
                if (enabled) detectTapGestures { onClick() }
            },
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = "UNDO",
            style = TextStyle(
                color = MasterDiceColors.Ivory.copy(alpha = 0.55f),
                fontSize = 6.5.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
    }
}

/** Debug-only panel: pick the three solution dice (in order) and the decoy. */
@Composable
private fun DebugDealPanel(
    onDeal: (solution: List<Die>, decoy: Die) -> Unit,
    onDismiss: () -> Unit,
) {
    val labels = listOf("SOL 1", "SOL 2", "SOL 3", "DECOY")
    val dice = remember {
        mutableStateListOf(
            Die(DieColour.RED, 1), Die(DieColour.BLUE, 2),
            Die(DieColour.GREEN, 3), Die(DieColour.ORANGE, 4),
        )
    }
    var selected by remember { mutableIntStateOf(0) }
    val distinct = dice.distinct().size == TRAY_SIZE

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("debugDealPanel")
            .background(MasterDiceColors.Board.copy(alpha = 0.82f))
            .pointerInput(Unit) { detectTapGestures { onDismiss() } },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .clip(WellShape)
                .background(MasterDiceColors.Board)
                .border(1.dp, MasterDiceColors.Brass, WellShape)
                .padding(16.dp)
                .pointerInput(Unit) { detectTapGestures { } },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BasicText(
                text = "Fix the deal",
                style = TextStyle(
                    color = MasterDiceColors.Ivory,
                    fontSize = 18.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                ),
            )
            BasicText(
                text = "DEBUG BUILD ONLY",
                style = TextStyle(
                    color = MasterDiceColors.AmberBorder.copy(alpha = 0.8f),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                ),
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                dice.forEachIndexed { index, die ->
                    val isSelected = index == selected
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .testTag("dealSlot-$index")
                            .clip(WellShape)
                            .background(
                                if (isSelected) MasterDiceColors.RowPanel
                                else MasterDiceColors.SubmittedPanel
                            )
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) MasterDiceColors.Brass
                                else MasterDiceColors.SubmittedPanelBorder,
                                shape = WellShape,
                            )
                            .pointerInput(index) { detectTapGestures { selected = index } }
                            .padding(8.dp),
                    ) {
                        DieView(die = die, size = 40.dp)
                        Spacer(Modifier.height(4.dp))
                        BasicText(
                            text = labels[index],
                            style = TextStyle(
                                color = if (isSelected) MasterDiceColors.BrassBright
                                else MasterDiceColors.Ivory.copy(alpha = 0.45f),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                            ),
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                DieColour.entries.forEach { colour ->
                    val isCurrent = dice[selected].colour == colour
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .testTag("dealColour-${colour.name}")
                            .semantics {
                                contentDescription = "Set ${colourName(colour)} colour"
                            }
                            .clip(CircleShape)
                            .background(dieColourSwatch(colour))
                            .then(
                                if (isCurrent) Modifier.border(
                                    2.dp, MasterDiceColors.Ivory, CircleShape
                                ) else Modifier
                            )
                            .pointerInput(colour) {
                                detectTapGestures {
                                    dice[selected] = dice[selected].copy(colour = colour)
                                }
                            },
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                (1..6).forEach { value ->
                    val isCurrent = dice[selected].value == value
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .testTag("dealValue-$value")
                            .semantics { contentDescription = "Set value $value" }
                            .clip(WellShape)
                            .background(
                                if (isCurrent) MasterDiceColors.Brass
                                else MasterDiceColors.RowPanel
                            )
                            .border(
                                1.dp,
                                if (isCurrent) MasterDiceColors.BrassBright
                                else MasterDiceColors.SubmittedPanelBorder,
                                WellShape,
                            )
                            .pointerInput(value) {
                                detectTapGestures {
                                    dice[selected] = dice[selected].copy(value = value)
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        BasicText(
                            text = "$value",
                            style = TextStyle(
                                color = if (isCurrent) MasterDiceColors.Board
                                else MasterDiceColors.Ivory,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            BasicText(
                text = if (distinct) "Tray order: SOL 1, SOL 2, SOL 3, DECOY"
                else "Dice must all be distinct",
                style = TextStyle(
                    color = if (distinct) MasterDiceColors.Ivory.copy(alpha = 0.4f)
                    else MasterDiceColors.AmberBorder,
                    fontSize = 10.sp,
                ),
            )
            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .testTag("dealNow")
                    .semantics { contentDescription = "Start game with this deal" }
                    .clip(WellShape)
                    .background(
                        if (distinct) MasterDiceColors.Brass
                        else MasterDiceColors.RowPanel
                    )
                    .pointerInput(distinct, dice.toList()) {
                        if (distinct) {
                            detectTapGestures {
                                onDeal(dice.take(GUESS_SIZE), dice[GUESS_SIZE])
                            }
                        }
                    }
                    .padding(horizontal = 24.dp, vertical = 8.dp),
            ) {
                BasicText(
                    text = "Deal this hand",
                    style = TextStyle(
                        color = if (distinct) MasterDiceColors.Board
                        else MasterDiceColors.Ivory.copy(alpha = 0.3f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
        }
    }
}

@Composable
private fun HelpPanel(
    showMatchIndicators: Boolean,
    onDismiss: () -> Unit,
) {
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
            RuleText("A hidden sequence of three dice is the solution. The tray holds those three plus one decoy — work out which three belong, and in what order.")
            Spacer(Modifier.height(8.dp))
            RuleText("Drag dice from the tray into the active row — or tap a tray die to fill the first free cell. Tap or drag a placed die to return it to the tray. You have $ROWS guesses.")
            Spacer(Modifier.height(12.dp))
            if (showMatchIndicators) {
                RuleLine("■", MasterDiceColors.GreenBorder, "Correct colour AND value")
                RuleLine("■", MasterDiceColors.AmberBorder, "Correct colour OR value")
                //RuleLine("□", MasterDiceColors.Ivory.copy(alpha = 0.4f), "Neither matches")
            } else {
                //RuleText("Match indicators are off — each submitted row shows a peg for every column:")
                Spacer(Modifier.height(8.dp))
                RuleLine("●", MasterDiceColors.GreenBorder, "Correct colour AND value")
                RuleLine("●", MasterDiceColors.AmberBorder, "Correct colour OR value")
                //RuleLine("●", MasterDiceColors.Ivory.copy(alpha = 0.4f), "Neither matches")
            }
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
private fun SettingsPanel(
    showMatchIndicators: Boolean,
    onToggle: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("settingsPanel")
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
                text = "Settings",
                style = TextStyle(
                    color = MasterDiceColors.Ivory,
                    fontSize = 20.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                ),
            )
            Spacer(Modifier.height(14.dp))
            if (debugToolsEnabled) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        BasicText(
                            text = "Colour match indicators",
                            style = TextStyle(
                                color = MasterDiceColors.Ivory.copy(alpha = 0.85f),
                                fontSize = 14.sp,
                            ),
                        )
                        BasicText(
                            text = "DEBUG BUILD ONLY",
                            style = TextStyle(
                                color = MasterDiceColors.AmberBorder.copy(alpha = 0.8f),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp,
                            ),
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    // Pill switch: brass track + knob when on, muted when off.
                    Box(
                        modifier = Modifier
                            .size(width = 44.dp, height = 24.dp)
                            .testTag("toggleMatchIndicators")
                            .semantics {
                                contentDescription =
                                    "Colour match indicators, " +
                                        if (showMatchIndicators) "on" else "off"
                            }
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (showMatchIndicators) MasterDiceColors.Brass
                                else MasterDiceColors.RowPanel
                            )
                            .border(
                                1.dp,
                                if (showMatchIndicators) MasterDiceColors.BrassBright
                                else MasterDiceColors.SubmittedPanelBorder,
                                RoundedCornerShape(50),
                            )
                            .pointerInput(showMatchIndicators) {
                                detectTapGestures { onToggle(!showMatchIndicators) }
                            }
                            .padding(3.dp),
                        contentAlignment = if (showMatchIndicators) Alignment.CenterEnd
                        else Alignment.CenterStart,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .background(
                                    if (showMatchIndicators) MasterDiceColors.Board
                                    else MasterDiceColors.Ivory.copy(alpha = 0.4f),
                                    CircleShape,
                                ),
                        )
                    }
                }
            } else {
                BasicText(
                    text = "No settings available.",
                    style = TextStyle(
                        color = MasterDiceColors.Ivory.copy(alpha = 0.5f),
                        fontSize = 14.sp,
                    ),
                )
            }
            Spacer(Modifier.height(18.dp))
            BasicText(
                text = "Done",
                style = TextStyle(
                    color = MasterDiceColors.Board,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                ),
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .testTag("settingsDone")
                    .semantics { contentDescription = "Close settings" }
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
private fun RuleLine(marker: String, color: Color, text: String) {
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

/** One guess row overlaid on the board artwork's wells (row 0 = bottom). */
@Composable
private fun BoxScope.GuessRow(
    state: GameState,
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
    val colX = BOARD_COLS_PX.map { boardW * (it / 2367f) }
    val rowTop =
        boardW * ((BOARD_ROW_TOP_PX + (ROWS - 1 - row) * BOARD_ROW_PITCH_PX) / 2367f)

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
            alpha = 1f,
            highlighted = highlighted,
            feedback = if (showFeedback) feedback else null,
            showMatchIndicator = showMatchIndicator,
            description = cellDescription(state, row, col, die, feedback),
            draggable = isActive && die != null && !draggingAway,
            state = state,
            onIntent = onIntent,
            onTap = { onIntent(GameIntent.TapMove(slot)) },
            onEmptyTap = { onEmptyTap(slot) },
            modifier = Modifier.absoluteOffset(x = colX[col], y = rowTop),
        )
    }

    // Column 4: enter cell on the active row, feedback pegs on submitted rows.
    // The cell is sized to the enter key and centered in the artwork well.
    Box(
        modifier = Modifier
            .absoluteOffset(
                x = colX[3] + (cell - enterSize) / 2,
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
                        .semantics {
                            contentDescription =
                                "Row ${row + 1}: $greens exact, $ambers partial matches"
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(5.dp))
                            .background(
                                MasterDiceColors.Board.copy(alpha = 0.55f),
                            )
                            .border(
                                1.dp,
                                MasterDiceColors.SubmittedPanelBorder,
                                RoundedCornerShape(5.dp),
                            )
                            .padding(4.dp),
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

    // The debug settings toggle hides the border/glow match indicators only;
    // `feedback` still drives semantics via `description`.
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
                DieView(die = die, size = dieSize, alpha = alpha)
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
                    detectTapGestures { onIntent(GameIntent.Submit) }
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

/** Deal-tray artwork with the four tray cells overlaid on its wells. */
@Composable
private fun TrayRow(
    state: GameState,
    boardW: Dp,
    cell: Dp,
    dieSize: Dp,
    dd: DragDropState,
    onIntent: (GameIntent) -> Unit,
) {
    val trayW = boardW * (2292f / 2367f)
    val colX = TRAY_COLS_PX.map { trayW * (it / 2292f) }
    val rowTop = trayW * (TRAY_ROW_TOP_PX / 2292f)

    Box(modifier = Modifier.size(trayW, trayW * TRAY_ASPECT)) {
        Image(
            painter = painterResource(Res.drawable.deal_tray),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.matchParentSize(),
        )
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
                alpha = 1f,
                highlighted = highlighted,
                feedback = null,
                showMatchIndicator = true,
                description = "Tray slot ${i + 1}: " +
                    (die?.let { "${colourName(it.colour)} ${it.value}" } ?: "empty"),
                draggable = canDrag,
                state = state,
                onIntent = onIntent,
                onTap = { onIntent(GameIntent.TapMove(slot)) },
                modifier = Modifier.absoluteOffset(x = colX[i], y = rowTop),
            )
        }
    }
}

@Composable
private fun EndPanel(
    state: GameState,
    onNewGame: () -> Unit,
    modifier: Modifier = Modifier,
    bestScore: Int? = null,
    newBest: Boolean = false,
    totalScore: Int = 0,
) {
    val level = LEVELS[state.levelIndex]
    val score = gameScore(state, level)
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
            Spacer(Modifier.height(4.dp))
            BasicText(
                text = "Par ${level.par} · always solvable in ${level.bound ?: level.traySize}",
                style = TextStyle(
                    color = MasterDiceColors.Ivory.copy(alpha = 0.45f),
                    fontSize = 11.sp,
                    letterSpacing = 0.5.sp,
                ),
            )
            Spacer(Modifier.height(8.dp))
            if (state.scored && score != null) {
                BasicText(
                    text = "+$score pts",
                    style = TextStyle(
                        color = MasterDiceColors.BrassBright,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    modifier = Modifier
                        .testTag("endPanelScore")
                        .semantics { contentDescription = "Score: $score points" },
                )
                BasicText(
                    text = if (newBest) "NEW BEST"
                    else bestScore?.let { "BEST $it" } ?: "",
                    style = TextStyle(
                        color = if (newBest) MasterDiceColors.BrassBright.copy(alpha = 0.8f)
                        else MasterDiceColors.Ivory.copy(alpha = 0.45f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                    ),
                    modifier = Modifier.padding(top = 2.dp),
                )
                BasicText(
                    text = "TOTAL $totalScore",
                    style = TextStyle(
                        color = MasterDiceColors.Ivory.copy(alpha = 0.35f),
                        fontSize = 10.sp,
                        letterSpacing = 1.5.sp,
                    ),
                    modifier = Modifier.padding(top = 4.dp),
                )
            } else if (!state.scored) {
                BasicText(
                    text = "Unscored game",
                    style = TextStyle(
                        color = MasterDiceColors.Ivory.copy(alpha = 0.4f),
                        fontSize = 11.sp,
                        fontStyle = FontStyle.Italic,
                    ),
                    modifier = Modifier.testTag("endPanelScore"),
                )
            }
            if (state.status == GameStatus.LOST) {
                Spacer(Modifier.height(4.dp))
                BasicText(
                    text = "The solution was",
                    style = TextStyle(
                        color = MasterDiceColors.Ivory.copy(alpha = 0.6f),
                        fontSize = 13.sp,
                    ),
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.semantics {
                    contentDescription = "Solution: " + state.solution.joinToString(", ") {
                        "${colourName(it.colour)} ${it.value}"
                    }
                },
            ) {
                state.solution.forEachIndexed { col, die ->
                    DieView(
                        die = die,
                        size = 44.dp,
                        modifier = Modifier.testTag("solution-$col"),
                    )
                }
            }
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

/** Felt surface: radial light falloff plus a heavy edge vignette. */
private fun DrawScope.drawFeltBackground() {
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(
                MasterDiceColors.FeltLight,
                MasterDiceColors.FeltMid,
                MasterDiceColors.FeltDark,
            ),
            center = center.copy(y = size.height * 0.3f),
            radius = size.maxDimension * 0.85f,
        ),
    )
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.45f)),
            center = center.copy(y = size.height * 0.4f),
            radius = size.maxDimension,
        ),
    )
}

/** Transient bubble anchored above a tapped empty cell, telling the player
 * how to fill it. `anchor` is the cell center in root coordinates. */
@Composable
private fun HintBubble(anchor: Offset, aboveBy: Dp, onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .testTag("cellHint")
            .semantics { contentDescription = "Drag or tap a tray die to fill this cell" }
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                layout(placeable.width, placeable.height) {
                    placeable.place(
                        x = (anchor.x - placeable.width / 2f)
                            .roundToInt()
                            .coerceIn(0, (constraints.maxWidth - placeable.width).coerceAtLeast(0)),
                        y = (anchor.y - aboveBy.toPx() - placeable.height)
                            .roundToInt()
                            .coerceAtLeast(0),
                    )
                }
            }
            .clip(RoundedCornerShape(8.dp))
            .background(MasterDiceColors.Board)
            .border(1.dp, MasterDiceColors.Brass, RoundedCornerShape(8.dp))
            .pointerInput(Unit) { detectTapGestures { onDismiss() } }
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        BasicText(
            text = "Drag or tap a tray die to fill this cell",
            style = TextStyle(
                color = MasterDiceColors.Ivory,
                fontSize = 11.sp,
            ),
        )
    }
}

/** Pointing hand bobbing beside the deal tray, shown while the empty-cell
 * hint is up. Decorative — the message lives on the bubble. */
@Composable
private fun TrayPointer(dd: DragDropState, visible: Boolean) {
    val left = dd.boundsOf(Slot.Tray(0))
    if (left == null) return
    // Tip lands at the tray's left edge, mid-height — the hand sits to the
    // left of the tray pointing right at it.
    val tip = Offset(left.left, left.center.y)

    val appear = remember { Animatable(0f) }
    val bob = remember { Animatable(0f) }
    LaunchedEffect(visible) {
        if (visible) {
            appear.snapTo(0f)
            appear.animateTo(1f, tween(300))
            // Finite dips — an infinite loop would keep the effect clock
            // running past the hint's own auto-dismiss (and block tests'
            // waitForIdle). Roughly matches the 2.2s hint lifetime.
            repeat(3) {
                bob.animateTo(1f, tween(300))
                bob.animateTo(0f, tween(300))
            }
        } else {
            appear.snapTo(0f)
            bob.snapTo(0f)
        }
    }

    Box(
        modifier = Modifier
            .testTag("trayPointer")
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                layout(placeable.width, placeable.height) {
                    placeable.place(
                        // Glyph right edge = fingertip; the bob nudges it
                        // toward the tray.
                        x = (tip.x - placeable.width - 6.dp.toPx() +
                            bob.value * 5.dp.toPx()).roundToInt()
                            .coerceAtLeast(0),
                        y = (tip.y - placeable.height / 2f).roundToInt(),
                    )
                }
            }
            .graphicsLayer {
                alpha = appear.value
                val s = 0.6f + 0.4f * appear.value
                scaleX = s
                scaleY = s
            },
    ) {
        BasicText(
            text = "👉",
            style = TextStyle(fontSize = 38.sp),
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


