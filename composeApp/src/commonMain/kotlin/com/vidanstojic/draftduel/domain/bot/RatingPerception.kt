package com.vidanstojic.draftduel.domain.bot

import com.vidanstojic.draftduel.domain.match.GameRules
import com.vidanstojic.draftduel.domain.model.AthleteCard
import kotlin.random.Random

/**
 * How well a bot "knows" athletes. Ratings are hidden during bidding, so every bot bids on its own
 * guess: the true rating, shifted by its biases and blurred by up to [noise] points either way.
 */
data class RatingPerception(
    val noise: Int,
    /** Added per position code, e.g. a fan who overrates forwards: "FWD" to +5. */
    val positionBias: Map<String, Int> = emptyMap(),
    /** Added to athletes rated [starThreshold] or more, i.e. famous names look even better. */
    val starBonus: Int = 0,
    val starThreshold: Int = STAR_RATING,
) {
    init {
        require(noise >= 0) { "noise must not be negative" }
    }

    fun perceive(card: AthleteCard, random: Random): Double {
        val bias = (positionBias[card.position] ?: 0) + if (card.rating >= starThreshold) starBonus else 0
        val blur = if (noise == 0) 0 else random.nextInt(from = -noise, until = noise + 1)
        return (card.rating + bias + blur).coerceIn(GameRules.MIN_RATING, GameRules.MAX_RATING).toDouble()
    }

    companion object {
        const val STAR_RATING = 85
    }
}
