# Android Sheet Tab — Dan Thanh Guzheng

**Date:** 2026-09-05
**Status:** Approved design, ready to plan
**Parent:** [2026-08-13-android-port-design.md](2026-08-13-android-port-design.md)

## Goal

Port the iOS Sheet tab (tab 4, `Sheet`) to Android: the Firestore-backed sheet-music
library, title search, the page viewer, and pinch-to-zoom on a single page. Replaces the
`PlaceholderScreen` wired to `Destination.SHEET`.

## Scope

**In scope**

- `SheetItem` model and the Firestore repository (single ordered query, no pagination)
- Google Drive URL rewriting so page images actually load
- Local cache of the catalog, so the list paints instantly on relaunch
- `SheetScreen` — header, search bar, cards, skeleton / empty / error states
- `SheetViewerScreen` — scrolling list of page images
- `SheetPageZoomScreen` — full-screen pinch/pan/double-tap on one page

**Out of scope**

| Deferred | Why |
|---|---|
| Gating premium sheets behind a purchase | iOS shows the lock badge but opens the sheet anyway. Android mirrors that. Play Billing is its own cycle. |
| Writing / editing / reordering sheets | Admin work lives in the `create-sheet` skill, not the app |
| Fractional `order` support | iOS parses `order` as `Int` and the skill refuses fractional writes. Changing this is a cross-platform decision, not a Sheet-tab one. |
| Downloading or printing a sheet | iOS has neither |

## Source of truth

| Android file | iOS source |
|---|---|
| `data/sheet/SheetItem.kt` | `Views/SheetMusicView.swift` (`SheetRepository.SheetItem`) |
| `data/sheet/SheetRepository.kt` | `Views/SheetMusicView.swift` (`SheetRepository.fetch`) |
| `data/sheet/SheetCache.kt` | `Views/SheetMusicView.swift` (`saveCache` / `loadCache`) |
| `data/sheet/DriveImageUrl.kt` | `Views/SheetMusicView.swift` (`directImageURL`) |
| `ui/sheet/SheetScreen.kt` | `Views/SheetMusicView.swift` (`SheetMusicView`) |
| `ui/sheet/SheetCards.kt` | `Views/SheetMusicView.swift` (`SheetCard`, `SheetSkeletonCard`) |
| `ui/sheet/SheetViewerScreen.kt` | `Views/SheetMusicView.swift` (`SheetViewer`) |
| `ui/sheet/SheetPageZoomScreen.kt` | `Views/SheetMusicView.swift` (`PageZoomView`) |

## Live data (verified 2026-09-05 via `create-sheet/scripts/inspect.js`)

The `sheets` collection holds **19 documents**, all `isVisible: true`, none skipped by the
iOS parser.

| Field | Type | Notes |
|---|---|---|
| `title` | string | Vietnamese |
| `pages` | array\<string\> | 22 URLs total; 17 sheets have 1 page, 2 have 2 pages |
| `isFree` | bool | **all `true` today** |
| `isVisible` | bool | all true today |
| `order` | integer | **-1 … 17** — newest sheets use non-positive orders |

Four consequences the implementation must respect:

1. **Every one of the 22 page URLs is a `drive.google.com/file/d/<id>/view?...` link.**
   The rewrite to `lh3.googleusercontent.com/d/<id>` is load-bearing, not defensive —
   without it Coil receives Drive's HTML page and every sheet renders blank. This is the
   single highest-risk item in the port.
2. **`order` goes negative.** The sort must be a signed comparison, and any parsing that
   assumes a positive index is wrong. `-1` sorts above `0`, which sorts above `1`.
3. **No premium sheet exists in live data.** The lock overlay and `Premium` pill cannot
   be verified on-device against Firestore, so they get a synthetic-item test instead.
4. **Firestore hands integers back as `Long`.** `fields["order"] as? Int` silently
   returns null on Android where the iOS `as? Int` succeeds. Parse via `Number.toInt()`.

## Data layer

### `SheetItem`

```kotlin
data class SheetItem(
    val id: String,
    val title: String,
    val pages: List<String>,
    val isFree: Boolean,
    val order: Int,
)
```

`sheetFromFields(id, fields): SheetItem?` mirrors `videoFromFields`: returns null when
`isVisible != true`, when `title` or `pages` is missing or the wrong type, or when `order`
is absent. `isFree` defaults to `true` when absent, matching iOS.

### `DriveImageUrl`

`directImageUrl(raw: String): String` — pure, no Android or network dependency.

- Not a `drive.google.com` URL → returned unchanged
- File id from `/d/<id>` (stopping at `/` or `?`), else from an `id=` query parameter
- No id extractable → returned unchanged, so a malformed link degrades to a broken image
  rather than a crash
- Otherwise → `https://lh3.googleusercontent.com/d/<id>`

### `SheetRepository`

```kotlin
interface SheetRepository {
    suspend fun load(): List<SheetItem>
}
```

`FirestoreSheetRepository` runs `collection("sheets").orderBy("order")` — a single-field
order that uses the auto-created index, with `isVisible` filtered client-side so no
composite index is needed. Same reasoning as iOS.

### `SheetCache`

Serializes `List<SheetItem>` to JSON in DataStore under key `cached_sheets_v1`, reusing
the iOS cache key name for symmetry. Decode failures are swallowed and treated as a cache
miss — a schema change must never brick the tab. Bump to `v2` if `SheetItem` changes shape.

## UI layer

### State

```kotlin
data class SheetUiState(
    val sheets: List<SheetItem> = emptyList(),
    val query: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
) {
    val filtered: List<SheetItem>   // case-insensitive title contains, or all when blank
}
```

Derived filtering lives in the state class, not the composable, so search is testable
without a device — the same split as `VideoUiState`. `filtered` uses
`title.contains(query, ignoreCase = true)`, which matches iOS
`localizedCaseInsensitiveContains`: case-insensitive but **diacritic-sensitive**, so `nho`
does not find `Nhỏ ơi`. Diacritic folding would be a nicer search for Vietnamese typists,
but it is a cross-platform product change, not a port decision.

`SheetViewModel.refresh()` emits the cached list first (if any), then the network result.
On failure with a non-empty cached list, the list stays and no error is shown; on failure
with nothing cached, `errorMessage` renders the retry state. This is a deliberate
improvement over iOS, which clears nothing but also shows the error over stale content.

### `SheetScreen`

`PaperBackground` + `Column` of header, search bar, content.

- **Header** — `OverlineLabel("樂譜 · SHEET NHẠC")`, serif 32sp title split two-tone
  (`Sheet ` in `inkDark` + `Guzheng` italic in `sageDark`), tagline in `inkSoft`,
  `InkSeal("樂")` trailing. Padding `top 56 / horizontal 24 / bottom 14`, as iOS.
- **Search bar** — leading magnifier, `BasicTextField`, trailing clear button when
  non-empty. `paperWarm` at 80% on a 10dp rounded rect with a `woodDark` 20% border.
- **Content** — exactly one of:
  - error + retry, when `errorMessage != null` and nothing is cached
  - six `SheetSkeletonCard`s with a shimmering alpha, while loading an empty list
  - empty state — a large faded `樂` glyph, with copy that differs for "no sheets at all"
    vs. "no results for <query>"
  - `LazyColumn` of `SheetCard`, 10dp spacing, 18dp horizontal, 110dp bottom padding to
    clear the navigation bar

### `SheetCard`

Row: 64×80 thumbnail, info column, spacer, chevron. Thumbnail is a Coil image of
`directImageUrl(pages.first())` over a `paperDeep` rounded rect, falling back to a
music-note icon on failure. When `!isFree`, a 50% black scrim and a white lock icon sit on
top. Info column is the serif 18sp title (max 2 lines) above a row of `N trang` and a
Free/Premium pill — `sageDark` for free, `plum` for premium, each at 10% opacity behind.

The card is clickable regardless of `isFree`, per the approved decision.

### Navigation

Lesson detail was added to the shared `NavHost` so the bottom bar stays visible, because
iOS pushes it inside the tab's own `NavigationStack`. **The sheet viewer is different:**
iOS presents it as a `fullScreenCover`, which covers the tab bar. Android matches by adding

- `sheet/{sheetId}` → `SheetViewerScreen`
- `sheet/{sheetId}/page/{pageIndex}` → `SheetPageZoomScreen`

to the same `NavHost`, and having `MainScaffold` render no `NavigationBar` when the current
route is either of them. Route membership is decided by a single
`fun isFullScreenRoute(route: String?): Boolean` so the rule is unit-testable and there is
one place to add future full-screen routes.

Both screens read their sheet from a shared `SheetViewModel` scoped to the tab, so the
viewer needs no argument beyond the id and no second Firestore read. A direct navigation
to an unknown id pops back rather than rendering an empty viewer.

### `SheetViewerScreen`

Back chip (38dp `paperWarm` circle + chevron) beside the serif italic title, `BrushDivider`,
then a `LazyColumn` of pages. A one-time hint sits above the first page. Each page is a Coil
image at `fillMaxWidth` with `ContentScale.FillWidth`, 8dp corners, 16dp horizontal padding,
tappable to open the zoom route. A page that fails to load shows a 200dp-tall
"could not load page N" block rather than collapsing to zero height.

### `SheetPageZoomScreen`

Black background, `ZoomableAsyncImage` from Telephoto with `maxZoomFactor = 6f` to match
the iOS clamp. Close button top-left, `Trang N` top-right, gesture hint at the bottom.
Telephoto supplies pan clamping, double-tap-to-zoom and fling; the iOS gesture math is not
ported by hand.

New dependency: `me.saket.telephoto:zoomable-image-coil:0.15.1`, pinned in
`libs.versions.toml`. Version chosen by checking the published POMs rather than taking the
newest release — 0.15.1 is the last version whose transitive dependencies all sit at or
below what this project already resolves:

| Telephoto | coil-compose | compose foundation | kotlin-stdlib | Verdict |
|---|---|---|---|---|
| 0.13.0 / 0.14.0 | 2.6.0 | 1.6.7 | 1.9.24 | older Coil than ours |
| **0.15.1** | **2.7.0** | **1.7.3** | **2.1.0** | **exact Coil and Kotlin match; foundation is below the BOM's 1.7.6, so the BOM stays authoritative** |
| 0.16.0 … 0.19.0 | 2.7.0 | 1.8.0 | 2.1.21 | would drag Compose past the BOM and the stdlib past the Kotlin 2.1.0 compiler |

Coil 3 is not a risk here: it lives in a separate `zoomable-image-coil3` artifact, so the
`zoomable-image-coil` line stays on Coil 2 across all of these.

## Localization

All copy goes through `strings.xml` (vi, default) and `values-en/strings.xml`, following
the existing `video_*` naming with a `sheet_*` prefix. Sheet **titles** stay Vietnamese —
they are content, and Firestore holds one title per document, matching the Lessons and
Video decisions.

Note the iOS screen has two untranslated strings — the `N trang` / `Miễn phí` / `Premium`
labels and the two viewer hints are hardcoded Vietnamese even in English mode. Android
translates them; this is a small, deliberate divergence that fixes an iOS oversight rather
than replicating it.

## Testing

**Unit (JVM, no device)**

- `directImageUrl` — a live `/file/d/<id>/view?usp=sharing` link rewrites correctly; an
  `?id=` link rewrites; a non-Drive URL passes through; a Drive URL with no extractable id
  passes through
- `sheetFromFields` — a valid doc maps; `isVisible: false` is dropped; a missing `title`,
  a missing `pages`, and a missing `order` are each dropped; absent `isFree` defaults true;
  a `Long` order parses; a **negative** order parses
- `SheetUiState.filtered` — blank query returns all; a query matches case-insensitively;
  a query that differs only by diacritics (`nho` vs `Nhỏ`) does **not** match; a
  non-matching query returns empty
- `SheetViewModel` against a fake repository — cache paints before the network resolves;
  a network success replaces the cached list; a network failure with a warm cache keeps the
  list and sets no error; a network failure with a cold cache sets `errorMessage`; retry
  clears the error
- `isFullScreenRoute` — both sheet routes are full-screen, the five tab routes are not

**Instrumented (device)**

- The card list renders titles from a fake repository
- Typing in the search bar filters the list; the clear button restores it
- The "no results" empty state renders for a query that matches nothing
- Skeletons render while loading
- A **synthetic** premium item renders the lock overlay and the `Premium` pill, and is
  still clickable — the only coverage of that branch, since no live sheet is premium
- Tapping a card navigates to the viewer and the bottom navigation bar disappears
- Tapping a page navigates to the zoom route

## Verification

Before anything else, confirm Telephoto did not drag the toolchain forward:
`./gradlew :app:dependencies --configuration releaseRuntimeClasspath` must still resolve
`androidx.compose.foundation` to the BOM's version and `io.coil-kt:coil-compose` to 2.7.0.
If either moved, the Telephoto version is wrong — revisit the table above rather than
bumping the BOM.

Then build, install, and drive the tab on the OPPO CPH2363 over `adb`: confirm all 19 sheets
load in order with `Haru Haru` topmost (order `-1`) and `Chúc Tết` last (order `17`), that
every thumbnail renders a real image rather than a blank Drive page, that searching
`nhỏ` narrows to `Nhỏ ơi`, that opening `Đoản xuân ca` shows both of its pages, that the
bottom bar is gone inside the viewer, and that pinch-zoom works on a page. Kill and relaunch
the app in airplane mode to confirm the cache paints the list. Compare against the iOS
screen before calling it done.
