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
import androidx.compose.foundation.layout.requiredSize
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.masterdice.platform.SoundFx
import com.masterdice.platform.debugToolsEnabled
import com.masterdice.resources.Res
import com.masterdice.resources.board_4x10
import com.masterdice.resources.deal_row
import com.masterdice.resources.enter_key
import com.masterdice.resources.pointing_hand
import androidx.compose.ui.layout.ContentScale
import org.jetbrains.compose.resources.painterResource
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private val WellShape = RoundedCornerShape(10.dp)

// board_4x10.png geometry (pixels of the 2400x5482 source artwork; the
// bundled resource is a 0.6x downscale — identical fractions): 10 rows of
// 3 square wells plus a continuous recessed peg track.
private const val BOARD_ASPECT = 5482f / 2400f
private const val BOARD_WELL_PX = 500f
private val BOARD_COLS_PX = floatArrayOf(98f, 630f, 1162f)
private const val BOARD_TRACK_CX_PX = 2014f
private const val BOARD_ROW_TOP_PX = 98f
private const val BOARD_ROW_PITCH_PX = 531.8f

// deal_row.png: standalone 2400x580 asset rendered below the board —
// a brass-bordered strip holding four dice wells evenly spaced across
// its width (80px margins and gaps between the 500px dice).
private const val DEAL_ASPECT = 580f / 2400f
private const val DEAL_DIE_TOP_PX = 40f
private val BOARD_DICE_COLS_PX = floatArrayOf(80f, 660f, 1240f, 1820f)

@Composable
fun GameScreen(
    state: GameState,
    onIntent: (GameIntent) -> Unit,
    onNewGame: () -> Unit,
    bestScore: Int? = null,
    newBest: Boolean = false,
    totalScore: Int = 0,
    modifier: Modifier = Modifier,
    soundEnabled: Boolean = SoundFx.enabled,
    onToggleSound: (Boolean) -> Unit = {
        SoundFx.enabled = it
        SoundFx.tap()
    },
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
        // The board is a fixed-aspect artwork scaled from boardW; its width
        // is bound by whichever is tighter — the column width, or the height
        // budget once the header, deal row, and spacers are paid for.
        val boardW = minOf(
            maxBoardWidth - 16.dp,
            (maxHeight - 80.dp - gap) / (BOARD_ASPECT + DEAL_ASPECT),
        ) * 0.81f
        val boardH = boardW * BOARD_ASPECT
        // One cell = one artwork well (500px of the 2400px-wide image);
        // dice fill their wells edge to edge.
        val cell = boardW * (BOARD_WELL_PX / 2400f)
        val dieSize = cell
        // 4th-column cell — sized to the enter key, centered on the peg track.
        val enterSize = dieSize * 0.9f

        Column(
            modifier = Modifier
                .width(maxBoardWidth)
                .align(Alignment.Center)
                .padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                UndoPill(
                    enabled = state.guesses.isNotEmpty(),
                    onClick = { onIntent(GameIntent.UndoGuess) },
                )
                if (debugToolsEnabled) {
                    DebugDealButton(onClick = { showDealPicker = true })
                }
                Spacer(Modifier.weight(1f))
                ScorePill(totalScore)
                SettingsButton(onClick = { showSettings = true })
                HelpButton(onClick = { showHelp = true })
            }

            Box(modifier = Modifier.size(boardW, boardH)) {
                Image(
                    painter = painterResource(Res.drawable.board_4x10),
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.matchParentSize(),
                )
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

            // Standalone deal row: a brass-bordered strip holding the
            // four dealt dice, detached below the board.
            Spacer(Modifier.height(gap))
            Box(modifier = Modifier.size(boardW, boardW * DEAL_ASPECT)) {
                Image(
                    painter = painterResource(Res.drawable.deal_row),
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.matchParentSize(),
                )
                DiceRow(state, boardW, cell, dd, onIntent)
            }
        }

        DragOverlay(dd, dieSize)

        // Hint bubble above an empty active cell that was tapped, plus a
        // finger bouncing over the deal row.
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
                soundEnabled = soundEnabled,
                onToggleSound = onToggleSound,
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

/** Gold circular header button with an ink glyph inside. */
@Composable
private fun GoldIconButton(
    tag: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .size(34.dp)
            .testTag(tag)
            .semantics { contentDescription = description }
            .clip(CircleShape)
            .background(goldPillBrush())
            .border(1.5.dp, MasterDiceColors.GoldLow, CircleShape)
            .pointerInput(Unit) { detectTapGestures { onClick() } },
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
private fun HelpButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    GoldIconButton(
        tag = "help",
        description = "Show game rules",
        onClick = onClick,
        modifier = modifier,
    ) {
        BasicText(
            text = "?",
            style = TextStyle(
                color = MasterDiceColors.InkBrown,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
    }
}

@Composable
private fun SettingsButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    GoldIconButton(
        tag = "settings",
        description = "Open settings",
        onClick = onClick,
        modifier = modifier,
    ) {
        Canvas(Modifier.size(15.dp)) {
            val ink = MasterDiceColors.InkBrown
            val c = center
            val r = size.minDimension / 2f
            // Eight gear teeth as short radial strokes, then the hub ring.
            for (i in 0 until 8) {
                val a = i * kotlin.math.PI / 4
                drawLine(
                    color = ink,
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
            drawCircle(color = ink, radius = r * 0.58f, center = c)
            drawCircle(color = MasterDiceColors.Gold, radius = r * 0.3f, center = c)
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

/** Champagne-gold pill used by the header controls. */
private fun goldPillBrush() = Brush.verticalGradient(
    listOf(MasterDiceColors.GoldHi, MasterDiceColors.Gold, MasterDiceColors.GoldLow),
)

/** Gold UNDO pill: curved-arrow glyph + label, like the reference header. */
@Composable
private fun UndoPill(enabled: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .height(34.dp)
            .testTag("undo")
            .semantics { contentDescription = "Undo last guess" }
            .clip(RoundedCornerShape(50))
            .background(goldPillBrush())
            .border(1.5.dp, MasterDiceColors.GoldLow, RoundedCornerShape(50))
            .graphicsLayer { alpha = if (enabled) 1f else 0.4f }
            .pointerInput(enabled) {
                if (enabled) detectTapGestures { onClick() }
            }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(Modifier.size(14.dp)) {
            val ink = MasterDiceColors.InkBrown
            val r = size.minDimension / 2f
            val ring = r * 0.72f
            // Arc from just below the head, sweeping clockwise around the top.
            drawArc(
                color = ink,
                startAngle = 175f,
                sweepAngle = 290f,
                useCenter = false,
                topLeft = Offset(r - ring, r - ring),
                size = Size(ring * 2, ring * 2),
                style = Stroke(width = r * 0.3f, cap = StrokeCap.Round),
            )
            // Arrowhead at the arc start (left side, ~9 o'clock).
            val a = kotlin.math.PI * 175.0 / 180.0
            val tipX = r + ring * kotlin.math.cos(a).toFloat()
            val tipY = r + ring * kotlin.math.sin(a).toFloat()
            val tx = kotlin.math.sin(a).toFloat()
            val ty = -kotlin.math.cos(a).toFloat()
            val nx = kotlin.math.cos(a).toFloat()
            val ny = kotlin.math.sin(a).toFloat()
            val head = Path().apply {
                moveTo(tipX + tx * r * 0.55f, tipY + ty * r * 0.55f)
                lineTo(
                    tipX - tx * r * 0.35f + nx * r * 0.5f,
                    tipY - ty * r * 0.35f + ny * r * 0.5f,
                )
                lineTo(
                    tipX - tx * r * 0.35f - nx * r * 0.5f,
                    tipY - ty * r * 0.35f - ny * r * 0.5f,
                )
                close()
            }
            drawPath(head, ink)
        }
        Spacer(Modifier.width(5.dp))
        BasicText(
            text = "UNDO",
            style = TextStyle(
                color = MasterDiceColors.InkBrown,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
            ),
        )
    }
}

/** Purple SCORE pill with a small crown, like the reference header. */
@Composable
private fun ScorePill(score: Int) {
    Row(
        modifier = Modifier
            .height(34.dp)
            .testTag("runningTotal")
            .semantics { contentDescription = "Score $score" }
            .clip(RoundedCornerShape(50))
            .background(
                Brush.verticalGradient(
                    listOf(MasterDiceColors.ScorePurple, MasterDiceColors.ScorePurpleDeep),
                ),
            )
            .border(1.5.dp, MasterDiceColors.BrassBright, RoundedCornerShape(50))
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(Modifier.size(13.dp)) {
            val w = size.width
            val h = size.height
            val crown = Path().apply {
                moveTo(w * 0.08f, h * 0.82f)
                lineTo(w * 0.92f, h * 0.82f)
                lineTo(w * 0.88f, h * 0.30f)
                lineTo(w * 0.68f, h * 0.54f)
                lineTo(w * 0.50f, h * 0.16f)
                lineTo(w * 0.32f, h * 0.54f)
                lineTo(w * 0.12f, h * 0.30f)
                close()
            }
            drawPath(crown, MasterDiceColors.BrassBright)
        }
        Spacer(Modifier.width(6.dp))
        Column {
            BasicText(
                text = "SCORE",
                style = TextStyle(
                    color = MasterDiceColors.GoldHi.copy(alpha = 0.85f),
                    fontSize = 6.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.5.sp,
                ),
            )
            BasicText(
                text = score.toString()
                    .reversed().chunked(3).joinToString(",").reversed(),
                style = TextStyle(
                    color = MasterDiceColors.GoldHi,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
        }
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
            RuleText("Drag dice from the bottom row into the active row — or tap a die to fill the first free cell. Tap or drag a placed die to return it to the bottom row. You have $ROWS guesses.")
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
    soundEnabled: Boolean,
    onToggleSound: (Boolean) -> Unit,
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
            SettingsRow(
                label = "Sound effects",
                tag = "toggleSound",
                checked = soundEnabled,
                onToggle = onToggleSound,
            )
            if (debugToolsEnabled) {
                Spacer(Modifier.height(12.dp))
                SettingsRow(
                    label = "Colour match indicators",
                    tag = "toggleMatchIndicators",
                    checked = showMatchIndicators,
                    onToggle = onToggle,
                    badge = "DEBUG BUILD ONLY",
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

/** A labelled settings row with a pill switch. */
@Composable
private fun SettingsRow(
    label: String,
    tag: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
    badge: String? = null,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            BasicText(
                text = label,
                style = TextStyle(
                    color = MasterDiceColors.Ivory.copy(alpha = 0.85f),
                    fontSize = 14.sp,
                ),
            )
            if (badge != null) {
                BasicText(
                    text = badge,
                    style = TextStyle(
                        color = MasterDiceColors.AmberBorder.copy(alpha = 0.8f),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                    ),
                )
            }
        }
        Spacer(Modifier.width(16.dp))
        // Pill switch: brass track + knob when on, muted when off.
        Box(
            modifier = Modifier
                .size(width = 44.dp, height = 24.dp)
                .testTag(tag)
                .semantics {
                    contentDescription = "$label, " + if (checked) "on" else "off"
                }
                .clip(RoundedCornerShape(50))
                .background(
                    if (checked) MasterDiceColors.Brass else MasterDiceColors.RowPanel,
                )
                .border(
                    1.dp,
                    if (checked) MasterDiceColors.BrassBright
                    else MasterDiceColors.SubmittedPanelBorder,
                    RoundedCornerShape(50),
                )
                .pointerInput(checked) {
                    detectTapGestures { onToggle(!checked) }
                }
                .padding(3.dp),
            contentAlignment = if (checked) Alignment.CenterEnd
            else Alignment.CenterStart,
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .background(
                        if (checked) MasterDiceColors.Board
                        else MasterDiceColors.Ivory.copy(alpha = 0.4f),
                        CircleShape,
                    ),
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
    val colX = BOARD_COLS_PX.map { boardW * (it / 2400f) }
    val rowTop =
        boardW * ((BOARD_ROW_TOP_PX + (ROWS - 1 - row) * BOARD_ROW_PITCH_PX) / 2400f)
    // Center-x of the baked peg track that replaces the 4th well column.
    val trackCx = boardW * (BOARD_TRACK_CX_PX / 2400f)

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

/** The four dealt dice, seated in the standalone deal-row wells. */
@Composable
private fun DiceRow(
    state: GameState,
    boardW: Dp,
    cell: Dp,
    dd: DragDropState,
    onIntent: (GameIntent) -> Unit,
) {
    val colX = BOARD_DICE_COLS_PX.map { boardW * (it / 2400f) }
    val rowTop = boardW * (DEAL_DIE_TOP_PX / 2400f)
    // The die art is a rounded square covering an inscribed 0.87 square;
    // dice-row cells sit fully behind the die, centered on the wells.
    val diceCell = cell * 0.87f
    val inset = (cell - diceCell) / 2

    for (i in 0 until TRAY_SIZE) {
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
            .semantics { contentDescription = "Drag or tap a die from the bottom row to fill this cell" }
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
            text = "Drag or tap a die from the bottom row to fill this cell",
            style = TextStyle(
                color = MasterDiceColors.Ivory,
                fontSize = 11.sp,
            ),
        )
    }
}

/** Pointing hand bobbing beside the bottom dice row, shown while the
 * empty-cell hint is up. Decorative — the message lives on the bubble. */
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
        Image(
            painter = painterResource(Res.drawable.pointing_hand),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.height(44.dp),
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


