package vn.hocdantranh.guzheng.tuner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

/**
 * Runs the real TarsosDSP YIN on synthetic plucks, so a library or parameter change that
 * breaks detection fails here rather than on a learner's guzheng.
 */
class PitchDetectorTest {

    private fun tone(hz: Double, amplitude: Float = 0.3f): FloatArray =
        FloatArray(PitchDetector.BUFFER_SIZE) { i ->
            val t = i.toDouble() / PitchDetector.SAMPLE_RATE
            // Fundamental plus two harmonics, roughly like a plucked string.
            (amplitude * (sin(2 * PI * hz * t) + 0.5 * sin(4 * PI * hz * t) + 0.25 * sin(6 * PI * hz * t)) / 1.75).toFloat()
        }

    @Test
    fun `detects every guzheng string within 1 Hz`() {
        val yin = PitchDetector.newYin()
        for (s in GUZHENG_STRINGS) {
            val hz = PitchDetector.analyse(yin, tone(s.frequency))
            assertNotNull("String ${s.id} not detected", hz)
            assertEquals("String ${s.id}", s.frequency, hz!!, 1.0)
        }
    }

    @Test
    fun `frames below minus 60 dBFS report no pitch`() {
        val yin = PitchDetector.newYin()
        assertNull(PitchDetector.analyse(yin, tone(437.9, amplitude = 0.0005f)))
        assertNull(PitchDetector.analyse(yin, FloatArray(PitchDetector.BUFFER_SIZE)))
    }
}
