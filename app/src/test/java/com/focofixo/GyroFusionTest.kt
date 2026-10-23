package com.focofixo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class GyroFusionTest {
    private val dt = 0.01f

    // aparelho em pé (retrato): a leitura de gravidade aponta para cima da tela
    private fun upright(f: GyroFusion, wx: Float, wy: Float, wz: Float, steps: Int): Vec2 {
        var r = Vec2(0f, 0f)
        repeat(steps) { r = f.update(wx, wy, wz, 0f, 0f, 0f, 0f, 9.8f, 0f, dt) }
        return r
    }

    @Test fun biasIsLearnedWhenStill() {
        val r = upright(GyroFusion(), 0.02f, 0.01f, 0.015f, steps = 1500)
        assertTrue(abs(r.x) < 0.005f && abs(r.y) < 0.005f)
    }

    @Test fun yawIsRotationAboutWorldUp() {
        val r = upright(GyroFusion(), 0f, 0.5f, 0f, steps = 200)
        assertEquals(0.5f, r.x, 0.05f)
        assertEquals(0f, r.y, 0.05f)
    }

    @Test fun pitchingTheTopAwayIsPositivePitch() {
        val r = upright(GyroFusion(), -0.5f, 0f, 0f, steps = 200)
        assertEquals(0.5f, r.y, 0.05f)
        assertEquals(0f, r.x, 0.05f)
    }

    @Test fun flatPhoneHasYawOnlyAndNoPitch() {
        val f = GyroFusion()
        var r = Vec2(0f, 0f)
        repeat(200) { r = f.update(0f, 0f, 0.5f, 0f, 0f, 0f, 0f, 0f, 9.8f, dt) }
        assertEquals(0.5f, r.x, 0.05f)
        assertEquals(0f, r.y, 1e-6f)
    }

    @Test fun nonFiniteInputIsZero() {
        val r = GyroFusion().update(Float.NaN, 0f, 0f, 0f, 0f, 0f, 0f, 9.8f, 0f, dt)
        assertTrue(r.x.isFinite() && r.y.isFinite())
    }
}
