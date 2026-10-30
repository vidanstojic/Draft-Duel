package com.vidanstojic.draftduel.data.pack

import com.vidanstojic.draftduel.data.fake.FakePacks
import com.vidanstojic.draftduel.domain.model.AthleteCard
import com.vidanstojic.draftduel.domain.model.PoolSource
import com.vidanstojic.draftduel.domain.model.Theme
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

    private var packs: Map<Theme, ThemePack> = samplePacks()

    override suspend fun load() {
        val football = runCatching { PackJson.decode(readFile(FOOTBALL_PATH).decodeToString(), Theme.FOOTBALL) }
            .getOrNull()
            ?.takeIf { it.cards.isNotEmpty() }
        if (football != null) packs = packs + (Theme.FOOTBALL to football)
    }

    override fun themes(): List<Theme> = listOf(Theme.FOOTBALL, Theme.BASKETBALL, Theme.TENNIS)

    override fun pack(theme: Theme): ThemePack? = packs[theme]

    override fun drawPool(theme: Theme, source: PoolSource, size: Int): List<AthleteCard> {
        val pack = packs[theme] ?: return emptyList()
        return pack.candidatesFor(source, size).shuffled(random).take(size)
    }

    private fun samplePacks(): Map<Theme, ThemePack> =
        listOf(FakePacks.football, FakePacks.basketball).associateBy { it.theme }

    companion object {
        const val FOOTBALL_PATH = "files/packs/football.json"
    }
}

@OptIn(ExperimentalResourceApi::class)
private suspend fun readResource(path: String): ByteArray = Res.readBytes(path)
