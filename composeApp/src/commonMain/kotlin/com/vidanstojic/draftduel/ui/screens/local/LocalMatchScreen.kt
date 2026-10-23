package com.vidanstojic.draftduel.ui.screens.local

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vidanstojic.draftduel.di.LocalAppContainer
import com.vidanstojic.draftduel.domain.match.Seat
import com.vidanstojic.draftduel.domain.model.LocalLayout
import com.vidanstojic.draftduel.domain.model.Sport
import com.vidanstojic.draftduel.resources.Res
import com.vidanstojic.draftduel.resources.local_leave_text
import com.vidanstojic.draftduel.resources.local_ratings_at_end
import com.vidanstojic.draftduel.resources.match_card_label
import com.vidanstojic.draftduel.resources.match_card_of
import com.vidanstojic.draftduel.resources.match_choosing
import com.vidanstojic.draftduel.resources.match_give_to
import com.vidanstojic.draftduel.resources.match_leads
import com.vidanstojic.draftduel.resources.match_opener_must_bid
import com.vidanstojic.draftduel.resources.match_opens
import com.vidanstojic.draftduel.resources.match_pass_to
import com.vidanstojic.draftduel.resources.match_points_left
import com.vidanstojic.draftduel.resources.match_seconds
import com.vidanstojic.draftduel.resources.match_take_it
import com.vidanstojic.draftduel.resources.match_takes_it
import com.vidanstojic.draftduel.resources.match_waiting_for
import com.vidanstojic.draftduel.resources.match_you_lead
import com.vidanstojic.draftduel.resources.match_your_turn
import com.vidanstojic.draftduel.resources.match_yours_for
import com.vidanstojic.draftduel.resources.menu
import com.vidanstojic.draftduel.ui.components.AthleteCardCompact
import com.vidanstojic.draftduel.ui.components.BidControls
import com.vidanstojic.draftduel.ui.components.CardPile
import com.vidanstojic.draftduel.ui.components.DdIconButton
import com.vidanstojic.draftduel.ui.components.DdOutlinedButton
import com.vidanstojic.draftduel.ui.components.DdPrimaryButton
import com.vidanstojic.draftduel.ui.components.DdScreen
import com.vidanstojic.draftduel.ui.components.DdTopBar
import com.vidanstojic.draftduel.ui.components.InfoPill
import com.vidanstojic.draftduel.ui.components.TopBarTitle
import com.vidanstojic.draftduel.ui.components.TurnTimerRing
import com.vidanstojic.draftduel.ui.screens.match.Leader
import com.vidanstojic.draftduel.ui.screens.match.LeaveMatchDialog
import com.vidanstojic.draftduel.ui.screens.match.MatchContent
import com.vidanstojic.draftduel.ui.screens.match.MatchPhaseUi
import com.vidanstojic.draftduel.ui.screens.match.MatchUiState
import com.vidanstojic.draftduel.ui.screens.match.previewMatchState
import com.vidanstojic.draftduel.ui.surface.PlaySurface
import com.vidanstojic.draftduel.ui.surface.surfaceScreenBackground
import com.vidanstojic.draftduel.ui.surface.surfaceStyle
import com.vidanstojic.draftduel.ui.theme.DraftDuelTheme
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

/** Actions of one seat, so the same controls drive either half of the table. */
private class SeatActions(
    val onRaise: (Int) -> Unit,
    val onPass: () -> Unit,
    val onAssign: (toSelf: Boolean) -> Unit,
)

@Composable
fun LocalMatchRoute(
    onFinished: () -> Unit,
    onLeave: () -> Unit,
) {
    val container = LocalAppContainer.current
    val viewModel = viewModel { LocalMatchViewModel(container.packRepository, container.sessionRepository) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val finished by viewModel.finished.collectAsStateWithLifecycle()
    LaunchedEffect(finished) {
        if (finished) onFinished()
    }
    var confirmLeave by rememberSaveable { mutableStateOf(false) }

    val actionsFor: (Seat) -> SeatActions = { seat ->
        SeatActions(
            onRaise = { step -> viewModel.onRaise(seat, step) },
            onPass = { viewModel.onPass(seat) },
            onAssign = { toSelf -> viewModel.onAssign(seat, toSelf) },
        )
    }
    val handoff = state.handoff
    when {
        state.layout == LocalLayout.SPLIT_SCREEN -> SplitMatchScreen(
            state = state,
            bottomActions = actionsFor(Seat.A),
            topActions = actionsFor(Seat.B),
            onMenu = { confirmLeave = true },
        )
        handoff != null -> HandoffScreen(handoff = handoff, sport = state.bottom.sport, onReady = viewModel::onHandoffConfirmed)
        else -> PassDeviceMatchScreen(
            state = state.active,
            actions = actionsFor(state.activeSeat),
            onMenu = { confirmLeave = true },
        )
    }

    if (confirmLeave) {
        LeaveMatchDialog(
            text = stringResource(Res.string.local_leave_text),
            onConfirm = {
                confirmLeave = false
                onLeave()
            },
            onDismiss = { confirmLeave = false },
        )
    }
}

/** One player's view at a time; the same layout as the bot match. */
@Composable
private fun PassDeviceMatchScreen(
    state: MatchUiState,
    actions: SeatActions,
    onMenu: () -> Unit,
) {
    DdScreen(surface = state.sport.surfaceStyle()) {
        DdTopBar(
            leading = { DdIconButton(Icons.Filled.Menu, stringResource(Res.string.menu), onMenu) },
            title = {
                TopBarTitle(
                    label = stringResource(Res.string.match_card_label),
                    value = stringResource(Res.string.match_card_of, state.cardNumber.toString().padStart(2, '0'), state.totalCards),
                )
            },
        )
        MatchContent(state = state, onRaise = actions.onRaise, onPass = actions.onPass, onAssign = actions.onAssign)
    }
}

/**
 * Phone lying flat between two players: player 2's half is rotated 180°, the card sits in the
 * middle. Both halves are always live; the one not on turn is dimmed.
 */
@Composable
private fun SplitMatchScreen(
    state: LocalMatchUiState,
    bottomActions: SeatActions,
    topActions: SeatActions,
    onMenu: () -> Unit,
) {
    val surface = state.bottom.sport.surfaceStyle()
    DraftDuelTheme(surface = surface) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .surfaceScreenBackground(surface)
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            SeatHalf(
                state = state.top,
                actions = topActions,
                modifier = Modifier
                    .weight(1f)
                    .rotate(180f),
            )
            CenterStrip(state = state.bottom, onMenu = onMenu)
            SeatHalf(
                state = state.bottom,
                actions = bottomActions,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SeatHalf(
    state: MatchUiState,
    actions: SeatActions,
    modifier: Modifier = Modifier,
) {
    val colors = DraftDuelTheme.colors
    val phase = state.phase
    val active = when (phase) {
        is MatchPhaseUi.Bidding -> phase.yourTurn
        is MatchPhaseUi.Choosing -> phase.youChoose
        else -> false
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Bottom),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(12.dp).background(colors.accent, CircleShape))
            Text(state.you.name, style = DraftDuelTheme.type.title, color = colors.onScreen, modifier = Modifier.weight(1f))
            CardPile(state.you.pile, cardWidth = 30.dp, overlap = 9.dp)
            InfoPill {
                Text(stringResource(Res.string.match_points_left, state.you.budget), style = DraftDuelTheme.type.mono, color = colors.onScreen)
            }
        }
        InfoPill(
            background = if (active) colors.accent else colors.panel,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        ) {
            Text(
                text = statusFor(state),
                style = DraftDuelTheme.type.bodyStrong,
                color = if (active) colors.onAccent else colors.onScreen,
                textAlign = TextAlign.Center,
            )
        }
        Box(Modifier.alpha(if (active) 1f else 0.45f)) {
            when (phase) {
                is MatchPhaseUi.Choosing -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DdPrimaryButton(
                        text = stringResource(Res.string.match_take_it),
                        onClick = { actions.onAssign(true) },
                        enabled = phase.youChoose,
                        modifier = Modifier.weight(1f),
                    )
                    DdOutlinedButton(
                        text = stringResource(Res.string.match_give_to, state.opponent.name),
                        onClick = { actions.onAssign(false) },
                        enabled = phase.youChoose,
                        height = 60.dp,
                        modifier = Modifier.weight(1f),
                    )
                }
                is MatchPhaseUi.Bidding -> BidControls(
                    options = phase.options,
                    passText = when {
                        !phase.yourTurn -> stringResource(Res.string.match_waiting_for, state.opponent.name)
                        phase.leader == Leader.NONE -> stringResource(Res.string.match_opener_must_bid)
                        else -> stringResource(Res.string.match_pass_to, state.opponent.name, phase.price)
                    },
                    passEnabled = phase.yourTurn && phase.canPass,
                    onBid = actions.onRaise,
                    onPass = actions.onPass,
                    chipHeight = 64.dp,
                    passHeight = 44.dp,
                    modifier = Modifier.fillMaxWidth(),
                )
                else -> Box(Modifier.height(118.dp))
            }
        }
    }
}

@Composable
private fun statusFor(state: MatchUiState): String = when (val phase = state.phase) {
    is MatchPhaseUi.Bidding -> when {
        phase.yourTurn -> stringResource(Res.string.match_your_turn)
        phase.leader == Leader.YOU -> stringResource(Res.string.match_you_lead)
        phase.leader == Leader.NONE -> stringResource(Res.string.match_opens, state.opponent.name)
        else -> stringResource(Res.string.match_leads, state.opponent.name)
    }
    is MatchPhaseUi.Choosing -> if (phase.youChoose) {
        stringResource(Res.string.match_your_turn)
    } else {
        stringResource(Res.string.match_choosing, state.opponent.name)
    }
    is MatchPhaseUi.Awarded -> if (phase.toYou) {
        stringResource(Res.string.match_yours_for, phase.price)
    } else {
        stringResource(Res.string.match_takes_it, state.opponent.name, phase.price)
    }
    MatchPhaseUi.Finished -> ""
}

@Composable
private fun CenterStrip(state: MatchUiState, onMenu: () -> Unit) {
    val colors = DraftDuelTheme.colors
    val card = state.card
    val price = when (val phase = state.phase) {
        is MatchPhaseUi.Bidding -> phase.price
        is MatchPhaseUi.Awarded -> phase.price
        else -> 0
    }
    PlaySurface(
        style = state.sport.surfaceStyle(),
        modifier = Modifier
            .fillMaxWidth()
            .height(196.dp)
            .padding(horizontal = 10.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            if (card != null) {
                Text(
                    text = "${card.name} · ${card.season} · ${card.country} · ${card.position}",
                    style = DraftDuelTheme.type.mono,
                    color = colors.onScreen,
                    modifier = Modifier.rotate(180f),
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (card != null) AthleteCardCompact(card = card, sport = state.sport, ratingVisible = false)
                TurnTimerRing(
                    progress = state.timerProgress,
                    value = price.toString(),
                    caption = stringResource(Res.string.match_seconds, state.secondsLeft),
                    size = 60.dp,
                )
                DdIconButton(Icons.Filled.Menu, stringResource(Res.string.menu), onMenu)
            }
            Text(
                text = stringResource(Res.string.match_card_of, state.cardNumber.toString().padStart(2, '0'), state.totalCards) +
                    " · " + stringResource(Res.string.local_ratings_at_end),
                style = DraftDuelTheme.type.mono,
                color = colors.muted,
            )
        }
    }
}

@Preview
@Composable
private fun SplitMatchScreenPreview() {
    val bottom = previewMatchState(Sport.BASKETBALL)
    val noop = SeatActions(onRaise = {}, onPass = {}, onAssign = {})
    SplitMatchScreen(
        state = LocalMatchUiState(
            layout = LocalLayout.SPLIT_SCREEN,
            bottom = bottom,
            top = bottom.copy(you = bottom.opponent, opponent = bottom.you),
            activeSeat = Seat.A,
            handoff = null,
        ),
        bottomActions = noop,
        topActions = noop,
        onMenu = {},
    )
}
