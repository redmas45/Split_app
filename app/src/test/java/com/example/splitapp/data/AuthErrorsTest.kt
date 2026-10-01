package com.example.splitapp.data

import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialUnknownException
import androidx.credentials.exceptions.NoCredentialException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AuthErrorsTest {
    private val fallback = "Something went wrong. Please try again."

    // ---- authErrorMessage(code) ----

    @Test fun invalidEmail() = assertEquals("That email address doesn't look right.", authErrorMessage("ERROR_INVALID_EMAIL"))

    @Test fun wrongPassword() = assertEquals("Email or password is incorrect.", authErrorMessage("ERROR_WRONG_PASSWORD"))

    @Test fun invalidCredential_readsTheSameAsWrongPassword() =
        assertEquals("Email or password is incorrect.", authErrorMessage("ERROR_INVALID_CREDENTIAL"))

    @Test fun userNotFound() = assertEquals("No account with that email.", authErrorMessage("ERROR_USER_NOT_FOUND"))

    @Test fun emailAlreadyInUse() =
        assertEquals("An account with this email already exists — log in instead.", authErrorMessage("ERROR_EMAIL_ALREADY_IN_USE"))

    @Test fun weakPassword() = assertEquals("Password must be at least 6 characters.", authErrorMessage("ERROR_WEAK_PASSWORD"))

    @Test fun tooManyRequests() =
        assertEquals("Too many attempts — try again in a few minutes.", authErrorMessage("ERROR_TOO_MANY_REQUESTS"))

    @Test fun anythingElse_getsTheFallback() {
        assertEquals(fallback, authErrorMessage("ERROR_SOMETHING_NEW"))
        assertEquals(fallback, authErrorMessage(""))
        assertEquals(fallback, authErrorMessage(null as String?))
    }

    // ---- authErrorMessage(Throwable) ----
    // The FirebaseAuthException / FirebaseNetworkException cases are in androidTest/AuthErrorsDeviceTest:
    // constructing a FirebaseException calls android.text.TextUtils, which does not exist on the plain JVM.

    @Test fun anyOtherException_getsTheFallback_andNeverLeaksItsRawMessage() =
        assertEquals(fallback, authErrorMessage(IllegalStateException("java.net.UnknownHostException: firestore.googleapis.com")))

    // ---- googleSignInMessage ----

    @Test fun googleCancelled_showsNothing() = assertNull(googleSignInMessage(GetCredentialCancellationException("user cancelled")))

    @Test fun googleNoAccountOnTheDevice() =
        assertEquals("No Google account found on this device.", googleSignInMessage(NoCredentialException("none")))

    @Test fun googleOtherCredentialError_isUnavailable() =
        assertEquals("Google sign-in isn't available right now.", googleSignInMessage(GetCredentialUnknownException("boom")))

    @Test fun googleNonCredentialError_isUnavailable() =
        assertEquals("Google sign-in isn't available right now.", googleSignInMessage(IllegalArgumentException("bad token")))

    // ---- normalizeEmail ----

    @Test fun email_isTrimmedAndLowercased() = assertEquals("raj@example.com", normalizeEmail("  Raj@Example.COM \n"))

    @Test fun email_blankStaysEmpty() = assertEquals("", normalizeEmail("   "))
}
