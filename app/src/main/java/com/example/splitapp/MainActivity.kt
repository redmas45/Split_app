package com.example.splitapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.splitapp.data.NotificationHelper
import com.example.splitapp.theme.SplitAppTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Setup local notification channel
    NotificationHelper.createNotificationChannel(this)

    // Request notification permission for Android 13+
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      val launcher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ -> }
      if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
      }
    }

    enableEdgeToEdge()
    setContent {
      SplitAppTheme {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = MaterialTheme.colorScheme.background
        ) {
          MainNavigation()
        }
      }
    }
  }
}
