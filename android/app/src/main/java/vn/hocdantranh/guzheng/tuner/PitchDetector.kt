package vn.hocdantranh.guzheng.tuner

import android.Manifest
import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.annotation.RequiresPermission
import be.tarsos.dsp.pitch.Yin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlin.math.log10
import kotlin.math.sqrt

/**
 * Microphone → YIN pitch frames. The Android stand-in for Tuna's `PitchEngine`.
 *
 * The algorithm is TarsosDSP's YIN; this class only feeds it. Parameters follow Tuna:
 * - buffer 4096 samples, non-overlapping (Tuna's `bufferSize: 4096`),
 * - frames quieter than −60 dBFS report no pitch (Tuna `InputSignalTracker` default level
 *   threshold),
 * - YIN threshold 0.20 — the one deliberate difference. Tuna uses 0.05; on the OPPO test
 *   phone 0.05 made YIN skip the true period on noisy frames and report 1/3 or 1/4 of the
 *   pitch (a 437.9 Hz tone read as 146 or 109 Hz, which the engine then "matched" to strings
 *   16 and 18). 0.20, TarsosDSP's default, removed those frames in the same test.
 */
object PitchDetector {
    const val SAMPLE_RATE = 44_100
    const val BUFFER_SIZE = 4096
    private const val YIN_THRESHOLD = 0.20
    private const val LEVEL_THRESHOLD_DB = -60.0
    private const val TAG = "PitchDetector"

    /**
     * Emits one value per buffer (~10.8 per second): the pitch in Hz, or null for silence or
     * no clear pitch. Recording starts on collection and stops when the collector cancels.
     */
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun frames(): Flow<Double?> = flow {
        val record = createRecord() ?: return@flow
        val yin = newYin()
        val buffer = FloatArray(BUFFER_SIZE)
        try {
            record.startRecording()
            while (currentCoroutineContext().isActive) {
                if (!fill(record, buffer)) break
                emit(analyse(yin, buffer))
            }
        } finally {
            runCatching { record.stop() }
            record.release()
        }
    }.flowOn(Dispatchers.Default)

    /** Pure analysis step, separated so it can be unit-tested with synthetic tones. */
    internal fun analyse(yin: Yin, buffer: FloatArray): Double? {
        if (levelDb(buffer) < LEVEL_THRESHOLD_DB) return null
        val result = yin.getPitch(buffer)
        return if (result.isPitched && result.pitch > 0f) result.pitch.toDouble() else null
    }

    internal fun newYin(): Yin = Yin(SAMPLE_RATE.toFloat(), BUFFER_SIZE, YIN_THRESHOLD)

    /** RMS level in dBFS (full scale = 1.0). */
    internal fun levelDb(buffer: FloatArray): Double {
        var sum = 0.0
        for (s in buffer) sum += s * s
        val rms = sqrt(sum / buffer.size)
        return if (rms <= 0.0) -160.0 else 20 * log10(rms)
    }

    /** Blocks until [buffer] is full. Returns false if the recorder failed. */
    private fun fill(record: AudioRecord, buffer: FloatArray): Boolean {
        var offset = 0
        while (offset < buffer.size) {
            val read = record.read(buffer, offset, buffer.size - offset, AudioRecord.READ_BLOCKING)
            if (read < 0) {
                Log.w(TAG, "AudioRecord.read failed: $read")
                return false
            }
            offset += read
        }
        return true
    }

    @SuppressLint("MissingPermission") // Guarded by @RequiresPermission on frames().
    private fun createRecord(): AudioRecord? {
        val minBytes = AudioRecord.getMinBufferSize(
            SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_FLOAT,
        )
        if (minBytes <= 0) {
            Log.w(TAG, "Float PCM capture unsupported: $minBytes")
            return null
        }
        // VOICE_RECOGNITION asks the HAL for minimal processing (no AGC/noise suppression),
        // the closest Android match to iOS's AVAudioSession `.measurement` mode.
        val record = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_FLOAT,
            maxOf(minBytes, BUFFER_SIZE * Float.SIZE_BYTES * 2),
        )
        if (record.state != AudioRecord.STATE_INITIALIZED) {
            Log.w(TAG, "AudioRecord failed to initialise")
            record.release()
            return null
        }
        return record
    }
}
