package vn.hocdantranh.guzheng.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ColorTest {

    private fun assertHex(expected: String, actual: Color) {
        val expectedColor = Color(android.graphics.Color.parseColor("#$expected"))
        assertEquals(expectedColor.value, actual.value)
    }

    @Test
    fun `paper tones match iOS AppColors`() {
        assertHex("F2E8D5", VintageColors.paper)
        assertHex("FFF8EB", VintageColors.paperWarm)
        assertHex("E8DCC0", VintageColors.paperDeep)
    }

    @Test
    fun `ink tones match iOS AppColors`() {
        assertHex("3A3228", VintageColors.inkDark)
        assertHex("5C4E3D", VintageColors.inkSoft)
        assertHex("8A7B66", VintageColors.inkMuted)
    }

    @Test
    fun `sage tones match iOS AppColors`() {
        assertHex("8A9A7B", VintageColors.sage)
        assertHex("6B7C5E", VintageColors.sageDark)
        assertHex("B5C2A6", VintageColors.sageSoft)
    }

    @Test
    fun `wood tones match iOS AppColors`() {
        assertHex("B8956A", VintageColors.wood)
        assertHex("8B6F4E", VintageColors.woodDark)
        assertHex("D4B896", VintageColors.woodSoft)
    }

    @Test
    fun `accent tones match iOS AppColors`() {
        assertHex("A65F5F", VintageColors.plum)
        assertHex("C48888", VintageColors.plumSoft)
        assertHex("C9A961", VintageColors.vintageGold)
        assertHex("B84A3E", VintageColors.sealRed)
    }
}
