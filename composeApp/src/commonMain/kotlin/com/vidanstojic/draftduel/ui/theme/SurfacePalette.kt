package com.vidanstojic.draftduel.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/** Colors of one bid chip: face, label and the darker edge drawn under it. */
@Immutable
data class BidColors(
    val container: Color,
    val content: Color,
    val edge: Color,
)

/**
 * Color tokens that change with the play surface (table, pitch, court, clay).
 * Components read them through [DraftDuelTheme.colors] and never hard-code surface colors.
 */
@Immutable
data class SurfacePalette(
    /** Background around the play surface. */
    val screen: Color,
    val onScreen: Color,
    val muted: Color,
    val accent: Color,
    val onAccent: Color,
    val accentEdge: Color,
    /** Translucent panels and pills drawn over the surface. */
    val panel: Color,
    val outline: Color,
    /** The opponent's color, used by the "leads" badge together with an arrow and text. */
    val opponent: Color,
    val onOpponent: Color,
    val primaryBid: BidColors,
    val secondaryBid: BidColors,
    val disabledBid: BidColors,
)

/** Colors that stay the same on every surface: the printed cards. */
object CardColors {
    val Paper = Color(0xFFF4ECD6)
    val PaperEdge = Color(0xFFC9B98A)
    val Ink = Color(0xFF13382D)
    val InkMuted = Color(0xFF3F6457)
    val BackStripeA = Color(0xFF1C2A5A)
    val BackStripeB = Color(0xFF24366F)
    val BackInk = Color(0xFFF7D774)
    val Holo = listOf(
        Color(0xFFF7D774),
        Color(0xFFFF9FD2),
        Color(0xFF8FE3FF),
        Color(0xFFB9FFB0),
        Color(0xFFF7D774),
    )
}
