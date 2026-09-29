# TZA-004: Compose Multiplatform address-picker UI module

**Type:** Feature
**Priority:** Medium
**Platform:** `:ui` (new KMP module), `:app`
**Depends on:** TZA-003 (repository API)

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

`:ui` is a separate artifact (Compose Multiplatform: Android + iOS + desktop JVM) that depends on `:core` and `:data`. Backend/headless users depend on `:core` + `:data` only.

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
  requiredLevel: Level = Level.Ward,     // deepest level the form requires
  modifier)
```

### Behaviour

- **Search field:** debounced (250 ms) calls to `repository.search`; shows up to 10 suggestions, each as `Name — District, Region` plus postcode; selecting fills the field and calls `onSelected`. Empty query shows no list; no results shows a "No matches" row; an exception from the repository shows an inline error row and does not crash.
- **Picker:** five dependent dropdowns (Region, District, Ward, Mtaa/Village, Kitongoji). A level is disabled until its parent is chosen; changing a parent clears all descendants; levels below `requiredLevel` are shown as optional. Selecting a ward displays its postcode read-only. Deep levels with no children (source data gaps) show a "None listed" state and count as complete.
- Selection state survives configuration change / process death on Android (`rememberSaveable` with the IDs) and is restored from IDs via `repository.path`.
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
- [ ] Maintainer: Compose UI tests (commonTest/Android instrumented) cover the search and cascade behaviours above using `FakeAddressRepository`.
- [ ] Maintainer: `:ui` compiles for Android, iOS and desktop JVM; `:core`/`:data` have **no** dependency on `:ui`.
- [ ] Maintainer: the sample `:app` shows both widgets against the real bundled DB; screenshot(s) added to the README.

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
- Compose Multiplatform / Material 3 versions to be confirmed as latest stable at implementation time.
- The old `:app` XML/AppCompat layout is replaced by a Compose activity in this ticket (the app is only a sample).
- **Rejected alternative:** a single monolithic module including UI. Cost: every backend or non-Compose consumer pulls Compose dependencies.
- **Rejected alternative:** an Android-View widget. Cost: no iOS/desktop reuse and legacy tech.
- If Compose Multiplatform iOS is not wanted by a consumer, they can still use the widgets on Android only; iOS support here means "compiles and runs", not a native SwiftUI wrapper.

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
| `ui/src/commonTest/kotlin/**`, `ui/src/androidInstrumentedTest/**` | New |
| `app/build.gradle.kts`, `app/src/main/java/.../MainActivity.kt` | Modified |
| `app/src/main/res/layout/activity_main.xml` | Deleted |
