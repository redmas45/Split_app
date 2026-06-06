package com.example.splitapp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.splitapp.data.FirebaseService
import com.example.splitapp.ui.admin.AdminPanelScreen
import com.example.splitapp.ui.group.GroupSelectionScreen
import com.example.splitapp.ui.login.LoginScreen
import com.example.splitapp.ui.main.MainScreen

@Composable
fun MainNavigation() {
  val firebaseService = remember { FirebaseService() }
  
  val startDestination = remember {
    if (firebaseService.isLoggedIn()) GroupSelection else Login
  }
  
  val backStack = rememberNavBackStack(startDestination)

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
      },
  )
}
