package vn.hocdantranh.guzheng.ui.sheet

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import vn.hocdantranh.guzheng.data.sheet.SheetItem

class SheetUiStateTest {

    private val sheets = listOf(
        SheetItem("1", "Nhỏ ơi", listOf("a"), isFree = true, order = 11),
        SheetItem("2", "Hồn quê", listOf("b"), isFree = true, order = 13),
        SheetItem("3", "Haru Haru", listOf("c", "d"), isFree = false, order = -1),
    )

    private fun state(query: String = "") = SheetUiState(sheets = sheets, query = query)

    @Test
    fun `a blank query returns everything`() {
        assertEquals(3, state().filtered.size)
    }

    @Test
    fun `matches a title substring`() {
        assertEquals(listOf("Nhỏ ơi"), state("nhỏ").filtered.map { it.title })
    }

    @Test
    fun `matches case-insensitively`() {
        assertEquals(listOf("Haru Haru"), state("HARU").filtered.map { it.title })
    }

    /** iOS uses localizedCaseInsensitiveContains, which folds case but not diacritics. */
    @Test
    fun `does not fold diacritics`() {
        assertTrue(state("nho").filtered.isEmpty())
    }

    @Test
    fun `returns empty for a query that matches nothing`() {
        assertTrue(state("zzz").filtered.isEmpty())
    }

    @Test
    fun `ignores surrounding whitespace`() {
        assertEquals(listOf("Hồn quê"), state("  hồn  ").filtered.map { it.title })
    }

    @Test
    fun `finds a sheet by id`() {
        assertEquals("Hồn quê", state().sheetById("2")?.title)
    }

    @Test
    fun `returns null for an unknown id`() {
        assertNull(state().sheetById("nope"))
    }
}
