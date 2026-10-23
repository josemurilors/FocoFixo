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

    @Test fun clamped() {
        val f = MotionFilter(sensitivity = 3f)
        repeat(50) {
            val v = f.update(1000f, 1000f, dt)
            assertTrue(abs(v.x) <= 1f && abs(v.y) <= 1f)
        }
    }
}
