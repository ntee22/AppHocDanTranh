package vn.hocdantranh.guzheng.ui.lessons

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import vn.hocdantranh.guzheng.FeatureFlags
import vn.hocdantranh.guzheng.data.billing.BillingProducts
import vn.hocdantranh.guzheng.data.billing.BillingRepository
import vn.hocdantranh.guzheng.data.lesson.Lesson
import vn.hocdantranh.guzheng.data.lesson.LessonAccess
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
    val ownsCourse: Boolean = false,
    val iapEnabled: Boolean = FeatureFlags.IAP_ENABLED,
) {
    fun accessOf(lesson: Lesson): LessonAccess =
        LessonAccess.resolve(lesson, iapEnabled = iapEnabled, ownsCourse = ownsCourse)

    private val playableLessons: List<Lesson> get() = lessons.filter { accessOf(it).isPlayable }

    /** Denominator of the progress ring: lessons the learner cannot open cannot be completed. */
    val freeLessonsCount: Int get() = playableLessons.size

    val completedFreeCount: Int
        get() = playableLessons.count { it.lessonNumber in openedLessonNumbers }

    /** The lesson the "Tiếp" button jumps to; null once every playable lesson has been opened. */
    val nextLesson: Lesson?
        get() = playableLessons.firstOrNull { it.lessonNumber !in openedLessonNumbers }
}

class LessonsViewModel(
    private val progress: LessonProgressPrefs,
    /** Product IDs the account owns; empty forever while selling is switched off. */
    ownedProducts: Flow<Set<String>> = kotlinx.coroutines.flow.flowOf(emptySet()),
) : ViewModel() {

    val uiState: StateFlow<LessonsUiState> = combine(
        progress.openedLessonNumbers,
        ownedProducts.map { owned -> owned.any { it in BillingProducts.COURSE_GRANTING } },
    ) { opened, ownsCourse ->
        LessonsUiState(LessonCatalog.lessons, opened, ownsCourse)
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = LessonsUiState(LessonCatalog.lessons),
        )

    fun lessonByNumber(lessonNumber: Int): Lesson? =
        uiState.value.lessons.firstOrNull { it.lessonNumber == lessonNumber }

    /**
     * Records that the learner opened a lesson. Lessons they cannot play are ignored —
     * otherwise opening a "coming soon" card would inflate the progress ring.
     *
     * Returns the write's [Job] so a caller can await it; the UI ignores it. Without a handle
     * the write is unobservable, since it suspends into DataStore's own IO scope and so
     * outlives any drain of the calling scheduler.
     */
    fun markOpened(lesson: Lesson): Job? {
        if (!uiState.value.accessOf(lesson).isPlayable) return null
        return viewModelScope.launch { progress.markOpened(lesson.lessonNumber) }
    }

    companion object {
        private const val STOP_TIMEOUT_MS = 5_000L

        fun factory(context: Context): ViewModelProvider.Factory {
            val prefs = LessonProgressPrefs(context.applicationContext)
            val billing = BillingRepository.get(context)
            billing.start()
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    LessonsViewModel(prefs, billing.ownedProducts) as T
            }
        }
    }
}
