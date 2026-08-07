package com.example.churchmusicplayer.data

import android.content.Context
import android.os.Build
import android.provider.Settings

/**
 * What this device calls itself, for the admin to read.
 *
 * Every panel used to introduce itself with the same built-in string, so two
 * tablets in two rooms were indistinguishable in a log or an admin view, and
 * naming a newly installed one meant building the app again. The device already
 * has a name — the one it shows on Wi-Fi and Bluetooth — so it is used, and
 * falls back to the model when a device does not expose one.
 */
object DeviceName {
    fun of(context: Context): String {
        val resolver = context.contentResolver
        val chosen = runCatching { Settings.Global.getString(resolver, "device_name") }.getOrNull()
            ?: runCatching { Settings.Secure.getString(resolver, "bluetooth_name") }.getOrNull()
        return chosen?.takeIf { it.isNotBlank() } ?: "${Build.MANUFACTURER} ${Build.MODEL}".trim()
    }
}
