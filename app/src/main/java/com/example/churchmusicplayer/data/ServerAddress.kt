package com.example.churchmusicplayer.data

import android.content.Context
import com.example.churchmusicplayer.BuildConfig

/**
 * Where the media server is, as a setting on this device.
 *
 * The build ships an address, but the address of a machine on a church network
 * is not a fact about this app: a new router, a new Pi, or a changed port would
 * otherwise leave every mounted device unable to connect — and unable to reach
 * the update link either, since that is served by the same server. So it can be
 * corrected on the device, and reverts to the build's address on request.
 */
object ServerAddress {
    private const val PREFS = "server"
    private const val KEY = "url"

    val fromBuild: String = BuildConfig.SERVER_URL

    fun of(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null) ?: fromBuild

    fun set(context: Context, url: String) {
        val cleaned = normalize(url)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().apply {
            if (cleaned == null || cleaned == fromBuild) remove(KEY) else putString(KEY, cleaned)
        }.apply()
    }

    /**
     * What someone typing on a tablet is likely to mean: an address with no
     * scheme is http, and a missing trailing slash is not a mistake worth
     * refusing. Blank clears the override. Null means "do not accept this".
     */
    fun normalize(input: String): String? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return null
        val withScheme = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            trimmed
        } else {
            "http://$trimmed"
        }
        val host = withScheme.removePrefix("http://").removePrefix("https://").substringBefore('/')
        if (host.isEmpty() || host.startsWith(":")) return null
        return if (withScheme.endsWith("/")) withScheme else "$withScheme/"
    }
}
