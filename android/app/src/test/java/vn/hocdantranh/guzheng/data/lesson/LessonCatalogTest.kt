package vn.hocdantranh.guzheng.data.lesson

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The catalog is the Android mirror of iOS `Lesson.mockLessons()`.
 * These assertions pin the invariants the UI relies on: the progress ring divides by
 * the free-lesson count, and the detail screen only reaches for a player when a
 * lesson is free — so a free lesson without a video ID would render a dead player.
 */
class LessonCatalogTest {

    private val lessons = LessonCatalog.lessons

    @Test
    fun `ships ten lessons`() {
        assertEquals(10, lessons.size)
    }

    @Test
    fun `numbers lessons one through ten in order`() {
        assertEquals((1..10).toList(), lessons.map { it.lessonNumber })
    }

    @Test
    fun `marks exactly six lessons free`() {
        assertEquals(6, lessons.count { it.isFree })
    }

    @Test
    fun `free lessons are the first six`() {
        assertEquals((1..6).toList(), lessons.filter { it.isFree }.map { it.lessonNumber })
    }

    @Test
    fun `every free lesson has a playable YouTube video`() {
        lessons.filter { it.isFree }.forEach { lesson ->
            assertTrue(
                "Lesson ${lesson.lessonNumber} is free but has a blank videoId",
                lesson.videoId.isNotBlank()
            )
            assertTrue(
                "Lesson ${lesson.lessonNumber} is free but is not marked isYouTube",
                lesson.isYouTube
            )
        }
    }

    @Test
    fun `locked lessons have no video to play`() {
        lessons.filterNot { it.isFree }.forEach { lesson ->
            assertTrue(
                "Lesson ${lesson.lessonNumber} is locked but carries a videoId",
                lesson.videoId.isBlank()
            )
        }
    }

    @Test
    fun `every lesson has a title, subtitle and duration`() {
        lessons.forEach { lesson ->
            assertTrue("Lesson ${lesson.lessonNumber} has a blank title", lesson.title.isNotBlank())
            assertTrue(
                "Lesson ${lesson.lessonNumber} has a blank subtitle",
                lesson.subtitle.isNotBlank()
            )
            assertTrue(
                "Lesson ${lesson.lessonNumber} has a blank duration",
                lesson.duration.isNotBlank()
            )
        }
    }

    @Test
    fun `free lessons carry the teaching content the detail screen renders`() {
        lessons.filter { it.isFree }.forEach { lesson ->
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
