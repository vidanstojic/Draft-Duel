package com.vidanstojic.draftduel.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Two families: a display/body sans and a mono for numbers, seasons and labels.
 *
 * TODO: bundle Bricolage Grotesque and IBM Plex Mono in composeResources/font and swap the
 * families here; nothing else needs to change.
 */
@Immutable
data class DraftDuelTypography(
    val displayFamily: FontFamily = FontFamily.SansSerif,
    val monoFamily: FontFamily = FontFamily.Monospace,
) {
    val hero = TextStyle(fontFamily = displayFamily, fontWeight = FontWeight.ExtraBold, fontSize = 46.sp, lineHeight = 48.sp)
    val headline = TextStyle(fontFamily = displayFamily, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, lineHeight = 26.sp)
    val title = TextStyle(fontFamily = displayFamily, fontWeight = FontWeight.Bold, fontSize = 17.sp, lineHeight = 22.sp)
    val body = TextStyle(fontFamily = displayFamily, fontWeight = FontWeight.Medium, fontSize = 15.sp, lineHeight = 20.sp)
    val bodyStrong = body.copy(fontWeight = FontWeight.Bold)
    val caption = TextStyle(fontFamily = displayFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 17.sp)
    val number = TextStyle(fontFamily = displayFamily, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp, lineHeight = 28.sp)
    val numberLarge = number.copy(fontSize = 44.sp, lineHeight = 46.sp)
    val label = TextStyle(fontFamily = monoFamily, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, letterSpacing = 1.5.sp)
    val mono = TextStyle(fontFamily = monoFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp)
    val monoStrong = mono.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp)
}
