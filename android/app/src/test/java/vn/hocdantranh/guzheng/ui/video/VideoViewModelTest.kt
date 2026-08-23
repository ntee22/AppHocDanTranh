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
        items = range.map { item("$it") },
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
