package vn.hocdantranh.guzheng.data.video

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VideoThumbnailsTest {

    @Test
    fun `extracts the id from a standard watch url`() {
        assertEquals("dQw4w9WgXcQ", extractYouTubeId("https://www.youtube.com/watch?v=dQw4w9WgXcQ"))
    }

    @Test
    fun `extracts the id from a short url and drops the si suffix`() {
        assertEquals(
            "VGqJwQPjA3g",
            extractYouTubeId("https://youtu.be/VGqJwQPjA3g?si=bUa_4EuFM-eeDlkM"),
        )
    }

    @Test
    fun `extracts the id from a shorts url`() {
        assertEquals("abcdefghijk", extractYouTubeId("https://www.youtube.com/shorts/abcdefghijk"))
    }

    @Test
    fun `returns null for a url with no id`() {
        assertNull(extractYouTubeId("https://www.tiktok.com/@danthanhdantranh/video/7596276865115966727"))
    }

    @Test
    fun `returns null when the id is too short`() {
        assertNull(extractYouTubeId("https://youtu.be/tooshort"))
    }

    @Test
    fun `builds the youtube thumbnail url`() {
        assertEquals(
            "https://img.youtube.com/vi/VGqJwQPjA3g/mqdefault.jpg",
            youTubeThumbnailUrl("VGqJwQPjA3g"),
        )
    }
}
