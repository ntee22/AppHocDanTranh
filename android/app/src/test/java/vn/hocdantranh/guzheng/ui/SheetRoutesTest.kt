package vn.hocdantranh.guzheng.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SheetRoutesTest {

    @Test
    fun `builds a viewer route`() {
        assertEquals("sheet/sheet_019", sheetViewerRoute("sheet_019"))
    }

    @Test
    fun `builds a page route`() {
        assertEquals("sheet/sheet_019/page/1", sheetPageRoute("sheet_019", 1))
    }

    @Test
    fun `the viewer and page patterns are full-screen`() {
        assertTrue(isFullScreenRoute(SHEET_VIEWER_ROUTE))
        assertTrue(isFullScreenRoute(SHEET_PAGE_ROUTE))
    }

    /** Every tab keeps its navigation bar. */
    @Test
    fun `no tab route is full-screen`() {
        Destination.entries.forEach { destination ->
            assertFalse(destination.route, isFullScreenRoute(destination.route))
        }
    }

    @Test
    fun `lesson detail keeps the navigation bar`() {
        assertFalse(isFullScreenRoute(LESSON_DETAIL_ROUTE))
    }

    @Test
    fun `a null route keeps the navigation bar`() {
        assertFalse(isFullScreenRoute(null))
    }

    /**
     * The sheet tab route is a prefix of the viewer route, so a naive `startsWith`
     * would hide the bar on the list itself.
     */
    @Test
    fun `the sheet tab route itself is not full-screen`() {
        assertFalse(isFullScreenRoute(Destination.SHEET.route))
    }
}
