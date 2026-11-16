package com.vidanstojic.draftduel.platform

import android.content.Context
import android.content.SharedPreferences

/** The Android pieces behind [PlatformServices]. */
fun androidPlatformServices(context: Context): PlatformServices = PlatformServices(
    store = SharedPreferencesStore(context.getSharedPreferences("draft_duel", Context.MODE_PRIVATE)),
)

private class SharedPreferencesStore(private val prefs: SharedPreferences) : KeyValueStore {

    override fun getString(key: String): String? = prefs.getString(key, null)

    override fun putString(key: String, value: String?) {
        prefs.edit().apply { if (value == null) remove(key) else putString(key, value) }.apply()
    }

    override fun getBoolean(key: String, default: Boolean): Boolean = prefs.getBoolean(key, default)

    override fun putBoolean(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
    }
}
