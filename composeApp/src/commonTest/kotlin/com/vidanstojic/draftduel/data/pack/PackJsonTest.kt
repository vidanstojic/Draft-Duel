package com.vidanstojic.draftduel.data.pack

import com.vidanstojic.draftduel.domain.model.PoolSource
import com.vidanstojic.draftduel.domain.model.Theme
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PackJsonTest {

    private val sample = """
        {
          "id": "football-top5",
          "version": 1,
          "theme": "Football",
          "categories": ["England", "Spain"],
          "athletes": [
            {"id": "fb-Q1", "name": "Harry Kane", "rating": 99, "categories": ["Germany"], "position": "FW",
             "season": "2025/26", "avatar": {"color": "#B9770E", "number": 7, "initials": "HK"}, "future": true},
            {"id": "fb-Q2", "name": "Keeper", "rating": 76, "categories": ["Spain"], "position": "GK",
             "avatar": {"color": "#212F3D", "number": 1, "initials": "KE"}},
            {"id": "fb-Q3", "name": "Broken", "rating": 120, "categories": ["Spain"],
             "avatar": {"color": "#000000", "number": 2, "initials": "BR"}}
          ]
        }
    """.trimIndent()

    @Test
    fun decode_mapsPackFormatToDomain() {
        val pack = PackJson.decode(sample, Theme.FOOTBALL)

        assertEquals("football-top5", pack.id)
        assertEquals(1, pack.version)
        assertEquals(listOf("england", "spain", "germany"), pack.categories.map { it.id })
        assertEquals(2, pack.cards.size, "ratings outside 60-99 are dropped")

        val kane = pack.cards.first()
        assertEquals("FWD", kane.position)
        assertEquals("2025/26", kane.season)
        assertEquals("Germany", kane.country)
        assertEquals(setOf("germany"), kane.categoryIds)
        assertEquals(7, kane.avatar.jerseyNumber)
        assertEquals("", pack.cards[1].season)
    }

    @Test
    fun bundledRepository_fallsBackToSamplePackWhenFileIsBroken() = runTest {
        val repository = BundledPackRepository(readFile = { "not json".encodeToByteArray() })
        repository.load()
        assertTrue(repository.pack(Theme.FOOTBALL)!!.cards.isNotEmpty())
    }

    @Test
    fun bundledRepository_drawsFromCategoryWhenItHasEnoughAthletes() = runTest {
        val athletes = (1..12).joinToString(",") { i ->
            """{"id": "a$i", "name": "A$i", "rating": ${60 + i}, "categories": ["${if (i <= 10) "Spain" else "Italy"}"],
               "avatar": {"color": "#111111", "number": $i, "initials": "A$i"}}"""
        }
        val json = """{"id": "p", "version": 2, "theme": "Football", "athletes": [$athletes]}"""
        val repository = BundledPackRepository(readFile = { json.encodeToByteArray() })
        repository.load()

        assertEquals(2, repository.pack(Theme.FOOTBALL)!!.version)
        val spain = repository.drawPool(Theme.FOOTBALL, PoolSource.ByCategory("spain"))
        assertEquals(10, spain.size)
        assertTrue(spain.all { "spain" in it.categoryIds })
        // Italy has only 2 athletes, so the pool comes from the whole pack.
        assertEquals(10, repository.drawPool(Theme.FOOTBALL, PoolSource.ByCategory("italy")).size)
    }
}
