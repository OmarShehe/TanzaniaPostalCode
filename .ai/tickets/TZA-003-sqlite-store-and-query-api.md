# TZA-003: Read-only SQLite address store with search, lookup and browse API

**Type:** Feature
**Priority:** High
**Platform:** `:data` (new KMP module)
**Depends on:** TZA-001 (model + `AddressRepository`), TZA-002 (`dataset/tz-address.json`)

---

## Problem Statement

With the model (TZA-001) and dataset (TZA-002) in place, nothing lets a project actually *use* the addresses. Consumers need Google-Places-style behaviour offline: type-ahead search, postcode ↔ address resolution, and cascading region → kitongoji browsing. The dataset is large (five levels across 31 regions), so holding it all as in-memory Kotlin objects (the old design) costs heap and startup time on mobile.

---

## User Stories

- As a **Consumer developer**, I want `repository.search("kivu")` to return ranked suggestions with their full hierarchy and postcode, so that I can build address autocomplete.
- As a **Consumer developer**, I want to resolve a postcode to its full address chain and back, so that I can validate and normalise user input.
- As a **Consumer developer**, I want cascading browse calls, so that I can build region → district → ward → mtaa → kitongoji pickers.
- As a **Maintainer**, I want the database produced by a build task from the committed dataset, so that a dataset update is regenerate-and-release, not hand-editing.

---

## Approved Design / Behaviour

### Storage

A **prebuilt read-only SQLite database** generated at build time from `dataset/tz-address.json` and bundled as a resource of `:data`. On first use it is copied to app storage (Android/iOS/JVM each via a small `expect/actual` file-location abstraction) and opened read-only. Access uses `androidx.sqlite` (KMP) with the **bundled SQLite driver**, so the SQLite version, and therefore FTS behaviour, is identical on all targets. The dataset version (`DatasetInfo`) is stored in the DB; if the bundled version differs from the copied one, the file is replaced.

Schema (one table per level plus a search index):

```
region(code PK, name)
district(code PK, name, region_code FK)
ward(postcode PK, name, district_code FK)
mtaa(id PK, name, ward_postcode FK)
kitongoji(id PK, name, mtaa_id FK)
search_index  -- FTS virtual table: (level, ref_id, name, path_text)
dataset_info(version, source_edition, generated_at)
```

Names are also stored normalised (lower-case, diacritics/apostrophes/quotes stripped) for matching `Jang'ombe` with `jangombe`.

### API (extends `AddressRepository` from TZA-001)

```kotlin
suspend fun search(query: String, limit: Int = 20, levels: Set<Level> = Level.all): List<AddressMatch>
suspend fun byPostcode(postcode: String): AddressPath?   // ward postcode → full chain
suspend fun byPrefix(prefix: String): List<AddressPath>  // "111" → all wards under it (region/district/ward code prefixes)
suspend fun path(level: Level, id: String): AddressPath? // reverse: any node → chain
fun isValidPostcode(postcode: String): Boolean           // exists in dataset (suspend-free, uses cached set)
data class AddressPath(region, district?, ward?, mtaa?, kitongoji?)
data class AddressMatch(path: AddressPath, level: Level, label: String, postcode: String?, score: Double)
```

Behaviour:
- `search`: prefix-and-token match over name and the ancestor names (typing `ilala kariakoo` finds Kariakoo under Ilala). Ranking: exact name > name prefix > token match; higher levels rank above lower levels on ties; deterministic order. Blank query returns an empty list; `limit` is clamped to 1..100.
- Every match carries the postcode of its nearest ancestor that has one (ward postcode for mtaa/kitongoji) in `postcode`.
- `byPostcode` accepts a 5-digit ward postcode; also `byPrefix` for 2-, 3- or 5-digit inputs (region/district/ward code). Non-digit or wrong-length input returns null/empty, never throws.
- All calls are safe to invoke off the main thread and use no global mutable state beyond the opened DB.

### Build task

`./gradlew :data:generateAddressDb` reads `dataset/tz-address.json` and writes `data/src/commonMain/resources/tz-address.db`. The DB is a generated build output (not committed); `:data:assemble` depends on the task.

---

## Acceptance Criteria

- [ ] Maintainer: **FTS spike passes first** — a test proves FTS works on the JVM target and on an Android (instrumented or Robolectric-free host-with-bundled-driver) target, and iOS simulator runs the same query test; recorded in the ticket PR. If any target fails, the ticket stops and returns to discussion (fallback: `LIKE` on the normalised column with an indexed prefix column).
- [ ] Consumer developer: `search("kivu")` returns Kivukoni (ward and/or mtaa) with region "Dar es Salaam", district "Ilala CBD" and postcode `11101`.
- [ ] Consumer developer: `search("jangombe")` matches `Jang'ombe`; `search("KARIAKOO")` matches case-insensitively; `search("ilala kariakoo")` returns Kariakoo ward ahead of unrelated matches.
- [ ] Consumer developer: `byPostcode("11101")` returns the full chain Region/District/Ward; `byPostcode("00000")` and `byPostcode("abc")` return null; `byPrefix("111")` returns only wards whose postcodes start with `111`.
- [ ] Consumer developer: for every ward in the dataset, `byPostcode(ward.postcode)` round-trips to that ward (test iterates all wards).
- [ ] Consumer developer: browse calls return children ordered by name; unknown parent codes return an empty list.
- [ ] Consumer developer: `search("")` and `search("   ")` return an empty list; `limit = 0` and `limit = 1000` are clamped, not errors.
- [ ] Maintainer: `search` p95 latency on a mid-range Android emulator profile is under 50 ms for a 3-letter prefix (measured by a benchmark test; result recorded, not flaky-asserted in CI).
- [ ] Maintainer: on first run the DB is copied once; a second launch does not re-copy; a bundled `dataset_info.version` change replaces the copied file.
- [ ] Maintainer: `generateAddressDb` counts per table equal the counts in `dataset/import-report.md`; the task fails if they differ.
- [ ] Consumer developer: `:data` compiles for Android, JVM and iOS targets and its API contains no Android-only types.

---

## Technical Notes

### Impacted Modules
| Module | Change type | Notes |
|---|---|---|
| `:data` | New | KMP module: schema, generator task, `SqliteAddressRepository`, `expect/actual` file access |
| `:core` | Modified | Extend `AddressRepository`; add `AddressPath`, `AddressMatch`, `Level` types |
| `:app` | Modified | Sample search box wired to the repository |

### DB / Migration
New bundled SQLite database (see schema). No runtime migrations: the DB is read-only and replaced wholesale when the dataset version changes.

### API Contract
Kotlin API above; no network.

### Key Implementation Notes
- **Decision (SQLite vs Room):** plain KMP `androidx.sqlite` with the bundled driver. **Rejected alternative: Room KMP.** Cost of Room here: it validates the schema identity hash of a pre-packaged DB, so each dataset regeneration is friction; pre-packaged DB support is Android-first; its migrations and write DAOs are unused for a read-only dataset; and it adds KSP. Room becomes worthwhile only if user-writable data (saved/favourite addresses) is added later. **Rejected alternative: SQLDelight.** Valid and equivalent, but adds a plugin for a small query surface.
- **Rejected alternative:** generated Kotlin/JSON loaded fully in memory (the old repo's approach). Cost: tens of thousands of objects on the heap, slow class loading/startup, no efficient prefix search.
- `androidx.sqlite`/bundled-driver artifact versions must be confirmed at implementation time.
- `AddressIds` (TZA-001) is the single source of mtaa/kitongoji IDs; the generator must call it.

---

## Out of Scope

- Compose UI (TZA-004).
- Publishing/versioning of the artifact (TZA-005).
- Fuzzy/typo-tolerant matching (edit-distance) — prefix/token match only.
- Swahili/English alias search or spelling variants beyond case/diacritic/apostrophe normalisation.
- Writable data, favourites, history.
- Coordinates/geocoding (deferred TZA-006).

---

## Open Questions

None. (Decisions: prebuilt read-only SQLite via `androidx.sqlite` + bundled driver; FTS spike is an explicit acceptance gate with a named fallback.)

---

## Files Expected to Change

| File | Change |
|---|---|
| `settings.gradle.kts` | Modified (`include(":data")`) |
| `gradle/libs.versions.toml` | Modified (`androidx.sqlite`, bundled driver, coroutines) |
| `data/build.gradle.kts` | New (KMP targets, `generateAddressDb`) |
| `data/src/commonMain/kotlin/.../SqliteAddressRepository.kt`, `Schema.kt`, `SearchQuery.kt`, `Normalizer.kt` | New |
| `data/src/{androidMain,iosMain,jvmMain}/kotlin/.../DbFiles.*.kt` | New (`actual`s) |
| `data/src/commonTest/kotlin/**`, `data/src/androidInstrumentedTest/**` | New |
| `core/src/commonMain/kotlin/.../AddressRepository.kt` | Modified (search/lookup methods) |
| `core/src/commonMain/kotlin/.../{AddressPath,AddressMatch,Level}.kt` | New |
| `app/src/main/java/.../MainActivity.kt` | Modified |
