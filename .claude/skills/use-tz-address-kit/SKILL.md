---
name: use-tz-address-kit
description: Use when adding Tanzanian addresses to a Kotlin or Kotlin Multiplatform app with TZ Address Kit (io.github.omarshehe:tz-address-core, tz-address-data, tz-address-ui) - offline type-ahead search, postcode lookup, Region to Kitongoji browsing, ward map positions (latitude/longitude), an app's own extra places, and the Compose AddressSearchField and AddressPicker widgets.
---

# Using TZ Address Kit

Offline Tanzanian address data (no network, no API key; the data ships inside the library). Mainland regions come from TCRA's regional postcode lists
(Government Gazette Notice No. 240 of 22 April 2016) and Zanzibar from the 2012-07-30 list: 31 regions, 168 districts, 4,058 wards (5-digit postcodes),
17,039 mtaa/villages/shehia and 64,262 kitongoji. Songwe and the districts created after 2012 (Kigamboni, Ubungo, Malinyi, Kibiti) are covered; changes after 2016
are not. Read `README.md` for details and `DATA_SOURCE.md` for sources and attribution.

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
The first call installs the bundled database into app storage (about 9 MB); later calls reuse it. A new library version with a new dataset replaces the installed copy on first open.

## The API (package `com.omarshehe.tzaddress`)

```kotlin
addresses.search("kivu", limit = 20, levels = Level.all)   // List<AddressMatch>: path, level, label, postcode, score
addresses.byPostcode("11101")                              // AddressPath? (region, district, ward) or null
addresses.byPrefix("111")                                  // wards whose postcode starts with a 2, 3 or 5 digit prefix
addresses.path(Level.MTAA, mtaaId)                         // reverse lookup of any node
addresses.isValidPostcode("11101")                         // true only for ward postcodes in the dataset (not suspend)
addresses.regions(); addresses.districts(regionCode); addresses.wards(districtCode)
addresses.mtaas(wardPostcode); addresses.kitongojis(mtaaId)
addresses.info()                                           // DatasetInfo(version, sourceEdition, generatedAt, attribution)
```

- Model: `Region(code, name)`, `District(code, name, regionCode)`, `Ward(postcode, name, districtCode, latitude?, longitude?)`, `Mtaa(id, name, wardPostcode)`,
  `Kitongoji(id, name, mtaaId)`. Mtaa and kitongoji have no postcode; their ids are deterministic (`AddressIds`).
- `AddressPath(region, district?, ward?, mtaa?, kitongoji?)` is a node with its ancestors; levels below the node are null.
- `Level` is REGION, DISTRICT, WARD, MTAA, KITONGOJI. `AddressMatch.postcode` is the ward postcode of the nearest ancestor that has one (null above ward).
- `search` ignores case, apostrophes and diacritics (`jangombe` finds `Jang'ombe`; `ilala kariakoo` finds Kariakoo under Ilala). A blank query returns an empty list.
  `limit` is clamped to 1..100. Unknown parents yield empty lists, never errors. `AddressText.normalize` is the same normaliser if you need to match text yourself.
- **Ward positions:** `Ward.latitude` and `Ward.longitude` (both set or both null) are a point inside the ward, for centring a map, not a property location. About 79% of wards
  have one. The positions come from OpenStreetMap ward boundaries (ODbL), so an app that shows or ships them keeps the credit in `info().attribution` (see the README's Licence section).
- **Extra places:** pass `extraPlaces = listOf(ExtraPlace(level, name, parentId, postcode?, latitude?, longitude?))` as the second argument of `createAddressRepository`
  to add wards, mtaa/villages and kitongoji the dataset lacks. They appear in browse, lookup and search with the same ordering and ranking. `parentId` is a district code for a ward,
  a ward postcode for an mtaa and an mtaa id for a kitongoji. No extra regions or districts. A bad list throws `ExtraPlacesInvalidException` listing every problem; a place the
  dataset already has is `SHADOWED` (see `addresses.extraPlaceStatuses()`, which also gives the id). The library stores nothing: keep the list in the app. Meant for tens or hundreds of places.
- **Postcode ids:** a ward's postcode is its id. Between the 2012 and 2016 lists some wards got a new postcode; `dataset/edition-changes.md` in the repository lists old and new.

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
(`./gradlew :core:jvmTest :data:jvmTest :ui:jvmTest`); run them after changing the library.

## Gotchas

- Do not create a new store per screen or per call: create once and share it.
- The JVM target uses sqlite-jdbc and runs on macOS (Intel and Apple silicon), Windows (x64, arm64) and Linux; it adds about 12 MB to a JVM app.
- Postcodes are the ward level only (5 digits). A postcode that is not in the dataset, or a ward created after 2016, is reported as unknown; add it as an extra place if the app needs it.
- A ward without a position has `latitude == null`; do not treat that as an error.
- Do not invent API: read the interface in `core/src/commonMain/kotlin/com/omarshehe/tzaddress/` before using something not shown here.
