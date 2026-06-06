package com.example.splitapp.ui.group

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.ui.platform.LocalContext
import com.example.splitapp.data.FirebaseService
import com.example.splitapp.data.NotificationHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupSelectionScreen(
    firebaseService: FirebaseService,
    onGroupSelected: (groupId: String, groupName: String) -> Unit,
    onSignOut: () -> Unit,
    onAdminClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val groupsState = firebaseService.getUserGroups().collectAsState(initial = null)
    
    val lastSeenTxIds = remember { mutableStateMapOf<String, Int>() }

    LaunchedEffect(groupsState.value) {
        val groups = groupsState.value
        if (groups != null) {
            groups.forEach { group ->
                val groupId = group["id"] as? String ?: ""
                val groupName = group["name"] as? String ?: "Group"
                val txList = group["transactions"] as? List<*> ?: emptyList<Any>()
                val maxTxId = txList.mapNotNull { 
                    val txMap = it as? Map<*, *>
                    (txMap?.get("id") as? Number)?.toInt() 
                }.maxOrNull() ?: 0
                
                val lastSeen = lastSeenTxIds[groupId]
                if (lastSeen != null && maxTxId > lastSeen) {
                    val newTx = txList.lastOrNull() as? Map<*, *>
                    if (newTx != null) {
                        val amount = newTx["amount"] as? String ?: "0"
                        val type = newTx["type"] as? String ?: "expense"
                        val detail = if (type == "expense") {
                            newTx["description"] as? String ?: "Expense"
                        } else {
                            "Transfer"
                        }
                        NotificationHelper.showPaymentNotification(
                            context = context,
                            title = "New Activity in $groupName 💸",
                            text = "$detail: ₹$amount"
                        )
                    }
                }
                lastSeenTxIds[groupId] = maxTxId
            }
        }
    }
    
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
            Color(0xFF203A43),
            Color(0xFF2C5364)
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "SplitShare Hub", 
                        fontWeight = FontWeight.Bold, 
                        color = Color.White,
                        fontSize = 20.sp
                    ) 
                },
                actions = {
                    if (firebaseService.isAdmin()) {
                        TextButton(
                            onClick = onAdminClick,
                            colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFFD700)) // Gold accent for admin
                        ) {
                            Text("Admin 🔑", fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    IconButton(
                        onClick = {
                            firebaseService.signOut()
                            onSignOut()
                        }
                    ) {
                        Text("🚪", fontSize = 20.sp)
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
                .background(gradientBrush)
        ) {
            val groups = groupsState.value

            if (groups == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White)
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Header welcome widget (glassmorphic style card)
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = Color.White.copy(alpha = 0.12f)
                        ),
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Hello there! 👋",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = firebaseService.currentUser?.email ?: "User",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            // Stats bubble
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("Groups", color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp)
                                Text("${groups.size}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                    }

                    // Large Premium Action Buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { 
                                errorMessage = null
                                showCreateDialog = true 
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF00B4DB),
                                contentColor = Color.White
                            )
                        ) {
                            Text("Create Group ➕", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                        Button(
                            onClick = { 
                                errorMessage = null
                                showJoinDialog = true 
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF00F2FE),
                                contentColor = Color(0xFF0F2027)
                            )
                        ) {
                            Text("Join Group 🤝", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }

                    Text(
                        text = "My Ledger Rooms",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    if (groups.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(20.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("💸", fontSize = 64.sp)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "No active groups yet.\nClick Create or Join above to begin!",
                                    textAlign = TextAlign.Center,
                                    color = Color.White.copy(alpha = 0.7f),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.elevatedCardColors(
                                        containerColor = Color.White.copy(alpha = 0.95f)
                                    ),
                                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(20.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = name,
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF0F2027)
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                SuggestionChip(
                                                    onClick = {},
                                                    label = { Text("👥 ${membersList.size} members") },
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                SuggestionChip(
                                                    onClick = {},
                                                    label = { Text("📄 ${txList.size} txs") },
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                            }
                                            Text(
                                                text = "Group Code: $id",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color.Gray,
                                                modifier = Modifier.padding(top = 4.dp)
                                            )
                                        }
                                        Text("➡️", fontSize = 24.sp, color = Color(0xFF203A43))
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
            title = { Text("Create New Group", fontWeight = FontWeight.Bold, color = Color(0xFF0F2027)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = groupNameInput,
                        onValueChange = { groupNameInput = it },
                        label = { Text("Group Name (e.g. Goa Trip)") },
                        singleLine = true,
                        enabled = !actionLoading,
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = creatorNameInput,
                        onValueChange = { creatorNameInput = it },
                        label = { Text("Your Nickname (e.g. Raj)") },
                        singleLine = true,
                        enabled = !actionLoading,
                        shape = RoundedCornerShape(12.dp)
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
                    enabled = !actionLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF203A43))
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
            title = { Text("Join Existing Group", fontWeight = FontWeight.Bold, color = Color(0xFF0F2027)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = joinGroupIdInput,
                        onValueChange = { joinGroupIdInput = it },
                        label = { Text("Group Code (e.g. 5xJ1...)") },
                        singleLine = true,
                        enabled = !actionLoading,
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = joinNameInput,
                        onValueChange = { joinNameInput = it },
                        label = { Text("Your Nickname (e.g. Sam)") },
                        singleLine = true,
                        enabled = !actionLoading,
                        shape = RoundedCornerShape(12.dp)
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
                    enabled = !actionLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF203A43))
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
