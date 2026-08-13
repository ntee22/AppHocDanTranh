package vn.hocdantranh.guzheng.data.prefs

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AppPrefsTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val prefs = AppPrefs(context)

    // NOTE: deviates from the brief, which deletes the on-disk "datastore" dir directly.
    // `Context.dataStore` is a top-level singleton delegate; Robolectric reuses the same
    // sandbox/classloader across all @Test methods in this class, so that singleton (and its
    // in-memory cache) survives between tests. Deleting the file bypasses the cache, so a value
    // written by an earlier test kept leaking into "defaults to Vietnamese" depending on method
    // execution order (confirmed via JUnit XML: order was switches-back -> rejects-unknown ->
    // defaults-to-Vietnamese -> persists-English, so it saw the stale "en" from rejects-unknown).
    // Resetting through the public API goes through the same cached instance and actually clears it.
    @After
    fun tearDown() = runTest {
        prefs.setLanguage(AppPrefs.VI)
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
