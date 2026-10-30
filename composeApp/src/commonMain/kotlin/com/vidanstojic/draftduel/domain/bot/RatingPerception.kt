package com.vidanstojic.draftduel.domain.bot

import com.vidanstojic.draftduel.domain.model.GameCard
import kotlin.random.Random

/**
 * How well a bot "knows" the cards. Values are hidden during bidding, so every bot bids on its own
 * guess: the true value, shifted by its biases and blurred by up to [noise] points either way,
 * kept inside the range the pack can have.
 */
data class RatingPerception(
    val noise: Int,
    /** Added per position code, e.g. a fan who overrates forwards: "FWD" to +5. */
    val positionBias: Map<String, Int> = emptyMap(),
    /** Added to cards at or above [PoolStats.starLine], i.e. famous names look even better. */
    val starBonus: Int = 0,
) {
    init {
        require(noise >= 0) { "noise must not be negative" }
    }

    fun perceive(card: GameCard, stats: PoolStats, random: Random): Double {
        val bias = (positionBias[card.position] ?: 0) + if (card.rating >= stats.starLine) starBonus else 0
        val blur = if (noise == 0) 0 else random.nextInt(from = -noise, until = noise + 1)
        return (card.rating + bias + blur).coerceIn(stats.min, stats.max).toDouble()
    }
}
