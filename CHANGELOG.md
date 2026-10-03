# Changelog

Versions follow [semantic versioning](README.md#versioning). Every release records the dataset behind it.

## 0.2.0 (unreleased)

- `tz-address-core`: `Ward` gains nullable `latitude` and `longitude` (a position inside the ward, for centring a map, not a property location). `DatasetInfo` gains `attribution`. The three-argument `Ward(...)` and `DatasetInfo(...)` constructors still work, for Kotlin and Java; `copy` and `componentN` on both change shape.
- `tz-address-data`: reads the new columns; `info().attribution` returns the data notice. The bundled database is replaced on first open (dataset version `3`); no migration code needed.
- Dataset: `DatasetInfo.version` = `3`, source edition `2016-04-22 (Gazette Notice 240); Zanzibar 2012-07-30`: 31 regions, 168 districts, 4,058 wards, 17,039 mtaa/shehia, 64,262 kitongoji. Adds Songwe and the districts created after 2012 (Kigamboni, Ubungo, Malinyi, Kibiti, and the four Songwe districts). 3,100 wards have a position, from OpenStreetMap ward boundaries (ODbL 1.0; see the README's Licence section).
- Migration: 162 wards have a new postcode, and ward postcodes are ids, so postcodes saved from 0.1.x may no longer match. `dataset/edition-changes.md` lists old and new postcodes.
- Importer: `importPostcodes` reads a folder of TCRA regional files (`-PpdfDir`); new `importWardPoints` task; new reports `edition-changes.md`, `ward-points-report.md` and `ward-points-anomalies.csv` in `dataset/`. The ward-position coverage gate is now 75%.

## 0.1.1 (2026-10-03)

- `tz-address-data`: the JVM target now uses sqlite-jdbc instead of the AndroidX bundled SQLite, so it runs on Intel Macs and Windows as well as Linux and Apple-silicon Macs. Android and iOS are unchanged. The dataset is unchanged (`DatasetInfo.version` = `1`).

## 0.1.0 (2026-10-03)

First release.

- `tz-address-ui` uses [`io.github.omarshehe:forminput`](https://github.com/OmarShehe/FormInputs) 2.1.0 (outlined text field and dropdown).
- `tz-address-core`, `tz-address-data`, `tz-address-ui` 0.1.0 (group `io.github.omarshehe`).
- Dataset: `DatasetInfo.version` = `1`, source edition `2012-07-30` (30 regions, 163 districts, 3,416 wards, 15,820 mtaa/shehia, 16,883 kitongoji).
- Not included: Songwe region (created 2016) and districts created after 2012 such as Kigamboni.
