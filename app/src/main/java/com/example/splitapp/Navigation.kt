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

@Composable
fun MainNavigation() {
  val firebaseService = remember { FirebaseService() }
  
  val startDestination = remember {
    if (firebaseService.isLoggedIn()) GroupSelection else Login
  }
  
  val backStack = rememberNavBackStack(startDestination)
  
  // Real-time listener for current user's ban status
  var isUserBanned by remember { mutableStateOf(false) }
  val currentUser = firebaseService.currentUser

  LaunchedEffect(currentUser) {
    if (currentUser != null) {
      val listener = FirebaseFirestore.getInstance()
        .collection("users").document(currentUser.uid)
        .addSnapshotListener { snapshot, _ ->
          if (snapshot != null && snapshot.exists()) {
            isUserBanned = snapshot.getBoolean("isBanned") ?: false
          }
        }
      // Clean up listener when user signs out or scope closes
      return@LaunchedEffect
    } else {
      isUserBanned = false
    }
  }

  // Handle immediate ban kick out
  LaunchedEffect(isUserBanned) {
    if (isUserBanned) {
      backStack.add(BannedScreen)
      // Prevent user from clicking back from the banned screen
      backStack.remove(GroupSelection)
      backStack.remove(Login)
    }
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
              backStack.add(GroupSelection)
              backStack.remove(Login)
            }
          )
        }
        entry<GroupSelection> {
          GroupSelectionScreen(
            firebaseService = firebaseService,
            onGroupSelected = { groupId, groupName ->
              backStack.add(Main(groupId, groupName))
            },
            onSignOut = {
              backStack.add(Login)
              backStack.remove(GroupSelection)
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
              backStack.add(Login)
              backStack.remove(BannedScreen)
            }
          )
        }
      },
  )
}
