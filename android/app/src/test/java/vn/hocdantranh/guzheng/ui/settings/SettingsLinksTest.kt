package vn.hocdantranh.guzheng.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsLinksTest {

    @Test
    fun `builds the play store app url from the real package`() {
        assertEquals(
            "market://details?id=vn.hocdantranh.guzheng",
            SettingsLinks.playStoreAppUrl()
        )
    }

    @Test
    fun `builds the play store web url from the real package`() {
        assertEquals(
            "https://play.google.com/store/apps/details?id=vn.hocdantranh.guzheng",
            SettingsLinks.playStoreWebUrl()
        )
    }

    /** The package here must track applicationId in build.gradle.kts. */
    @Test
    fun `package matches the application id`() {
        assertEquals("vn.hocdantranh.guzheng", SettingsLinks.PACKAGE)
    }

    @Test
    fun `every social link is https`() {
        listOf(
            SettingsLinks.FACEBOOK,
            SettingsLinks.TIKTOK,
            SettingsLinks.YOUTUBE,
            SettingsLinks.SHOPEE,
        ).forEach { assertTrue(it, it.startsWith("https://")) }
    }

    @Test
    fun `email is a bare address, not a mailto url`() {
        assertTrue(SettingsLinks.EMAIL, !SettingsLinks.EMAIL.startsWith("mailto:"))
        assertTrue(SettingsLinks.EMAIL, SettingsLinks.EMAIL.contains("@"))
    }
}
