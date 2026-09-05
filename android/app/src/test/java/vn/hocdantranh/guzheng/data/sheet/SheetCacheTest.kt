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
