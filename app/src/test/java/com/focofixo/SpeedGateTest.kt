package com.focofixo

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeedGateTest {
    private val kmh30 = 30f / 3.6f

    @Test fun needsTwoFixesAboveLimit() {
        val g = SpeedGate(20f)
        assertFalse(g.update(kmh30, 0.0, 0.0, 0))
        assertTrue(g.update(kmh30, 0.0, 0.0, 2_000))
    }

    @Test fun trafficLightDoesNotHide() {
        val g = SpeedGate(20f)
        g.update(kmh30, 0.0, 0.0, 0); g.update(kmh30, 0.0, 0.0, 2_000)
        assertTrue(g.update(0f, 0.0, 0.0, 60_000)) // 1 min parado
        assertTrue(g.update(kmh30, 0.0, 0.0, 120_000))
        assertTrue(g.update(0f, 0.0, 0.0, 200_000)) // contador recomeça
    }

    @Test fun hidesAfterThreeMinutesSlow() {
        val g = SpeedGate(20f)
        g.update(kmh30, 0.0, 0.0, 0); g.update(kmh30, 0.0, 0.0, 2_000)
        g.update(0f, 0.0, 0.0, 10_000)
        assertTrue(g.update(0f, 0.0, 0.0, 189_000))
        assertFalse(g.update(0f, 0.0, 0.0, 190_000))
        assertFalse(g.fast)
    }

    @Test fun withoutSpeedUsesDistance() {
        val g = SpeedGate(20f)
        g.update(null, 0.0, 0.0, 0)
        // ~0,0025° de latitude ≈ 278 m em 30 s ≈ 33 km/h
        g.update(null, 0.0025, 0.0, 30_000)
        assertTrue(g.update(null, 0.005, 0.0, 60_000))
    }

    @Test fun fastNearLimit() {
        val g = SpeedGate(20f)
        g.update(12f / 3.6f, 0.0, 0.0, 0)
        assertTrue(g.fast)
        assertFalse(g.visible)
    }
}
