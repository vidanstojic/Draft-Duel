package com.vidanstojic.draftduel.ui.screens.result

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vidanstojic.draftduel.data.fake.FakePacks
import com.vidanstojic.draftduel.di.LocalAppContainer
import com.vidanstojic.draftduel.domain.match.MatchEngine
import com.vidanstojic.draftduel.domain.match.MatchPhase
import com.vidanstojic.draftduel.domain.match.OwnedCard
import com.vidanstojic.draftduel.domain.match.Seat
import com.vidanstojic.draftduel.domain.model.GameMode
import com.vidanstojic.draftduel.domain.model.Scoring
import com.vidanstojic.draftduel.domain.model.Theme
import com.vidanstojic.draftduel.platform.rememberShareText
import com.vidanstojic.draftduel.resources.Res
import com.vidanstojic.draftduel.resources.close
import com.vidanstojic.draftduel.resources.result_best_buy
import com.vidanstojic.draftduel.resources.result_cards
import com.vidanstojic.draftduel.resources.result_draw
import com.vidanstojic.draftduel.resources.result_empty
import com.vidanstojic.draftduel.resources.result_paid
import com.vidanstojic.draftduel.resources.result_rematch
import com.vidanstojic.draftduel.resources.result_share
import com.vidanstojic.draftduel.resources.result_share_message
import com.vidanstojic.draftduel.resources.result_title
import com.vidanstojic.draftduel.resources.result_wins_by
import com.vidanstojic.draftduel.resources.result_you_lose_by
import com.vidanstojic.draftduel.resources.result_you_win_by
import com.vidanstojic.draftduel.ui.components.DdIconButton
import com.vidanstojic.draftduel.ui.components.DdOutlinedButton
import com.vidanstojic.draftduel.ui.components.DdPrimaryButton
import com.vidanstojic.draftduel.ui.components.DdScreen
import com.vidanstojic.draftduel.ui.components.DdTopBar
import com.vidanstojic.draftduel.ui.components.InfoPill
import com.vidanstojic.draftduel.ui.components.TopBarTitle
import com.vidanstojic.draftduel.ui.components.format
import com.vidanstojic.draftduel.ui.surface.PlaySurface
import com.vidanstojic.draftduel.ui.surface.SurfaceStyle
import com.vidanstojic.draftduel.ui.surface.surfaceStyle
import com.vidanstojic.draftduel.ui.theme.CardColors
import com.vidanstojic.draftduel.ui.theme.DraftDuelTheme
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun ResultRoute(
    onClose: () -> Unit,
    onRematch: (GameMode) -> Unit,
) {
    val container = LocalAppContainer.current
    val viewModel = viewModel { ResultViewModel(container.sessionRepository) }
    val share = rememberShareText()
    val state = viewModel.state
    if (state == null) {
        EmptyResult(onClose)
        return
    }
    val shareMessage = stringResource(
        Res.string.result_share_message,
        state.bottom.total,
        state.top.total,
        state.bestBuy?.name ?: "-",
    )
    ResultScreen(
        state = state,
        onClose = onClose,
        onShare = { share(shareMessage) },
        onRematch = { onRematch(viewModel.mode) },
    )
}

@Composable
fun ResultScreen(
    state: ResultUiState,
    onClose: () -> Unit,
    onShare: () -> Unit,
    onRematch: () -> Unit,
) {
    val surface = state.theme.surfaceStyle()
    DdScreen(surface = surface) {
        DdTopBar(
            leading = { DdIconButton(Icons.Filled.Close, stringResource(Res.string.close), onClose) },
            title = {
                TopBarTitle(
                    label = stringResource(Res.string.result_title),
                    value = state.goal ?: stringResource(Res.string.result_cards, state.bottom.rows.size + state.top.rows.size),
                )
            },
        )
        PlaySurface(style = surface, modifier = Modifier.weight(1f).fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                RosterBlock(state.top, nameFirst = true, isBottom = false, scoring = state.theme.scoring)
                Scoreboard(state)
                RosterBlock(state.bottom, nameFirst = false, isBottom = true, scoring = state.theme.scoring)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DdPrimaryButton(
                text = stringResource(Res.string.result_share),
                onClick = onShare,
                icon = Icons.Filled.Share,
                modifier = Modifier.weight(1f),
            )
            DdOutlinedButton(
                text = stringResource(Res.string.result_rematch),
                onClick = onRematch,
                height = 60.dp,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun RosterBlock(roster: RosterUi, nameFirst: Boolean, isBottom: Boolean, scoring: Scoring) {
    val colors = DraftDuelTheme.colors
    val header: @Composable () -> Unit = {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            InfoPill {
                Text(roster.name, style = DraftDuelTheme.type.bodyStrong, color = if (isBottom) colors.accent else colors.onScreen)
            }
            InfoPill {
                Text(stringResource(Res.string.result_paid, roster.spent), style = DraftDuelTheme.type.mono, color = colors.muted)
            }
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (nameFirst) header()
        roster.rows.forEach { RosterRow(it, scoring) }
        if (!nameFirst) header()
    }
}

@Composable
private fun RosterRow(row: RosterRowUi, scoring: Scoring) {
    val shape = RoundedCornerShape(6.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(30.dp)
            .background(CardColors.Paper, shape)
            .then(if (row.bestBuy) Modifier.border(2.dp, CardColors.BackInk, shape) else Modifier)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (row.bestBuy) Icon(Icons.Filled.Star, contentDescription = null, tint = CardColors.Ink, modifier = Modifier.size(14.dp))
        Text(
            text = row.name,
            style = DraftDuelTheme.type.bodyStrong.copy(fontSize = 13.sp),
            color = CardColors.Ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = listOf(row.details, row.price.toString()).filter { it.isNotBlank() }.joinToString(" · "),
            style = DraftDuelTheme.type.mono.copy(fontSize = 9.sp),
            color = CardColors.InkMuted,
        )
        Text(scoring.format(row.rating), style = DraftDuelTheme.type.monoStrong, color = CardColors.Ink)
    }
}

@Composable
private fun Scoreboard(state: ResultUiState) {
    val colors = DraftDuelTheme.colors
    val headline = when (state.outcome) {
        ResultOutcome.DRAW -> stringResource(Res.string.result_draw)
        ResultOutcome.BOTTOM_WINS -> if (state.bottomIsYou) {
            stringResource(Res.string.result_you_win_by, state.margin)
        } else {
            stringResource(Res.string.result_wins_by, state.bottom.name, state.margin)
        }
        ResultOutcome.TOP_WINS -> if (state.bottomIsYou) {
            stringResource(Res.string.result_you_lose_by, state.margin)
        } else {
            stringResource(Res.string.result_wins_by, state.top.name, state.margin)
        }
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        InfoPill {
            Text(state.bottom.total.toString(), style = DraftDuelTheme.type.numberLarge, color = colors.accent)
            Text(":", style = DraftDuelTheme.type.title, color = colors.muted)
            Text(state.top.total.toString(), style = DraftDuelTheme.type.number, color = colors.onScreen)
        }
        InfoPill(background = colors.accent) {
            Text(headline, style = DraftDuelTheme.type.bodyStrong, color = colors.onAccent)
        }
        state.bestBuy?.let { best ->
            Text(
                text = stringResource(Res.string.result_best_buy, best.name, best.rating, best.price),
                style = DraftDuelTheme.type.mono,
                color = colors.onScreen,
            )
        }
    }
}

@Composable
private fun EmptyResult(onClose: () -> Unit) {
    DdScreen(surface = SurfaceStyle.TABLE) {
        DdTopBar(
            leading = { DdIconButton(Icons.Filled.Close, stringResource(Res.string.close), onClose) },
            title = { TopBarTitle(stringResource(Res.string.result_title), "") },
        )
        Text(stringResource(Res.string.result_empty), style = DraftDuelTheme.type.body, color = DraftDuelTheme.colors.muted)
    }
}

/** A finished sample match for previews. */
internal fun previewResultState(theme: Theme): ResultUiState {
    val pack = if (theme == Theme.BASKETBALL) FakePacks.basketball else FakePacks.football
    val cards = pack.cards.take(10)
    val prices = listOf(8, 6, 3, 2, 1, 5, 4, 4, 4, 3)
    var match = MatchEngine.start(theme, cards, "You", "Marco_23", firstOpener = Seat.A)
    val owners = listOf(Seat.A, Seat.B, Seat.A, Seat.B, Seat.A, Seat.B, Seat.A, Seat.B, Seat.A, Seat.B)
    val seats = owners.indices.groupBy { owners[it] }.mapValues { (seat, indices) ->
        match.seat(seat).copy(
            budget = 20 - indices.sumOf { prices[it] },
            cards = indices.map { OwnedCard(cards[it], prices[it]) },
        )
    }
    match = match.copy(seats = seats, roundIndex = cards.size, phase = MatchPhase.Finished)
    return match.toResultUi(bottomIsYou = true)
}

@Preview
@Composable
private fun ResultScreenPreview() {
    ResultScreen(previewResultState(Theme.FOOTBALL), onClose = {}, onShare = {}, onRematch = {})
}
