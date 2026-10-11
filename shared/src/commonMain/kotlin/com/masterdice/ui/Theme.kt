package com.masterdice.ui

import androidx.compose.ui.graphics.Color
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

    // Felt vignette (Stitch tabletop-felt, in the board's slate range).
    val FeltLight = Color(0xFF1F3050)
    val FeltMid = Color(0xFF111B2E)
    val FeltDark = Color(0xFF070D17)

    // Tray cell wells, drawn to match the die corner radius (board_well.png).
    val WellFill = Color(0xFF2C3A52)
    val WellRim = Color(0xFF152238)

    // Deal tray: dark recessed well with a warm bronze rim, lit from above.
    val TrayFill = Color(0xFF1C1410)
    val TrayRimTop = Color(0xFF9A6339)
    val TrayRim = Color(0xFF6E4426)
    val TrayRimBottom = Color(0xFF2E1C0E)

    // Glowing amber submit button.
    val EnterLight = Color(0xFFFFA726)
    val EnterMid = Color(0xFFFB8C00)
    val EnterDark = Color(0xFFE65100)
    val EnterEdge = Color(0xFFFFE082)
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
