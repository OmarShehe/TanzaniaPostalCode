# TZA-002: Repeatable PDF importer with validation report

**Type:** Feature
**Priority:** High
**Platform:** `:importer` (new, JVM tool)
**Depends on:** TZA-001 (reads the model, ID rules and `DatasetInfo` from `:core`)

---

## Problem Statement

The authoritative address data lives in a 1,211-page PDF (`tzPostcodeList.pdf`, a 2012-07-30 snapshot: 30 regions, Region → District → Ward → Mtaa/Village → Kitongoji). Nothing of it is in the repo. Hand-typing it is infeasible and unauditable, and the PDF's layout makes naive parsing error-prone. The Maintainer needs a repeatable, re-runnable import that produces a canonical dataset and a report showing exactly what was parsed and what looks wrong.

---

## User Stories

- As a **Maintainer**, I want to run one command against the PDF and get a canonical dataset plus a validation report, so that I can trust and review the data before it ships.
- As a **Maintainer**, I want to re-run the same command on a new PDF edition, so that dataset updates don't require code changes.
- As a **Consumer developer**, I want the dataset to be versioned and reproducible, so that a released library version maps to a known source.

---

## Approved Design / Behaviour

### Command

`./gradlew :importer:importPostcodes -Ppdf=/path/to/tzPostcodeList.pdf -PsourceEdition="<label>"`

Outputs (into `dataset/`, committed to git):
- `dataset/tz-address.json` — canonical dataset (see schema below).
- `dataset/import-report.md` — human-readable validation report.
- `dataset/import-anomalies.csv` — one row per anomaly (`page,line,kind,text`).

The importer is a JVM-only Gradle module; it is **not** a dependency of any published artifact.

### Source layout the parser must handle (observed in the PDF)

- Region banner: `<NAME> REGION - <code>` (e.g. `DAR ES SALAAM REGION - 11000`; note the dash is a non-ASCII hyphen `‐`).
- The columns' x-positions **differ on almost every page** (Word auto-sized each table); the parser reads column boundaries from each page's own header row.
- A third layout exists for Zanzibar (16 pages): `REGION POSTCODE DISTRICT POSTCODE WARD (DELIVERY AREAS) POSTCODE SHEHIA`. A shehia is stored as a `Mtaa`; there is no kitongoji level there.
- The column header repeats at the top of every page and comes in **two variants**: `REGION POSTCODE WARD POSTCODE MTAA/VILLAGE KITONGOJI` (Dar es Salaam, where the "REGION" column actually holds the district, e.g. `ILALA CBD 11`) and `REGION POSTCODE DISTRICT POSTCODE WARD POSTCODE MTAA/VILLAGE KITONGOJI` (other regions). Headers must be dropped, not parsed as data.
- Columns are positioned by layout; a ward's mtaa/kitongoji rows continue across page breaks; a row can have blank left columns (continuation of the previous ward/mtaa).
- Names repeat under one parent (e.g. `Rubumba`), contain quotes (`Mtambani "A"`) and apostrophes (`Jang'ombe`), and vary in case (wards upper-case in some regions, title-case in others).

The parser uses column x-positions from the PDF text layer (not regex on flattened text) to assign cells to levels, and normalises names (trim, collapse whitespace, canonical title case with a documented exception list) while preserving the raw printed text in the anomaly CSV.

### Canonical dataset schema

```json
{
  "info": { "version": "1", "sourceEdition": "...", "generatedAt": "ISO-8601" },
  "regions": [ { "code": "11000", "name": "Dar es Salaam",
    "districts": [ { "code": "11", "name": "Ilala CBD",
      "wards": [ { "postcode": "11101", "name": "Kivukoni",
        "mtaas": [ { "name": "Kivukoni", "kitongojis": ["..."] } ] } ] } ] } ]
}
```

IDs for mtaa/kitongoji are **not stored**; they are derived at load time with the deterministic rule owned by TZA-001 (`AddressIds`), so both the importer and TZA-003 call the same function (no reimplementation).

### Validation report (`import-report.md`)

Must contain: dataset info; counts of regions/districts/wards/mtaas/kitongojis (total and per region); ward postcodes not 5 digits; duplicate ward postcodes; ward postcodes whose first digits don't match their district/region code; wards with zero mtaas; districts with zero wards; regions found vs. the 30 expected; and the anomaly count with the first 50 listed.

### Failure policy

The task **fails the build** (non-zero exit) if any of: a region count different from `importer.expectedRegions` (30); duplicate ward postcode; a ward postcode not matching `^\d{5}$`; a ward postcode prefix inconsistent with its district code; anomaly rate above a threshold configured in `importer/gradle.properties` (`importer.maxAnomalyRatio`, default `0.005`). `-Pforce` writes outputs anyway but still marks the report `FAILED`.

---

## Acceptance Criteria

- [ ] Maintainer: running the command on `tzPostcodeList.pdf` produces `tz-address.json`, `import-report.md` and `import-anomalies.csv`, and exits 0 (or non-zero with a clear report if the anomaly policy trips).
- [ ] Maintainer: the report lists exactly 30 regions with their printed region codes, including all five Zanzibar regions (Mjini Magharibi, Kusini Unguja, Kaskazini Unguja, Kusini Pemba, Kaskazini Pemba), and notes that Songwe (created 2016) is absent from this edition.
- [ ] Maintainer: for each region, the report's ward/mtaa/kitongoji counts equal a count made **independently** of the parser (a spot-check test that counts ward-postcode tokens `\d{5}` per region straight from `pdftotext` output). The counts must match for every region.
- [ ] Maintainer: running the command twice on the same PDF yields byte-identical `tz-address.json` (stable ordering, no timestamps except `generatedAt`, which is excluded from the determinism check or passed via `-PgeneratedAt`).
- [ ] Maintainer: a fixture-based unit test set (small PDF pages/text snippets committed under `importer/src/test/resources`) covers: both header variants, page-break continuation, blank-left-column rows, duplicate names, names with `"` and `'`, and the non-ASCII hyphen banner.
- [ ] Maintainer: introducing a duplicate ward postcode into a fixture makes the task fail with the offending code named in the report.
- [ ] Consumer developer: `dataset/tz-address.json` loads with the `:core` model types and every ward's `districtCode` and every district's `regionCode` resolves.
- [ ] Maintainer: the importer module is excluded from published artifacts (no publication configured for `:importer`).

---

## Technical Notes

### Impacted Modules
| Module | Change type | Notes |
|---|---|---|
| `:importer` | New | JVM app/Gradle task, PDF text-with-positions extraction, report writer |
| `:core` | None | Consumed as a dependency for model + `AddressIds` |
| `dataset/` | New | Committed canonical output |

### DB / Migration
No DB changes.

### API Contract
No API changes. File contract is the JSON schema above.

### Key Implementation Notes
- Use a JVM PDF library that exposes text positions (e.g. Apache PDFBox `PDFTextStripper` with per-character positions). Pin the version in `libs.versions.toml`; confirm the current stable at implementation time.
- `pdftotext -layout` was used to survey the file and is fine for the independent count in the spot-check test, but it is a system tool and must not be a build requirement; the spot-check test may be skipped (with a visible skip message) when `pdftotext` is absent.
- **Rejected alternative:** one-off conversion, committing only the data. Cost: no audit trail, no way to review parse decisions, and every future PDF edition means redoing manual work.
- **Rejected alternative:** regex over flattened text. Cost: the multi-column layout means blank left cells look identical to continuation lines; errors are silent and mis-parent wards/mtaas.
- The PDF's producer metadata is generic (`Microsoft Word - REGION.DAR`); the origin/licence of the list is recorded by the Maintainer in TZA-005's `DATA_SOURCE.md`, not inferred by the importer.

---

## Scope notes (added during implementation)

- **30 regions, not 31.** The PDF is a 2012 snapshot; Songwe (2016) and later districts such as Kigamboni are not in it. The importer reproduces the PDF as printed; post-2012 changes are out of scope (a separate supplement would need a second data source).
- **Three layouts, per-page columns.** Dar es Salaam, standard and Zanzibar layouts are handled; columns come from each page's header row.
- **Split and wrapped cells** are recovered by rule: a ward/district code on the line before or after its name, and a name wrapped over two lines, are merged. A ward listing kitongoji with no mtaa is kept under an implicit mtaa named after the ward and recorded as an *informational* anomaly (140 in the current PDF); informational anomalies are excluded from the failure ratio.
- **Names:** typographic hyphens (U+2010–2015, U+2212) are normalised to `-`; `CBD` is kept upper-case; `es`, `wa`, `ya`, `la`, `na` are lower-case mid-name.
- The PDF is not committed (unknown redistribution terms). The real-PDF checks (independent per-region ward counts, determinism) are opt-in via `-Ppdf`.
- `.ai/context.md` was updated in this ticket (module map, import commands).

---

## Out of Scope

- Building the SQLite database (TZA-003).
- Fixing errors in the source PDF itself; anomalies are reported, and manual corrections go through an explicit `importer/overrides.json` mechanism only if the Maintainer adds them (not built here).
- OCR (the PDF has a text layer).
- Coordinates (deferred TZA-006).

---

## Open Questions

None. (Decisions: repeatable task with build-failing validation; committed outputs under `dataset/`; IDs derived via TZA-001's `AddressIds`.)

---

## Files Expected to Change

| File | Change |
|---|---|
| `settings.gradle.kts` | Modified (`include(":importer")`) |
| `gradle/libs.versions.toml` | Modified (PDF library, kotlinx-serialization, test libs) |
| `importer/build.gradle.kts` | New (`importPostcodes` task) |
| `importer/gradle.properties` | New (`importer.maxAnomalyRatio`) |
| `importer/src/main/kotlin/.../PdfColumnParser.kt`, `Normalizer.kt`, `DatasetWriter.kt`, `ReportWriter.kt`, `Validator.kt` | New |
| `importer/src/test/kotlin/...` and `importer/src/test/resources/**` | New (fixtures + tests) |
| `dataset/tz-address.json`, `dataset/import-report.md`, `dataset/import-anomalies.csv` | New (generated, committed) |
