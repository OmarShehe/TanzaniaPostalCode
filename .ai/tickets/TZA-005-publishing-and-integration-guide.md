# TZA-005: Publish the artifacts and document how to plug them into any project

**Type:** Chore
**Priority:** Medium
**Platform:** All (`:core`, `:data`, `:ui`)
**Depends on:** TZA-003 (required), TZA-004 (optional; `:ui` is published only if it has shipped)

---

## Problem Statement

The stated goal is a repo whose artifacts can be plugged into any of the owner's projects. Today nothing is publishable or documented: the README is one line, there is no group/artifact coordinate, no versioning policy, and no record of where the dataset came from. Without that, the "plug in" promise depends on copying source between projects.

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

`maven-publish` + signing, publishing to Maven Central via the Central Portal, run from a manual GitHub Actions workflow triggered by a `v*` tag. Credentials come from repository secrets only. `:app` and `:importer` are never published. A `publishToMavenLocal` path is documented for local integration testing.

### README (replaces the one-line file)

Sections: what it is; what data it covers (five levels, dataset edition); install snippets (Android/KMP `commonMain`, JVM backend); 10-line usage for search, postcode lookup, browse; the `:ui` picker (with screenshot from TZA-004); dataset update procedure (run `:importer:importPostcodes`, regenerate DB, bump version); versioning rules; licence; data attribution.

### Data source record

`DATA_SOURCE.md` records the PDF's title, publisher, edition/date, how it was obtained, and the terms under which the derived dataset may be redistributed. The PDF itself carries only generic metadata (`Microsoft Word - REGION.DAR`), so the Maintainer must fill this in.

---

## Acceptance Criteria

- [ ] Maintainer: `./gradlew publishToMavenLocal` publishes `tz-address-core` and `tz-address-data` (and `tz-address-ui` if present) with POM metadata (name, description, licence, SCM, developer).
- [ ] Consumer developer: a throwaway project outside this repo, using only the README's Android instructions and `mavenLocal()`, compiles and runs a `search("kivu")` call.
- [ ] Consumer developer: a throwaway JVM project using the README's JVM instructions runs `byPostcode("11101")` successfully.
- [ ] Maintainer: `:app` and `:importer` have no publication configured (`./gradlew publishToMavenLocal` produces no `app`/`importer` artifacts).
- [ ] Maintainer: pushing a `v*` tag runs the release workflow, which fails without signing secrets and never prints them in logs.
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
- Publishing plugin/version choices to be confirmed at implementation time.

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
