# Android Video Tab Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the Video tab's `PlaceholderScreen` with the full Firestore-backed video catalog — category chips, featured and standard cards, pagination, and hand-off to the TikTok/YouTube apps.

**Architecture:** Data layer under `data/video/` (model + pure field mapping + a `VideoRepository` interface with a Firestore implementation), UI under `ui/video/` (`VideoViewModel` exposing a single `StateFlow<VideoUiState>`, plus composables). The repository seam keeps every filter and pagination rule unit-testable on the JVM; only the Firestore wire-up and the composables need a device.

**Tech Stack:** Kotlin, Jetpack Compose (Material3), Firebase Firestore KTX, Coil 2.7.0, kotlinx-coroutines, JUnit4 + Robolectric, Compose UI test.

**Spec:** [docs/superpowers/specs/2026-08-23-android-video-tab-design.md](../specs/2026-08-23-android-video-tab-design.md)

## Global Constraints

- Package root: **`vn.hocdantranh.guzheng`**. Gradle JDK: Android Studio's bundled JBR 17 at `/Applications/Android Studio.app/Contents/jbr/Contents/Home`.
- All Android code lives under `android/`. **Do not modify anything under `HocDanTranh-Guzheng-DanThanh/`** (the iOS app).
- Vietnamese is the default locale (`values/`); English is `values-en/`. Chrome is localized; video **titles stay Vietnamese** (they are content, one per Firestore document).
- Page size is **10**, matching iOS. `hasMore` is computed from the **raw page count before** the `isVisible` filter.
- Category chips render **only categories present in the loaded data** — never a hardcoded list of four.
- YouTube id extraction must take **exactly 11 characters** so the live `youtu.be/<id>?si=...` suffix is dropped.
- Reuse existing components from `ui/components/VintageComponents.kt` (`PaperBackground`, `OverlineLabel`, `InkSeal`) and colors from `ui/theme/Color.kt`. Do not introduce new color literals.
- Run unit tests with `./gradlew :app:testDebugUnitTest`, device tests with `./gradlew :app:connectedDebugAndroidTest`.
- `connectedAndroidTest` **uninstalls the app when it finishes** — reinstall the debug APK before any manual device check.

---

### Task 1: Video model and Firestore field mapping

**Files:**
- Create: `android/app/src/main/java/vn/hocdantranh/guzheng/data/video/VideoItem.kt`
- Create: `android/app/src/main/res/values/strings.xml` (modify — append the Video block)
- Create: `android/app/src/main/res/values-en/strings.xml` (modify — append the Video block)
- Test: `android/app/src/test/java/vn/hocdantranh/guzheng/data/video/VideoItemTest.kt`

**Interfaces:**
- Produces: `VideoSource`, `VideoCategory`, `VideoItem`, and `videoFromFields(id: String, fields: Map<String, Any?>): VideoItem?` — Task 2 calls `videoFromFields` per document; Tasks 4–6 consume the model.

- [ ] **Step 1: Append the Video strings to `values/strings.xml`**

Insert before the closing `</resources>`:

```xml
    <!-- Video tab -->
    <string name="video_header_title">Bài hát </string>
    <string name="video_header_accent">Guzheng</string>
    <string name="video_header_tagline">💡 Tip &amp; Trick hữu ích</string>
    <string name="video_overline">影片 · Video</string>
    <string name="video_category_all">Tất cả</string>
    <string name="video_category_intro">Giới thiệu</string>
    <string name="video_category_songs">Bài hát</string>
    <string name="video_category_tips">Tip &amp; Trick</string>
    <string name="video_category_tutorial">Hướng dẫn</string>
    <string name="video_empty">Chưa có video nào 🎬</string>
    <string name="video_play">Phát video</string>
    <string name="video_open_failed">Không mở được video</string>
```

- [ ] **Step 2: Append the matching English strings to `values-en/strings.xml`**

Insert before the closing `</resources>`:

```xml
    <string name="video_header_title">Songs </string>
    <string name="video_header_accent">Guzheng</string>
    <string name="video_header_tagline">💡 Tips &amp; Tricks</string>
    <string name="video_overline">影片 · Video</string>
    <string name="video_category_all">All</string>
    <string name="video_category_intro">Intro</string>
    <string name="video_category_songs">Songs</string>
    <string name="video_category_tips">Tips &amp; Tricks</string>
    <string name="video_category_tutorial">Tutorial</string>
    <string name="video_empty">No videos yet 🎬</string>
    <string name="video_play">Play video</string>
    <string name="video_open_failed">Couldn\'t open the video</string>
```

- [ ] **Step 3: Write the failing test**

`android/app/src/test/java/vn/hocdantranh/guzheng/data/video/VideoItemTest.kt`:

```kotlin
package vn.hocdantranh.guzheng.data.video

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VideoItemTest {

    private fun fields(
        title: Any? = "Gương Sáng Bồ Đề",
        url: Any? = "https://www.tiktok.com/@danthanhdantranh/video/7596276865115966727",
        source: Any? = "tiktok",
        category: Any? = "songs",
        order: Any? = 9L,
        isVisible: Any? = true,
    ): Map<String, Any?> = buildMap {
        title?.let { put("title", it) }
        url?.let { put("url", it) }
        source?.let { put("source", it) }
        category?.let { put("category", it) }
        order?.let { put("order", it) }
        isVisible?.let { put("isVisible", it) }
    }

    @Test
    fun `maps a well formed document`() {
        val item = videoFromFields("abc123", fields())

        assertEquals(
            VideoItem(
                id = "abc123",
                title = "Gương Sáng Bồ Đề",
                url = "https://www.tiktok.com/@danthanhdantranh/video/7596276865115966727",
                source = VideoSource.TIKTOK,
                category = VideoCategory.SONGS,
                order = 9,
            ),
            item,
        )
    }

    @Test
    fun `treats a missing isVisible as visible`() {
        assertEquals(VideoCategory.SONGS, videoFromFields("id", fields(isVisible = null))?.category)
    }

    @Test
    fun `drops a hidden document`() {
        assertNull(videoFromFields("id", fields(isVisible = false)))
    }

    @Test
    fun `drops a document with an unknown category`() {
        assertNull(videoFromFields("id", fields(category = "not-a-category")))
    }

    @Test
    fun `drops a document with an unknown source`() {
        assertNull(videoFromFields("id", fields(source = "vimeo")))
    }

    @Test
    fun `drops a document missing its order`() {
        assertNull(videoFromFields("id", fields(order = null)))
    }

    @Test
    fun `drops a document missing its title or url`() {
        assertNull(videoFromFields("id", fields(title = null)))
        assertNull(videoFromFields("id", fields(url = null)))
    }

    @Test
    fun `accepts an order stored as a double`() {
        assertEquals(4, videoFromFields("id", fields(order = 4.0))?.order)
    }
}
```

- [ ] **Step 4: Run the test to verify it fails**

```bash
cd /Users/kelvin/Code/AppHocDanTranh/android
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew :app:testDebugUnitTest --tests "*VideoItemTest*"
```

Expected: FAIL — `Unresolved reference 'videoFromFields'`.

- [ ] **Step 5: Write the model**

`android/app/src/main/java/vn/hocdantranh/guzheng/data/video/VideoItem.kt`:

```kotlin
package vn.hocdantranh.guzheng.data.video

import androidx.annotation.StringRes
import vn.hocdantranh.guzheng.R

/** Where a video lives. Drives the platform badge and which thumbnail lookup runs. */
enum class VideoSource(val key: String) {
    YOUTUBE("youtube"),
    TIKTOK("tiktok");

    companion object {
        fun fromKey(key: String?): VideoSource? = entries.firstOrNull { it.key == key }
    }
}

/**
 * Mirrors the iOS `VideoCategory`. `TIPS` has no video in Firestore today, which is
 * exactly why the chip row is derived from the data rather than from these entries.
 */
enum class VideoCategory(val key: String, @StringRes val labelRes: Int) {
    INTRO("intro", R.string.video_category_intro),
    SONGS("songs", R.string.video_category_songs),
    TIPS("tips", R.string.video_category_tips),
    TUTORIAL("tutorial", R.string.video_category_tutorial);

    companion object {
        fun fromKey(key: String?): VideoCategory? = entries.firstOrNull { it.key == key }
    }
}

data class VideoItem(
    val id: String,
    val title: String,
    val url: String,
    val source: VideoSource,
    val category: VideoCategory,
    val order: Int,
)

/**
 * Maps one Firestore document's fields to a [VideoItem], or null if it should not render.
 *
 * Null rather than throwing, mirroring the iOS `compactMap`: one malformed document must
 * not empty the whole tab. `isVisible` is applied here and then dropped from the model —
 * a hidden video never reaches the UI, so nothing downstream needs to re-check it.
 */
fun videoFromFields(id: String, fields: Map<String, Any?>): VideoItem? {
    val isVisible = fields["isVisible"] as? Boolean ?: true
    if (!isVisible) return null

    val title = fields["title"] as? String ?: return null
    val url = fields["url"] as? String ?: return null
    val source = VideoSource.fromKey(fields["source"] as? String) ?: return null
    val category = VideoCategory.fromKey(fields["category"] as? String) ?: return null
    val order = (fields["order"] as? Number)?.toInt() ?: return null

    return VideoItem(
        id = id,
        title = title,
        url = url,
        source = source,
        category = category,
        order = order,
    )
}
```

- [ ] **Step 6: Run the test to verify it passes**

```bash
./gradlew :app:testDebugUnitTest --tests "*VideoItemTest*"
```

Expected: PASS, 8 tests.

- [ ] **Step 7: Commit**

```bash
cd /Users/kelvin/Code/AppHocDanTranh
git add android/app/src/main/java/vn/hocdantranh/guzheng/data/video/VideoItem.kt \
        android/app/src/test/java/vn/hocdantranh/guzheng/data/video/VideoItemTest.kt \
        android/app/src/main/res/values/strings.xml \
        android/app/src/main/res/values-en/strings.xml
git commit -m "feat(android): add the video model and Firestore field mapping"
```

---

### Task 2: Thumbnail URL resolution

**Files:**
- Create: `android/app/src/main/java/vn/hocdantranh/guzheng/data/video/VideoThumbnails.kt`
- Test: `android/app/src/test/java/vn/hocdantranh/guzheng/data/video/VideoThumbnailsTest.kt`

**Interfaces:**
- Consumes: `VideoItem`, `VideoSource` from Task 1.
- Produces: `extractYouTubeId(url: String): String?`, `youTubeThumbnailUrl(id: String): String`, and `interface ThumbnailResolver { suspend fun resolve(item: VideoItem): String? }` with `DefaultThumbnailResolver`. Task 5's cards call `resolve`.

- [ ] **Step 1: Write the failing test**

`android/app/src/test/java/vn/hocdantranh/guzheng/data/video/VideoThumbnailsTest.kt`:

```kotlin
package vn.hocdantranh.guzheng.data.video

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VideoThumbnailsTest {

    @Test
    fun `extracts the id from a standard watch url`() {
        assertEquals("dQw4w9WgXcQ", extractYouTubeId("https://www.youtube.com/watch?v=dQw4w9WgXcQ"))
    }

    @Test
    fun `extracts the id from a short url and drops the si suffix`() {
        assertEquals(
            "VGqJwQPjA3g",
            extractYouTubeId("https://youtu.be/VGqJwQPjA3g?si=bUa_4EuFM-eeDlkM"),
        )
    }

    @Test
    fun `extracts the id from a shorts url`() {
        assertEquals("abcdefghijk", extractYouTubeId("https://www.youtube.com/shorts/abcdefghijk"))
    }

    @Test
    fun `returns null for a url with no id`() {
        assertNull(extractYouTubeId("https://www.tiktok.com/@danthanhdantranh/video/7596276865115966727"))
    }

    @Test
    fun `returns null when the id is too short`() {
        assertNull(extractYouTubeId("https://youtu.be/tooshort"))
    }

    @Test
    fun `builds the youtube thumbnail url`() {
        assertEquals(
            "https://img.youtube.com/vi/VGqJwQPjA3g/mqdefault.jpg",
            youTubeThumbnailUrl("VGqJwQPjA3g"),
        )
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

```bash
./gradlew :app:testDebugUnitTest --tests "*VideoThumbnailsTest*"
```

Expected: FAIL — `Unresolved reference 'extractYouTubeId'`.

- [ ] **Step 3: Write the implementation**

`android/app/src/main/java/vn/hocdantranh/guzheng/data/video/VideoThumbnails.kt`:

```kotlin
package vn.hocdantranh.guzheng.data.video

import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

private const val YOUTUBE_ID_LENGTH = 11

/**
 * Pulls the 11-character video id out of a YouTube URL.
 *
 * Taking exactly [YOUTUBE_ID_LENGTH] characters is what strips the `?si=...` suffix the
 * live Firestore documents carry on their `youtu.be` short links.
 */
fun extractYouTubeId(url: String): String? {
    for (marker in listOf("v=", "youtu.be/", "/shorts/")) {
        val start = url.indexOf(marker)
        if (start == -1) continue
        val id = url.substring(start + marker.length).take(YOUTUBE_ID_LENGTH)
        if (id.length == YOUTUBE_ID_LENGTH) return id
    }
    return null
}

fun youTubeThumbnailUrl(id: String): String = "https://img.youtube.com/vi/$id/mqdefault.jpg"

/** Resolves a video to an image URL that Coil can load. */
interface ThumbnailResolver {
    suspend fun resolve(item: VideoItem): String?
}

/**
 * YouTube thumbnails are derived from the URL; TikTok needs an oembed round-trip, so its
 * results are memoized for the process lifetime — ten of the twelve live videos are TikTok
 * and scrolling back up must not re-hit the network.
 */
object DefaultThumbnailResolver : ThumbnailResolver {

    private val tikTokCache = ConcurrentHashMap<String, String>()

    override suspend fun resolve(item: VideoItem): String? = when (item.source) {
        VideoSource.YOUTUBE -> extractYouTubeId(item.url)?.let(::youTubeThumbnailUrl)
        VideoSource.TIKTOK -> tikTokThumbnail(item)
    }

    private suspend fun tikTokThumbnail(item: VideoItem): String? {
        tikTokCache[item.id]?.let { return it }
        return withContext(Dispatchers.IO) {
            runCatching {
                val encoded = URLEncoder.encode(item.url, "UTF-8")
                val body = URL("https://www.tiktok.com/oembed?url=$encoded").readText()
                JSONObject(body).optString("thumbnail_url").takeIf { it.isNotEmpty() }
            }.getOrNull()?.also { tikTokCache[item.id] = it }
        }
    }
}
```

- [ ] **Step 4: Run the test to verify it passes**

```bash
./gradlew :app:testDebugUnitTest --tests "*VideoThumbnailsTest*"
```

Expected: PASS, 6 tests.

- [ ] **Step 5: Commit**

```bash
cd /Users/kelvin/Code/AppHocDanTranh
git add android/app/src/main/java/vn/hocdantranh/guzheng/data/video/VideoThumbnails.kt \
        android/app/src/test/java/vn/hocdantranh/guzheng/data/video/VideoThumbnailsTest.kt
git commit -m "feat(android): resolve YouTube and TikTok thumbnail urls"
```

---

### Task 3: Paginated Firestore repository

**Files:**
- Create: `android/app/src/main/java/vn/hocdantranh/guzheng/data/video/VideoRepository.kt`
- Test: `android/app/src/test/java/vn/hocdantranh/guzheng/data/video/VideoPageTest.kt`

**Interfaces:**
- Consumes: `videoFromFields`, `VideoItem` from Task 1.
- Produces: `data class VideoPage(val items: List<VideoItem>, val cursor: Any?, val hasMore: Boolean)`, `VIDEO_PAGE_SIZE`, `interface VideoRepository { suspend fun loadPage(cursor: Any?): VideoPage }`, and `FirestoreVideoRepository`. Task 4's ViewModel depends on the interface only.

- [ ] **Step 1: Write the failing test**

`android/app/src/test/java/vn/hocdantranh/guzheng/data/video/VideoPageTest.kt`:

```kotlin
package vn.hocdantranh.guzheng.data.video

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoPageTest {

    private fun doc(order: Int, visible: Boolean = true) =
        "id$order" to mapOf<String, Any?>(
            "title" to "Video $order",
            "url" to "https://youtu.be/VGqJwQPjA3g?si=x",
            "source" to "youtube",
            "category" to "songs",
            "order" to order,
            "isVisible" to visible,
        )

    @Test
    fun `a full raw page means there may be more`() {
        val raw = (1..VIDEO_PAGE_SIZE).map { doc(it) }

        val page = videoPageFrom(raw, cursor = "last")

        assertEquals(VIDEO_PAGE_SIZE, page.items.size)
        assertTrue(page.hasMore)
    }

    @Test
    fun `a short raw page means the end`() {
        val page = videoPageFrom((1..2).map { doc(it) }, cursor = "last")

        assertEquals(2, page.items.size)
        assertFalse(page.hasMore)
    }

    @Test
    fun `hasMore follows the raw page, not the visible count`() {
        // A full page where everything is hidden still means more pages may follow.
        val raw = (1..VIDEO_PAGE_SIZE).map { doc(it, visible = false) }

        val page = videoPageFrom(raw, cursor = "last")

        assertTrue("all filtered out, but the raw page was full", page.hasMore)
        assertEquals(0, page.items.size)
    }

    @Test
    fun `malformed documents are skipped without dropping the rest`() {
        val raw = listOf(
            doc(1),
            "bad" to mapOf<String, Any?>("title" to "no other fields"),
            doc(2),
        )

        val page = videoPageFrom(raw, cursor = null)

        assertEquals(listOf("Video 1", "Video 2"), page.items.map { it.title })
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

```bash
./gradlew :app:testDebugUnitTest --tests "*VideoPageTest*"
```

Expected: FAIL — `Unresolved reference 'videoPageFrom'`.

- [ ] **Step 3: Write the repository**

`android/app/src/main/java/vn/hocdantranh/guzheng/data/video/VideoRepository.kt`:

```kotlin
package vn.hocdantranh.guzheng.data.video

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

/** Matches the iOS page size so both platforms paginate identically. */
const val VIDEO_PAGE_SIZE = 10

/**
 * One page of videos.
 *
 * [cursor] is opaque to callers — the Firestore implementation stores a `DocumentSnapshot`
 * in it and hands it back on the next call, which keeps the ViewModel free of Firestore types.
 */
data class VideoPage(
    val items: List<VideoItem>,
    val cursor: Any?,
    val hasMore: Boolean,
)

/**
 * Builds a [VideoPage] from raw `id to fields` pairs.
 *
 * [hasMore] counts the **raw** documents, before the `isVisible` filter: a full page of
 * hidden videos still means another page may follow, and testing the filtered count there
 * would end pagination early. Extracted from the Firestore call so this rule is testable
 * without a device.
 */
fun videoPageFrom(raw: List<Pair<String, Map<String, Any?>>>, cursor: Any?): VideoPage =
    VideoPage(
        items = raw.mapNotNull { (id, fields) -> videoFromFields(id, fields) },
        cursor = cursor,
        hasMore = raw.size == VIDEO_PAGE_SIZE,
    )

interface VideoRepository {
    /** Loads the page after [cursor]; pass null for the first page. */
    suspend fun loadPage(cursor: Any?): VideoPage
}

class FirestoreVideoRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) : VideoRepository {

    override suspend fun loadPage(cursor: Any?): VideoPage {
        var query: Query = firestore.collection("videos")
            .orderBy("order")
            .limit(VIDEO_PAGE_SIZE.toLong())

        if (cursor is DocumentSnapshot) query = query.startAfter(cursor)

        val snapshot = query.get().await()
        val raw = snapshot.documents.map { it.id to it.data.orEmpty() }
        return videoPageFrom(raw, cursor = snapshot.documents.lastOrNull())
    }
}
```

- [ ] **Step 4: Run the test to verify it passes**

```bash
./gradlew :app:testDebugUnitTest --tests "*VideoPageTest*"
```

Expected: PASS, 4 tests.

- [ ] **Step 5: Commit**

```bash
cd /Users/kelvin/Code/AppHocDanTranh
git add android/app/src/main/java/vn/hocdantranh/guzheng/data/video/VideoRepository.kt \
        android/app/src/test/java/vn/hocdantranh/guzheng/data/video/VideoPageTest.kt
git commit -m "feat(android): add the paginated Firestore video repository"
```

---

### Task 4: Video ViewModel and UI state

**Files:**
- Create: `android/app/src/main/java/vn/hocdantranh/guzheng/ui/video/VideoViewModel.kt`
- Test: `android/app/src/test/java/vn/hocdantranh/guzheng/ui/video/VideoUiStateTest.kt`
- Test: `android/app/src/test/java/vn/hocdantranh/guzheng/ui/video/VideoViewModelTest.kt`

**Interfaces:**
- Consumes: `VideoRepository`, `VideoPage`, `VideoItem`, `VideoCategory` from Tasks 1 and 3.
- Produces: `VideoUiState(videos, selectedCategory, isLoading, isLoadingMore, hasMore)` with derived `filtered` and `availableCategories`; `VideoViewModel` with `uiState: StateFlow<VideoUiState>`, `refresh()`, `loadMore()`, `selectCategory(category: VideoCategory?)`, and `companion object { fun factory(): ViewModelProvider.Factory }`. Task 6's screen consumes these.

- [ ] **Step 1: Write the failing state test**

`android/app/src/test/java/vn/hocdantranh/guzheng/ui/video/VideoUiStateTest.kt`:

```kotlin
package vn.hocdantranh.guzheng.ui.video

import org.junit.Assert.assertEquals
import org.junit.Test
import vn.hocdantranh.guzheng.data.video.VideoCategory
import vn.hocdantranh.guzheng.data.video.VideoItem
import vn.hocdantranh.guzheng.data.video.VideoSource

class VideoUiStateTest {

    private fun item(id: String, category: VideoCategory) = VideoItem(
        id = id,
        title = "Video $id",
        url = "https://youtu.be/VGqJwQPjA3g?si=x",
        source = VideoSource.YOUTUBE,
        category = category,
        order = 0,
    )

    private val videos = listOf(
        item("1", VideoCategory.INTRO),
        item("2", VideoCategory.SONGS),
        item("3", VideoCategory.SONGS),
        item("4", VideoCategory.TUTORIAL),
    )

    @Test
    fun `no selection returns every video`() {
        assertEquals(4, VideoUiState(videos = videos).filtered.size)
    }

    @Test
    fun `a selection returns only that category`() {
        val state = VideoUiState(videos = videos, selectedCategory = VideoCategory.SONGS)

        assertEquals(listOf("2", "3"), state.filtered.map { it.id })
    }

    @Test
    fun `available categories reflect the data, not the enum`() {
        // The live collection has no `tips` video, so no TIPS chip may appear.
        assertEquals(
            listOf(VideoCategory.INTRO, VideoCategory.SONGS, VideoCategory.TUTORIAL),
            VideoUiState(videos = videos).availableCategories,
        )
    }

    @Test
    fun `available categories are empty before anything loads`() {
        assertEquals(emptyList<VideoCategory>(), VideoUiState().availableCategories)
    }
}
```

- [ ] **Step 2: Write the failing ViewModel test**

`android/app/src/test/java/vn/hocdantranh/guzheng/ui/video/VideoViewModelTest.kt`:

```kotlin
package vn.hocdantranh.guzheng.ui.video

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import vn.hocdantranh.guzheng.data.video.VIDEO_PAGE_SIZE
import vn.hocdantranh.guzheng.data.video.VideoCategory
import vn.hocdantranh.guzheng.data.video.VideoItem
import vn.hocdantranh.guzheng.data.video.VideoPage
import vn.hocdantranh.guzheng.data.video.VideoRepository
import vn.hocdantranh.guzheng.data.video.VideoSource

@OptIn(ExperimentalCoroutinesApi::class)
class VideoViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    private fun item(id: String) = VideoItem(
        id = id,
        title = "Video $id",
        url = "https://youtu.be/VGqJwQPjA3g?si=x",
        source = VideoSource.YOUTUBE,
        category = VideoCategory.SONGS,
        order = id.toInt(),
    )

    /** Serves [pages] in order, one per loadPage call. */
    private class FakeRepository(private val pages: List<VideoPage>) : VideoRepository {
        var calls = 0
            private set

        override suspend fun loadPage(cursor: Any?): VideoPage = pages[calls++]
    }

    private fun fullPage(range: IntRange) = VideoPage(
        items = range.map { VideoItem("$it", "Video $it", "https://youtu.be/VGqJwQPjA3g?si=x", VideoSource.YOUTUBE, VideoCategory.SONGS, it) },
        cursor = "c",
        hasMore = true,
    )

    @Test
    fun `loads the first page on creation`() = runTest {
        val repo = FakeRepository(listOf(fullPage(1..VIDEO_PAGE_SIZE)))

        val vm = VideoViewModel(repo)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(VIDEO_PAGE_SIZE, vm.uiState.value.videos.size)
        assertFalse(vm.uiState.value.isLoading)
        assertTrue(vm.uiState.value.hasMore)
    }

    @Test
    fun `loadMore appends the next page`() = runTest {
        val repo = FakeRepository(
            listOf(
                fullPage(1..VIDEO_PAGE_SIZE),
                VideoPage(items = listOf(item("11"), item("12")), cursor = null, hasMore = false),
            )
        )
        val vm = VideoViewModel(repo)
        dispatcher.scheduler.advanceUntilIdle()

        vm.loadMore()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(12, vm.uiState.value.videos.size)
        assertFalse("the short second page ends pagination", vm.uiState.value.hasMore)
    }

    @Test
    fun `loadMore does nothing once there is no more`() = runTest {
        val repo = FakeRepository(
            listOf(VideoPage(items = listOf(item("1")), cursor = null, hasMore = false))
        )
        val vm = VideoViewModel(repo)
        dispatcher.scheduler.advanceUntilIdle()

        vm.loadMore()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("no second request", 1, repo.calls)
    }

    @Test
    fun `refresh replaces rather than appends`() = runTest {
        val repo = FakeRepository(
            listOf(
                fullPage(1..VIDEO_PAGE_SIZE),
                VideoPage(items = listOf(item("99")), cursor = null, hasMore = false),
            )
        )
        val vm = VideoViewModel(repo)
        dispatcher.scheduler.advanceUntilIdle()

        vm.refresh()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf("99"), vm.uiState.value.videos.map { it.id })
    }

    @Test
    fun `selecting a category updates the state`() = runTest {
        val repo = FakeRepository(listOf(fullPage(1..VIDEO_PAGE_SIZE)))
        val vm = VideoViewModel(repo)
        dispatcher.scheduler.advanceUntilIdle()

        vm.selectCategory(VideoCategory.SONGS)

        assertEquals(VideoCategory.SONGS, vm.uiState.value.selectedCategory)
    }
}
```

- [ ] **Step 3: Run both tests to verify they fail**

```bash
./gradlew :app:testDebugUnitTest --tests "*VideoUiStateTest*" --tests "*VideoViewModelTest*"
```

Expected: FAIL — `Unresolved reference 'VideoUiState'` / `'VideoViewModel'`.

- [ ] **Step 4: Write the ViewModel**

`android/app/src/main/java/vn/hocdantranh/guzheng/ui/video/VideoViewModel.kt`:

```kotlin
package vn.hocdantranh.guzheng.ui.video

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.hocdantranh.guzheng.data.video.FirestoreVideoRepository
import vn.hocdantranh.guzheng.data.video.VideoCategory
import vn.hocdantranh.guzheng.data.video.VideoItem
import vn.hocdantranh.guzheng.data.video.VideoRepository

/**
 * Everything the Video tab renders. The derived values live here rather than in the
 * composables so filtering and chip selection can be tested without a device.
 */
data class VideoUiState(
    val videos: List<VideoItem> = emptyList(),
    val selectedCategory: VideoCategory? = null,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = true,
) {
    val filtered: List<VideoItem>
        get() = selectedCategory?.let { c -> videos.filter { it.category == c } } ?: videos

    /**
     * Only categories with at least one loaded video, in enum order. Derived from the data
     * because the live collection has no `tips` video — a hardcoded list would show a chip
     * that filters to nothing.
     */
    val availableCategories: List<VideoCategory>
        get() = VideoCategory.entries.filter { c -> videos.any { it.category == c } }
}

class VideoViewModel(
    private val repository: VideoRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(VideoUiState(isLoading = true))
    val uiState: StateFlow<VideoUiState> = _uiState.asStateFlow()

    private var cursor: Any? = null

    init {
        refresh()
    }

    /** Reloads from the first page, replacing what is on screen. Also backs pull-to-refresh. */
    fun refresh() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val page = runCatching { repository.loadPage(null) }.getOrNull()
            cursor = page?.cursor
            _uiState.update {
                it.copy(
                    videos = page?.items ?: it.videos,
                    isLoading = false,
                    hasMore = page?.hasMore ?: false,
                )
            }
        }
    }

    /** Appends the next page. Ignored while a load is in flight or the catalog is exhausted. */
    fun loadMore() {
        val state = _uiState.value
        if (state.isLoading || state.isLoadingMore || !state.hasMore) return

        _uiState.update { it.copy(isLoadingMore = true) }
        viewModelScope.launch {
            val page = runCatching { repository.loadPage(cursor) }.getOrNull()
            cursor = page?.cursor
            _uiState.update {
                it.copy(
                    videos = it.videos + (page?.items ?: emptyList()),
                    isLoadingMore = false,
                    hasMore = page?.hasMore ?: false,
                )
            }
        }
    }

    /** Null clears the filter. Tapping the active chip clears it too, matching iOS. */
    fun selectCategory(category: VideoCategory?) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    companion object {
        fun factory(): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                VideoViewModel(FirestoreVideoRepository()) as T
        }
    }
}
```

- [ ] **Step 5: Run both tests to verify they pass**

```bash
./gradlew :app:testDebugUnitTest --tests "*VideoUiStateTest*" --tests "*VideoViewModelTest*"
```

Expected: PASS, 9 tests.

- [ ] **Step 6: Commit**

```bash
cd /Users/kelvin/Code/AppHocDanTranh
git add android/app/src/main/java/vn/hocdantranh/guzheng/ui/video/VideoViewModel.kt \
        android/app/src/test/java/vn/hocdantranh/guzheng/ui/video/
git commit -m "feat(android): add the video view model with filtering and pagination"
```

---

### Task 5: Video cards and category chips

**Files:**
- Modify: `android/gradle/libs.versions.toml`
- Modify: `android/app/build.gradle.kts`
- Create: `android/app/src/main/java/vn/hocdantranh/guzheng/ui/video/VideoCards.kt`

**Interfaces:**
- Consumes: `VideoItem`, `VideoSource`, `ThumbnailResolver`, `DefaultThumbnailResolver` from Tasks 1–2.
- Produces: `@Composable FeaturedVideoCard(item, onClick)`, `VideoCard(item, onClick)`, `SkeletonCard()`, `CategoryChip(label, isSelected, onClick)`. Task 6 composes these.

- [ ] **Step 1: Add Coil to the version catalog**

In `android/gradle/libs.versions.toml`, under `[versions]` add:

```toml
coil = "2.7.0"
```

under `[libraries]` add:

```toml
coil-compose = { group = "io.coil-kt", name = "coil-compose", version.ref = "coil" }
```

- [ ] **Step 2: Add the dependency**

In `android/app/build.gradle.kts`, in `dependencies`, after `implementation(libs.kotlinx.coroutines.play.services)`:

```kotlin
    implementation(libs.coil.compose)
```

- [ ] **Step 3: Write the cards**

`android/app/src/main/java/vn/hocdantranh/guzheng/ui/video/VideoCards.kt`:

```kotlin
package vn.hocdantranh.guzheng.ui.video

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import vn.hocdantranh.guzheng.R
import vn.hocdantranh.guzheng.data.video.DefaultThumbnailResolver
import vn.hocdantranh.guzheng.data.video.ThumbnailResolver
import vn.hocdantranh.guzheng.data.video.VideoItem
import vn.hocdantranh.guzheng.data.video.VideoSource
import vn.hocdantranh.guzheng.ui.theme.VintageColors

/** TikTok resolution is a suspending oembed call, so the URL arrives after composition. */
@Composable
private fun rememberThumbnailUrl(
    item: VideoItem,
    resolver: ThumbnailResolver = DefaultThumbnailResolver,
): String? {
    var url by remember(item.id) { mutableStateOf<String?>(null) }
    LaunchedEffect(item.id) { url = resolver.resolve(item) }
    return url
}

private val VideoItem.platformLabel: String
    get() = if (source == VideoSource.TIKTOK) "TikTok" else "YouTube"

@Composable
private fun VideoItem.platformColor() =
    if (source == VideoSource.TIKTOK) VintageColors.inkDark else VintageColors.sealRed

/** The first card in the list: a large 180dp tile with the title over a gradient scrim. */
@Composable
fun FeaturedVideoCard(item: VideoItem, onClick: () -> Unit) {
    val playLabel = stringResource(R.string.video_play)

    Box(
        Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(VintageColors.woodSoft.copy(alpha = 0.4f))
            .border(1.dp, VintageColors.inkMuted.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .semantics { contentDescription = "$playLabel: ${item.title}" }
    ) {
        AsyncImage(
            model = rememberThumbnailUrl(item),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )

        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.5f to androidx.compose.ui.graphics.Color.Transparent,
                        1f to VintageColors.inkDark.copy(alpha = 0.7f),
                    )
                )
        )

        Box(
            Modifier
                .align(Alignment.Center)
                .size(52.dp)
                .clip(CircleShape)
                .background(VintageColors.paper.copy(alpha = 0.92f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = VintageColors.inkDark,
                modifier = Modifier.size(24.dp),
            )
        }

        Column(
            Modifier
                .align(Alignment.BottomStart)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = item.title,
                color = VintageColors.paper,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Serif,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Badge(
                    text = stringResource(item.category.labelRes).uppercase(),
                    background = VintageColors.paper.copy(alpha = 0.2f),
                    color = VintageColors.paper.copy(alpha = 0.85f),
                )
                Badge(
                    text = item.platformLabel,
                    background = item.platformColor().copy(alpha = 0.85f),
                    color = VintageColors.paper,
                )
            }
        }
    }
}

@Composable
private fun Badge(
    text: String,
    background: androidx.compose.ui.graphics.Color,
    color: androidx.compose.ui.graphics.Color,
) {
    Text(
        text = text,
        color = color,
        fontSize = 9.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.2.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(background)
            .padding(horizontal = 7.dp, vertical = 2.dp),
    )
}

/** Every card after the first: a 108x68 thumbnail beside the title and category pill. */
@Composable
fun VideoCard(item: VideoItem, onClick: () -> Unit) {
    val playLabel = stringResource(R.string.video_play)

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(VintageColors.paperWarm.copy(alpha = 0.5f))
            .border(1.dp, VintageColors.inkMuted.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .semantics { contentDescription = "$playLabel: ${item.title}" }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            Modifier
                .width(108.dp)
                .height(68.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(VintageColors.woodSoft.copy(alpha = 0.5f))
        ) {
            AsyncImage(
                model = rememberThumbnailUrl(item),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                Modifier
                    .align(Alignment.Center)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(VintageColors.inkDark.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = VintageColors.paper,
                    modifier = Modifier.size(14.dp),
                )
            }
            Text(
                text = item.platformLabel,
                color = VintageColors.paper,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(item.platformColor().copy(alpha = 0.85f))
                    .padding(horizontal = 5.dp, vertical = 2.dp),
            )
        }

        Column(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = item.title,
                color = VintageColors.inkDark,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Serif,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Badge(
                text = stringResource(item.category.labelRes).uppercase(),
                background = VintageColors.sageDark.copy(alpha = 0.1f),
                color = VintageColors.sageDark,
            )
        }

        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = VintageColors.inkSoft.copy(alpha = 0.4f),
            modifier = Modifier.size(18.dp),
        )
    }
}

/** Shown while the first page loads, so the tab never flashes an empty state. */
@Composable
fun SkeletonCard() {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "shimmer",
    )

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(VintageColors.paperWarm.copy(alpha = 0.5f))
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            Modifier
                .width(108.dp)
                .height(68.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(VintageColors.woodSoft.copy(alpha = alpha))
        )
        Column(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(VintageColors.woodSoft.copy(alpha = alpha))
            )
            Box(
                Modifier
                    .width(80.dp)
                    .height(10.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(VintageColors.woodSoft.copy(alpha = alpha))
            )
        }
    }
}

@Composable
fun CategoryChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Text(
        text = label,
        color = if (isSelected) VintageColors.paper else VintageColors.inkSoft,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (isSelected) VintageColors.sageDark else VintageColors.paperWarm.copy(alpha = 0.6f))
            .border(
                1.dp,
                if (isSelected) androidx.compose.ui.graphics.Color.Transparent
                else VintageColors.inkMuted.copy(alpha = 0.25f),
                RoundedCornerShape(50),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
    )
}
```

- [ ] **Step 4: Verify it compiles**

```bash
./gradlew :app:compileDebugKotlin
```

Expected: BUILD SUCCESSFUL. If `VintageColors` members differ, check `ui/theme/Color.kt` — do not invent new colors.

- [ ] **Step 5: Commit**

```bash
cd /Users/kelvin/Code/AppHocDanTranh
git add android/gradle/libs.versions.toml android/app/build.gradle.kts \
        android/app/src/main/java/vn/hocdantranh/guzheng/ui/video/VideoCards.kt
git commit -m "feat(android): add vintage video cards, chips and skeletons"
```

---

### Task 6: Video screen wired into the tab

**Files:**
- Create: `android/app/src/main/java/vn/hocdantranh/guzheng/ui/video/VideoScreen.kt`
- Modify: `android/app/src/main/java/vn/hocdantranh/guzheng/ui/MainScaffold.kt`
- Test: `android/app/src/androidTest/java/vn/hocdantranh/guzheng/ui/video/VideoScreenTest.kt`

**Interfaces:**
- Consumes: `VideoUiState`, `VideoViewModel` (Task 4), the cards (Task 5).
- Produces: `@Composable VideoScreen(state, onCategorySelected, onVideoClick, onLoadMore)` (stateless, testable) and `@Composable VideoRoute()` (binds the ViewModel and the intent). `MainScaffold` renders `VideoRoute`.

- [ ] **Step 1: Write the failing UI test**

`android/app/src/androidTest/java/vn/hocdantranh/guzheng/ui/video/VideoScreenTest.kt`:

```kotlin
package vn.hocdantranh.guzheng.ui.video

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import vn.hocdantranh.guzheng.data.video.VideoCategory
import vn.hocdantranh.guzheng.data.video.VideoItem
import vn.hocdantranh.guzheng.data.video.VideoSource

class VideoScreenTest {

    @get:Rule val compose = createComposeRule()

    private fun item(id: String, title: String, category: VideoCategory) = VideoItem(
        id = id,
        title = title,
        url = "https://youtu.be/VGqJwQPjA3g?si=x",
        source = VideoSource.YOUTUBE,
        category = category,
        order = id.toInt(),
    )

    private val videos = listOf(
        item("1", "Giới thiệu Đàn Tranh", VideoCategory.INTRO),
        item("2", "Gương Sáng Bồ Đề", VideoCategory.SONGS),
        item("3", "Hướng dẫn thay dây", VideoCategory.TUTORIAL),
    )

    @Test
    fun rendersEveryVideo() {
        compose.setContent {
            VideoScreen(VideoUiState(videos = videos), {}, {}, {})
        }

        compose.onNodeWithText("Gương Sáng Bồ Đề").assertIsDisplayed()
        compose.onNodeWithText("Hướng dẫn thay dây").assertIsDisplayed()
    }

    @Test
    fun showsOnlyChipsForCategoriesPresentInTheData() {
        compose.setContent {
            VideoScreen(VideoUiState(videos = videos), {}, {}, {})
        }

        // No `tips` video exists, so no Tip & Trick chip may render.
        compose.onNodeWithText("Tất cả").assertIsDisplayed()
        compose.onNodeWithText("Giới thiệu").assertIsDisplayed()
        compose.onNodeWithText("Tip & Trick").assertDoesNotExist()
    }

    @Test
    fun tappingAChipReportsTheCategory() {
        var selected: VideoCategory? = null
        compose.setContent {
            VideoScreen(VideoUiState(videos = videos), { selected = it }, {}, {})
        }

        compose.onNodeWithText("Bài hát").performClick()

        assertEquals(VideoCategory.SONGS, selected)
    }

    @Test
    fun tappingACardReportsTheVideo() {
        var clicked: VideoItem? = null
        compose.setContent {
            VideoScreen(VideoUiState(videos = videos), {}, { clicked = it }, {})
        }

        compose.onNodeWithText("Gương Sáng Bồ Đề").performClick()

        assertEquals("2", clicked?.id)
    }

    @Test
    fun showsTheEmptyState() {
        compose.setContent {
            VideoScreen(VideoUiState(videos = emptyList(), isLoading = false), {}, {}, {})
        }

        compose.onNodeWithText("Chưa có video nào 🎬").assertIsDisplayed()
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

```bash
./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=vn.hocdantranh.guzheng.ui.video.VideoScreenTest
```

Expected: FAIL to compile — `Unresolved reference 'VideoScreen'`.

- [ ] **Step 3: Write the screen**

`android/app/src/main/java/vn/hocdantranh/guzheng/ui/video/VideoScreen.kt`:

```kotlin
package vn.hocdantranh.guzheng.ui.video

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
import vn.hocdantranh.guzheng.R
import vn.hocdantranh.guzheng.data.video.VideoCategory
import vn.hocdantranh.guzheng.data.video.VideoItem
import vn.hocdantranh.guzheng.ui.components.InkSeal
import vn.hocdantranh.guzheng.ui.components.OverlineLabel
import vn.hocdantranh.guzheng.ui.components.PaperBackground
import vn.hocdantranh.guzheng.ui.theme.VintageColors

/** Binds the ViewModel and hands a tapped video to whichever app owns it. */
@Composable
fun VideoRoute() {
    val context = LocalContext.current
    val viewModel: VideoViewModel = viewModel(factory = VideoViewModel.factory())
    val state by viewModel.uiState.collectAsState()
    val openFailed = stringResource(R.string.video_open_failed)

    VideoScreen(
        state = state,
        onCategorySelected = viewModel::selectCategory,
        onVideoClick = { item ->
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(item.url)))
            } catch (e: ActivityNotFoundException) {
                // No browser and no platform app — tell the user rather than dying silently.
                Toast.makeText(context, openFailed, Toast.LENGTH_SHORT).show()
            }
        },
        onLoadMore = viewModel::loadMore,
    )
}

@Composable
fun VideoScreen(
    state: VideoUiState,
    onCategorySelected: (VideoCategory?) -> Unit,
    onVideoClick: (VideoItem) -> Unit,
    onLoadMore: () -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        PaperBackground()

        Column(Modifier.fillMaxSize()) {
            Header()

            if (state.availableCategories.isNotEmpty()) {
                Chips(state, onCategorySelected)
            }

            Content(state, onVideoClick, onLoadMore)
        }
    }
}

@Composable
private fun Header() {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(top = 56.dp, bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        OverlineLabel(text = stringResource(R.string.video_overline))
        Row {
            Text(
                text = stringResource(R.string.video_header_title),
                color = VintageColors.inkDark,
                fontSize = 32.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Serif,
            )
            Text(
                text = stringResource(R.string.video_header_accent),
                color = VintageColors.sageDark,
                fontSize = 32.sp,
                fontWeight = FontWeight.Light,
                fontStyle = FontStyle.Italic,
                fontFamily = FontFamily.Serif,
            )
        }
        Text(
            text = stringResource(R.string.video_header_tagline),
            color = VintageColors.inkMuted,
            fontSize = 15.sp,
            fontStyle = FontStyle.Italic,
            fontFamily = FontFamily.Serif,
        )
    }
}

@Composable
private fun Chips(state: VideoUiState, onCategorySelected: (VideoCategory?) -> Unit) {
    LazyRow(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(bottom = 12.dp),
    ) {
        item {
            CategoryChip(
                label = stringResource(R.string.video_category_all),
                isSelected = state.selectedCategory == null,
                onClick = { onCategorySelected(null) },
            )
        }
        items(state.availableCategories) { category ->
            CategoryChip(
                label = stringResource(category.labelRes),
                isSelected = state.selectedCategory == category,
                // Tapping the active chip clears the filter, matching iOS.
                onClick = {
                    onCategorySelected(if (state.selectedCategory == category) null else category)
                },
            )
        }
    }
}

@Composable
private fun Content(
    state: VideoUiState,
    onVideoClick: (VideoItem) -> Unit,
    onLoadMore: () -> Unit,
) {
    val listState = rememberLazyListState()

    // Page in as the last card comes into view, replacing the iOS 1px trigger spacer.
    val shouldLoadMore by remember(state.filtered.size) {
        derivedStateOf {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: return@derivedStateOf false
            last >= state.filtered.size - 1
        }
    }
    LaunchedEffect(listState, state.hasMore, state.filtered.size) {
        snapshotFlow { shouldLoadMore }.collect { if (it && state.hasMore) onLoadMore() }
    }

    when {
        state.isLoading && state.videos.isEmpty() -> {
            Column(
                Modifier.padding(horizontal = 18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                repeat(3) { SkeletonCard() }
            }
        }

        state.filtered.isEmpty() -> {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 60.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                InkSeal(char = "琴", size = 48.dp)
                Text(
                    text = stringResource(R.string.video_empty),
                    color = VintageColors.inkMuted,
                    fontSize = 18.sp,
                    fontStyle = FontStyle.Italic,
                    fontFamily = FontFamily.Serif,
                )
            }
        }

        else -> {
            LazyColumn(
                state = listState,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 18.dp, end = 18.dp, bottom = 20.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                itemsIndexed(state.filtered) { index, item ->
                    if (index == 0) {
                        FeaturedVideoCard(item) { onVideoClick(item) }
                    } else {
                        VideoCard(item) { onVideoClick(item) }
                    }
                }

                if (state.isLoadingMore) {
                    item {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(color = VintageColors.inkMuted)
                        }
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 4: Wire the route into `MainScaffold`**

In `android/app/src/main/java/vn/hocdantranh/guzheng/ui/MainScaffold.kt`, add the import:

```kotlin
import vn.hocdantranh.guzheng.ui.video.VideoRoute
```

and replace the placeholder loop so Video gets its real screen:

```kotlin
            composable(Destination.VIDEO.route) {
                VideoRoute()
            }

            Destination.entries
                .filterNot { it == Destination.LESSONS || it == Destination.VIDEO }
                .forEach { destination ->
                    composable(destination.route) {
                        PlaceholderScreen(title = stringResource(destination.labelRes))
                    }
                }
```

- [ ] **Step 5: Run the UI test to verify it passes**

```bash
./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=vn.hocdantranh.guzheng.ui.video.VideoScreenTest
```

Expected: PASS, 5 tests.

- [ ] **Step 6: Run the full suites**

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:connectedDebugAndroidTest
```

Expected: all pass. Unit count rises by 27 (8 + 6 + 4 + 9); connected rises by 5.

- [ ] **Step 7: Commit**

```bash
cd /Users/kelvin/Code/AppHocDanTranh
git add android/app/src/main/java/vn/hocdantranh/guzheng/ui/video/VideoScreen.kt \
        android/app/src/main/java/vn/hocdantranh/guzheng/ui/MainScaffold.kt \
        android/app/src/androidTest/java/vn/hocdantranh/guzheng/ui/video/VideoScreenTest.kt
git commit -m "feat(android): build the Video tab screen and wire it into the shell"
```

---

### Task 7: Verify on the real device

**Files:** none — this task only observes.

**Interfaces:**
- Consumes: everything from Tasks 1–6.

- [ ] **Step 1: Reinstall (connectedAndroidTest uninstalls the app)**

```bash
cd /Users/kelvin/Code/AppHocDanTranh/android
export PATH="$PATH:$HOME/Library/Android/sdk/platform-tools"
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

If the install fails with a dropped device, run `adb kill-server && adb start-server` and retry — this OPPO drops its USB link mid-install intermittently.

- [ ] **Step 2: Open the Video tab and screenshot**

```bash
adb shell am start -n vn.hocdantranh.guzheng/.MainActivity
sleep 3
adb shell input tap 320 1880   # the Video tab in the bottom bar
sleep 4
adb exec-out screencap -p > /tmp/video-tab.png
```

- [ ] **Step 3: Confirm against the spec**

Check every item:

- The chip row reads exactly **Tất cả · Giới thiệu · Bài hát · Hướng dẫn** — four chips, no "Tip & Trick"
- The first card is the large featured tile; the rest are compact rows
- **Both** TikTok and YouTube thumbnails render (10 of 12 are TikTok, so mostly TikTok)
- Platform badges show the right label and colour
- Selecting a chip filters; tapping it again clears the filter
- Scrolling past the tenth card loads the remaining two (12 total)
- Tapping a card opens the TikTok or YouTube app

- [ ] **Step 4: Check for runtime errors**

```bash
adb logcat -d | grep -iE "AndroidRuntime|FATAL|Firestore.*(error|denied)"
```

Expected: no fatal exceptions and no permission errors.

- [ ] **Step 5: Commit any fixes the device check surfaced**

If the pass was clean, there is nothing to commit — say so rather than inventing a commit.

---

## Definition of done

- [ ] `./gradlew :app:testDebugUnitTest` passes, including the 27 new video tests
- [ ] `./gradlew :app:connectedDebugAndroidTest` passes, including the 5 new screen tests
- [ ] The Video tab shows all 12 live videos with working thumbnails on the OPPO
- [ ] Chips derive from the data — no "Tip & Trick" chip appears
- [ ] Pagination loads videos 11 and 12 on scroll
- [ ] Tapping a card opens the TikTok or YouTube app
- [ ] The language toggle switches chip labels and the header between vi and en
- [ ] Nothing under `HocDanTranh-Guzheng-DanThanh/` was modified
