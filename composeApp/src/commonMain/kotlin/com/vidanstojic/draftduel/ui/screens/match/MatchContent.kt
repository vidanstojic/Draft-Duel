package com.vidanstojic.draftduel.ui.screens.match

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vidanstojic.draftduel.resources.Res
import com.vidanstojic.draftduel.resources.cards_count
import com.vidanstojic.draftduel.resources.match_can_bid_up_to
import com.vidanstojic.draftduel.resources.match_choose_explain
import com.vidanstojic.draftduel.resources.match_choosing
import com.vidanstojic.draftduel.resources.match_free_card
import com.vidanstojic.draftduel.resources.match_give_to
import com.vidanstojic.draftduel.resources.match_leads
import com.vidanstojic.draftduel.resources.match_next_card
import com.vidanstojic.draftduel.resources.match_opener_must_bid
import com.vidanstojic.draftduel.resources.match_opens
import com.vidanstojic.draftduel.resources.match_pass_to
import com.vidanstojic.draftduel.resources.match_points_left
import com.vidanstojic.draftduel.resources.match_price
import com.vidanstojic.draftduel.resources.match_rating_gain
import com.vidanstojic.draftduel.resources.match_rating_later
import com.vidanstojic.draftduel.resources.match_seconds
import com.vidanstojic.draftduel.resources.match_shot_clock
import com.vidanstojic.draftduel.resources.match_take_it
import com.vidanstojic.draftduel.resources.match_takes_it
import com.vidanstojic.draftduel.resources.match_thinking
import com.vidanstojic.draftduel.resources.match_total
import com.vidanstojic.draftduel.resources.match_waiting_for
import com.vidanstojic.draftduel.resources.match_yours_for
import com.vidanstojic.draftduel.resources.match_you_lead
import com.vidanstojic.draftduel.resources.match_you_open
import com.vidanstojic.draftduel.resources.match_your_turn
import com.vidanstojic.draftduel.ui.components.AthleteCardView
import com.vidanstojic.draftduel.ui.components.BidControls
import com.vidanstojic.draftduel.ui.components.BudgetMeter
import com.vidanstojic.draftduel.ui.components.CardPile
import com.vidanstojic.draftduel.ui.components.DdOutlinedButton
import com.vidanstojic.draftduel.ui.components.DdPrimaryButton
import com.vidanstojic.draftduel.ui.components.InfoPill
import com.vidanstojic.draftduel.ui.components.LeaderBadge
import com.vidanstojic.draftduel.ui.components.ShotClock
import com.vidanstojic.draftduel.ui.components.TurnTimerRing
import com.vidanstojic.draftduel.ui.surface.PlaySurface
import com.vidanstojic.draftduel.ui.surface.SurfaceStyle
import com.vidanstojic.draftduel.ui.surface.surfaceStyle
import com.vidanstojic.draftduel.ui.theme.DraftDuelTheme
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** Fixed height of the control area, so the table does not jump between phases. */
private val ControlsHeight = 136.dp

/**
 * The match as one viewer sees it: the surface with both sides and the card on auction, then the
 * controls for the current phase. Shared by the bot match and the pass-the-phone match.
 */
@Composable
fun ColumnScope.MatchContent(
    state: MatchUiState,
    onRaise: (Int) -> Unit,
    onPass: () -> Unit,
    onAssign: (toSelf: Boolean) -> Unit,
) {
    PlaySurface(
        style = state.sport.surfaceStyle(),
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            OpponentSide(state)
            CenterRow(state)
            OwnSide(state)
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(ControlsHeight),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = state.phase,
            contentKey = { it::class },
            transitionSpec = { fadeIn() togetherWith fadeOut() },
        ) { phase ->
            PhaseControls(
                phase = phase,
                opponentName = state.opponent.name,
                onRaise = onRaise,
                onPass = onPass,
                onAssign = onAssign,
            )
        }
    }
}

@Composable
private fun OpponentSide(state: MatchUiState) {
    val colors = DraftDuelTheme.colors
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        InfoPill {
            Text(state.opponent.name, style = DraftDuelTheme.type.bodyStrong, color = colors.onScreen)
            Text(
                pluralStringResource(Res.plurals.cards_count, state.opponent.pile.size, state.opponent.pile.size),
                style = DraftDuelTheme.type.mono,
                color = colors.muted,
            )
        }
        CardPile(state.opponent.pile)
        BudgetPill(points = state.opponent.budget, max = state.maxBudget, isYou = false)
    }
}

@Composable
private fun OwnSide(state: MatchUiState) {
    val colors = DraftDuelTheme.colors
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        BudgetPill(points = state.you.budget, max = state.maxBudget, isYou = true)
        CardPile(state.you.pile)
        InfoPill {
            Text(state.you.name, style = DraftDuelTheme.type.bodyStrong, color = colors.accent)
            val total = state.you.total
            Text(
                text = pluralStringResource(Res.plurals.cards_count, state.you.pile.size, state.you.pile.size) +
                    if (total != null) " · " + stringResource(Res.string.match_total, total) else "",
                style = DraftDuelTheme.type.mono,
                color = colors.muted,
            )
        }
    }
}

@Composable
private fun BudgetPill(points: Int, max: Int, isYou: Boolean) {
    val colors = DraftDuelTheme.colors
    InfoPill {
        Text(stringResource(Res.string.match_points_left, points), style = DraftDuelTheme.type.mono, color = colors.onScreen)
        BudgetMeter(points = points, max = max, color = if (isYou) colors.accent else colors.opponent)
    }
}

@Composable
private fun CenterRow(state: MatchUiState) {
    val card = state.card ?: return
    val awarded = state.phase as? MatchPhaseUi.Awarded
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        AthleteCardView(
            card = card,
            sport = state.sport,
            ratingVisible = state.cardRatingVisible,
            glow = awarded?.toYou == true,
        )
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AuctionClock(state)
            AuctionStatus(state)
        }
    }
}

@Composable
private fun AuctionClock(state: MatchUiState) {
    val price = when (val phase = state.phase) {
        is MatchPhaseUi.Bidding -> phase.price.toString()
        is MatchPhaseUi.Awarded -> phase.price.toString()
        is MatchPhaseUi.Choosing -> "0"
        MatchPhaseUi.Finished -> ""
    }
    val ticking = state.phase is MatchPhaseUi.Bidding || state.phase is MatchPhaseUi.Choosing
    if (DraftDuelTheme.surface == SurfaceStyle.COURT) {
        ShotClock(seconds = if (ticking) state.secondsLeft else 0, label = stringResource(Res.string.match_shot_clock))
        InfoPill {
            Text(stringResource(Res.string.match_price), style = DraftDuelTheme.type.mono, color = DraftDuelTheme.colors.muted)
            Text(price, style = DraftDuelTheme.type.title, color = DraftDuelTheme.colors.onScreen)
        }
    } else {
        TurnTimerRing(
            progress = if (ticking) state.timerProgress else 1f,
            value = price,
            caption = if (ticking) stringResource(Res.string.match_seconds, state.secondsLeft) else null,
        )
    }
}

@Composable
private fun AuctionStatus(state: MatchUiState) {
    val colors = DraftDuelTheme.colors
    val opponent = state.opponent.name
    when (val phase = state.phase) {
        is MatchPhaseUi.Bidding -> {
            when (phase.leader) {
                Leader.OPPONENT -> LeaderBadge(stringResource(Res.string.match_leads, opponent))
                Leader.YOU -> LeaderBadge(stringResource(Res.string.match_you_lead), container = colors.accent, content = colors.onAccent)
                Leader.NONE -> InfoPill {
                    Text(
                        text = if (phase.youOpen) stringResource(Res.string.match_you_open) else stringResource(Res.string.match_opens, opponent),
                        style = DraftDuelTheme.type.mono,
                        color = colors.onScreen,
                    )
                }
            }
            StatusText(
                text = if (phase.yourTurn) stringResource(Res.string.match_your_turn) else stringResource(Res.string.match_thinking, opponent),
                highlight = phase.yourTurn,
            )
            Text(
                text = stringResource(Res.string.match_can_bid_up_to, opponent, phase.opponentMaxBid),
                style = DraftDuelTheme.type.mono.copy(fontSize = DraftDuelTheme.type.label.fontSize),
                color = colors.muted,
                textAlign = TextAlign.Center,
            )
        }
        is MatchPhaseUi.Choosing -> StatusText(stringResource(Res.string.match_free_card), highlight = phase.youChoose)
        is MatchPhaseUi.Awarded -> {
            if (phase.toYou) {
                StatusText(stringResource(Res.string.match_yours_for, phase.price), highlight = true)
                phase.rating?.let {
                    Text(stringResource(Res.string.match_rating_gain, it), style = DraftDuelTheme.type.monoStrong, color = colors.accent)
                }
            } else {
                StatusText(stringResource(Res.string.match_takes_it, opponent, phase.price), highlight = false)
            }
        }
        MatchPhaseUi.Finished -> Unit
    }
}

@Composable
private fun StatusText(text: String, highlight: Boolean) {
    val colors = DraftDuelTheme.colors
    InfoPill(background = if (highlight) colors.accent else colors.panel) {
        Text(text, style = DraftDuelTheme.type.bodyStrong, color = if (highlight) colors.onAccent else colors.onScreen, textAlign = TextAlign.Center)
    }
}

@Composable
private fun PhaseControls(
    phase: MatchPhaseUi,
    opponentName: String,
    onRaise: (Int) -> Unit,
    onPass: () -> Unit,
    onAssign: (toSelf: Boolean) -> Unit,
) {
    val colors = DraftDuelTheme.colors
    when (phase) {
        is MatchPhaseUi.Bidding -> BidControls(
            options = phase.options,
            passText = when {
                !phase.yourTurn -> stringResource(Res.string.match_waiting_for, opponentName)
                phase.leader == Leader.NONE -> stringResource(Res.string.match_opener_must_bid)
                else -> stringResource(Res.string.match_pass_to, opponentName, phase.price)
            },
            passEnabled = phase.yourTurn && phase.canPass,
            onBid = onRaise,
            onPass = onPass,
            modifier = Modifier.fillMaxWidth(),
        )
        is MatchPhaseUi.Choosing -> if (phase.youChoose) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(Res.string.match_choose_explain, opponentName),
                    style = DraftDuelTheme.type.caption,
                    color = colors.muted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DdPrimaryButton(stringResource(Res.string.match_take_it), onClick = { onAssign(true) }, modifier = Modifier.weight(1f))
                    DdOutlinedButton(
                        text = stringResource(Res.string.match_give_to, opponentName),
                        onClick = { onAssign(false) },
                        height = 60.dp,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        } else {
            InfoPill { Text(stringResource(Res.string.match_choosing, opponentName), style = DraftDuelTheme.type.bodyStrong, color = colors.onScreen) }
        }
        is MatchPhaseUi.Awarded -> Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = if (phase.toYou) {
                    stringResource(Res.string.match_yours_for, phase.price)
                } else {
                    stringResource(Res.string.match_takes_it, opponentName, phase.price)
                },
                style = DraftDuelTheme.type.headline,
                color = if (phase.toYou) colors.accent else colors.onScreen,
            )
            if (!phase.toYou) {
                Text(stringResource(Res.string.match_rating_later), style = DraftDuelTheme.type.caption, color = colors.muted)
            }
            Text(stringResource(Res.string.match_next_card), style = DraftDuelTheme.type.mono, color = colors.muted)
        }
        MatchPhaseUi.Finished -> Box(Modifier.width(1.dp))
    }
}
