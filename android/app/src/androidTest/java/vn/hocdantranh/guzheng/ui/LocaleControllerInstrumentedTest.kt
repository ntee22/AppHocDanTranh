package vn.hocdantranh.guzheng.ui

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import vn.hocdantranh.guzheng.MainActivity
import vn.hocdantranh.guzheng.data.prefs.AppPrefs

/**
 * Verifies the in-app locale switch on a REAL device/emulator, not under Robolectric.
 *
 * [LocaleControllerTest] (in src/test) covers this under Robolectric shadows, which only
 * approximate real API 33+ AppCompatDelegate/LocaleManager behavior. Here there is no
 * shadow: LocaleController.apply() drives the real per-app language API on the actual OS.
 *
 * Root-caused via the AppCompat 1.7.0 bytecode (androidx.appcompat.app.AppCompatDelegate):
 * on API 33+, both setApplicationLocales() and getApplicationLocales() route through
 * getLocaleManagerForApplication(), which walks the static sActivityDelegates set and
 * returns null — silently making apply() a no-op — until at least one AppCompatDelegate
 * (i.e. an AppCompatActivity) has been created in the process. This is true on a real
 * device exactly as much as under Robolectric; it is not a shadow artifact. The shipped
 * app only ever calls LocaleController.apply() from MainActivity.onCreate(), by which
 * point its own delegate is already registered, so this is not reachable in production —
 * but a standalone instrumented test with no launched Activity would silently no-op and
 * always "pass" without exercising anything, which is worse than not testing at all.
 *
 * So this test launches the real MainActivity via ActivityScenario first. To avoid a race
 * with MainActivity's own startup coroutine (which reads AppPrefs and calls
 * LocaleController.apply() asynchronously once its delegate is ready), both the seeding
 * apply(), the target apply(), and the assertion run inside a single
 * `scenario.onActivity { }` block: that whole block executes as one atomic, non-suspending
 * unit on the main thread, so no other posted main-thread work (including the coroutine's
 * resumed continuation) can interleave partway through it — whichever finishes first is
 * simply overwritten or ignored, and the in-block apply()+assert pair is always consistent.
 */
@RunWith(AndroidJUnit4::class)
class LocaleControllerInstrumentedTest {

    @Test
    fun applyingVietnameseIsReadableBackOnRealDevice() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity {
                LocaleController.apply(AppPrefs.EN)

                LocaleController.apply(AppPrefs.VI)

                assertEquals("vi", LocaleController.currentLanguage())
            }
        }
    }

    @Test
    fun applyingEnglishIsReadableBackOnRealDevice() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity {
                LocaleController.apply(AppPrefs.VI)

                LocaleController.apply(AppPrefs.EN)

                assertEquals("en", LocaleController.currentLanguage())
            }
        }
    }
}
