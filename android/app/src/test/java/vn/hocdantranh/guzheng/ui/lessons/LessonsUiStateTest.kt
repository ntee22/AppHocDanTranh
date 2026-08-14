package vn.hocdantranh.guzheng.ui.lessons

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import vn.hocdantranh.guzheng.data.lesson.Lesson

/**
 * The progress-card maths, ported from the iOS `LessonViewModel` computed properties.
 * Uses a hand-built lesson list rather than the real catalog so these stay true when the
 * catalog grows.
 */
class LessonsUiStateTest {

    private fun lesson(number: Int, free: Boolean) = Lesson(
        lessonNumber = number,
        title = "Bài $number",
        subtitle = "Lesson $number",
        thumbnailUrl = "",
        videoId = if (free) "vid$number" else "",
        isFree = free,
        duration = "1:00",
        isYouTube = free,
    )

    private val lessons = listOf(
        lesson(1, free = true),
        lesson(2, free = true),
        lesson(3, free = true),
        lesson(4, free = false),
        lesson(5, free = false),
    )

    @Test
    fun `counts only the free lessons toward the total`() {
        val state = LessonsUiState(lessons = lessons)
        assertEquals(3, state.freeLessonsCount)
    }

    @Test
    fun `counts nothing completed before anything is opened`() {
        val state = LessonsUiState(lessons = lessons)
        assertEquals(0, state.completedFreeCount)
    }

    @Test
    fun `counts an opened free lesson as completed`() {
        val state = LessonsUiState(lessons = lessons, openedLessonNumbers = setOf(1))
        assertEquals(1, state.completedFreeCount)
    }

    @Test
    fun `does not count an opened locked lesson as completed`() {
        val state = LessonsUiState(lessons = lessons, openedLessonNumbers = setOf(4))
        assertEquals(0, state.completedFreeCount)
    }

    @Test
    fun `ignores opened numbers that are not in the catalog`() {
        val state = LessonsUiState(lessons = lessons, openedLessonNumbers = setOf(99))
        assertEquals(0, state.completedFreeCount)
    }

    @Test
    fun `offers the first lesson as next before anything is opened`() {
        val state = LessonsUiState(lessons = lessons)
        assertEquals(1, state.nextLesson?.lessonNumber)
    }

    @Test
    fun `skips opened lessons when choosing the next one`() {
        val state = LessonsUiState(lessons = lessons, openedLessonNumbers = setOf(1, 2))
        assertEquals(3, state.nextLesson?.lessonNumber)
    }

    @Test
    fun `picks the earliest unopened lesson, not the one after the last opened`() {
        val state = LessonsUiState(lessons = lessons, openedLessonNumbers = setOf(3))
        assertEquals(1, state.nextLesson?.lessonNumber)
    }

    @Test
    fun `never offers a locked lesson as next`() {
        val state = LessonsUiState(lessons = lessons, openedLessonNumbers = setOf(1, 2, 3))
        assertNull(state.nextLesson)
    }
}
