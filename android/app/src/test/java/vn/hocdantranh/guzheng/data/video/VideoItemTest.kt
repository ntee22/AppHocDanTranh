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
