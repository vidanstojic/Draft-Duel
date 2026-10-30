package com.vidanstojic.draftduel.data.pack

import com.vidanstojic.draftduel.data.fake.FakePacks
import com.vidanstojic.draftduel.domain.model.AthleteCard
import com.vidanstojic.draftduel.domain.model.PoolSource
import com.vidanstojic.draftduel.domain.model.Sport
import com.vidanstojic.draftduel.domain.model.ThemePack
import com.vidanstojic.draftduel.domain.repository.PackRepository
import com.vidanstojic.draftduel.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi
import kotlin.random.Random

/**
 * Packs shipped inside the app (composeResources/files/packs).
 *
 * Football comes from the rating database (`tools/export_pack.py`). Basketball has no database
 * yet, so it stays the hand-written sample. If a bundled file is missing or broken the sample
 * pack is used, so the app still opens.
 */
class BundledPackRepository(
    private val random: Random = Random.Default,
    private val readFile: suspend (path: String) -> ByteArray = ::readResource,
) : PackRepository {

    private var packs: Map<Sport, ThemePack> = samplePacks()

    override suspend fun load() {
        val football = runCatching { PackJson.decode(readFile(FOOTBALL_PATH).decodeToString(), Sport.FOOTBALL) }
            .getOrNull()
            ?.takeIf { it.cards.isNotEmpty() }
        if (football != null) packs = packs + (Sport.FOOTBALL to football)
    }

    override fun sports(): List<Sport> = listOf(Sport.FOOTBALL, Sport.BASKETBALL, Sport.TENNIS)

    override fun pack(sport: Sport): ThemePack? = packs[sport]

    override fun drawPool(sport: Sport, source: PoolSource, size: Int): List<AthleteCard> {
        val pack = packs[sport] ?: return emptyList()
        return pack.candidatesFor(source, size).shuffled(random).take(size)
    }

    private fun samplePacks(): Map<Sport, ThemePack> =
        listOf(FakePacks.football, FakePacks.basketball).associateBy { it.sport }

    companion object {
        const val FOOTBALL_PATH = "files/packs/football.json"
    }
}

@OptIn(ExperimentalResourceApi::class)
private suspend fun readResource(path: String): ByteArray = Res.readBytes(path)
