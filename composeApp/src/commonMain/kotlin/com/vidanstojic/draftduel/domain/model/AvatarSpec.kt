package com.vidanstojic.draftduel.domain.model

/**
 * Generated avatar: a faceless silhouette in a jersey. No photos, crests or club colors (spec §5).
 */
data class AvatarSpec(
    val jerseyNumber: Int,
    val initials: String,
    val colorway: AvatarColorway,
)

/** Neutral color combinations that do not imitate a real club or national team. */
enum class AvatarColorway {
    NAVY_GOLD,
    TEAL_ORANGE,
    PLUM_CREAM,
    FOREST_SAND,
    SLATE_LIME,
}
