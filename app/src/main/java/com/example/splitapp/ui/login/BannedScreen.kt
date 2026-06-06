package com.example.splitapp.ui.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.splitapp.data.FirebaseService

@Composable
fun BannedScreen(
    firebaseService: FirebaseService,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0F2027),
            Color(0xFF1C1E24)
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(gradientBrush),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "🚫",
                fontSize = 80.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Text(
                text = "Access Blocked",
                fontSize = 28.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Your account has been restricted or banned by the Super Admin. If you believe this is an error, please contact the administrator.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(36.dp))

            Button(
                onClick = {
                    firebaseService.signOut()
                    onSignOut()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF5252),
                    contentColor = Color.White
                )
            ) {
                Text("LOG OUT & SWITCH ACCOUNT", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
        }
    }
}
