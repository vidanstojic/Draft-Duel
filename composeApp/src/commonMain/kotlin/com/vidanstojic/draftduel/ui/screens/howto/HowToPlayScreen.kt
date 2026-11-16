package com.vidanstojic.draftduel.ui.screens.howto

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vidanstojic.draftduel.di.LocalAppContainer
import com.vidanstojic.draftduel.resources.Res
import com.vidanstojic.draftduel.resources.close
import com.vidanstojic.draftduel.resources.how_to_play
import com.vidanstojic.draftduel.resources.howto_auction_text
import com.vidanstojic.draftduel.resources.howto_auction_title
import com.vidanstojic.draftduel.resources.howto_budget_text
import com.vidanstojic.draftduel.resources.howto_budget_title
import com.vidanstojic.draftduel.resources.howto_next
import com.vidanstojic.draftduel.resources.howto_page
import com.vidanstojic.draftduel.resources.howto_play
import com.vidanstojic.draftduel.resources.howto_reveal_text
import com.vidanstojic.draftduel.resources.howto_reveal_title
import com.vidanstojic.draftduel.resources.howto_what_text
import com.vidanstojic.draftduel.resources.howto_what_title
import com.vidanstojic.draftduel.resources.match_slots
import com.vidanstojic.draftduel.resources.result_ratings_line
import com.vidanstojic.draftduel.ui.components.BudgetMeter
import com.vidanstojic.draftduel.ui.components.ChipDisc
import com.vidanstojic.draftduel.ui.components.DdIconButton
import com.vidanstojic.draftduel.ui.components.DdPrimaryButton
import com.vidanstojic.draftduel.ui.components.DdScreen
import com.vidanstojic.draftduel.ui.components.DdTopBar
import com.vidanstojic.draftduel.ui.components.HiddenRating
import com.vidanstojic.draftduel.ui.components.InfoPill
import com.vidanstojic.draftduel.ui.components.MiniCard
import com.vidanstojic.draftduel.ui.components.PileCard
import com.vidanstojic.draftduel.ui.components.CardPile
import com.vidanstojic.draftduel.ui.components.TopBarTitle
import com.vidanstojic.draftduel.ui.components.TurnTimerRing
import com.vidanstojic.draftduel.ui.surface.PlaySurface
import com.vidanstojic.draftduel.ui.surface.SurfaceStyle
import com.vidanstojic.draftduel.ui.theme.CardColors
import com.vidanstojic.draftduel.ui.theme.DraftDuelTheme
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

/** Whether the guide was seen; the first launch opens it once by itself. */
const val HOW_TO_PLAY_SEEN_KEY = "how_to_play_seen"

@Composable
fun HowToPlayRoute(onClose: () -> Unit) {
    val store = LocalAppContainer.current.platform.store
    // Opened once counts as seen, also when the player leaves with the system Back.
    LaunchedEffect(Unit) { store.putBoolean(HOW_TO_PLAY_SEEN_KEY, true) }
    HowToPlayScreen(onClose = onClose)
}

private class GuidePage(
    val title: StringResource,
    val text: StringResource,
    val picture: @Composable () -> Unit,
)

/**
 * The guide starts with what the game is, then one round, then the budget, then how a match is
 * won: the order a new player needs it in (tester feedback: it used to open with the points).
 */
@Composable
fun HowToPlayScreen(onClose: () -> Unit) {
    val pages = guidePages()
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val last = pagerState.currentPage == pages.lastIndex
    DdScreen(surface = SurfaceStyle.TABLE) {
        DdTopBar(
            leading = { DdIconButton(Icons.Filled.Close, stringResource(Res.string.close), onClose) },
            title = {
                TopBarTitle(
                    label = stringResource(Res.string.how_to_play),
                    value = stringResource(Res.string.howto_page, pagerState.currentPage + 1, pages.size),
                )
            },
        )
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) { index ->
            GuidePageContent(pages[index])
        }
        PageDots(count = pages.size, current = pagerState.currentPage, modifier = Modifier.align(Alignment.CenterHorizontally))
        DdPrimaryButton(
            text = stringResource(if (last) Res.string.howto_play else Res.string.howto_next),
            onClick = {
                if (last) onClose() else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun guidePages(): List<GuidePage> = listOf(
    GuidePage(Res.string.howto_what_title, Res.string.howto_what_text) { WhatPicture() },
    GuidePage(Res.string.howto_auction_title, Res.string.howto_auction_text) { AuctionPicture() },
    GuidePage(Res.string.howto_budget_title, Res.string.howto_budget_text) { BudgetPicture() },
    GuidePage(Res.string.howto_reveal_title, Res.string.howto_reveal_text) { RevealPicture() },
)

@Composable
private fun GuidePageContent(page: GuidePage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        PlaySurface(
            style = SurfaceStyle.TABLE,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
        ) {
            Box(Modifier.align(Alignment.Center)) { page.picture() }
        }
        Text(
            text = stringResource(page.title),
            style = DraftDuelTheme.type.headline,
            color = DraftDuelTheme.colors.onScreen,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        Text(
            text = stringResource(page.text),
            style = DraftDuelTheme.type.body,
            color = DraftDuelTheme.colors.muted,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
    }
}

@Composable
private fun PageDots(count: Int, current: Int, modifier: Modifier = Modifier) {
    val colors = DraftDuelTheme.colors
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(count) { index ->
            Box(
                Modifier
                    .size(if (index == current) 10.dp else 8.dp)
                    .background(if (index == current) colors.accent else colors.outline, CircleShape),
            )
        }
    }
}

/** Two chips facing a hidden card: two players, one card, a rating nobody sees. */
@Composable
private fun WhatPicture() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
        ChipDisc(color = DraftDuelTheme.colors.primaryBid.container, ringColor = CardColors.Paper, size = 44.dp)
        Box(
            modifier = Modifier
                .size(width = 64.dp, height = 88.dp)
                .background(CardColors.Paper, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            HiddenRating(size = 38.dp)
        }
        ChipDisc(color = DraftDuelTheme.colors.secondaryBid.container, ringColor = CardColors.Paper, size = 44.dp)
    }
}

/** The clock with the price, and the three raises. */
@Composable
private fun AuctionPicture() {
    val colors = DraftDuelTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        TurnTimerRing(progress = 0.65f, value = "4", caption = "7s", size = 76.dp)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(1, 2, 5).forEach { step ->
                InfoPill(background = if (step == 2) colors.accent else colors.panel) {
                    Text("+$step", style = DraftDuelTheme.type.monoStrong, color = if (step == 2) colors.onAccent else colors.onScreen)
                }
            }
        }
    }
}

/** Points left and a half-full hand of five. */
@Composable
private fun BudgetPicture() {
    val colors = DraftDuelTheme.colors
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        CardPile(listOf(PileCard.Back(6), PileCard.Back(3), PileCard.Back(2)))
        InfoPill {
            Text(stringResource(Res.string.match_slots, 3, 5), style = DraftDuelTheme.type.mono, color = colors.onScreen)
            BudgetMeter(points = 9, max = 20, color = colors.accent)
        }
    }
}

/** Cards turned over and the final score. */
@Composable
private fun RevealPicture() {
    val colors = DraftDuelTheme.colors
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(contentAlignment = Alignment.Center) {
            MiniCard(PileCard.Face(rating = 88, jerseyNumber = 9), width = 46.dp, modifier = Modifier.offset(x = (-40).dp).rotate(-10f))
            MiniCard(PileCard.Face(rating = 74, jerseyNumber = 4), width = 46.dp, modifier = Modifier.offset(x = 40.dp).rotate(10f))
            MiniCard(PileCard.Face(rating = 93, jerseyNumber = 10, highlighted = true), width = 52.dp)
        }
        InfoPill {
            Text("2", style = DraftDuelTheme.type.number, color = colors.accent)
            Text(":", style = DraftDuelTheme.type.title, color = colors.muted)
            Text("1", style = DraftDuelTheme.type.number, color = colors.onScreen)
        }
        Text(
            text = stringResource(Res.string.result_ratings_line, 412, 398),
            style = DraftDuelTheme.type.mono,
            color = colors.muted,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview
@Composable
private fun HowToPlayScreenPreview() {
    DraftDuelTheme {
        HowToPlayScreen(onClose = {})
    }
}
