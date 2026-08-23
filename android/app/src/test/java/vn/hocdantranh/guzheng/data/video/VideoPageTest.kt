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
