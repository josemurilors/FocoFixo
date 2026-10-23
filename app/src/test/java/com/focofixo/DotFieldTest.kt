package com.focofixo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class DotFieldTest {
    private val dt = 0.016f

    private fun circ(a: Float, b: Float): Float {
        var d = b - a
        if (d > 0.5f) d -= 1f
        if (d <= -0.5f) d += 1f
        return d
    }

    @Test fun focusIsZeroAtCenterAndFullAtCorner() {
        assertEquals(0f, focusScale(0.5f, 0.5f), 1e-6f)
        assertEquals(1f, focusScale(0f, 0f), 1e-6f)
        assertEquals(1f, focusScale(1f, 1f), 1e-6f)
    }

    @Test fun hasColsTimesRowsDotsWithStaggeredOddRows() {
        val f = DotField(cols = 4, rows = 6)
        assertEquals(24, f.u.size)
        assertTrue(abs(f.u[4] - f.u[0]) > 0.05f) // linha 1 deslocada meia coluna em relação à 0
    }

    @Test fun yawRateScrollsDotsRight() {
        val f = DotField(4, 6)
        val x0 = f.u[0]
        repeat(30) { f.update(0f, 0f, rateX = 0.2f, rateDown = 0f, dt = dt) }
        assertTrue(circ(x0, f.u[0]) > 0f)
    }

    @Test fun pitchRateScrollsDotsDown() {
        val f = DotField(4, 6)
        val y0 = f.v[0]
        repeat(30) { f.update(0f, 0f, rateX = 0f, rateDown = 0.2f, dt = dt) }
        assertTrue(circ(y0, f.v[0]) > 0f)
    }

    @Test fun forwardDriveSlidesDownKeepsGlidingThenSettles() {
        val f = DotField(4, 6)
        val y0 = f.v[0]
        repeat(30) { f.update(0f, -0.5f, 0f, 0f, dt) } // offset.y < 0: acelerando para a frente
        assertTrue(circ(y0, f.v[0]) > 0f)
        val mid = f.v[0]
        repeat(30) { f.update(0f, 0f, 0f, 0f, dt) }
        assertTrue(circ(mid, f.v[0]) > 0f) // inércia
        repeat(1500) { f.update(0f, 0f, 0f, 0f, dt) }
        val rest = f.v[0]
        repeat(60) { f.update(0f, 0f, 0f, 0f, dt) }
        assertTrue(abs(circ(rest, f.v[0])) < 0.001f)
    }

    @Test fun positionsStayWrapped() {
        val f = DotField(5, 8)
        repeat(2000) { f.update(if (it % 400 < 200) 1f else -1f, if (it % 300 < 150) 1f else -1f, 0.5f, -0.5f, dt) }
        assertTrue(f.u.all { it >= 0f && it < 1f } && f.v.all { it >= 0f && it < 1f })
    }
}
