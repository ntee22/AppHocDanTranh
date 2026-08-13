package vn.hocdantranh.guzheng.ui

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import vn.hocdantranh.guzheng.data.prefs.AppPrefs

/**
 * Applies the in-app language choice.
 *
 * The app does NOT follow system locale — iOS ships an explicit vi/en switch in
 * Settings and Android must behave identically. AppCompat's per-app locale API
 * is backported to API 21, so this works on the full minSdk 24 range.
 */
object LocaleController {

    fun apply(code: String) {
        val locales = if (code.isBlank()) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(code)
        }
        AppCompatDelegate.setApplicationLocales(locales)
    }

    fun currentLanguage(): String {
        val locales = AppCompatDelegate.getApplicationLocales()
        if (locales.isEmpty) return AppPrefs.DEFAULT
        return locales[0]?.language ?: AppPrefs.DEFAULT
    }
}
