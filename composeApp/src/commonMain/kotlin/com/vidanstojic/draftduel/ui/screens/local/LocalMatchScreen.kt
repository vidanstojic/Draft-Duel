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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vidanstojic.draftduel.di.LocalAppContainer
import com.vidanstojic.draftduel.domain.match.Seat
import com.vidanstojic.draftduel.domain.model.LocalLayout
import com.vidanstojic.draftduel.domain.model.Theme
import com.vidanstojic.draftduel.resources.Res
import com.vidanstojic.draftduel.resources.local_leave_text
import com.vidanstojic.draftduel.resources.match_skipped
import com.vidanstojic.draftduel.resources.local_ratings_at_end
import com.vidanstojic.draftduel.resources.match_card_label
import com.vidanstojic.draftduel.resources.match_card_of
import com.vidanstojic.draftduel.resources.match_choosing
import com.vidanstojic.draftduel.resources.match_pause
import com.vidanstojic.draftduel.resources.match_give_to
import com.vidanstojic.draftduel.resources.match_leads
import com.vidanstojic.draftduel.resources.match_points_left
import com.vidanstojic.draftduel.resources.match_seconds
import com.vidanstojic.draftduel.resources.match_take_it
import com.vidanstojic.draftduel.resources.match_takes_it
import com.vidanstojic.draftduel.resources.match_you_lead
import com.vidanstojic.draftduel.resources.match_your_turn
import com.vidanstojic.draftduel.resources.match_yours_for
import com.vidanstojic.draftduel.resources.menu
import com.vidanstojic.draftduel.ui.components.GameCardCompact
import com.vidanstojic.draftduel.ui.components.BidControls
import com.vidanstojic.draftduel.ui.components.CardPile
import com.vidanstojic.draftduel.ui.components.DdIconButton
import com.vidanstojic.draftduel.ui.components.DdIcons
import com.vidanstojic.draftduel.ui.components.PauseOverlay
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
import com.vidanstojic.draftduel.ui.screens.match.openerText
import com.vidanstojic.draftduel.ui.screens.match.passLabel
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
    val viewModel = viewModel { LocalMatchViewModel(container.packRepository, container.sessionRepository, container.feedback) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val finished by viewModel.finished.collectAsStateWithLifecycle()
    val paused by viewModel.paused.collectAsStateWithLifecycle()
    LaunchedEffect(finished) {
        if (finished) onFinished()
    }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { viewModel.pause() }
    var confirmLeave by rememberSaveable { mutableStateOf(false) }

    val actionsFor: (Seat) -> SeatActions = { seat ->
        SeatActions(
            onRaise = { step -> viewModel.onRaise(seat, step) },
            onPass = { viewModel.onPass(seat) },
            onAssign = { toSelf -> viewModel.onAssign(seat, toSelf) },
        )
    }
    val handoff = state.handoff
    Box {
        when {
            state.layout == LocalLayout.SPLIT_SCREEN -> SplitMatchScreen(
                state = state,
                bottomActions = actionsFor(Seat.A),
                topActions = actionsFor(Seat.B),
                onMenu = { confirmLeave = true },
                onPause = viewModel::pause,
            )
            handoff != null -> HandoffScreen(handoff = handoff, theme = state.bottom.theme, onReady = viewModel::onHandoffConfirmed)
            else -> PassDeviceMatchScreen(
                state = state.active,
                actions = actionsFor(state.activeSeat),
                onMenu = { confirmLeave = true },
                onPause = viewModel::pause,
            )
        }
        if (paused) {
            DraftDuelTheme(surface = state.bottom.theme.surfaceStyle()) {
                PauseOverlay(onResume = viewModel::resume, onLeave = { confirmLeave = true })
            }
        }
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
    onPause: () -> Unit,
) {
    DdScreen(surface = state.theme.surfaceStyle()) {
        DdTopBar(
            leading = { DdIconButton(Icons.Filled.Menu, stringResource(Res.string.menu), onMenu) },
            trailing = { DdIconButton(DdIcons.Pause, stringResource(Res.string.match_pause), onPause) },
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
 * middle. The surface fills the whole screen, so each player gets their half of the pitch (court,
 * table…) and the card lies on the center line. Both halves are always live; the one not on turn
 * is dimmed.
 */
@Composable
private fun SplitMatchScreen(
    state: LocalMatchUiState,
    bottomActions: SeatActions,
    topActions: SeatActions,
    onMenu: () -> Unit,
    onPause: () -> Unit,
) {
    val surface = state.bottom.theme.surfaceStyle()
    DraftDuelTheme(surface = surface) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .surfaceScreenBackground(surface)
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            PlaySurface(
                style = surface,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(6.dp),
                content = {},
            )
            Column(Modifier.fillMaxSize()) {
                SeatHalf(
                    state = state.top,
                    actions = topActions,
                    modifier = Modifier
                        .weight(1f)
                        .rotate(180f),
                )
                CenterStrip(state = state.bottom, onMenu = onMenu, onPause = onPause)
                SeatHalf(
                    state = state.bottom,
                    actions = bottomActions,
                    modifier = Modifier.weight(1f),
                )
            }
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
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Bottom),
    ) {
        // Drawn over the surface, so the name row gets a panel to stay readable on light courts.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.panel, RoundedCornerShape(14.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
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
                    passText = passLabel(phase, state.opponent.name),
                    passEnabled = phase.passEnabled,
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
        phase.handedOver && phase.leader == Leader.NONE -> openerText(phase, state.opponent.name)
        phase.yourTurn -> stringResource(Res.string.match_your_turn)
        phase.leader == Leader.YOU -> stringResource(Res.string.match_you_lead)
        phase.leader == Leader.NONE -> openerText(phase, state.opponent.name)
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
    MatchPhaseUi.Discarded -> stringResource(Res.string.match_skipped)
    MatchPhaseUi.Finished -> ""
}

@Composable
private fun CenterStrip(state: MatchUiState, onMenu: () -> Unit, onPause: () -> Unit) {
    val colors = DraftDuelTheme.colors
    val card = state.card
    val price = when (val phase = state.phase) {
        is MatchPhaseUi.Bidding -> phase.price
        is MatchPhaseUi.Awarded -> phase.price
        else -> 0
    }
    // The surface behind is the whole screen; the strip is a panel on its center line.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .background(colors.panel, RoundedCornerShape(18.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (card != null) {
                Text(
                    text = listOf(card.name, card.details).filter { it.isNotBlank() }.joinToString(" · "),
                    style = DraftDuelTheme.type.mono,
                    color = colors.onScreen,
                    modifier = Modifier.rotate(180f),
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (card != null) GameCardCompact(card = card, theme = state.theme, ratingVisible = false)
                TurnTimerRing(
                    progress = state.timerProgress,
                    value = price.toString(),
                    caption = stringResource(Res.string.match_seconds, state.secondsLeft),
                    size = 60.dp,
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    DdIconButton(DdIcons.Pause, stringResource(Res.string.match_pause), onPause)
                    DdIconButton(Icons.Filled.Menu, stringResource(Res.string.menu), onMenu)
                }
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
    val bottom = previewMatchState(Theme.BASKETBALL)
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
        onPause = {},
    )
}
