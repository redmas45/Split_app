package com.example.splitapp.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationAskTest {
    @Test fun asksOnceOnAndroid13Plus_whenNotGranted_andNeverAskedBefore() =
        assertTrue(shouldAskForNotifications(sdkInt = 33, granted = false, alreadyAsked = false))

    @Test fun neverAsksOnAndroid12AndBelow_noRuntimePermissionExists() {
        assertFalse(shouldAskForNotifications(sdkInt = 32, granted = false, alreadyAsked = false))
        assertFalse(shouldAskForNotifications(sdkInt = 24, granted = false, alreadyAsked = false))
    }

    @Test fun doesNotAskAgain_afterAskingOnce() =
        assertFalse(shouldAskForNotifications(sdkInt = 34, granted = false, alreadyAsked = true))

    @Test fun doesNotAsk_whenAlreadyGranted() =
        assertFalse(shouldAskForNotifications(sdkInt = 34, granted = true, alreadyAsked = false))

    @Test fun laterVersionsBehaveTheSame() =
        assertTrue(shouldAskForNotifications(sdkInt = 36, granted = false, alreadyAsked = false))
}
