package com.example.splitapp

import androidx.compose.runtime.*
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.splitapp.data.FirebaseService
import com.example.splitapp.ui.admin.AdminPanelScreen
import com.example.splitapp.ui.group.GroupSelectionScreen
import com.example.splitapp.ui.login.BannedScreen
import com.example.splitapp.ui.login.LoginScreen
import com.example.splitapp.ui.main.MainScreen
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

@Composable
fun MainNavigation() {
  val firebaseService = remember { FirebaseService() }
  val scope = rememberCoroutineScope()

  val startDestination = remember {
    if (firebaseService.isLoggedIn()) GroupSelection else Login
  }

  val backStack = rememberNavBackStack(startDestination)

  // Who is signed in right now. Changes on login, logout and account switch, so the ban watcher below always
  // follows the current user (it used to read the user once, so later logins were never ban-checked).
  val uid by remember { firebaseService.authState() }.collectAsState(initial = firebaseService.currentUser?.uid)

  // Real-time listener for the current user's ban status
  var isUserBanned by remember(uid) { mutableStateOf(false) }

  DisposableEffect(uid) {
    var listener: com.google.firebase.firestore.ListenerRegistration? = null
    uid?.let { currentUid ->
      listener = FirebaseFirestore.getInstance()
        .collection("users").document(currentUid)
        .addSnapshotListener { snapshot, _ ->
          if (snapshot != null && snapshot.exists()) {
            isUserBanned = snapshot.getBoolean("isBanned") ?: false
          }
        }
    }
    onDispose {
      listener?.remove()
    }
  }

  // Ban kick-out: nothing may stay under the banned screen, or Back would bypass the ban.
  LaunchedEffect(isUserBanned) {
    if (isUserBanned) backStack.resetTo(BannedScreen)
  }

  // Signed out (from anywhere, including a session that expired): back to Login with no history.
  LaunchedEffect(uid) {
    if (uid == null && backStack.lastOrNull() != Login) backStack.resetTo(Login)
  }

  NavDisplay(
    backStack = backStack,
    onBack = { backStack.removeLastOrNull() },
    entryProvider =
      entryProvider {
        entry<Login> {
          LoginScreen(
            firebaseService = firebaseService,
            onLoginSuccess = {
              // Check the ban BEFORE showing anything, so a banned user never sees the group list.
              scope.launch {
                val banned = firebaseService.currentUser?.uid?.let { firebaseService.checkIsBanned(it) } ?: false
                backStack.resetTo(if (banned) BannedScreen else GroupSelection)
              }
            }
          )
        }
        entry<GroupSelection> {
          GroupSelectionScreen(
            firebaseService = firebaseService,
            onGroupSelected = { groupId, groupName ->
              backStack.pushIfNotTop(Main(groupId, groupName))
            },
            onSignOut = {
              backStack.resetTo(Login)
            },
            onAdminClick = {
              backStack.add(AdminPanel)
            }
          )
        }
        entry<Main> { key ->
          MainScreen(
            groupId = key.groupId,
            groupName = key.groupName,
            firebaseService = firebaseService,
            onBackClick = {
              backStack.removeLastOrNull()
            }
          )
        }
        entry<AdminPanel> {
          AdminPanelScreen(
            firebaseService = firebaseService,
            onBackClick = {
              backStack.removeLastOrNull()
            }
          )
        }
        entry<BannedScreen> {
          BannedScreen(
            firebaseService = firebaseService,
            onSignOut = {
              backStack.resetTo(Login)
            }
          )
        }
      },
  )
}
