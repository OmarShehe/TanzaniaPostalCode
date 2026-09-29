# TZA-004: Compose Multiplatform address-picker UI module

**Type:** Feature
**Priority:** Medium
**Platform:** `:ui` (new KMP module), `:app`
**Depends on:** TZA-003 (repository API: `AddressRepository.search/path`, `AddressPath`, `AddressMatch`, `Level`, all in `:core`)

---

## Problem Statement

Consumers of the address data all end up building the same two widgets: a type-ahead address search field and a cascading region → district → ward → mtaa → kitongoji picker. Without a shared, optional UI module each project reimplements them. The owner wants to plug this library into several projects, so an optional ready-made picker removes repeated work — without forcing UI dependencies on backend or headless users.

---

## User Stories

- As a **Consumer developer**, I want a drop-in `AddressPicker` composable that returns a selected address path, so that my form needs one line to capture a Tanzanian address.
- As a **Consumer developer**, I want a standalone `AddressSearchField` (autocomplete), so that I can use it in my own layouts.
- As a **Consumer developer**, I want to theme and localise the widgets, so that they match my app.
- As a **Maintainer**, I want the sample app to demonstrate both widgets, so that regressions are visible.

---

## Approved Design / Behaviour

`:ui` is a separate artifact (Compose Multiplatform: Android + iOS + desktop JVM) that depends on **`:core` only**: the widgets take any `AddressRepository`, so they do not need `:data`. Only the sample `:app` depends on `:data`. Backend/headless users depend on `:core` + `:data` and never on `:ui`.

### Components

```
AddressSearchField(
  repository: AddressRepository,
  onSelected: (AddressPath) -> Unit,
  modifier, label, placeholder)

AddressPicker(
  repository: AddressRepository,
  value: AddressPath?,
  onValueChange: (AddressPath?) -> Unit,
  requiredLevel: Level = Level.WARD,     // deepest level the form requires
  modifier)
```

### Behaviour

- **Search field:** debounced (250 ms) calls to `repository.search`; shows up to 10 suggestions, each as `Name — District, Region` plus postcode; selecting fills the field and calls `onSelected`. Empty query shows no list; no results shows a "No matches" row; an exception from the repository shows an inline error row and does not crash.
- **Picker:** five dependent dropdowns (Region, District, Ward, Mtaa/Village, Kitongoji). A level is disabled until its parent is chosen; changing a parent clears all descendants; levels below `requiredLevel` are shown as optional. Selecting a ward displays its postcode read-only. A Mtaa or Kitongoji level with no children (source data gaps: some wards have no mtaa, most mtaa have no kitongoji) shows a "None listed" state and counts as complete.
- **`onValueChange` semantics:** emits `null` until every level up to `requiredLevel` is chosen (or is "None listed"), then emits the deepest chosen `AddressPath` and re-emits as optional deeper levels are chosen or cleared. Selection is restored from ids via `repository.path(level, id)`.
- Selection state survives configuration change / process death on Android (`rememberSaveable` holding the deepest `Level` + its id) and is restored via `repository.path`.
- All strings live in a resources file; English and Swahili provided; consumers can override via Compose resources.
- Accessibility: every control has a content description; touch targets ≥ 48 dp; dropdowns are keyboard/TalkBack operable.
- Uses `MaterialTheme` from the host app; no hard-coded colours.

Layout (compact, phone width):

```
Region        [ Dar es Salaam        v ]
District      [ Ilala CBD            v ]
Ward          [ Kivukoni             v ]   Postcode 11101
Mtaa/Village  [ Select…              v ]
Kitongoji     [ (select mtaa first)  v ]   (disabled)
```

---

## Acceptance Criteria

- [ ] Consumer developer: typing `kiv` in `AddressSearchField` shows Kivukoni suggestions with district, region and postcode after the debounce, and selecting one invokes `onSelected` with the matching `AddressPath`.
- [ ] Consumer developer: an empty query shows no suggestions; a nonsense query shows "No matches"; a repository that throws shows an inline error and no crash.
- [ ] Consumer developer: in `AddressPicker`, District is disabled until Region is chosen, and changing Region clears District, Ward, Mtaa and Kitongoji.
- [ ] Consumer developer: with `requiredLevel = Ward`, `onValueChange` emits a non-null value once a ward is chosen and shows Mtaa/Kitongoji as optional.
- [ ] Consumer developer: selecting a ward shows its 5-digit postcode.
- [ ] Consumer developer: rotating the device (Android) preserves the selection.
- [ ] Consumer developer: the widgets render in English and Swahili when the device language changes.
- [ ] Maintainer: Compose UI tests (commonTest, run on desktop JVM; an Android device test on the connected phone) cover the search and cascade behaviours above using a fake repository declared in `:ui`'s tests (`:core`'s `FakeAddressRepository` is test-only and not visible to other modules).
- [ ] Maintainer: `:ui` compiles for Android, iOS and desktop JVM; `:core`/`:data` have **no** dependency on `:ui`.
- [ ] Maintainer: the sample `:app` shows both widgets against the real bundled DB; screenshot(s) saved under `docs/` (linking them from the README is TZA-005).

---

## Technical Notes

### Impacted Modules
| Module | Change type | Notes |
|---|---|---|
| `:ui` | New | Compose Multiplatform module, strings, tests |
| `:app` | Modified | Sample screen using the widgets |
| `:core`, `:data` | None | |

### DB / Migration
No DB changes.

### API Contract
Composable API above.

### Key Implementation Notes
- Compose Multiplatform 1.12.1 and Material 3 1.9.0 are the latest stable at refine time (2026-09-29); the Compose compiler plugin (`org.jetbrains.kotlin.plugin.compose`, Kotlin-versioned) is required alongside the `org.jetbrains.compose` plugin. Both plugins are needed only in `:ui` and `:app`; applying them to `:data` broke the plain-JVM compile (found in TZA-003).
- The `:app` View-based sample screen from TZA-003 (search box + list) is replaced by a Compose activity in this ticket; `activity_main.xml` and the AppCompat theme go. The repository comes from `createAddressRepository(context)` (a closable `AddressStore`); the activity/ViewModel owns it and closes it.
- Desktop JVM: `:core` already has a `jvm()` target; running the desktop target on an Intel Mac is fine for `:ui` (it does not touch SQLite).
- iOS: klib compile only here (no Xcode); iOS Compose behaviour is unverified.
- **Rejected alternative:** a single monolithic module including UI. Cost: every backend or non-Compose consumer pulls Compose dependencies.
- **Rejected alternative:** an Android-View widget. Cost: no iOS/desktop reuse and legacy tech.
- If Compose Multiplatform iOS is not wanted by a consumer, they can still use the widgets on Android only; iOS support here means "compiles and runs", not a native SwiftUI wrapper.

---

## Scope notes (added during implementation)

- **Logic is unit-tested apart from the views:** `AddressSearchController` and `AddressPickerController` hold every rule (debounce, cascade, `requiredLevel`, "None listed", restore); the composables are thin. Saveable state is `PickerSelection` (deepest level + id) rather than a file named `AddressUiState.kt`.
- **Suggestions** read `Name — District, Region` plus postcode; for mtaa and kitongoji the ward name is added (`Kariakoo Magharibi — Kariakoo, Ilala CBD, Dar es Salaam`), since those names repeat across wards.
- **Suggestions render inline** under the field (no popup) so they are simple to test and to read with TalkBack; the picker uses one `ExposedDropdownMenuBox` per level.
- **`value` handling:** the picker restores from the host's `value` (or its own saved selection when `value` is null), keeps even a partial selection across rotation, and follows the host when it later changes `value` (e.g. null to reset the form).
- **Tests:** controllers and the saver run in common tests; Compose UI tests (`runComposeUiTest`) run on desktop JVM only (kept out of the Android host test, which has no UI harness); the Swahili test is JVM-only (sets the default locale). Device tests live in **`:app`** (`SampleAppTest`), not `:ui`: they drive both widgets over the real bundled database and check rotation. The AGP-KMP device-test variant of a library does not package the library's Compose resources, and `StateRestorationTester` is not implemented on desktop, so an app-level instrumented test is the reliable place.
- **Android resources:** the AGP-KMP library plugin has Android resource processing off by default; without `androidResources { enable = true }` in `:ui` the Compose strings never reach the AAR and the app throws `MissingResourceException` (found by the device test).
- **Restored value:** after a selection is restored from the picker's own saved state, the host is told the restored value via `onValueChange` (found on the phone: the host showed "Nothing selected" after rotation while the picker showed a ward).
- **Sample app** follows the system light/dark theme and calls `enableEdgeToEdge()`; the AppCompat theme and layout are gone. Screenshots: `docs/search.png`, `docs/picker.png`, `docs/picker-sw.png` (Swahili).
- **iOS:** klib compile only; behaviour unverified (no Xcode).

---

## Out of Scope

- Map preview or GPS "use my location" (coordinates/geocoding, deferred TZA-006).
- SwiftUI/UIKit-native widgets.
- Free-text "street address" line entry.
- Publishing (TZA-005).

---

## Open Questions

None.

---

## Files Expected to Change

| File | Change |
|---|---|
| `settings.gradle.kts` | Modified (`include(":ui")`) |
| `gradle/libs.versions.toml` | Modified (Compose Multiplatform plugin/libs) |
| `ui/build.gradle.kts` | New |
| `ui/src/commonMain/kotlin/.../AddressSearchField.kt`, `AddressPicker.kt`, `AddressUiState.kt` | New |
| `ui/src/commonMain/composeResources/values/strings.xml`, `values-sw/strings.xml` | New |
| `ui/src/commonTest/kotlin/**`, `ui/src/androidDeviceTest/**` | New (fake repository lives here) |
| `app/build.gradle.kts`, `app/src/main/java/.../MainActivity.kt` | Modified |
| `app/src/main/res/layout/activity_main.xml`, `app/src/main/res/values/{styles,strings}.xml` | Deleted/Modified (Compose activity; AppCompat theme replaced) |
