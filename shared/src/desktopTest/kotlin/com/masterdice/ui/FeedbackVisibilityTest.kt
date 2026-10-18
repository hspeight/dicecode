package com.masterdice.ui

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.test.v2.runComposeUiTest
import com.masterdice.game.Feedback
import com.masterdice.game.Guess
import com.masterdice.game.newGame
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal fun withDesktopDebugTools(enabled: Boolean, block: () -> Unit) {
    val previous = System.getProperty("masterdice.debug")
    try {
        System.setProperty("masterdice.debug", enabled.toString())
        block()
    } finally {
        if (previous == null) System.clearProperty("masterdice.debug")
        else System.setProperty("masterdice.debug", previous)
    }
}


@OptIn(ExperimentalTestApi::class)
class FeedbackVisibilityTest {

    @Test
    fun normalModeHidesDebugToggleAndPositionalSemantics() = withDesktopDebugTools(false) {
        runComposeUiTest {
            val initial = newGame(7L)
            val state = initial.copy(guesses = listOf(
                Guess(initial.solution, List(3) { Feedback.GREEN }),
            ))
            setContent { GameScreen(state, onIntent = {}, onNewGame = {}) }
            val description = onNodeWithTag("cell-0-0").fetchSemanticsNode()
                .config[SemanticsProperties.ContentDescription].joinToString()
            assertFalse(description.contains("exact match"))
            onNodeWithTag("settings").performClick()
            onNodeWithTag("toggleMatchIndicators").assertDoesNotExist()
        }
    }

    @Test
    fun debugModeRevealsPositionalSemanticsOnlyAfterOptIn() = withDesktopDebugTools(true) {
        runComposeUiTest {
            val initial = newGame(7L)
            val state = initial.copy(guesses = listOf(
                Guess(initial.solution, List(3) { Feedback.GREEN }),
            ))
            setContent { GameScreen(state, onIntent = {}, onNewGame = {}) }
            fun description() = onNodeWithTag("cell-0-0").fetchSemanticsNode()
                .config[SemanticsProperties.ContentDescription].joinToString()
            assertFalse(description().contains("exact match"))
            onNodeWithTag("settings").performClick()
            onNodeWithTag("toggleMatchIndicators").performTouchInput {
                click(center.copy(x = center.x * 1.5f))
            }
            onNodeWithTag("settingsDone").performClick()
            assertTrue(description().contains("exact match"))
        }
    }
}
