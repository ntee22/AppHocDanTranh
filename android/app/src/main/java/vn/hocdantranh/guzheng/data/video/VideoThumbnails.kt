package vn.hocdantranh.guzheng.data.video

import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject

private const val YOUTUBE_ID_LENGTH = 11

/** How many times one oembed lookup may be retried while TikTok is throttling us. */
const val MAX_OEMBED_ATTEMPTS = 4

/** Gap between consecutive oembed calls. TikTok 429s a burst of ten immediately. */
private const val DEFAULT_THROTTLE_MS = 350L

/**
 * Pulls the 11-character video id out of a YouTube URL.
 *
 * Taking exactly [YOUTUBE_ID_LENGTH] characters is what strips the `?si=...` suffix the
 * live Firestore documents carry on their `youtu.be` short links.
 */
fun extractYouTubeId(url: String): String? {
    for (marker in listOf("v=", "youtu.be/", "/shorts/")) {
        val start = url.indexOf(marker)
        if (start == -1) continue
        val id = url.substring(start + marker.length).take(YOUTUBE_ID_LENGTH)
        if (id.length == YOUTUBE_ID_LENGTH) return id
    }
    return null
}

fun youTubeThumbnailUrl(id: String): String = "https://img.youtube.com/vi/$id/mqdefault.jpg"

/** Outcome of one TikTok oembed lookup. Rate limiting is worth retrying; nothing else is. */
sealed interface OembedResult {
    data class Success(val thumbnailUrl: String) : OembedResult
    data object RateLimited : OembedResult
    data object Failed : OembedResult
}

/** The oembed network call, behind an interface so the retry policy is testable. */
interface OembedClient {
    suspend fun fetch(videoUrl: String): OembedResult
}

object HttpOembedClient : OembedClient {
    override suspend fun fetch(videoUrl: String): OembedResult = withContext(Dispatchers.IO) {
        runCatching {
            val encoded = URLEncoder.encode(videoUrl, "UTF-8")
            val connection = (URL("https://www.tiktok.com/oembed?url=$encoded")
                .openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000
                readTimeout = 10_000
            }
            try {
                if (connection.responseCode == 429) return@runCatching OembedResult.RateLimited
                if (connection.responseCode !in 200..299) return@runCatching OembedResult.Failed

                val body = connection.inputStream.bufferedReader().use { it.readText() }
                JSONObject(body).optString("thumbnail_url")
                    .takeIf { it.isNotEmpty() }
                    ?.let { OembedResult.Success(it) }
                    ?: OembedResult.Failed
            } finally {
                connection.disconnect()
            }
        }.getOrDefault(OembedResult.Failed)
    }
}

/**
 * Resolves TikTok thumbnails, one at a time.
 *
 * TikTok's oembed endpoint answers 429 to a burst — and ten of the twelve live videos are
 * TikTok, so composing the list fires ten lookups at once and most came back throttled.
 * Requests are therefore serialized behind a mutex with a gap between them, retried with
 * backoff while throttled, and cached on success so the cost is paid once per process.
 */
class TikTokThumbnails(
    private val client: OembedClient = HttpOembedClient,
    private val throttleMs: Long = DEFAULT_THROTTLE_MS,
) {
    private val cache = ConcurrentHashMap<String, String>()
    private val lock = Mutex()

    suspend fun resolve(id: String, videoUrl: String): String? {
        cache[id]?.let { return it }

        return lock.withLock {
            // A caller that queued behind us may already have fetched this one.
            cache[id]?.let { return@withLock it }

            repeat(MAX_OEMBED_ATTEMPTS) { attempt ->
                if (throttleMs > 0) delay(throttleMs * (attempt + 1))

                when (val result = client.fetch(videoUrl)) {
                    is OembedResult.Success -> {
                        cache[id] = result.thumbnailUrl
                        return@withLock result.thumbnailUrl
                    }
                    // A malformed or missing response will not fix itself on a retry.
                    OembedResult.Failed -> return@withLock null
                    OembedResult.RateLimited -> Unit
                }
            }
            null
        }
    }
}

/** Resolves a video to an image URL that Coil can load. */
interface ThumbnailResolver {
    suspend fun resolve(item: VideoItem): String?
}

/** YouTube thumbnails come straight from the URL; TikTok needs the throttled oembed path. */
object DefaultThumbnailResolver : ThumbnailResolver {

    private val tikTok = TikTokThumbnails()

    override suspend fun resolve(item: VideoItem): String? = when (item.source) {
        VideoSource.YOUTUBE -> extractYouTubeId(item.url)?.let(::youTubeThumbnailUrl)
        VideoSource.TIKTOK -> tikTok.resolve(item.id, item.url)
    }
}
