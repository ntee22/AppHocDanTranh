package vn.hocdantranh.guzheng.data.review

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.core.content.edit
import com.google.android.play.core.review.ReviewManagerFactory
import vn.hocdantranh.guzheng.BuildConfig

/**
 * Asks for a Play Store review after positive actions — the Android `ReviewManager`.
 *
 * Call [recordPositiveAction] after something went well (a tuner string matched; later,
 * a lesson finished). Play decides whether the dialog actually appears and never says, so
 * the throttle is recorded as soon as the flow is launched, as iOS does.
 *
 * SharedPreferences rather than DataStore: a few synchronous counters read on the main
 * thread right after a match, with no flow observers.
 */
class ReviewPrompter(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun recordPositiveAction(activity: Activity) {
        val count = prefs.getInt(KEY_ACTION_COUNT, 0) + 1
        prefs.edit { putInt(KEY_ACTION_COUNT, count) }

        val now = System.currentTimeMillis()
        val due = ReviewPolicy.shouldRequest(
            actionCount = count,
            currentVersion = BuildConfig.VERSION_NAME,
            lastRequestVersion = prefs.getString(KEY_LAST_VERSION, null),
            lastRequestAtMs = prefs.getLong(KEY_LAST_AT, -1L).takeIf { it >= 0 },
            nowMs = now,
        )
        if (!due) return

        prefs.edit {
            putLong(KEY_LAST_AT, now)
            putString(KEY_LAST_VERSION, BuildConfig.VERSION_NAME)
            putInt(KEY_ACTION_COUNT, 0)
        }

        val manager = ReviewManagerFactory.create(appContext)
        manager.requestReviewFlow().addOnCompleteListener { task ->
            if (task.isSuccessful && !activity.isFinishing) {
                manager.launchReviewFlow(activity, task.result)
            } else {
                Log.w(TAG, "Review flow unavailable", task.exception)
            }
        }
    }

    private companion object {
        const val TAG = "ReviewPrompter"
        const val PREFS = "review_prompt"
        const val KEY_ACTION_COUNT = "reviewTriggerActionCount"
        const val KEY_LAST_AT = "lastReviewRequestDate"
        const val KEY_LAST_VERSION = "appVersionAtLastReviewRequest"
    }
}
