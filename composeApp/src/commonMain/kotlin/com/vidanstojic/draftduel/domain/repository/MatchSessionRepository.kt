package com.vidanstojic.draftduel.domain.repository

import com.vidanstojic.draftduel.domain.match.MatchState
import com.vidanstojic.draftduel.domain.model.MatchSetup

/**
 * Hands the chosen setup and the finished match between screens, so navigation routes stay
 * argument-free. In-memory for now; the online version will be backed by the server session.
 */
interface MatchSessionRepository {

    var setup: MatchSetup?

    var lastFinishedMatch: MatchState?
}

class InMemoryMatchSessionRepository : MatchSessionRepository {
    override var setup: MatchSetup? = null
    override var lastFinishedMatch: MatchState? = null
}
