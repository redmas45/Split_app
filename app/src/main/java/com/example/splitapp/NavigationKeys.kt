package com.example.splitapp

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object Login : NavKey
@Serializable data object GroupSelection : NavKey
@Serializable data class Main(val groupId: String, val groupName: String) : NavKey
@Serializable data object AdminPanel : NavKey
@Serializable data object BannedScreen : NavKey
