package com.vidanstojic.draftduel.di

/** Build facts the platform entry point knows and shared code needs (shown to testers, feature gates). */
data class AppInfo(
    val versionName: String,
    val versionCode: Long,
    /** Debug builds show work in progress; release builds hide it. */
    val isDebug: Boolean,
) {
    /** "v0.1.0 (1)", so testers can say which build they played. */
    val label: String get() = "v$versionName ($versionCode)"

    companion object {
        /** Previews, tests and platforms that do not report their version. */
        val DEVELOPMENT = AppInfo(versionName = "dev", versionCode = 0, isDebug = true)
    }
}

/** What a build offers. Release builds only show modes that work end to end. */
data class Features(
    /** "Play a friend" needs the server (spec §7); until then the invite is a mock-up. */
    val friendMatches: Boolean,
) {
    companion object {
        fun forBuild(appInfo: AppInfo) = Features(friendMatches = appInfo.isDebug)
    }
}
