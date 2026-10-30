package com.vidanstojic.draftduel.domain.repository

import com.vidanstojic.draftduel.domain.match.GameRules
import com.vidanstojic.draftduel.domain.model.MatchPool
import com.vidanstojic.draftduel.domain.model.PoolSource
import com.vidanstojic.draftduel.domain.model.Theme
import com.vidanstojic.draftduel.domain.model.ThemePack

interface PackRepository {

    /** Reads the packs (bundled now, cached downloads later). Called once before the first screen. */
    suspend fun load()

    /** Themes in the order they are offered. A theme without a pack is shown as "coming soon". */
    fun themes(): List<Theme>

    fun pack(theme: Theme): ThemePack?

    /** Shuffled pool for one match. On the server this happens server-side (spec §11). */
    fun drawPool(theme: Theme, source: PoolSource, size: Int = GameRules.POOL_SIZE): MatchPool?
}
