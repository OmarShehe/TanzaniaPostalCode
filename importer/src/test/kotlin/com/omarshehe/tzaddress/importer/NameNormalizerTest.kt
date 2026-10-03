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

    @Test fun acronymsInsideMixedCaseNames_stayUpper() {
        assertEquals("Nyamalembo CCM", n("Nyamalembo CCM"))
        assertEquals("Buligi CCM", n("Buligi CCM"))
    }

    private fun acronym(s: String) = NameNormalizer.normalize(s, shortAllCapsAreAcronyms = true)

    @Test fun shortAllUpperNames_areAcronymsWhenTheLevelAllowsIt() {
        assertEquals("CCM", acronym("CCM"))
        assertEquals("NHC", acronym("NHC"))
        assertEquals("TTC UWT", acronym("TTC UWT"))
        assertEquals("N.H.C.", acronym("N.H.C."))
    }

    @Test fun shortAllUpperNames_areWordsWhenTheLevelIsWardOrDistrict() {
        assertEquals("Hai", n("HAI"))
        assertEquals("Kia", n("KIA"))
        assertEquals("Uzi", n("UZI"))
    }

    @Test fun connectorsInMixedCaseNames_stayLowerCase() {
        assertEquals("Mgulu wa Ndege", n("Mgulu wa Ndege"))
        assertEquals("Kakola na 9", n("Kakola Na 9"))
        assertEquals("Taasisi ya KIA", n("Taasisi ya KIA"))
    }

    @Test fun shoutingNamesWithLongWords_areStillTitleCased() {
        assertEquals("Ukuu", n("UKUU"))
        assertEquals("Mji Mpya", n("MJI MPYA"))
    }

    @Test fun knownStandaloneAcronyms_stayUpperAtEveryLevel() {
        assertEquals("TANESCO", n("TANESCO"))
        assertEquals("TANESCO", NameNormalizer.normalize("TANESCO", shortAllCapsAreAcronyms = true))
        assertEquals("Kituo Cha TANESCO", NameNormalizer.normalize("Kituo cha Tanesco", shortAllCapsAreAcronyms = true))
    }

    @Test fun trailingFootnoteStar_isDropped() {
        assertEquals("Kariakoo", NameNormalizer.normalize("Kariakoo*"))
        assertEquals("Kimbugu \"B\"", NameNormalizer.normalize("Kimbugu \"B\"*"))
    }

    @Test fun acuteAccentInsideAName_isAnApostrophe() {
        assertEquals("Chang'ombe", NameNormalizer.normalize("Chang\u00B4ombe"))
    }
}
