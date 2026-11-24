package vn.hocdantranh.guzheng.data.lesson

/**
 * A single Guzheng lesson. Mirrors the iOS `Lesson` struct field-for-field.
 *
 * Swift's `id: String = UUID()` is deliberately absent: it exists there only to satisfy
 * `Identifiable` for `ForEach`, and [lessonNumber] is already unique and stable. It doubles
 * as the navigation argument here.
 */
data class Lesson(
    val lessonNumber: Int,
    val title: String,
    /** The English title. iOS shows this beneath [title] in both locales. */
    val subtitle: String,
    val thumbnailUrl: String,
    /** Blank until the lesson is recorded; see [isReleased]. */
    val videoId: String,
    /**
     * Access tier: free, or part of the paid course. This does NOT mean playable — a free
     * lesson with no video is still "coming soon". Use [LessonAccess] to decide what to show.
     */
    val isFree: Boolean,
    /** Human-readable running time, e.g. "12:30". */
    val duration: String,
    /** Free lessons stream from YouTube; paid ones will come from Cloudflare. */
    val isYouTube: Boolean = false,
    /**
     * Carried for parity with iOS, which records it but never reads it — the player is
     * pinned to a fixed height regardless of orientation.
     */
    val isPortrait: Boolean = true,
    val description: String = "",
    val practiceTips: List<String> = emptyList(),
    val whatYoullLearn: List<String> = emptyList(),
    val funFact: String? = null,
) {
    /** The video exists. Until then the lesson shows as "coming soon" whatever its tier. */
    val isReleased: Boolean get() = videoId.isNotBlank()
}
