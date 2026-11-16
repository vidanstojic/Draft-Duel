package com.vidanstojic.draftduel.domain.model

import kotlin.random.Random

/** Remembers which cards a player has already met, per theme, across matches and restarts. */
interface SeenCardsStore {
    fun load(key: String): Set<String>

    fun save(key: String, ids: Set<String>)
}

class InMemorySeenCardsStore : SeenCardsStore {
    private val seen = mutableMapOf<String, Set<String>>()

    override fun load(key: String): Set<String> = seen[key].orEmpty()

    override fun save(key: String, ids: Set<String>) {
        seen[key] = ids
    }
}

/**
 * Draws match pools without repeats: a card comes back only after every other card of the same
 * pool has been played (a "shuffle bag"). Plain random draws from a pack of 50 bring the same
 * names back within two or three matches, which testers noticed.
 *
 * One bag per theme ([key]). A pool by category only uses that category's cards from the bag, so
 * a card met in a random pool also counts as met in its category.
 */
class ShuffleBag(private val store: SeenCardsStore = InMemorySeenCardsStore()) {

    fun draw(key: String, candidates: List<GameCard>, size: Int, random: Random): List<GameCard> {
        val candidateIds = candidates.mapTo(HashSet()) { it.id }
        val seen = store.load(key)
        val (met, fresh) = candidates.partition { it.id in seen }
        val picked = if (fresh.size >= size) {
            fresh.shuffled(random).take(size)
        } else {
            // The bag of these candidates is empty: play the last fresh cards, fill up from the
            // rest and start a new bag in which only this match counts as met.
            fresh + met.shuffled(random).take(size - fresh.size)
        }
        val pickedIds = picked.mapTo(HashSet()) { it.id }
        val updated = if (fresh.size >= size) seen + pickedIds else seen - candidateIds + pickedIds
        store.save(key, updated)
        return picked.shuffled(random)
    }
}
