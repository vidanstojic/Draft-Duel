package com.vidanstojic.draftduel.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe destinations. Setup data travels through MatchSessionRepository, so only the
 * category screen needs an argument (the mode, as its enum name).
 */
sealed interface Route {

    @Serializable
    data object Home : Route

    @Serializable
    data class ChooseCategory(val mode: String) : Route

    @Serializable
    data object FindOpponent : Route

    @Serializable
    data object InviteFriend : Route
}
