package vn.hocdantranh.guzheng.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The accent ramp assigned to each lesson tile. Values are pinned to the 16-entry cycle in
 * iOS `LessonRow.accent` so the two platforms colour the same lesson the same way.
 */
class LessonAccentTest {

    @Test
    fun `assigns the iOS cycle to lessons one through ten`() {
        val expected = listOf(
            LessonAccent.SAGE, // 1
            LessonAccent.WOOD, // 2
            LessonAccent.SAGE, // 3
            LessonAccent.PLUM, // 4
            LessonAccent.WOOD, // 5
            LessonAccent.SAGE, // 6
            LessonAccent.PLUM, // 7
            LessonAccent.WOOD, // 8
            LessonAccent.SAGE, // 9
            LessonAccent.PLUM, // 10
        )
        assertEquals(expected, (1..10).map { LessonAccent.forLesson(it) })
    }

    @Test
    fun `wraps after sixteen lessons`() {
        assertEquals(LessonAccent.forLesson(1), LessonAccent.forLesson(17))
        assertEquals(LessonAccent.forLesson(16), LessonAccent.forLesson(32))
    }

    @Test
    fun `never throws on a lesson number below one`() {
        // Guards the modulo: Kotlin's % returns negative for negative operands, which would
        // index out of bounds. floorMod is what keeps this total.
        assertEquals(LessonAccent.forLesson(16), LessonAccent.forLesson(0))
    }
}
