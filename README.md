# TZ Address Kit

Offline Tanzanian address data for Kotlin Multiplatform: type-ahead search, postcode lookup and cascading
Region → District → Ward → Mtaa/Village → Kitongoji browsing, with an optional Compose picker.
No network, no API key. The data ships inside the library.

| Search | Picker | Picker (Swahili) |
|---|---|---|
| ![Search](docs/search.png) | ![Picker](docs/picker.png) | ![Picker in Swahili](docs/picker-sw.png) |

## What data it covers

Edition `2012-07-30` of the national postcode list: **30 regions, 163 districts, 3,416 wards (5-digit postcodes),
15,820 mtaa/villages/shehia and 16,883 kitongoji.** Zanzibar's regions are included; a shehia is stored as an mtaa.

Wards also carry a position (`latitude`, `longitude`) for centring a map: **3,042 of the 3,416 wards** have one, the rest are
`null`. It comes from OpenStreetMap ward boundaries, so it is under the ODbL and needs a credit (see [Licence](#licence)).

Not covered: Songwe (created 2016) and districts created after 2012, such as Kigamboni. Sources and attribution are in
[DATA_SOURCE.md](DATA_SOURCE.md).

## Artifacts

| Artifact | For | Needs |
|---|---|---|
| `io.github.omarshehe:tz-address-core` | the model and the `AddressRepository` interface | Kotlin 2.2+ (iOS: 2.4+) |
| `io.github.omarshehe:tz-address-data` | the bundled database and the repository (includes `core`) | Kotlin 2.2+ (iOS: 2.4+) |
| `io.github.omarshehe:tz-address-ui` | `AddressSearchField` and `AddressPicker` (Compose) | Compose Multiplatform 1.12, a recent Kotlin with the Compose plugin; pulls in [`io.github.omarshehe:forminput`](https://github.com/OmarShehe/FormInputs) 2.1.0 |

Targets: Android (minSdk 21 for `core`, **23** for `data` and `ui`), JVM 17, iOS (arm64 and simulator arm64).
Backends and headless apps use `core` + `data` and never pull in Compose. iOS libraries carry the version of the compiler that built them (2.4.20), hence Kotlin 2.4+ there.

## Install

```kotlin
// Android: dependencies { … }.  Multiplatform: commonMain.dependencies { … }
implementation("io.github.omarshehe:tz-address-data:0.1.1")
implementation("io.github.omarshehe:tz-address-ui:0.1.1") // optional, the Compose widgets
```

- **Repositories:** `mavenCentral()` and `google()`. On a JVM backend `google()` is needed too, because `androidx.sqlite` is only on Google's Maven.
- **Multiplatform:** `commonMain` sees the `AddressRepository` interface; each platform creates the store.
- **iOS:** the Kotlin artifact has no data inside it, because iOS has no classpath. Download `tz-address.db` and
  `tz-address.db.version` from the matching [GitHub release](https://github.com/OmarShehe/TanzaniaPostalCode/releases)
  and add both files to your app target (Xcode: *Build Phases → Copy Bundle Resources*). Without them,
  `createAddressRepository()` fails with "Bundled resource … not found".

## Use it

Every call is a `suspend` function (add `org.jetbrains.kotlinx:kotlinx-coroutines-core` if you do not have it). Create the
store **once**, share it, and close it when done: it holds an open database connection.

```kotlin
val addresses: AddressStore = createAddressRepository(context)                  // Android
// val addresses: AddressStore = createAddressRepository("/var/lib/myapp/tz")   // JVM: folder for the working copy of the database
// val addresses: AddressStore = createAddressRepository()                      // iOS

val hits = addresses.search("kivu")            // ranked suggestions: label, full path, postcode
val ward = addresses.byPostcode("11101")       // AddressPath? (region, district, ward) or null
val wards = addresses.byPrefix("111")          // every ward whose postcode starts with 111
val valid = addresses.isValidPostcode("11101") // true only for ward postcodes in the dataset

val regions = addresses.regions()              // browse, ordered by name
val districts = addresses.districts(regions.first().code)

addresses.close()
```

`search` matches names and ancestor names, ignoring case and apostrophes (`jangombe` finds `Jang'ombe`,
`ilala kariakoo` finds Kariakoo under Ilala). A blank query returns an empty list; `limit` is clamped to 1..100.
The first call installs the bundled database into app storage (about 9 MB); later calls reuse it.

### Ward positions

```kotlin
val ward = addresses.byPostcode("11101")?.ward
val lat = ward?.latitude   // Double?, null when the ward has no known position
val lon = ward?.longitude  // both are set, or both are null
```

The position is a point **inside the ward**, to centre a map when someone picks a ward. It is not the location of a house or a
street, so let people place an exact point themselves. `ward-points-report.md` and `ward-points-anomalies.csv` in `dataset/` list the
wards without a position and why (mostly spelling differences between the 2012 postcode list and the boundary data).

## The Compose picker

```kotlin
AddressSearchField(
    repository = addresses,
    onSelected = { path -> println(path.ward?.postcode) },
)

var value by remember { mutableStateOf<AddressPath?>(null) }
AddressPicker(
    repository = addresses,
    value = value,
    onValueChange = { value = it },
    requiredLevel = Level.WARD, // null is reported until this level is chosen
)
```

Both use your `MaterialTheme`, ship English and Swahili strings, and survive rotation. A level with nothing
listed in the source (some wards have no mtaa, most mtaa have no kitongoji) shows "None listed" and counts as complete.
They are built on `forminput`, so a `FormInputTheme` around them changes their style and shape. The `:app` module is a working sample.

**JVM hosts:** the JVM target uses [sqlite-jdbc](https://github.com/xerial/sqlite-jdbc), so it runs on macOS (Intel and
Apple silicon), Windows (x64, arm64) and Linux (including Alpine). It adds about 12 MB to a JVM app; Android and iOS use the AndroidX bundled SQLite.

## Updating the dataset

The dataset is regenerated from the published postcode list (see [DATA_SOURCE.md](DATA_SOURCE.md)), never edited by hand:

```
./gradlew :importer:importPostcodes -Ppdf=/path/to/postcode-list -PsourceEdition="<label>"
```

This rewrites `dataset/tz-address.json`, `dataset/import-report.md` and `dataset/import-anomalies.csv`, and fails on bad
postcodes, duplicates or too many parse anomalies. The bundled database is built from that file at build time
(`./gradlew :data:generateAddressDb`, which also checks its table counts against the report). Then bump `VERSION_NAME`
in `gradle.properties` and add a `CHANGELOG.md` entry.

Ward positions are added to the committed `dataset/tz-address.json` by a second step (the boundary files are public and, like the
postcode list, are not stored here; the pinned download links are in [DATA_SOURCE.md](DATA_SOURCE.md)):

```
./gradlew :importer:importWardPoints -PwardBoundaries=/path/geoBoundaries-TZA-ADM3_simplified.geojson \
    -PdistrictBoundaries=/path/geoBoundaries-TZA-ADM2_simplified.geojson
```

It fails when fewer than 85% of wards get a position (`-PminMatchRatio` to change) or a point is outside Tanzania, and writes
`dataset/ward-points-report.md` and `dataset/ward-points-anomalies.csv`. Re-importing the postcode list keeps the positions already in
the dataset.

## Versioning

- **MAJOR:** breaking API change, or ids (`Mtaa.id`, `Kitongoji.id`) changing for existing places.
- **MINOR:** new API, or a **new dataset edition**.
- **PATCH:** data corrections that do not change ids.

Each release records the artifact version and the dataset in [CHANGELOG.md](CHANGELOG.md); `addresses.info()` returns the same at runtime.

## Building and releasing

- `./gradlew :core:jvmTest :data:jvmTest :ui:jvmTest` runs the unit and desktop UI tests; `./gradlew :app:connectedDebugAndroidTest`
  runs both widgets over the real database on a device.
- `./gradlew publishToMavenLocal` publishes `core`, `data` and `ui` to `~/.m2` for trying them in another project (add `mavenLocal()` first there).
- **Releasing:** set `VERSION_NAME`, update `CHANGELOG.md`, commit and push. Then run `./gradlew publishAndReleaseToMavenCentral`
  (needs the Central token and signing key in `~/.gradle/gradle.properties`): it signs, uploads and releases once Central validates.
  `./gradlew publishToMavenCentral` only stages the deployment for a manual *Publish* in the Central Portal. A release cannot be undone.
  Afterwards push the tag (`git tag v<version> && git push origin v<version>`): `.github/workflows/release.yml` checks the repository and
  attaches the database files to the GitHub release. `:app` and `:importer` are never published.

## Licence

Code: [Apache License 2.0](LICENSE). The address data comes from a public postcode list (see [DATA_SOURCE.md](DATA_SOURCE.md)).

**Ward positions** (`Ward.latitude`, `Ward.longitude`) are derived from OpenStreetMap data and are licensed under the
[Open Database License (ODbL) 1.0](https://opendatacommons.org/licenses/odbl/1-0/). If your app uses them, credit
**© OpenStreetMap contributors** where your app shows credits (an About or credits screen is enough; there is no per-record notice).
The full text is in the dataset itself: `addresses.info().attribution`.

## For AI coding assistants

`.claude/skills/use-tz-address-kit/SKILL.md` teaches an assistant such as Claude Code how to use this library. Copy the folder into another project's `.claude/skills/` to use it there.
