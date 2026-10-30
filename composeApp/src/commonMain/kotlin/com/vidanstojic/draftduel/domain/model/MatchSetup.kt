package com.vidanstojic.draftduel.domain.model

import com.vidanstojic.draftduel.domain.bot.BotPersonality

/** Everything chosen before a match starts. Passed between screens through the session repository. */
data class MatchSetup(
    val mode: GameMode,
    val sport: Sport,
    val poolSource: PoolSource,
    val you: PlayerProfile,
    val opponent: PlayerProfile? = null,
    val localLayout: LocalLayout = LocalLayout.SPLIT_SCREEN,
    /** Who the bot plays like. Null means a random personality (quick match). */
    val bot: BotPersonality? = null,
)
