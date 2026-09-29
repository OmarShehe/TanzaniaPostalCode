# Project context — TZ Address Kit

Offline Tanzanian address data (Region → District → Ward → Mtaa/Village → Kitongoji) as a Kotlin Multiplatform library. Epic tickets: `.ai/tickets/TZA-*.md`; per-ticket specs: `.ai/specs/`.

## Module map
| Module | Kind | Purpose |
|---|---|---|
| `:core` | KMP library (Android, JVM, iosArm64, iosSimulatorArm64) | Domain model, `AddressRepository`, `AddressIds`. Package root `com.omarshehe.tzaddress`. `commonMain` must not import `android.*` or `java.*`. |
| `:importer` | Kotlin/JVM tool (never published) | Parses `tzPostcodeList.pdf` into `dataset/`. Package `com.omarshehe.tzaddress.importer`. Depends on `:core`; `:core` never depends on it. |
| `:app` | Android application | Sample app; package `com.omarshehe.tanzaniapostalcode`. |

Committed data: `dataset/tz-address.json` (canonical dataset), `dataset/import-report.md`, `dataset/import-anomalies.csv`. The source PDF is **not** in the repo (unknown redistribution terms).

Planned (by ticket): `:data` (TZA-003), `:ui` (TZA-004).

## Build & test
- Use JDK 17+ (JAVA_HOME). Versions live only in `gradle/libs.versions.toml`.
- `./gradlew :core:jvmTest :core:testAndroidHostTest` — core unit tests.
- `./gradlew :core:compileKotlinIosArm64 :core:compileKotlinIosSimulatorArm64` — iOS klib compile (works without Xcode).
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

## Known caveats
- Kotlin/Native reports host `macos_x64` as deprecated (Intel Mac); harmless for now.
