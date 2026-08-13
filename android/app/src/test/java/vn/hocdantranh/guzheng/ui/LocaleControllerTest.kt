package vn.hocdantranh.guzheng.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import vn.hocdantranh.guzheng.data.prefs.AppPrefs

// AppCompatDelegate's real per-app-language API (SDK 33+) only works through an
// Activity-bound delegate registered in its internal registry — there is none in a
// plain unit test, so on the project's pinned Robolectric sdk=35,
// setApplicationLocales()/getApplicationLocales() silently no-op. Below API 33,
// AppCompatDelegate stores the requested locales in its own static field instead,
// which is exactly what this controller needs to be unit-testable. This @Config
// override is local to this test class; it does not touch the global
// robolectric.properties pin used by the rest of the suite.
@Config(sdk = [30])
@RunWith(RobolectricTestRunner::class)
class LocaleControllerTest {

    @Test
    fun `unset locale reports the Vietnamese default`() {
        LocaleController.apply("")
        assertEquals(AppPrefs.DEFAULT, LocaleController.currentLanguage())
    }

    @Test
    fun `applying English is readable back`() {
        LocaleController.apply(AppPrefs.EN)
        assertEquals("en", LocaleController.currentLanguage())
    }

    @Test
    fun `applying Vietnamese is readable back`() {
        LocaleController.apply(AppPrefs.EN)
        LocaleController.apply(AppPrefs.VI)
        assertEquals("vi", LocaleController.currentLanguage())
    }
}
