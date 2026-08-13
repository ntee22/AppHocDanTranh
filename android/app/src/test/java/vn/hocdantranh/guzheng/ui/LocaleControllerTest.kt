package vn.hocdantranh.guzheng.ui

import androidx.appcompat.app.AppCompatActivity
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
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

    // Everything above runs on the class-level @Config(sdk = 30) override, which only
    // exercises AppCompatDelegate's legacy static-field path. This test overrides back
    // to the project's real pinned sdk=35 and gives AppCompat an actual Activity-bound
    // delegate (via Robolectric.buildActivity(...).setup()) so
    // AppCompatDelegate.getLocaleManagerForApplication() finds a non-null delegate and
    // routes through the real android.app.LocaleManager / ShadowLocaleManager — the
    // code path real API 33+ users hit. Without this, a regression on the modern path
    // could ship silently while the suite stayed green.
    @Config(sdk = [35])
    @Test
    fun `applying English is readable back through the real API 33+ LocaleManager path`() {
        Robolectric.buildActivity(AppCompatActivity::class.java).setup()

        LocaleController.apply(AppPrefs.EN)

        assertEquals("en", LocaleController.currentLanguage())
    }
}
