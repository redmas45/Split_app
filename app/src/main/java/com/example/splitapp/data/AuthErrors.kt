package com.example.splitapp.data

import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException

private const val GENERIC_AUTH_ERROR = "Something went wrong. Please try again."

/** Plain-words message for a `FirebaseAuthException.errorCode`. Unknown codes get a generic line, never raw SDK text. */
fun authErrorMessage(code: String?): String = when (code) {
    "ERROR_INVALID_EMAIL" -> "That email address doesn't look right."
    "ERROR_WRONG_PASSWORD", "ERROR_INVALID_CREDENTIAL" -> "Email or password is incorrect."
    "ERROR_USER_NOT_FOUND" -> "No account with that email."
    "ERROR_EMAIL_ALREADY_IN_USE" -> "An account with this email already exists — log in instead."
    "ERROR_WEAK_PASSWORD" -> "Password must be at least 6 characters."
    "ERROR_TOO_MANY_REQUESTS" -> "Too many attempts — try again in a few minutes."
    else -> GENERIC_AUTH_ERROR
}

fun authErrorMessage(e: Throwable): String = when (e) {
    is FirebaseNetworkException -> "No internet connection."
    is FirebaseAuthException -> authErrorMessage(e.errorCode)
    else -> GENERIC_AUTH_ERROR
}

/** Message for a failed Google credential request, or null when the user simply cancelled (show nothing). */
fun googleSignInMessage(e: Throwable): String? = when (e) {
    is GetCredentialCancellationException -> null
    is NoCredentialException -> "No Google account found on this device."
    else -> "Google sign-in isn't available right now."
}

fun normalizeEmail(input: String): String = input.trim().lowercase()
