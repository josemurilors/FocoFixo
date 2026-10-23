package com.focofixo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class MotionFilterTest {
    private val dt = 0.016f

    @Test fun oppositeDirection() {
        val f = MotionFilter(sensitivity = 1f)
        var v = Vec2(0f, 0f)
        repeat(30) { v = f.update(2f, 0f, dt) }
        assertTrue(v.x < 0f)
    }

    @Test fun constantAccelerationDecays() {
        val f = MotionFilter(sensitivity = 1f)
        var v = Vec2(0f, 0f)
        repeat(600) { v = f.update(3f, 3f, dt) }
        assertTrue(abs(v.x) < 0.05f && abs(v.y) < 0.05f)
    }

    @Test fun nanAndHugeAreSafe() {
        val f = MotionFilter(sensitivity = 1f)
        val v = f.update(Float.NaN, Float.POSITIVE_INFINITY, dt)
        assertTrue(v.x.isFinite() && v.y.isFinite())
        assertTrue(abs(v.x) <= 1f && abs(v.y) <= 1f)
    }

    @Test fun zeroSensitivity() {
        val f = MotionFilter(sensitivity = 0f)
        var v = Vec2(1f, 1f)
        repeat(20) { v = f.update(50f, -50f, dt) }
        assertEquals(0f, v.x, 0f)
        assertEquals(0f, v.y, 0f)
    }

    @Test fun thresholdSilencesSmallMotion() {
        val f = MotionFilter(sensitivity = 1f).apply { threshold = 0.5f }
        var v = Vec2(1f, 1f)
        repeat(60) { v = f.update(if (it % 2 == 0) 0.2f else -0.2f, 0f, dt) }
        assertEquals(0f, v.x, 0f)
    }

    @Test fun slowerSmoothingReactsLess() {
        val fast = MotionFilter(sensitivity = 1f).apply { smoothingTau = 0.03f }
        val slow = MotionFilter(sensitivity = 1f).apply { smoothingTau = 0.4f }
        var a = Vec2(0f, 0f); var b = Vec2(0f, 0f)
        repeat(5) { a = fast.update(2f, 0f, dt); b = slow.update(2f, 0f, dt) }
        assertTrue(abs(a.x) > abs(b.x))
    }

    @Test fun presenceFadesAtRestAndReturnsWithMotion() {
        val f = MotionFilter(sensitivity = 1f)
        repeat(60) { f.update(if (it % 20 < 10) 3f else -3f, 0f, dt) }
        assertTrue(f.presence > 0.8f)
        repeat(1500) { f.update(0f, 0f, dt) } // 24 s parado
        assertTrue(f.presence < 0.05f)
        repeat(40) { f.update(if (it % 20 < 10) 3f else -3f, 0f, dt) }
        assertTrue(f.presence > 0.5f)
    }

    @Test fun clamped() {
        val f = MotionFilter(sensitivity = 3f)
        repeat(50) {
            val v = f.update(1000f, 1000f, dt)
            assertTrue(abs(v.x) <= 1f && abs(v.y) <= 1f)
        }
    }
}
