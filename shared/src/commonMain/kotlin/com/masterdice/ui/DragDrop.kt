package com.masterdice.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import com.masterdice.game.Die
import com.masterdice.game.Slot

data class DragSession(
    val from: Slot,
    val die: Die,
    val offset: Offset,
    val returning: Boolean = false,
)

/**
 * Registry of slot bounds (in root coordinates) plus the live drag session.
 * Dice report their wells via [register]; drags read [slotAt] on drop.
 */
class DragDropState {
    private val bounds = mutableStateMapOf<Slot, Rect>()

    /** Coordinates of the box that hosts the drag overlay; bounds are stored
     * relative to it so the overlay lines up regardless of window insets. */
    var container: LayoutCoordinates? = null

    var session by mutableStateOf<DragSession?>(null)
        private set

    /**
     * Registers [slot]'s bounds. [inflatePx] grows the rect outward — tray
     * cells sit undersized beneath full-size dice, so drops should land on
     * the die's footprint, not just the small cell box.
     */
    fun register(slot: Slot, coords: LayoutCoordinates, inflatePx: Float = 0f) {
        val c = container
        var rect = c?.localBoundingBoxOf(coords) ?: coords.boundsInRoot()
        if (inflatePx > 0f) rect = rect.inflate(inflatePx)
        bounds[slot] = rect
    }

    fun slotAt(point: Offset): Slot? =
        bounds.entries.firstOrNull { it.value.contains(point) }?.key

    fun centerOf(slot: Slot): Offset? = bounds[slot]?.center

    fun boundsOf(slot: Slot): Rect? = bounds[slot]

    fun begin(from: Slot, die: Die) {
        session = DragSession(from = from, die = die, offset = centerOf(from) ?: Offset.Zero)
    }

    fun moveBy(delta: Offset) {
        session = session?.let { it.copy(offset = it.offset + delta) }
    }

    fun moveTo(offset: Offset) {
        session = session?.let { it.copy(offset = offset) }
    }

    fun startReturn() {
        session = session?.copy(returning = true)
    }

    fun finish() {
        session = null
    }
}

@Composable
fun rememberDragDropState(): DragDropState = remember { DragDropState() }
