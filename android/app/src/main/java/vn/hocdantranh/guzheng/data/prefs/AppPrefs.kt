package vn.hocdantranh.guzheng.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal val Context.dataStore by preferencesDataStore(name = "app_prefs")

/**
 * App-wide user preferences.
 * Replaces iOS @AppStorage — the "appLanguage" key becomes [language].
 */
class AppPrefs(private val context: Context) {

    val language: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[LANGUAGE_KEY] ?: DEFAULT
    }

    /** No-ops on codes the app does not ship, so a bad value can never brick the UI. */
    suspend fun setLanguage(code: String) {
        if (code !in SUPPORTED) return
        context.dataStore.edit { prefs -> prefs[LANGUAGE_KEY] = code }
    }

    companion object {
        const val VI = "vi"
        const val EN = "en"
        const val DEFAULT = VI

        private val SUPPORTED = setOf(VI, EN)
        private val LANGUAGE_KEY = stringPreferencesKey("appLanguage")
    }
}
