package com.example.splitapp.ui.group

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.splitapp.data.FirebaseService
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupSelectionScreen(
    firebaseService: FirebaseService,
    onGroupSelected: (groupId: String, groupName: String) -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val groupsState = firebaseService.getUserGroups().collectAsState(initial = null)
    
    var showCreateDialog by remember { mutableStateOf(false) }
    var showJoinDialog by remember { mutableStateOf(false) }
    
    var groupNameInput by remember { mutableStateOf("") }
    var creatorNameInput by remember { mutableStateOf("") }
    
    var joinGroupIdInput by remember { mutableStateOf("") }
    var joinNameInput by remember { mutableStateOf("") }

    var actionLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0F2027),
            Color(0xFF203A43)
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Groups", fontWeight = FontWeight.Bold, color = Color.White) },
                actions = {
                    TextButton(onClick = {
                        firebaseService.signOut()
                        onSignOut()
                    }) {
                        Text("Sign Out", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0F2027)
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF7F9FC))
        ) {
            val groups = groupsState.value

            if (groups == null) {
                // Loading state
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF203A43))
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Welcome header card
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 20.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = Color(0xFF203A43))
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = "Welcome to SplitShare!",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = firebaseService.currentUser?.email ?: "User",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { 
                                errorMessage = null
                                showCreateDialog = true 
                            },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C5364))
                        ) {
                            Text("Create Group", fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { 
                                errorMessage = null
                                showJoinDialog = true 
                            },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF203A43))
                        ) {
                            Text("Join Group", fontWeight = FontWeight.Bold)
                        }
                    }

                    if (groups.isEmpty()) {
                        // Empty state
                        Box(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("📁", fontSize = 64.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No groups found.\nCreate or join a group to start splitting expenses!",
                                    textAlign = TextAlign.Center,
                                    color = Color.Gray,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(groups) { group ->
                                val id = group["id"] as? String ?: ""
                                val name = group["name"] as? String ?: "Unnamed Group"
                                val membersList = group["members"] as? List<*> ?: emptyList<Any>()
                                val txList = group["transactions"] as? List<*> ?: emptyList<Any>()

                                ElevatedCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onGroupSelected(id, name) },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.elevatedCardColors(containerColor = Color.White)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = name,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF0F2027)
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Members: ${membersList.size} | Transactions: ${txList.size}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color.Gray
                                            )
                                            Text(
                                                text = "ID: $id",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color.LightGray
                                            )
                                        }
                                        Text("➡️", fontSize = 20.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Create Group Dialog
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { if (!actionLoading) showCreateDialog = false },
            title = { Text("Create Group", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = groupNameInput,
                        onValueChange = { groupNameInput = it },
                        label = { Text("Group Name") },
                        singleLine = true,
                        enabled = !actionLoading
                    )
                    OutlinedTextField(
                        value = creatorNameInput,
                        onValueChange = { creatorNameInput = it },
                        label = { Text("Your Nickname") },
                        singleLine = true,
                        enabled = !actionLoading
                    )
                    AnimatedVisibility(visible = errorMessage != null) {
                        errorMessage?.let { error ->
                            Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (groupNameInput.isBlank() || creatorNameInput.isBlank()) {
                            errorMessage = "Please fill in all fields"
                            return@Button
                        }
                        errorMessage = null
                        actionLoading = true
                        coroutineScope.launch {
                            val result = firebaseService.createGroup(groupNameInput.trim(), creatorNameInput.trim())
                            actionLoading = false
                            if (result.isSuccess) {
                                val newGroupId = result.getOrThrow()
                                showCreateDialog = false
                                onGroupSelected(newGroupId, groupNameInput.trim())
                                groupNameInput = ""
                                creatorNameInput = ""
                            } else {
                                errorMessage = result.exceptionOrNull()?.localizedMessage ?: "Failed to create group"
                            }
                        }
                    },
                    enabled = !actionLoading
                ) {
                    if (actionLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("Create")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }, enabled = !actionLoading) {
                    Text("Cancel")
                }
            }
        )
    }

    // Join Group Dialog
    if (showJoinDialog) {
        AlertDialog(
            onDismissRequest = { if (!actionLoading) showJoinDialog = false },
            title = { Text("Join Group", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = joinGroupIdInput,
                        onValueChange = { joinGroupIdInput = it },
                        label = { Text("Group ID/Code") },
                        singleLine = true,
                        enabled = !actionLoading
                    )
                    OutlinedTextField(
                        value = joinNameInput,
                        onValueChange = { joinNameInput = it },
                        label = { Text("Your Nickname") },
                        singleLine = true,
                        enabled = !actionLoading
                    )
                    AnimatedVisibility(visible = errorMessage != null) {
                        errorMessage?.let { error ->
                            Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (joinGroupIdInput.isBlank() || joinNameInput.isBlank()) {
                            errorMessage = "Please fill in all fields"
                            return@Button
                        }
                        errorMessage = null
                        actionLoading = true
                        coroutineScope.launch {
                            val result = firebaseService.joinGroup(joinGroupIdInput.trim(), joinNameInput.trim())
                            actionLoading = false
                            if (result.isSuccess) {
                                showJoinDialog = false
                                onGroupSelected(joinGroupIdInput.trim(), "Group")
                                joinGroupIdInput = ""
                                joinNameInput = ""
                            } else {
                                errorMessage = result.exceptionOrNull()?.localizedMessage ?: "Failed to join group"
                            }
                        }
                    },
                    enabled = !actionLoading
                ) {
                    if (actionLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("Join")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showJoinDialog = false }, enabled = !actionLoading) {
                    Text("Cancel")
                }
            }
        )
    }
}
