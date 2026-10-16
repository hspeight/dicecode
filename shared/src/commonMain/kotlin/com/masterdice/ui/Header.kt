package com.masterdice.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.gestures.detectTapGestures

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
            .chunkyShadow(CircleShape, tint = MasterDiceColors.GoldLow)
            .background(goldPillBrush())
            .chunkySheen()
            .pointerInput(Unit) { detectTapGestures { onClick() } },
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
internal fun HelpButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
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
internal fun SettingsButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
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
internal fun DebugDealButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(32.dp)
            .testTag("debugDeal")
            .semantics { contentDescription = "Open debug deal picker" }
            .chunkyShadow(RoundedCornerShape(50), elevation = 5.dp)
            .background(MasterDiceColors.RowPanel)
            .chunkySheen()
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
internal fun goldPillBrush() = Brush.verticalGradient(
    listOf(MasterDiceColors.GoldHi, MasterDiceColors.Gold, MasterDiceColors.GoldLow),
)

/** Gold UNDO pill: curved-arrow glyph + label, like the reference header. */
@Composable
internal fun UndoPill(enabled: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .height(34.dp)
            .testTag("undo")
            .semantics { contentDescription = "Undo last guess" }
            .chunkyShadow(RoundedCornerShape(50), tint = MasterDiceColors.GoldLow)
            .background(goldPillBrush())
            .chunkySheen()
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

/** Large purple SCORE badge, centered on its own row above the controls. */
@Composable
internal fun ScorePill(score: Int) {
    Row(
        modifier = Modifier
            .width(160.dp)
            .height(56.dp)
            .testTag("runningTotal")
            .semantics { contentDescription = "Score $score" }
            .chunkyShadow(
                RoundedCornerShape(12.dp),
                elevation = 10.dp,
                tint = MasterDiceColors.ScorePurpleDeep,
            )
            .background(
                Brush.verticalGradient(
                    listOf(MasterDiceColors.ScorePurple, MasterDiceColors.ScorePurpleDeep),
                ),
            )
            .chunkySheen(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            BasicText(
                text = "SCORE",
                style = TextStyle(
                    color = MasterDiceColors.GoldHi.copy(alpha = 0.85f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 2.5.sp,
                ),
            )
            BasicText(
                text = score.toString()
                    .reversed().chunked(3).joinToString(",").reversed(),
                style = TextStyle(
                    color = MasterDiceColors.GoldHi,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
        }
    }
}

/** Debug-only panel: pick the three solution dice (in order) and the decoy. */