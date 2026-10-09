package com.masterdice.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.masterdice.game.Die

private val PIP_POSITIONS: Map<Int, List<Pair<Float, Float>>> = mapOf(
    1 to listOf(0.5f to 0.5f),
    2 to listOf(0.28f to 0.28f, 0.72f to 0.72f),
    3 to listOf(0.26f to 0.26f, 0.5f to 0.5f, 0.74f to 0.74f),
    4 to listOf(0.3f to 0.3f, 0.7f to 0.3f, 0.3f to 0.7f, 0.7f to 0.7f),
    5 to listOf(0.28f to 0.28f, 0.72f to 0.28f, 0.5f to 0.5f, 0.28f to 0.72f, 0.72f to 0.72f),
    6 to listOf(0.32f to 0.24f, 0.68f to 0.24f, 0.32f to 0.5f, 0.68f to 0.5f, 0.32f to 0.76f, 0.68f to 0.76f),
)

/** A single die face: coloured rounded square, pips, and a colour-initial label. */
@Composable
fun DieView(
    die: Die,
    size: Dp,
    modifier: Modifier = Modifier,
    dimmed: Boolean = false,
) {
    val face = dieColour(die.colour)
    val pips = pipColour(die.colour)
    val alpha = if (dimmed) 0.4f else 1f

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer { this.alpha = alpha }
            .clip(RoundedCornerShape(size * 0.18f))
            .drawBehind {
                drawRect(face)
                // Top-left bevel highlight.
                drawRect(
                    Brush.linearGradient(
                        colors = listOf(Color.White.copy(alpha = 0.22f), Color.Transparent),
                        start = Offset.Zero,
                        end = Offset(this.size.width, this.size.height),
                    )
                )
            },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val radius = this.size.minDimension * 0.075f
            PIP_POSITIONS.getValue(die.value).forEach { (fx, fy) ->
                drawCircle(
                    color = pips,
                    radius = radius,
                    center = Offset(this.size.width * fx, this.size.height * fy),
                )
            }
        }
        BasicText(
            text = colourInitial(die.colour),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .graphicsLayer { translationX = -size.toPx() * 0.08f; translationY = size.toPx() * 0.05f },
            style = TextStyle(
                color = pips.copy(alpha = 0.85f),
                fontSize = (size.value * 0.22f).sp,
                fontWeight = FontWeight.Bold,
            ),
        )
    }
}
