package com.example.splitapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.splitapp.data.NotificationHelper
import com.example.splitapp.theme.SplitAppTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Setup local notification channel. (The notification permission is NOT requested here any more: asking at
    // first launch, before login and with no explanation, mostly gets a "no". It is asked once, with context,
    // after the user first creates or joins a group.)
    NotificationHelper.createNotificationChannel(this)

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
