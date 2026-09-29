# Project context — TZ Address Kit

Offline Tanzanian address data (Region → District → Ward → Mtaa/Village → Kitongoji) as a Kotlin Multiplatform library. Epic tickets: `.ai/tickets/TZA-*.md`; per-ticket specs: `.ai/specs/`.

## Module map
| Module | Kind | Purpose |
|---|---|---|
| `:core` | KMP library (Android, JVM, iosArm64, iosSimulatorArm64) | Domain model, `AddressRepository`, `AddressIds`. Package root `com.omarshehe.tzaddress`. `commonMain` must not import `android.*` or `java.*`. |
| `:importer` | Kotlin/JVM tool (never published) | Parses `tzPostcodeList.pdf` into `dataset/`. Package `com.omarshehe.tzaddress.importer`. Depends on `:core`; `:core` never depends on it. |
| `:data` | KMP library (Android, JVM, iosArm64, iosSimulatorArm64) | `SqliteAddressRepository` over a bundled read-only SQLite DB (`androidx.sqlite` bundled driver, FTS5 search). Package `com.omarshehe.tzaddress.data`. Public API: the platform `createAddressRepository(...)` factories. |
| `:data` | KMP library (Android, JVM, iosArm64, iosSimulatorArm64) | `SqliteAddressRepository` over a bundled read-only SQLite DB (`androidx.sqlite` bundled driver, FTS5 search). Package `com.omarshehe.tzaddress.data`. Public API: the platform `createAddressRepository(...)` factories. |
| `:ui` | Compose Multiplatform library (Android, desktop JVM, iosArm64, iosSimulatorArm64) | Optional `AddressSearchField` and `AddressPicker`. Package `com.omarshehe.tzaddress.ui`. Depends on `:core` only (never `:data`); behaviour lives in `AddressSearchController` / `AddressPickerController`, strings in `composeResources` (en, sw). |
| `:app` | Android application | Sample app; package `com.omarshehe.tanzaniapostalcode`. |

Committed data: `dataset/tz-address.json` (canonical dataset), `dataset/import-report.md`, `dataset/import-anomalies.csv`. The source PDF is **not** in the repo (unknown redistribution terms).

The database `tz-address.db` (+ `tz-address.db.version` stamp) is **generated, not committed**: `:data:generateAddressDb` builds it from `dataset/tz-address.json` (via `:importer`, using `sqlite-jdbc`) and fails if table counts differ from `dataset/import-report.md`. JVM/Android bundle it as classpath resources; iOS reads it from the app bundle (the host app must add both files to its target).

## Build & test
- Use JDK 17+ (JAVA_HOME). Versions live only in `gradle/libs.versions.toml`.
- `./gradlew :core:jvmTest :core:testAndroidHostTest` — core unit tests.
- `./gradlew :core:compileKotlinIosArm64 :core:compileKotlinIosSimulatorArm64` — iOS klib compile (works without Xcode).
- `./gradlew :data:jvmTest` (also builds the DB) and `./gradlew :data:connectedAndroidDeviceTest` (real device: FTS with the bundled driver, install + search, prints a p95 benchmark to logcat tag `TzAddressBench`).
- **Intel Macs:** the bundled SQLite driver has no macOS x64 binary, so `BundledDriverFtsSpikeTest` skips there and `:data` JVM tests use a test-only JDBC driver (`JdbcSQLiteDriver`). Linux x64/arm64, Windows x64 and macOS arm64 are supported by the driver.
- `./gradlew :data:jvmTest` (also builds the DB) and `./gradlew :data:connectedAndroidDeviceTest` (real device: FTS with the bundled driver, install + search, p95 benchmark to logcat tag `TzAddressBench`).
- **Intel Macs:** the bundled SQLite driver has no macOS x64 binary, so `BundledDriverFtsSpikeTest` skips there and `:data` JVM tests use a test-only JDBC driver (`JdbcSQLiteDriver`). Linux x64/arm64, Windows x64 and macOS arm64 are supported by the driver.
- `./gradlew :ui:jvmTest` (controller tests plus Compose UI tests on desktop JVM) and `./gradlew :app:connectedDebugAndroidTest` (both widgets over the real bundled DB on a device, including rotation).
- `:ui` needs `androidResources { enable = true }` so its Compose strings reach the AAR (AGP-KMP library plugin default is off).
- Apply the Compose plugins (`org.jetbrains.compose`, `org.jetbrains.kotlin.plugin.compose`) only in `:ui` and `:app`; on `:data`/`:core` they break the plain-JVM compile.
- `./gradlew :app:assembleDebug :app:assembleRelease`
- `./gradlew build` — fails on this machine at `:core:linkDebugTestIosSimulatorArm64` (linking iOS test binaries needs full Xcode; only Command Line Tools are installed). Locally use `-x linkDebugTestIosSimulatorArm64 -x iosSimulatorArm64Test -x linkDebugTestIosArm64`.
- Checks: `grep -rnE "^import (android|java)\." core/src/commonMain` and `grep -rn "\bvar\b" core/src/commonMain/kotlin/com/omarshehe/tzaddress/model` must return nothing.

## Importing the dataset
- `./gradlew :importer:importPostcodes -Ppdf=/path/to/tzPostcodeList.pdf [-PsourceEdition=..] [-PgeneratedAt=2026-09-29T00:00:00Z] [-Pforce] [-PoutDir=dataset]` (about 10 s). Fixing `generatedAt` makes the JSON byte-identical between runs.
- Fails the build on: region count != `-PexpectedRegions` (default 30), bad/duplicate ward postcodes, ward postcode not starting with its district code, bad (not 2-3 digit) or duplicate district codes, suspect-anomaly ratio above `-PmaxAnomalyRatio` (default 0.005). Defaults live in `ImportOptions`; override per run with `-P`. On failure the report and CSV are still written but `tz-address.json` is not (the report says so); `-Pforce` (or `-Pforce=true`) writes it anyway; `-Pforce=false` does not.
- Opt-in real-PDF tests (independent per-region ward counts via `pdftotext`, determinism): `./gradlew :importer:test -Ppdf=/path/to/tzPostcodeList.pdf`. Without `-Ppdf` they are skipped.
- The source is a 2012-07-30 snapshot: 30 regions, no Songwe (2016) or Kigamboni. Zanzibar shehia are stored as `Mtaa` (no kitongoji level). PDF column positions differ on every page; the parser reads each page's own header row.

## Workflow
Local-only for now: no pushes. Integration branch `feature/tz-address-kit`; per-ticket branches `feature/TZA-NNN` merge into it.

## Publishing
- Artifacts (group `io.github.omarshehe`, version `VERSION_NAME` in `gradle.properties`): `tz-address-core` (`:core`), `tz-address-data` (`:data`), `tz-address-ui` (`:ui`). `:app` and `:importer` are never published. Plugin: `com.vanniktech.maven.publish`.
- `./gradlew publishToMavenLocal` for local integration tests (consumer projects list `mavenLocal()` first and, for JVM, `google()` too: `androidx.sqlite` is only on Google Maven).
- Release: push tag `v<VERSION_NAME>`; `.github/workflows/release.yml` (macOS runner) runs `.github/scripts/check-release.sh` (needs `LICENSE`, no `TODO(maintainer)` in `DATA_SOURCE.md`, tag = `VERSION_NAME`), requires secrets `MAVEN_CENTRAL_USERNAME`, `MAVEN_CENTRAL_PASSWORD`, `SIGNING_IN_MEMORY_KEY`, `SIGNING_IN_MEMORY_KEY_PASSWORD`, publishes, and attaches `tz-address.db` + `.version` to the GitHub release (iOS consumers need them in their app bundle).
- `:core` and `:data` compile with language/API level 2.2 and stdlib 2.2.0 so Kotlin 2.2+ consumers can read them; `:ui` needs a newer Kotlin (Compose 1.12, built with 2.4.20).

## Known caveats
- Kotlin/Native reports host `macos_x64` as deprecated (Intel Mac); harmless for now.
