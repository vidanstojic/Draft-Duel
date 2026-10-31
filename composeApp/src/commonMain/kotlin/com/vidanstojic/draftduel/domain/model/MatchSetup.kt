package com.vidanstojic.draftduel.domain.model

import com.vidanstojic.draftduel.domain.bot.BotPersonality
import com.vidanstojic.draftduel.domain.match.MatchRules

/** Everything chosen before a match starts. Passed between screens through the session repository. */
data class MatchSetup(
    val mode: GameMode,
    val theme: Theme,
    val poolSource: PoolSource,
    val you: PlayerProfile,
    val opponent: PlayerProfile? = null,
    val localLayout: LocalLayout = LocalLayout.SPLIT_SCREEN,
    /** Who the bot plays like. Null means a random personality (quick match). */
    val bot: BotPersonality? = null,
    val rules: MatchRules = MatchRules.DEFAULT,
) {
    /** The rules as played: recipe packs keep their own skip rule instead of slots and hand-overs. */
    val effectiveRules: MatchRules get() = if (theme.scoring == Scoring.RECIPE) rules.forRecipe() else rules
}
