# TZA-005: Publish the artifacts and document how to plug them into any project

**Type:** Chore
**Priority:** Medium
**Platform:** All (`:core`, `:data`, `:ui`)
**Depends on:** TZA-003 and TZA-004 (both shipped: `:core`, `:data` and `:ui` are all published)

---

## Problem Statement

The stated goal is a repo whose artifacts can be plugged into any of the owner's projects. The code now exists (`:core`, `:data`, `:ui`, sample `:app`) but nothing is publishable or documented: the README is one line, there is no group/artifact coordinate, no versioning policy, and no record of where the dataset came from. Without that, the "plug in" promise depends on copying source between projects.

---

## User Stories

- As a **Consumer developer**, I want to add one Gradle dependency line and follow a README recipe for Android, iOS or a JVM backend, so that I'm using the address data in minutes.
- As a **Maintainer**, I want a documented release procedure with versioning rules, so that a data or API change is published safely and reproducibly.
- As a **Consumer developer**, I want to know the dataset edition behind each library version, so that I can reason about address changes.

---

## Approved Design / Behaviour

### Coordinates and versioning

- Group `io.github.omarshehe` (Maven Central namespace via GitHub ownership); artifacts `tz-address-core`, `tz-address-data`, `tz-address-ui`. KMP publishes the per-target artifacts and Gradle metadata automatically.
- Semantic versioning: MAJOR = breaking API/ID change; MINOR = new API or **new dataset edition**; PATCH = data corrections that don't change IDs. The dataset edition and `DatasetInfo.version` are recorded in `CHANGELOG.md` per release and exposed by `AddressRepository.info()`.
- Version lives in one place (`gradle.properties` `VERSION_NAME`).

### Publishing

The `com.vanniktech.maven.publish` Gradle plugin (0.37.0 at refine time) handles Kotlin Multiplatform publications, POM metadata, signing and the Central Portal upload, so no hand-written `maven-publish` wiring is needed. It runs from a GitHub Actions workflow triggered by a `v*` tag, on a **macOS runner** (the iOS targets can only be built on macOS). Credentials come from repository secrets only. `:app` and `:importer` are never published. Only `iosArm64` and `iosSimulatorArm64` exist (no `iosX64`); the README says so. A `publishToMavenLocal` path is documented for local integration testing.

### How consumers get the database

`tz-address.db` is generated, not committed. It ships **inside** the Android and JVM artifacts of `tz-address-data` (classpath resources) but **not** inside the iOS klibs, because iOS has no classpath: the host app must add `tz-address.db` and `tz-address.db.version` to its target's bundle resources. The release workflow therefore attaches both files to the GitHub release, and the README explains the Xcode step.

### Platform notes the README must state

- The bundled SQLite driver has no binary for **Intel macOS**: the JVM target works on Linux x64/arm64, Windows x64 and Apple-silicon macOS (development on Intel Macs needs Docker or another host).
- Android: `:ui` ships Compose resources (`androidResources` is enabled in the library); consumers of `:ui` use Compose Multiplatform 1.12.
- `createAddressRepository(...)` returns a closable `AddressStore`: create once, share, close when done.

### README (replaces the one-line file)

Sections: what it is; what data it covers (five levels, dataset edition); install snippets (Android/KMP `commonMain`, JVM backend); 10-line usage for search, postcode lookup, browse; the `:ui` picker (with screenshot from TZA-004); dataset update procedure (run `:importer:importPostcodes`, regenerate DB, bump version); versioning rules; licence; data attribution.

### Data source record

`DATA_SOURCE.md` records the PDF's title, publisher, edition/date, how it was obtained, and the terms under which the derived dataset may be redistributed. The PDF itself carries only generic metadata (`Microsoft Word - REGION.DAR`), so the Maintainer must fill this in.

---

## Acceptance Criteria

- [ ] Maintainer: `./gradlew publishToMavenLocal` publishes `tz-address-core` and `tz-address-data` (and `tz-address-ui` if present) with POM metadata (name, description, licence, SCM, developer).
- [ ] Consumer developer: a throwaway project outside this repo, using only the README's Android instructions and `mavenLocal()`, compiles and runs a `search("kivu")` call.
- [ ] Consumer developer: a throwaway JVM project using the README's JVM instructions runs `byPostcode("11101")` successfully on a supported host (Linux via Docker on this Intel Mac).
- [ ] Maintainer: `:app` and `:importer` have no publication configured (`./gradlew publishToMavenLocal` produces no `app`/`importer` artifacts).
- [ ] Maintainer: pushing a `v*` tag runs the release workflow (macOS runner), which fails without signing secrets, never prints them in logs, and attaches `tz-address.db` and `tz-address.db.version` to the GitHub release. (Not verifiable locally: it needs a push and repository secrets.)
- [ ] Maintainer: `CHANGELOG.md` has an entry for the first release listing the artifact version and `DatasetInfo` version/source edition.
- [ ] Maintainer: `DATA_SOURCE.md` exists and states the source's origin and redistribution terms as confirmed by the Maintainer; the first public release does **not** proceed until this is filled in (the workflow checks the file for the placeholder marker `TODO(maintainer)` and fails if present).
- [ ] Consumer developer: `README.md` contains working install snippets for Android, KMP and JVM, and every code snippet compiles (verified by copying them into a test source set or the sample app).
- [ ] Maintainer: a licence file is added (licence chosen by the Maintainer before tagging; `LICENSE` presence is checked by the workflow).

---

## Technical Notes

### Impacted Modules
| Module | Change type | Notes |
|---|---|---|
| `:core`, `:data`, `:ui` | Modified | Publishing config, POM metadata |
| `:app`, `:importer` | None | Explicitly not published |

### DB / Migration
No DB changes.

### API Contract
No API changes.

### Key Implementation Notes
- **Rejected alternative:** JitPack. Cost: builds from source on the consumer's first request, which is fragile for KMP/iOS targets (needs a macOS builder) and gives no signed artifacts. Kept only as an optional fallback for Android-only consumers.
- **Rejected alternative:** copy modules into each project. Cost: divergent copies and no dataset provenance.
- Central namespace verification requires control of the GitHub account/repo `OmarShehe`; register the namespace before the first release.
- Vanniktech `com.vanniktech.maven.publish` 0.37.0 is the chosen plugin; artifact ids are set explicitly (`tz-address-core`, `-data`, `-ui`), since the Gradle project names are `:core`, `:data`, `:ui`.
- The `origin` remote is `https://github.com/OmarShehe/TanzaniaPostalCode`; the git author on recent commits is Omar Mtara. The Maven group `io.github.omarshehe` must match the account that owns the repo on GitHub, so the Maintainer confirms it before registering the namespace.
- `dataset/tz-address.json` (derived from the PDF) is already committed; publishing releases it publicly, which is exactly why `DATA_SOURCE.md` gates the first release.

---

## Scope notes (added during implementation)

- **Plugin:** `com.vanniktech.maven.publish` 0.37.0; group and version come from `gradle.properties` (`GROUP`, `VERSION_NAME`, shared POM fields), artifact ids are set per module. Signing is enabled only when `signingInMemoryKey` is present, so `publishToMavenLocal` works without keys.
- **Verified locally (via `publishToMavenLocal`):** only `tz-address-core|data|ui` and their per-target artifacts are published (no `app`/`importer`); the database is inside the JVM jar and the Android AAR, `:ui`'s Compose resources are in its AAR; POMs carry name, description, licence, SCM and developer.
- **Consumer checks (throwaway projects outside the repo, README snippets copied verbatim):** an Android project on AGP's default Kotlin 2.2 compiles against `tz-address-data`; a plain JVM project on Kotlin 2.2.0 compiles and resolves everything, but **running** it fails on this Intel Mac at the bundled SQLite driver ("Cannot find a suitable SQLite binary"), so the JVM run (`byPostcode("11101")`) is **unverified** here and needs Linux, Windows or Apple-silicon macOS (Docker Desktop is installed but its daemon is not running). A KMP consumer checks the `commonMain` install line.
- **Found by those checks and fixed in `:core`/`:data`:** they compile against Kotlin language/API 2.2 with stdlib 2.2.0, so Kotlin 2.2+ consumers can read them on Android/JVM. iOS/native klibs are stamped with the building compiler (2.4.20), so iOS consumers need Kotlin 2.4+ (documented).
- **README additions from the same checks:** JVM consumers need `google()` in their repositories (`androidx.sqlite` is only on Google Maven); the API is `suspend`, so consumers need `kotlinx-coroutines`.
- **Not verifiable locally:** the Maven Central upload and the real `v*` tag run (need a push and repository secrets). The release workflow's YAML parses, and its guard script (`.github/scripts/check-release.sh`: `LICENSE` present, no `TODO(maintainer)` in `DATA_SOURCE.md`, tag = `VERSION_NAME`) was exercised locally in the passing and failing states.
- **Maintainer decisions taken as defaults and to be confirmed:** group `io.github.omarshehe`, licence Apache-2.0 (full text in `LICENSE`), `DATA_SOURCE.md` with `TODO(maintainer)` markers, so **the first public release stays blocked until the maintainer fills it in.**

---

## Out of Scope

- Geocoding/coordinates (deferred TZA-006).
- Automatic dataset update detection or scraping of newer PDF editions.
- Documentation site (README only).
- Publishing `:app` or `:importer`.

---

## Open Questions

None. (Redistribution terms are enforced as an acceptance criterion via `DATA_SOURCE.md`, not left implicit.)

---

## Files Expected to Change

| File | Change |
|---|---|
| `gradle.properties` | Modified (`VERSION_NAME`, group) |
| `gradle/libs.versions.toml` | Modified (publishing plugin, if a plugin is used) |
| `core/build.gradle.kts`, `data/build.gradle.kts`, `ui/build.gradle.kts` | Modified (publishing + POM) |
| `.github/workflows/release.yml` | New |
| `README.md` | Modified |
| `CHANGELOG.md` | New |
| `DATA_SOURCE.md` | New |
| `LICENSE` | New |
