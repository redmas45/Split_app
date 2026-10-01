package com.example.splitapp.domain

/**
 * Ask for the notification permission only on Android 13+ (older versions have no runtime permission), only if it is
 * not already granted, and only once ever: a second "no" would just be nagging.
 */
fun shouldAskForNotifications(sdkInt: Int, granted: Boolean, alreadyAsked: Boolean): Boolean =
    sdkInt >= 33 && !granted && !alreadyAsked
