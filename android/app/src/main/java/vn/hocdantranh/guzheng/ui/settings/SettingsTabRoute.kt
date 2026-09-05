package vn.hocdantranh.guzheng.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import vn.hocdantranh.guzheng.data.prefs.AppPrefs
import vn.hocdantranh.guzheng.ui.LocaleController

/**
 * Owns the language preference so [SettingsScreen] stays a pure function of its inputs.
 *
 * Writing the preference and applying the locale are deliberately separate steps: the
 * write is what survives a restart, while [LocaleController.apply] is what re-renders the
 * UI now. Skipping either one produces the bug where the toggle looks like it worked but
 * reverts on next launch — or changes nothing until relaunch.
 */
@Composable
fun SettingsTabRoute() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember(context) { AppPrefs(context.applicationContext) }
    val language by prefs.language.collectAsState(initial = AppPrefs.DEFAULT)

    SettingsRoute(
        language = language,
        onLanguageChange = { code ->
            scope.launch {
                prefs.setLanguage(code)
                LocaleController.apply(code)
            }
        },
    )
}
