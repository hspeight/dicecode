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

    // Stitch palette additions.
    val BrassLight = Color(0xFFE8C766)
    val RowPanel = Color(0xFF194047)
    val SubmittedPanel = Color(0xFF112A2F)
    val SubmittedPanelBorder = Color(0xFF23535C)

    // Recessed socket gradient stops (occupied well / empty "deeper" well).
    val SocketLight = Color(0xFF15383E)
    val SocketMid = Color(0xFF102C31)
    val SocketDeep = Color(0xFF0B2024)
    val SocketEmptyLight = Color(0xFF133339)
    val SocketEmptyMid = Color(0xFF0C2327)
    val SocketEmptyDeep = Color(0xFF071719)

    // Brass mystery tile.
    val TileEdge = Color(0xFF785A0D)
    val TileGlyph = Color(0xFF4A3208)

    // Walnut tray.
    val TrayTop = Color(0xFF57361E)
    val TrayBottom = Color(0xFF54341B)
    val TrayRimTop = Color(0xFF3F2412)
    val TrayRimBottom = Color(0xFF281509)
    val GrooveDark = Color(0xFF2D180B)
    val GrooveMid = Color(0xFF3B2110)
    val GrooveLight = Color(0xFF462713)

    // Glowing amber submit button.
    val EnterLight = Color(0xFFFFA726)
    val EnterMid = Color(0xFFFB8C00)
    val EnterDark = Color(0xFFE65100)
    val EnterEdge = Color(0xFFFFE082)

    /** Brass tile gradient: 145° (#F3D98A → #C9A227 → #967414 → #6C5209). */
    val BrassTileStops: Array<Pair<Float, Color>> = arrayOf(
        0f to Color(0xFFF3D98A),
        0.45f to Color(0xFFC9A227),
        0.85f to Color(0xFF967414),
        1f to Color(0xFF6C5209),
    )
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
