package com.masterdice.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.masterdice.game.Die
import com.masterdice.game.DieColour
import com.masterdice.resources.Res
import com.masterdice.resources.die_blue_1
import com.masterdice.resources.die_blue_2
import com.masterdice.resources.die_blue_3
import com.masterdice.resources.die_blue_4
import com.masterdice.resources.die_blue_5
import com.masterdice.resources.die_blue_6
import com.masterdice.resources.die_green_1
import com.masterdice.resources.die_green_2
import com.masterdice.resources.die_green_3
import com.masterdice.resources.die_green_4
import com.masterdice.resources.die_green_5
import com.masterdice.resources.die_green_6
import com.masterdice.resources.die_orange_1
import com.masterdice.resources.die_orange_2
import com.masterdice.resources.die_orange_3
import com.masterdice.resources.die_orange_4
import com.masterdice.resources.die_orange_5
import com.masterdice.resources.die_orange_6
import com.masterdice.resources.die_pink_1
import com.masterdice.resources.die_pink_2
import com.masterdice.resources.die_pink_3
import com.masterdice.resources.die_pink_4
import com.masterdice.resources.die_pink_5
import com.masterdice.resources.die_pink_6
import com.masterdice.resources.die_red_1
import com.masterdice.resources.die_red_2
import com.masterdice.resources.die_red_3
import com.masterdice.resources.die_red_4
import com.masterdice.resources.die_red_5
import com.masterdice.resources.die_red_6
import com.masterdice.resources.die_teal_1
import com.masterdice.resources.die_teal_2
import com.masterdice.resources.die_teal_3
import com.masterdice.resources.die_teal_4
import com.masterdice.resources.die_teal_5
import com.masterdice.resources.die_teal_6
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

private val DICE: Map<DieColour, List<DrawableResource>> = mapOf(
    DieColour.RED to listOf(
        Res.drawable.die_red_1, Res.drawable.die_red_2, Res.drawable.die_red_3,
        Res.drawable.die_red_4, Res.drawable.die_red_5, Res.drawable.die_red_6,
    ),
    DieColour.BLUE to listOf(
        Res.drawable.die_blue_1, Res.drawable.die_blue_2, Res.drawable.die_blue_3,
        Res.drawable.die_blue_4, Res.drawable.die_blue_5, Res.drawable.die_blue_6,
    ),
    DieColour.GREEN to listOf(
        Res.drawable.die_green_1, Res.drawable.die_green_2, Res.drawable.die_green_3,
        Res.drawable.die_green_4, Res.drawable.die_green_5, Res.drawable.die_green_6,
    ),
    DieColour.ORANGE to listOf(
        Res.drawable.die_orange_1, Res.drawable.die_orange_2, Res.drawable.die_orange_3,
        Res.drawable.die_orange_4, Res.drawable.die_orange_5, Res.drawable.die_orange_6,
    ),
    DieColour.PINK to listOf(
        Res.drawable.die_pink_1, Res.drawable.die_pink_2, Res.drawable.die_pink_3,
        Res.drawable.die_pink_4, Res.drawable.die_pink_5, Res.drawable.die_pink_6,
    ),
    DieColour.TEAL to listOf(
        Res.drawable.die_teal_1, Res.drawable.die_teal_2, Res.drawable.die_teal_3,
        Res.drawable.die_teal_4, Res.drawable.die_teal_5, Res.drawable.die_teal_6,
    ),
)

fun dieResource(die: Die): DrawableResource =
    DICE.getValue(die.colour)[die.value - 1]

@Composable
fun diePainter(die: Die): Painter = painterResource(dieResource(die))

/** A single die rendered from the artwork (colour + pips baked in). */
@Composable
fun DieView(
    die: Die,
    size: Dp,
    modifier: Modifier = Modifier,
    alpha: Float = 1f,
) {
    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer { this.alpha = alpha }
            .clip(RoundedCornerShape(size * 0.18f)),
    ) {
        Image(
            painter = diePainter(die),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.size(size),
        )
    }
}
