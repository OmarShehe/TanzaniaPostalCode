package com.omarshehe.tzaddress

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AddressIdsTest {

    @Test
    fun slug_lowercasesAndHyphenates() {
        assertEquals("kivukoni", AddressIds.slug("Kivukoni"))
        assertEquals("kariakoo-magharibi", AddressIds.slug("Kariakoo Magharibi"))
        assertEquals("a-b", AddressIds.slug("  A   B "))
    }

    @Test
    fun slug_dropsApostrophesAndQuotes() {
        assertEquals("jangombe", AddressIds.slug("Jang'ombe"))
        assertEquals("jangombe", AddressIds.slug("Jang’ombe"))
        assertEquals("mtambani-a", AddressIds.slug("Mtambani \"A\""))
    }

    @Test
    fun slug_emptyOrSymbolOnly_isUnnamed() {
        assertEquals("unnamed", AddressIds.slug(""))
        assertEquals("unnamed", AddressIds.slug("---"))
    }

    @Test
    fun mtaaIds_arePrefixedWithWardPostcode() {
        assertEquals(
            listOf("11101/kivukoni", "11101/sea-view"),
            AddressIds.mtaaIds("11101", listOf("Kivukoni", "Sea View")),
        )
    }

    @Test
    fun mtaaIds_disambiguateDuplicatesInEncounterOrder() {
        assertEquals(
            listOf("47405/rubumba", "47405/rubumba-2", "47405/rubumba-3"),
            AddressIds.mtaaIds("47405", listOf("Rubumba", "Rubumba", "Rubumba")),
        )
    }

    @Test
    fun mtaaIds_suffixNeverCollidesWithLiteralName() {
        val ids = AddressIds.mtaaIds("47405", listOf("Rubumba", "Rubumba-2", "Rubumba"))
        assertEquals(3, ids.toSet().size)
        assertEquals("47405/rubumba", ids[0])
        assertEquals("47405/rubumba-2", ids[1])
        assertTrue(ids[2].startsWith("47405/rubumba-"))
    }

    @Test
    fun kitongojiIds_buildOnMtaaId() {
        assertEquals(
            listOf("11101/kivukoni/mlimani", "11101/kivukoni/mlimani-2"),
            AddressIds.kitongojiIds("11101/kivukoni", listOf("Mlimani", "Mlimani")),
        )
    }

    @Test
    fun ids_areDeterministic() {
        val names = listOf("A", "A", "B'c")
        assertEquals(AddressIds.mtaaIds("1", names), AddressIds.mtaaIds("1", names))
    }
}
