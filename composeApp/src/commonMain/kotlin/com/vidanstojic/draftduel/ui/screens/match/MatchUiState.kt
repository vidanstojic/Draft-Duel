package com.vidanstojic.draftduel.ui.screens.match

import androidx.compose.runtime.Immutable
import com.vidanstojic.draftduel.domain.model.GameCard
import com.vidanstojic.draftduel.domain.model.Theme
import com.vidanstojic.draftduel.ui.components.BidOptionUi
import com.vidanstojic.draftduel.ui.components.PileCard

/** One side of the table as seen by the viewer. */
@Immutable
data class PlayerPanelUi(
    val name: String,
    val budget: Int,
    val pile: List<PileCard>,
    /** Known only for the viewer's own cards; null while hidden. */
    val total: Int?,
    /** Card limit of the rule set, or null without one. */
    val slots: Int? = null,
)

enum class Leader { NONE, YOU, OPPONENT }

@Immutable
sealed interface MatchPhaseUi {

    data class Bidding(
        val price: Int,
        val leader: Leader,
        val yourTurn: Boolean,
        val youOpen: Boolean,
        val options: List<BidOptionUi>,
        val canPass: Boolean,
        /** The opponent's budget is the most they can bid. */
        val opponentMaxBid: Int,
        /** Price of handing this card over, when the viewer is an opener who may; null otherwise. */
        val handOverCost: Int? = null,
        /** The first opener paid to hand this card over to the current opener. */
        val handedOver: Boolean = false,
    ) : MatchPhaseUi {
        /** The pass button passes, skips (recipe packs) or hands over (five slots). */
        val passEnabled: Boolean get() = yourTurn && (canPass || handOverCost != null)
    }

    /** One player is on 0 points; the other decides who takes the card for free. */
    data class Choosing(val youChoose: Boolean) : MatchPhaseUi

    /** Recipe packs: the opener skipped the card and nobody gets it. */
    data object Discarded : MatchPhaseUi

    data class Awarded(
        val toYou: Boolean,
        val price: Int,
        /** Shown only when the card went to the viewer. */
        val rating: Int?,
    ) : MatchPhaseUi

    data object Finished : MatchPhaseUi
}

@Immutable
data class MatchUiState(
    val theme: Theme,
    /** The dish to build in recipe packs; null in rating packs. */
    val goal: String?,
    val cardNumber: Int,
    val totalCards: Int,
    val cardsLeft: Int,
    val you: PlayerPanelUi,
    val opponent: PlayerPanelUi,
    val card: GameCard?,
    val cardRatingVisible: Boolean,
    val phase: MatchPhaseUi,
    val secondsLeft: Int,
    val timerProgress: Float,
    val maxBudget: Int,
)
