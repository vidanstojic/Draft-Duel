package com.vidanstojic.draftduel.ui.screens.match

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vidanstojic.draftduel.data.fake.FakePacks
import com.vidanstojic.draftduel.di.LocalAppContainer
import com.vidanstojic.draftduel.domain.match.MatchAction
import com.vidanstojic.draftduel.domain.match.MatchEngine
import com.vidanstojic.draftduel.domain.match.Seat
import com.vidanstojic.draftduel.domain.model.Sport
import com.vidanstojic.draftduel.resources.Res
import com.vidanstojic.draftduel.resources.match_card_label
import com.vidanstojic.draftduel.resources.match_card_of
import com.vidanstojic.draftduel.resources.match_cards_left
import com.vidanstojic.draftduel.resources.match_leave_cancel
import com.vidanstojic.draftduel.resources.match_leave_confirm
import com.vidanstojic.draftduel.resources.match_leave_text
import com.vidanstojic.draftduel.resources.match_leave_title
import com.vidanstojic.draftduel.resources.menu
import com.vidanstojic.draftduel.ui.components.DdIconButton
import com.vidanstojic.draftduel.ui.components.DdScreen
import com.vidanstojic.draftduel.ui.components.DdTopBar
import com.vidanstojic.draftduel.ui.components.TopBarTitle
import com.vidanstojic.draftduel.ui.surface.surfaceStyle
import com.vidanstojic.draftduel.ui.theme.DraftDuelTheme
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun MatchRoute(
    onFinished: () -> Unit,
    onLeave: () -> Unit,
) {
    val container = LocalAppContainer.current
    val viewModel = viewModel {
        MatchViewModel(container.packRepository, container.profileRepository, container.sessionRepository)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val finished by viewModel.finished.collectAsStateWithLifecycle()
    LaunchedEffect(finished) {
        if (finished) onFinished()
    }
    MatchScreen(
        state = state,
        onRaise = viewModel::onRaise,
        onPass = viewModel::onPass,
        onAssign = viewModel::onAssign,
        onLeave = onLeave,
    )
}

@Composable
fun MatchScreen(
    state: MatchUiState,
    onRaise: (Int) -> Unit,
    onPass: () -> Unit,
    onAssign: (toSelf: Boolean) -> Unit,
    onLeave: () -> Unit,
) {
    var confirmLeave by rememberSaveable { mutableStateOf(false) }
    DdScreen(surface = state.sport.surfaceStyle()) {
        DdTopBar(
            leading = { DdIconButton(Icons.Filled.Menu, stringResource(Res.string.menu), onClick = { confirmLeave = true }) },
            trailing = {
                Text(
                    text = stringResource(Res.string.match_cards_left, state.cardsLeft),
                    style = DraftDuelTheme.type.mono,
                    color = DraftDuelTheme.colors.muted,
                )
            },
            title = {
                TopBarTitle(
                    label = stringResource(Res.string.match_card_label),
                    value = stringResource(Res.string.match_card_of, state.cardNumber.toString().padStart(2, '0'), state.totalCards),
                )
            },
        )
        MatchContent(state = state, onRaise = onRaise, onPass = onPass, onAssign = onAssign)
    }

    if (confirmLeave) {
        LeaveMatchDialog(
            text = stringResource(Res.string.match_leave_text, state.opponent.name),
            onConfirm = {
                confirmLeave = false
                onLeave()
            },
            onDismiss = { confirmLeave = false },
        )
    }
}

@Composable
internal fun LeaveMatchDialog(
    text: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.match_leave_title)) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(Res.string.match_leave_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.match_leave_cancel)) }
        },
    )
}

/** Preview state: card 6 of 10, the opponent leads at 6 and the viewer is to act. */
internal fun previewMatchState(sport: Sport): MatchUiState {
    val pack = if (sport == Sport.BASKETBALL) FakePacks.basketball else FakePacks.football
    var match = MatchEngine.start(sport, pack.cards.take(10), "You", "Marco_23", firstOpener = Seat.B)
    repeat(5) { round ->
        val opener = MatchEngine.openerFor(match, round)
        match = MatchEngine.reduce(match, MatchAction.Raise(opener, if (round % 2 == 0) 2 else 1))
        match = MatchEngine.reduce(match, MatchAction.Pass(opener.other))
        match = MatchEngine.advance(match)
    }
    // Round 6 is opened by the viewer (openers alternate); the opponent answers with +5.
    match = MatchEngine.reduce(match, MatchAction.Raise(Seat.A, 1))
    match = MatchEngine.reduce(match, MatchAction.Raise(Seat.B, 5))
    return match.toUi(viewer = Seat.A, millisLeft = 7_000)
}

@Preview
@Composable
private fun MatchScreenPitchPreview() {
    MatchScreen(previewMatchState(Sport.FOOTBALL), onRaise = {}, onPass = {}, onAssign = {}, onLeave = {})
}

@Preview
@Composable
private fun MatchScreenCourtPreview() {
    MatchScreen(previewMatchState(Sport.BASKETBALL), onRaise = {}, onPass = {}, onAssign = {}, onLeave = {})
}
