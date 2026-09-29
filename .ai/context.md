# Project context — TZ Address Kit

Offline Tanzanian address data (Region → District → Ward → Mtaa/Village → Kitongoji) as a Kotlin Multiplatform library. Epic tickets: `.ai/tickets/TZA-*.md`; per-ticket specs: `.ai/specs/`.

## Module map
| Module | Kind | Purpose |
|---|---|---|
| `:core` | KMP library (Android, JVM, iosArm64, iosSimulatorArm64) | Domain model, `AddressRepository`, `AddressIds`. Package root `com.omarshehe.tzaddress`. `commonMain` must not import `android.*` or `java.*`. |
| `:app` | Android application | Sample app; package `com.omarshehe.tanzaniapostalcode`. |

Planned (by ticket): `:importer` (TZA-002), `:data` (TZA-003), `:ui` (TZA-004).

## Build & test
- Use JDK 17+ (JAVA_HOME). Versions live only in `gradle/libs.versions.toml`.
- `./gradlew :core:jvmTest :core:testAndroidHostTest` — core unit tests.
- `./gradlew :core:compileKotlinIosArm64 :core:compileKotlinIosSimulatorArm64` — iOS klib compile (works without Xcode).
- `./gradlew :app:assembleDebug :app:assembleRelease`
- `./gradlew build` — fails on this machine at `:core:linkDebugTestIosSimulatorArm64` (linking iOS test binaries needs full Xcode; only Command Line Tools are installed). Locally use `-x linkDebugTestIosSimulatorArm64 -x iosSimulatorArm64Test -x linkDebugTestIosArm64`.
- Checks: `grep -rnE "^import (android|java)\." core/src/commonMain` and `grep -rn "\bvar\b" core/src/commonMain/kotlin/com/omarshehe/tzaddress/model` must return nothing.

## Workflow
Local-only for now: no pushes. Integration branch `feature/tz-address-kit`; per-ticket branches `feature/TZA-NNN` merge into it.

## Known caveats
- Kotlin/Native reports host `macos_x64` as deprecated (Intel Mac); harmless for now.
