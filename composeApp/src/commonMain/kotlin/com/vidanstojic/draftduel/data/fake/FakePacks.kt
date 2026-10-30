package com.vidanstojic.draftduel.data.fake

import com.vidanstojic.draftduel.domain.model.GameCard
import com.vidanstojic.draftduel.domain.model.AvatarColorway
import com.vidanstojic.draftduel.domain.model.AvatarSpec
import com.vidanstojic.draftduel.domain.model.PackCategory
import com.vidanstojic.draftduel.domain.model.Theme
import com.vidanstojic.draftduel.domain.model.ThemePack

/**
 * Sample packs for the UI demo. Ratings are placeholders written by hand, not copied from
 * EA or 2K. Real packs will be versioned JSON downloaded at startup (spec §5).
 */
internal object FakePacks {

    private const val LEGENDS = "legends"
    private const val PREMIER = "premier-league"
    private const val LA_LIGA = "la-liga"
    private const val NBA = "nba"
    private const val EUROLEAGUE = "euroleague"

    val football = ThemePack(
        id = "football-v1",
        theme = Theme.FOOTBALL,
        name = "Football",
        version = 1,
        categories = listOf(
            PackCategory(LEGENDS, "Legends"),
            PackCategory(PREMIER, "Premier League"),
            PackCategory(LA_LIGA, "La Liga"),
        ),
        cards = listOf(
            card("fb-messi-12", "L. Messi", "2011/12", "Spain", "FWD", 97, 10, AvatarColorway.NAVY_GOLD, LA_LIGA),
            card("fb-ronaldo-08", "C. Ronaldo", "2007/08", "England", "FWD", 95, 7, AvatarColorway.PLUM_CREAM, PREMIER, LEGENDS),
            card("fb-iniesta-10", "A. Iniesta", "2009/10", "Spain", "MID", 90, 8, AvatarColorway.FOREST_SAND, LA_LIGA, LEGENDS),
            card("fb-modric-18", "L. Modrić", "2017/18", "Spain", "MID", 91, 10, AvatarColorway.SLATE_LIME, LA_LIGA),
            card("fb-vandijk-19", "V. van Dijk", "2018/19", "England", "DEF", 89, 4, AvatarColorway.TEAL_ORANGE, PREMIER),
            card("fb-salah-18", "M. Salah", "2017/18", "England", "FWD", 90, 11, AvatarColorway.NAVY_GOLD, PREMIER),
            card("fb-kante-17", "N. Kanté", "2016/17", "England", "MID", 87, 7, AvatarColorway.FOREST_SAND, PREMIER),
            card("fb-ramos-17", "S. Ramos", "2016/17", "Spain", "DEF", 88, 4, AvatarColorway.PLUM_CREAM, LA_LIGA),
            card("fb-lewandowski-20", "R. Lewandowski", "2019/20", "Germany", "FWD", 92, 9, AvatarColorway.SLATE_LIME),
            card("fb-debruyne-20", "K. De Bruyne", "2019/20", "England", "MID", 91, 17, AvatarColorway.TEAL_ORANGE, PREMIER),
            card("fb-buffon-06", "G. Buffon", "2005/06", "Italy", "GK", 89, 1, AvatarColorway.NAVY_GOLD, LEGENDS),
            card("fb-pirlo-12", "A. Pirlo", "2011/12", "Italy", "MID", 86, 21, AvatarColorway.FOREST_SAND, LEGENDS),
            card("fb-henry-04", "T. Henry", "2003/04", "England", "FWD", 93, 14, AvatarColorway.PLUM_CREAM, PREMIER, LEGENDS),
            card("fb-vidic-09", "N. Vidić", "2008/09", "England", "DEF", 86, 15, AvatarColorway.SLATE_LIME, PREMIER, LEGENDS),
            card("fb-stankovic-10", "D. Stanković", "2009/10", "Italy", "MID", 82, 5, AvatarColorway.TEAL_ORANGE, LEGENDS),
            card("fb-alonso-12", "X. Alonso", "2011/12", "Spain", "MID", 84, 14, AvatarColorway.NAVY_GOLD, LA_LIGA, LEGENDS),
            card("fb-oblak-18", "J. Oblak", "2017/18", "Spain", "GK", 87, 13, AvatarColorway.FOREST_SAND, LA_LIGA),
            card("fb-tadic-19", "D. Tadić", "2018/19", "Netherlands", "MID", 85, 10, AvatarColorway.PLUM_CREAM),
            card("fb-xavi-11", "Xavi", "2010/11", "Spain", "MID", 92, 6, AvatarColorway.SLATE_LIME, LA_LIGA, LEGENDS),
            card("fb-lampard-10", "F. Lampard", "2009/10", "England", "MID", 88, 8, AvatarColorway.TEAL_ORANGE, PREMIER, LEGENDS),
        ),
    )

    val basketball = ThemePack(
        id = "basketball-v1",
        theme = Theme.BASKETBALL,
        name = "Basketball",
        version = 1,
        categories = listOf(
            PackCategory(LEGENDS, "Legends"),
            PackCategory(NBA, "NBA"),
            PackCategory(EUROLEAGUE, "EuroLeague"),
        ),
        cards = listOf(
            card("bb-jokic-23", "N. Jokić", "2022/23", "USA", "C", 97, 15, AvatarColorway.TEAL_ORANGE, NBA),
            card("bb-james-13", "L. James", "2012/13", "USA", "SF", 96, 6, AvatarColorway.PLUM_CREAM, NBA),
            card("bb-curry-16", "S. Curry", "2015/16", "USA", "PG", 95, 30, AvatarColorway.NAVY_GOLD, NBA),
            card("bb-durant-14", "K. Durant", "2013/14", "USA", "SF", 93, 35, AvatarColorway.SLATE_LIME, NBA),
            card("bb-giannis-20", "G. Antetokounmpo", "2019/20", "USA", "PF", 94, 34, AvatarColorway.FOREST_SAND, NBA),
            card("bb-doncic-24", "L. Dončić", "2023/24", "USA", "PG", 94, 77, AvatarColorway.NAVY_GOLD, NBA),
            card("bb-nowitzki-11", "D. Nowitzki", "2010/11", "USA", "PF", 90, 41, AvatarColorway.TEAL_ORANGE, NBA, LEGENDS),
            card("bb-teodosic-16", "M. Teodosić", "2015/16", "Russia", "PG", 86, 4, AvatarColorway.PLUM_CREAM, EUROLEAGUE),
            card("bb-micic-21", "V. Micić", "2020/21", "Turkey", "PG", 87, 22, AvatarColorway.SLATE_LIME, EUROLEAGUE),
            card("bb-llull-17", "S. Llull", "2016/17", "Spain", "PG", 85, 23, AvatarColorway.FOREST_SAND, EUROLEAGUE),
            card("bb-spanoulis-13", "V. Spanoulis", "2012/13", "Greece", "PG", 88, 7, AvatarColorway.NAVY_GOLD, EUROLEAGUE, LEGENDS),
            card("bb-parker-07", "T. Parker", "2006/07", "USA", "PG", 88, 9, AvatarColorway.TEAL_ORANGE, NBA, LEGENDS),
            card("bb-gasol-09", "P. Gasol", "2008/09", "USA", "PF", 89, 16, AvatarColorway.PLUM_CREAM, NBA, LEGENDS),
            card("bb-divac-01", "V. Divac", "2000/01", "USA", "C", 83, 21, AvatarColorway.SLATE_LIME, NBA, LEGENDS),
            card("bb-bogdanovic-17", "B. Bogdanović", "2016/17", "USA", "SG", 82, 44, AvatarColorway.FOREST_SAND, NBA),
            card("bb-diamantidis-11", "D. Diamantidis", "2010/11", "Greece", "PG", 87, 13, AvatarColorway.NAVY_GOLD, EUROLEAGUE, LEGENDS),
            card("bb-navarro-09", "J. C. Navarro", "2008/09", "Spain", "SG", 86, 11, AvatarColorway.TEAL_ORANGE, EUROLEAGUE, LEGENDS),
            card("bb-shved-17", "A. Shved", "2016/17", "Russia", "SG", 81, 1, AvatarColorway.PLUM_CREAM, EUROLEAGUE),
            card("bb-bjelica-15", "N. Bjelica", "2014/15", "Turkey", "PF", 79, 8, AvatarColorway.SLATE_LIME, EUROLEAGUE),
            card("bb-vesely-19", "J. Veselý", "2018/19", "Turkey", "PF", 80, 24, AvatarColorway.FOREST_SAND, EUROLEAGUE),
        ),
    )

    private fun card(
        id: String,
        name: String,
        season: String,
        country: String,
        position: String,
        rating: Int,
        number: Int,
        colorway: AvatarColorway,
        vararg categories: String,
    ) = GameCard(
        id = id,
        name = name,
        season = season,
        country = country,
        position = position,
        rating = rating,
        avatar = AvatarSpec(jerseyNumber = number, initials = initialsOf(name), colorway = colorway),
        categoryIds = categories.toSet(),
    )

    private fun initialsOf(name: String): String =
        name.split(' ', '.').filter { it.isNotBlank() }.let { parts ->
            (parts.first().take(1) + parts.last().take(1)).uppercase()
        }
}
