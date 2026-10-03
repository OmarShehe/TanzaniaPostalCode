---
name: use-tz-address-kit
description: Use when adding Tanzanian addresses to a Kotlin or Kotlin Multiplatform app with TZ Address Kit (io.github.omarshehe:tz-address-core, tz-address-data, tz-address-ui) - offline type-ahead search, postcode lookup, Region to Kitongoji browsing, and the Compose AddressSearchField and AddressPicker widgets.
---

# Using TZ Address Kit

Offline Tanzanian address data (no network, no API key; the data ships inside the library). Edition `2012-07-30` of the national postcode list:
30 regions, 163 districts, 3,416 wards (5-digit postcodes), 15,820 mtaa/villages/shehia and 16,883 kitongoji. Songwe (created 2016) and districts created after
2012 (for example Kigamboni) are **not** covered. Read `README.md` for details and `DATA_SOURCE.md` for attribution.

## Pick the artifacts

| Artifact | Use it for |
|---|---|
| `io.github.omarshehe:tz-address-core` | the model and the `AddressRepository` interface only |
| `io.github.omarshehe:tz-address-data` | the bundled database and the repository (includes core); backends and headless apps stop here |
| `io.github.omarshehe:tz-address-ui` | `AddressSearchField` and `AddressPicker` (Compose Multiplatform 1.12, built on `io.github.omarshehe:forminput` 2.1.0, pulled in transitively) |

Targets: Android (minSdk 21 for core, **23** for data and ui), JVM 17, iOS (arm64, simulator arm64). Kotlin 2.2+ for core and data; iOS consumers need Kotlin 2.4+.
On a JVM backend add `google()` next to `mavenCentral()` (`androidx.sqlite` is only on Google's Maven).
`tz-address-ui` pulls in `io.github.omarshehe:forminput` from Maven Central, so no extra repository is needed beyond `google()` and `mavenCentral()`.

## Create the store once

Every call is a `suspend` function. Create the `AddressStore` once, share it, and `close()` it when done (it holds an open database connection).

```kotlin
val addresses: AddressStore = createAddressRepository(context)                 // Android
// val addresses: AddressStore = createAddressRepository("/var/lib/app/tz")    // JVM: folder for the working copy of the database
// val addresses: AddressStore = createAddressRepository()                     // iOS
```

**iOS needs two files added to the app target by hand** (Xcode: Build Phases, Copy Bundle Resources): `tz-address.db` and `tz-address.db.version`
from the matching GitHub release. Without them `createAddressRepository()` fails with "Bundled resource ... not found".
The first call installs the bundled database into app storage (about 9 MB); later calls reuse it.

## The API (package `com.omarshehe.tzaddress`)

```kotlin
addresses.search("kivu", limit = 20, levels = Level.all)   // List<AddressMatch>: path, level, label, postcode, score
addresses.byPostcode("11101")                              // AddressPath? (region, district, ward) or null
addresses.byPrefix("111")                                  // wards whose postcode starts with a 2, 3 or 5 digit prefix
addresses.path(Level.MTAA, mtaaId)                         // reverse lookup of any node
addresses.isValidPostcode("11101")                         // true only for ward postcodes in the dataset (not suspend)
addresses.regions(); addresses.districts(regionCode); addresses.wards(districtCode)
addresses.mtaas(wardPostcode); addresses.kitongojis(mtaaId)
addresses.info()                                           // DatasetInfo(version, sourceEdition, generatedAt)
```

- Model: `Region(code, name)`, `District(code, name, regionCode)`, `Ward(postcode, name, districtCode)`, `Mtaa(id, name, wardPostcode)`,
  `Kitongoji(id, name, mtaaId)`. Mtaa and kitongoji have no postcode; their ids are deterministic (`AddressIds`).
- `AddressPath(region, district?, ward?, mtaa?, kitongoji?)` is a node with its ancestors; levels below the node are null.
- `Level` is REGION, DISTRICT, WARD, MTAA, KITONGOJI. `AddressMatch.postcode` is the ward postcode of the nearest ancestor that has one (null above ward).
- `search` ignores case, apostrophes and diacritics (`jangombe` finds `Jang'ombe`; `ilala kariakoo` finds Kariakoo under Ilala). A blank query returns an empty list.
  `limit` is clamped to 1..100. Unknown parents yield empty lists, never errors. `AddressText.normalize` is the same normaliser if you need to match text yourself.

## Compose widgets (`com.omarshehe.tzaddress.ui`)

```kotlin
AddressSearchField(repository = addresses, onSelected = { path -> println(path.ward?.postcode) })   // optional label, placeholder

var value by remember { mutableStateOf<AddressPath?>(null) }
AddressPicker(repository = addresses, value = value, onValueChange = { value = it }, requiredLevel = Level.WARD)
```

- `AddressPicker` reports `null` until `requiredLevel` is chosen. A level with nothing listed in the source (some wards have no mtaa, most mtaa have no kitongoji)
  shows "None listed" and counts as complete.
- Both use your `MaterialTheme`, ship English and Swahili strings, and survive rotation. They are built on `forminput`, so a `FormInputTheme` around
  them can change their style and shape (see the `use-forminput` skill in the FormInputs repo).
- The `:app` module is a working sample.

## Testing your own code

`AddressRepository` is an interface, so unit tests can use a small fake instead of the database. The library's own tests live in `core`, `data` and `ui`
(`./gradlew :core:allTests`, `:data:allTests`, `:ui:testAndroidHostTest`); run them after changing the library.

## Gotchas

- Do not create a new store per screen or per call: create once and share it.
- Intel Macs: the bundled SQLite driver has no macOS x64 binary. The JVM target runs on Linux (x64, arm64), Windows x64 and Apple-silicon macOS.
- Postcodes are the ward level only (5 digits). A postcode outside the 2012 list, or a newer district, is reported as unknown; that is a data limit, not a bug.
- Do not invent API: read the interface in `core/src/commonMain/kotlin/com/omarshehe/tzaddress/` before using something not shown here.
