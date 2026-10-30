package com.vidanstojic.draftduel.data.pack

import com.vidanstojic.draftduel.domain.match.GameRules
import com.vidanstojic.draftduel.domain.model.AthleteCard
import com.vidanstojic.draftduel.domain.model.AvatarColorway
import com.vidanstojic.draftduel.domain.model.AvatarSpec
import com.vidanstojic.draftduel.domain.model.PackCategory
import com.vidanstojic.draftduel.domain.model.Theme
import com.vidanstojic.draftduel.domain.model.ThemePack
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Pack file format (spec §5), as written by `tools/export_pack.py`. `position` and `season` are
 * optional extras the card shows; unknown fields are ignored so newer packs still load.
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
    val avatar: AvatarDto,
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
            .filter { it.rating in GameRules.MIN_RATING..GameRules.MAX_RATING }
            .map { it.toDomain() },
    )
}

private fun AthleteDto.toDomain() = AthleteCard(
    id = id,
    name = name,
    season = season.orEmpty(),
    // Categories of the football pack are league countries (no club names, spec §5).
    country = categories.firstOrNull().orEmpty(),
    position = position?.let(::positionCode).orEmpty(),
    rating = rating,
    avatar = AvatarSpec(
        jerseyNumber = avatar.number,
        initials = avatar.initials,
        colorway = colorwayFor(avatar.color),
    ),
    categoryIds = categories.map(::categoryId).toSet(),
)

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
