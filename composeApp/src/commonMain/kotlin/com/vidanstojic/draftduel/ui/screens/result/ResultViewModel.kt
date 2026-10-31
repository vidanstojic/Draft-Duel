package com.vidanstojic.draftduel.ui.screens.result

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import com.vidanstojic.draftduel.domain.match.MatchEngine
import com.vidanstojic.draftduel.domain.match.MatchOutcome
import com.vidanstojic.draftduel.domain.match.MatchState
import com.vidanstojic.draftduel.domain.match.OwnedCard
import com.vidanstojic.draftduel.domain.match.Seat
import com.vidanstojic.draftduel.domain.model.GameMode
import com.vidanstojic.draftduel.domain.model.Theme
import com.vidanstojic.draftduel.domain.repository.MatchSessionRepository

@Immutable
data class RosterRowUi(
    val name: String,
    /** Season, country and tag, e.g. "2025/26 · ENGLAND · FWD". */
    val details: String,
    val rating: Int,
    val price: Int,
    val bestBuy: Boolean = false,
)

@Immutable
data class RosterUi(
    val name: String,
    val rows: List<RosterRowUi>,
    val total: Int,
    val spent: Int,
    /** Starting points, for "paid 14 / 20". */
    val budget: Int,
)

enum class ResultOutcome { BOTTOM_WINS, TOP_WINS, DRAW }

@Immutable
data class ResultUiState(
    val theme: Theme,
    /** The dish of a recipe match; null in rating packs. */
    val goal: String? = null,
    /** Bottom of the screen: the local user, or player 1 on a shared device. */
    val bottom: RosterUi,
    val top: RosterUi,
    val outcome: ResultOutcome,
    /** True when the bottom roster is "you" (bot and friend matches). */
    val bottomIsYou: Boolean,
    val bestBuy: RosterRowUi?,
) {
    val margin: Int get() = kotlin.math.abs(bottom.total - top.total)
}

/** Final reveal: every rating becomes visible for both players (spec §6 result screen). */
class ResultViewModel(
    private val sessionRepository: MatchSessionRepository,
) : ViewModel() {

    val mode: GameMode = sessionRepository.setup?.mode ?: GameMode.QUICK_MATCH

    val state: ResultUiState? = sessionRepository.lastFinishedMatch?.toResultUi(bottomIsYou = mode != GameMode.SAME_DEVICE)
}

internal fun MatchState.toResultUi(bottomIsYou: Boolean): ResultUiState {
    val bottomCards = seat(Seat.A).cards
    val best = bottomCards.maxByOrNull { valueFor(it) }
    val outcome = when (MatchEngine.outcome(this)) {
        MatchOutcome.A_WINS -> ResultOutcome.BOTTOM_WINS
        MatchOutcome.B_WINS -> ResultOutcome.TOP_WINS
        MatchOutcome.DRAW -> ResultOutcome.DRAW
    }
    return ResultUiState(
        theme = theme,
        goal = goal,
        bottom = roster(Seat.A, best),
        top = roster(Seat.B, best = null),
        outcome = outcome,
        bottomIsYou = bottomIsYou,
        bestBuy = best?.toRow(isBest = true),
    )
}

private fun MatchState.roster(seat: Seat, best: OwnedCard?): RosterUi {
    val state = seat(seat)
    return RosterUi(
        name = state.name,
        rows = state.cards.sortedByDescending { it.card.rating }.map { it.toRow(isBest = it == best) },
        total = state.total,
        spent = state.cards.sumOf { it.price },
        budget = rules.budget,
    )
}

/** Rating points per spent point; a free card counts as one point spent. */
private fun valueFor(owned: OwnedCard): Double = owned.card.rating.toDouble() / owned.price.coerceAtLeast(1)

private fun OwnedCard.toRow(isBest: Boolean) = RosterRowUi(
    name = card.name,
    details = card.details,
    rating = card.rating,
    price = price,
    bestBuy = isBest,
)
