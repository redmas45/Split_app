package com.example.splitapp.ui.login

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.example.splitapp.data.FirebaseService
import com.example.splitapp.data.authErrorMessage
import com.example.splitapp.data.googleSignInMessage
import com.example.splitapp.data.normalizeEmail
import com.example.splitapp.theme.BrandButtonGradient
import com.example.splitapp.theme.BrandCyan
import com.example.splitapp.theme.BrandCyanLight
import com.example.splitapp.theme.BrandGradient
import com.example.splitapp.theme.BrandInk900
import com.example.splitapp.theme.OnBrandError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    firebaseService: FirebaseService,
    onLoginSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    var email by rememberSaveable { mutableStateOf("") }
    // The password is deliberately NOT saved across rotation/process death (instance state is not a safe place for it).
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var isSignUp by rememberSaveable { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var infoMessage by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    fun submit() {
        if (isLoading) return
        if (email.isBlank() || password.isBlank()) {
            errorMessage = "Please fill in all fields"
            infoMessage = null
            return
        }
        if (password.length < 6) {
            errorMessage = "Password must be at least 6 characters"
            infoMessage = null
            return
        }
        errorMessage = null
        infoMessage = null
        isLoading = true
        focusManager.clearFocus()
        coroutineScope.launch {
            val cleaned = normalizeEmail(email)
            val result = if (isSignUp) firebaseService.signUp(cleaned, password) else firebaseService.signIn(cleaned, password)
            isLoading = false
            result.onSuccess { onLoginSuccess() }
                .onFailure { errorMessage = authErrorMessage(it) }
        }
    }

    fun requestPasswordReset() {
        val cleaned = normalizeEmail(email)
        infoMessage = null
        if (cleaned.isEmpty()) {
            errorMessage = "Enter your email above first"
            return
        }
        errorMessage = null
        isLoading = true
        coroutineScope.launch {
            val result = firebaseService.sendPasswordReset(cleaned)
            isLoading = false
            result.onSuccess { infoMessage = "Check your inbox for a password reset link." }
                .onFailure { errorMessage = authErrorMessage(it) }
        }
    }

    // safeDrawingPadding: the screen is edge-to-edge, so this keeps the form clear of the status bar, the navigation
    // bar and the keyboard. The form scrolls, so the buttons stay reachable with the keyboard up and in landscape.
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BrandGradient)
            .safeDrawingPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
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

                    val fieldColors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White.copy(alpha = 0.9f),
                        focusedBorderColor = BrandCyan,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedLabelColor = BrandCyanLight,
                        unfocusedLabelColor = Color.White.copy(alpha = 0.5f),
                        cursorColor = BrandCyan
                    )

                    // Email Input: Next moves on to the password
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Address") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                        colors = fieldColors,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Password Input: Done submits; Show/Hide reveals it
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        singleLine = true,
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        // Plain-text keyboard type while revealed: a field typed as Password keeps announcing itself (and its
                        // text) as masked to screen readers even when the characters are visible.
                        keyboardOptions = KeyboardOptions(
                            keyboardType = if (showPassword) KeyboardType.Text else KeyboardType.Password,
                            autoCorrectEnabled = false,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { submit() }),
                        trailingIcon = {
                            TextButton(onClick = { showPassword = !showPassword }) {
                                Text(if (showPassword) "Hide" else "Show", color = BrandCyanLight, fontWeight = FontWeight.Bold)
                            }
                        },
                        colors = fieldColors,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )

                    if (!isSignUp) {
                        TextButton(
                            onClick = { requestPasswordReset() },
                            enabled = !isLoading,
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Forgot password?", color = BrandCyanLight, fontSize = 13.sp)
                        }
                    }

                    AnimatedVisibility(
                        visible = errorMessage != null || infoMessage != null,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        val error = errorMessage
                        Text(
                            text = error ?: infoMessage.orEmpty(),
                            color = if (error != null) OnBrandError else Color.White,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 12.dp),
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // Submit Button with Gradient
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .background(
                                brush = BrandButtonGradient,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable(enabled = !isLoading, role = Role.Button) { submit() },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = BrandInk900,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = if (isSignUp) "GET STARTED" else "LOG IN",
                                color = BrandInk900,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                fontSize = 15.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("OR", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)

                    Spacer(modifier = Modifier.height(16.dp))

                    // Google Sign In Button (disabled while anything is in flight, so taps cannot stack popups)
                    OutlinedButton(
                        onClick = {
                            isLoading = true
                            errorMessage = null
                            infoMessage = null
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
                                    if (credential is androidx.credentials.CustomCredential &&
                                        credential.type == com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {

                                        val googleIdTokenCredential = com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.createFrom(credential.data)
                                        val idToken = googleIdTokenCredential.idToken
                                        firebaseService.signInWithGoogle(idToken)
                                            .onSuccess { onLoginSuccess() }
                                            .onFailure {
                                                Log.w("LoginScreen", "Firebase rejected the Google sign-in", it)
                                                errorMessage = authErrorMessage(it)
                                            }
                                    } else {
                                        errorMessage = googleSignInMessage(IllegalStateException("unexpected credential type"))
                                    }
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (e: Exception) {
                                    // Cancelling the account picker is not an error: show nothing for that.
                                    Log.w("LoginScreen", "Google credential request failed", e)
                                    errorMessage = googleSignInMessage(e)
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        enabled = !isLoading,
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
                            infoMessage = null
                        }
                    ) {
                        Text(
                            text = if (isSignUp) "Already have an account? Log In" else "New to SplitShare? Create Account",
                            color = BrandCyanLight,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}
