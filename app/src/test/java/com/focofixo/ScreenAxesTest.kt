package com.focofixo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenAxesTest {
    private val eps = 1e-4f

    @Test fun uprightForwardIsMinusZ() {
        val v = screenAccel(0f, 0f, -2f, 0f, 9.8f, 0f, rotation = 0)
        assertEquals(0f, v.x, eps)
        assertEquals(2f, v.y, eps)
    }

    @Test fun uprightLateralIsDeviceX() {
        val v = screenAccel(1.5f, 0f, 0f, 0f, 9.8f, 0f, rotation = 0)
        assertEquals(1.5f, v.x, eps)
        assertEquals(0f, v.y, eps)
    }

    @Test fun landscapeLateralAndForward() {
        // aparelho girado 90°: topo à esquerda, gravidade (reação) no eixo +x
        val lat = screenAccel(0f, -1f, 0f, 9.8f, 0f, 0f, rotation = 1)
        assertEquals(1f, lat.x, eps)
        val fwd = screenAccel(0f, 0f, -2f, 9.8f, 0f, 0f, rotation = 1)
        assertEquals(2f, fwd.y, eps)
    }

    @Test fun flatFallsBackToScreenUp() {
        val v = screenAccel(0f, 2f, 0f, 0f, 0f, 9.8f, rotation = 0)
        assertEquals(2f, v.y, eps)
    }

    @Test fun nonFiniteIsZero() {
        val v = screenAccel(Float.NaN, 1f, 1f, 0f, 0f, 0f, rotation = 0)
        assertTrue(v.x.isFinite() && v.y.isFinite())
    }
}
