package com.vidanstojic.draftduel.domain.repository

import com.vidanstojic.draftduel.domain.model.PlayerProfile

interface ProfileRepository {

    fun currentPlayer(): PlayerProfile

    /** Opponent shown by "Find opponent". In v1 this is the bot's identity. */
    fun quickMatchOpponent(): PlayerProfile
}
