package vn.hocdantranh.guzheng.data.lesson

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import vn.hocdantranh.guzheng.data.prefs.dataStore

@RunWith(RobolectricTestRunner::class)
class LessonProgressPrefsTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val progress = LessonProgressPrefs(context)

    // Same reasoning as AppPrefsTest: `Context.dataStore` is a top-level singleton whose
    // in-memory cache outlives individual @Test methods under Robolectric, so the store has to
    // be cleared through that same singleton before each test rather than deleted from disk.
    @Before
    fun setUp() = runTest {
        context.dataStore.edit { it.clear() }
    }

    @Test
    fun `starts with no lessons opened`() = runTest {
        assertEquals(emptySet<Int>(), progress.openedLessonNumbers.first())
    }

    @Test
    fun `remembers an opened lesson`() = runTest {
        progress.markOpened(3)
        assertEquals(setOf(3), progress.openedLessonNumbers.first())
    }

    @Test
    fun `accumulates lessons opened out of order`() = runTest {
        progress.markOpened(4)
        progress.markOpened(1)
        assertEquals(setOf(1, 4), progress.openedLessonNumbers.first())
    }

    @Test
    fun `opening the same lesson twice changes nothing`() = runTest {
        progress.markOpened(2)
        progress.markOpened(2)
        assertEquals(setOf(2), progress.openedLessonNumbers.first())
    }

    @Test
    fun `ignores stored values that are not lesson numbers`() = runTest {
        context.dataStore.edit { prefs ->
            prefs[stringSetPreferencesKey("openedLessonNumbers")] = setOf("1", "", "banana", "5")
        }
        assertEquals(setOf(1, 5), progress.openedLessonNumbers.first())
    }

    @Test
    fun `clears all progress`() = runTest {
        progress.markOpened(1)
        progress.markOpened(2)
        progress.reset()
        assertEquals(emptySet<Int>(), progress.openedLessonNumbers.first())
    }
}
