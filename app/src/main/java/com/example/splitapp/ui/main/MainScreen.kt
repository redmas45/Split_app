package com.example.splitapp.ui.main

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.text.format.DateUtils
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.splitapp.R
import com.example.splitapp.data.FirebaseService
import com.example.splitapp.data.GroupState
import com.example.splitapp.domain.FormResult
import com.example.splitapp.domain.Member
import com.example.splitapp.domain.Transaction
import com.example.splitapp.domain.TransactionDraft
import com.example.splitapp.domain.TxKind
import com.example.splitapp.domain.addedByName
import com.example.splitapp.domain.computeBalances
import com.example.splitapp.domain.deleteMessage
import com.example.splitapp.domain.deleteSummary
import com.example.splitapp.domain.newForeignTransactions
import com.example.splitapp.domain.formatRupees
import com.example.splitapp.domain.getCategoryIcon
import com.example.splitapp.domain.memberFromMap
import com.example.splitapp.domain.memberName
import com.example.splitapp.domain.removalBlockReason
import com.example.splitapp.domain.settle
import com.example.splitapp.domain.splitLabel
import com.example.splitapp.domain.totalExpensePaise
import com.example.splitapp.domain.transactionFromMap
import com.example.splitapp.domain.validateDraft
import com.example.splitapp.ui.common.ConfirmDialog
import kotlinx.coroutines.launch

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
    val snackbarHostState = remember { SnackbarHostState() }

    val groupState = remember(groupId) { firebaseService.observeGroup(groupId) }.collectAsState(initial = GroupState.Loading)
    var currentTab by rememberSaveable { mutableStateOf("Members") }

    val group = (groupState.value as? GroupState.Ready)?.group
    if (group == null) {
        GroupUnavailable(groupState.value, onBackClick, modifier)
        return
    }

    fun showMessage(message: String) {
        coroutineScope.launch { snackbarHostState.showSnackbar(message) }
    }

    // A failed group edit is shown to the user as a Snackbar; returns whether the edit went through.
    fun report(result: Result<Unit>): Boolean {
        result.exceptionOrNull()?.let { showMessage(it.message ?: "Something went wrong. Please try again.") }
        return result.isSuccess
    }

    // Map Firestore document data to domain models
    val members = remember(group) { group.members.mapNotNull { memberFromMap(it) } }
    val transactions = remember(group) { group.transactions.mapNotNull { transactionFromMap(it) } }


    val title = group.name.ifEmpty { groupName }

    // Notify only about transactions someone ELSE added since we last looked: never our own, never deletions, and not
    // the rows that were already there when the screen opened.
    var seenIds by remember { mutableStateOf<Set<Int>?>(null) }
    LaunchedEffect(transactions) {
        val myUid = firebaseService.currentUser?.uid
        newForeignTransactions(seenIds, transactions, myUid).forEach { tx ->
            com.example.splitapp.data.NotificationHelper.showPaymentNotification(
                context = context,
                title = "New activity in $title",
                text = deleteSummary(tx, members)
            )
        }
        seenIds = transactions.map { it.id }.toSet()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(title, fontWeight = FontWeight.Bold)
                        Text(
                            text = "Code: $groupId",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Group ID", groupId)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Group code copied to clipboard!", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(painterResource(R.drawable.ic_content_copy), contentDescription = "Copy group code")
                    }
                    IconButton(onClick = {
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "Join my SplitShare group \"$title\" with code: $groupId")
                        }
                        context.startActivity(Intent.createChooser(send, "Share group code"))
                    }) {
                        Icon(Icons.Filled.Share, contentDescription = "Share group code")
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
                            when (tab) {
                                "Members" -> Icon(Icons.Filled.Person, contentDescription = null)
                                "Ledger" -> Icon(Icons.AutoMirrored.Filled.List, contentDescription = null)
                                else -> Icon(painterResource(R.drawable.ic_bar_chart), contentDescription = null)
                            }
                        },
                        label = { Text(tab) }
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = modifier.fillMaxSize().padding(paddingValues).background(MaterialTheme.colorScheme.background)) {
            when (currentTab) {
                "Members" -> MembersTab(
                    members = members,
                    transactions = transactions,
                    onAddMember = { name -> report(firebaseService.addMember(groupId, name)) },
                    onRemoveMember = { id -> report(firebaseService.removeMember(groupId, id)) },
                    onMessage = ::showMessage
                )
                "Ledger" -> TransactionsTab(
                    members = members,
                    transactions = transactions,
                    onAdd = { draft -> report(firebaseService.addTransaction(groupId, draft)) },
                    onDelete = { txId -> report(firebaseService.deleteTransaction(groupId, txId)) }
                )
                "Summary" -> SummaryTab(members, transactions)
            }
        }
    }
}

@Composable
private fun GroupUnavailable(state: GroupState, onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    val message = when (state) {
        GroupState.NotFound -> "This group no longer exists."
        is GroupState.Failed -> state.message
        else -> null
    }
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (message == null) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                Text(message, style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onBackClick) { Text("Back") }
            }
        }
    }
}

@Composable
fun MembersTab(
    members: List<Member>,
    transactions: List<Transaction>,
    onAddMember: suspend (String) -> Boolean,
    onRemoveMember: suspend (Int) -> Unit,
    onMessage: (String) -> Unit
) {
    var newName by rememberSaveable { mutableStateOf("") }
    var adding by remember { mutableStateOf(false) }
    var pendingRemove by rememberSaveable { mutableStateOf<Int?>(null) }
    val scope = rememberCoroutineScope()

    // The member vanished (removed elsewhere): nothing left to confirm.
    val pending = members.firstOrNull { it.id == pendingRemove }
    LaunchedEffect(pending) { if (pendingRemove != null && pending == null) pendingRemove = null }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Group Members", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(members) { member ->
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(18.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                member.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold,
                                maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (members.size > 1) {
                            IconButton(
                                onClick = {
                                    // Say why straight away (no round trip) when removal isn't allowed; otherwise confirm first.
                                    val reason = removalBlockReason(member, transactions)
                                    if (reason != null) onMessage(reason) else pendingRemove = member.id
                                }
                            ) {
                                Icon(Icons.Filled.Close, contentDescription = "Remove ${member.name}", tint = MaterialTheme.colorScheme.error)
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
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    scope.launch {
                        adding = true
                        if (onAddMember(newName.trim())) newName = ""
                        adding = false
                    }
                },
                enabled = newName.isNotBlank() && !adding && members.size < 30,
                modifier = Modifier.height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (adding) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                else Text("Add", fontWeight = FontWeight.Bold)
            }
        }
    }

    if (pending != null) {
        ConfirmDialog(
            title = "Remove ${pending.name}?",
            message = "${pending.name} will be removed from this group.",
            confirmLabel = "Remove",
            destructive = true,
            onConfirm = { onRemoveMember(pending.id) },
            onDismiss = { pendingRemove = null }
        )
    }
}

private val IntSetSaver = listSaver<Set<Int>, Int>(save = { it.toList() }, restore = { it.toSet() })

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsTab(
    members: List<Member>,
    transactions: List<Transaction>,
    onAdd: suspend (TransactionDraft) -> Boolean,
    onDelete: suspend (Int) -> Unit
) {
    val scope = rememberCoroutineScope()

    // Everything the user typed survives rotation (rememberSaveable).
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var isTransfer by rememberSaveable { mutableStateOf(false) }
    var desc by rememberSaveable { mutableStateOf("") }
    var amount by rememberSaveable { mutableStateOf("") }
    var fromId by rememberSaveable { mutableStateOf<Int?>(null) }
    var toId by rememberSaveable { mutableStateOf<Int?>(null) }
    var splitIds by rememberSaveable(stateSaver = IntSetSaver) { mutableStateOf(emptySet<Int>()) }
    var amountTouched by rememberSaveable { mutableStateOf(false) }
    var descTouched by rememberSaveable { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var pendingDelete by rememberSaveable { mutableStateOf<Int?>(null) }

    fun resetForm() {
        isTransfer = false
        desc = ""
        amount = ""
        fromId = members.firstOrNull()?.id
        toId = members.getOrNull(1)?.id
        splitIds = members.map { it.id }.toSet()   // everyone, by default
        amountTouched = false
        descTouched = false
    }

    // The row vanished (deleted by someone else while the dialog was open): nothing left to confirm.
    val pendingTx = transactions.firstOrNull { it.id == pendingDelete }
    LaunchedEffect(pendingTx) { if (pendingDelete != null && pendingTx == null) pendingDelete = null }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text("Ledger Room", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(16.dp))

            if (transactions.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("No transactions logged yet.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                // Newest first; bottom padding keeps the last row clear of the ➕ button.
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 88.dp)
                ) {
                    items(transactions.asReversed()) { tx ->
                        val icon = if (tx is Transaction.Expense) getCategoryIcon(tx.description) else "💸"
                        val title = if (tx is Transaction.Expense) tx.description.trim().ifEmpty { "Expense" } else "Transfer"
                        val subtitle = when (tx) {
                            is Transaction.Expense -> "Paid by ${memberName(members, tx.payerId)}"
                            is Transaction.Transfer -> "${memberName(members, tx.fromId)} ➡️ ${memberName(members, tx.toId)}"
                        }
                        val meta = listOfNotNull(
                            addedByName(tx, members)?.let { "Added by $it" },
                            tx.createdAt?.let { at ->
                                if (System.currentTimeMillis() - at < DateUtils.MINUTE_IN_MILLIS) "Just now"
                                else DateUtils.getRelativeTimeSpanString(at).toString()
                            }
                        ).joinToString(" · ")

                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            // Top line: icon, what it is, delete. The amount sits on its own line below, so a long amount
                            // or a large font can never squeeze the title (which wraps instead of being cut).
                            Column(modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 12.dp, end = 4.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .background(
                                                color = if (tx is Transaction.Expense) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                                                shape = RoundedCornerShape(12.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(icon, fontSize = 22.sp)
                                    }
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 2, overflow = TextOverflow.Ellipsis
                                        )
                                        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    }
                                    // Never deletes directly: opens the confirmation dialog below.
                                    IconButton(onClick = { pendingDelete = tx.id }) {
                                        Icon(
                                            Icons.Filled.Delete,
                                            contentDescription = "Delete transaction: $title",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(end = 12.dp, top = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                        splitLabel(tx, members)?.let {
                                            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        if (meta.isNotEmpty()) {
                                            Text(meta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    Text(
                                        formatRupees(tx.amountPaise),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (tx is Transaction.Expense) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = {
                resetForm()
                showAddDialog = true
            },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Add transaction")
        }
    }

    if (pendingTx != null) {
        ConfirmDialog(
            title = "Delete this transaction?",
            message = deleteMessage(pendingTx, members),
            confirmLabel = "Delete",
            destructive = true,
            onConfirm = { onDelete(pendingTx.id) },
            onDismiss = { pendingDelete = null }
        )
    }

    if (showAddDialog) {
        val result = validateDraft(
            kind = if (isTransfer) TxKind.Transfer else TxKind.Expense,
            amountText = amount, description = desc, fromId = fromId, toId = toId,
            splitIds = splitIds, memberIds = members.map { it.id }.toSet()
        )
        val errors = (result as? FormResult.Invalid)?.errors

        ModalBottomSheet(onDismissRequest = {
            showAddDialog = false
            resetForm()
        }) {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp)) {
                Text("Add New Transaction", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    FilterChip(
                        selected = !isTransfer,
                        onClick = { isTransfer = false },
                        label = { Text("Expense") },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(
                        selected = isTransfer,
                        onClick = { isTransfer = true },
                        enabled = members.size >= 2,
                        label = { Text("Transfer") },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (members.size < 2) {
                    Text(
                        "Add another member first",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it; amountTouched = true },
                    label = { Text("Amount (₹)") },
                    isError = amountTouched && errors?.amount != null,
                    supportingText = { if (amountTouched) errors?.amount?.let { Text(it) } },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = if (isTransfer) ImeAction.Done else ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                if (!isTransfer) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = desc,
                        onValueChange = { desc = it; descTouched = true },
                        label = { Text("Description (e.g. Dinner, Tickets)") },
                        isError = descTouched && errors?.description != null,
                        supportingText = { if (descTouched) errors?.description?.let { Text(it) } },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Paid By:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    MemberChips(members, selected = { fromId == it }, onClick = { fromId = it })
                    errors?.people?.let { ErrorText(it) }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Split between:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    MemberChips(
                        members,
                        selected = { it in splitIds },
                        onClick = { splitIds = if (it in splitIds) splitIds - it else splitIds + it }
                    )
                    errors?.split?.let { ErrorText(it) }
                } else {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("From (Sender):", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    MemberChips(members, selected = { fromId == it }, onClick = { fromId = it })
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("To (Receiver):", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    MemberChips(members, selected = { toId == it }, onClick = { toId = it })
                    errors?.people?.let { ErrorText(it) }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        val draft = (result as? FormResult.Valid)?.draft ?: return@Button
                        scope.launch {
                            saving = true
                            val saved = onAdd(draft)
                            saving = false
                            if (saved) {            // fields are cleared only after a successful save
                                showAddDialog = false
                                resetForm()
                            }
                        }
                    },
                    enabled = result is FormResult.Valid && !saving,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (saving) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    else Text("Save Transaction", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun MemberChips(members: List<Member>, selected: (Int) -> Boolean, onClick: (Int) -> Unit) {
    LazyRow(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
        items(members) { m ->
            FilterChip(
                selected = selected(m.id),
                onClick = { onClick(m.id) },
                label = { Text(m.name) },
                modifier = Modifier.padding(end = 8.dp)
            )
        }
    }
}

@Composable
private fun ErrorText(message: String) {
    Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
}

@Composable
fun SummaryTab(members: List<Member>, transactions: List<Transaction>) {
    val balances = remember(members, transactions) { computeBalances(members, transactions) }
    val totalExpense = remember(transactions) { totalExpensePaise(transactions) }
    val settlements = remember(balances, members) {
        settle(balances).map { "${memberName(members, it.fromId)} owes ${memberName(members, it.toId)} ${formatRupees(it.paise)}" }
    }
    val maxAbsBalance = remember(balances) { balances.values.maxOfOrNull { kotlin.math.abs(it) } ?: 0L }

    // One scrolling list for the whole tab, so the settle-up part never gets squeezed out with many members.
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text("Summary & Settlements", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }

        // Total Group Expense Display Card
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Total Group Expense", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f))
                    Text(formatRupees(totalExpense), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }

        item {
            Text(
                "Visual Balance Share", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(top = 12.dp)
            )
        }

        // One row per member: balance and a relative progress bar.
        items(members) { member ->
            val balance = balances[member.id] ?: 0L
            val isCreditor = balance > 0
            val isDebtor = balance < 0
            val balanceText = if (isCreditor) "+${formatRupees(balance)}" else formatRupees(balance)

            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            member.name, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = balanceText,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isCreditor) MaterialTheme.colorScheme.tertiary else if (isDebtor) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    val ratio = kotlin.math.abs(balance).toFloat() / (if (maxAbsBalance == 0L) 1L else maxAbsBalance)
                    LinearProgressIndicator(
                        progress = { ratio },
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                        color = if (isCreditor) MaterialTheme.colorScheme.tertiary else if (isDebtor) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                }
            }
        }

        item {
            Text(
                "How to Settle Up:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(top = 12.dp)
            )
        }

        if (settlements.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp)).padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Everyone is settled up! 🎉", color = MaterialTheme.colorScheme.tertiary, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            items(settlements) { s ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("💸", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(s, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }
}
