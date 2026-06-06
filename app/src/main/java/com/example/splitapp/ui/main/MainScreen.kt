package com.example.splitapp.ui.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.NavKey

data class Member(val id: Int, val name: String)

sealed class Transaction {
    abstract val id: Int
    data class Expense(override val id: Int, val description: String, val payerId: Int, val amount: String) : Transaction()
    data class Transfer(override val id: Int, val fromId: Int, val toId: Int, val amount: String) : Transaction()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onItemClick: (NavKey) -> Unit,
    modifier: Modifier = Modifier
) {
    var members by remember { mutableStateOf(listOf(Member(1, "Person A"), Member(2, "Person B"))) }
    var nextMemberId by remember { mutableIntStateOf(3) }
    var transactions by remember { mutableStateOf<List<Transaction>>(emptyList()) }
    var nextTxId by remember { mutableIntStateOf(1) }

    var currentTab by remember { mutableStateOf("Members") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Expense Splitter", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        bottomBar = {
            NavigationBar {
                listOf("Members", "Ledger", "Summary").forEach { tab ->
                    NavigationBarItem(
                        selected = currentTab == tab,
                        onClick = { currentTab = tab },
                        icon = {
                            Text(
                                when (tab) {
                                    "Members" -> "👥"
                                    "Ledger" -> "💸"
                                    else -> "📊"
                                }, fontSize = 24.sp
                            )
                        },
                        label = { Text(tab) }
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = modifier.fillMaxSize().padding(paddingValues)) {
            when (currentTab) {
                "Members" -> MembersTab(members, { members = it }, { nextMemberId++ }, nextMemberId)
                "Ledger" -> TransactionsTab(members, transactions, { transactions = it }, { nextTxId++ }, nextTxId)
                "Summary" -> SummaryTab(members, transactions)
            }
        }
    }
}

@Composable
fun MembersTab(
    members: List<Member>,
    onMembersChange: (List<Member>) -> Unit,
    onNextId: () -> Unit,
    nextId: Int
) {
    var newName by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Group Members", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(members) { member ->
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(member.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        if (members.size > 2) {
                            Text(
                                "❌",
                                modifier = Modifier.clickable {
                                    onMembersChange(members.filter { it.id != member.id })
                                }.padding(4.dp)
                            )
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                modifier = Modifier.weight(1f),
                label = { Text("New member name") },
                singleLine = true
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    if (newName.isNotBlank() && members.size < 10) {
                        onMembersChange(members + Member(nextId, newName.trim()))
                        onNextId()
                        newName = ""
                    }
                },
                modifier = Modifier.height(56.dp)
            ) {
                Text("Add")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsTab(
    members: List<Member>,
    transactions: List<Transaction>,
    onTxChange: (List<Transaction>) -> Unit,
    onNextId: () -> Unit,
    nextId: Int
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var txType by remember { mutableStateOf("Expense") }
    
    var desc by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var payerId by remember { mutableIntStateOf(members.firstOrNull()?.id ?: -1) }
    var receiverId by remember { mutableIntStateOf(members.getOrNull(1)?.id ?: -1) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text("Ledger", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            if (transactions.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("No transactions yet.", color = Color.Gray)
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(transactions) { tx ->
                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = if (tx is Transaction.Expense) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    if (tx is Transaction.Expense) {
                                        Text("🛒 ${tx.description}", fontWeight = FontWeight.Bold)
                                        val payerName = members.find { it.id == tx.payerId }?.name ?: "Unknown"
                                        Text("Paid by $payerName", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    } else if (tx is Transaction.Transfer) {
                                        val fromName = members.find { it.id == tx.fromId }?.name ?: "Unknown"
                                        val toName = members.find { it.id == tx.toId }?.name ?: "Unknown"
                                        Text("💸 Transfer", fontWeight = FontWeight.Bold)
                                        Text("$fromName ➡️ $toName", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    }
                                }
                                Text(
                                    "₹${if (tx is Transaction.Expense) tx.amount else (tx as Transaction.Transfer).amount}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (tx is Transaction.Expense) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "❌",
                                    modifier = Modifier.clickable {
                                        onTxChange(transactions.filter { it.id != tx.id })
                                    }.padding(4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
        
        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) {
            Text("➕", fontSize = 24.sp)
        }
    }

    if (showAddDialog) {
        ModalBottomSheet(onDismissRequest = { showAddDialog = false }) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text("Add New Transaction", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(modifier = Modifier.fillMaxWidth()) {
                    FilterChip(
                        selected = txType == "Expense",
                        onClick = { txType = "Expense" },
                        label = { Text("Expense") },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(
                        selected = txType == "Transfer",
                        onClick = { txType = "Transfer" },
                        label = { Text("Transfer") },
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                
                if (txType == "Expense") {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = desc,
                        onValueChange = { desc = it },
                        label = { Text("Description (e.g. Dinner, Tickets)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Paid By:", style = MaterialTheme.typography.bodyMedium)
                    LazyRow(modifier = Modifier.fillMaxWidth()) {
                        items(members) { m ->
                            FilterChip(
                                selected = payerId == m.id,
                                onClick = { payerId = m.id },
                                label = { Text(m.name) },
                                modifier = Modifier.padding(end = 8.dp)
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("From (Sender):", style = MaterialTheme.typography.bodyMedium)
                    LazyRow(modifier = Modifier.fillMaxWidth()) {
                        items(members) { m ->
                            FilterChip(
                                selected = payerId == m.id,
                                onClick = { payerId = m.id },
                                label = { Text(m.name) },
                                modifier = Modifier.padding(end = 8.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("To (Receiver):", style = MaterialTheme.typography.bodyMedium)
                    LazyRow(modifier = Modifier.fillMaxWidth()) {
                        items(members) { m ->
                            FilterChip(
                                selected = receiverId == m.id,
                                onClick = { receiverId = m.id },
                                label = { Text(m.name) },
                                modifier = Modifier.padding(end = 8.dp)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        val amt = amount.toDoubleOrNull()
                        if (amt != null && amt > 0) {
                            if (txType == "Expense" && desc.isNotBlank()) {
                                onTxChange(transactions + Transaction.Expense(nextId, desc, payerId, amount))
                                onNextId()
                                showAddDialog = false
                            } else if (txType == "Transfer" && payerId != receiverId) {
                                onTxChange(transactions + Transaction.Transfer(nextId, payerId, receiverId, amount))
                                onNextId()
                                showAddDialog = false
                            }
                        }
                        amount = ""
                        desc = ""
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text("Save Transaction")
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun SummaryTab(members: List<Member>, transactions: List<Transaction>) {
    val balances = mutableMapOf<Int, Double>()
    members.forEach { balances[it.id] = 0.0 }
    
    var totalExpense = 0.0

    transactions.forEach { tx ->
        when (tx) {
            is Transaction.Expense -> {
                val amt = tx.amount.toDoubleOrNull() ?: 0.0
                totalExpense += amt
                balances[tx.payerId] = (balances[tx.payerId] ?: 0.0) + amt
                
                val split = amt / members.size
                members.forEach { m ->
                    balances[m.id] = (balances[m.id] ?: 0.0) - split
                }
            }
            is Transaction.Transfer -> {
                val amt = tx.amount.toDoubleOrNull() ?: 0.0
                balances[tx.fromId] = (balances[tx.fromId] ?: 0.0) + amt
                balances[tx.toId] = (balances[tx.toId] ?: 0.0) - amt
            }
        }
    }

    val debtors = balances.filter { it.value < -0.01 }.map { Pair(it.key, -it.value) }.toMutableList()
    val creditors = balances.filter { it.value > 0.01 }.map { Pair(it.key, it.value) }.toMutableList()

    debtors.sortByDescending { it.second }
    creditors.sortByDescending { it.second }

    val settlements = mutableListOf<String>()
    var d = 0
    var c = 0

    while (d < debtors.size && c < creditors.size) {
        val debtor = debtors[d]
        val creditor = creditors[c]

        val amount = minOf(debtor.second, creditor.second)
        if (amount > 0.01) {
            val dName = members.find { it.id == debtor.first }?.name ?: "Unknown"
            val cName = members.find { it.id == creditor.first }?.name ?: "Unknown"
            settlements.add("$dName owes $cName ₹%.2f".format(amount))
        }

        debtors[d] = Pair(debtor.first, debtor.second - amount)
        creditors[c] = Pair(creditor.first, creditor.second - amount)

        if (debtors[d].second < 0.01) d++
        if (creditors[c].second < 0.01) c++
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Summary & Settlements", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Total Group Expense", style = MaterialTheme.typography.titleMedium)
                Text("₹%.2f".format(totalExpense), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        Text("How to Settle Up:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        
        if (settlements.isEmpty()) {
            Text("Everyone is settled up! 🎉", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyLarge)
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(settlements) { s ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Text(s, modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}
