package vn.hocdantranh.guzheng.data.review

/**
 * When to ask for a Play review. Port of the iOS `ReviewManager` throttle:
 * after [POSITIVE_ACTIONS_THRESHOLD] positive actions, at most once per app version, and
 * at least [MIN_DAYS_BETWEEN_REQUESTS] days apart. Pure, so it is unit-tested.
 */
object ReviewPolicy {
    const val POSITIVE_ACTIONS_THRESHOLD = 3
    const val MIN_DAYS_BETWEEN_REQUESTS = 60
    private const val DAY_MS = 24L * 60 * 60 * 1000

    fun shouldRequest(
        actionCount: Int,
        currentVersion: String,
        lastRequestVersion: String?,
        lastRequestAtMs: Long?,
        nowMs: Long,
    ): Boolean {
        if (actionCount < POSITIVE_ACTIONS_THRESHOLD) return false
        if (currentVersion == lastRequestVersion) return false
        if (lastRequestAtMs != null && (nowMs - lastRequestAtMs) / DAY_MS < MIN_DAYS_BETWEEN_REQUESTS) {
            return false
        }
        return true
    }
}
