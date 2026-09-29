# Data source

The address data in `dataset/tz-address.json` is derived from a PDF postcode list (`tzPostcodeList.pdf`, 1,211 pages, edition dated 2012-07-30). The PDF itself is **not** in this repository. Its metadata is generic (`Microsoft Word - REGION.DAR`), so where it came from cannot be read from the file.

The maintainer must fill in the facts below before the first public release. The release workflow refuses to run while any `TODO(maintainer)` marker remains in this file.

| Fact | Value |
|---|---|
| Title of the document | TODO(maintainer) |
| Publisher / issuing body | TODO(maintainer) |
| Edition / date | 2012-07-30 (from the file; confirm) |
| How it was obtained (URL, date, person) | TODO(maintainer) |
| Terms under which the list and a derived dataset may be redistributed | TODO(maintainer) |
| Attribution text to show users | TODO(maintainer) |

## What the importer does

`:importer` reads the PDF's text layer and rebuilds the Region → District → Ward → Mtaa/Village → Kitongoji hierarchy. It normalises names and reports anomalies in `dataset/import-report.md` and `dataset/import-anomalies.csv`. It never edits the source; where the PDF is inconsistent (for example 140 wards that list kitongoji without an mtaa), the dataset keeps what is printed and the report says so.

## Known gaps

- 30 regions only: Songwe (2016) and post-2012 districts such as Kigamboni are not in this edition.
- Names appear as printed, including 16 entries with a source numbering prefix (for example `60. Kaseme A Mabamba`).
