package com.vidanstojic.draftduel.platform

/**
 * Small settings and memories that survive a restart: SharedPreferences on Android,
 * NSUserDefaults on iOS. Values are plain strings and booleans; nothing here is secret.
 */
interface KeyValueStore {
    fun getString(key: String): String?

    /** Null removes the key. */
    fun putString(key: String, value: String?)

    fun getBoolean(key: String, default: Boolean): Boolean

    fun putBoolean(key: String, value: Boolean)
}

/** For previews and tests: forgets everything when the process ends. */
class InMemoryKeyValueStore : KeyValueStore {
    private val values = mutableMapOf<String, Any>()

    override fun getString(key: String): String? = values[key] as? String

    override fun putString(key: String, value: String?) {
        if (value == null) values.remove(key) else values[key] = value
    }

    override fun getBoolean(key: String, default: Boolean): Boolean = values[key] as? Boolean ?: default

    override fun putBoolean(key: String, value: Boolean) {
        values[key] = value
    }
}
