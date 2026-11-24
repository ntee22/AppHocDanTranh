package vn.hocdantranh.guzheng.data.review

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewPolicyTest {
    private val day = 24L * 60 * 60 * 1000
    private val now = 1_000L * day

    @Test
    fun `waits for three positive actions`() {
        assertFalse(ReviewPolicy.shouldRequest(2, "1.1", null, null, now))
        assertTrue(ReviewPolicy.shouldRequest(3, "1.1", null, null, now))
    }

    @Test
    fun `asks at most once per version`() {
        assertFalse(ReviewPolicy.shouldRequest(5, "1.1", "1.1", now - 400 * day, now))
    }

    @Test
    fun `waits sixty days between requests`() {
        assertFalse(ReviewPolicy.shouldRequest(5, "1.2", "1.1", now - 59 * day, now))
        assertTrue(ReviewPolicy.shouldRequest(5, "1.2", "1.1", now - 60 * day, now))
    }
}
