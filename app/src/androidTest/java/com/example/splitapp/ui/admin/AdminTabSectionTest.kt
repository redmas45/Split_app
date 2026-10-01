package com.example.splitapp.ui.admin

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.splitapp.data.FirebaseService
import org.junit.Rule
import org.junit.Test

/** Moderation actions are destructive: they ask first, and "Delete User" no longer exists (it un-banned people). */
class AdminTabSectionTest {

    @get:Rule val rule = createComposeRule()

    private val users: List<Map<String, Any>> = listOf(
        mapOf("uid" to "u1", "email" to "sam@example.com", "groups" to listOf("g1"), "isBanned" to false),
        mapOf("uid" to "u2", "email" to "nia@example.com", "groups" to emptyList<String>(), "isBanned" to true),
    )
    private val groups: List<Map<String, Any>> = listOf(
        mapOf("id" to "g1", "name" to "Goa Trip", "members" to listOf(mapOf("id" to 1, "name" to "Sam")), "transactions" to emptyList<Any>()),
    )

    private fun show() {
        rule.setContent {
            MaterialTheme {
                TabSection(
                    firebaseService = FirebaseService(),
                    users = users, groups = groups,
                    userSearchQuery = "", onUserSearchChange = {},
                    groupSearchQuery = "", onGroupSearchChange = {},
                    onGroupClick = { },
                )
            }
        }
    }

    @Test fun thereIsNoDeleteUserButton() {
        show()
        rule.onNodeWithText("Delete User").assertDoesNotExist()
    }

    @Test fun banning_asksForConfirmation_naming_theUser_andCancelLeavesItAlone() {
        show()
        rule.onNodeWithText("Ban User").performClick()

        rule.onNodeWithText("Ban sam@example.com?").assertIsDisplayed()
        rule.onNodeWithText("Cancel").performClick()
        rule.onNodeWithText("Ban sam@example.com?").assertDoesNotExist()
    }

    @Test fun unbanning_alsoAsks() {
        show()
        rule.onNodeWithText("Unban").performClick()

        rule.onNodeWithText("Unban nia@example.com?").assertIsDisplayed()
    }

    @Test fun deletingAGroup_asksFirst_naming_theGroup() {
        show()
        rule.onNodeWithText("Groups (1)").performClick()
        rule.onNodeWithText("Delete Group").performClick()

        rule.onNodeWithText("Delete \"Goa Trip\"?").assertIsDisplayed()
        rule.onNodeWithText("Cancel").performClick()
        rule.onNodeWithText("Delete \"Goa Trip\"?").assertDoesNotExist()
    }
}
