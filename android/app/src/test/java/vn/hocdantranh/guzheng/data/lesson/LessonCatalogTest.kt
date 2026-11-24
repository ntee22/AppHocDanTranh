package vn.hocdantranh.guzheng.data.lesson

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The catalog is the Android mirror of iOS `Lesson.mockLessons()`.
 * These assertions pin the invariants the UI relies on: released lessons have a video to
 * play, and the free/paid split matches the course Kelvin plans to sell (7 free, 8–16 paid).
 */
class LessonCatalogTest {

    private val lessons = LessonCatalog.lessons

    @Test
    fun `ships sixteen lessons numbered in order`() {
        assertEquals((1..16).toList(), lessons.map { it.lessonNumber })
    }

    @Test
    fun `lessons one to seven are free and eight to sixteen are paid`() {
        assertEquals((1..7).toList(), lessons.filter { it.isFree }.map { it.lessonNumber })
        assertEquals((8..16).toList(), lessons.filterNot { it.isFree }.map { it.lessonNumber })
    }

    @Test
    fun `only the first six are released`() {
        assertEquals((1..6).toList(), lessons.filter { it.isReleased }.map { it.lessonNumber })
    }

    @Test
    fun `every released lesson has a playable YouTube video and a duration`() {
        lessons.filter { it.isReleased }.forEach { lesson ->
            assertTrue("Lesson ${lesson.lessonNumber} is not marked isYouTube", lesson.isYouTube)
            assertTrue("Lesson ${lesson.lessonNumber} has a blank duration", lesson.duration.isNotBlank())
        }
    }

    @Test
    fun `every lesson has a title and subtitle`() {
        lessons.forEach { lesson ->
            assertTrue("Lesson ${lesson.lessonNumber} has a blank title", lesson.title.isNotBlank())
            assertTrue("Lesson ${lesson.lessonNumber} has a blank subtitle", lesson.subtitle.isNotBlank())
        }
    }

    @Test
    fun `released lessons carry the teaching content the detail screen renders`() {
        lessons.filter { it.isReleased }.forEach { lesson ->
            assertFalse(
                "Lesson ${lesson.lessonNumber} has no whatYoullLearn items",
                lesson.whatYoullLearn.isEmpty()
            )
            assertFalse(
                "Lesson ${lesson.lessonNumber} has no practiceTips",
                lesson.practiceTips.isEmpty()
            )
        }
    }
}
