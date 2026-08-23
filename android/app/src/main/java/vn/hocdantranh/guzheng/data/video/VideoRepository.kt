package vn.hocdantranh.guzheng.data.video

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

/** Matches the iOS page size so both platforms paginate identically. */
const val VIDEO_PAGE_SIZE = 10

/**
 * One page of videos.
 *
 * [cursor] is opaque to callers — the Firestore implementation stores a `DocumentSnapshot`
 * in it and hands it back on the next call, which keeps the ViewModel free of Firestore types.
 */
data class VideoPage(
    val items: List<VideoItem>,
    val cursor: Any?,
    val hasMore: Boolean,
)

/**
 * Builds a [VideoPage] from raw `id to fields` pairs.
 *
 * `hasMore` counts the **raw** documents, before the `isVisible` filter: a full page of
 * hidden videos still means another page may follow, and testing the filtered count there
 * would end pagination early. Extracted from the Firestore call so this rule is testable
 * without a device.
 */
fun videoPageFrom(raw: List<Pair<String, Map<String, Any?>>>, cursor: Any?): VideoPage =
    VideoPage(
        items = raw.mapNotNull { (id, fields) -> videoFromFields(id, fields) },
        cursor = cursor,
        hasMore = raw.size == VIDEO_PAGE_SIZE,
    )

interface VideoRepository {
    /** Loads the page after [cursor]; pass null for the first page. */
    suspend fun loadPage(cursor: Any?): VideoPage
}

class FirestoreVideoRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) : VideoRepository {

    override suspend fun loadPage(cursor: Any?): VideoPage {
        var query: Query = firestore.collection("videos")
            .orderBy("order")
            .limit(VIDEO_PAGE_SIZE.toLong())

        if (cursor is DocumentSnapshot) query = query.startAfter(cursor)

        val snapshot = query.get().await()
        val raw = snapshot.documents.map { it.id to it.data.orEmpty() }
        return videoPageFrom(raw, cursor = snapshot.documents.lastOrNull())
    }
}
