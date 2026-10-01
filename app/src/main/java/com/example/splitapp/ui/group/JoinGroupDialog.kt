package com.example.splitapp.ui.group

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.splitapp.domain.JoinChoice
import com.example.splitapp.domain.Member

/** What step 2 shows: the group's name and the members nobody has claimed yet. */
data class JoinPrompt(val groupName: String, val freeMembers: List<Member>)

/**
 * Two-step join. Step 1 (`prompt == null`): enter the group code. Step 2: "Which one are you?": pick a member nobody
 * has claimed yet, or join as a new member with a nickname. State is hoisted so the screen can keep it across rotation.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun JoinGroupDialog(
    code: String,
    onCodeChange: (String) -> Unit,
    prompt: JoinPrompt?,
    selectedMemberId: Int?,
    onSelectMember: (Int) -> Unit,
    nickname: String,
    onNicknameChange: (String) -> Unit,
    busy: Boolean,
    error: String?,
    onNext: () -> Unit,
    onJoin: (JoinChoice) -> Unit,
    onBack: () -> Unit,
    onDismiss: () -> Unit,
) {
    val canJoin = !busy && (selectedMemberId != null || nickname.isNotBlank())

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text(if (prompt == null) "Join Existing Group" else "Which one are you?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (prompt == null) {
                    OutlinedTextField(
                        value = code,
                        onValueChange = onCodeChange,
                        label = { Text("Group Code (e.g. 5xJ1...)") },
                        singleLine = true,
                        enabled = !busy,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        shape = RoundedCornerShape(12.dp)
                    )
                } else {
                    Text("Joining ${prompt.groupName}", style = MaterialTheme.typography.bodyMedium)
                    if (prompt.freeMembers.isNotEmpty()) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            prompt.freeMembers.forEach { m ->
                                FilterChip(
                                    selected = selectedMemberId == m.id,
                                    onClick = { onSelectMember(m.id) },
                                    enabled = !busy,
                                    label = { Text(m.name) }
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = nickname,
                        onValueChange = onNicknameChange,
                        label = { Text("I'm new — my nickname") },
                        singleLine = true,
                        enabled = !busy,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            if (prompt == null) {
                Button(onClick = onNext, enabled = !busy && code.isNotBlank()) {
                    if (busy) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    else Text("Next")
                }
            } else {
                Button(
                    onClick = {
                        onJoin(
                            if (selectedMemberId != null) JoinChoice.ClaimMember(selectedMemberId)
                            else JoinChoice.NewMember(nickname)
                        )
                    },
                    enabled = canJoin
                ) {
                    if (busy) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    else Text("Join")
                }
            }
        },
        dismissButton = {
            if (prompt == null) TextButton(onClick = onDismiss, enabled = !busy) { Text("Cancel") }
            else TextButton(onClick = onBack, enabled = !busy) { Text("Back") }
        }
    )
}
