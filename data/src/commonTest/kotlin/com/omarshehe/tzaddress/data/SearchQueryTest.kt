package com.omarshehe.tzaddress.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SearchQueryTest {
    @Test fun singleTokenBecomesQuotedPrefix() = assertEquals("\"kivu\"*", SearchQuery.match("kivu"))
    @Test fun tokensAreAnded() = assertEquals("\"ilala\"* \"kariakoo\"*", SearchQuery.match("ilala  Kariakoo"))
    @Test fun apostrophesAndCaseAreNormalised() = assertEquals("\"jangombe\"*", SearchQuery.match("Jang'ombe"))
    @Test fun ftsOperatorsAreNeutralised() = assertEquals("\"a\"* \"or\"* \"b\"*", SearchQuery.match("a\" OR b*"))
    @Test fun punctuationOnlyIsNull() = assertNull(SearchQuery.match("-:*()"))
    @Test fun blankIsNull() = assertNull(SearchQuery.match("   "))
    @Test fun limitIsClamped() {
        assertEquals(1, SearchQuery.clampLimit(0))
        assertEquals(1, SearchQuery.clampLimit(-5))
        assertEquals(100, SearchQuery.clampLimit(1000))
        assertEquals(20, SearchQuery.clampLimit(20))
    }
}
