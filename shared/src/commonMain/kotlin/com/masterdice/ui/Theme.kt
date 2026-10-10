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

    // Walnut tray strip.
    val TrayTop = Color(0xFF4A2D18)
    val Tray = Color(0xFF3A2312)
    val TrayBottom = Color(0xFF2A180C)
    val TrayRimTop = Color(0xFF1E1107)
    val TrayRimBottom = Color(0xFF120A04)

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
