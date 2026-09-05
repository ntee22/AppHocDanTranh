package vn.hocdantranh.guzheng.data.sheet

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SheetItemTest {

    private fun fields(
        title: Any? = "Haru Haru",
        pages: Any? = listOf("https://drive.google.com/file/d/A/view"),
        isFree: Any? = true,
        isVisible: Any? = true,
        order: Any? = -1L,
    ): Map<String, Any?> = buildMap {
        put("title", title)
        put("pages", pages)
        put("isFree", isFree)
        put("isVisible", isVisible)
        put("order", order)
    }

    @Test
    fun `maps a valid document`() {
        val item = sheetFromFields("sheet_019", fields())
        assertEquals("sheet_019", item?.id)
        assertEquals("Haru Haru", item?.title)
        assertEquals(1, item?.pages?.size)
        assertEquals(-1, item?.order)
        assertEquals(true, item?.isFree)
    }

    /** Firestore hands integers back as Long; `as? Int` would silently drop every doc. */
    @Test
    fun `parses a Long order`() {
        assertEquals(17, sheetFromFields("s", fields(order = 17L))?.order)
    }

    /** Live orders run -1..17 — newest sheets sit on non-positive orders. */
    @Test
    fun `parses a negative order`() {
        assertEquals(-1, sheetFromFields("s", fields(order = -1L))?.order)
    }

    @Test
    fun `parses a zero order`() {
        assertEquals(0, sheetFromFields("s", fields(order = 0L))?.order)
    }

    @Test
    fun `drops a hidden document`() {
        assertNull(sheetFromFields("s", fields(isVisible = false)))
    }

    @Test
    fun `drops a document with no title`() {
        assertNull(sheetFromFields("s", fields(title = null)))
    }

    @Test
    fun `drops a document with no pages`() {
        assertNull(sheetFromFields("s", fields(pages = null)))
    }

    @Test
    fun `drops a document with no order`() {
        assertNull(sheetFromFields("s", fields(order = null)))
    }

    @Test
    fun `drops a document whose pages are not strings`() {
        assertNull(sheetFromFields("s", fields(pages = listOf(1, 2))))
    }

    @Test
    fun `treats a missing isFree as free, matching iOS`() {
        val item = sheetFromFields("s", fields().minus("isFree"))
        assertTrue(item!!.isFree)
    }

    @Test
    fun `treats a missing isVisible as visible`() {
        assertEquals("s", sheetFromFields("s", fields().minus("isVisible"))?.id)
    }

    @Test
    fun `reads a premium document`() {
        assertEquals(false, sheetFromFields("s", fields(isFree = false))?.isFree)
    }
}
