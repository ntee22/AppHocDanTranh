package vn.hocdantranh.guzheng

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay
import vn.hocdantranh.guzheng.ui.launch.LaunchScreen
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import vn.hocdantranh.guzheng.data.prefs.AppPrefs
import vn.hocdantranh.guzheng.ui.LocaleController
import vn.hocdantranh.guzheng.ui.MainScaffold
import vn.hocdantranh.guzheng.ui.theme.DanThanhTheme

/** iOS dismisses its launch screen 2.8 s after appearing (2.6 s unroll + a beat). */
private const val LAUNCH_DISMISS_MS = 2_800L

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
                // iOS DanThanhGuzhengApp: the tabs are composed underneath from the start
                // (so nothing jumps when the cover lifts), the paper-scroll launch screen
                // plays for 2.8 s, then fades out over 0.5 s. Saveable so a configuration
                // change does not replay it.
                var showLaunch by rememberSaveable { mutableStateOf(true) }
                LaunchedEffect(Unit) {
                    delay(LAUNCH_DISMISS_MS)
                    showLaunch = false
                }
                Box(Modifier.fillMaxSize()) {
                    MainScaffold()
                    AnimatedVisibility(
                        visible = showLaunch,
                        enter = EnterTransition.None,
                        exit = fadeOut(tween(500)),
                    ) {
                        LaunchScreen()
                    }
                }
            }
        }
    }
}
