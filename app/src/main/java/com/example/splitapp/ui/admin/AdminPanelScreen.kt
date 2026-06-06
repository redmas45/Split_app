package com.example.splitapp.ui.admin

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.splitapp.data.FirebaseService
import kotlinx.coroutines.flow.map

@Suppress("UNCHECKED_CAST", "DEPRECATION")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelScreen(
    firebaseService: FirebaseService,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val usersState = firebaseService.getAllUsers().collectAsState(initial = null)
    val groupsState = firebaseService.getAllGroups().collectAsState(initial = null)

    var userSearchQuery by remember { mutableStateOf("") }
    var groupSearchQuery by remember { mutableStateOf("") }
    
    var selectedGroupTransactions by remember { mutableStateOf<List<Map<String, Any>>?>(null) }
    var selectedGroupName by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Dashboard", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Text("⬅️", fontSize = 20.sp)
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
                .background(Color(0xFFF7F9FC))
        ) {
            val users = usersState.value
            val groups = groupsState.value

            if (users == null || groups == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                // Calculate metrics
                val totalUsers = users.size
                val totalGroups = groups.size
                
                var totalTransactionsCount = 0
                var totalVolume = 0.0

                groups.forEach { group ->
                    val txList = group["transactions"] as? List<Map<String, Any>> ?: emptyList()
                    totalTransactionsCount += txList.size
                    txList.forEach { tx ->
                        val amount = (tx["amount"] as? String)?.toDoubleOrNull() ?: 0.0
                        totalVolume += amount
                    }
                }

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
                        MetricCard(title = "Total Users", value = totalUsers.toString(), modifier = Modifier.weight(1f))
                        MetricCard(title = "Total Groups", value = totalGroups.toString(), modifier = Modifier.weight(1f))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MetricCard(title = "Transactions", value = totalTransactionsCount.toString(), modifier = Modifier.weight(1f))
                        MetricCard(title = "Volume (₹)", value = "%.2f".format(totalVolume), modifier = Modifier.weight(1f))
                    }

                    TabSection(
                        users = users,
                        groups = groups,
                        userSearchQuery = userSearchQuery,
                        onUserSearchChange = { userSearchQuery = it },
                        groupSearchQuery = groupSearchQuery,
                        onGroupSearchChange = { groupSearchQuery = it },
                        onGroupClick = { txs, name ->
                            selectedGroupTransactions = txs
                            selectedGroupName = name
                        }
                    )
                }
            }
        }
    }

    // View Transactions Dialog
    if (selectedGroupTransactions != null) {
        AlertDialog(
            onDismissRequest = { selectedGroupTransactions = null },
            title = { Text("Ledger: $selectedGroupName", fontWeight = FontWeight.Bold) },
            text = {
                val txs = selectedGroupTransactions ?: emptyList()
                if (txs.isEmpty()) {
                    Text("No transactions logged in this group yet.", color = Color.Gray)
                } else {
                    Box(modifier = Modifier.heightIn(max = 400.dp)) {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(txs) { tx ->
                                val type = tx["type"] as? String ?: "expense"
                                val amount = tx["amount"] as? String ?: "0"
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (type == "expense") Color(0xFFF1F8E9) else Color(0xFFE8F5E9)
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        if (type == "expense") {
                                            val desc = tx["description"] as? String ?: ""
                                            val payerId = (tx["payerId"] as? Number)?.toInt() ?: -1
                                            Text("🛒 Expense: $desc", fontWeight = FontWeight.Bold)
                                            Text("Payer ID: $payerId", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                        } else {
                                            val fromId = (tx["fromId"] as? Number)?.toInt() ?: -1
                                            val toId = (tx["toId"] as? Number)?.toInt() ?: -1
                                            Text("💸 Transfer", fontWeight = FontWeight.Bold)
                                            Text("Sender: $fromId ➡️ Receiver: $toId", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Amount: ₹$amount", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { selectedGroupTransactions = null }) {
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
            containerColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, style = MaterialTheme.typography.bodySmall, color = Color.Gray, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color(0xFF203A43), textAlign = TextAlign.Center)
        }
    }
}

@Suppress("UNCHECKED_CAST", "DEPRECATION")
@Composable
fun TabSection(
    users: List<Map<String, Any>>,
    groups: List<Map<String, Any>>,
    userSearchQuery: String,
    onUserSearchChange: (String) -> Unit,
    groupSearchQuery: String,
    onGroupSearchChange: (String) -> Unit,
    onGroupClick: (List<Map<String, Any>>, String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxWidth()) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = Color(0xFF203A43)
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

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(email, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("UID: $uid", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            Text("Groups Joined: ${userGroups.size}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
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
                    val transactions = group["transactions"] as? List<Map<String, Any>> ?: emptyList()

                    Card(
                        modifier = Modifier.fillMaxWidth().clickable {
                            onGroupClick(transactions, name)
                        },
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("🔍 View Ledger", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("ID: $id", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            Spacer(modifier = Modifier.height(6.dp))
                            
                            Text("Members:", fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodySmall)
                            Text(
                                text = members.joinToString { it["name"] as? String ?: "" },
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        }
    }
}
