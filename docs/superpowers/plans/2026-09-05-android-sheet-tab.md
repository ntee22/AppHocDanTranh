# Android Sheet Tab Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the Sheet tab's `PlaceholderScreen` with the ported iOS sheet-music library — Firestore catalog, title search, page viewer, and pinch-to-zoom.

**Architecture:** A `data/sheet` layer (model, Drive URL rewrite, Firestore repository, DataStore cache) feeds a `ui/sheet` layer (state + ViewModel + three screens). Derived values — search filtering, full-screen route membership — live in plain classes and top-level functions so they are testable on the JVM, matching how the Video tab is split. The viewer and zoom screens join the existing `NavHost` as full-screen routes that hide the bottom navigation bar.

**Tech Stack:** Kotlin, Jetpack Compose (BOM 2025.01.00), Navigation Compose, Firebase Firestore, Coil 2.7.0, Telephoto 0.15.1, DataStore Preferences, JUnit4 + Robolectric + Compose UI test.

**Spec:** [docs/superpowers/specs/2026-09-05-android-sheet-tab-design.md](../specs/2026-09-05-android-sheet-tab-design.md)

## Global Constraints

- **Package root:** `vn.hocdantranh.guzheng`. New code goes in `data/sheet/` and `ui/sheet/`.
- **Drive rewrite is load-bearing.** All 22 live page URLs are `drive.google.com` share links. Every image URL handed to Coil must pass through `directImageUrl` first, or the sheet renders blank.
- **`order` is a signed `Int` parsed from a `Number`.** Live values run `-1`…`17`, and Firestore returns integers as `Long`. Never use `as? Int`.
- **`isFree` defaults to `true`** when the field is absent, matching iOS.
- **`isVisible != true` filters the document out client-side.** Never add a `where` clause for it — that would demand a composite index.
- **Premium sheets are shown, not gated.** The lock overlay and `Premium` pill are cosmetic; the card stays clickable.
- **Search is case-insensitive and diacritic-sensitive:** `title.contains(query, ignoreCase = true)`. `nho` must not match `Nhỏ ơi`.
- **All user-facing copy goes through `strings.xml`** (vi, default) + `values-en/strings.xml`, prefixed `sheet_`. Sheet *titles* stay Vietnamese — they are content.
- **Colours come from `VintageColors`.** Never re-derive a hex value.
- **Telephoto is pinned to `0.15.1`.** Do not take a newer release; 0.16+ pulls Compose foundation 1.8.0 and Kotlin stdlib 2.1.21, both ahead of this project.
- **Unit tests that touch `org.json` or `Context` need `@RunWith(RobolectricTestRunner::class)`.**
- **Run unit tests with:** `cd android && JAVA_HOME=/Applications/Android\ Studio.app/Contents/jbr/Contents/Home ./gradlew :app:testDebugUnitTest`. `JAVA_HOME` must be set on every Gradle invocation in this environment.

---

### Task 1: Google Drive image URL rewriting

The single highest-risk item in the port. Pure function, no Android dependencies.

**Files:**
- Create: `android/app/src/main/java/vn/hocdantranh/guzheng/data/sheet/DriveImageUrl.kt`
- Test: `android/app/src/test/java/vn/hocdantranh/guzheng/data/sheet/DriveImageUrlTest.kt`

**Interfaces:**
- Consumes: nothing
- Produces: `fun directImageUrl(raw: String): String`

- [ ] **Step 1: Write the failing test**

```kotlin
package vn.hocdantranh.guzheng.data.sheet

import org.junit.Assert.assertEquals
import org.junit.Test

class DriveImageUrlTest {

    /** The exact shape of all 22 page URLs in the live `sheets` collection. */
    @Test
    fun `rewrites a live file-d share link`() {
        assertEquals(
            "https://lh3.googleusercontent.com/d/1aM8SnLS0B6z6O55TS_26ARnygqAhS8LQ",
            directImageUrl(
                "https://drive.google.com/file/d/1aM8SnLS0B6z6O55TS_26ARnygqAhS8LQ/view?usp=drive_link"
            )
        )
    }

    @Test
    fun `rewrites a usp-sharing link`() {
        assertEquals(
            "https://lh3.googleusercontent.com/d/1kpS9omXRY2gd61PHTBpj0M5Mj6YcFbPu",
            directImageUrl(
                "https://drive.google.com/file/d/1kpS9omXRY2gd61PHTBpj0M5Mj6YcFbPu/view?usp=sharing"
            )
        )
    }

    @Test
    fun `rewrites an id query-parameter link`() {
        assertEquals(
            "https://lh3.googleusercontent.com/d/ABC123",
            directImageUrl("https://drive.google.com/uc?id=ABC123&export=download")
        )
    }

    @Test
    fun `rewrites a bare d-slash link`() {
        assertEquals(
            "https://lh3.googleusercontent.com/d/XYZ789",
            directImageUrl("https://drive.google.com/d/XYZ789/")
        )
    }

    @Test
    fun `passes a non-Drive url through untouched`() {
        val url = "https://example.com/sheet.png"
        assertEquals(url, directImageUrl(url))
    }

    /** Degrade to a broken image, never a crash. */
    @Test
    fun `passes a Drive url with no extractable id through untouched`() {
        val url = "https://drive.google.com/drive/my-drive"
        assertEquals(url, directImageUrl(url))
    }

    @Test
    fun `passes an empty string through untouched`() {
        assertEquals("", directImageUrl(""))
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd android && JAVA_HOME=/Applications/Android\ Studio.app/Contents/jbr/Contents/Home ./gradlew :app:testDebugUnitTest --tests '*DriveImageUrlTest*'`
Expected: FAIL — compilation error, `directImageUrl` unresolved.

- [ ] **Step 3: Write minimal implementation**

```kotlin
package vn.hocdantranh.guzheng.data.sheet

/** Matches `/d/<id>`, stopping at the next `/` or `?`. */
private val DRIVE_PATH_ID = Regex("""/d/([^/?]+)""")

/** Matches `id=<value>` in a query string. */
private val DRIVE_QUERY_ID = Regex("""[?&]id=([^&]+)""")

/**
 * Rewrites a Google Drive sharing URL to one that serves the raw image bytes.
 *
 * Drive's `/file/d/<id>/view` links return an HTML viewer page with HTTP 200, so Coil
 * decodes markup instead of an image and the sheet renders blank. Every page URL in the
 * live collection is such a link, which makes this conversion load-bearing rather than
 * defensive. Port of the iOS `directImageURL`.
 *
 * Anything that is not a Drive URL, or is one with no extractable file id, is returned
 * unchanged — a malformed link should degrade to a broken image, never to a crash.
 */
fun directImageUrl(raw: String): String {
    if (!raw.contains("drive.google.com")) return raw

    val id = DRIVE_PATH_ID.find(raw)?.groupValues?.get(1)
        ?: DRIVE_QUERY_ID.find(raw)?.groupValues?.get(1)
        ?: return raw

    return "https://lh3.googleusercontent.com/d/$id"
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd android && JAVA_HOME=/Applications/Android\ Studio.app/Contents/jbr/Contents/Home ./gradlew :app:testDebugUnitTest --tests '*DriveImageUrlTest*'`
Expected: PASS, 7 tests.

- [ ] **Step 5: Commit**

```bash
git add android/app/src/main/java/vn/hocdantranh/guzheng/data/sheet/DriveImageUrl.kt \
        android/app/src/test/java/vn/hocdantranh/guzheng/data/sheet/DriveImageUrlTest.kt
git commit -m "feat(android): rewrite Google Drive share links to direct image urls"
```

---

### Task 2: SheetItem model and document mapping

**Files:**
- Create: `android/app/src/main/java/vn/hocdantranh/guzheng/data/sheet/SheetItem.kt`
- Test: `android/app/src/test/java/vn/hocdantranh/guzheng/data/sheet/SheetItemTest.kt`

**Interfaces:**
- Consumes: nothing
- Produces:
  - `data class SheetItem(val id: String, val title: String, val pages: List<String>, val isFree: Boolean, val order: Int)`
  - `fun sheetFromFields(id: String, fields: Map<String, Any?>): SheetItem?`

- [ ] **Step 1: Write the failing test**

```kotlin
package vn.hocdantranh.guzheng.data.sheet

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SheetItemTest {

    private fun fields(
        title: Any? = "Haru Haru",
        pages: Any? = listOf("https://drive.google.com/file/d/A/view"),
        isFree: Any? = true,
        isVisible: Any? = true,
        order: Any? = -1L,
    ): Map<String, Any?> = buildMap {
        put("title", title)
        put("pages", pages)
        put("isFree", isFree)
        put("isVisible", isVisible)
        put("order", order)
    }

    @Test
    fun `maps a valid document`() {
        val item = sheetFromFields("sheet_019", fields())
        assertEquals("sheet_019", item?.id)
        assertEquals("Haru Haru", item?.title)
        assertEquals(1, item?.pages?.size)
        assertEquals(-1, item?.order)
        assertEquals(true, item?.isFree)
    }

    /** Firestore hands integers back as Long; `as? Int` would silently drop every doc. */
    @Test
    fun `parses a Long order`() {
        assertEquals(17, sheetFromFields("s", fields(order = 17L))?.order)
    }

    /** Live orders run -1..17 — newest sheets sit on non-positive orders. */
    @Test
    fun `parses a negative order`() {
        assertEquals(-1, sheetFromFields("s", fields(order = -1L))?.order)
    }

    @Test
    fun `parses a zero order`() {
        assertEquals(0, sheetFromFields("s", fields(order = 0L))?.order)
    }

    @Test
    fun `drops a hidden document`() {
        assertNull(sheetFromFields("s", fields(isVisible = false)))
    }

    @Test
    fun `drops a document with no title`() {
        assertNull(sheetFromFields("s", fields(title = null)))
    }

    @Test
    fun `drops a document with no pages`() {
        assertNull(sheetFromFields("s", fields(pages = null)))
    }

    @Test
    fun `drops a document with no order`() {
        assertNull(sheetFromFields("s", fields(order = null)))
    }

    @Test
    fun `drops a document whose pages are not strings`() {
        assertNull(sheetFromFields("s", fields(pages = listOf(1, 2))))
    }

    @Test
    fun `treats a missing isFree as free, matching iOS`() {
        val item = sheetFromFields("s", fields().minus("isFree"))
        assertTrue(item!!.isFree)
    }

    @Test
    fun `treats a missing isVisible as visible`() {
        assertEquals("s", sheetFromFields("s", fields().minus("isVisible"))?.id)
    }

    @Test
    fun `reads a premium document`() {
        assertEquals(false, sheetFromFields("s", fields(isFree = false))?.isFree)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd android && JAVA_HOME=/Applications/Android\ Studio.app/Contents/jbr/Contents/Home ./gradlew :app:testDebugUnitTest --tests '*SheetItemTest*'`
Expected: FAIL — compilation error, `sheetFromFields` unresolved.

- [ ] **Step 3: Write minimal implementation**

```kotlin
package vn.hocdantranh.guzheng.data.sheet

/**
 * One sheet-music document. `pages` holds the raw Firestore URLs — call
 * [directImageUrl] at the point of display rather than rewriting here, so the cached
 * payload stays byte-identical to what Firestore served.
 */
data class SheetItem(
    val id: String,
    val title: String,
    val pages: List<String>,
    val isFree: Boolean,
    val order: Int,
)

/**
 * Maps one Firestore document's fields to a [SheetItem], or null if it should not render.
 *
 * Null rather than throwing, mirroring the iOS `compactMap`: one malformed document must
 * not empty the whole tab. `isVisible` is applied here and then dropped from the model, so
 * nothing downstream re-checks it.
 *
 * `order` is read through `Number` because Firestore returns integers as `Long` on
 * Android — the iOS `as? Int` spelling would drop all 19 live documents.
 */
fun sheetFromFields(id: String, fields: Map<String, Any?>): SheetItem? {
    val isVisible = fields["isVisible"] as? Boolean ?: true
    if (!isVisible) return null

    val title = fields["title"] as? String ?: return null
    val rawPages = fields["pages"] as? List<*> ?: return null
    val pages = rawPages.filterIsInstance<String>()
    if (pages.size != rawPages.size) return null
    val order = (fields["order"] as? Number)?.toInt() ?: return null

    return SheetItem(
        id = id,
        title = title,
        pages = pages,
        isFree = fields["isFree"] as? Boolean ?: true,
        order = order,
    )
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd android && JAVA_HOME=/Applications/Android\ Studio.app/Contents/jbr/Contents/Home ./gradlew :app:testDebugUnitTest --tests '*SheetItemTest*'`
Expected: PASS, 12 tests.

- [ ] **Step 5: Commit**

```bash
git add android/app/src/main/java/vn/hocdantranh/guzheng/data/sheet/SheetItem.kt \
        android/app/src/test/java/vn/hocdantranh/guzheng/data/sheet/SheetItemTest.kt
git commit -m "feat(android): add the SheetItem model and Firestore document mapping"
```

---

### Task 3: Sheet catalog cache

JSON in DataStore, so the list paints instantly on relaunch and survives being offline.

**Files:**
- Create: `android/app/src/main/java/vn/hocdantranh/guzheng/data/sheet/SheetCache.kt`
- Test: `android/app/src/test/java/vn/hocdantranh/guzheng/data/sheet/SheetCacheTest.kt`

**Interfaces:**
- Consumes: `SheetItem` (Task 2)
- Produces:
  - `fun encodeSheets(items: List<SheetItem>): String`
  - `fun decodeSheets(json: String): List<SheetItem>`
  - `class SheetCache(context: Context)` with `suspend fun read(): List<SheetItem>` and `suspend fun write(items: List<SheetItem>)`

- [ ] **Step 1: Write the failing test**

```kotlin
package vn.hocdantranh.guzheng.data.sheet

import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import vn.hocdantranh.guzheng.data.prefs.dataStore

/** Robolectric: `org.json` and `Context` are both unavailable to a plain JVM test. */
@RunWith(RobolectricTestRunner::class)
class SheetCacheTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val cache = SheetCache(context)

    private val items = listOf(
        SheetItem("sheet_019", "Haru Haru", listOf("a", "b"), isFree = true, order = -1),
        SheetItem("sheet_001", "Xin chào Việt Nam", listOf("c"), isFree = false, order = 1),
    )

    // The `Context.dataStore` delegate is a singleton that Robolectric reuses across the
    // methods of a class, so clear it before each test rather than after — see AppPrefsTest.
    @Before
    fun setUp() = runTest {
        context.dataStore.edit { it.clear() }
    }

    @Test
    fun `round-trips every field`() {
        assertEquals(items, decodeSheets(encodeSheets(items)))
    }

    @Test
    fun `round-trips an empty list`() {
        assertEquals(emptyList<SheetItem>(), decodeSheets(encodeSheets(emptyList())))
    }

    /** A schema change must never brick the tab — a bad payload reads as a cache miss. */
    @Test
    fun `decodes malformed json as empty`() {
        assertEquals(emptyList<SheetItem>(), decodeSheets("{not json"))
    }

    @Test
    fun `decodes a json array of the wrong shape as empty`() {
        assertEquals(emptyList<SheetItem>(), decodeSheets("""[{"unexpected":1}]"""))
    }

    @Test
    fun `reads back what it wrote`() = runTest {
        cache.write(items)
        assertEquals(items, cache.read())
    }

    @Test
    fun `reads empty when nothing was ever written`() = runTest {
        assertTrue(cache.read().isEmpty())
    }

    @Test
    fun `a later write replaces the earlier one`() = runTest {
        cache.write(items)
        cache.write(items.take(1))
        assertEquals(1, cache.read().size)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd android && JAVA_HOME=/Applications/Android\ Studio.app/Contents/jbr/Contents/Home ./gradlew :app:testDebugUnitTest --tests '*SheetCacheTest*'`
Expected: FAIL — compilation error, `SheetCache` / `encodeSheets` unresolved.

- [ ] **Step 3: Write minimal implementation**

```kotlin
package vn.hocdantranh.guzheng.data.sheet

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import vn.hocdantranh.guzheng.data.prefs.dataStore

/**
 * Key name matches the iOS `cached_sheets_v1` UserDefaults key. Bump the suffix if
 * [SheetItem] ever changes shape — a stale payload then reads as a miss rather than
 * decoding into something wrong.
 */
private val CACHED_SHEETS_KEY = stringPreferencesKey("cached_sheets_v1")

/** `org.json` rather than kotlinx.serialization — five flat fields do not justify a plugin. */
fun encodeSheets(items: List<SheetItem>): String {
    val array = JSONArray()
    items.forEach { item ->
        array.put(
            JSONObject()
                .put("id", item.id)
                .put("title", item.title)
                .put("pages", JSONArray(item.pages))
                .put("isFree", item.isFree)
                .put("order", item.order)
        )
    }
    return array.toString()
}

/**
 * Returns an empty list for anything it cannot read — malformed JSON, or an entry missing
 * a field. A cache is an optimisation; a corrupt one must degrade to a network fetch, never
 * take the tab down.
 */
fun decodeSheets(json: String): List<SheetItem> = runCatching {
    val array = JSONArray(json)
    (0 until array.length()).map { i ->
        val obj = array.getJSONObject(i)
        val pagesArray = obj.getJSONArray("pages")
        SheetItem(
            id = obj.getString("id"),
            title = obj.getString("title"),
            pages = (0 until pagesArray.length()).map { pagesArray.getString(it) },
            isFree = obj.getBoolean("isFree"),
            order = obj.getInt("order"),
        )
    }
}.getOrDefault(emptyList())

/** Last-known catalog, so the list paints before Firestore answers. */
class SheetCache(private val context: Context) {

    suspend fun read(): List<SheetItem> {
        val json = context.dataStore.data.first()[CACHED_SHEETS_KEY] ?: return emptyList()
        return decodeSheets(json)
    }

    suspend fun write(items: List<SheetItem>) {
        context.dataStore.edit { prefs -> prefs[CACHED_SHEETS_KEY] = encodeSheets(items) }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd android && JAVA_HOME=/Applications/Android\ Studio.app/Contents/jbr/Contents/Home ./gradlew :app:testDebugUnitTest --tests '*SheetCacheTest*'`
Expected: PASS, 7 tests.

- [ ] **Step 5: Commit**

```bash
git add android/app/src/main/java/vn/hocdantranh/guzheng/data/sheet/SheetCache.kt \
        android/app/src/test/java/vn/hocdantranh/guzheng/data/sheet/SheetCacheTest.kt
git commit -m "feat(android): cache the sheet catalog in DataStore"
```

---

### Task 4: Firestore sheet repository

**Files:**
- Create: `android/app/src/main/java/vn/hocdantranh/guzheng/data/sheet/SheetRepository.kt`
- Test: none — the class is a thin Firestore adapter with no branching. `sheetFromFields` (Task 2) carries the logic and is already covered; the wiring is verified on-device in Task 11.

**Interfaces:**
- Consumes: `SheetItem`, `sheetFromFields` (Task 2)
- Produces:
  - `interface SheetRepository { suspend fun load(): List<SheetItem> }`
  - `class FirestoreSheetRepository(firestore: FirebaseFirestore = FirebaseFirestore.getInstance()) : SheetRepository`

- [ ] **Step 1: Write the implementation**

```kotlin
package vn.hocdantranh.guzheng.data.sheet

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

interface SheetRepository {
    /** The whole visible catalog, already ordered. Throws if Firestore is unreachable. */
    suspend fun load(): List<SheetItem>
}

/**
 * Reads the entire `sheets` collection in one query — no pagination.
 *
 * The catalog is 19 documents and search needs the full set client-side, so paging it the
 * way the Video tab does would make search silently incomplete. `isVisible` is filtered in
 * [sheetFromFields] rather than in a `where` clause, because combining it with `orderBy`
 * would demand a composite index; the single-field order uses the automatic one.
 */
class FirestoreSheetRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) : SheetRepository {

    override suspend fun load(): List<SheetItem> =
        firestore.collection("sheets")
            .orderBy("order")
            .get()
            .await()
            .documents
            .mapNotNull { sheetFromFields(it.id, it.data.orEmpty()) }
}
```

- [ ] **Step 2: Verify it compiles**

Run: `cd android && JAVA_HOME=/Applications/Android\ Studio.app/Contents/jbr/Contents/Home ./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add android/app/src/main/java/vn/hocdantranh/guzheng/data/sheet/SheetRepository.kt
git commit -m "feat(android): read the sheets collection from Firestore"
```

---

### Task 5: Sheet UI state and ViewModel

**Files:**
- Create: `android/app/src/main/java/vn/hocdantranh/guzheng/ui/sheet/SheetViewModel.kt`
- Test: `android/app/src/test/java/vn/hocdantranh/guzheng/ui/sheet/SheetUiStateTest.kt`
- Test: `android/app/src/test/java/vn/hocdantranh/guzheng/ui/sheet/SheetViewModelTest.kt`

**Interfaces:**
- Consumes: `SheetItem` (Task 2), `SheetCache` (Task 3), `SheetRepository`, `FirestoreSheetRepository` (Task 4)
- Produces:
  - `data class SheetUiState(sheets: List<SheetItem>, query: String, isLoading: Boolean, errorMessage: String?)` with `val filtered: List<SheetItem>` and `fun sheetById(id: String): SheetItem?`
  - `class SheetViewModel(repository: SheetRepository, cache: SheetCache?)` exposing `uiState: StateFlow<SheetUiState>`, `refresh()`, `search(query: String)`
  - `SheetViewModel.factory(context: Context): ViewModelProvider.Factory`

- [ ] **Step 1: Write the failing state test**

```kotlin
package vn.hocdantranh.guzheng.ui.sheet

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import vn.hocdantranh.guzheng.data.sheet.SheetItem

class SheetUiStateTest {

    private val sheets = listOf(
        SheetItem("1", "Nhỏ ơi", listOf("a"), isFree = true, order = 11),
        SheetItem("2", "Hồn quê", listOf("b"), isFree = true, order = 13),
        SheetItem("3", "Haru Haru", listOf("c", "d"), isFree = false, order = -1),
    )

    private fun state(query: String = "") = SheetUiState(sheets = sheets, query = query)

    @Test
    fun `a blank query returns everything`() {
        assertEquals(3, state().filtered.size)
    }

    @Test
    fun `matches a title substring`() {
        assertEquals(listOf("Nhỏ ơi"), state("nhỏ").filtered.map { it.title })
    }

    @Test
    fun `matches case-insensitively`() {
        assertEquals(listOf("Haru Haru"), state("HARU").filtered.map { it.title })
    }

    /** iOS uses localizedCaseInsensitiveContains, which folds case but not diacritics. */
    @Test
    fun `does not fold diacritics`() {
        assertTrue(state("nho").filtered.isEmpty())
    }

    @Test
    fun `returns empty for a query that matches nothing`() {
        assertTrue(state("zzz").filtered.isEmpty())
    }

    @Test
    fun `ignores surrounding whitespace`() {
        assertEquals(listOf("Hồn quê"), state("  hồn  ").filtered.map { it.title })
    }

    @Test
    fun `finds a sheet by id`() {
        assertEquals("Hồn quê", state().sheetById("2")?.title)
    }

    @Test
    fun `returns null for an unknown id`() {
        assertNull(state().sheetById("nope"))
    }
}
```

- [ ] **Step 2: Write the failing ViewModel test**

```kotlin
package vn.hocdantranh.guzheng.ui.sheet

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import vn.hocdantranh.guzheng.data.sheet.SheetItem
import vn.hocdantranh.guzheng.data.sheet.SheetRepository

@OptIn(ExperimentalCoroutinesApi::class)
class SheetViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    private fun item(id: String, title: String = "Sheet $id") =
        SheetItem(id, title, listOf("https://drive.google.com/file/d/$id/view"), true, id.toInt())

    private class FakeRepository(
        private val result: Result<List<SheetItem>>,
        private val gate: CompletableDeferred<Unit>? = null,
    ) : SheetRepository {
        var calls = 0
            private set

        override suspend fun load(): List<SheetItem> {
            calls++
            gate?.await()
            return result.getOrThrow()
        }
    }

    @Test
    fun `loads the catalog on construction`() = runTest {
        val vm = SheetViewModel(FakeRepository(Result.success(listOf(item("1")))), cache = null)
        advanceUntilIdle()

        assertEquals(1, vm.uiState.value.sheets.size)
        assertFalse(vm.uiState.value.isLoading)
        assertNull(vm.uiState.value.errorMessage)
    }

    @Test
    fun `is loading until the repository answers`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val vm = SheetViewModel(FakeRepository(Result.success(emptyList()), gate), cache = null)
        advanceUntilIdle()

        assertTrue(vm.uiState.value.isLoading)

        gate.complete(Unit)
        advanceUntilIdle()
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun `a failure with no cached sheets surfaces an error`() = runTest {
        val vm = SheetViewModel(FakeRepository(Result.failure(RuntimeException("offline"))), cache = null)
        advanceUntilIdle()

        assertNotNull(vm.uiState.value.errorMessage)
        assertTrue(vm.uiState.value.sheets.isEmpty())
    }

    /** Stale sheets beat an error screen — the user can still read what they had. */
    @Test
    fun `a failure keeps sheets already on screen and shows no error`() = runTest {
        val good = FakeRepository(Result.success(listOf(item("1"))))
        val vm = SheetViewModel(good, cache = null)
        advanceUntilIdle()

        vm.replaceRepositoryForTest(FakeRepository(Result.failure(RuntimeException("offline"))))
        vm.refresh()
        advanceUntilIdle()

        assertEquals(1, vm.uiState.value.sheets.size)
        assertNull(vm.uiState.value.errorMessage)
    }

    @Test
    fun `refresh replaces the catalog`() = runTest {
        val vm = SheetViewModel(FakeRepository(Result.success(listOf(item("1")))), cache = null)
        advanceUntilIdle()

        vm.replaceRepositoryForTest(FakeRepository(Result.success(listOf(item("2"), item("3")))))
        vm.refresh()
        advanceUntilIdle()

        assertEquals(listOf("2", "3"), vm.uiState.value.sheets.map { it.id })
    }

    @Test
    fun `retry after a failure clears the error`() = runTest {
        val vm = SheetViewModel(FakeRepository(Result.failure(RuntimeException("offline"))), cache = null)
        advanceUntilIdle()
        assertNotNull(vm.uiState.value.errorMessage)

        vm.replaceRepositoryForTest(FakeRepository(Result.success(listOf(item("1")))))
        vm.refresh()
        advanceUntilIdle()

        assertNull(vm.uiState.value.errorMessage)
        assertEquals(1, vm.uiState.value.sheets.size)
    }

    @Test
    fun `search updates the query`() = runTest {
        val vm = SheetViewModel(FakeRepository(Result.success(listOf(item("1", "Nhỏ ơi")))), cache = null)
        advanceUntilIdle()

        vm.search("nhỏ")
        assertEquals("nhỏ", vm.uiState.value.query)
        assertEquals(1, vm.uiState.value.filtered.size)

        vm.search("zzz")
        assertTrue(vm.uiState.value.filtered.isEmpty())
    }
}
```

- [ ] **Step 3: Run both tests to verify they fail**

Run: `cd android && JAVA_HOME=/Applications/Android\ Studio.app/Contents/jbr/Contents/Home ./gradlew :app:testDebugUnitTest --tests '*SheetUiStateTest*' --tests '*SheetViewModelTest*'`
Expected: FAIL — compilation error, `SheetUiState` / `SheetViewModel` unresolved.

- [ ] **Step 4: Write minimal implementation**

```kotlin
package vn.hocdantranh.guzheng.ui.sheet

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.hocdantranh.guzheng.data.sheet.FirestoreSheetRepository
import vn.hocdantranh.guzheng.data.sheet.SheetCache
import vn.hocdantranh.guzheng.data.sheet.SheetItem
import vn.hocdantranh.guzheng.data.sheet.SheetRepository

/**
 * Everything the Sheet tab renders. Filtering lives here rather than in the composables so
 * search is testable without a device — the same split as `VideoUiState`.
 */
data class SheetUiState(
    val sheets: List<SheetItem> = emptyList(),
    val query: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
) {
    /**
     * Case-insensitive, diacritic-**sensitive** title match, mirroring the iOS
     * `localizedCaseInsensitiveContains`: `nho` does not find `Nhỏ ơi`. Folding diacritics
     * would suit Vietnamese typists better, but that is a change for both platforms.
     */
    val filtered: List<SheetItem>
        get() {
            val needle = query.trim()
            if (needle.isEmpty()) return sheets
            return sheets.filter { it.title.contains(needle, ignoreCase = true) }
        }

    fun sheetById(id: String): SheetItem? = sheets.firstOrNull { it.id == id }
}

class SheetViewModel(
    repository: SheetRepository,
    private val cache: SheetCache?,
) : ViewModel() {

    private var repository: SheetRepository = repository

    private val _uiState = MutableStateFlow(SheetUiState(isLoading = true))
    val uiState: StateFlow<SheetUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // Paint the last-known catalog first, so a warm start shows sheets immediately.
            cache?.read()?.takeIf { it.isNotEmpty() }?.let { cached ->
                _uiState.update { it.copy(sheets = cached) }
            }
            load()
        }
    }

    /** Reloads from Firestore, replacing what is on screen. Also backs the retry button. */
    fun refresh() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        val result = runCatching { repository.load() }
        _uiState.update { state ->
            result.fold(
                onSuccess = { sheets ->
                    state.copy(sheets = sheets, isLoading = false, errorMessage = null)
                },
                onFailure = { error ->
                    // Stale sheets beat an error screen; only a cold failure is worth showing.
                    state.copy(
                        isLoading = false,
                        errorMessage = if (state.sheets.isEmpty()) {
                            error.message ?: "Unknown error"
                        } else {
                            null
                        },
                    )
                },
            )
        }
        result.getOrNull()?.let { cache?.write(it) }
    }

    fun search(query: String) {
        _uiState.update { it.copy(query = query) }
    }

    /** Test seam: swaps the backing repository so one instance can exercise both outcomes. */
    internal fun replaceRepositoryForTest(next: SheetRepository) {
        repository = next
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T = SheetViewModel(
                    repository = FirestoreSheetRepository(),
                    cache = SheetCache(context.applicationContext),
                ) as T
            }
    }
}
```

- [ ] **Step 5: Run both tests to verify they pass**

Run: `cd android && JAVA_HOME=/Applications/Android\ Studio.app/Contents/jbr/Contents/Home ./gradlew :app:testDebugUnitTest --tests '*SheetUiStateTest*' --tests '*SheetViewModelTest*'`
Expected: PASS, 8 + 7 tests.

- [ ] **Step 6: Commit**

```bash
git add android/app/src/main/java/vn/hocdantranh/guzheng/ui/sheet/SheetViewModel.kt \
        android/app/src/test/java/vn/hocdantranh/guzheng/ui/sheet/
git commit -m "feat(android): add the sheet UI state and ViewModel"
```

---

### Task 6: Sheet tab strings

Every `sheet_*` key the UI tasks reference. Adding them up front keeps Tasks 7–11 from each editing the same two files.

**Files:**
- Modify: `android/app/src/main/res/values/strings.xml` (append before `</resources>`)
- Modify: `android/app/src/main/res/values-en/strings.xml` (append before `</resources>`)

**Interfaces:**
- Consumes: nothing
- Produces: `R.string.sheet_*` — the exact keys used by Tasks 7, 8, 10 and 11.

- [ ] **Step 1: Append the Vietnamese strings**

Add to `android/app/src/main/res/values/strings.xml`, immediately before `</resources>`:

```xml
    <!-- Sheet tab -->
    <string name="sheet_overline">樂譜 · Sheet Nhạc</string>
    <string name="sheet_header_title">"Sheet "</string>
    <string name="sheet_header_accent">Guzheng</string>
    <string name="sheet_header_tagline">Những sheet Guzheng tinh tuyển được Đan Thanh biên soạn và sưu tầm cho mọi người 🎶</string>
    <string name="sheet_search_hint">Nhập tên bài hát bạn muốn tìm</string>
    <string name="sheet_search_clear">Xoá tìm kiếm</string>
    <string name="sheet_pages_count">%1$d trang</string>
    <string name="sheet_badge_free">Miễn phí</string>
    <string name="sheet_badge_premium">Premium</string>
    <string name="sheet_locked">Sheet premium</string>
    <string name="sheet_empty">Chưa có sheet nào cả 📭</string>
    <string name="sheet_empty_search">Không tìm thấy \"%1$s\" 🔍</string>
    <string name="sheet_error_title">Ối, tải không được rồi 😵</string>
    <string name="sheet_error_retry">🔄 Thử lại</string>
    <string name="sheet_open">Mở sheet</string>
    <string name="sheet_back">Quay lại</string>
    <string name="sheet_viewer_hint">👆 Nhấn vào trang để phóng to xem cho rõ nhé!</string>
    <string name="sheet_page_load_failed">Không thể tải trang %1$d</string>
    <string name="sheet_page_number">Trang %1$d</string>
    <string name="sheet_zoom_hint">🤏 Chụm để phóng to · 👆👆 Nhấp đúp để reset</string>
    <string name="sheet_zoom_close">Đóng</string>
```

- [ ] **Step 2: Append the English strings**

Add to `android/app/src/main/res/values-en/strings.xml`, immediately before `</resources>`.

Note these translate four labels that iOS leaves hardcoded Vietnamese even in English mode (`N trang`, `Miễn phí`, and both viewer hints) — a deliberate divergence recorded in the spec.

```xml
    <!-- Sheet tab -->
    <string name="sheet_overline">樂譜 · Sheet Music</string>
    <string name="sheet_header_title">"Sheet "</string>
    <string name="sheet_header_accent">Guzheng</string>
    <string name="sheet_header_tagline">Hand-picked Guzheng sheet music compiled and arranged by Đan Thanh 🎶</string>
    <string name="sheet_search_hint">Enter a song name to search</string>
    <string name="sheet_search_clear">Clear search</string>
    <string name="sheet_pages_count">%1$d pages</string>
    <string name="sheet_badge_free">Free</string>
    <string name="sheet_badge_premium">Premium</string>
    <string name="sheet_locked">Premium sheet</string>
    <string name="sheet_empty">No sheets yet 📭</string>
    <string name="sheet_empty_search">No results for \"%1$s\" 🔍</string>
    <string name="sheet_error_title">Oops, couldn\'t load 😵</string>
    <string name="sheet_error_retry">🔄 Retry</string>
    <string name="sheet_open">Open sheet</string>
    <string name="sheet_back">Back</string>
    <string name="sheet_viewer_hint">👆 Tap a page to zoom in for a closer look!</string>
    <string name="sheet_page_load_failed">Couldn\'t load page %1$d</string>
    <string name="sheet_page_number">Page %1$d</string>
    <string name="sheet_zoom_hint">🤏 Pinch to zoom · 👆👆 Double-tap to reset</string>
    <string name="sheet_zoom_close">Close</string>
```

- [ ] **Step 3: Verify both files parse and every key exists in both locales**

Run:
```bash
cd android && JAVA_HOME=/Applications/Android\ Studio.app/Contents/jbr/Contents/Home ./gradlew :app:assembleDebug
diff <(grep -o 'name="sheet_[^"]*"' app/src/main/res/values/strings.xml | sort) \
     <(grep -o 'name="sheet_[^"]*"' app/src/main/res/values-en/strings.xml | sort)
```
Expected: BUILD SUCCESSFUL, and `diff` prints nothing (21 keys in each).

- [ ] **Step 4: Commit**

```bash
git add android/app/src/main/res/values/strings.xml android/app/src/main/res/values-en/strings.xml
git commit -m "feat(android): add the Sheet tab strings in Vietnamese and English"
```

---

### Task 7: Sheet card and skeleton card

**Files:**
- Create: `android/app/src/main/java/vn/hocdantranh/guzheng/ui/sheet/SheetCards.kt`

**Interfaces:**
- Consumes: `SheetItem` (Task 2), `directImageUrl` (Task 1), `R.string.sheet_*` (Task 6)
- Produces:
  - `@Composable fun SheetCard(sheet: SheetItem, onClick: () -> Unit)`
  - `@Composable fun SheetSkeletonCard()`

- [ ] **Step 1: Write the implementation**

```kotlin
package vn.hocdantranh.guzheng.ui.sheet

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import vn.hocdantranh.guzheng.R
import vn.hocdantranh.guzheng.data.sheet.SheetItem
import vn.hocdantranh.guzheng.data.sheet.directImageUrl
import vn.hocdantranh.guzheng.ui.theme.VintageColors

private val ThumbWidth = 64.dp
private val ThumbHeight = 80.dp

/**
 * One row in the sheet library.
 *
 * A premium sheet gets a scrim and a padlock but stays clickable — iOS shows the same
 * badge and opens the sheet anyway, and Android mirrors that until Play Billing lands.
 */
@Composable
fun SheetCard(sheet: SheetItem, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(VintageColors.paperWarm.copy(alpha = 0.7f))
            .border(1.dp, VintageColors.inkMuted.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Thumbnail(sheet)

        Column(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = sheet.title,
                color = VintageColors.inkDark,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Serif,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.sheet_pages_count, sheet.pages.size),
                    color = VintageColors.inkMuted,
                    fontSize = 11.sp,
                )
                AccessPill(isFree = sheet.isFree)
            }
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = VintageColors.inkMuted.copy(alpha = 0.4f),
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun Thumbnail(sheet: SheetItem) {
    Box(
        Modifier
            .width(ThumbWidth)
            .height(ThumbHeight)
            .clip(RoundedCornerShape(8.dp))
            .background(VintageColors.paperDeep),
        contentAlignment = Alignment.Center,
    ) {
        val firstPage = sheet.pages.firstOrNull()
        if (firstPage != null) {
            SubcomposeAsyncImage(
                // Raw Drive links serve an HTML page, not bytes — always rewrite first.
                model = directImageUrl(firstPage),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                error = {
                    Icon(
                        Icons.Filled.MusicNote,
                        contentDescription = null,
                        tint = VintageColors.inkMuted,
                    )
                },
            )
        } else {
            Icon(Icons.Filled.MusicNote, contentDescription = null, tint = VintageColors.inkMuted)
        }

        if (!sheet.isFree) {
            val locked = stringResource(R.string.sheet_locked)
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .semantics { contentDescription = locked },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Lock,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun AccessPill(isFree: Boolean) {
    val tint = if (isFree) VintageColors.sageDark else VintageColors.plum
    Text(
        text = stringResource(
            if (isFree) R.string.sheet_badge_free else R.string.sheet_badge_premium
        ),
        color = tint,
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(tint.copy(alpha = 0.1f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

/** Placeholder row shown while the first load is in flight. */
@Composable
fun SheetSkeletonCard() {
    val transition = rememberInfiniteTransition(label = "sheet-skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "shimmer",
    )

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(VintageColors.paperWarm.copy(alpha = 0.5f))
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            Modifier
                .width(ThumbWidth)
                .height(ThumbHeight)
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
                    .height(16.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(VintageColors.woodSoft.copy(alpha = alpha))
            )
            Box(
                Modifier
                    .width(80.dp)
                    .height(11.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(VintageColors.woodSoft.copy(alpha = alpha))
            )
        }
    }
}
```

- [ ] **Step 2: Verify it compiles**

Run: `cd android && JAVA_HOME=/Applications/Android\ Studio.app/Contents/jbr/Contents/Home ./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add android/app/src/main/java/vn/hocdantranh/guzheng/ui/sheet/SheetCards.kt
git commit -m "feat(android): add the sheet card and its loading skeleton"
```

---

### Task 8: Sheet list screen

**Files:**
- Create: `android/app/src/main/java/vn/hocdantranh/guzheng/ui/sheet/SheetScreen.kt`
- Test: `android/app/src/androidTest/java/vn/hocdantranh/guzheng/ui/sheet/SheetScreenTest.kt`

**Interfaces:**
- Consumes: `SheetUiState`, `SheetViewModel` (Task 5), `SheetCard`, `SheetSkeletonCard` (Task 7), `R.string.sheet_*` (Task 6)
- Produces:
  - `@Composable fun SheetRoute(onSheetClick: (SheetItem) -> Unit)`
  - `@Composable fun SheetScreen(state: SheetUiState, onQueryChange: (String) -> Unit, onSheetClick: (SheetItem) -> Unit, onRetry: () -> Unit)`

- [ ] **Step 1: Write the failing instrumented test**

```kotlin
package vn.hocdantranh.guzheng.ui.sheet

import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import vn.hocdantranh.guzheng.R
import vn.hocdantranh.guzheng.data.sheet.SheetItem

/**
 * Asserts against string resources rather than Vietnamese literals: these tests drive
 * `SheetScreen` directly, so they never run `LocaleController` and resolve whichever
 * locale the device is set to. Same reasoning as `VideoScreenTest`.
 */
class SheetScreenTest {

    @get:Rule val compose = createComposeRule()

    private fun str(@StringRes id: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private fun str(@StringRes id: Int, vararg args: Any): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *args)

    private val sheets = listOf(
        SheetItem("1", "Nhỏ ơi", listOf("a"), isFree = true, order = 11),
        SheetItem("2", "Hồn quê", listOf("b"), isFree = true, order = 13),
    )

    private fun show(
        state: SheetUiState,
        onSheetClick: (SheetItem) -> Unit = {},
        onRetry: () -> Unit = {},
    ) {
        compose.setContent {
            var query by androidx.compose.runtime.remember {
                androidx.compose.runtime.mutableStateOf(state.query)
            }
            SheetScreen(
                state = state.copy(query = query),
                onQueryChange = { query = it },
                onSheetClick = onSheetClick,
                onRetry = onRetry,
            )
        }
    }

    @Test
    fun rendersEverySheetTitle() {
        show(SheetUiState(sheets = sheets))

        compose.onNodeWithText("Nhỏ ơi").assertIsDisplayed()
        compose.onNodeWithText("Hồn quê").assertIsDisplayed()
    }

    @Test
    fun typingInSearchFiltersTheList() {
        show(SheetUiState(sheets = sheets))

        compose.onNodeWithText(str(R.string.sheet_search_hint)).performTextInput("nhỏ")

        compose.onNodeWithText("Nhỏ ơi").assertIsDisplayed()
        compose.onNodeWithText("Hồn quê").assertDoesNotExist()
    }

    @Test
    fun clearButtonRestoresTheFullList() {
        show(SheetUiState(sheets = sheets))

        compose.onNodeWithText(str(R.string.sheet_search_hint)).performTextInput("nhỏ")
        compose.onNodeWithContentDescription(str(R.string.sheet_search_clear)).performClick()

        compose.onNodeWithText("Hồn quê").assertIsDisplayed()
    }

    @Test
    fun showsTheNoResultsEmptyStateForAQueryThatMatchesNothing() {
        show(SheetUiState(sheets = sheets, query = "zzz"))

        compose.onNodeWithText(str(R.string.sheet_empty_search, "zzz")).assertIsDisplayed()
    }

    @Test
    fun showsTheEmptyCatalogStateWhenThereAreNoSheets() {
        show(SheetUiState(sheets = emptyList()))

        compose.onNodeWithText(str(R.string.sheet_empty)).assertIsDisplayed()
    }

    @Test
    fun showsTheErrorStateAndRetries() {
        var retries = 0
        show(SheetUiState(errorMessage = "offline"), onRetry = { retries++ })

        compose.onNodeWithText(str(R.string.sheet_error_title)).assertIsDisplayed()
        compose.onNodeWithText(str(R.string.sheet_error_retry)).performClick()

        assertEquals(1, retries)
    }

    @Test
    fun tappingASheetReportsIt() {
        var tapped: SheetItem? = null
        show(SheetUiState(sheets = sheets), onSheetClick = { tapped = it })

        compose.onNodeWithText("Hồn quê").performClick()

        assertEquals("2", tapped?.id)
    }

    /**
     * The only coverage of the premium branch: no live sheet is premium, so this synthetic
     * item is the sole thing exercising the lock overlay. It must still be clickable.
     */
    @Test
    fun premiumSheetShowsTheLockAndStaysClickable() {
        var tapped: SheetItem? = null
        val premium = SheetItem("9", "Sheet trả phí", listOf("x"), isFree = false, order = 1)
        show(SheetUiState(sheets = listOf(premium)), onSheetClick = { tapped = it })

        compose.onNodeWithText(str(R.string.sheet_badge_premium)).assertIsDisplayed()
        compose.onNodeWithContentDescription(str(R.string.sheet_locked)).assertIsDisplayed()

        compose.onNodeWithText("Sheet trả phí").performClick()
        assertEquals("9", tapped?.id)
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `cd android && JAVA_HOME=/Applications/Android\ Studio.app/Contents/jbr/Contents/Home ./gradlew :app:connectedDebugAndroidTest --tests '*SheetScreenTest*'`
Expected: FAIL — compilation error, `SheetScreen` unresolved. (Requires the OPPO CPH2363 attached; run `adb devices` first and re-pair if it dropped.)

- [ ] **Step 3: Write minimal implementation**

```kotlin
package vn.hocdantranh.guzheng.ui.sheet

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import vn.hocdantranh.guzheng.R
import vn.hocdantranh.guzheng.data.sheet.SheetItem
import vn.hocdantranh.guzheng.ui.components.InkSeal
import vn.hocdantranh.guzheng.ui.components.OverlineLabel
import vn.hocdantranh.guzheng.ui.components.PaperBackground
import vn.hocdantranh.guzheng.ui.theme.VintageColors

/** Binds the ViewModel and reports a tapped sheet to the navigation host. */
@Composable
fun SheetRoute(onSheetClick: (SheetItem) -> Unit) {
    val context = LocalContext.current
    val viewModel: SheetViewModel = viewModel(factory = SheetViewModel.factory(context))
    val state by viewModel.uiState.collectAsState()

    SheetScreen(
        state = state,
        onQueryChange = viewModel::search,
        onSheetClick = onSheetClick,
        onRetry = viewModel::refresh,
    )
}

@Composable
fun SheetScreen(
    state: SheetUiState,
    onQueryChange: (String) -> Unit,
    onSheetClick: (SheetItem) -> Unit,
    onRetry: () -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        PaperBackground()

        Column(Modifier.fillMaxSize()) {
            Header()
            SearchBar(query = state.query, onQueryChange = onQueryChange)
            Content(state = state, onSheetClick = onSheetClick, onRetry = onRetry)
        }
    }
}

@Composable
private fun Header() {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(top = 56.dp, bottom = 14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            OverlineLabel(text = stringResource(R.string.sheet_overline))
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = VintageColors.inkDark)) {
                        append(stringResource(R.string.sheet_header_title))
                    }
                    withStyle(
                        SpanStyle(
                            color = VintageColors.sageDark,
                            fontStyle = FontStyle.Italic,
                        )
                    ) {
                        append(stringResource(R.string.sheet_header_accent))
                    }
                },
                fontSize = 32.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Serif,
            )
            Text(
                text = stringResource(R.string.sheet_header_tagline),
                color = VintageColors.inkSoft,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        InkSeal(char = "樂", size = 32.dp)
    }
}

@Composable
private fun SearchBar(query: String, onQueryChange: (String) -> Unit) {
    Row(
        Modifier
            .padding(horizontal = 18.dp)
            .padding(bottom = 12.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(VintageColors.paperWarm.copy(alpha = 0.8f))
            .border(1.dp, VintageColors.woodDark.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Filled.Search,
            contentDescription = null,
            tint = VintageColors.inkMuted,
            modifier = Modifier.size(16.dp),
        )

        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (query.isEmpty()) {
                Text(
                    text = stringResource(R.string.sheet_search_hint),
                    color = VintageColors.inkMuted.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(
                    color = VintageColors.inkDark,
                    fontSize = 14.sp,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (query.isNotEmpty()) {
            Icon(
                Icons.Filled.Cancel,
                contentDescription = stringResource(R.string.sheet_search_clear),
                tint = VintageColors.inkMuted,
                modifier = Modifier
                    .size(16.dp)
                    .clickable { onQueryChange("") },
            )
        }
    }
}

@Composable
private fun Content(
    state: SheetUiState,
    onSheetClick: (SheetItem) -> Unit,
    onRetry: () -> Unit,
) {
    val listPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 110.dp)

    when {
        state.errorMessage != null -> ErrorState(message = state.errorMessage, onRetry = onRetry)

        state.isLoading && state.sheets.isEmpty() ->
            LazyColumn(
                contentPadding = listPadding,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(6) { SheetSkeletonCard() }
            }

        state.filtered.isEmpty() -> EmptyState(query = state.query)

        else -> LazyColumn(
            contentPadding = listPadding,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(state.filtered, key = { it.id }) { sheet ->
                SheetCard(sheet = sheet) { onSheetClick(sheet) }
            }
        }
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 60.dp, start = 32.dp, end = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            Icons.Filled.WarningAmber,
            contentDescription = null,
            tint = VintageColors.sealRed,
            modifier = Modifier.size(32.dp),
        )
        Text(
            text = stringResource(R.string.sheet_error_title),
            color = VintageColors.inkDark,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.Serif,
        )
        Text(
            text = message,
            color = VintageColors.inkMuted,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.sheet_error_retry),
            color = VintageColors.sageDark,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.clickable(onClick = onRetry),
        )
    }
}

@Composable
private fun EmptyState(query: String) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "樂",
            color = VintageColors.inkMuted.copy(alpha = 0.4f),
            fontSize = 48.sp,
            fontFamily = FontFamily.Serif,
        )
        Text(
            text = if (query.isBlank()) {
                stringResource(R.string.sheet_empty)
            } else {
                stringResource(R.string.sheet_empty_search, query)
            },
            color = VintageColors.inkMuted,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            fontStyle = FontStyle.Italic,
            fontFamily = FontFamily.Serif,
            textAlign = TextAlign.Center,
        )
    }
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `cd android && JAVA_HOME=/Applications/Android\ Studio.app/Contents/jbr/Contents/Home ./gradlew :app:connectedDebugAndroidTest --tests '*SheetScreenTest*'`
Expected: PASS, 8 tests.

- [ ] **Step 5: Commit**

```bash
git add android/app/src/main/java/vn/hocdantranh/guzheng/ui/sheet/SheetScreen.kt \
        android/app/src/androidTest/java/vn/hocdantranh/guzheng/ui/sheet/SheetScreenTest.kt
git commit -m "feat(android): build the sheet library screen with search"
```

---

### Task 9: Full-screen sheet routes and bottom-bar hiding

The viewer covers the tab bar, unlike lesson detail which keeps it — iOS presents the viewer as a `fullScreenCover`.

**Files:**
- Modify: `android/app/src/main/java/vn/hocdantranh/guzheng/ui/Destinations.kt` (append after `lessonDetailRoute`)
- Modify: `android/app/src/main/java/vn/hocdantranh/guzheng/ui/MainScaffold.kt`
- Test: `android/app/src/test/java/vn/hocdantranh/guzheng/ui/SheetRoutesTest.kt`

**Interfaces:**
- Consumes: `Destination` (existing)
- Produces:
  - `const val ARG_SHEET_ID = "sheetId"`, `const val ARG_PAGE_INDEX = "pageIndex"`
  - `const val SHEET_VIEWER_ROUTE = "sheet/{sheetId}"`, `const val SHEET_PAGE_ROUTE = "sheet/{sheetId}/page/{pageIndex}"`
  - `fun sheetViewerRoute(sheetId: String): String`, `fun sheetPageRoute(sheetId: String, pageIndex: Int): String`
  - `fun isFullScreenRoute(route: String?): Boolean`

- [ ] **Step 1: Write the failing test**

```kotlin
package vn.hocdantranh.guzheng.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SheetRoutesTest {

    @Test
    fun `builds a viewer route`() {
        assertEquals("sheet/sheet_019", sheetViewerRoute("sheet_019"))
    }

    @Test
    fun `builds a page route`() {
        assertEquals("sheet/sheet_019/page/1", sheetPageRoute("sheet_019", 1))
    }

    @Test
    fun `the viewer and page patterns are full-screen`() {
        assertTrue(isFullScreenRoute(SHEET_VIEWER_ROUTE))
        assertTrue(isFullScreenRoute(SHEET_PAGE_ROUTE))
    }

    /** Every tab keeps its navigation bar. */
    @Test
    fun `no tab route is full-screen`() {
        Destination.entries.forEach { destination ->
            assertFalse(destination.route, isFullScreenRoute(destination.route))
        }
    }

    @Test
    fun `lesson detail keeps the navigation bar`() {
        assertFalse(isFullScreenRoute(LESSON_DETAIL_ROUTE))
    }

    @Test
    fun `a null route keeps the navigation bar`() {
        assertFalse(isFullScreenRoute(null))
    }

    /**
     * The sheet tab route is a prefix of the viewer route, so a naive `startsWith`
     * would hide the bar on the list itself.
     */
    @Test
    fun `the sheet tab route itself is not full-screen`() {
        assertFalse(isFullScreenRoute(Destination.SHEET.route))
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `cd android && JAVA_HOME=/Applications/Android\ Studio.app/Contents/jbr/Contents/Home ./gradlew :app:testDebugUnitTest --tests '*SheetRoutesTest*'`
Expected: FAIL — compilation error, `sheetViewerRoute` unresolved.

- [ ] **Step 3: Add the routes**

Append to `android/app/src/main/java/vn/hocdantranh/guzheng/ui/Destinations.kt`:

```kotlin
/** Argument names for the sheet viewer and its page zoom. */
const val ARG_SHEET_ID = "sheetId"
const val ARG_PAGE_INDEX = "pageIndex"

/**
 * The sheet viewer and page zoom are presented full-screen, hiding the bottom bar —
 * iOS shows them in a `fullScreenCover`, which covers the tab bar. This is the opposite
 * of [LESSON_DETAIL_ROUTE], which keeps the bar because iOS pushes it inside the tab's
 * own NavigationStack.
 */
const val SHEET_VIEWER_ROUTE = "sheet/{$ARG_SHEET_ID}"
const val SHEET_PAGE_ROUTE = "sheet/{$ARG_SHEET_ID}/page/{$ARG_PAGE_INDEX}"

fun sheetViewerRoute(sheetId: String): String = "sheet/$sheetId"

fun sheetPageRoute(sheetId: String, pageIndex: Int): String = "sheet/$sheetId/page/$pageIndex"

/**
 * Whether [route] hides the bottom navigation bar.
 *
 * Compares whole route patterns rather than prefixes: `sheet` (the tab) is a prefix of
 * `sheet/{sheetId}` (the viewer), so a `startsWith` test would wrongly hide the bar on the
 * library list itself. Add future full-screen destinations here.
 */
fun isFullScreenRoute(route: String?): Boolean =
    route == SHEET_VIEWER_ROUTE || route == SHEET_PAGE_ROUTE
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `cd android && JAVA_HOME=/Applications/Android\ Studio.app/Contents/jbr/Contents/Home ./gradlew :app:testDebugUnitTest --tests '*SheetRoutesTest*'`
Expected: PASS, 7 tests.

- [ ] **Step 5: Hide the bar in MainScaffold**

In `MainScaffold.kt`, wrap the `NavigationBar` so it renders only outside full-screen routes. Replace the `bottomBar = {` lambda's opening so its whole body is guarded:

```kotlin
        bottomBar = {
            if (!isFullScreenRoute(currentRoute)) {
                NavigationBar(containerColor = VintageColors.paperWarm) {
                    // ... existing Destination.entries.forEach block, unchanged ...
                }
            }
        }
```

- [ ] **Step 6: Verify the existing scaffold tests still pass**

Run: `cd android && JAVA_HOME=/Applications/Android\ Studio.app/Contents/jbr/Contents/Home ./gradlew :app:connectedDebugAndroidTest --tests '*MainScaffoldTest*'`
Expected: PASS — the five tabs still render their bar.

- [ ] **Step 7: Commit**

```bash
git add android/app/src/main/java/vn/hocdantranh/guzheng/ui/Destinations.kt \
        android/app/src/main/java/vn/hocdantranh/guzheng/ui/MainScaffold.kt \
        android/app/src/test/java/vn/hocdantranh/guzheng/ui/SheetRoutesTest.kt
git commit -m "feat(android): add full-screen sheet routes that hide the bottom bar"
```

---

### Task 10: Sheet viewer screen

**Files:**
- Create: `android/app/src/main/java/vn/hocdantranh/guzheng/ui/sheet/SheetViewerScreen.kt`

**Interfaces:**
- Consumes: `SheetItem` (Task 2), `directImageUrl` (Task 1), `BrushDivider` (existing), `R.string.sheet_*` (Task 6)
- Produces: `@Composable fun SheetViewerScreen(sheet: SheetItem, onBack: () -> Unit, onPageClick: (Int) -> Unit)`

- [ ] **Step 1: Write the implementation**

```kotlin
package vn.hocdantranh.guzheng.ui.sheet

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import vn.hocdantranh.guzheng.R
import vn.hocdantranh.guzheng.data.sheet.SheetItem
import vn.hocdantranh.guzheng.data.sheet.directImageUrl
import vn.hocdantranh.guzheng.ui.components.BrushDivider
import vn.hocdantranh.guzheng.ui.components.PaperBackground
import vn.hocdantranh.guzheng.ui.theme.VintageColors

/** All pages of one sheet, stacked vertically. Tapping a page opens the zoom screen. */
@Composable
fun SheetViewerScreen(
    sheet: SheetItem,
    onBack: () -> Unit,
    onPageClick: (Int) -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        PaperBackground()

        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 56.dp, start = 18.dp, end = 18.dp, bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(VintageColors.paperWarm.copy(alpha = 0.9f))
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = stringResource(R.string.sheet_back),
                        tint = VintageColors.inkSoft,
                        modifier = Modifier.size(20.dp),
                    )
                }

                Text(
                    text = sheet.title,
                    color = VintageColors.inkDark,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    fontStyle = FontStyle.Italic,
                    fontFamily = FontFamily.Serif,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            BrushDivider(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 40.dp)
                    .padding(bottom = 8.dp)
            )

            LazyColumn(
                contentPadding = PaddingValues(bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    Text(
                        text = stringResource(R.string.sheet_viewer_hint),
                        color = VintageColors.inkMuted.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp),
                    )
                }

                itemsIndexed(sheet.pages) { index, page ->
                    SheetPage(
                        url = page,
                        pageNumber = index + 1,
                        onClick = { onPageClick(index) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SheetPage(url: String, pageNumber: Int, onClick: () -> Unit) {
    SubcomposeAsyncImage(
        // Raw Drive links serve an HTML page, not bytes — always rewrite first.
        model = directImageUrl(url),
        contentDescription = stringResource(R.string.sheet_page_number, pageNumber),
        contentScale = ContentScale.FillWidth,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        loading = {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .background(VintageColors.paperDeep.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = VintageColors.inkMuted)
            }
        },
        error = {
            // A fixed height, so a failed page stays visible instead of collapsing away.
            Column(
                Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                horizontalArrangement = Arrangement.Center.let { Arrangement.Center },
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    Icons.Filled.WarningAmber,
                    contentDescription = null,
                    tint = VintageColors.inkMuted,
                )
                Text(
                    text = stringResource(R.string.sheet_page_load_failed, pageNumber),
                    color = VintageColors.inkMuted,
                    fontSize = 13.sp,
                )
            }
        },
    )
}
```

Note: the `Column` in the error branch takes `verticalArrangement` and `horizontalAlignment` only — delete the stray `horizontalArrangement` line if the compiler flags it; it is not a valid `Column` parameter.

- [ ] **Step 2: Verify it compiles**

Run: `cd android && JAVA_HOME=/Applications/Android\ Studio.app/Contents/jbr/Contents/Home ./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add android/app/src/main/java/vn/hocdantranh/guzheng/ui/sheet/SheetViewerScreen.kt
git commit -m "feat(android): add the sheet page viewer"
```

---

### Task 11: Page zoom screen, and wire the tab up

Adds Telephoto, the zoom screen, and replaces the Sheet placeholder in the NavHost. Ends with on-device verification of the whole tab.

**Files:**
- Modify: `android/gradle/libs.versions.toml`
- Modify: `android/app/build.gradle.kts`
- Create: `android/app/src/main/java/vn/hocdantranh/guzheng/ui/sheet/SheetPageZoomScreen.kt`
- Modify: `android/app/src/main/java/vn/hocdantranh/guzheng/ui/MainScaffold.kt`

**Interfaces:**
- Consumes: `SheetRoute` (Task 8), `SheetViewerScreen` (Task 10), route helpers (Task 9), `SheetViewModel` (Task 5), `directImageUrl` (Task 1)
- Produces: `@Composable fun SheetPageZoomScreen(url: String, pageNumber: Int, onClose: () -> Unit)`

- [ ] **Step 1: Add the Telephoto dependency**

In `android/gradle/libs.versions.toml`, add to `[versions]`:

```toml
telephoto = "0.15.1"
```

and to `[libraries]`:

```toml
telephoto-zoomable-image-coil = { group = "me.saket.telephoto", name = "zoomable-image-coil", version.ref = "telephoto" }
```

In `android/app/build.gradle.kts`, add below `implementation(libs.coil.compose)`:

```kotlin
    implementation(libs.telephoto.zoomable.image.coil)
```

- [ ] **Step 2: Verify the dependency did not drag the toolchain forward**

Run:
```bash
cd android && JAVA_HOME=/Applications/Android\ Studio.app/Contents/jbr/Contents/Home \
  ./gradlew :app:dependencies --configuration debugRuntimeClasspath | grep -E "compose.foundation:foundation:|coil-compose"
```
Expected: `coil-compose` resolves to **2.7.0**, and `androidx.compose.foundation:foundation` still resolves to the BOM's version (**1.7.6**), shown as `1.7.3 -> 1.7.6`. If foundation resolved to 1.8.0, the Telephoto version is wrong — go back to the spec's version table rather than bumping the BOM.

- [ ] **Step 3: Write the zoom screen**

```kotlin
package vn.hocdantranh.guzheng.ui.sheet

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.saket.telephoto.zoomable.ZoomSpec
import me.saket.telephoto.zoomable.coil.ZoomableAsyncImage
import me.saket.telephoto.zoomable.rememberZoomableImageState
import me.saket.telephoto.zoomable.rememberZoomableState
import vn.hocdantranh.guzheng.R
import vn.hocdantranh.guzheng.data.sheet.directImageUrl

/**
 * One page, full-screen on black.
 *
 * Telephoto supplies the pinch, pan-with-edge-clamping, fling and double-tap-to-reset that
 * iOS hand-rolls with gesture recognisers; [ZoomSpec] caps the zoom at the same 6x.
 */
@Composable
fun SheetPageZoomScreen(url: String, pageNumber: Int, onClose: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        val zoomableState = rememberZoomableState(zoomSpec = ZoomSpec(maxZoomFactor = 6f))

        ZoomableAsyncImage(
            // Raw Drive links serve an HTML page, not bytes — always rewrite first.
            model = directImageUrl(url),
            contentDescription = stringResource(R.string.sheet_page_number, pageNumber),
            contentScale = ContentScale.Fit,
            state = rememberZoomableImageState(zoomableState),
            modifier = Modifier.fillMaxSize(),
        )

        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 56.dp, start = 18.dp, end = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.18f))
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = stringResource(R.string.sheet_zoom_close),
                    tint = Color.White,
                    modifier = Modifier.size(16.dp),
                )
            }

            Box(Modifier.weight(1f))

            Text(
                text = stringResource(R.string.sheet_page_number, pageNumber),
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 13.sp,
            )
        }

        Column(
            Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.sheet_zoom_hint),
                color = Color.White.copy(alpha = 0.35f),
                fontSize = 11.sp,
            )
        }
    }
}
```

- [ ] **Step 4: Wire the three routes into MainScaffold**

In `MainScaffold.kt`, add these imports:

```kotlin
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.getBackStackEntry
import androidx.compose.ui.platform.LocalContext
import vn.hocdantranh.guzheng.ui.sheet.SheetPageZoomScreen
import vn.hocdantranh.guzheng.ui.sheet.SheetRoute
import vn.hocdantranh.guzheng.ui.sheet.SheetViewModel
import vn.hocdantranh.guzheng.ui.sheet.SheetViewerScreen
```

Replace the placeholder `forEach` block so `SHEET` is no longer a placeholder, and add the two full-screen routes. The viewer and zoom read the sheet from the ViewModel scoped to the **sheet tab's** back-stack entry, so neither refetches from Firestore:

```kotlin
            composable(Destination.SHEET.route) {
                SheetRoute(
                    onSheetClick = { sheet ->
                        navController.navigate(sheetViewerRoute(sheet.id))
                    }
                )
            }

            composable(
                route = SHEET_VIEWER_ROUTE,
                arguments = listOf(navArgument(ARG_SHEET_ID) { type = NavType.StringType }),
            ) { entry ->
                val sheetId = entry.arguments?.getString(ARG_SHEET_ID)
                val viewModel = sheetTabViewModel(navController, entry)
                val state by viewModel.uiState.collectAsState()
                val sheet = sheetId?.let(state::sheetById)

                // Deep link to an id the catalog does not hold: pop rather than show nothing.
                LaunchedEffect(sheet, state.isLoading) {
                    if (sheet == null && !state.isLoading) navController.popBackStack()
                }

                if (sheet != null) {
                    SheetViewerScreen(
                        sheet = sheet,
                        onBack = { navController.popBackStack() },
                        onPageClick = { index ->
                            navController.navigate(sheetPageRoute(sheet.id, index))
                        },
                    )
                }
            }

            composable(
                route = SHEET_PAGE_ROUTE,
                arguments = listOf(
                    navArgument(ARG_SHEET_ID) { type = NavType.StringType },
                    navArgument(ARG_PAGE_INDEX) { type = NavType.IntType },
                ),
            ) { entry ->
                val sheetId = entry.arguments?.getString(ARG_SHEET_ID)
                val pageIndex = entry.arguments?.getInt(ARG_PAGE_INDEX) ?: 0
                val viewModel = sheetTabViewModel(navController, entry)
                val state by viewModel.uiState.collectAsState()
                val page = sheetId?.let(state::sheetById)?.pages?.getOrNull(pageIndex)

                LaunchedEffect(page, state.isLoading) {
                    if (page == null && !state.isLoading) navController.popBackStack()
                }

                if (page != null) {
                    SheetPageZoomScreen(
                        url = page,
                        pageNumber = pageIndex + 1,
                        onClose = { navController.popBackStack() },
                    )
                }
            }
```

Then keep the placeholder loop for the tabs that are still unbuilt:

```kotlin
            Destination.entries
                .filterNot {
                    it == Destination.LESSONS ||
                        it == Destination.VIDEO ||
                        it == Destination.SHEET
                }
                .forEach { destination ->
                    composable(destination.route) {
                        PlaceholderScreen(title = stringResource(destination.labelRes))
                    }
                }
```

Add this helper at the bottom of `MainScaffold.kt`:

```kotlin
/**
 * The [SheetViewModel] owned by the sheet tab's back-stack entry.
 *
 * Scoping it to the tab rather than to each destination means the viewer and the zoom
 * screen reuse the catalog the list already loaded, instead of each running its own
 * Firestore query.
 */
@Composable
private fun sheetTabViewModel(
    navController: NavHostController,
    entry: NavBackStackEntry,
): SheetViewModel {
    val context = LocalContext.current
    val parentEntry = remember(entry) {
        navController.getBackStackEntry(Destination.SHEET.route)
    }
    return viewModel(
        viewModelStoreOwner = parentEntry,
        factory = SheetViewModel.factory(context),
    )
}
```

This needs two more imports:

```kotlin
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
```

- [ ] **Step 5: Build and run every test**

Run:
```bash
cd android && JAVA_HOME=/Applications/Android\ Studio.app/Contents/jbr/Contents/Home \
  ./gradlew :app:testDebugUnitTest :app:assembleDebug
```
Expected: BUILD SUCCESSFUL, all unit tests pass.

- [ ] **Step 6: Verify on the device**

Run `adb devices` first; re-pair the OPPO CPH2363 if it dropped. Then install and drive the tab:

```bash
cd android && JAVA_HOME=/Applications/Android\ Studio.app/Contents/jbr/Contents/Home \
  ./gradlew :app:installDebug
adb shell am start -n vn.hocdantranh.guzheng/.MainActivity
```

Confirm each of these, taking a screenshot of the list and the viewer:

- All 19 sheets load, `Haru Haru` topmost (order `-1`), `Chúc Tết` last (order `17`)
- Every thumbnail is a real image, not a blank Drive page — this is the Drive-rewrite check
- Searching `nhỏ` narrows to `Nhỏ ơi`; the clear button restores the list
- Opening `Đoản xuân ca` shows **both** its pages
- The bottom navigation bar is gone inside the viewer, and back restores it
- Tapping a page opens the zoom screen; pinch, pan and double-tap-to-reset all work
- Kill the app, enable airplane mode, relaunch: the cached list still paints

- [ ] **Step 7: Commit**

```bash
git add android/gradle/libs.versions.toml android/app/build.gradle.kts \
        android/app/src/main/java/vn/hocdantranh/guzheng/ui/sheet/SheetPageZoomScreen.kt \
        android/app/src/main/java/vn/hocdantranh/guzheng/ui/MainScaffold.kt
git commit -m "feat(android): add page zoom and wire the Sheet tab into the shell"
```

---

## Self-Review

**Spec coverage**

| Spec section | Task |
|---|---|
| `SheetItem` + `sheetFromFields` | 2 |
| `directImageUrl` | 1 |
| `SheetRepository` | 4 |
| `SheetCache` | 3 |
| `SheetUiState` + ViewModel, cache-then-network | 5 |
| `SheetScreen` header / search / 4 content states | 8 |
| `SheetCard` + skeleton | 7 |
| Navigation + bottom-bar hiding | 9 |
| `SheetViewerScreen` | 10 |
| `SheetPageZoomScreen` + Telephoto | 11 |
| Localization | 6 |
| Dependency-drift verification | 11 step 2 |
| Device verification | 11 step 6 |

Every spec section maps to a task. Both spec-listed divergences from iOS (translated hardcoded strings, error suppressed when sheets are already on screen) are implemented in Tasks 6 and 5 respectively.

**Naming consistency check**

`directImageUrl` (T1) → used T7, T10, T11. `SheetItem`/`sheetFromFields` (T2) → T3, T4, T5. `encodeSheets`/`decodeSheets`/`SheetCache.read`/`.write` (T3) → T5. `SheetRepository.load` (T4) → T5. `SheetUiState.filtered`/`.sheetById`/`SheetViewModel.refresh`/`.search`/`.factory(context)` (T5) → T8, T11. `SheetCard`/`SheetSkeletonCard` (T7) → T8. `SheetRoute` (T8) → T11. Route helpers + `isFullScreenRoute` (T9) → T11 and `MainScaffold`. `SheetViewerScreen` (T10), `SheetPageZoomScreen` (T11) → `MainScaffold`. All consistent.

**Known follow-ups (not in this plan)**

- The Settings tab, the next cycle.
- The Tuner tab, explicitly deferred by the user.
- Play Billing, which would turn the cosmetic lock into a real gate.
