package com.vidanstojic.draftduel.data.pack

import com.vidanstojic.draftduel.domain.match.GameRules
import com.vidanstojic.draftduel.domain.model.GameCard
import com.vidanstojic.draftduel.domain.model.AvatarColorway
import com.vidanstojic.draftduel.domain.model.AvatarSpec
import com.vidanstojic.draftduel.domain.model.PackCategory
import com.vidanstojic.draftduel.domain.model.Scoring
import com.vidanstojic.draftduel.domain.model.Theme
import com.vidanstojic.draftduel.domain.model.ThemePack
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Pack file format (spec §5), as written by `tools/export_pack.py` or by hand. `position`, `season`,
 * `country` and `avatar` are optional extras the card shows; unknown fields are ignored so newer
 * packs still load. In recipe packs (food) each category is a dish and `rating` is the
 * ingredient's points for that dish, negative for ingredients that do not belong.
 */
@Serializable
internal data class PackDto(
    val id: String,
    val version: Int,
    val theme: String,
    val categories: List<String> = emptyList(),
    val athletes: List<AthleteDto>,
)

@Serializable
internal data class AthleteDto(
    val id: String,
    val name: String,
    val rating: Int,
    val categories: List<String> = emptyList(),
    val position: String? = null,
    val season: String? = null,
    /** Shown on the card. Football packs without it use the league country (their category). */
    val country: String? = null,
    val avatar: AvatarDto? = null,
)

@Serializable
internal data class AvatarDto(
    val color: String,
    val number: Int,
    val initials: String,
)

internal object PackJson {

    private val json = Json { ignoreUnknownKeys = true }

    fun decode(text: String, theme: Theme): ThemePack = json.decodeFromString<PackDto>(text).toDomain(theme)
}

internal fun PackDto.toDomain(theme: Theme): ThemePack {
    val categoryNames = (categories + athletes.flatMap { it.categories }).distinct()
    return ThemePack(
        id = id,
        theme = theme,
        name = this.theme,
        version = version,
        categories = categoryNames.map { PackCategory(id = categoryId(it), name = it) },
        cards = athletes
            .filter { it.rating in validValues(theme.scoring) }
            .map { it.toDomain() },
    )
}

/** Ratings are 60–99 (spec §5); recipe points stay within one dish's 100. */
internal fun validValues(scoring: Scoring): IntRange = when (scoring) {
    Scoring.RATINGS -> GameRules.MIN_RATING..GameRules.MAX_RATING
    Scoring.RECIPE -> -100..100
}

private fun AthleteDto.toDomain() = GameCard(
    id = id,
    name = name,
    season = season.orEmpty(),
    // Football categories are league countries (no club names, spec §5).
    country = country ?: categories.firstOrNull().orEmpty(),
    position = position?.let(::positionCode).orEmpty(),
    rating = rating,
    avatar = AvatarSpec(
        jerseyNumber = avatar?.number ?: 0,
        initials = avatar?.initials ?: initialsOf(name),
        colorway = colorwayFor(avatar?.color ?: id),
    ),
    categoryIds = categories.map(::categoryId).toSet(),
)

internal fun initialsOf(name: String): String =
    name.split(' ', '-').filter { it.isNotBlank() }.let { parts ->
        when {
            parts.isEmpty() -> "?"
            parts.size == 1 -> parts.first().take(2)
            else -> parts.first().take(1) + parts.last().take(1)
        }
    }.uppercase()

internal fun categoryId(name: String): String = name.trim().lowercase().replace(' ', '-')

/** Pack positions (GK/DF/MF/FW) to the short codes the cards use. Other themes pass through. */
internal fun positionCode(position: String): String = when (position.uppercase()) {
    "DF" -> "DEF"
    "MF" -> "MID"
    "FW" -> "FWD"
    else -> position.uppercase()
}

/**
 * Avatars use neutral colorways rather than arbitrary colors (so no club colors sneak in), so the
 * pack color only picks one of them, stably.
 */
internal fun colorwayFor(color: String): AvatarColorway {
    val entries = AvatarColorway.entries
    return entries[color.lowercase().hashCode().mod(entries.size)]
}
