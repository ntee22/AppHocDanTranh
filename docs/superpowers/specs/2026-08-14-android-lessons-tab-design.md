# Android Lessons Tab — Dan Thanh Guzheng

**Date:** 2026-08-14
**Status:** Approved design, implementing
**Parent:** [2026-08-13-android-port-design.md](2026-08-13-android-port-design.md)

## Goal

Port the iOS Lessons tab (tab 1, `Khóa học`) to Android: the lesson catalog, the
vintage list screen, the detail screen, and YouTube playback for the six free
lessons. This replaces the `PlaceholderScreen` currently wired to
`Destination.LESSONS`.

## Scope

The Lessons tab on iOS spans a data layer, two screens, video playback, and
billing. This spec covers the first three. Billing is a separate sub-project.

**In scope**

- `Lesson` model and the ten-lesson catalog
- Lesson progress persistence (which free lessons the user has opened)
- `LessonsScreen` — the full vintage list screen
- `LessonDetailScreen` — metadata, cards, and the coming-soon state
- YouTube playback for the six free lessons

**Out of scope**

| Deferred | Why |
|---|---|
| Google Play Billing, paywall sheet | Own sub-project; the four locked lessons show iOS's "coming soon" card, which is what iOS ships today anyway |
| Cloudflare secure player | Only used by paid lessons, which have no video IDs yet |
| Play In-App Review trigger | iOS calls `ReviewManager.recordPositiveAction()` at 80% watched. Play's equivalent is separate work. The player leaves the hook point but does not wire it. |
| Update banner | iOS `UpdateChecker` polls the App Store. No meaning on Play; Play updates in the background. |

## Source of truth

| Android file | iOS source |
|---|---|
| `Lesson.kt`, `LessonCatalog.kt` | `Models/Lesson.swift` |
| `LessonsViewModel.kt` | `ViewModels/LessonViewModel.swift` |
| `LessonsScreen.kt` | `Views/HomeView.swift` |
| `LessonDetailScreen.kt` | `Views/LessonDetailView.swift` |
| `VintageComponents.kt`, `PlumBranch.kt`, `LessonThumb.kt` | `Views/Components/VintageComponents.swift` |
| `YouTubeLessonPlayer.kt` | `Views/Components/YouTubePlayerView.swift` |

`Views/Components/LessonCard.swift` and `LockBadge.swift` are dead code on iOS —
`HomeView` uses its own `LessonRow`. They are not ported.

## Decisions

| Decision | Choice | Rationale |
|---|---|---|
| Lesson content source | Hardcoded Kotlin `LessonCatalog` object | Mirrors iOS `Lesson.mockLessons()`. No backend exists for lessons. |
| Content localization | Vietnamese only; chrome localized | iOS ships Vietnamese `description`/`practiceTips`/`whatYoullLearn`/`funFact` to both languages, with `subtitle` carrying the English title. Lifting content into `values-en` would mean inventing English content that does not exist on iOS, then maintaining the divergence twice. |
| Detail navigation | Flat route in the existing `NavHost` | Bottom bar stays visible on detail, matching iOS pushing inside the tab's own `NavigationStack`. A nested graph would hide it. |
| Progress storage | Existing `app_prefs` DataStore | `AppPrefs` already owns this store; replaces iOS `UserDefaults["openedLessonNumbers"]`. |
| YouTube playback | `android-youtube-player` 12.1.2 | Same architecture as iOS `YouTubePlayerKit`: the YouTube IFrame API inside a managed WebView, which is what survives YouTube's 2025 `web_embedded` enforcement (errors 150–153). Hand-rolling a WebView would re-solve a problem the iOS side already moved away from. |
| Component placement | `ui/components/`, not `ui/lessons/` | Video, Tuner, Sheet, and Settings all reuse `PaperBackground`, `OverlineLabel`, `InkSeal`, and `BrushDivider`. |

### Rejected alternatives

- **Raw `WebView` + IFrame API** — no dependency, but we hand-maintain the origin
  and referrer handling that YouTube's 2025 enforcement requires. iOS explicitly
  moved to a library for this.
- **Intent hand-off to the YouTube app** — trivial, but the user leaves the app and
  all playback signals are lost. A downgrade from iOS.
- **Lesson content in `strings.xml`** — see localization decision above.

## Architecture

```
vn.hocdantranh.guzheng/
├── data/lesson/
│   ├── Lesson.kt                 data class, mirrors the Swift struct
│   ├── LessonCatalog.kt          the ten lessons, verbatim from Lesson.swift
│   └── LessonProgressPrefs.kt    openedLessonNumbers in the app_prefs DataStore
├── ui/components/
│   ├── VintageComponents.kt      PaperBackground, OverlineLabel, InkSeal, BrushDivider
│   ├── PlumBranch.kt             Canvas corner ornament
│   └── LessonThumb.kt            Canvas 16:10 gradient tile
└── ui/lessons/
    ├── LessonsViewModel.kt
    ├── LessonsScreen.kt
    ├── LessonRow.kt
    ├── LessonDetailScreen.kt
    └── YouTubeLessonPlayer.kt
```

### Data model

`Lesson` mirrors the Swift struct field-for-field:

```kotlin
data class Lesson(
    val lessonNumber: Int,
    val title: String,
    val subtitle: String,
    val thumbnailUrl: String,
    val videoId: String,
    val isFree: Boolean,
    val duration: String,
    val isYouTube: Boolean = false,
    val isPortrait: Boolean = true,
    val description: String = "",
    val practiceTips: List<String> = emptyList(),
    val whatYoullLearn: List<String> = emptyList(),
    val funFact: String? = null,
)
```

Swift's `id: String = UUID()` is dropped. It is never read — `HomeView` keys on
`lesson.id` only for `ForEach` identity, and `lessonNumber` is already unique and
stable. It also serves as the navigation argument.

`isPortrait` is carried for parity but unused: iOS pins the player to a fixed
200pt height regardless.

### Catalog

Ten lessons, content copied verbatim from `Lesson.swift`:

- **1–6 free**, `isYouTube = true`, real YouTube IDs, full `description`,
  `practiceTips`, `whatYoullLearn`, `funFact`
- **7–10 locked**, empty `videoId` and `thumbnailUrl`, `description` only

### Progress

`LessonProgressPrefs` exposes `openedLessonNumbers: Flow<Set<Int>>` and
`suspend fun markOpened(lessonNumber: Int)`, backed by
`stringSetPreferencesKey("openedLessonNumbers")` in the existing store. Values are
stored as strings because DataStore has no int-set type; non-numeric entries are
dropped on read so a corrupt store cannot crash the list.

### ViewModel

`LessonsViewModel` exposes a single `LessonsUiState`:

| Field | iOS equivalent |
|---|---|
| `lessons` | `viewModel.lessons` |
| `openedLessonNumbers` | `viewModel.openedLessonNumbers` |
| `completedFreeCount` | free lessons whose number is in `openedLessonNumbers` |
| `freeLessonsCount` | count of free lessons |
| `nextLesson` | first free lesson not yet opened; `null` when all are done |

`markOpened(lesson)` no-ops on locked lessons, matching the iOS `guard`.

iOS's artificial 0.3s `loadLessons()` delay is not ported — the catalog is a
compile-time constant and there is nothing to wait for.

### Screens

**`LessonsScreen`** — scrolling `LazyColumn` over `PaperBackground`:

1. Masthead — `卷 一` / rule / `Volume One` overline, "Hành trình" (44sp serif
   medium, ink) over "Đàn Tranh" (44sp serif light italic, sage), Vietnamese or
   English blurb capped at 280dp, plum-branch ornament in the top-right at 0.35
   opacity
2. Progress card — 44dp ring showing `completed/total`, overline reading
   `📖 Đang học` or `🎉 Hoàn thành`, next lesson title, and a sage "Tiếp" pill that
   navigates straight to that lesson
3. Section header — `Mục lục` overline, "Tất cả bài học 🎼" in serif italic, and
   the `樂` ink seal
4. Lesson rows
5. Footer — `· 琴 ·`

**`LessonRow`** — 92dp `LessonThumb`, then a metadata line
(`MIỄN PHÍ · 8:45` in sage, or `COMING SOON · 16:20` in muted ink), 17sp serif
title capped at two lines, 11sp muted subtitle, and a chevron. A hairline divider
inset 106dp separates rows, omitted after the last.

Accent cycles `sage, wood, sage, plum, wood, sage, plum, wood, sage, plum, wood,
sage, plum, wood, sage, plum` indexed by `(lessonNumber - 1) % 16`, exactly as iOS
does.

**`LessonDetailScreen`** — inline top bar with a back arrow and a centered
`BÀI 01 · 8:45` overline, then:

- Free: the YouTube player at 200dp on black
- Locked: the coming-soon card (lock glyph, "Bài học sắp ra mắt 🚧", body copy)

followed by a status pill, 28sp serif title, 16sp serif italic subtitle, a 120dp
brush divider, the description, and up to three cards — what-you'll-learn
(numbered, sage), practice tips (numbered, seal-red bullet), and fun fact (serif
italic on a sage-to-paper gradient). Cards render only when their content exists;
the description and practice tips fall back to the iOS default copy when empty.

`markOpened` fires once on first composition, matching iOS `.onAppear`.

### Player

`YouTubeLessonPlayer` wraps `YouTubePlayerView` in an `AndroidView`. The view is
registered with the local `LifecycleOwner` so background/foreground is handled,
and released in `onRelease`. On player error it swaps to a black surface with a
"Xem trên YouTube ↗" button that opens
`https://www.youtube.com/watch?v={videoId}` — the same fallback as iOS.

`android.permission.INTERNET` is already declared in the manifest.

## Localization

New chrome strings in `values/` and `values-en/`, following the existing
`tab_*` / `screen_*` naming:

`lessons_volume_one`, `lessons_hero_line1`, `lessons_hero_line2`,
`lessons_blurb`, `lessons_progress_in_progress`, `lessons_progress_done`,
`lessons_all_done`, `lessons_next`, `lessons_contents`, `lessons_all_lessons`,
`lesson_badge_free`, `lesson_badge_coming_soon`, `lesson_detail_prefix`,
`lesson_coming_soon_title`, `lesson_coming_soon_body`, `lesson_youll_learn`,
`lesson_practice_tips`, `lesson_fun_fact`, `lesson_default_description`,
`lesson_default_tip_1`, `lesson_default_tip_2`, `lesson_watch_on_youtube`,
`lesson_player_error`, `lesson_back`.

The hero lines and `· 琴 ·` stay Vietnamese/Chinese in both locales, as on iOS.

## Testing

TDD throughout — test first, watch it fail, then implement.

**JVM / Robolectric**

- `LessonCatalogTest` — ten lessons; numbers 1–10 unique and ordered; exactly six
  free; every free lesson has a non-blank `videoId` and `isYouTube = true`; every
  locked lesson has a blank `videoId`
- `LessonProgressPrefsTest` — empty by default; `markOpened` persists; repeated
  calls stay idempotent; non-numeric stored values are ignored
- `LessonsViewModelTest` — `completedFreeCount` counts only free opened lessons;
  `nextLesson` returns the first unopened free lesson and `null` once all six are
  open; `markOpened` on a locked lesson changes nothing

**Compose UI, on device**

- `LessonsScreenTest` — all ten rows render; free and locked rows show the right
  badge; tapping a row opens detail; "Tiếp" opens the next unopened lesson
- `LessonDetailScreenTest` — a free lesson shows the player; a locked lesson shows
  the coming-soon card and no player; opening a lesson advances the progress ring

## Verification

Build, install, and drive the app on the connected OPPO CPH2363 (`adb`), then
confirm the list and detail screens against the iOS screens visually before
calling this done.
