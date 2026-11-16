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

    @Serializable
    data object ChooseBot : Route

    @Serializable
    data object Match : Route

    @Serializable
    data object Result : Route

    @Serializable
    data object LocalSetup : Route

    @Serializable
    data object LocalMatch : Route

    @Serializable
    data object HowToPlay : Route
}
