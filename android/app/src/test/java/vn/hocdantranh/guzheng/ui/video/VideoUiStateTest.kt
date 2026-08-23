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
