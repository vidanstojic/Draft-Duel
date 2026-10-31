package com.vidanstojic.draftduel.ui.screens.match

import com.vidanstojic.draftduel.domain.match.GameRules
import com.vidanstojic.draftduel.domain.match.MatchEngine
import com.vidanstojic.draftduel.domain.match.MatchPhase
import com.vidanstojic.draftduel.domain.match.MatchState
import com.vidanstojic.draftduel.domain.match.Seat
import com.vidanstojic.draftduel.domain.match.SeatState
import com.vidanstojic.draftduel.ui.components.BidOptionUi
import com.vidanstojic.draftduel.ui.components.PileCard
import kotlin.math.ceil

/**
 * Projects the match onto one viewer. Hidden-rating rule: the viewer sees ratings of the cards
 * they won (when [revealOwnRatings]) and never the opponent's until the result screen.
 */
fun MatchState.toUi(
    viewer: Seat,
    millisLeft: Long,
    revealOwnRatings: Boolean = true,
): MatchUiState {
    val phaseUi = when (val current = phase) {
        is MatchPhase.Bidding -> MatchPhaseUi.Bidding(
            price = current.price,
            leader = when (current.leader) {
                null -> Leader.NONE
                viewer -> Leader.YOU
                else -> Leader.OPPONENT
            },
            yourTurn = current.toAct == viewer,
            youOpen = current.opener == viewer,
            options = GameRules.BID_STEPS.map { step ->
                BidOptionUi(
                    step = step,
                    resultingPrice = current.price + step,
                    enabled = MatchEngine.canRaise(this, viewer, step),
                )
            },
            canPass = MatchEngine.canPass(this, viewer),
            opponentMaxBid = seat(viewer.other).budget,
            handOverCost = if (MatchEngine.canHandOver(this, viewer)) MatchEngine.handOverCost(this, viewer) else null,
            handedOver = current.handedOver,
        )
        is MatchPhase.Choosing -> MatchPhaseUi.Choosing(youChoose = current.chooser == viewer)
        is MatchPhase.Awarded -> MatchPhaseUi.Awarded(
            toYou = current.winner == viewer,
            price = current.price,
            rating = if (current.winner == viewer && revealOwnRatings) currentCard?.rating else null,
        )
        MatchPhase.Discarded -> MatchPhaseUi.Discarded
        MatchPhase.Finished -> MatchPhaseUi.Finished
    }
    val awardedToViewer = (phase as? MatchPhase.Awarded)?.winner == viewer
    val turnMillis = GameRules.TURN_SECONDS * 1_000L
    return MatchUiState(
        theme = theme,
        goal = goal,
        cardNumber = (roundIndex + 1).coerceAtMost(pool.size),
        totalCards = pool.size,
        cardsLeft = cardsLeft,
        you = seat(viewer).toPanel(faceUp = revealOwnRatings, includeTotal = revealOwnRatings, slots = ruleSet.slots),
        opponent = seat(viewer.other).toPanel(faceUp = false, includeTotal = false, slots = ruleSet.slots),
        card = currentCard,
        cardRatingVisible = awardedToViewer && revealOwnRatings,
        phase = phaseUi,
        secondsLeft = ceil(millisLeft / 1_000.0).toInt().coerceIn(0, GameRules.TURN_SECONDS),
        timerProgress = (millisLeft.toFloat() / turnMillis).coerceIn(0f, 1f),
        maxBudget = GameRules.STARTING_BUDGET,
    )
}

private fun SeatState.toPanel(faceUp: Boolean, includeTotal: Boolean, slots: Int?): PlayerPanelUi = PlayerPanelUi(
    name = name,
    budget = budget,
    pile = cards.mapIndexed { index, owned ->
        if (faceUp) {
            PileCard.Face(
                rating = owned.card.rating,
                jerseyNumber = owned.card.avatar.jerseyNumber,
                highlighted = index == cards.lastIndex,
            )
        } else {
            PileCard.Back(paidPrice = owned.price)
        }
    },
    total = if (includeTotal) total else null,
    slots = slots,
)
