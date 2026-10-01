package com.example.splitapp.ui.login

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import com.example.splitapp.data.FirebaseService
import org.junit.Rule
import org.junit.Test

/** Login form behaviour that does not need the network (no sign-in is actually attempted). */
class LoginScreenTest {

    @get:Rule val rule = createComposeRule()

    private fun show(windowHeight: Int? = null) {
        rule.setContent {
            MaterialTheme {
                Box(if (windowHeight == null) Modifier.fillMaxSize() else Modifier.height(windowHeight.dp)) {
                    LoginScreen(firebaseService = FirebaseService(), onLoginSuccess = {})
                }
            }
        }
    }

    @Test fun inAShortWindow_theFormScrolls_soTheLoginButtonCanAlwaysBeReached() {
        show(windowHeight = 280)   // like a phone with the keyboard up, or in landscape

        rule.onNodeWithText("LOG IN").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("LOG IN").performClick()
        rule.onNodeWithText("Please fill in all fields").assertExists()
    }

    @Test fun forgotPassword_withNoEmail_asksForTheEmailFirst() {
        show()
        rule.onNodeWithText("Forgot password?").performScrollTo().performClick()

        rule.onNodeWithText("Enter your email above first").assertExists()
    }

    // What the field exposes as its editable text (this is what a screen reader announces): dots while masked, the
    // real characters once revealed. (A field typed as KeyboardType.Password keeps exposing dots even after "Show".)
    private fun editableText(matches: (String) -> Boolean) = SemanticsMatcher("editable text matches") { node ->
        val text: String? = node.config.getOrNull(SemanticsProperties.EditableText)?.text
        text != null && matches(text)
    }

    private val masked = editableText { it.isNotEmpty() && it.all { c -> c == '•' } }
    private val revealed = editableText { it == "secret123" }

    @Test fun theShowHideToggle_revealsAndReHidesThePassword() {
        show()
        rule.onNodeWithText("Password").performTextInput("secret123")
        rule.onNodeWithText("secret123").assert(masked)

        rule.onNodeWithText("Show").performClick()
        rule.onNodeWithText("secret123").assert(revealed)

        rule.onNodeWithText("Hide").performClick()
        rule.onNodeWithText("secret123").assert(masked)
    }
}
