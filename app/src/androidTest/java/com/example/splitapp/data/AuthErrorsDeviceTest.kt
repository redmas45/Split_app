package com.example.splitapp.data

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException
import org.junit.Assert.assertEquals
import org.junit.Test

/** Needs a device: Firebase exceptions call android.text.TextUtils when constructed, which the plain JVM lacks. */
class AuthErrorsDeviceTest {
    @Test fun anAuthException_isMappedByItsErrorCode() =
        assertEquals("No account with that email.", authErrorMessage(FirebaseAuthException("ERROR_USER_NOT_FOUND", "raw firebase text")))

    @Test fun anAuthException_withAnUnknownCode_getsTheFallbackNotItsRawText() =
        assertEquals("Something went wrong. Please try again.", authErrorMessage(FirebaseAuthException("ERROR_SOMETHING_NEW", "internal detail")))

    @Test fun aNetworkException_saysThereIsNoConnection() =
        assertEquals("No internet connection.", authErrorMessage(FirebaseNetworkException("A network error occurred")))
}
