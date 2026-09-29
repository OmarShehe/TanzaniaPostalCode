# TZA-001: Restructure into a Kotlin Multiplatform project with an immutable address domain model

**Type:** Refactor
**Priority:** High
**Platform:** All (`:core`, `:app`)
**Depends on:** None (builds on the already-compiling AGP 9.4.1 / Gradle 9.7.1 / version-catalog baseline)

---

## Problem Statement

The library is Android-only, and its content is a stub: `Address.kt` holds one hard-coded street, `Districts.kt`/`Wards.kt` are partial name lists, and the models (`RegionModel`, `DistrictModel`, `WardModel`, `StreetModel`) are mutable `var` classes over `ArrayList` with `Int` postcodes and inconsistent field names (`postCode` vs `postalCode`, `ward` vs `streets`). The owner wants one library they can plug into any project (Android, iOS, backend), so the module structure and the domain model must be fixed before any data is imported (TZA-002) or queried (TZA-003).

---

## User Stories

- As a **Consumer developer**, I want a single `:core` artifact with a stable, immutable address model and repository interface, so that I can depend on it from Android, iOS or a JVM backend.
- As a **Maintainer**, I want the module layout, model and dataset-version type defined once, so that the importer, the data store and the UI all build against the same contract.

---

## Approved Design / Behaviour

### Module map (after this ticket)

| Module | Kind | Purpose |
|---|---|---|
| `:core` | KMP library (Android, iOS arm64/simulatorArm64/x64, JVM) | Domain model, `AddressRepository` interface, `DatasetInfo`. **Renamed from `:library`.** Pure Kotlin in `commonMain`, no Android/`java.*` imports. |
| `:app` | Android application | Sample app; depends on `:core`. Keeps its current package. |

`:data` (TZA-003), `:ui` (TZA-004) and `:importer` (TZA-002) are created by their own tickets, not here.

### Domain model (`commonMain`, package `com.omarshehe.tzaddress`)

Immutable `data class`es, `List<>` not `ArrayList`, postcodes as `String` (a postcode is an identifier, not a number).

```
Region(code: String, name: String)                        // e.g. "11000"? see Postcode rules
District(code: String, name: String, regionCode: String)  // 2-3 digit code
Ward(postcode: String, name: String, districtCode: String)// 5-digit postcode
Mtaa(id: String, name: String, wardPostcode: String)      // no postcode in source
Kitongoji(id: String, name: String, mtaaId: String)       // no postcode in source
DatasetInfo(version: String, sourceEdition: String, generatedAt: String)
```

Postcode rules (from the source list `tzPostcodeList.pdf`, 31 regions, 5 levels):
- Region header is `<NAME> REGION - NNNNN` (e.g. Dar es Salaam `11000`); the region code stored is the value as printed. District codes are 2-3 digits (`11`, `231`); ward postcodes are 5 digits (`11101`).
- Mtaa/village and kitongoji have **no** postcode in the source. Their `id` is deterministic: `"<parentId>/<slug(name)>"`, with `-2`, `-3` suffixes to disambiguate duplicates under the same parent (the source repeats names, e.g. `Rubumba` twice under one ward).
- `Ward.postcode` is globally unique and is the ward identifier.

Nullable extension point for the deferred geocoding work: `Ward`, `Mtaa` and `Kitongoji` do **not** carry coordinates in this ticket. Geocoding will add a separate `GeoPoint` lookup keyed by these IDs (no model change now, no `lat/lng` fields).

### Repository contract (interface only, `commonMain`)

```kotlin
interface AddressRepository {
    suspend fun info(): DatasetInfo
    suspend fun regions(): List<Region>
    suspend fun districts(regionCode: String): List<District>
    suspend fun wards(districtCode: String): List<Ward>
    suspend fun mtaas(wardPostcode: String): List<Mtaa>
    suspend fun kitongojis(mtaaId: String): List<Kitongoji>
    // search / lookup methods are added by TZA-003
}
```

Only the browse methods and `info()` are declared here; TZA-003 adds search and lookup and provides the implementation. A small in-memory fake (`FakeAddressRepository`, `commonTest`) proves the contract is implementable.

### Removed

`Address.kt`, `Districts.kt`, `Wards.kt`, `Regions.kt`, `PostalCode.kt`, the four `model/*Model.kt` files and `res/values/arrays.xml` are deleted. The current `:app` `MainActivity` is updated to call the new API (or shows a placeholder until TZA-003). Public API breaks: the old `com.omarshehe.library` package was never published to a repository (only `versionName 1.0.1` in the old Gradle file), so no compatibility shim is provided.

### Build modernisation (finishing the work already done)

- KMP setup uses the AGP-9 KMP Android library plugin (verify the current plugin id at implementation time), Kotlin 2.4.x, JVM target 17.
- All versions in `gradle/libs.versions.toml`; add `compileSdk`/`minSdk`/`targetSdk` as catalog `[versions]` entries.
- Enable Gradle configuration cache (`org.gradle.configuration-cache=true`) and fix anything it flags.

---

## Acceptance Criteria

- [ ] Consumer developer: `./gradlew :core:jvmTest :core:testAndroidHostTest` (or the AGP-9 equivalent) passes on a clean checkout with JDK 17.
- [ ] Consumer developer: `:core` compiles for Android, JVM and the three iOS targets (`./gradlew :core:compileKotlinIosArm64` etc.; iOS targets are only configured on macOS hosts).
- [ ] `:core/src/commonMain` contains no `android.*` or `java.*` imports (verified by a grep check in the build or a documented command).
- [ ] Maintainer: a unit test asserts that the model types are immutable (no `var` properties; all collections are `List`) via `FakeAddressRepository` usage compiling against `AddressRepository`.
- [ ] Maintainer: unit tests cover deterministic `Mtaa.id`/`Kitongoji.id` generation, including the duplicate-name suffix rule (`Rubumba`, `Rubumba` → `.../rubumba`, `.../rubumba-2`) and names with quotes/apostrophes (`Mtambani "A"`, `Jang'ombe`).
- [ ] Consumer developer: `:app` builds (`assembleDebug`, `assembleRelease`) and launches without crashing.
- [ ] The old `com.omarshehe.library` sources and `:library` module no longer exist; no references remain (`grep -r "com.omarshehe.library"` returns nothing).
- [ ] `./gradlew build` passes with the configuration cache enabled.

---

## Technical Notes

### Impacted Modules
| Module | Change type | Notes |
|---|---|---|
| `:library` → `:core` | Modified (renamed, converted to KMP) | New model, repository interface, fake, tests |
| `:app` | Modified | Updated dependency, sample call |

### DB / Migration
No DB changes. (Schema is defined in TZA-003.)

### API Contract
No network API. Kotlin API per "Repository contract" above.

### Key Implementation Notes
- Postcodes as `String`: **rejected alternative** — keeping `Int` (as in the old models). Cost: loses leading zeros if a code ever has one, prevents non-numeric or longer codes, and makes prefix queries (region → ward) awkward. The current source has no leading zeros, but an identifier typed as a number is a known bug source.
- Deterministic IDs: **rejected alternative** — auto-increment integers. Cost: IDs change between dataset regenerations, breaking any consumer that stores a selected address.
- Coordinates on the model: **rejected alternative** — adding nullable `lat/lng` now. Cost: dead fields with no data source and a locked-in shape before the geocoding ticket has a design.
- The dataset version type lives here so TZA-002 (writes it) and TZA-003 (reads/exposes it) share one definition.

---

## Scope notes (added during implementation)

- iOS targets are `iosArm64` and `iosSimulatorArm64` only; `iosX64` (Intel simulator) was dropped as no longer worth supporting.
- Immutability criterion is verified by a documented grep (no `var` in `core/.../model`) plus the contract test compiling against `List` types, not by a reflection unit test.
- `./gradlew build` cannot fully pass on a machine without full Xcode: linking iOS test binaries needs it. iOS klib compilation (`compileKotlinIosArm64`, `compileKotlinIosSimulatorArm64`) passes without Xcode; iOS *test execution* is unverified until run on a machine with Xcode.
- `.ai/context.md` (module map, build commands) was created in this ticket.

---

## Out of Scope

- Importing data from the PDF (TZA-002).
- SQLite storage, search and lookup implementation (TZA-003).
- Compose UI (TZA-004).
- Publishing (TZA-005).
- Geocoding/coordinates (deferred TZA-006).
- Backwards-compatible shim for the old `com.omarshehe.library` package.

---

## Open Questions

None. (Decisions: KMP with Android/iOS/JVM targets, String postcodes, deterministic IDs, `:library` renamed to `:core`.)

---

## Files Expected to Change

| File | Change |
|---|---|
| `settings.gradle.kts` | Modified (`:library` → `:core`) |
| `build.gradle.kts` | Modified (KMP plugin alias) |
| `gradle/libs.versions.toml` | Modified (Kotlin, SDK versions, KMP/test libs) |
| `gradle.properties` | Modified (configuration cache) |
| `core/build.gradle.kts` | New (replaces `library/build.gradle.kts`) |
| `core/src/commonMain/kotlin/com/omarshehe/tzaddress/model/*.kt` | New (`Region`, `District`, `Ward`, `Mtaa`, `Kitongoji`, `DatasetInfo`) |
| `core/src/commonMain/kotlin/com/omarshehe/tzaddress/AddressRepository.kt` | New |
| `core/src/commonMain/kotlin/com/omarshehe/tzaddress/AddressIds.kt` | New (slug + disambiguation) |
| `core/src/commonTest/kotlin/.../FakeAddressRepository.kt`, `AddressIdsTest.kt` | New |
| `library/**` | Deleted |
| `app/build.gradle.kts`, `app/src/main/java/.../MainActivity.kt` | Modified |
