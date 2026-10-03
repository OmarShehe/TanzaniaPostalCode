package com.omarshehe.tzaddress.importer

/** DDL of the bundled read-only database; `:data` queries these table and column names. */
object AddressDbSchema {
    val statements = listOf(
        "CREATE TABLE region (code TEXT PRIMARY KEY, name TEXT NOT NULL)",
        "CREATE TABLE district (code TEXT PRIMARY KEY, name TEXT NOT NULL, region_code TEXT NOT NULL REFERENCES region(code))",
        "CREATE TABLE ward (postcode TEXT PRIMARY KEY, name TEXT NOT NULL, district_code TEXT NOT NULL REFERENCES district(code), latitude REAL, longitude REAL)",
        "CREATE TABLE mtaa (id TEXT PRIMARY KEY, name TEXT NOT NULL, ward_postcode TEXT NOT NULL REFERENCES ward(postcode))",
        "CREATE TABLE kitongoji (id TEXT PRIMARY KEY, name TEXT NOT NULL, mtaa_id TEXT NOT NULL REFERENCES mtaa(id))",
        "CREATE INDEX district_region ON district(region_code)",
        "CREATE INDEX ward_district ON ward(district_code)",
        "CREATE INDEX mtaa_ward ON mtaa(ward_postcode)",
        "CREATE INDEX kitongoji_mtaa ON kitongoji(mtaa_id)",
        // level and ref_id are stored but not tokenised; name and path_text hold AddressText-normalised text.
        "CREATE VIRTUAL TABLE search_index USING fts5(level UNINDEXED, ref_id UNINDEXED, name, path_text, prefix='2 3 4', tokenize='unicode61')",
        "CREATE TABLE dataset_info (version TEXT NOT NULL, source_edition TEXT NOT NULL, generated_at TEXT NOT NULL, attribution TEXT NOT NULL)",
    )
}
