package com.vidanstojic.draftduel.domain.model

import com.vidanstojic.draftduel.domain.bot.SimPacks
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ShuffleBagTest {

    private val cards = SimPacks.football.cards.take(25)

    @Test
    fun noCardRepeats_untilEveryCardWasPlayed() {
        val bag = ShuffleBag()
        val random = Random(1)
        val firstTwo = (1..2).flatMap { bag.draw("FOOTBALL", cards, 10, random) }
        assertEquals(20, firstTwo.toSet().size, "two matches of 10 from 25 cards share no card")
        val third = bag.draw("FOOTBALL", cards, 10, random)
        assertEquals(10, third.toSet().size)
        // The last 5 fresh cards are all in the third match.
        assertTrue(third.containsAll(cards - firstTwo.toSet()))
        // A new bag starts: the next match avoids the third one.
        val fourth = bag.draw("FOOTBALL", cards, 10, random)
        assertTrue(fourth.none { it in third })
    }

    @Test
    fun everyCardComesUp_overManyMatches() {
        val bag = ShuffleBag()
        val random = Random(2)
        val counts = mutableMapOf<GameCard, Int>()
        repeat(50) { bag.draw("FOOTBALL", cards, 10, random).forEach { counts[it] = (counts[it] ?: 0) + 1 } }
        assertEquals(cards.size, counts.size)
        // 500 cards over 25: about 20 each. Refills at the end of a bag cost a little evenness.
        assertTrue(counts.values.min() >= 14 && counts.values.max() <= 26, "uneven: ${counts.values.sorted()}")
    }

    @Test
    fun categoryPools_shareTheThemeBag() {
        val store = InMemorySeenCardsStore()
        val bag = ShuffleBag(store)
        val random = Random(3)
        val all = bag.draw("FOOTBALL", cards, 10, random)
        val fromOthers = bag.draw("FOOTBALL", cards.filter { it !in all }.take(8) + all.take(7), 5, random)
        assertTrue(fromOthers.none { it in all })
        assertEquals(15, store.load("FOOTBALL").size)
    }

    @Test
    fun packDrawPool_usesThePicker() {
        val bag = ShuffleBag()
        val random = Random(4)
        val first = SimPacks.football.drawPool(PoolSource.Random, 10, random) { c, n -> bag.draw("F", c, n, random) }
        val second = SimPacks.football.drawPool(PoolSource.Random, 10, random) { c, n -> bag.draw("F", c, n, random) }
        assertTrue(first.cards.none { it in second.cards })
    }
}
