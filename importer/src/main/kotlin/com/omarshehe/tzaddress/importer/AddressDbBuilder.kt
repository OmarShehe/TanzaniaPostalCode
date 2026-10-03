package com.omarshehe.tzaddress.importer

import com.omarshehe.tzaddress.AddressText
import com.omarshehe.tzaddress.Level
import java.io.File
import java.sql.Connection
import java.sql.DriverManager

/** Writes the read-only address database from the flattened dataset. Insertion order is the dataset order. */
object AddressDbBuilder {

    /** Builds [file] (replacing any existing one) and returns the counts read back from it. */
    fun build(dataset: CoreDataset, file: File): AddressDbCounts {
        file.parentFile?.mkdirs()
        file.delete()
        connect(file).use { c ->
            c.autoCommit = false
            AddressDbSchema.statements.forEach { c.createStatement().use { s -> s.execute(it) } }
            insert(c, dataset)
            c.commit()
            c.autoCommit = true
            c.createStatement().use { it.execute("INSERT INTO search_index(search_index) VALUES('optimize')") }
            c.createStatement().use { it.execute("VACUUM") }
        }
        val counts = readCounts(file)
        counts.verifyEquals(
            AddressDbCounts(dataset.regions.size, dataset.districts.size, dataset.wards.size, dataset.mtaas.size, dataset.kitongojis.size),
            "the dataset",
        )
        return counts
    }

    fun readCounts(file: File): AddressDbCounts = connect(file).use { c ->
        fun count(table: String) = c.createStatement().use { s -> s.executeQuery("SELECT count(*) FROM $table").use { it.next(); it.getInt(1) } }
        AddressDbCounts(count("region"), count("district"), count("ward"), count("mtaa"), count("kitongoji"))
    }

    private fun connect(file: File): Connection = DriverManager.getConnection("jdbc:sqlite:${file.absolutePath}")

    private fun insert(c: Connection, d: CoreDataset) {
        val regions = d.regions.associateBy { it.code }
        val districts = d.districts.associateBy { it.code }
        val wards = d.wards.associateBy { it.postcode }
        val mtaas = d.mtaas.associateBy { it.id }
        val index = c.prepareStatement("INSERT INTO search_index(level, ref_id, name, path_text) VALUES (?, ?, ?, ?)")

        fun indexRow(level: Level, id: String, name: String, ancestors: List<String>) {
            index.setString(1, level.name)
            index.setString(2, id)
            index.setString(3, AddressText.normalize(name))
            index.setString(4, ancestors.joinToString(" ") { AddressText.normalize(it) }.trim())
            index.addBatch()
        }

        c.prepareStatement("INSERT INTO region(code, name) VALUES (?, ?)").use { st ->
            d.regions.forEach { st.setString(1, it.code); st.setString(2, it.name); st.addBatch(); indexRow(Level.REGION, it.code, it.name, emptyList()) }
            st.executeBatch()
        }
        c.prepareStatement("INSERT INTO district(code, name, region_code) VALUES (?, ?, ?)").use { st ->
            d.districts.forEach {
                st.setString(1, it.code); st.setString(2, it.name); st.setString(3, it.regionCode); st.addBatch()
                indexRow(Level.DISTRICT, it.code, it.name, listOf(regions.getValue(it.regionCode).name))
            }
            st.executeBatch()
        }
        c.prepareStatement("INSERT INTO ward(postcode, name, district_code, latitude, longitude) VALUES (?, ?, ?, ?, ?)").use { st ->
            d.wards.forEach {
                st.setString(1, it.postcode); st.setString(2, it.name); st.setString(3, it.districtCode)
                if (it.latitude == null || it.longitude == null) {
                    st.setNull(4, java.sql.Types.REAL); st.setNull(5, java.sql.Types.REAL)
                } else {
                    st.setDouble(4, it.latitude!!); st.setDouble(5, it.longitude!!)
                }
                st.addBatch()
                val district = districts.getValue(it.districtCode)
                indexRow(Level.WARD, it.postcode, it.name, listOf(regions.getValue(district.regionCode).name, district.name))
            }
            st.executeBatch()
        }
        c.prepareStatement("INSERT INTO mtaa(id, name, ward_postcode) VALUES (?, ?, ?)").use { st ->
            d.mtaas.forEach {
                st.setString(1, it.id); st.setString(2, it.name); st.setString(3, it.wardPostcode); st.addBatch()
                val ward = wards.getValue(it.wardPostcode)
                val district = districts.getValue(ward.districtCode)
                indexRow(Level.MTAA, it.id, it.name, listOf(regions.getValue(district.regionCode).name, district.name, ward.name))
            }
            st.executeBatch()
        }
        c.prepareStatement("INSERT INTO kitongoji(id, name, mtaa_id) VALUES (?, ?, ?)").use { st ->
            d.kitongojis.forEach {
                st.setString(1, it.id); st.setString(2, it.name); st.setString(3, it.mtaaId); st.addBatch()
                val mtaa = mtaas.getValue(it.mtaaId)
                val ward = wards.getValue(mtaa.wardPostcode)
                val district = districts.getValue(ward.districtCode)
                indexRow(Level.KITONGOJI, it.id, it.name, listOf(regions.getValue(district.regionCode).name, district.name, ward.name, mtaa.name))
            }
            st.executeBatch()
        }
        index.executeBatch()
        index.close()
        c.prepareStatement("INSERT INTO dataset_info(version, source_edition, generated_at, attribution) VALUES (?, ?, ?, ?)").use { st ->
            st.setString(1, d.info.version); st.setString(2, d.info.sourceEdition); st.setString(3, d.info.generatedAt); st.setString(4, d.info.attribution)
            st.executeUpdate()
        }
    }
}
