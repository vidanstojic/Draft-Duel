package com.vidanstojic.draftduel.domain.repository

import com.vidanstojic.draftduel.domain.match.GameRules
import com.vidanstojic.draftduel.domain.model.AthleteCard
import com.vidanstojic.draftduel.domain.model.PoolSource
import com.vidanstojic.draftduel.domain.model.Sport
import com.vidanstojic.draftduel.domain.model.ThemePack

interface PackRepository {

    /** Sports in the order they are offered. A sport without a pack is shown as "coming soon". */
    fun sports(): List<Sport>

    fun pack(sport: Sport): ThemePack?

    /** Shuffled pool for one match. On the server this happens server-side (spec §11). */
    fun drawPool(sport: Sport, source: PoolSource, size: Int = GameRules.POOL_SIZE): List<AthleteCard>
}
