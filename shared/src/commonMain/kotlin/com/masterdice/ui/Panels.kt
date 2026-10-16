package com.masterdice.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.gestures.detectTapGestures
import com.masterdice.game.Die
import com.masterdice.game.DieColour
import com.masterdice.game.GameState
import com.masterdice.game.GameStatus
import com.masterdice.game.GUESS_SIZE
import com.masterdice.game.LEVELS
import com.masterdice.game.ROWS
import com.masterdice.game.TRAY_SIZE
import com.masterdice.game.gameScore
import com.masterdice.platform.debugToolsEnabled

@Composable
internal fun DebugDealPanel(
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
internal fun HelpPanel(
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
internal fun SettingsPanel(
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
                .width(320.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(MasterDiceColors.FeltMid, MasterDiceColors.FeltDark),
                    ),
                )
                .border(1.5.dp, MasterDiceColors.Gold, RoundedCornerShape(16.dp))
                .padding(18.dp)
                // Swallow taps on the card itself so it doesn't dismiss.
                .pointerInput(Unit) { detectTapGestures { } },
        ) {
            SettingsRow(
                label = "SOUND FX",
                tag = "toggleSound",
                checked = soundEnabled,
                onToggle = onToggleSound,
            ) {
                drawSpeaker(MasterDiceColors.Gold)
            }
            if (debugToolsEnabled) {
                Spacer(Modifier.height(14.dp))
                SettingsRow(
                    label = "MATCH TINTS",
                    tag = "toggleMatchIndicators",
                    checked = showMatchIndicators,
                    onToggle = onToggle,
                ) {
                    val r = size.minDimension / 2f
                    drawCircle(MasterDiceColors.GreenBorder, r * 0.5f, Offset(r * 0.62f, r))
                    drawCircle(MasterDiceColors.AmberBorder, r * 0.5f, Offset(r * 1.38f, r))
                }
            }
            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("settingsDone")
                    .semantics { contentDescription = "Done" }
                    .clip(RoundedCornerShape(50))
                    .background(goldPillBrush())
                    .border(2.dp, MasterDiceColors.GoldLow, RoundedCornerShape(50))
                    .pointerInput(Unit) { detectTapGestures { onDismiss() } },
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    text = "DONE",
                    style = TextStyle(
                        color = MasterDiceColors.InkBrown,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                    ),
                )
            }
        }
    }
}

/** Modal warning shown when the filled row repeats an earlier guess. */
@Composable
internal fun RepeatGuessDialog(guessNumber: Int, onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("repeatWarning")
            .background(MasterDiceColors.Board.copy(alpha = 0.82f))
            .pointerInput(Unit) { detectTapGestures { onDismiss() } },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .width(230.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(MasterDiceColors.FeltMid, MasterDiceColors.FeltDark),
                    ),
                )
                .border(1.5.dp, MasterDiceColors.Gold, RoundedCornerShape(16.dp))
                .padding(12.dp)
                .pointerInput(Unit) { detectTapGestures { } },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Canvas(Modifier.size(30.dp)) {
                val w = size.width
                val h = size.height
                val tri = Path().apply {
                    moveTo(w * 0.5f, h * 0.06f)
                    lineTo(w * 0.94f, h * 0.86f)
                    lineTo(w * 0.06f, h * 0.86f)
                    close()
                }
                drawPath(tri, MasterDiceColors.AmberBorder)
                drawLine(
                    MasterDiceColors.InkBrown,
                    Offset(w * 0.5f, h * 0.34f),
                    Offset(w * 0.5f, h * 0.62f),
                    strokeWidth = w * 0.08f,
                    cap = StrokeCap.Round,
                )
                drawCircle(MasterDiceColors.InkBrown, w * 0.045f, Offset(w * 0.5f, h * 0.74f))
            }
            Spacer(Modifier.height(8.dp))
            BasicText(
                text = "REPEATED GUESS",
                style = TextStyle(
                    color = MasterDiceColors.AmberBorder,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                ),
            )
            Spacer(Modifier.height(6.dp))
            BasicText(
                text = "This row is the same as guess $guessNumber.",
                style = TextStyle(
                    color = MasterDiceColors.Ivory.copy(alpha = 0.85f),
                    fontSize = 12.sp,
                ),
            )
            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .testTag("repeatWarningDismiss")
                    .semantics { contentDescription = "OK" }
                    .clip(RoundedCornerShape(50))
                    .background(goldPillBrush())
                    .border(1.5.dp, MasterDiceColors.GoldLow, RoundedCornerShape(50))
                    .pointerInput(Unit) { detectTapGestures { onDismiss() } },
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    text = "OK",
                    style = TextStyle(
                        color = MasterDiceColors.InkBrown,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                    ),
                )
            }
        }
    }
}

/** Recessed settings row: icon tile, gold label, segmented ON/OFF toggle. */
@Composable
private fun SettingsRow(
    label: String,
    tag: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
    icon: DrawScope.() -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MasterDiceColors.FeltDark)
            .border(1.dp, MasterDiceColors.Gold.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(Modifier.size(24.dp)) { icon() }
        Spacer(Modifier.width(10.dp))
        BasicText(
            text = label,
            modifier = Modifier.weight(1f),
            style = TextStyle(
                color = MasterDiceColors.GoldHi,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
            ),
        )
        // Segmented ON/OFF toggle — gold segment is the active one.
        Row(
            modifier = Modifier
                .testTag(tag)
                .semantics {
                    contentDescription = "$label, " + if (checked) "on" else "off"
                }
                .clip(RoundedCornerShape(50))
                .background(MasterDiceColors.Board)
                .border(1.dp, MasterDiceColors.Gold.copy(alpha = 0.4f), RoundedCornerShape(50)),
        ) {
            for (on in listOf(false, true)) {
                val active = checked == on
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (active) goldPillBrush() else Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Transparent),
                        ))
                        .pointerInput(on) {
                            detectTapGestures { if (checked != on) onToggle(on) }
                        }
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                ) {
                    BasicText(
                        text = if (on) "ON" else "OFF",
                        style = TextStyle(
                            color = if (active) MasterDiceColors.InkBrown
                            else MasterDiceColors.Gold.copy(alpha = 0.6f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                        ),
                    )
                }
            }
        }
    }
}

/** Speaker glyph for the SOUND FX row: cone plus two sound arcs. */
private fun DrawScope.drawSpeaker(color: Color) {
    val w = size.width
    val h = size.height
    val cone = Path().apply {
        moveTo(w * 0.12f, h * 0.38f)
        lineTo(w * 0.34f, h * 0.38f)
        lineTo(w * 0.58f, h * 0.16f)
        lineTo(w * 0.58f, h * 0.84f)
        lineTo(w * 0.34f, h * 0.62f)
        lineTo(w * 0.12f, h * 0.62f)
        close()
    }
    drawPath(cone, color)
    drawArc(
        color = color,
        startAngle = -60f,
        sweepAngle = 120f,
        useCenter = false,
        topLeft = Offset(w * 0.52f, h * 0.28f),
        size = Size(w * 0.42f, h * 0.44f),
        style = Stroke(width = w * 0.11f, cap = StrokeCap.Round),
    )
    drawArc(
        color = color,
        startAngle = -60f,
        sweepAngle = 120f,
        useCenter = false,
        topLeft = Offset(w * 0.62f, h * 0.14f),
        size = Size(w * 0.72f, h * 0.72f),
        style = Stroke(width = w * 0.11f, cap = StrokeCap.Round),
    )
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
internal fun EndPanel(
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
