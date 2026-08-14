package vn.hocdantranh.guzheng.data.lesson

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import vn.hocdantranh.guzheng.data.prefs.dataStore

/**
 * Which lessons the learner has opened. Replaces iOS
 * `UserDefaults["openedLessonNumbers"]`, and shares the `app_prefs` store with
 * [vn.hocdantranh.guzheng.data.prefs.AppPrefs].
 *
 * DataStore has no int-set type, so numbers are stored as strings. Anything unparseable is
 * dropped on read — a hand-edited or corrupt store degrades to lost progress rather than a
 * crash on the app's first screen.
 */
class LessonProgressPrefs(private val context: Context) {

    val openedLessonNumbers: Flow<Set<Int>> = context.dataStore.data.map { prefs ->
        prefs[OPENED_KEY].orEmpty().mapNotNull { it.toIntOrNull() }.toSet()
    }

    suspend fun markOpened(lessonNumber: Int) {
        context.dataStore.edit { prefs ->
            prefs[OPENED_KEY] = prefs[OPENED_KEY].orEmpty() + lessonNumber.toString()
        }
    }

    /** Wipes progress. Used by the debug reset affordance, mirroring iOS `resetProgress()`. */
    suspend fun reset() {
        context.dataStore.edit { prefs -> prefs.remove(OPENED_KEY) }
    }

    private companion object {
        val OPENED_KEY = stringSetPreferencesKey("openedLessonNumbers")
    }
}
