package com.vidanstojic.draftduel.ui.surface

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.vidanstojic.draftduel.domain.model.Theme
import com.vidanstojic.draftduel.ui.theme.BidColors
import com.vidanstojic.draftduel.ui.theme.SurfacePalette

/**
 * The surface seen from above behind a match. The theme of the pack decides which one is used;
 * the table is the neutral surface for menus and mixed content.
 */
enum class SurfaceStyle(
    val palette: SurfacePalette,
    val shape: Shape,
    /** Keeps content inside the drawn area (the table's rounded rim, the pitch lines). */
    val contentPadding: PaddingValues,
) {
    TABLE(
        palette = SurfacePalette(
            screen = Color(0xFF24150E),
            onScreen = Color(0xFFF4ECD6),
            muted = Color(0xFFD9C99A),
            accent = Color(0xFFF7D774),
            onAccent = Color(0xFF13382D),
            accentEdge = Color(0xFFB8932F),
            panel = Color(0x47000000),
            outline = Color(0x99E9CF8C),
            opponent = Color(0xFF3B7BD8),
            onOpponent = Color.White,
            primaryBid = BidColors(Color(0xFFD2462F), Color.White, Color(0xFF8A2A1B)),
            secondaryBid = BidColors(Color(0xFF3B7BD8), Color.White, Color(0xFF23508F)),
            disabledBid = BidColors(Color(0xFF4A4F4C), Color(0xFFC9CFCC), Color(0xFF2E322F)),
        ),
        shape = RoundedCornerShape(120.dp),
        contentPadding = PaddingValues(horizontal = 26.dp, vertical = 24.dp),
    ),
    PITCH(
        palette = SurfacePalette(
            screen = Color(0xFF10241A),
            onScreen = Color(0xFFF4ECD6),
            muted = Color(0xFFCFE3C9),
            accent = Color(0xFFF7D774),
            onAccent = Color(0xFF13261A),
            accentEdge = Color(0xFFB8932F),
            panel = Color(0xB8081E0E),
            outline = Color(0x8CCFE3C9),
            opponent = Color(0xFF3B7BD8),
            onOpponent = Color.White,
            primaryBid = BidColors(Color(0xFFF4ECD6), Color(0xFF13261A), Color(0xFF9A9178)),
            secondaryBid = BidColors(Color(0xFFF7D774), Color(0xFF13261A), Color(0xFFB8932F)),
            disabledBid = BidColors(Color(0xFF33463A), Color(0xFF9FB3A6), Color(0xFF1D2B22)),
        ),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 20.dp),
    ),
    COURT(
        palette = SurfacePalette(
            screen = Color(0xFF141A2B),
            onScreen = Color(0xFFF4ECD6),
            muted = Color(0xFFC9D3EA),
            accent = Color(0xFFFF8A3D),
            onAccent = Color(0xFF141A2B),
            accentEdge = Color(0xFFB35A20),
            panel = Color(0xD1141A2B),
            outline = Color(0x80C9D3EA),
            opponent = Color(0xFF3B7BD8),
            onOpponent = Color.White,
            primaryBid = BidColors(Color(0xFFFF8A3D), Color(0xFF141A2B), Color(0xFFB35A20)),
            secondaryBid = BidColors(Color(0xFF3B7BD8), Color.White, Color(0xFF23508F)),
            disabledBid = BidColors(Color(0xFF2B3349), Color(0xFF8D97B0), Color(0xFF1A2033)),
        ),
        shape = RoundedCornerShape(6.dp),
        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 20.dp),
    ),
    CLAY(
        palette = SurfacePalette(
            screen = Color(0xFF2A140C),
            onScreen = Color(0xFFFBEFE6),
            muted = Color(0xFFF0D2C2),
            accent = Color(0xFFF7D774),
            onAccent = Color(0xFF2A140C),
            accentEdge = Color(0xFFB8932F),
            panel = Color(0xB82A140C),
            outline = Color(0x80F0D2C2),
            opponent = Color(0xFF3B7BD8),
            onOpponent = Color.White,
            primaryBid = BidColors(Color(0xFFFBEFE6), Color(0xFF2A140C), Color(0xFFB9A99A)),
            secondaryBid = BidColors(Color(0xFF2F8F6A), Color.White, Color(0xFF1D5C44)),
            disabledBid = BidColors(Color(0xFF5A3A2C), Color(0xFFC9A99A), Color(0xFF3A2218)),
        ),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 26.dp, vertical = 20.dp),
    ),
}

fun Theme.surfaceStyle(): SurfaceStyle = when (this) {
    Theme.FOOTBALL -> SurfaceStyle.PITCH
    Theme.BASKETBALL -> SurfaceStyle.COURT
    Theme.TENNIS -> SurfaceStyle.CLAY
}
