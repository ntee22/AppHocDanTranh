package vn.hocdantranh.guzheng.tuner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

/** Behaviour copied from iOS `TunerEngine`; each test names the iOS rule it pins. */
class TunerEngineTest {

    private var now = 0L
    private val engine = TunerEngine(nowMs = { now })

    private fun hzAtCents(baseHz: Double, cents: Double) = baseHz * 2.0.pow(cents / 1200)

    @Test
    fun `auto-detect switches string after three consecutive frames`() {
        val target = guzhengString(13).frequency
        repeat(2) { engine.processFrame(hzAtCents(target, 30.0)) }
        assertEquals(TunerState.DEFAULT_STRING_ID, engine.state.activeStringId)
        engine.processFrame(hzAtCents(target, 30.0))
        assertEquals(13, engine.state.activeStringId)
    }

    @Test
    fun `frequencies outside 55 to 1500 Hz are ignored`() {
        engine.processFrame(40.0)
        engine.processFrame(2000.0)
        assertFalse(engine.state.isSounding)
    }

    @Test
    fun `silence clears isSounding`() {
        engine.processFrame(guzhengString(8).frequency)
        assertTrue(engine.state.isSounding)
        engine.processFrame(null)
        assertFalse(engine.state.isSounding)
    }

    @Test
    fun `in tune means under 15 cents, sign of cents gives direction`() {
        val a = guzhengString(8).frequency
        engine.processFrame(hzAtCents(a, -14.0))
        assertTrue(engine.state.isInTune)
        engine.processFrame(hzAtCents(a, -20.0))
        assertFalse(engine.state.isInTune)
        assertTrue(engine.state.cents < 0)
    }

    @Test
    fun `medium sensitivity confirms a match after five frames within 5 cents`() {
        val a = guzhengString(8).frequency
        repeat(4) { assertFalse(engine.processFrame(hzAtCents(a, 2.0))) }
        assertTrue(engine.processFrame(hzAtCents(a, 2.0)))
        assertTrue(engine.state.isMatched)
    }

    @Test
    fun `sensitivity tiers need 7, 5 and 3 frames`() {
        assertEquals(7, TunerEngine.stabilityFramesRequired("low"))
        assertEquals(5, TunerEngine.stabilityFramesRequired("medium"))
        assertEquals(3, TunerEngine.stabilityFramesRequired("high"))
    }

    @Test
    fun `a frame outside 5 cents resets the match streak`() {
        val a = guzhengString(8).frequency
        repeat(4) { engine.processFrame(hzAtCents(a, 2.0)) }
        engine.processFrame(hzAtCents(a, 8.0))
        repeat(4) { assertFalse(engine.processFrame(hzAtCents(a, 2.0))) }
        assertTrue(engine.processFrame(hzAtCents(a, 2.0)))
    }

    @Test
    fun `matched state holds for 1_5 s then returns to listening`() {
        engine.sensitivity = "high"
        val a = guzhengString(8).frequency
        repeat(3) { engine.processFrame(a) }
        assertTrue(engine.state.isMatched)
        now += 1_499
        engine.processFrame(a)
        assertTrue(engine.state.isMatched)
        now += 1
        engine.processFrame(a)
        assertNull(engine.state.matchedUntilMs)
    }

    @Test
    fun `readings over 200 cents off are discarded`() {
        engine.selectString(8)
        engine.setPlayingReference(false)
        val before = engine.state.cents
        engine.processFrame(hzAtCents(guzhengString(8).frequency, 250.0))
        assertEquals(before, engine.state.cents, 0.0)
    }

    @Test
    fun `manual lock measures against the chosen string and ignores reference playback`() {
        engine.selectString(13)
        assertTrue(engine.state.manualLock)
        engine.setPlayingReference(true)
        engine.processFrame(guzhengString(13).frequency)
        assertFalse(engine.state.isSounding)
        engine.setPlayingReference(false)
        engine.processFrame(hzAtCents(guzhengString(13).frequency, 50.0))
        assertEquals(13, engine.state.activeStringId)
        assertEquals(50.0, engine.state.cents, 0.01)
    }

    @Test
    fun `tapping the locked string again returns to auto`() {
        engine.selectString(13)
        engine.selectString(13)
        assertFalse(engine.state.manualLock)
    }
}
