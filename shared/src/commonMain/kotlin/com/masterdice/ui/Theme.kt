package com.masterdice.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.masterdice.game.DieColour

object MasterDiceColors {
    val Board = Color(0xFF141C2C)
    val Brass = Color(0xFFC9A227)
    val Ivory = Color(0xFFFFF8E7)
    val GreenBorder = Color(0xFF4ADE80)
    val AmberBorder = Color(0xFFF5B700)

    // Stitch palette additions, retuned to the board artwork's slate range.
    val BrassLight = Color(0xFFE8C766)
    val BrassBright = Color(0xFFECC246)
    val RowPanel = Color(0xFF222E44)
    val SubmittedPanel = Color(0xFF141C2A)
    val SubmittedPanelBorder = Color(0xFF33425C)

    // Felt vignette (casino tabletop green — lit centre, deep edges).
    val FeltLight = Color(0xFF1C7A36)
    val FeltMid = Color(0xFF0D4C1C)
    val FeltDark = Color(0xFF03180A)

    // Header pills: champagne-gold buttons, ink glyphs, purple score.
    val GoldHi = Color(0xFFFFE9A8)
    val Gold = Color(0xFFE3B64B)
    val GoldLow = Color(0xFFB07E14)
    val InkBrown = Color(0xFF4A2E07)
    val ScorePurple = Color(0xFF6D28D9)
    val ScorePurpleDeep = Color(0xFF4C1D95)
}

/**
 * Deep, colour-tinted drop shadow + clip — the raised base of the "chunky
 * tactile" look. Replaces a hard outline border with a shadow that reads as
 * thickness instead of an edge.
 */
fun Modifier.chunkyShadow(
    shape: Shape,
    elevation: Dp = 8.dp,
    tint: Color = Color.Black,
): Modifier = this
    .shadow(
        elevation = elevation,
        shape = shape,
        ambientColor = tint.copy(alpha = 0.5f),
        spotColor = tint.copy(alpha = 0.5f),
    )
    .clip(shape)

/**
 * Soft top highlight + bottom shade painted over whatever this element
 * already drew — fakes the inset bevel of a pressable/gummy surface without
 * needing a true inset shadow. Pairs with [chunkyShadow]; apply after the
 * element's background.
 */
fun Modifier.chunkySheen(): Modifier = this.drawWithContent {
    drawContent()
    val h = size.height
    drawRect(
        brush = Brush.verticalGradient(
            0f to Color.White.copy(alpha = 0.28f),
            1f to Color.Transparent,
            startY = 0f,
            endY = h * 0.45f,
        ),
    )
    drawRect(
        brush = Brush.verticalGradient(
            0f to Color.Transparent,
            1f to Color.Black.copy(alpha = 0.30f),
            startY = h * 0.55f,
            endY = h,
        ),
    )
}

fun colourName(colour: DieColour): String = when (colour) {
    DieColour.RED -> "Red"
    DieColour.BLUE -> "Blue"
    DieColour.GREEN -> "Green"
    DieColour.ORANGE -> "Orange"
    DieColour.PINK -> "Pink"
    DieColour.TEAL -> "Teal"
}

/** Small solid swatch per die colour (debug deal picker). */
fun dieColourSwatch(colour: DieColour): Color = when (colour) {
    DieColour.RED -> Color(0xFFD64545)
    DieColour.BLUE -> Color(0xFF4A7FD4)
    DieColour.GREEN -> Color(0xFF46A860)
    DieColour.ORANGE -> Color(0xFFE8930C)
    DieColour.PINK -> Color(0xFFDE6FA8)
    DieColour.TEAL -> Color(0xFF3FB8AF)
}
