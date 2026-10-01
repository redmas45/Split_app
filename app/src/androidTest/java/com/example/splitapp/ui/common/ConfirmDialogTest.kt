package com.example.splitapp.ui.common

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ConfirmDialogTest {

    @get:Rule val rule = createComposeRule()

    private var confirmCalls = 0
    private var dismissCalls = 0
    private val release = CompletableDeferred<Unit>()

    private fun show(slowConfirm: Boolean) {
        rule.setContent {
            MaterialTheme {
                var open by remember { mutableStateOf(true) }
                if (open) {
                    ConfirmDialog(
                        title = "Leave group?", message = "You can rejoin with the code.", confirmLabel = "Leave",
                        destructive = true,
                        onConfirm = { confirmCalls++; if (slowConfirm) release.await() },
                        onDismiss = { dismissCalls++; open = false },
                    )
                }
            }
        }
    }

    @Test fun cancel_dismissesWithoutConfirming() {
        show(slowConfirm = false)
        rule.onNodeWithText("Cancel").performClick()

        assertEquals(0, confirmCalls)
        assertEquals(1, dismissCalls)
        rule.onNodeWithText("Leave group?").assertDoesNotExist()
    }

    @Test fun confirm_runsTheActionOnce_thenCloses() {
        show(slowConfirm = false)
        rule.onNodeWithText("Leave").performClick()
        rule.waitForIdle()

        assertEquals(1, confirmCalls)
        assertEquals(1, dismissCalls)
        rule.onNodeWithText("Leave group?").assertDoesNotExist()
    }

    @Test fun whileTheActionRuns_theDialogStaysAndCannotBeCancelledOrConfirmedAgain() {
        show(slowConfirm = true)
        rule.onNodeWithText("Leave").performClick()
        rule.waitForIdle()

        rule.onNodeWithText("Leave group?").assertIsDisplayed()   // still open while busy
        rule.onNodeWithText("Cancel").assertIsNotEnabled()
        assertEquals(1, confirmCalls)
        assertTrue("must not dismiss while busy", dismissCalls == 0)

        release.complete(Unit)                                    // the action finishes
        rule.waitForIdle()
        assertEquals(1, dismissCalls)
        rule.onNodeWithText("Leave group?").assertDoesNotExist()
    }
}
