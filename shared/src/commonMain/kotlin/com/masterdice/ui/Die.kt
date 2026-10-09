package com.masterdice.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.masterdice.game.Die

private val PIP_POSITIONS: Map<Int, List<Pair<Float, Float>>> = mapOf(
    1 to listOf(0.5f to 0.5f),
    2 to listOf(0.28f to 0.28f, 0.72f to 0.72f),
    3 to listOf(0.26f to 0.26f, 0.5f to 0.5f, 0.74f to 0.74f),
    4 to listOf(0.3f to 0.3f, 0.7f to 0.3f, 0.3f to 0.7f, 0.7f to 0.7f),
    5 to listOf(0.28f to 0.28f, 0.72f to 0.28f, 0.5f to 0.5f, 0.28f to 0.72f, 0.72f to 0.72f),
    6 to listOf(0.32f to 0.24f, 0.68f to 0.24f, 0.32f to 0.5f, 0.68f to 0.5f, 0.32f to 0.76f, 0.68f to 0.76f),
)

/** A single die face: coloured rounded square with sunken pips and gloss. */
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
                // Specular gloss (CSS .dice-cube::before): a bright band over
                // the top 38% of the face, inset slightly from the sides.
                val insetX = 2.dp.toPx()
                val glossTop = 1.dp.toPx()
                val glossHeight = this.size.height * 0.38f
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.45f),
                            Color.White.copy(alpha = 0.05f),
                        ),
                        startY = glossTop,
                        endY = glossTop + glossHeight,
                    ),
                    topLeft = Offset(insetX, glossTop),
                    size = Size(this.size.width - insetX * 2, glossHeight),
                    cornerRadius = CornerRadius(this.size.minDimension * 0.14f),
                )
            },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val radius = this.size.minDimension * 0.075f
            PIP_POSITIONS.getValue(die.value).forEach { (fx, fy) ->
                val center = Offset(this.size.width * fx, this.size.height * fy)
                // Sunken pip: a slightly larger dark circle offset up-left
                // peeks out as the shadowed rim of the carved socket.
                drawCircle(
                    color = Color.Black.copy(alpha = 0.35f),
                    radius = radius * 1.15f,
                    center = center + Offset(-0.5f, -0.5f),
                )
                drawCircle(color = pips, radius = radius, center = center)
                // Faint ledge highlight below the pip
                // (CSS 0 .5px .5px rgba(255,255,255,.3)).
                drawArc(
                    color = Color.White.copy(alpha = 0.3f),
                    startAngle = 15f,
                    sweepAngle = 150f,
                    useCenter = false,
                    topLeft = center - Offset(radius + 0.75f, radius + 0.75f),
                    size = Size((radius + 0.75f) * 2, (radius + 0.75f) * 2),
                    style = Stroke(width = 1f),
                )
            }
        }
    }
}
