package com.example.splitapp.ui.group

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.splitapp.domain.JoinChoice
import com.example.splitapp.domain.Member
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** The two-step join: step 2 asks "Which one are you?" (UX-HUB-3 / DATA-2). */
class JoinGroupDialogTest {

    @get:Rule val rule = createComposeRule()

    private val joined = mutableListOf<JoinChoice>()
    private val unclaimed = listOf(Member(2, "Sam"), Member(3, "Nia"))   // Raj (id 1) is already linked, so not offered

    private fun showStep2() {
        rule.setContent {
            MaterialTheme {
                var selected by remember { mutableStateOf<Int?>(null) }
                var nickname by remember { mutableStateOf("") }
                JoinGroupDialog(
                    code = "abc", onCodeChange = {},
                    prompt = JoinPrompt("Goa Trip", unclaimed),
                    selectedMemberId = selected,
                    onSelectMember = { selected = it; nickname = "" },
                    nickname = nickname,
                    onNicknameChange = { nickname = it; selected = null },
                    busy = false, error = null,
                    onNext = {}, onJoin = { joined += it }, onBack = {}, onDismiss = {},
                )
            }
        }
    }

    @Test fun stepTwo_asksWhichOneYouAre_andOffersOnlyUnclaimedMembers() {
        showStep2()

        rule.onNodeWithText("Which one are you?").assertIsDisplayed()
        rule.onNodeWithText("Sam").assertIsDisplayed()
        rule.onNodeWithText("Nia").assertIsDisplayed()
        rule.onNodeWithText("Raj").assertDoesNotExist()
    }

    @Test fun nothingIsSubmittedBeforeAChoiceIsMade() {
        showStep2()

        rule.onNodeWithText("Join").assertIsNotEnabled()
        assertTrue(joined.isEmpty())
    }

    @Test fun pickingAMember_claimsThatMember() {
        showStep2()
        rule.onNodeWithText("Sam").performClick()
        rule.onNodeWithText("Join").assertIsEnabled().performClick()

        assertEquals(listOf<JoinChoice>(JoinChoice.ClaimMember(2)), joined)
    }

    @Test fun typingANickname_joinsAsANewMember() {
        showStep2()
        rule.onNodeWithText("I'm new — my nickname").performTextInput("Zed")
        rule.onNodeWithText("Join").assertIsEnabled().performClick()

        assertEquals(listOf<JoinChoice>(JoinChoice.NewMember("Zed")), joined)
    }

    @Test fun aBlankNickname_isNotAChoice() {
        showStep2()
        rule.onNodeWithText("I'm new — my nickname").performTextInput("   ")

        rule.onNodeWithText("Join").assertIsNotEnabled()
    }
}
