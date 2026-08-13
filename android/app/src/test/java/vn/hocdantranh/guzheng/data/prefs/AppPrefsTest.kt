package vn.hocdantranh.guzheng.data.prefs

import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AppPrefsTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val prefs = AppPrefs(context)

    // NOTE: deviates from the brief, which deletes the on-disk "datastore" dir directly in an
    // @After. `Context.dataStore` is a top-level singleton delegate; Robolectric reuses the same
    // sandbox/classloader across all @Test methods in this class, so that singleton (and its
    // in-memory cache) survives between tests regardless of method order. Deleting the on-disk
    // file never touches that in-memory cache, so a post-hoc @After can't produce a genuinely
    // key-absent state for the *next* test. Instead, force an empty backing store immediately
    // before each test body via @Before, going through the same cached singleton (made
    // `internal` in AppPrefs.kt so this test source set can reach it) so the clear is real.
    @Before
    fun setUp() = runTest {
        context.dataStore.edit { it.clear() }
    }

    @Test
    fun `defaults to Vietnamese`() = runTest {
        assertEquals("vi", prefs.language.first())
    }

    @Test
    fun `persists English selection`() = runTest {
        prefs.setLanguage(AppPrefs.EN)
        assertEquals("en", prefs.language.first())
    }

    @Test
    fun `switches back to Vietnamese`() = runTest {
        prefs.setLanguage(AppPrefs.EN)
        prefs.setLanguage(AppPrefs.VI)
        assertEquals("vi", prefs.language.first())
    }

    @Test
    fun `rejects unknown language codes and keeps current value`() = runTest {
        prefs.setLanguage(AppPrefs.EN)
        prefs.setLanguage("fr")
        assertEquals("en", prefs.language.first())
    }
}
