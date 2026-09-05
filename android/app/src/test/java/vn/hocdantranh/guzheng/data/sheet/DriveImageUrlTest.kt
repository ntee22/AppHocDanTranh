package vn.hocdantranh.guzheng.data.sheet

import org.junit.Assert.assertEquals
import org.junit.Test

class DriveImageUrlTest {

    /** The exact shape of all 22 page URLs in the live `sheets` collection. */
    @Test
    fun `rewrites a live file-d share link`() {
        assertEquals(
            "https://lh3.googleusercontent.com/d/1aM8SnLS0B6z6O55TS_26ARnygqAhS8LQ",
            directImageUrl(
                "https://drive.google.com/file/d/1aM8SnLS0B6z6O55TS_26ARnygqAhS8LQ/view?usp=drive_link"
            )
        )
    }

    @Test
    fun `rewrites a usp-sharing link`() {
        assertEquals(
            "https://lh3.googleusercontent.com/d/1kpS9omXRY2gd61PHTBpj0M5Mj6YcFbPu",
            directImageUrl(
                "https://drive.google.com/file/d/1kpS9omXRY2gd61PHTBpj0M5Mj6YcFbPu/view?usp=sharing"
            )
        )
    }

    @Test
    fun `rewrites an id query-parameter link`() {
        assertEquals(
            "https://lh3.googleusercontent.com/d/ABC123",
            directImageUrl("https://drive.google.com/uc?id=ABC123&export=download")
        )
    }

    @Test
    fun `rewrites a bare d-slash link`() {
        assertEquals(
            "https://lh3.googleusercontent.com/d/XYZ789",
            directImageUrl("https://drive.google.com/d/XYZ789/")
        )
    }

    @Test
    fun `passes a non-Drive url through untouched`() {
        val url = "https://example.com/sheet.png"
        assertEquals(url, directImageUrl(url))
    }

    /** Degrade to a broken image, never a crash. */
    @Test
    fun `passes a Drive url with no extractable id through untouched`() {
        val url = "https://drive.google.com/drive/my-drive"
        assertEquals(url, directImageUrl(url))
    }

    @Test
    fun `passes an empty string through untouched`() {
        assertEquals("", directImageUrl(""))
    }
}
