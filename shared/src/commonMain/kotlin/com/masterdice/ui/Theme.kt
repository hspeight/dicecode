package com.masterdice.ui

import androidx.compose.ui.graphics.Color
import com.masterdice.game.DieColour

object MasterDiceColors {
    val Board = Color(0xFF14343B)
    val Well = Color(0xFF1E4A52)
    val WellActive = Color(0xFF265A64)
    val Tray = Color(0xFF6B4428)
    val TrayRim = Color(0xFF4A2E1A)
    val Brass = Color(0xFFC9A227)
    val Ivory = Color(0xFFFFF8E7)
    val PipDark = Color(0xFF2B2B2B)
    val GreenBorder = Color(0xFF4ADE80)
    val AmberBorder = Color(0xFFF5B700)
}

fun dieColour(colour: DieColour): Color = when (colour) {
    DieColour.RED -> Color(0xFFC62828)
    DieColour.BLUE -> Color(0xFF0072B2)
    DieColour.GREEN -> Color(0xFF009E73)
    DieColour.YELLOW -> Color(0xFFF0E442)
    DieColour.PURPLE -> Color(0xFF8E5BB5)
    DieColour.ORANGE -> Color(0xFFE69F00)
}

fun pipColour(colour: DieColour): Color = when (colour) {
    DieColour.YELLOW, DieColour.ORANGE -> MasterDiceColors.PipDark
    else -> MasterDiceColors.Ivory
}

fun colourInitial(colour: DieColour): String = when (colour) {
    DieColour.RED -> "R"
    DieColour.BLUE -> "B"
    DieColour.GREEN -> "G"
    DieColour.YELLOW -> "Y"
    DieColour.PURPLE -> "P"
    DieColour.ORANGE -> "O"
}

fun colourName(colour: DieColour): String = when (colour) {
    DieColour.RED -> "Red"
    DieColour.BLUE -> "Blue"
    DieColour.GREEN -> "Green"
    DieColour.YELLOW -> "Yellow"
    DieColour.PURPLE -> "Purple"
    DieColour.ORANGE -> "Orange"
}
