package vn.hocdantranh.guzheng.ui.lessons

import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import vn.hocdantranh.guzheng.data.lesson.LessonCatalog
import vn.hocdantranh.guzheng.data.lesson.LessonProgressPrefs
import vn.hocdantranh.guzheng.data.prefs.dataStore

@RunWith(RobolectricTestRunner::class)
class LessonsViewModelTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val dispatcher = StandardTestDispatcher()

    private fun viewModel() = LessonsViewModel(LessonProgressPrefs(context))

    @Before
    fun setUp() = runTest {
        Dispatchers.setMain(dispatcher)
        context.dataStore.edit { it.clear() }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `exposes the whole catalog`() = runTest(dispatcher) {
        assertEquals(LessonCatalog.lessons, viewModel().uiState.value.lessons)
    }

    private suspend fun opened(): Set<Int> =
        LessonProgressPrefs(context).openedLessonNumbers.first()

    // markOpened's write suspends into DataStore's own IO scope, so draining the test scheduler
    // proves nothing — joining the returned Job is what actually waits for the write to land.
    @Test
    fun `records a free lesson as opened`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.markOpened(LessonCatalog.lessons.first { it.isFree })?.join()

        assertEquals(setOf(1), opened())
    }

    /**
     * Opens the locked lesson first, then a free one, and joins both. Once lesson 1 is visible
     * any write for lesson 7 would be too — so the absence of 7 is real evidence the guard held,
     * rather than a read that simply arrived too early.
     */
    @Test
    fun `does not record a locked lesson as opened`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.markOpened(LessonCatalog.lessons.first { !it.isFree })?.join()
        vm.markOpened(LessonCatalog.lessons.first { it.isFree })?.join()

        assertEquals(setOf(1), opened())
    }

    @Test
    fun `returns no job for a locked lesson`() = runTest(dispatcher) {
        assertNull(viewModel().markOpened(LessonCatalog.lessons.first { !it.isFree }))
    }
}
