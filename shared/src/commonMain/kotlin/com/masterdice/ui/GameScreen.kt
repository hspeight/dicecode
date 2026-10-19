package com.masterdice.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.BasicText
import com.masterdice.BUILD_VERSION
import com.masterdice.game.Die
import com.masterdice.game.GameIntent
import com.masterdice.game.GameState
import com.masterdice.game.GameStatus
import com.masterdice.game.ROWS
import com.masterdice.game.TwoDecoyPuzzleLibrary
import com.masterdice.game.Slot
import com.masterdice.game.duplicateGuessIndex
import com.masterdice.platform.SoundFx
import com.masterdice.platform.debugToolsEnabled
import com.masterdice.resources.Res
import com.masterdice.resources.board_4x10
import com.masterdice.resources.board_l1
import com.masterdice.resources.deal_l1
import com.masterdice.resources.deal_row
import androidx.compose.ui.layout.ContentScale
import org.jetbrains.compose.resources.painterResource
import kotlin.random.Random
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun GameScreen(
    state: GameState,
    onIntent: (GameIntent) -> Unit,
    onNewGame: () -> Unit,
    modifier: Modifier = Modifier,
    bestScore: Int? = null,
    newBest: Boolean = false,
    totalScore: Int = 0,
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
    // The row contents last dismissed from the repeated-guess warning.
    var dismissedDup by remember { mutableStateOf<List<Die?>?>(null) }
    val reveal = remember { Animatable(1f) }
    LaunchedEffect(hintCell) {
        if (hintCell != null) {
            delay(2200.milliseconds)
            hintCell = null
        }
    }
    LaunchedEffect(state.guesses.size) {
        if (state.guesses.size > revealedRows) {
            reveal.snapTo(0f)
            reveal.animateTo(1f, tween(state.level.cols * 150))
            revealedRows = state.guesses.size
        }
    }

    fun feedbackShown(row: Int, col: Int): Boolean =
        row < revealedRows ||
            (row == state.guesses.lastIndex && col < reveal.value * state.level.cols)

    // A full active row identical to a submitted guess earns a warning.
    // Dismissed once per arrangement; re-arms if the row changes.
    val dupGuessIndex = duplicateGuessIndex(state)
    val showDupWarning = dupGuessIndex >= 0 && state.activeCells != dismissedDup

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MasterDiceColors.Board)
            .drawBehind { drawFeltBackground() }
            .safeDrawingPadding()
            .onGloballyPositioned { dd.container = it },
    ) {
        val spec = boardSpec(state.levelIndex)
        val maxBoardWidth = if (maxWidth > 480.dp) 480.dp else maxWidth
        val gap = 24.dp
        // The board is a fixed-aspect artwork scaled from boardW; its width
        // is bound by whichever is tighter — the column width, or the height
        // budget once the header rows, deal row, and spacers are paid for.
        val boardW = minOf(
            maxBoardWidth - 16.dp,
            (maxHeight - 144.dp - gap) / (spec.boardAspect + spec.dealAspect),
        ) * 0.81f
        val boardH = boardW * spec.boardAspect
        // One cell = one artwork well scaled by the image width; dice fill
        // their wells edge to edge.
        val cell = boardW * (spec.wellPx / spec.imageWidthPx)
        // Peg-column cell — sized to the enter key, centered on the track.
        val enterSize = cell * 0.9f

        Column(
            modifier = Modifier
                .width(maxBoardWidth)
                .align(Alignment.Center)
                .padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Score badge: large and centered on its own row.
            ScorePill(totalScore)

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
                SettingsButton(onClick = { showSettings = true })
                HelpButton(onClick = { showHelp = true })
            }

            Box(modifier = Modifier.size(boardW, boardH)) {
                Image(
                    painter = painterResource(
                        if (state.levelIndex == 1) Res.drawable.board_l1
                        else Res.drawable.board_4x10,
                    ),
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.matchParentSize(),
                )
                for (row in ROWS - 1 downTo 0) {
                    GuessRow(
                        state = state,
                        spec = spec,
                        row = row,
                        boardW = boardW,
                        cell = cell,
                        dieSize = cell,
                        enterSize = enterSize,
                        dd = dd,
                        onIntent = onIntent,
                        feedbackShown = { col -> feedbackShown(row, col) },
                        showMatchIndicator = debugToolsEnabled && showMatchIndicators,
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
            // dealt dice, detached below the board.
            Spacer(Modifier.height(gap))
            Box(modifier = Modifier.size(boardW, boardW * spec.dealAspect)) {
                if (state.levelIndex == TwoDecoyPuzzleLibrary.LEVEL_INDEX) {
                    Box(Modifier.matchParentSize()
                        .background(MasterDiceColors.RowPanel, WellShape)
                        .border(2.dp, MasterDiceColors.Brass, WellShape))
                } else {
                    Image(
                        painter = painterResource(
                            if (state.levelIndex == 1) Res.drawable.deal_l1
                            else Res.drawable.deal_row,
                        ),
                        contentDescription = null,
                        contentScale = ContentScale.FillBounds,
                        modifier = Modifier.matchParentSize(),
                    )
                }
                DiceRow(state, spec, boardW, boardW * (spec.dealWellPx / spec.imageWidthPx), dd, onIntent)
            }

            BasicText(
                text = BUILD_VERSION,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .testTag("buildVersion")
                    .semantics { contentDescription = "Build $BUILD_VERSION" },
                style = TextStyle(
                    color = MasterDiceColors.Ivory.copy(alpha = 0.35f),
                    fontSize = 10.sp,
                ),
            )
        }

        DragOverlay(dd, cell)

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
                showMatchIndicators = debugToolsEnabled && showMatchIndicators,
                level = state.level,
                onDismiss = { showHelp = false },
            )
        }

        if (showSettings) {
            SettingsPanel(
                levelIndex = state.levelIndex,
                onLevelSelect = { index ->
                    onIntent(GameIntent.NewGame(Random.nextLong(), index))
                    showSettings = false
                },
                soundEnabled = soundEnabled,
                onToggleSound = onToggleSound,
                showMatchIndicators = debugToolsEnabled && showMatchIndicators,
                onToggle = { showMatchIndicators = debugToolsEnabled && it },
                onDismiss = { showSettings = false },
            )
        }

        if (showDealPicker) {
            DebugDealPanel(
                levelIndex = state.levelIndex,
                onLevelSelect = {
                    onIntent(GameIntent.NewGame(Random.nextLong(), it))
                },
                onDeal = { solution, decoys ->
                    onIntent(
                        GameIntent.NewGameWithDeal(solution, decoys, state.levelIndex),
                    )
                    showDealPicker = false
                },
                onDismiss = { showDealPicker = false },
            )
        }

        if (showDupWarning) {
            RepeatGuessDialog(
                guessNumber = dupGuessIndex + 1,
                onDismiss = { dismissedDup = state.activeCells },
            )
        }
    }
}
