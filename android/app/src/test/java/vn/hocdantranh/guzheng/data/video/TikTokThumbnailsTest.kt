package vn.hocdantranh.guzheng.data.video

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TikTokThumbnailsTest {

    /** Replays [results] one per call so retry behaviour can be driven exactly. */
    private class FakeClient(private val results: MutableList<OembedResult>) : OembedClient {
        var calls = 0
            private set

        override suspend fun fetch(videoUrl: String): OembedResult {
            calls++
            return if (results.isEmpty()) OembedResult.Failed else results.removeAt(0)
        }
    }

    @Test
    fun `returns the thumbnail on success`() = runTest {
        val client = FakeClient(mutableListOf(OembedResult.Success("https://cdn/thumb.jpg")))
        val thumbs = TikTokThumbnails(client, throttleMs = 0)

        assertEquals("https://cdn/thumb.jpg", thumbs.resolve("id1", "https://tiktok/1"))
    }

    @Test
    fun `caches so a second call does not hit the network`() = runTest {
        val client = FakeClient(mutableListOf(OembedResult.Success("https://cdn/thumb.jpg")))
        val thumbs = TikTokThumbnails(client, throttleMs = 0)

        thumbs.resolve("id1", "https://tiktok/1")
        val second = thumbs.resolve("id1", "https://tiktok/1")

        assertEquals("https://cdn/thumb.jpg", second)
        assertEquals("only one network call", 1, client.calls)
    }

    @Test
    fun `retries when rate limited and then succeeds`() = runTest {
        val client = FakeClient(
            mutableListOf(
                OembedResult.RateLimited,
                OembedResult.RateLimited,
                OembedResult.Success("https://cdn/thumb.jpg"),
            )
        )
        val thumbs = TikTokThumbnails(client, throttleMs = 0)

        assertEquals("https://cdn/thumb.jpg", thumbs.resolve("id1", "https://tiktok/1"))
        assertEquals(3, client.calls)
    }

    @Test
    fun `gives up after the attempt limit`() = runTest {
        val client = FakeClient(MutableList(10) { OembedResult.RateLimited })
        val thumbs = TikTokThumbnails(client, throttleMs = 0)

        assertNull(thumbs.resolve("id1", "https://tiktok/1"))
        assertEquals(MAX_OEMBED_ATTEMPTS, client.calls)
    }

    @Test
    fun `does not retry a hard failure`() = runTest {
        val client = FakeClient(mutableListOf(OembedResult.Failed))
        val thumbs = TikTokThumbnails(client, throttleMs = 0)

        assertNull(thumbs.resolve("id1", "https://tiktok/1"))
        assertEquals("a malformed response is not worth retrying", 1, client.calls)
    }

    @Test
    fun `concurrent callers for the same video share one fetch`() = runTest {
        val client = FakeClient(mutableListOf(OembedResult.Success("https://cdn/thumb.jpg")))
        val thumbs = TikTokThumbnails(client, throttleMs = 0)

        val results = (1..5)
            .map { async { thumbs.resolve("id1", "https://tiktok/1") } }
            .awaitAll()

        assertEquals(List(5) { "https://cdn/thumb.jpg" }, results)
        assertEquals("the lookups are serialized and deduped", 1, client.calls)
    }
}
