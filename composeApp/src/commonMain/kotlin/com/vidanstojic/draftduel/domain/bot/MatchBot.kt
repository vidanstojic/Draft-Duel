package com.vidanstojic.draftduel.domain.bot

import com.vidanstojic.draftduel.domain.match.MatchAction
import com.vidanstojic.draftduel.domain.match.MatchState
import com.vidanstojic.draftduel.domain.match.Seat

/**
 * An on-device opponent (spec §12). It sees what a player sees: the card on the table, both
 * budgets and the ratings of cards it has won, but never the pool order or hidden ratings.
 *
 * One instance plays one match, so implementations may remember things between turns
 * (e.g. how they judged the current card).
 */
interface MatchBot {

    /** The action for [seat], or null when [seat] is not the one to act. */
    fun decide(state: MatchState, seat: Seat): MatchAction?

    /** Delay before acting, so the bot feels human. Always well under the 10 s turn clock. */
    fun thinkTimeMillis(): Long
}
