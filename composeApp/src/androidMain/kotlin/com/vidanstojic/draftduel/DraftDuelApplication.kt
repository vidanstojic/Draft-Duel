package com.vidanstojic.draftduel

import android.app.Application
import android.content.pm.ApplicationInfo
import android.os.Build
import com.vidanstojic.draftduel.di.AppContainer
import com.vidanstojic.draftduel.di.AppInfo

/** Holds the app-scoped [AppContainer] so it survives activity recreation. */
class DraftDuelApplication : Application() {

    val container: AppContainer by lazy { AppContainer(appInfo = readAppInfo()) }

    private fun readAppInfo(): AppInfo {
        val info = packageManager.getPackageInfo(packageName, 0)
        val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            info.versionCode.toLong()
        }
        return AppInfo(
            versionName = info.versionName ?: "?",
            versionCode = versionCode,
            isDebug = applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0,
        )
    }
}
