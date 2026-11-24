package vn.hocdantranh.guzheng.ui.tuner

import android.Manifest
import android.app.Application
import android.content.Context
import android.util.Log
import vn.hocdantranh.guzheng.BuildConfig
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.annotation.RequiresPermission
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import vn.hocdantranh.guzheng.data.prefs.AppPrefs
import vn.hocdantranh.guzheng.tuner.PitchDetector
import vn.hocdantranh.guzheng.tuner.ReferencePlayer
import vn.hocdantranh.guzheng.tuner.TunerEngine
import vn.hocdantranh.guzheng.tuner.TunerState

/**
 * Owns the microphone, the reference player and the [TunerEngine]. Port of the iOS
 * `TunerEngine` ObservableObject's lifecycle half; the decision logic lives in the engine.
 *
 * Frames are processed on Main, as iOS re-dispatches Tuna callbacks to the main queue.
 */
class TunerViewModel(application: Application) : AndroidViewModel(application) {

    private val engine = TunerEngine()
    private val referencePlayer = ReferencePlayer(application)
    private var listenJob: Job? = null

    private val _state = MutableStateFlow(engine.state)
    val state: StateFlow<TunerState> = _state.asStateFlow()

    private val _matches = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    /** Fires once per confirmed match: the screen plays haptic + sound and counts a review action. */
    val matches: SharedFlow<Unit> = _matches.asSharedFlow()

    init {
        val prefs = AppPrefs(application)
        viewModelScope.launch {
            prefs.tunerSensitivity.collect { engine.sensitivity = it }
        }
    }

    /** Starts listening if RECORD_AUDIO is granted. Idempotent. */
    fun startIfPermitted(context: Context) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) !=
            PackageManager.PERMISSION_GRANTED
        ) return
        start()
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun start() {
        if (listenJob?.isActive == true) return
        listenJob = viewModelScope.launch {
            PitchDetector.frames().collect { hz ->
                if (BuildConfig.DEBUG) Log.d("TunerFrame", "hz=$hz")
                val matched = engine.processFrame(hz)
                publish()
                if (matched) _matches.tryEmit(Unit)
            }
        }
    }

    /** Stops the microphone and any reference playback — iOS `stop()`. */
    fun stop() {
        listenJob?.cancel()
        listenJob = null
        referencePlayer.stop()
        engine.setPlayingReference(false)
        publish()
    }

    fun selectString(id: Int) {
        val wasLocked = engine.state.manualLock && engine.state.activeStringId == id
        engine.selectString(id)
        if (wasLocked) {
            referencePlayer.stop()
        } else {
            // Mic frames are ignored while the reference plays, so the speaker cannot
            // "tune" the string to itself.
            engine.setPlayingReference(true)
            referencePlayer.play(id) {
                engine.setPlayingReference(false)
                publish()
            }
        }
        publish()
    }

    private fun publish() {
        _state.value = engine.state
    }

    override fun onCleared() {
        referencePlayer.stop()
    }

    companion object {
        fun factory(application: Application) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                TunerViewModel(application) as T
        }
    }
}
