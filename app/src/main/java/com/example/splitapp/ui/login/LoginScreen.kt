package com.example.splitapp.ui.login

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.example.splitapp.R
import com.example.splitapp.data.FirebaseService
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    firebaseService: FirebaseService,
    onLoginSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isSignUp by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Vibrant futuristic gradient background
    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0B0D17),
            Color(0xFF14192D),
            Color(0xFF1E2640)
        )
    )

    // Cyan-Blue premium gradient button brush
    val buttonGradient = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFF00B4DB),
            Color(0xFF00F2FE)
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Section
            Text(
                text = "💸",
                fontSize = 72.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Text(
                text = "SplitShare",
                fontSize = 36.sp,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                letterSpacing = 1.sp
            )
            
            Text(
                text = "Track expenses & settle up dynamically",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.padding(bottom = 36.dp),
                textAlign = TextAlign.Center
            )

            // Glassmorphism Card Wrapper
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.15f),
                                Color.White.copy(alpha = 0.02f)
                            )
                        ),
                        shape = RoundedCornerShape(28.dp)
                    )
                    .background(
                        color = Color.White.copy(alpha = 0.06f),
                        shape = RoundedCornerShape(28.dp)
                    )
                    .padding(28.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isSignUp) "Create Account" else "Welcome Back",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))

                    // Email Input
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Address") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White.copy(alpha = 0.9f),
                            focusedBorderColor = Color(0xFF00B4DB),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedLabelColor = Color(0xFF00B4DB),
                            unfocusedLabelColor = Color.White.copy(alpha = 0.5f),
                            cursorColor = Color(0xFF00B4DB)
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Password Input
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White.copy(alpha = 0.9f),
                            focusedBorderColor = Color(0xFF00B4DB),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedLabelColor = Color(0xFF00B4DB),
                            unfocusedLabelColor = Color.White.copy(alpha = 0.5f),
                            cursorColor = Color(0xFF00B4DB)
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )

                    AnimatedVisibility(
                        visible = errorMessage != null,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        errorMessage?.let { error ->
                            Text(
                                text = error,
                                color = Color(0xFFFF4B4B),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 12.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // Submit Button with Gradient
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .background(
                                brush = buttonGradient,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable(enabled = !isLoading) {
                                if (email.isBlank() || password.isBlank()) {
                                    errorMessage = "Please fill in all fields"
                                    return@clickable
                                }
                                if (password.length < 6) {
                                    errorMessage = "Password must be at least 6 characters"
                                    return@clickable
                                }
                                errorMessage = null
                                isLoading = true
                                coroutineScope.launch {
                                    val result = if (isSignUp) {
                                        firebaseService.signUp(email.trim(), password)
                                    } else {
                                        firebaseService.signIn(email.trim(), password)
                                    }
                                    isLoading = false
                                    if (result.isSuccess) {
                                        onLoginSuccess()
                                    } else {
                                        errorMessage = result.exceptionOrNull()?.localizedMessage ?: "Authentication failed"
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color(0xFF0B0D17),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = if (isSignUp) "GET STARTED" else "LOG IN",
                                color = Color(0xFF0B0D17),
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                fontSize = 15.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("OR", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)

                    Spacer(modifier = Modifier.height(16.dp))

                    // Google Sign In Button
                    OutlinedButton(
                        onClick = {
                            isLoading = true
                            errorMessage = null
                            coroutineScope.launch {
                                try {
                                    val credentialManager = CredentialManager.create(context)
                                    // Make sure to replace this with your actual Web Client ID from Firebase Console
                                    val webClientId = "658669374197-i2f0d12eshrk5j9e7s0240ac9q3lb06t.apps.googleusercontent.com" 
                                    
                                    val googleIdOption = GetGoogleIdOption.Builder()
                                        .setFilterByAuthorizedAccounts(false)
                                        .setServerClientId(webClientId)
                                        .setAutoSelectEnabled(true)
                                        .build()

                                    val request = GetCredentialRequest.Builder()
                                        .addCredentialOption(googleIdOption)
                                        .build()

                                    val result = credentialManager.getCredential(
                                        request = request,
                                        context = context,
                                    )

                                    val credential = result.credential
                                    if (credential is com.google.android.libraries.identity.googleid.GoogleIdTokenCredential) {
                                        val idToken = credential.idToken
                                        val signInResult = firebaseService.signInWithGoogle(idToken)
                                        if (signInResult.isSuccess) {
                                            onLoginSuccess()
                                        } else {
                                            errorMessage = signInResult.exceptionOrNull()?.localizedMessage ?: "Google Sign In Failed"
                                        }
                                    } else {
                                        errorMessage = "Unexpected credential type."
                                    }
                                } catch (e: GetCredentialException) {
                                    errorMessage = "Google Sign In Failed: ${e.message}"
                                } catch (e: Exception) {
                                    errorMessage = "Error: ${e.message}"
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
                    ) {
                        Text("Continue with Google", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Auth State Switcher Text
                    TextButton(
                        onClick = {
                            isSignUp = !isSignUp
                            errorMessage = null
                        }
                    ) {
                        Text(
                            text = if (isSignUp) "Already have an account? Log In" else "New to SplitShare? Create Account",
                            color = Color(0xFF00F2FE),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}
