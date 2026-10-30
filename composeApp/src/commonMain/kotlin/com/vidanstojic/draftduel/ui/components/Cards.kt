package com.vidanstojic.draftduel.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vidanstojic.draftduel.domain.model.GameCard
import com.vidanstojic.draftduel.domain.model.Scoring
import com.vidanstojic.draftduel.domain.model.Theme
import com.vidanstojic.draftduel.resources.Res
import com.vidanstojic.draftduel.resources.avatar_description
import com.vidanstojic.draftduel.resources.card_art_description
import com.vidanstojic.draftduel.resources.card_paid
import com.vidanstojic.draftduel.resources.points_short
import com.vidanstojic.draftduel.resources.rating_hidden
import com.vidanstojic.draftduel.resources.rating_short
import com.vidanstojic.draftduel.ui.theme.CardColors
import com.vidanstojic.draftduel.ui.theme.DraftDuelTheme
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private val HoloBrush = Brush.sweepGradient(CardColors.Holo)

private val CardBackBrush = Brush.linearGradient(
    0f to CardColors.BackStripeA,
    0.5f to CardColors.BackStripeA,
    0.5f to CardColors.BackStripeB,
    1f to CardColors.BackStripeB,
    start = Offset.Zero,
    end = Offset(14f, 14f),
    tileMode = TileMode.Repeated,
)

/** The card on auction (or just won): holographic frame, avatar, name, season, country. */
@Composable
fun GameCardView(
    card: GameCard,
    theme: Theme,
    ratingVisible: Boolean,
    modifier: Modifier = Modifier,
    width: Dp = 150.dp,
    tilt: Float = -3f,
    glow: Boolean = false,
) {
    val frameShape = RoundedCornerShape(14.dp)
    Box(
        modifier = modifier
            .rotate(tilt)
            .shadow(if (glow) 24.dp else 10.dp, frameShape, ambientColor = Color.Black, spotColor = if (glow) CardColors.BackInk else Color.Black)
            .background(HoloBrush, frameShape)
            .padding(3.dp),
    ) {
        Column(
            modifier = Modifier
                .width(width)
                .background(CardColors.Paper, RoundedCornerShape(11.dp))
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box {
                PlayerAvatar(
                    spec = card.avatar,
                    theme = theme,
                    contentDescription = cardArtDescription(card, theme),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(width * 0.6f)
                        .clip(RoundedCornerShape(8.dp)),
                )
                PositionTag(card.position, Modifier.align(Alignment.TopEnd).padding(6.dp))
            }
            Text(
                text = card.name,
                style = DraftDuelTheme.type.title.copy(fontWeight = FontWeight.ExtraBold),
                color = CardColors.Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = listOf(card.season, card.country.uppercase()).filter { it.isNotBlank() }.joinToString("\n"),
                    style = DraftDuelTheme.type.mono.copy(fontSize = 10.sp, lineHeight = 13.sp, fontWeight = FontWeight.SemiBold),
                    color = CardColors.InkMuted,
                )
                if (ratingVisible) RatingBadge(card.rating, size = 46.dp, scoring = theme.scoring) else HiddenRating(size = 42.dp)
            }
        }
    }
}

/** Horizontal card for tight layouts (split screen). */
@Composable
fun GameCardCompact(
    card: GameCard,
    theme: Theme,
    ratingVisible: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .background(HoloBrush, RoundedCornerShape(10.dp))
            .padding(2.dp),
    ) {
        Row(
            modifier = Modifier
                .background(CardColors.Paper, RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlayerAvatar(
                spec = card.avatar,
                theme = theme,
                contentDescription = cardArtDescription(card, theme),
                modifier = Modifier
                    .size(width = 38.dp, height = 48.dp)
                    .clip(RoundedCornerShape(5.dp)),
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(card.name, style = DraftDuelTheme.type.bodyStrong, color = CardColors.Ink, maxLines = 1)
                Text(
                    text = card.details,
                    style = DraftDuelTheme.type.mono.copy(fontSize = 9.sp),
                    color = CardColors.InkMuted,
                )
                Text(
                    text = "${stringResource(theme.scoring.scoreLabel())} ${if (ratingVisible) theme.scoring.format(card.rating) else "??"}",
                    style = DraftDuelTheme.type.mono.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                    color = CardColors.Ink,
                )
            }
        }
    }
}

@Composable
private fun PositionTag(position: String, modifier: Modifier = Modifier) {
    Text(
        text = position,
        style = DraftDuelTheme.type.mono.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
        color = CardColors.Ink,
        modifier = modifier
            .background(CardColors.BackInk, RoundedCornerShape(3.dp))
            .padding(horizontal = 4.dp, vertical = 2.dp),
    )
}

/** Short label of a card's value: rating or recipe points. */
fun Scoring.scoreLabel(): StringResource = when (this) {
    Scoring.RATINGS -> Res.string.rating_short
    Scoring.RECIPE -> Res.string.points_short
}

/** Recipe points carry a sign, so a wrong ingredient reads as a loss at a glance. */
fun Scoring.format(value: Int): String = if (this == Scoring.RECIPE && value > 0) "+$value" else value.toString()

@Composable
private fun cardArtDescription(card: GameCard, theme: Theme): String = when (theme) {
    Theme.MOVIES, Theme.FOOD -> stringResource(Res.string.card_art_description, card.name)
    Theme.FOOTBALL, Theme.BASKETBALL, Theme.TENNIS -> stringResource(Res.string.avatar_description, card.avatar.jerseyNumber)
}

@Composable
fun RatingBadge(rating: Int, size: Dp, modifier: Modifier = Modifier, scoring: Scoring = Scoring.RATINGS) {
    Column(
        modifier = modifier
            .size(size)
            .background(CardColors.Ink, CircleShape),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = scoring.format(rating),
            color = CardColors.BackInk,
            fontWeight = FontWeight.ExtraBold,
            fontSize = (size.value * 0.46f).sp,
            lineHeight = (size.value * 0.48f).sp,
        )
        Text(
            text = stringResource(scoring.scoreLabel()),
            color = CardColors.BackInk,
            style = DraftDuelTheme.type.mono.copy(fontSize = 7.sp, letterSpacing = 1.sp),
        )
    }
}

/** Dashed "??" disc shown while the rating is hidden. */
@Composable
fun HiddenRating(size: Dp, modifier: Modifier = Modifier, color: Color = CardColors.Ink) {
    val description = stringResource(Res.string.rating_hidden)
    Box(
        modifier = modifier
            .size(size)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.matchParentSize()) {
            val stroke = 2.dp.toPx()
            drawCircle(
                color = color,
                radius = this.size.minDimension / 2 - stroke,
                style = Stroke(width = stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 4f))),
            )
        }
        Text("??", color = color, fontWeight = FontWeight.ExtraBold, fontSize = (size.value * 0.42f).sp)
    }
}

/** One entry of a player's pile: face up (own card) or face down with the paid price. */
sealed interface PileCard {
    data class Face(val rating: Int, val jerseyNumber: Int, val highlighted: Boolean = false) : PileCard

    data class Back(val paidPrice: Int) : PileCard
}

private val MiniCardShape = RoundedCornerShape(6.dp)
private val PileTilts = listOf(-7f, -1f, 6f, -4f, 3f, -6f, 1f, 5f, -2f, 4f)

@Composable
fun MiniCard(card: PileCard, modifier: Modifier = Modifier, width: Dp = 42.dp) {
    val height = width * 1.33f
    when (card) {
        is PileCard.Face -> Column(
            modifier = modifier
                .size(width, height)
                .shadow(4.dp, MiniCardShape)
                .background(CardColors.Paper, MiniCardShape)
                .border(
                    width = if (card.highlighted) 2.dp else 1.dp,
                    color = if (card.highlighted) CardColors.BackInk else CardColors.PaperEdge,
                    shape = MiniCardShape,
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("#${card.jerseyNumber}", style = DraftDuelTheme.type.mono.copy(fontSize = 8.sp), color = CardColors.InkMuted)
            Text(card.rating.toString(), style = DraftDuelTheme.type.monoStrong, color = CardColors.Ink)
        }
        is PileCard.Back -> Column(
            modifier = modifier
                .size(width, height)
                .shadow(4.dp, MiniCardShape)
                .background(CardBackBrush, MiniCardShape)
                .border(2.dp, CardColors.Paper, MiniCardShape),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(stringResource(Res.string.card_paid), style = DraftDuelTheme.type.mono.copy(fontSize = 8.sp), color = CardColors.BackInk)
            Text(card.paidPrice.toString(), style = DraftDuelTheme.type.monoStrong, color = CardColors.BackInk)
        }
    }
}

/** Overlapping, slightly scattered pile. Grows with every card won (no 5-card limit). */
@Composable
fun CardPile(
    cards: List<PileCard>,
    modifier: Modifier = Modifier,
    cardWidth: Dp = 42.dp,
    overlap: Dp = 12.dp,
) {
    val step = cardWidth - overlap
    val count = cards.size.coerceAtLeast(1)
    Box(modifier.size(width = cardWidth + step * (count - 1), height = cardWidth * 1.4f)) {
        if (cards.isEmpty()) {
            EmptyPileSlot(cardWidth, Modifier.align(Alignment.Center))
        }
        cards.forEachIndexed { index, card ->
            MiniCard(
                card = card,
                width = cardWidth,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = step * index)
                    .rotate(PileTilts[index % PileTilts.size]),
            )
        }
    }
}

@Composable
private fun EmptyPileSlot(width: Dp, modifier: Modifier = Modifier) {
    val outline = DraftDuelTheme.colors.outline
    Canvas(modifier.size(width, width * 1.33f)) {
        drawRoundRect(
            color = outline,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx()),
            style = Stroke(width = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))),
        )
    }
}
