package com.vidanstojic.draftduel.domain.model

enum class GameMode {
    /** "Find opponent". In v1 the opponent is the on-device bot; v2 adds real matchmaking. */
    QUICK_MATCH,

    /** Invite link or code. */
    FRIEND,

    /** Two people, one phone. */
    SAME_DEVICE,

    /** Against a bot the player picks. Runs on the phone, so it also works offline. */
    VS_BOT,
}

/** How a same-device match is laid out. */
enum class LocalLayout {
    /** Mirrored halves, phone lying flat between the players. */
    SPLIT_SCREEN,

    /** One view at a time, with a hand-off screen between turns. */
    PASS_DEVICE,
}
