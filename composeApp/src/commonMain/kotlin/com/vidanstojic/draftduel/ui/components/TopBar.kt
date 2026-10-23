package com.vidanstojic.draftduel.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.vidanstojic.draftduel.ui.theme.DraftDuelTheme

/** 44 dp bar: a leading control, a centered title and a trailing slot of equal width. */
@Composable
fun DdTopBar(
    modifier: Modifier = Modifier,
    leading: @Composable () -> Unit = {},
    trailing: @Composable () -> Unit = {},
    title: @Composable () -> Unit,
) {
    val slot = Modifier.widthIn(min = DraftDuelTheme.spacing.touch)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(DraftDuelTheme.spacing.touch),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(slot, contentAlignment = Alignment.CenterStart) { leading() }
        title()
        Box(slot, contentAlignment = Alignment.CenterEnd) { trailing() }
    }
}

/** Small caps label over a value, e.g. "CARD" / "06 of 10". */
@Composable
fun TopBarTitle(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        SectionLabel(label)
        Text(
            text = value,
            style = DraftDuelTheme.type.monoStrong,
            color = DraftDuelTheme.colors.onScreen,
            textAlign = TextAlign.Center,
        )
    }
}
