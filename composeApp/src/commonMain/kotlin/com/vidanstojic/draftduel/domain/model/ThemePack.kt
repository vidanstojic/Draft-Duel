package com.vidanstojic.draftduel.domain.model

import com.vidanstojic.draftduel.domain.match.GameRules
import kotlin.random.Random

data class PackCategory(
    val id: String,
    val name: String,
)

/** All cards of one theme, with author-assigned values (spec §5). */
data class ThemePack(
    val id: String,
    val theme: Theme,
    val name: String,
    val version: Int,
    val categories: List<PackCategory>,
    val cards: List<GameCard>,
) {
    val scoring: Scoring get() = theme.scoring

    fun cardsIn(categoryId: String): List<GameCard> = cards.filter { categoryId in it.categoryIds }

    /** Categories that can feed a whole match on their own (spec §5: at least [poolSize] cards). */
    fun playableCategories(poolSize: Int = GameRules.POOL_SIZE): List<PackCategory> =
        categories.filter { cardsIn(it.id).size >= poolSize }

    /**
     * The cards a pool for [source] is drawn from: the category when it has at least [poolSize]
     * cards, otherwise the whole pack. Recipe packs always play one dish.
     */
    fun candidatesFor(source: PoolSource, poolSize: Int = GameRules.POOL_SIZE): List<GameCard> = when (source) {
        is PoolSource.ByCategory -> cardsIn(source.categoryId).takeIf { it.size >= poolSize } ?: cards
        PoolSource.Random, PoolSource.Custom -> cards
    }

    /**
     * Rating packs: [size] random cards from [candidatesFor]. Recipe packs: one dish (the chosen
     * category, or a random one), with every ingredient that belongs plus random wrong ones.
     */
    fun drawPool(
        source: PoolSource,
        size: Int = GameRules.POOL_SIZE,
        random: Random = Random.Default,
        /** Picks [size] of the candidates in auction order; a [ShuffleBag] avoids repeats. */
        pick: (candidates: List<GameCard>, size: Int) -> List<GameCard> = { candidates, n -> candidates.shuffled(random).take(n) },
    ): MatchPool {
        if (scoring == Scoring.RATINGS) {
            val candidates = candidatesFor(source, size)
            return MatchPool(cards = pick(candidates, size), drawnFrom = candidates)
        }
        val playable = playableCategories(size)
        val dish = (source as? PoolSource.ByCategory)?.let { chosen -> playable.firstOrNull { it.id == chosen.categoryId } }
            ?: playable.randomOrNull(random)
            ?: return MatchPool(cards = cards.shuffled(random).take(size), drawnFrom = cards)
        val ingredients = cardsIn(dish.id)
        val (belongs, wrong) = ingredients.partition { it.rating > 0 }
        val cards = (belongs.take(size) + wrong.shuffled(random).take((size - belongs.size).coerceAtLeast(0))).shuffled(random)
        return MatchPool(cards = cards, drawnFrom = ingredients, goal = dish)
    }
}
