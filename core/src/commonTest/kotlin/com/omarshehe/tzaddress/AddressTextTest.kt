package com.omarshehe.tzaddress

import kotlin.test.Test
import kotlin.test.assertEquals

class AddressTextTest {
    @Test fun apostrophesAreDropped() = assertEquals("jangombe", AddressText.normalize("Jang'ombe"))
    @Test fun caseIsFolded() = assertEquals("kariakoo", AddressText.normalize("KARIAKOO"))
    @Test fun quotesBecomeSeparatorsOrVanish() = assertEquals("mtambani a", AddressText.normalize("Mtambani \"A\""))
    @Test fun typographicApostropheIsDropped() = assertEquals("ngomas", AddressText.normalize("Ngoma’s"))
    @Test fun whitespaceIsCollapsed() = assertEquals("a b", AddressText.normalize("  A \t  B  "))
    @Test fun hyphensBecomeSpaces() = assertEquals("ol molog", AddressText.normalize("Ol-molog"))
    @Test fun diacriticsAreStripped() = assertEquals("cafe", AddressText.normalize("Café"))
    @Test fun blankStaysEmpty() = assertEquals("", AddressText.normalize("   "))
    @Test fun tokensSplitOnSpaces() = assertEquals(listOf("ilala", "kariakoo"), AddressText.tokens("Ilala  Kariakoo"))
    @Test fun tokensOfBlankAreEmpty() = assertEquals(emptyList(), AddressText.tokens(" "))
}
