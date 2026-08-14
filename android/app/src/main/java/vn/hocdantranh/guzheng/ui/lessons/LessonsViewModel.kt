package vn.hocdantranh.guzheng.ui.lessons

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import vn.hocdantranh.guzheng.data.lesson.Lesson
import vn.hocdantranh.guzheng.data.lesson.LessonCatalog
import vn.hocdantranh.guzheng.data.lesson.LessonProgressPrefs

/**
 * Everything the Lessons tab renders. The derived values mirror the iOS
 * `LessonViewModel` computed properties, and live here rather than in the composables so
 * the progress maths can be tested without a device.
 */
data class LessonsUiState(
    val lessons: List<Lesson> = emptyList(),
    val openedLessonNumbers: Set<Int> = emptySet(),
) {
    private val freeLessons: List<Lesson> get() = lessons.filter { it.isFree }

    /** Denominator of the progress ring: locked lessons cannot be completed. */
    val freeLessonsCount: Int get() = freeLessons.size

    val completedFreeCount: Int
        get() = freeLessons.count { it.lessonNumber in openedLessonNumbers }

    /** The lesson the "Tiếp" button jumps to; null once every free lesson has been opened. */
    val nextLesson: Lesson?
        get() = freeLessons.firstOrNull { it.lessonNumber !in openedLessonNumbers }
}

class LessonsViewModel(
    private val progress: LessonProgressPrefs,
) : ViewModel() {

    val uiState: StateFlow<LessonsUiState> = progress.openedLessonNumbers
        .map { opened -> LessonsUiState(LessonCatalog.lessons, opened) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = LessonsUiState(LessonCatalog.lessons),
        )

    fun lessonByNumber(lessonNumber: Int): Lesson? =
        uiState.value.lessons.firstOrNull { it.lessonNumber == lessonNumber }

    /**
     * Records that the learner opened a lesson. Locked lessons are ignored, matching the
     * iOS `guard lesson.isFree else { return }` — otherwise opening a "coming soon" card
     * would inflate the progress ring.
     *
     * Returns the write's [Job] so a caller can await it; the UI ignores it. Without a handle
     * the write is unobservable, since it suspends into DataStore's own IO scope and so
     * outlives any drain of the calling scheduler.
     */
    fun markOpened(lesson: Lesson): Job? {
        if (!lesson.isFree) return null
        return viewModelScope.launch { progress.markOpened(lesson.lessonNumber) }
    }

    companion object {
        private const val STOP_TIMEOUT_MS = 5_000L

        fun factory(context: Context): ViewModelProvider.Factory {
            val prefs = LessonProgressPrefs(context.applicationContext)
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    LessonsViewModel(prefs) as T
            }
        }
    }
}
