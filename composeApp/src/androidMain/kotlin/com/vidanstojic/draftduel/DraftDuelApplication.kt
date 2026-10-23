package com.vidanstojic.draftduel

import android.app.Application
import com.vidanstojic.draftduel.di.AppContainer

/** Holds the app-scoped [AppContainer] so it survives activity recreation. */
class DraftDuelApplication : Application() {

    val container: AppContainer by lazy { AppContainer() }
}
