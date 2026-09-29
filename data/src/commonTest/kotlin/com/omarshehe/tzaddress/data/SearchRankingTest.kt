package com.omarshehe.tzaddress.data

import com.omarshehe.tzaddress.Level
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SearchRankingTest {
    @Test fun exactBeatsPrefixBeatsToken() {
        val exact = SearchRanking.score("kivukoni", "Kivukoni")
        val prefix = SearchRanking.score("kivu", "Kivukoni")
        val token = SearchRanking.score("kivu", "Mji wa Kivukoni")
        assertTrue(exact > prefix, "$exact > $prefix")
        assertTrue(prefix > token, "$prefix > $token")
    }

    @Test fun caseAndApostropheAreIgnored() =
        assertEquals(SearchRanking.score("jangombe", "Jangombe"), SearchRanking.score("JANG'OMBE", "Jang'ombe"))

    @Test fun ancestorOnlyTokenScoresByNameCoverage() {
        val partial = SearchRanking.score("ilala kariakoo", "Kariakoo")
        val none = SearchRanking.score("ilala kariakoo", "Gerezani")
        assertTrue(partial > none, "$partial > $none")
    }

    @Test fun higherLevelWinsTies() {
        val ordered = listOf(
            SearchRanking.Ranked(1.0, Level.MTAA, "kariakoo", "m"),
            SearchRanking.Ranked(1.0, Level.WARD, "kariakoo", "w"),
        ).sortedWith(SearchRanking.order)
        assertEquals(listOf("w", "m"), ordered.map { it.refId })
    }

    @Test fun orderIsDeterministicByNameThenId() {
        val ordered = listOf(
            SearchRanking.Ranked(1.0, Level.WARD, "b", "2"),
            SearchRanking.Ranked(1.0, Level.WARD, "a", "9"),
            SearchRanking.Ranked(1.0, Level.WARD, "a", "1"),
        ).sortedWith(SearchRanking.order)
        assertEquals(listOf("1", "9", "2"), ordered.map { it.refId })
    }

    @Test fun higherScoreWinsOverLevel() {
        val ordered = listOf(
            SearchRanking.Ranked(1.0, Level.REGION, "x", "r"),
            SearchRanking.Ranked(3.0, Level.KITONGOJI, "y", "k"),
        ).sortedWith(SearchRanking.order)
        assertEquals("k", ordered.first().refId)
    }
}
