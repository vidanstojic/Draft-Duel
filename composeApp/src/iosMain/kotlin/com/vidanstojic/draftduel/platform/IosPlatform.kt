package com.vidanstojic.draftduel.platform

import platform.Foundation.NSUserDefaults

/** The iOS pieces behind [PlatformServices]. */
fun iosPlatformServices(): PlatformServices = PlatformServices(
    store = UserDefaultsStore(NSUserDefaults.standardUserDefaults),
)

private class UserDefaultsStore(private val defaults: NSUserDefaults) : KeyValueStore {

    override fun getString(key: String): String? = defaults.stringForKey(key)

    override fun putString(key: String, value: String?) {
        if (value == null) defaults.removeObjectForKey(key) else defaults.setObject(value, forKey = key)
    }

    override fun getBoolean(key: String, default: Boolean): Boolean =
        if (defaults.objectForKey(key) == null) default else defaults.boolForKey(key)

    override fun putBoolean(key: String, value: Boolean) {
        defaults.setBool(value, forKey = key)
    }
}
