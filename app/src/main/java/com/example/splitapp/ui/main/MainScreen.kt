package com.example.splitapp.ui.main

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.splitapp.data.FirebaseService
import kotlinx.coroutines.launch

data class Member(val id: Int, val name: String)

sealed class Transaction {
    abstract val id: Int
    data class Expense(override val id: Int, val description: String, val payerId: Int, val amount: String) : Transaction()
    data class Transfer(override val id: Int, val fromId: Int, val toId: Int, val amount: String) : Transaction()
}

// Automatic icon resolution based on description content
fun getCategoryIcon(desc: String): String {
    val d = desc.lowercase()
    return when {
        d.contains("food") || d.contains("dinner") || d.contains("lunch") || d.contains("cafe") || d.contains("drink") || d.contains("eat") || d.contains("restaurant") || d.contains("snack") || d.contains("tea") -> "🍔"
        d.contains("cab") || d.contains("taxi") || d.contains("fuel") || d.contains("car") || d.contains("bus") || d.contains("travel") || d.contains("flight") || d.contains("trip") || d.contains("metro") || d.contains("train") -> "🚗"
        d.contains("shopping") || d.contains("dress") || d.contains("gift") || d.contains("grocer") || d.contains("mart") || d.contains("buy") || d.contains("store") -> "🛍️"
        d.contains("movie") || d.contains("ticket") || d.contains("show") || d.contains("game") || d.contains("play") || d.contains("ent") -> "🎟️"
        d.contains("rent") || d.contains("bill") || d.contains("elect") || d.contains("wifi") || d.contains("stay") || d.contains("hotel") || d.contains("room") -> "🏠"
        else -> "📄"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    groupId: String,
    groupName: String,
    firebaseService: FirebaseService,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    val groupState = remember(groupId) { firebaseService.observeGroup(groupId) }.collectAsState(initial = null)
    var currentTab by remember { mutableStateOf("Members") }

    val group = groupState.value

    if (group == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    // Map Firestore document data to domain models
    val members = group.members.mapNotNull { map ->
        val id = (map["id"] as? Number)?.toInt()
        val name = map["name"] as? String
        if (id != null && name != null) Member(id, name) else null
    }

    val transactions = group.transactions.mapNotNull { map ->
        val id = (map["id"] as? Number)?.toInt() ?: return@mapNotNull null
        val type = map["type"] as? String ?: return@mapNotNull null
        val amount = map["amount"] as? String ?: return@mapNotNull null
        when (type) {
            "expense" -> {
                val description = map["description"] as? String ?: ""
                val payerId = (map["payerId"] as? Number)?.toInt() ?: return@mapNotNull null
                Transaction.Expense(id, description, payerId, amount)
            }
            "transfer" -> {
                val fromId = (map["fromId"] as? Number)?.toInt() ?: return@mapNotNull null
                val toId = (map["toId"] as? Number)?.toInt() ?: return@mapNotNull null
                Transaction.Transfer(id, fromId, toId, amount)
            }
            else -> null
        }
    }

    val previousTxCount = remember { mutableIntStateOf(-1) }
    
    LaunchedEffect(transactions.size) {
        if (previousTxCount.intValue != -1 && transactions.size > previousTxCount.intValue) {
            val latestTx = transactions.lastOrNull()
            if (latestTx != null) {
                val text = when (latestTx) {
                    is Transaction.Expense -> "New Expense: ₹${latestTx.amount} for ${latestTx.description}"
                    is Transaction.Transfer -> "New Transfer: ₹${latestTx.amount}"
                }
                com.example.splitapp.data.NotificationHelper.showPaymentNotification(
                    context = context,
                    title = "New Activity in $groupName",
                    text = text
                )
            }
        }
        previousTxCount.intValue = transactions.size
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(groupName, fontWeight = FontWeight.Bold)
                        Text(
                            text = "Code: $groupId", 
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Text("⬅️", fontSize = 20.sp)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Group ID", groupId)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Group code copied to clipboard!", Toast.LENGTH_SHORT).show()
                    }) {
                        Text("📋", fontSize = 20.sp)
                    }
                },
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
        Box(modifier = modifier.fillMaxSize().padding(paddingValues).background(Color(0xFFF7F9FC))) {
            when (currentTab) {
                "Members" -> MembersTab(
                    members = members,
                    onMembersChange = { updatedMembers ->
                        coroutineScope.launch {
                            val membersMap = updatedMembers.map { mapOf("id" to it.id, "name" to it.name) }
                            firebaseService.updateGroupData(groupId, membersMap, group.transactions)
                        }
                    }
                )
                "Ledger" -> TransactionsTab(
                    members = members,
                    transactions = transactions,
                    onTxChange = { updatedTxs ->
                        coroutineScope.launch {
                            val txsMap = updatedTxs.map { tx ->
                                when (tx) {
                                    is Transaction.Expense -> mapOf(
                                        "id" to tx.id,
                                        "type" to "expense",
                                        "description" to tx.description,
                                        "payerId" to tx.payerId,
                                        "amount" to tx.amount
                                    )
                                    is Transaction.Transfer -> mapOf(
                                        "id" to tx.id,
                                        "type" to "transfer",
                                        "fromId" to tx.fromId,
                                        "toId" to tx.toId,
                                        "amount" to tx.amount
                                    )
                                }
                            }
                            firebaseService.updateGroupData(groupId, group.members, txsMap)
                        }
                    }
                )
                "Summary" -> SummaryTab(members, transactions)
            }
        }
    }
}

@Composable
fun MembersTab(
    members: List<Member>,
    onMembersChange: (List<Member>) -> Unit
) {
    var newName by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Group Members", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color(0xFF0F2027))
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(members) { member ->
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = Color.White)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(18.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("👤", fontSize = 16.sp)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(member.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                        }
                        if (members.size > 1) {
                            IconButton(
                                onClick = { onMembersChange(members.filter { it.id != member.id }) }
                            ) {
                                Text("❌", fontSize = 14.sp)
                            }
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
                label = { Text("Add group member") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    if (newName.isNotBlank() && members.size < 30) {
                        val maxId = members.maxOfOrNull { it.id } ?: 0
                        onMembersChange(members + Member(maxId + 1, newName.trim()))
                        newName = ""
                    }
                },
                modifier = Modifier.height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Add", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsTab(
    members: List<Member>,
    transactions: List<Transaction>,
    onTxChange: (List<Transaction>) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var txType by remember { mutableStateOf("Expense") }
    
    var desc by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var payerId by remember { mutableIntStateOf(members.firstOrNull()?.id ?: -1) }
    var receiverId by remember { mutableIntStateOf(members.getOrNull(1)?.id ?: -1) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text("Ledger Room", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color(0xFF0F2027))
            Spacer(modifier = Modifier.height(16.dp))

            if (transactions.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("No transactions logged yet.", color = Color.Gray, style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(transactions) { tx ->
                        val icon = if (tx is Transaction.Expense) getCategoryIcon(tx.description) else "💸"
                        
                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = Color.White
                            )
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .background(
                                                color = if (tx is Transaction.Expense) Color(0xFFE8F5E9) else Color(0xFFE3F2FD),
                                                shape = RoundedCornerShape(12.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(icon, fontSize = 22.sp)
                                    }
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column {
                                        if (tx is Transaction.Expense) {
                                            Text(tx.description, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0F2027))
                                            val payerName = members.find { it.id == tx.payerId }?.name ?: "Unknown"
                                            Text("Paid by $payerName", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                        } else if (tx is Transaction.Transfer) {
                                            val fromName = members.find { it.id == tx.fromId }?.name ?: "Unknown"
                                            val toName = members.find { it.id == tx.toId }?.name ?: "Unknown"
                                            Text("Transfer Payment", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0F2027))
                                            Text("$fromName ➡️ $toName", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                        }
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "₹${if (tx is Transaction.Expense) tx.amount else (tx as Transaction.Transfer).amount}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (tx is Transaction.Expense) Color(0xFFC62828) else Color(0xFF1565C0)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    IconButton(
                                        onClick = { onTxChange(transactions.filter { it.id != tx.id }) }
                                    ) {
                                        Text("❌", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        FloatingActionButton(
            onClick = { 
                if (members.isNotEmpty()) {
                    payerId = members.first().id
                    receiverId = members.getOrNull(1)?.id ?: members.first().id
                }
                showAddDialog = true 
            },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            containerColor = Color(0xFF203A43),
            contentColor = Color.White
        ) {
            Text("➕", fontSize = 22.sp)
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
                        label = { Text("Expense 🛒") },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(
                        selected = txType == "Transfer",
                        onClick = { txType = "Transfer" },
                        label = { Text("Transfer 💸") },
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                
                if (txType == "Expense") {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = desc,
                        onValueChange = { desc = it },
                        label = { Text("Description (e.g. Dinner, Tickets)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Paid By:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    LazyRow(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
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
                    Text("From (Sender):", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    LazyRow(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
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
                    Text("To (Receiver):", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    LazyRow(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
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
                            val maxId = transactions.maxOfOrNull { it.id } ?: 0
                            val nextId = maxId + 1
                            if (txType == "Expense" && desc.isNotBlank()) {
                                onTxChange(transactions + Transaction.Expense(nextId, desc, payerId, amount))
                                showAddDialog = false
                            } else if (txType == "Transfer" && payerId != receiverId) {
                                onTxChange(transactions + Transaction.Transfer(nextId, payerId, receiverId, amount))
                                showAddDialog = false
                            }
                        }
                        amount = ""
                        desc = ""
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save Transaction", fontWeight = FontWeight.Bold)
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
        Text("Summary & Settlements", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color(0xFF0F2027))
        Spacer(modifier = Modifier.height(16.dp))

        // Total Group Expense Display Card
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = Color(0xFF203A43))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Total Group Expense", style = MaterialTheme.typography.titleMedium, color = Color.White.copy(alpha = 0.8f))
                Text("₹%.2f".format(totalExpense), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold, color = Color.White)
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        Text("Visual Balance Share", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF0F2027))
        Spacer(modifier = Modifier.height(8.dp))

        // Visual progress bars card for debts and credits
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val maxAbsBalance = balances.values.map { kotlin.math.abs(it) }.maxOrNull() ?: 1.0
                
                members.forEach { member ->
                    val balance = balances[member.id] ?: 0.0
                    val isCreditor = balance > 0.01
                    val isDebtor = balance < -0.01
                    val balanceText = if (isCreditor) "+₹%.2f".format(balance) else if (isDebtor) "-₹%.2f".format(-balance) else "₹0.00"
                    
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(member.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(
                                text = balanceText,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isCreditor) Color(0xFF2E7D32) else if (isDebtor) Color(0xFFC62828) else Color.Gray,
                                fontSize = 14.sp
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        // Relative progress bar representation
                        val ratio = (kotlin.math.abs(balance) / if (maxAbsBalance == 0.0) 1.0 else maxAbsBalance).toFloat()
                        LinearProgressIndicator(
                            progress = { ratio },
                            modifier = Modifier.fillMaxWidth().height(8.dp),
                            color = if (isCreditor) Color(0xFF2E7D32) else if (isDebtor) Color(0xFFD32F2F) else Color.LightGray,
                            trackColor = Color(0xFFEEEEEE),
                            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        Text("How to Settle Up:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF0F2027))
        Spacer(modifier = Modifier.height(8.dp))
        
        if (settlements.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth().background(Color.White, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("Everyone is settled up! 🎉", color = Color(0xFF2E7D32), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(settlements) { s ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("💸", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(s, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F2027))
                        }
                    }
                }
            }
        }
    }
}
