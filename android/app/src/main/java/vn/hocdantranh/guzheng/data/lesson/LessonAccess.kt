package vn.hocdantranh.guzheng.data.lesson

/** What the learner sees for one lesson. */
enum class LessonAccess {
    /** No video yet, or a paid lesson while selling is switched off. */
    COMING_SOON,

    /** Released and free. */
    FREE,

    /** Released, paid, and owned. */
    PURCHASED,

    /** Released and paid but not owned — the only state that may show a purchase entry. */
    LOCKED;

    val isPlayable: Boolean get() = this == FREE || this == PURCHASED

    companion object {
        /**
         * The single source of truth for lesson gating. iOS `LessonAccess.resolve` must stay
         * identical — see docs/superpowers/specs/2026-11-24-tuner-and-paid-lessons-design.md.
         */
        fun resolve(lesson: Lesson, iapEnabled: Boolean, ownsCourse: Boolean): LessonAccess = when {
            !lesson.isReleased -> COMING_SOON
            lesson.isFree -> FREE
            !iapEnabled -> COMING_SOON
            ownsCourse -> PURCHASED
            else -> LOCKED
        }
    }
}
