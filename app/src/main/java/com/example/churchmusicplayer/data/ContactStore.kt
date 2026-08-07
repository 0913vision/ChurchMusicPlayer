package com.example.churchmusicplayer.data

import android.content.Context

/**
 * The last helpline the server gave, kept on the device.
 *
 * The number arrives in the handshake, so a panel that cannot reach the server
 * would have nothing to show — which is the moment someone most needs it. So it
 * is remembered from the last time the server was reachable, and a later
 * handshake replaces it.
 */
object ContactStore {
    private const val PREFS = "contact"
    private const val NAME = "name"
    private const val PHONE = "phone"

    fun remember(context: Context, contact: Helpline.Known) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(NAME, contact.name)
            .putString(PHONE, contact.phone)
            .apply()
    }

    fun last(context: Context): Helpline {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val name = prefs.getString(NAME, null)
        val phone = prefs.getString(PHONE, null)
        return if (name.isNullOrBlank() || phone.isNullOrBlank()) Helpline.Unknown else Helpline.Known(name, phone)
    }
}
