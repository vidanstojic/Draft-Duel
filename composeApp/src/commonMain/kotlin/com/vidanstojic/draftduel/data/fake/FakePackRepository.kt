package com.vidanstojic.draftduel.data.fake

import com.vidanstojic.draftduel.domain.model.AthleteCard
import com.vidanstojic.draftduel.domain.model.PoolSource
import com.vidanstojic.draftduel.domain.model.Sport
import com.vidanstojic.draftduel.domain.model.ThemePack
import com.vidanstojic.draftduel.domain.repository.PackRepository
import kotlin.random.Random

/** Hand-written sample packs only. Handy for previews and tests that should not read resources. */
class FakePackRepository(
    private val random: Random = Random.Default,
) : PackRepository {

    private val packs: Map<Sport, ThemePack> = listOf(FakePacks.football, FakePacks.basketball).associateBy { it.sport }

    override suspend fun load() = Unit

    override fun sports(): List<Sport> = listOf(Sport.FOOTBALL, Sport.BASKETBALL, Sport.TENNIS)

    override fun pack(sport: Sport): ThemePack? = packs[sport]

    override fun drawPool(sport: Sport, source: PoolSource, size: Int): List<AthleteCard> {
        val pack = packs[sport] ?: return emptyList()
        return pack.candidatesFor(source, size).shuffled(random).take(size)
    }
}
