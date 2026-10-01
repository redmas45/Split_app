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
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import com.example.splitapp.data.FirebaseGroup
import com.example.splitapp.data.FirebaseService
import com.example.splitapp.data.NotificationHelper
import com.example.splitapp.domain.CreateOutcome
import com.example.splitapp.theme.BrandCyan
import com.example.splitapp.theme.BrandCyanLight
import com.example.splitapp.theme.BrandGradient
import com.example.splitapp.theme.BrandInk700
import com.example.splitapp.theme.BrandInk900
import com.example.splitapp.theme.BrandMutedText
import com.example.splitapp.domain.JoinChoice
import com.example.splitapp.domain.JoinStep
import com.example.splitapp.domain.createOrQueue
import com.example.splitapp.domain.deleteSummary
import com.example.splitapp.domain.newForeignTransactions
import com.example.splitapp.domain.transactionFromMap
import com.example.splitapp.domain.joinStepFor
import com.example.splitapp.domain.memberFromMap
import com.example.splitapp.domain.normalizeGroupCode
import com.example.splitapp.domain.shouldAskForNotifications
import com.example.splitapp.ui.common.ConfirmDialog
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
    // remember: the flow (and its Firestore listeners) must outlive redraws, not be rebuilt on each one.
    val groupsState = remember { firebaseService.observeUserGroups() }.collectAsState(initial = null)

    // Transaction ids seen per group; a notification only for NEW rows someone else added (not ours, not deletions).
    val lastSeenTxIds = remember { mutableStateMapOf<String, Set<Int>>() }

    LaunchedEffect(groupsState.value) {
        val groups = groupsState.value ?: return@LaunchedEffect
        val myUid = firebaseService.currentUser?.uid
        groups.forEach { group ->
            val members = group.members.mapNotNull { memberFromMap(it) }
            val transactions = group.transactions.mapNotNull { transactionFromMap(it) }
            newForeignTransactions(lastSeenTxIds[group.id], transactions, myUid).forEach { tx ->
                NotificationHelper.showPaymentNotification(
                    context = context,
                    title = "New activity in ${group.name.ifEmpty { "Group" }}",
                    text = deleteSummary(tx, members)
                )
            }
            lastSeenTxIds[group.id] = transactions.map { it.id }.toSet()
        }
    }
    
    // Dialog flags and everything typed survive rotation.
    var showCreateDialog by rememberSaveable { mutableStateOf(false) }
    var showJoinDialog by rememberSaveable { mutableStateOf(false) }
    var showSignOutConfirm by rememberSaveable { mutableStateOf(false) }

    var groupNameInput by rememberSaveable { mutableStateOf("") }
    var creatorNameInput by rememberSaveable { mutableStateOf("") }

    var joinGroupIdInput by rememberSaveable { mutableStateOf("") }
    var joinNameInput by rememberSaveable { mutableStateOf("") }

    // The group the user is about to leave (id + name), if the confirmation is open.
    var leaveGroupId by rememberSaveable { mutableStateOf<String?>(null) }
    var leaveGroupName by rememberSaveable { mutableStateOf("") }

    var actionLoading by remember { mutableStateOf(false) }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    // Notification permission: asked once, with an explanation, the first time the user creates or joins a group
    // (not at launch, before login and with no context). The group opens after the answer.
    var showNotifyRationale by rememberSaveable { mutableStateOf(false) }
    var pendingOpenId by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingOpenName by rememberSaveable { mutableStateOf("") }

    fun openPendingGroup() {
        val id = pendingOpenId ?: return
        pendingOpenId = null
        showNotifyRationale = false
        onGroupSelected(id, pendingOpenName)
    }
    val notificationPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { openPendingGroup() }

    fun openGroupAfterOfferingNotifications(id: String, name: String) {
        val prefs = context.getSharedPreferences("splitshare_prefs", Context.MODE_PRIVATE)
        val granted = Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (shouldAskForNotifications(Build.VERSION.SDK_INT, granted, prefs.getBoolean("asked_notifications", false))) {
            pendingOpenId = id
            pendingOpenName = name
            showNotifyRationale = true
        } else {
            onGroupSelected(id, name)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                            colors = ButtonDefaults.textButtonColors(contentColor = BrandCyanLight) // Accent for admin
                        ) {
                            Text("Admin", fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    IconButton(onClick = { showSignOutConfirm = true }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Sign out",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BrandInk900
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(BrandGradient)
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
                                containerColor = BrandCyan,
                                contentColor = BrandInk900
                            )
                        ) {
                            Text("Create Group", fontWeight = FontWeight.Bold, fontSize = 15.sp)
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
                                containerColor = BrandCyanLight,
                                contentColor = BrandInk900
                            )
                        ) {
                            Text("Join Group", fontWeight = FontWeight.Bold, fontSize = 15.sp)
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
                                val name = group.name.ifEmpty { "Unnamed Group" }
                                GroupCard(
                                    group = group,
                                    onOpen = { onGroupSelected(group.id, name) },
                                    onLeave = {
                                        leaveGroupId = group.id
                                        leaveGroupName = name
                                    }
                                )
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
            title = { Text("Create New Group", fontWeight = FontWeight.Bold) },
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
                            val newGroupId = firebaseService.newGroupId()
                            val groupName = groupNameInput.trim()
                            // Offline, a write only "finishes" once synced: give up waiting after 15 s. The group is
                            // already in the local cache, so we still open it and it syncs when we're back online.
                            val outcome = createOrQueue(15_000) {
                                firebaseService.createGroup(newGroupId, groupName, creatorNameInput.trim())
                            }
                            actionLoading = false
                            when (outcome) {
                                is CreateOutcome.Failed -> errorMessage = outcome.message   // dialog stays open
                                else -> {
                                    showCreateDialog = false
                                    groupNameInput = ""
                                    creatorNameInput = ""
                                    if (outcome == CreateOutcome.PendingOffline) {
                                        launch {
                                            snackbarHostState.showSnackbar("You're offline — the group will sync when you're back online")
                                        }
                                    }
                                    openGroupAfterOfferingNotifications(newGroupId, groupName)
                                }
                            }
                        }
                    },
                    enabled = !actionLoading,
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

    // Join Group Dialog: step 1 the code, step 2 "Which one are you?"
    if (showJoinDialog) {
        var prompt by remember { mutableStateOf<JoinPrompt?>(null) }   // fetched data: after a rotation we restart at step 1
        var selectedMemberId by rememberSaveable { mutableStateOf<Int?>(null) }
        val myUid = firebaseService.currentUser?.uid ?: ""

        fun closeJoinDialog() {
            showJoinDialog = false
            joinGroupIdInput = ""
            joinNameInput = ""
            selectedMemberId = null
            errorMessage = null
        }

        JoinGroupDialog(
            code = joinGroupIdInput,
            onCodeChange = { joinGroupIdInput = it; errorMessage = null },
            prompt = prompt,
            selectedMemberId = selectedMemberId,
            onSelectMember = { selectedMemberId = it; joinNameInput = "" },
            nickname = joinNameInput,
            onNicknameChange = { joinNameInput = it; selectedMemberId = null },
            busy = actionLoading,
            error = errorMessage,
            onNext = {
                val code = normalizeGroupCode(joinGroupIdInput)
                errorMessage = null
                actionLoading = true
                coroutineScope.launch {
                    val found = firebaseService.fetchGroupForJoin(code)
                    found.onFailure {
                        actionLoading = false
                        errorMessage = it.message ?: "Couldn't look that group up."
                    }
                    found.onSuccess { group ->
                        val members = group.members.mapNotNull { memberFromMap(it) }
                        val groupName = group.name.ifEmpty { "this group" }
                        when (val step = joinStepFor(members, myUid)) {
                            is JoinStep.AlreadyMember -> {
                                // Nothing to ask: make sure the group is in my list, then just open it.
                                val rejoined = firebaseService.joinGroup(code, JoinChoice.ClaimMember(step.memberId))
                                actionLoading = false
                                if (rejoined.isSuccess) {
                                    closeJoinDialog()
                                    launch { snackbarHostState.showSnackbar("You're already in this group") }
                                    onGroupSelected(code, groupName)
                                } else {
                                    errorMessage = rejoined.exceptionOrNull()?.message ?: "Failed to open the group"
                                }
                            }
                            is JoinStep.ChooseWho -> {
                                actionLoading = false
                                selectedMemberId = null
                                joinNameInput = ""
                                prompt = JoinPrompt(groupName, step.freeMembers)
                            }
                        }
                    }
                }
            },
            onJoin = { choice ->
                val joining = prompt
                val code = normalizeGroupCode(joinGroupIdInput)
                errorMessage = null
                actionLoading = true
                coroutineScope.launch {
                    val result = firebaseService.joinGroup(code, choice)
                    actionLoading = false
                    if (result.isSuccess) {
                        closeJoinDialog()
                        openGroupAfterOfferingNotifications(code, joining?.groupName ?: "Group")
                    } else {
                        errorMessage = result.exceptionOrNull()?.message ?: "Failed to join group"
                    }
                }
            },
            onBack = {
                prompt = null
                selectedMemberId = null
                joinNameInput = ""
                errorMessage = null
            },
            onDismiss = { showJoinDialog = false }
        )
    }

    // One-time explanation before the system permission prompt. Honest about what it does: there is no background push.
    if (showNotifyRationale) {
        fun markAsked() = context.getSharedPreferences("splitshare_prefs", Context.MODE_PRIVATE)
            .edit { putBoolean("asked_notifications", true) }
        AlertDialog(
            onDismissRequest = {
                markAsked()
                openPendingGroup()
            },
            title = { Text("Stay in the loop?") },
            text = { Text("Get an alert when someone adds an expense to your group (while the app is open).") },
            confirmButton = {
                TextButton(onClick = {
                    markAsked()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        openPendingGroup()   // no runtime permission before Android 13
                    }
                }) { Text("Allow") }
            },
            dismissButton = {
                TextButton(onClick = {
                    markAsked()
                    openPendingGroup()
                }) { Text("Not now") }
            }
        )
    }

    // Sign out
    if (showSignOutConfirm) {
        ConfirmDialog(
            title = "Sign out?",
            message = "You'll need to log in again to see your groups.",
            confirmLabel = "Sign out",
            onConfirm = {
                firebaseService.signOut()
                onSignOut()
            },
            onDismiss = { showSignOutConfirm = false }
        )
    }

    // Leave group: it disappears from my list; my name and past expenses stay, so I can rejoin later.
    leaveGroupId?.let { id ->
        ConfirmDialog(
            title = "Leave \"$leaveGroupName\"?",
            message = "It will disappear from your list. Your name and past expenses stay in the group, and you can rejoin with the group code.",
            confirmLabel = "Leave",
            destructive = true,
            onConfirm = {
                firebaseService.leaveGroup(id).exceptionOrNull()?.let {
                    coroutineScope.launch { snackbarHostState.showSnackbar(it.message ?: "Couldn't leave the group. Please try again.") }
                }
            },
            onDismiss = { leaveGroupId = null }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun GroupCard(group: FirebaseGroup, onOpen: () -> Unit, onLeave: () -> Unit) {
    val name = group.name.ifEmpty { "Unnamed Group" }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = Color.White.copy(alpha = 0.95f)
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(start = 20.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = BrandInk900,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                // Plain labels, not chips: a chip with an empty onClick swallows the tap, so tapping it
                // would not open the group.
                // Each label wraps as a unit, never in the middle ("12 / txs") at large font sizes.
                FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "👥 ${group.members.size} members",
                        style = MaterialTheme.typography.bodyMedium,
                        color = BrandInk700
                    )
                    Text(
                        text = "📄 ${group.transactions.size} txs",
                        style = MaterialTheme.typography.bodyMedium,
                        color = BrandInk700
                    )
                }
                Text(
                    text = "Group Code: ${group.id}",
                    style = MaterialTheme.typography.bodySmall,
                    color = BrandMutedText,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Box {
                var menuOpen by remember { mutableStateOf(false) }
                IconButton(onClick = { menuOpen = true }) {
                    Icon(
                        Icons.Filled.MoreVert,
                        contentDescription = "More options for $name",
                        tint = BrandInk700
                    )
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("Leave group") },
                        onClick = {
                            menuOpen = false
                            onLeave()
                        }
                    )
                }
            }
        }
    }
}
