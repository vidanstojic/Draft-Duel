package com.vidanstojic.draftduel.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class Spacing(
    val xs: Dp = 4.dp,
    val s: Dp = 8.dp,
    val m: Dp = 12.dp,
    val l: Dp = 16.dp,
    val xl: Dp = 24.dp,
    val screen: Dp = 14.dp,
    /** Minimum touch target (spec §14 accessibility). */
    val touch: Dp = 44.dp,
)
