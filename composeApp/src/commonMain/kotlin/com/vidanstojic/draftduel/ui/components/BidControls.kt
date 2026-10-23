package com.vidanstojic.draftduel.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vidanstojic.draftduel.resources.Res
import com.vidanstojic.draftduel.resources.bid_over_budget
import com.vidanstojic.draftduel.resources.bid_raise_description
import com.vidanstojic.draftduel.resources.bid_to_price
import com.vidanstojic.draftduel.ui.theme.BidColors
import com.vidanstojic.draftduel.ui.theme.DraftDuelTheme
import org.jetbrains.compose.resources.stringResource

/** One raise option. Disabled when the new price would exceed the player's budget (spec §6). */
@Immutable
data class BidOptionUi(
    val step: Int,
    val resultingPrice: Int,
    val enabled: Boolean,
)

@Composable
fun BidChip(
    option: BidOptionUi,
    colors: BidColors,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 76.dp,
) {
    val description = stringResource(Res.string.bid_raise_description, option.step, option.resultingPrice)
    EdgeSurface(
        colors = if (option.enabled) colors else DraftDuelTheme.colors.disabledBid,
        onClick = onClick,
        enabled = option.enabled,
        modifier = modifier
            .height(height)
            .semantics { contentDescription = description },
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            val content = LocalContentColor.current
            ChipDisc(color = Color.Transparent, ringColor = content.copy(alpha = if (option.enabled) 0.85f else 0.35f), size = 30.dp)
            Column {
                Text("+${option.step}", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = content)
                Text(
                    text = if (option.enabled) {
                        stringResource(Res.string.bid_to_price, option.resultingPrice)
                    } else {
                        stringResource(Res.string.bid_over_budget)
                    },
                    style = DraftDuelTheme.type.mono.copy(fontSize = 10.sp),
                    color = content,
                )
            }
        }
    }
}

/** +1 / +2 / +5 chips and the pass button, placed under the thumb. */
@Composable
fun BidControls(
    options: List<BidOptionUi>,
    passText: String,
    passEnabled: Boolean,
    onBid: (step: Int) -> Unit,
    onPass: () -> Unit,
    modifier: Modifier = Modifier,
    chipHeight: Dp = 76.dp,
    passHeight: Dp = 50.dp,
) {
    val palette = DraftDuelTheme.colors
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            options.forEachIndexed { index, option ->
                BidChip(
                    option = option,
                    colors = if (index == 0) palette.primaryBid else palette.secondaryBid,
                    onClick = { onBid(option.step) },
                    height = chipHeight,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        DdOutlinedButton(
            text = passText,
            onClick = onPass,
            enabled = passEnabled,
            height = passHeight,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
