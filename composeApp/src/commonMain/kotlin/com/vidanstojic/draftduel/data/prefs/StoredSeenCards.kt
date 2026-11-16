package com.vidanstojic.draftduel.data.prefs

import com.vidanstojic.draftduel.domain.model.SeenCardsStore
import com.vidanstojic.draftduel.platform.KeyValueStore

/** [SeenCardsStore] on top of the phone's key-value store: card ids, one per line. */
class StoredSeenCards(private val store: KeyValueStore) : SeenCardsStore {

    override fun load(key: String): Set<String> =
        store.getString(PREFIX + key)?.split('\n')?.filterTo(HashSet()) { it.isNotBlank() }.orEmpty()

    override fun save(key: String, ids: Set<String>) {
        store.putString(PREFIX + key, ids.joinToString("\n"))
    }

    private companion object {
        const val PREFIX = "seen_cards."
    }
}
