package com.omarshehe.tzaddress.importer

import java.io.File
import java.nio.file.Files
import java.sql.DriverManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class AddressDbBuilderTest {
    private val dir = Files.createTempDirectory("addressdb").toFile()
    private val dataset = DatasetFixtures.dataset(
        listOf(
            RegionDto(
                "11000", "Dar es Salaam",
                listOf(
                    DistrictDto(
                        "11", "Ilala CBD",
                        listOf(
                            WardDto("11101", "Kivukoni", listOf(MtaaDto("Kivukoni", listOf("Sea View", "Jang'ombe")), MtaaDto("Ferry", emptyList()))),
                            WardDto("11102", "Kariakoo", emptyList()),
                        ),
                    ),
                ),
            ),
        ),
    ).toCore()

    private fun <T> query(file: File, sql: String, read: (java.sql.ResultSet) -> T): List<T> =
        DriverManager.getConnection("jdbc:sqlite:${file.absolutePath}").use { c ->
            c.createStatement().executeQuery(sql).use { rs -> buildList { while (rs.next()) add(read(rs)) } }
        }

    @Test
    fun tableCountsEqualTheDataset() {
        val file = File(dir, "a.db")
        val counts = AddressDbBuilder.build(dataset, file)
        assertEquals(AddressDbCounts(1, 1, 2, 2, 2), counts)
        assertEquals(counts, AddressDbBuilder.readCounts(file))
    }

    @Test
    fun storesDatasetInfo() {
        val file = File(dir, "b.db")
        AddressDbBuilder.build(dataset, file)
        val row = query(file, "SELECT version, source_edition, generated_at FROM dataset_info") { Triple(it.getString(1), it.getString(2), it.getString(3)) }
        assertEquals(listOf(Triple("1", "test", "2026-01-01T00:00:00Z")), row)
    }

    @Test
    fun storesWardPositionsAndNullForUnmatchedWards() {
        val withPoints = DatasetDto(
            InfoDto("2", "test", "2026-01-01T00:00:00Z", "© OpenStreetMap contributors"),
            listOf(
                RegionDto(
                    "11000", "Dar es Salaam",
                    listOf(DistrictDto("11", "Ilala", listOf(WardDto("11101", "Kivukoni", emptyList(), -6.81234, 39.28765), WardDto("11102", "Kariakoo", emptyList())))),
                ),
            ),
        ).toCore()
        val file = File(dir, "points.db")
        AddressDbBuilder.build(withPoints, file)
        val rows = query(file, "SELECT postcode, latitude, longitude FROM ward ORDER BY postcode") {
            Triple(it.getString(1), it.getObject(2) as Double?, it.getObject(3) as Double?)
        }
        assertEquals(listOf(Triple("11101", -6.81234, 39.28765), Triple("11102", null, null)), rows)
        assertEquals(listOf("© OpenStreetMap contributors"), query(file, "SELECT attribution FROM dataset_info") { it.getString(1) })
    }

    @Test
    fun searchIndexHasOneRowPerNodeWithNormalisedText() {
        val file = File(dir, "c.db")
        AddressDbBuilder.build(dataset, file)
        val rows = query(file, "SELECT level, ref_id, name, path_text FROM search_index WHERE name = 'jangombe'") {
            listOf(it.getString(1), it.getString(2), it.getString(3), it.getString(4))
        }
        assertEquals(1, rows.size)
        assertEquals("KITONGOJI", rows[0][0])
        assertEquals("dar es salaam ilala cbd kivukoni kivukoni", rows[0][3])
        assertEquals(1 + 1 + 2 + 2 + 2, query(file, "SELECT count(*) FROM search_index") { it.getInt(1) }.single())
    }

    @Test
    fun ftsPrefixQueryFindsNodes() {
        val file = File(dir, "d.db")
        AddressDbBuilder.build(dataset, file)
        val hits = query(file, "SELECT ref_id FROM search_index WHERE search_index MATCH '\"kar\"*'") { it.getString(1) }
        assertEquals(listOf("11102"), hits)
    }

    @Test
    fun rebuildingOverwritesTheFile() {
        val file = File(dir, "e.db")
        AddressDbBuilder.build(dataset, file)
        AddressDbBuilder.build(dataset, file)
        assertTrue(file.length() > 0)
        assertEquals(1, AddressDbBuilder.readCounts(file).regions)
    }

    @Test
    fun verifyFailsWhenCountsDiffer() {
        val error = assertFailsWith<IllegalStateException> {
            AddressDbCounts(30, 163, 3416, 15820, 16883).verifyEquals(AddressDbCounts(30, 163, 3416, 15820, 16882), "report")
        }
        assertTrue("kitongoji" in error.message!!)
    }

    @Test
    fun parsesReportTotals() {
        val report = "## Totals\n\n- Regions: 30\n- Districts: 163\n- Wards: 3416\n- Mtaa/village (incl. Zanzibar shehia): 15820\n- Kitongoji: 16883\n- Source lines parsed: 1\n"
        assertEquals(AddressDbCounts(30, 163, 3416, 15820, 16883), AddressDbCounts.fromReport(report))
    }
}
