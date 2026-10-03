# Data source

The address data in `dataset/tz-address.json` is derived from the Tanzanian postcode list published by the Tanzania Communications
Regulatory Authority (TCRA). References:

- <https://www.tcra.go.tz/services/publication-of-postcode-list> (the regional lists, Government Gazette Notice No. 240 of 22 April 2016)
- <https://www.tanzaniapostcode.com/>

The repository publishes only the facts extracted from that list (region, district and ward names and codes, mtaa/village
and kitongoji names) as `dataset/tz-address.json`. It does not store copies of the source files.

| Fact | Value |
|---|---|
| Data | Mainland: TCRA's 26 regional postcode lists (PDF, produced February 2016, uploaded to the TCRA site 22 June 2021, retrieved 2026-10-04). Zanzibar: the postcode list dated 2012-07-30, because TCRA's regional lists do not include Zanzibar. |
| Availability | publicly available online (see the references above) |
| Licence / redistribution terms | none stated here; the data is a list of place names and postal codes. Anyone with a stricter requirement should check the references directly. |
| Attribution | Postcode data: Tanzania Communications Regulatory Authority (TCRA) postcode list, Government Gazette Notice No. 240 of 22 April 2016; Zanzibar from the 2012-07-30 list. See <https://www.tcra.go.tz/services/publication-of-postcode-list>. |

## Ward positions

Each ward's `latitude` and `longitude` are derived from ward boundary polygons published by [geoBoundaries](https://www.geoboundaries.org/)
(release `9469f09`, `gbOpen`, Tanzania), and the importer reduces each polygon to a point inside it.

| Fact | Value |
|---|---|
| Ward boundaries | geoBoundaries ADM3 Tanzania, boundary year 2015, 3,644 units. Source: OpenStreetMap. [Download](https://github.com/wmgeolab/geoBoundaries/raw/9469f09/releaseData/gbOpen/TZA/ADM3/geoBoundaries-TZA-ADM3_simplified.geojson) |
| District boundaries (used only to tell wards of the same name apart; nothing from it is published) | geoBoundaries ADM2 Tanzania, boundary year 2021, 170 units, CC BY 3.0 IGO. [Download](https://github.com/wmgeolab/geoBoundaries/raw/9469f09/releaseData/gbOpen/TZA/ADM2/geoBoundaries-TZA-ADM2_simplified.geojson) |
| Licence of the positions | [Open Database License (ODbL) 1.0](https://opendatacommons.org/licenses/odbl/1-0/) (the boundaries come from OpenStreetMap) |
| Attribution | © OpenStreetMap contributors, via geoBoundaries. The same text is stored as `info.attribution` in `dataset/tz-address.json` and in the database (`dataset_info`), and returned by `info().attribution`. |

The boundary files are not stored in this repository.

### How a ward is matched

The boundary data has names only (no district), so the importer places each boundary ward in a district polygon, then pairs it with a
ward of the postcode list by name (case, spacing, punctuation and the source numbering prefix are ignored): first by district and name, then
by a name that is unique in both lists. A name that fits more than one ward, or a boundary ward wanted twice, gets no position.
`dataset/ward-points-report.md` gives the counts per region and `dataset/ward-points-anomalies.csv` lists every ward without a position and why.

Result for this edition: 3,100 of 4,058 wards have a position (2,669 by district and name, 431 by unique name); 189 are ambiguous and 769 have no
usable boundary (no boundary ward of that name, or the only one lies in a district that the list has only in another region). Most of those are
wards created after the 2015 boundaries. Zanzibar: 119 of 140.

## What the importer does

`:importer` reads the published list files and rebuilds the Region → District → Ward → Mtaa/Village → Kitongoji hierarchy. It
normalises names and reports anomalies in `dataset/import-report.md` and `dataset/import-anomalies.csv`. It never edits the
source; where the list is inconsistent (for example 206 lines that list a kitongoji without an mtaa), the dataset keeps what is
printed and the report says so. A ward whose postcode is malformed or does not start with its district code is left out and named in
`dataset/import-report.md` (three wards in this edition). To rebuild the dataset, download the lists from the references above and pass
them to the importer (see the README). `dataset/edition-changes.md` compares this edition with the 2012 one.

## Known gaps

- 958 wards have no position (the list is in `dataset/ward-points-anomalies.csv`).
- Zanzibar is from the 2012 list; ward changes after 2016 are not reflected.
- Names appear as printed, including a few entries with a source numbering prefix (for example `60. Kaseme A Mabamba`).
