package com.masterdice.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.gestures.detectTapGestures
import com.masterdice.game.Slot
import com.masterdice.resources.Res
import com.masterdice.resources.pointing_hand
import androidx.compose.ui.layout.ContentScale
import org.jetbrains.compose.resources.painterResource
import kotlin.math.roundToInt

@Composable
internal fun BoxScope.DragOverlay(dd: DragDropState, dieSize: Dp) {
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
internal fun DrawScope.drawFeltBackground() {
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
internal fun HintBubble(anchor: Offset, aboveBy: Dp, onDismiss: () -> Unit) {
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
            .chunkyShadow(RoundedCornerShape(8.dp), elevation = 4.dp)
            .background(MasterDiceColors.Board)
            .chunkySheen()
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
internal fun TrayPointer(dd: DragDropState, visible: Boolean) {
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
