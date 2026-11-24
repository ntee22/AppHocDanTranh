package vn.hocdantranh.guzheng.data.lesson

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import vn.hocdantranh.guzheng.FeatureFlags

/** Pins the gating table in docs/superpowers/specs/2026-11-24-tuner-and-paid-lessons-design.md. */
class LessonAccessTest {

    private fun lesson(free: Boolean, released: Boolean) = Lesson(
        lessonNumber = 99,
        title = "t",
        subtitle = "s",
        thumbnailUrl = "",
        videoId = if (released) "abc" else "",
        isFree = free,
        duration = "1:00",
    )

    @Test
    fun `unreleased lessons are coming soon whatever the tier or ownership`() {
        for (free in listOf(true, false)) for (iap in listOf(true, false)) for (owns in listOf(true, false)) {
            assertEquals(
                LessonAccess.COMING_SOON,
                LessonAccess.resolve(lesson(free, released = false), iap, owns),
            )
        }
    }

    @Test
    fun `released free lessons are playable`() {
        assertEquals(LessonAccess.FREE, LessonAccess.resolve(lesson(true, true), false, false))
    }

    @Test
    fun `released paid lessons read coming soon while selling is off, even if owned`() {
        assertEquals(LessonAccess.COMING_SOON, LessonAccess.resolve(lesson(false, true), false, false))
        assertEquals(LessonAccess.COMING_SOON, LessonAccess.resolve(lesson(false, true), false, true))
    }

    @Test
    fun `released paid lessons lock or unlock once selling is on`() {
        assertEquals(LessonAccess.LOCKED, LessonAccess.resolve(lesson(false, true), true, false))
        assertEquals(LessonAccess.PURCHASED, LessonAccess.resolve(lesson(false, true), true, true))
    }

    /**
     * The release guard: with the shipped flag, nothing in the real catalog may reach the
     * LOCKED state — that is the only state that shows a price or a buy button.
     */
    @Test
    fun `shipped flag shows no purchase entry anywhere in the catalog`() {
        LessonCatalog.lessons.forEach { l ->
            for (owns in listOf(true, false)) {
                val access = LessonAccess.resolve(l, FeatureFlags.IAP_ENABLED, owns)
                if (!FeatureFlags.IAP_ENABLED) {
                    assertFalse("Lesson ${l.lessonNumber} would show a purchase entry", access == LessonAccess.LOCKED)
                }
            }
        }
    }

    @Test
    fun `today lessons seven to sixteen all read coming soon`() {
        LessonCatalog.lessons.filter { it.lessonNumber >= 7 }.forEach { l ->
            assertEquals(LessonAccess.COMING_SOON, LessonAccess.resolve(l, FeatureFlags.IAP_ENABLED, false))
        }
    }
}
