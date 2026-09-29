package com.omarshehe.tzaddress.importer

import kotlin.test.Test
import kotlin.test.assertEquals

class NameNormalizerTest {
    private fun n(s: String) = NameNormalizer.normalize(s)

    @Test fun upperCase_isTitleCased() {
        assertEquals("Kivukoni", n("KIVUKONI"))
        assertEquals("Upanga Mashariki", n("UPANGA MASHARIKI"))
    }

    @Test fun lowerCase_isTitleCased() = assertEquals("Kikwajuni Juu", n("kikwajuni juu"))

    @Test fun mixedCaseWords_areKept() {
        assertEquals("Mkombozi", n("Mkombozi"))
        assertEquals("Kigaga A", n("Kigaga A"))
    }

    @Test fun singleLetterAndQuotedLetter_stayUpper() {
        assertEquals("Mtambani \"A\"", n("Mtambani \"A\""))
        assertEquals("Songwe B", n("SONGWE B"))
    }

    @Test fun apostrophes_areKeptAndDoNotCapitalizeNext() {
        assertEquals("Jang'ombe", n("JANG'OMBE"))
        assertEquals("Ng'ombeni", n("ng'ombeni"))
    }

    @Test fun typographicQuotes_areNormalized() {
        assertEquals("Jang'ombe", n("Jang’ombe"))
        assertEquals("Mtambani \"A\"", n("Mtambani “A”"))
    }

    @Test fun whitespace_isTrimmedAndCollapsed() = assertEquals("Sea View", n("  Sea    View \n"))

    @Test fun connectors_areLowerCaseMidName() = assertEquals("Mto wa Mbu", n("MTO WA MBU"))

    @Test fun hyphenatedParts_areEachCapitalized() = assertEquals("Mji-Mpya", n("MJI-MPYA"))

    @Test fun romanNumerals_stayUpper() = assertEquals("Kata II", n("kata II"))

    @Test fun tokensWithDigits_areKept() = assertEquals("Block 5A", n("BLOCK 5A"))

    @Test fun knownAcronyms_stayUpper() = assertEquals("Ilala CBD", n("ILALA CBD"))

    @Test fun esConnector_isLowerCase() = assertEquals("Dar es Salaam", n("DAR ES SALAAM"))

    @Test fun typographicHyphens_becomeAsciiHyphens() {
        assertEquals("Oldonyo-Sambu", n("OLDONYO\u2010SAMBU"))
        assertEquals("Lyamungo-Kati", n("Lyamungo\u2013Kati"))
        assertEquals("Ol-Molog", n("ol\u2212molog"))
    }
}
