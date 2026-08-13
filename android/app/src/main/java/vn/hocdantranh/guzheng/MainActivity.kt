package vn.hocdantranh.guzheng

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import vn.hocdantranh.guzheng.data.prefs.AppPrefs
import vn.hocdantranh.guzheng.ui.LocaleController
import vn.hocdantranh.guzheng.ui.MainScaffold
import vn.hocdantranh.guzheng.ui.theme.DanThanhTheme

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Apply the stored language unconditionally.
        //
        // This must NOT be conditional on the current locale. With no per-app locale
        // set, Android follows the SYSTEM locale — so an English phone would show
        // English even though the stored preference is "vi". iOS defaults to
        // Vietnamese regardless of device language, and Android must match.
        // AppCompat no-ops internally when the locale is already correct.
        lifecycleScope.launch {
            val stored = AppPrefs(applicationContext).language.first()
            LocaleController.apply(stored)
        }

        setContent {
            DanThanhTheme {
                MainScaffold()
            }
        }
    }
}
