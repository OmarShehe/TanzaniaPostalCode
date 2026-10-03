package com.omarshehe.tzaddress.model

import kotlin.test.Test
import kotlin.test.assertEquals

/** Java callers and code compiled against 0.1.x call the shorter constructors, so they must stay in the bytecode. */
class ConstructorCompatibilityTest {
    @Test
    fun wardKeepsItsThreeArgumentConstructor() {
        val ward = Ward::class.java.getConstructor(String::class.java, String::class.java, String::class.java).newInstance("11101", "Kivukoni", "11")
        assertEquals(Ward("11101", "Kivukoni", "11"), ward)
        assertEquals(null, ward.latitude)
    }

    @Test
    fun datasetInfoKeepsItsThreeArgumentConstructor() {
        val info = DatasetInfo::class.java.getConstructor(String::class.java, String::class.java, String::class.java).newInstance("1", "2012-07-30", "t")
        assertEquals(DatasetInfo("1", "2012-07-30", "t"), info)
        assertEquals("", info.attribution)
    }
}
