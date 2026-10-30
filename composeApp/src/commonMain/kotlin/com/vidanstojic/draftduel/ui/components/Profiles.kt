package com.vidanstojic.draftduel.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.vidanstojic.draftduel.domain.model.Theme
import com.vidanstojic.draftduel.resources.Res
import com.vidanstojic.draftduel.resources.theme_basketball
import com.vidanstojic.draftduel.resources.theme_football
import com.vidanstojic.draftduel.resources.theme_tennis
import org.jetbrains.compose.resources.StringResource

/** Label of a theme, shared by every screen that names it. */
fun Theme.label(): StringResource = when (this) {
    Theme.FOOTBALL -> Res.string.theme_football
    Theme.BASKETBALL -> Res.string.theme_basketball
    Theme.TENNIS -> Res.string.theme_tennis
}

/** Round player badge with initials (players, not athletes). */
@Composable
fun ProfileBadge(
    name: String,
    color: Color,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(size)
            .shadow(size * 0.08f, CircleShape)
            .background(color, CircleShape)
            .border(size * 0.04f, Color.White.copy(alpha = 0.6f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initialsOf(name),
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            fontSize = (size.value * 0.3f).sp,
        )
    }
}

fun initialsOf(name: String): String =
    name.filter { it.isLetter() }.take(2).uppercase().ifEmpty { "?" }
