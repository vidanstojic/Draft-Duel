package com.vidanstojic.draftduel.data.fake

import com.vidanstojic.draftduel.domain.model.MatchPool
import com.vidanstojic.draftduel.domain.model.PoolSource
import com.vidanstojic.draftduel.domain.model.Theme
import com.vidanstojic.draftduel.domain.model.ThemePack
import com.vidanstojic.draftduel.domain.repository.PackRepository
import kotlin.random.Random

/** Hand-written sample packs only. Handy for previews and tests that should not read resources. */
class FakePackRepository(
    private val random: Random = Random.Default,
) : PackRepository {

    private val packs: Map<Theme, ThemePack> = listOf(FakePacks.football, FakePacks.basketball).associateBy { it.theme }

    override suspend fun load() = Unit

    override fun themes(): List<Theme> = listOf(Theme.FOOTBALL, Theme.BASKETBALL, Theme.TENNIS)

    override fun pack(theme: Theme): ThemePack? = packs[theme]

    override fun drawPool(theme: Theme, source: PoolSource, size: Int): MatchPool? =
        packs[theme]?.drawPool(source, size, random)
}
