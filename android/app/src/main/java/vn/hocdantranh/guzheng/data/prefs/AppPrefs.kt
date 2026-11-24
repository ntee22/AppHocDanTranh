package vn.hocdantranh.guzheng.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
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

    /** iOS `@AppStorage("tunerSensitivity")`: "low" | "medium" | "high". */
    val tunerSensitivity: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[TUNER_SENSITIVITY_KEY]?.takeIf { it in SENSITIVITIES } ?: SENSITIVITY_MEDIUM
    }

    suspend fun setTunerSensitivity(value: String) {
        if (value !in SENSITIVITIES) return
        context.dataStore.edit { prefs -> prefs[TUNER_SENSITIVITY_KEY] = value }
    }

    /** iOS `@AppStorage("dt-tuner-guide-seen")` — set by "Không hiện lần sau". */
    val tunerGuideSeen: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[TUNER_GUIDE_SEEN_KEY] ?: false
    }

    suspend fun setTunerGuideSeen() {
        context.dataStore.edit { prefs -> prefs[TUNER_GUIDE_SEEN_KEY] = true }
    }

    companion object {
        const val SENSITIVITY_LOW = "low"
        const val SENSITIVITY_MEDIUM = "medium"
        const val SENSITIVITY_HIGH = "high"
        private val SENSITIVITIES = setOf(SENSITIVITY_LOW, SENSITIVITY_MEDIUM, SENSITIVITY_HIGH)
        private val TUNER_SENSITIVITY_KEY = stringPreferencesKey("tunerSensitivity")
        private val TUNER_GUIDE_SEEN_KEY = booleanPreferencesKey("dt-tuner-guide-seen")

        const val VI = "vi"
        const val EN = "en"
        const val DEFAULT = VI

        private val SUPPORTED = setOf(VI, EN)
        private val LANGUAGE_KEY = stringPreferencesKey("appLanguage")
    }
}
