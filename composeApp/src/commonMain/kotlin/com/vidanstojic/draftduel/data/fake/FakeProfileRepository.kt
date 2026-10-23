package com.vidanstojic.draftduel.data.fake

import com.vidanstojic.draftduel.domain.model.PlayerProfile
import com.vidanstojic.draftduel.domain.repository.ProfileRepository
import kotlin.random.Random

class FakeProfileRepository(
    private val random: Random = Random.Default,
) : ProfileRepository {

    override fun currentPlayer(): PlayerProfile = PlayerProfile(nickname = "You", country = "Serbia", wins = 12)

    override fun quickMatchOpponent(): PlayerProfile = botIdentities.random(random)

    private val botIdentities = listOf(
        PlayerProfile(nickname = "Marco_23", country = "Italy", wins = 41),
        PlayerProfile(nickname = "lukadraft", country = "Croatia", wins = 17),
        PlayerProfile(nickname = "Ana.K", country = "Greece", wins = 63),
        PlayerProfile(nickname = "bigman_99", country = "Spain", wins = 8),
        PlayerProfile(nickname = "Tomasz", country = "Poland", wins = 29),
    )
}
