# Data source

The address data in `dataset/tz-address.json` is derived from the Tanzanian postcode list, which is publicly available.
References:

- <https://www.tanzaniapostcode.com/>
- <https://www.tcra.go.tz/services/publication-of-postcode-list>

The repository publishes only the facts extracted from that list (region, district and ward names and codes, mtaa/village
and kitongoji names) as `dataset/tz-address.json`. It does not store copies of the source files.

| Fact | Value |
|---|---|
| Data | Tanzanian postcode list, edition dated 2012-07-30 |
| Availability | publicly available online (see the references above) |
| Licence / redistribution terms | none stated here; the data is a list of place names and postal codes. Anyone with a stricter requirement should check the references directly. |
| Attribution | Postcode data: public Tanzanian postcode list (2012-07-30 edition); see <https://www.tanzaniapostcode.com/> and <https://www.tcra.go.tz/services/publication-of-postcode-list>. |

## What the importer does

`:importer` reads the published list file and rebuilds the Region → District → Ward → Mtaa/Village → Kitongoji hierarchy. It
normalises names and reports anomalies in `dataset/import-report.md` and `dataset/import-anomalies.csv`. It never edits the
source; where the list is inconsistent (for example 140 wards that list kitongoji without an mtaa), the dataset keeps what is
printed and the report says so. To rebuild the dataset, download the list from the references above and pass its path to the
importer (see the README).

## Known gaps

- 30 regions only: Songwe (2016) and post-2012 districts such as Kigamboni are not in this edition.
- Names appear as printed, including 16 entries with a source numbering prefix (for example `60. Kaseme A Mabamba`).
