package com.omarshehe.tzaddress.importer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NameSimilarityTest {

    @Test fun identicalKeys_scoreOne() = assertEquals(1.0, NameSimilarity.score("mikunguni", "mikunguni"))

    @Test fun oneLetterDifferenceInALongName_scoresHigh() {
        assertEquals(8.0 / 9.0, NameSimilarity.score("mikinguni", "mikunguni"), 1e-9)
    }

    @Test fun theSameDistanceInAShortName_scoresLow() {
        assertTrue(NameSimilarity.score("kati", "katu") < 0.8)
    }

    @Test fun differentFirstLetter_isNeverSimilar() {
        assertEquals(0.0, NameSimilarity.score("bwakasumbe", "mwakasumbe"))
    }

    @Test fun anInsertionCountsAsOneEdit() {
        assertEquals(9.0 / 10.0, NameSimilarity.score("hananasif", "hananasifu"), 1e-9)
    }

    @Test fun emptyKeys_areNotSimilar() = assertEquals(0.0, NameSimilarity.score("", "a"))
}
