package vn.hocdantranh.guzheng.data.video

import androidx.annotation.StringRes
import vn.hocdantranh.guzheng.R

/** Where a video lives. Drives the platform badge and which thumbnail lookup runs. */
enum class VideoSource(val key: String) {
    YOUTUBE("youtube"),
    TIKTOK("tiktok");

    companion object {
        fun fromKey(key: String?): VideoSource? = entries.firstOrNull { it.key == key }
    }
}

/**
 * Mirrors the iOS `VideoCategory`. `TIPS` has no video in Firestore today, which is
 * exactly why the chip row is derived from the data rather than from these entries.
 */
enum class VideoCategory(val key: String, @StringRes val labelRes: Int) {
    INTRO("intro", R.string.video_category_intro),
    SONGS("songs", R.string.video_category_songs),
    TIPS("tips", R.string.video_category_tips),
    TUTORIAL("tutorial", R.string.video_category_tutorial);

    companion object {
        fun fromKey(key: String?): VideoCategory? = entries.firstOrNull { it.key == key }
    }
}

data class VideoItem(
    val id: String,
    val title: String,
    val url: String,
    val source: VideoSource,
    val category: VideoCategory,
    val order: Int,
)

/**
 * Maps one Firestore document's fields to a [VideoItem], or null if it should not render.
 *
 * Null rather than throwing, mirroring the iOS `compactMap`: one malformed document must
 * not empty the whole tab. `isVisible` is applied here and then dropped from the model —
 * a hidden video never reaches the UI, so nothing downstream needs to re-check it.
 */
fun videoFromFields(id: String, fields: Map<String, Any?>): VideoItem? {
    val isVisible = fields["isVisible"] as? Boolean ?: true
    if (!isVisible) return null

    val title = fields["title"] as? String ?: return null
    val url = fields["url"] as? String ?: return null
    val source = VideoSource.fromKey(fields["source"] as? String) ?: return null
    val category = VideoCategory.fromKey(fields["category"] as? String) ?: return null
    val order = (fields["order"] as? Number)?.toInt() ?: return null

    return VideoItem(
        id = id,
        title = title,
        url = url,
        source = source,
        category = category,
        order = order,
    )
}
