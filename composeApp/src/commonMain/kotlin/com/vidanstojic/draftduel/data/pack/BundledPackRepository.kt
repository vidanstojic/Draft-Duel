package com.vidanstojic.draftduel.data.pack

import com.vidanstojic.draftduel.data.fake.FakePacks
import com.vidanstojic.draftduel.domain.model.MatchPool
import com.vidanstojic.draftduel.domain.model.PoolSource
import com.vidanstojic.draftduel.domain.model.ShuffleBag
import com.vidanstojic.draftduel.domain.model.Theme
import com.vidanstojic.draftduel.domain.model.ThemePack
import com.vidanstojic.draftduel.domain.repository.PackRepository
import com.vidanstojic.draftduel.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi
import kotlin.random.Random

/**
 * Packs shipped inside the app (composeResources/files/packs).
 *
 * Football comes from the rating database (`tools/export_pack.py`), basketball from the NBA and
 * EuroLeague tables (`tools/build_basketball_pack.py`); movies and food are written by hand. A
 * missing or broken file leaves that theme on its sample pack (or "coming soon"), so the app
 * still opens. Pools come from a [ShuffleBag], so cards do not repeat until the pool is used up.
 */
class BundledPackRepository(
    private val random: Random = Random.Default,
    private val readFile: suspend (path: String) -> ByteArray = ::readResource,
    private val shuffleBag: ShuffleBag = ShuffleBag(),
) : PackRepository {

    private var packs: Map<Theme, ThemePack> = samplePacks()

    override suspend fun load() {
        for ((theme, path) in FILES) {
            val pack = runCatching { PackJson.decode(readFile(path).decodeToString(), theme) }
                .getOrNull()
                ?.takeIf { it.cards.isNotEmpty() }
            if (pack != null) packs = packs + (theme to pack)
        }
    }

    override fun themes(): List<Theme> = listOf(Theme.FOOTBALL, Theme.BASKETBALL, Theme.MOVIES, Theme.FOOD, Theme.TENNIS)

    override fun pack(theme: Theme): ThemePack? = packs[theme]

    override fun drawPool(theme: Theme, source: PoolSource, size: Int): MatchPool? =
        packs[theme]?.drawPool(source, size, random) { candidates, n -> shuffleBag.draw(theme.name, candidates, n, random) }

    private fun samplePacks(): Map<Theme, ThemePack> =
        listOf(FakePacks.football, FakePacks.basketball).associateBy { it.theme }

    companion object {
        const val FOOTBALL_PATH = "files/packs/football.json"
        const val BASKETBALL_PATH = "files/packs/basketball.json"
        const val MOVIES_PATH = "files/packs/movies.json"
        const val FOOD_PATH = "files/packs/food.json"

        private val FILES = listOf(
            Theme.FOOTBALL to FOOTBALL_PATH,
            Theme.BASKETBALL to BASKETBALL_PATH,
            Theme.MOVIES to MOVIES_PATH,
            Theme.FOOD to FOOD_PATH,
        )
    }
}

@OptIn(ExperimentalResourceApi::class)
private suspend fun readResource(path: String): ByteArray = Res.readBytes(path)
