package vn.hocdantranh.guzheng.tuner

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import vn.hocdantranh.guzheng.R

/**
 * Plays the recorded reference pluck for a string (`res/raw/string_NN.m4a`, the same files
 * iOS bundles). Starting a new string interrupts the previous one, as on iOS.
 */
class ReferencePlayer(private val context: Context) {
    private var player: MediaPlayer? = null

    /** Calls [onFinished] when playback ends or fails; not called after [stop]. */
    fun play(stringId: Int, onFinished: () -> Unit) {
        stop()
        val resId = REFERENCE_AUDIO.getOrNull(stringId - 1)
        if (resId == null) {
            Log.w(TAG, "Reference audio for string $stringId not found")
            onFinished()
            return
        }
        val mp = MediaPlayer.create(
            context,
            resId,
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build(),
            0,
        )
        if (mp == null) {
            onFinished()
            return
        }
        player = mp
        mp.setOnCompletionListener {
            if (player === it) {
                release()
                onFinished()
            }
        }
        mp.start()
    }

    fun stop() = release()

    private fun release() {
        player?.let {
            it.setOnCompletionListener(null)
            runCatching { it.stop() }
            it.release()
        }
        player = null
    }

    private companion object {
        const val TAG = "ReferencePlayer"

        /**
         * Indexed by string id − 1. Listed explicitly rather than looked up by name so
         * release resource shrinking can see every file is used.
         */
        val REFERENCE_AUDIO = intArrayOf(
            R.raw.string_01, R.raw.string_02, R.raw.string_03, R.raw.string_04,
            R.raw.string_05, R.raw.string_06, R.raw.string_07, R.raw.string_08,
            R.raw.string_09, R.raw.string_10, R.raw.string_11, R.raw.string_12,
            R.raw.string_13, R.raw.string_14, R.raw.string_15, R.raw.string_16,
            R.raw.string_17, R.raw.string_18, R.raw.string_19, R.raw.string_20,
            R.raw.string_21,
        ).toList()
    }
}
