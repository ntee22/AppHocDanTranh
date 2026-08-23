# Android Video Tab — Dan Thanh Guzheng

**Date:** 2026-08-23
**Status:** Approved design, ready to plan
**Parent:** [2026-08-13-android-port-design.md](2026-08-13-android-port-design.md)

## Goal

Port the iOS Video tab (tab 2, `Video`) to Android: the Firestore-backed video
catalog, category filtering, the vintage card list, and hand-off to the YouTube
and TikTok apps. Replaces the `PlaceholderScreen` wired to `Destination.VIDEO`.

## Scope

**In scope**

- `VideoItem` model (`VideoSource`, `VideoCategory`) and the Firestore repository
- Cursor pagination (10 per page), pull-to-refresh, client-side `isVisible` filter
- `VideoScreen` — header, category chips, featured + standard cards, skeleton and empty states
- Thumbnail resolution for both platforms, including the TikTok oembed round-trip
- Opening a video in the YouTube / TikTok app (or browser fallback)

**Out of scope**

| Deferred | Why |
|---|---|
| In-app playback | iOS hands off to the platform app too; the parked in-app YouTube bug is unrelated and tracked separately |
| Writing/editing videos | Admin work happens in the `create-sheet` skill and Firestore console, not the app |
| Offline cache of the catalog | iOS has none; Firestore's own cache already covers a warm start |

## Source of truth

| Android file | iOS source |
|---|---|
| `data/video/VideoItem.kt` | `Models/VideoItem.swift` |
| `data/video/VideoRepository.kt` | `Managers/VideoRepository.swift` |
| `data/video/VideoThumbnails.kt` | `Managers/ThumbnailLoader.swift` |
| `ui/video/VideoScreen.kt` | `Views/VideosView.swift` (`VideoScreen`) |
| `ui/video/VideoCards.kt` | `Views/VideosView.swift` (`FeaturedVideoCard`, `VideoCard`, `SkeletonCard`, `CategoryChip`) |

## Live data (verified 2026-08-23 via Firestore REST)

The `videos` collection holds **12 documents**, all `isVisible: true`:

| Field | Type | Notes |
|---|---|---|
| `title` | string | Vietnamese |
| `url` | string | TikTok full URL, or YouTube `youtu.be/<id>?si=...` short form |
| `source` | string | `tiktok` (10) or `youtube` (2) |
| `category` | string | `songs` (6), `tutorial` (3), `intro` (3) |
| `order` | integer | sort key |
| `isVisible` | bool | all true today |

Two consequences the implementation must respect:

1. **No `tips` video exists.** iOS renders only categories present in the data, so
   the chip row shows "All" + 3 chips, not 4. Do not hardcode four chips.
2. **12 docs against a page size of 10** means pagination fires exactly once. This is
   the smallest real case that exercises `fetchMore`, so it is the pagination test.

## Decisions

| Decision | Choice | Rationale |
|---|---|---|
| Thumbnail loading | **Coil** (`io.coil-kt:coil-compose`) | TikTok needs an oembed round-trip per item and 10 of 12 videos are TikTok. iOS hand-rolls `URLSession` with no cache, re-fetching on every scroll. Coil gives memory+disk caching and scroll cancellation for one dependency. |
| Category chip labels | **Localized vi/en** in `strings.xml` | The Lessons spec's rule is *chrome localized, content Vietnamese*. Category names are chrome. iOS localizes only "All"/"Tất cả" and leaves chips Vietnamese — an inconsistency we do not port. |
| Repository seam | Interface + Firestore impl | Lets `VideoViewModel` be unit-tested with a fake; no emulator needed for filter/pagination logic. |
| Opening a video | `Intent.ACTION_VIEW` on the raw URL | Matches iOS `UIApplication.open`. Android resolves to the installed TikTok/YouTube app, else the browser. |
| Navigation | Flat tab screen, no detail route | iOS has no detail screen for videos — the card opens the external app directly. |

### Rejected alternatives

- **Hand-rolled `HttpURLConnection` + `BitmapFactory`** — matches iOS exactly, but re-fetches
  on recomposition and needs manual caching and cancellation. Coil is the idiomatic answer.
- **Server-side `isVisible` filter** — a Firestore `whereEqualTo` would break the `order`
  cursor unless a composite index exists. iOS filters client-side; the collection is 12 docs.
- **Loading all videos at once** — simpler, but diverges from iOS and degrades as the
  catalog grows. Pagination is already written on iOS and is cheap to mirror.

## Architecture

```
vn.hocdantranh.guzheng/
├── data/video/
│   ├── VideoItem.kt          data class + VideoSource / VideoCategory enums
│   ├── VideoRepository.kt    VideoRepository interface + FirestoreVideoRepository
│   └── VideoThumbnails.kt    URL resolution: YouTube id -> img.youtube.com, TikTok -> oembed
└── ui/video/
    ├── VideoViewModel.kt     StateFlow<VideoUiState>
    ├── VideoScreen.kt        header, chips, list, states, pull-to-refresh
    └── VideoCards.kt         FeaturedVideoCard, VideoCard, SkeletonCard, CategoryChip
```

### Model

```kotlin
enum class VideoSource { YOUTUBE, TIKTOK }

enum class VideoCategory(val key: String, @StringRes val labelRes: Int) {
    INTRO("intro", R.string.video_category_intro),
    SONGS("songs", R.string.video_category_songs),
    TIPS("tips", R.string.video_category_tips),
    TUTORIAL("tutorial", R.string.video_category_tutorial),
}

data class VideoItem(
    val id: String,
    val title: String,
    val url: String,
    val source: VideoSource,
    val category: VideoCategory,
    val order: Int,
)
```

`isVisible` is not carried on the model: the repository drops hidden documents while
mapping, so an invisible video never reaches the UI. iOS keeps the field and always
sets it to `true`, which is dead weight.

A document whose `source`, `category`, or `order` is missing or unrecognised is skipped,
mirroring iOS's `compactMap`. This keeps one malformed document from emptying the tab.

### UI state

```kotlin
data class VideoUiState(
    val videos: List<VideoItem> = emptyList(),
    val selectedCategory: VideoCategory? = null,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = true,
) {
    val filtered: List<VideoItem> =
        selectedCategory?.let { c -> videos.filter { it.category == c } } ?: videos

    /** Only categories actually present in the loaded data, in enum order. */
    val availableCategories: List<VideoCategory> =
        VideoCategory.entries.filter { c -> videos.any { it.category == c } }
}
```

`hasMore` is `items.count == pageSize` on the **raw page** before the `isVisible` filter,
matching iOS — otherwise a page of hidden videos would end pagination early.

### Thumbnails

`VideoThumbnails` resolves an item to an image URL, which Coil then loads:

- **YouTube** — extract the 11-character id from `?v=`, `youtu.be/`, or `/shorts/`, then
  `https://img.youtube.com/vi/<id>/mqdefault.jpg`. Extraction must stop at 11 characters so
  the `?si=...` suffix on the live short URLs is dropped.
- **TikTok** — `GET https://www.tiktok.com/oembed?url=<encoded>` and read `thumbnail_url`.
  Results are memoized per video id for the process lifetime so a scroll back up costs nothing.

A failed resolution leaves the card on its `woodSoft` placeholder rather than showing an
error — a missing thumbnail must never hide a playable video.

### Screen

Structure mirrors iOS top to bottom: `PaperBackground`, header
(`OverlineLabel("影片 · Video")` + serif title + tips line), a horizontally scrolling chip
row, then the list. First item renders as `FeaturedVideoCard` (180dp tall, gradient scrim,
centred play button, title and badges bottom-left); the rest render as `VideoCard`
(108×68dp thumbnail, title, category pill, chevron). Empty state is `InkSeal("琴")` plus
"Chưa có video nào 🎬". Loading state is three shimmering `SkeletonCard`s.

Pagination triggers when the last item scrolls into view, replacing iOS's 1px
`loadMoreTrigger` spacer with a `LazyListState` check.

## Localization

New `strings.xml` keys (vi default, en in `values-en`):

| Key | vi | en |
|---|---|---|
| `video_header_title` | `Bài hát ` | `Songs ` |
| `video_header_accent` | `Guzheng` | `Guzheng` |
| `video_header_tagline` | `💡 Tip & Trick hữu ích` | `💡 Tips & Tricks` |
| `video_category_all` | `Tất cả` | `All` |
| `video_category_intro` | `Giới thiệu` | `Intro` |
| `video_category_songs` | `Bài hát` | `Songs` |
| `video_category_tips` | `Tip & Trick` | `Tips & Tricks` |
| `video_category_tutorial` | `Hướng dẫn` | `Tutorial` |
| `video_empty` | `Chưa có video nào 🎬` | `No videos yet 🎬` |

Video **titles** stay Vietnamese — they are content, and Firestore holds one title per
document. This matches the Lessons decision.

## Testing

**Unit (JVM, no device)**

- `VideoUiState.filtered` — no category returns all; a category returns only its own
- `availableCategories` — returns only categories present; with the live mix returns exactly `[INTRO, SONGS, TUTORIAL]`
- YouTube id extraction — `?v=`, `youtu.be/<id>?si=...`, `/shorts/`, and a non-YouTube URL
- Thumbnail URL for a YouTube item
- Document mapping — a valid doc maps; a doc with unknown `category` or missing `order` is skipped; `isVisible: false` is dropped
- `hasMore` — true when a raw page fills `pageSize`, false when short
- `VideoViewModel` against a fake repository — initial load, `fetchMore` appends, refresh replaces

**Instrumented (device)**

- Chips render for present categories only, and selecting one filters the list
- Selecting the active chip clears the filter (iOS toggles it off)
- Empty state renders when the repository returns nothing
- Skeletons render while loading
- Tapping a card fires an `ACTION_VIEW` intent carrying that video's URL (Espresso intent stub — no external app launched)

## Verification

Build, install, and drive the tab on the OPPO CPH2363 over `adb`: confirm all 12 videos
load, the chip row shows exactly "Tất cả · Giới thiệu · Bài hát · Hướng dẫn", scrolling
past item 10 pages in the last 2, TikTok and YouTube thumbnails both render, and tapping a
card opens the right app. Compare against the iOS screen before calling it done.
