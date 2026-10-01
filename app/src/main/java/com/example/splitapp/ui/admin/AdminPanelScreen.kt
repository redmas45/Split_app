package com.example.splitapp.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.splitapp.data.FirebaseService
import com.example.splitapp.domain.adminLedgerLine
import com.example.splitapp.domain.adminMetrics
import com.example.splitapp.domain.formatRupees
import com.example.splitapp.ui.common.ConfirmDialog
import kotlinx.coroutines.launch

@Suppress("UNCHECKED_CAST")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelScreen(
    firebaseService: FirebaseService,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // remember: otherwise every redraw (e.g. each letter typed in a search box) re-attaches both Firestore listeners.
    val usersState = remember { firebaseService.getAllUsers() }.collectAsState(initial = null)
    val groupsState = remember { firebaseService.getAllGroups() }.collectAsState(initial = null)

    var userSearchQuery by remember { mutableStateOf("") }
    var groupSearchQuery by remember { mutableStateOf("") }

    var selectedGroup by remember { mutableStateOf<Map<String, Any>?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Admin Dashboard", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            val users = usersState.value
            val groups = groupsState.value

            if (users == null || groups == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                // Only recomputed when the data changes, not on every keystroke in the search boxes.
                val metrics = remember(groups) { adminMetrics(groups) }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Metrics Row
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MetricCard(title = "Total Users", value = users.size.toString(), modifier = Modifier.weight(1f))
                        MetricCard(title = "Total Groups", value = groups.size.toString(), modifier = Modifier.weight(1f))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MetricCard(title = "Transactions", value = metrics.transactionCount.toString(), modifier = Modifier.weight(1f))
                        // Expenses only: transfers are money moving between friends, not spending.
                        MetricCard(title = "Total spending", value = formatRupees(metrics.spendingPaise), modifier = Modifier.weight(1f))
                    }

                    TabSection(
                        firebaseService = firebaseService,
                        users = users,
                        groups = groups,
                        userSearchQuery = userSearchQuery,
                        onUserSearchChange = { userSearchQuery = it },
                        groupSearchQuery = groupSearchQuery,
                        onGroupSearchChange = { groupSearchQuery = it },
                        onGroupClick = { selectedGroup = it },
                        onMessage = { message -> scope.launch { snackbarHostState.showSnackbar(message) } }
                    )
                }
            }
        }
    }

    // View Transactions Dialog: names, not ids; unreadable rows are shown as such.
    selectedGroup?.let { group ->
        val name = group["name"] as? String ?: "Unnamed Group"
        val members = group["members"] as? List<Map<String, Any>> ?: emptyList()
        val txs = group["transactions"] as? List<Map<String, Any>> ?: emptyList()
        AlertDialog(
            onDismissRequest = { selectedGroup = null },
            title = { Text("Ledger: $name", fontWeight = FontWeight.Bold) },
            text = {
                if (txs.isEmpty()) {
                    Text("No transactions logged in this group yet.")
                } else {
                    Box(modifier = Modifier.heightIn(max = 400.dp)) {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(txs.asReversed()) { tx ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Text(
                                        text = adminLedgerLine(tx, members),
                                        modifier = Modifier.padding(12.dp),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { selectedGroup = null }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
        }
    }
}

@Suppress("UNCHECKED_CAST")
@Composable
fun TabSection(
    firebaseService: FirebaseService,
    users: List<Map<String, Any>>,
    groups: List<Map<String, Any>>,
    userSearchQuery: String,
    onUserSearchChange: (String) -> Unit,
    groupSearchQuery: String,
    onGroupSearchChange: (String) -> Unit,
    onGroupClick: (Map<String, Any>) -> Unit,
    onMessage: (String) -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    // Moderation is destructive and affects other people: always confirm first, then report the outcome.
    var pendingBan by remember { mutableStateOf<Triple<String, String, Boolean>?>(null) }   // uid, email, currentlyBanned
    var pendingDeleteGroup by remember { mutableStateOf<Pair<String, String>?>(null) }      // id, name

    Column(modifier = Modifier.fillMaxWidth()) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Users (${users.size})") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Groups (${groups.size})") })
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (selectedTab == 0) {
            // Users list
            OutlinedTextField(
                value = userSearchQuery,
                onValueChange = onUserSearchChange,
                label = { Text("Search users by email") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                shape = RoundedCornerShape(12.dp)
            )

            val filteredUsers = users.filter {
                val email = it["email"] as? String ?: ""
                email.contains(userSearchQuery, ignoreCase = true)
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filteredUsers) { user ->
                    val email = user["email"] as? String ?: "No Email"
                    val uid = user["uid"] as? String ?: ""
                    val userGroups = user["groups"] as? List<*> ?: emptyList<Any>()
                    val isBanned = user["isBanned"] as? Boolean ?: false
                    val isSuperAdmin = email == com.example.splitapp.data.ADMIN_EMAIL

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(email, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f))
                                if (isSuperAdmin) {
                                    Text(
                                        text = "Super Admin",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                } else if (isBanned) {
                                    Text(
                                        text = "Banned",
                                        color = MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("UID: $uid", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Groups Joined: ${userGroups.size}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                            if (!isSuperAdmin) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = { pendingBan = Triple(uid, email, isBanned) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isBanned) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                                        contentColor = if (isBanned) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onError
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Text(if (isBanned) "Unban" else "Ban User", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Groups list
            OutlinedTextField(
                value = groupSearchQuery,
                onValueChange = onGroupSearchChange,
                label = { Text("Search groups by name/ID") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                shape = RoundedCornerShape(12.dp)
            )

            val filteredGroups = groups.filter {
                val name = it["name"] as? String ?: ""
                val id = it["id"] as? String ?: ""
                name.contains(groupSearchQuery, ignoreCase = true) || id.contains(groupSearchQuery, ignoreCase = true)
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filteredGroups) { group ->
                    val name = group["name"] as? String ?: "Unnamed Group"
                    val id = group["id"] as? String ?: ""
                    val members = group["members"] as? List<Map<String, Any>> ?: emptyList()

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("ID: $id", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(6.dp))

                            Text("Members:", fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodySmall)
                            Text(
                                text = members.joinToString { it["name"] as? String ?: "" },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { onGroupClick(group) },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Text("View Ledger", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = { pendingDeleteGroup = id to name },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Text("Delete Group", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    pendingBan?.let { (uid, email, isBanned) ->
        ConfirmDialog(
            title = if (isBanned) "Unban $email?" else "Ban $email?",
            message = if (isBanned) "They will be able to use SplitShare again."
            else "They will be blocked from SplitShare straight away, even if they are signed in right now.",
            confirmLabel = if (isBanned) "Unban" else "Ban",
            destructive = !isBanned,
            onConfirm = {
                val result = firebaseService.banUser(uid, !isBanned)
                onMessage(
                    if (result.isSuccess) (if (isBanned) "$email unbanned" else "$email banned")
                    else "Couldn't update $email. Check your connection and try again."
                )
            },
            onDismiss = { pendingBan = null }
        )
    }

    pendingDeleteGroup?.let { (id, name) ->
        ConfirmDialog(
            title = "Delete \"$name\"?",
            message = "This deletes the group and all its transactions for everyone, and removes it from every member's list. This can't be undone.",
            confirmLabel = "Delete",
            destructive = true,
            onConfirm = {
                val result = firebaseService.deleteGroup(id)
                onMessage(
                    if (result.isSuccess) "\"$name\" deleted"
                    else "Couldn't delete \"$name\". Check your connection and try again."
                )
            },
            onDismiss = { pendingDeleteGroup = null }
        )
    }
}
