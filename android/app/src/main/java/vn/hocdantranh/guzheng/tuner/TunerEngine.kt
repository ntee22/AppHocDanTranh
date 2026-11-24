package vn.hocdantranh.guzheng.tuner

import kotlin.math.abs
import kotlin.math.ln

/** What the tuner screen renders. Mirrors the iOS `TunerEngine` @Published properties. */
data class TunerState(
    val activeStringId: Int = DEFAULT_STRING_ID,
    val detectedHz: Double = 0.0,
    val cents: Double = 0.0,
    val isInTune: Boolean = false,
    val isSounding: Boolean = false,
    val manualLock: Boolean = false,
    /** Non-null while a confirmed match is cooling down; epoch millis it ends at. */
    val matchedUntilMs: Long? = null,
    val isPlayingReference: Boolean = false,
) {
    val isMatched: Boolean get() = matchedUntilMs != null

    companion object {
        /** String 8 (A1, the green 440 Hz reference) — the iOS default. */
        const val DEFAULT_STRING_ID = 8
    }
}

/**
 * The tuner's decision logic, ported line-for-line from iOS `TunerEngine.processAudioFrame`
 * and `handlePitch`. Pure Kotlin, no Android types, so it is unit-tested directly.
 *
 * Not thread-safe: call it from one thread (the ViewModel uses Main, as iOS uses the main
 * queue).
 */
class TunerEngine(
    private val nowMs: () -> Long = System::currentTimeMillis,
) {
    var state: TunerState = TunerState()
        private set

    /** "low" | "medium" | "high" — see [stabilityFramesRequired]. */
    var sensitivity: String = "medium"

    private var pendingId = TunerState.DEFAULT_STRING_ID
    private var pendingCount = 0
    private var consecutiveMatchCount = 0

    /**
     * Feeds one analysis frame. [frequencyHz] is null when the detector found no pitch.
     * Returns true exactly when this frame confirmed a match — the caller plays the success
     * haptic/sound and counts a positive action.
     */
    fun processFrame(frequencyHz: Double?): Boolean {
        if (state.isPlayingReference) {
            state = state.copy(isSounding = false)
            consecutiveMatchCount = 0
            return false
        }

        val until = state.matchedUntilMs
        if (until != null) {
            // Stay locked on the confirmed string until the cooldown elapses.
            if (nowMs() >= until) {
                state = state.copy(matchedUntilMs = null)
                consecutiveMatchCount = 0
            }
            return false
        }

        if (frequencyHz == null) {
            state = state.copy(isSounding = false)
            consecutiveMatchCount = 0
            return false
        }
        return handlePitch(frequencyHz)
    }

    /** Tap on a string row. Tapping the locked string again returns to auto-detect. */
    fun selectString(id: Int) {
        if (state.manualLock && id == state.activeStringId) {
            state = state.copy(manualLock = false, isPlayingReference = false, matchedUntilMs = null)
            consecutiveMatchCount = 0
            return
        }
        pendingId = id
        pendingCount = CONFIRM_FRAMES
        consecutiveMatchCount = 0
        state = state.copy(
            activeStringId = id,
            manualLock = true,
            matchedUntilMs = null,
            isSounding = false,
        )
    }

    fun setPlayingReference(playing: Boolean) {
        state = state.copy(isPlayingReference = playing)
    }

    private fun handlePitch(hz: Double): Boolean {
        // Reject frequencies outside the guzheng's playable range.
        if (hz < MIN_HZ || hz > MAX_HZ) return false

        val closest = GUZHENG_STRINGS.minBy { abs(it.frequency - hz) }
        val targetHz = if (state.manualLock) guzhengString(state.activeStringId).frequency
        else closest.frequency
        val cents = 1200 * log2(hz / targetHz)

        // More than two semitones off is almost always a harmonic glitch.
        if (abs(cents) > MAX_PLAUSIBLE_CENTS) {
            state = state.copy(isSounding = true, detectedHz = hz)
            return false
        }

        state = state.copy(
            isSounding = true,
            detectedHz = hz,
            cents = cents,
            isInTune = abs(cents) < IN_TUNE_CENTS,
        )

        // Like iOS, a match does not move the highlighted string: in auto mode the
        // three-frame confirmation below normally got there first.
        if (abs(cents) <= MATCH_TOLERANCE_CENTS) {
            consecutiveMatchCount += 1
            if (consecutiveMatchCount >= stabilityFramesRequired(sensitivity)) {
                consecutiveMatchCount = 0
                state = state.copy(matchedUntilMs = nowMs() + MATCH_COOLDOWN_MS)
                return true
            }
        } else {
            consecutiveMatchCount = 0
        }

        if (state.manualLock) return false

        if (closest.id == pendingId) {
            pendingCount += 1
            if (pendingCount >= CONFIRM_FRAMES) {
                state = state.copy(activeStringId = closest.id)
            }
        } else {
            pendingId = closest.id
            pendingCount = 1
        }
        return false
    }

    companion object {
        const val MIN_HZ = 55.0
        const val MAX_HZ = 1500.0
        const val MAX_PLAUSIBLE_CENTS = 200.0
        const val IN_TUNE_CENTS = 15.0
        const val MATCH_TOLERANCE_CENTS = 5.0
        const val MATCH_COOLDOWN_MS = 1_500L

        /** Consecutive frames on the same nearest string before auto-detect switches to it. */
        const val CONFIRM_FRAMES = 3

        /** Fewer frames = quicker but jumpier lock. Same tiers as iOS `TunerTuning`. */
        fun stabilityFramesRequired(sensitivity: String): Int = when (sensitivity) {
            "low" -> 7
            "high" -> 3
            else -> 5
        }

        private fun log2(x: Double) = ln(x) / ln(2.0)
    }
}
