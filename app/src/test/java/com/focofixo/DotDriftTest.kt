package com.focofixo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class DotDriftTest {
    private val dt = 0.016f

    // diferença circular em (-0.5, 0.5]
    private fun delta(a: Float, b: Float): Float {
        var d = b - a
        if (d > 0.5f) d -= 1f
        if (d <= -0.5f) d += 1f
        return d
    }

    @Test fun startsEvenlySpaced() {
        val d = DotDrift(4)
        assertEquals(0.125f, d.y[0], 1e-6f)
        assertEquals(0.625f, d.y[2], 1e-6f)
    }

    @Test fun forwardDriveSlidesDown() {
        val d = DotDrift(4)
        val start = d.y.copyOf()
        repeat(30) { d.update(-0.5f, dt) } // offset.y < 0: acelerando para a frente
        for (i in 0 until 4) assertTrue(delta(start[i], d.y[i]) > 0f)
    }

    @Test fun keepsGlidingAfterDriveStopsThenSettles() {
        val d = DotDrift(4)
        repeat(30) { d.update(-0.5f, dt) }
        val before = d.y.copyOf()
        repeat(30) { d.update(0f, dt) }
        assertTrue(delta(before[0], d.y[0]) > 0f) // inércia
        repeat(1500) { d.update(0f, dt) }
        val rest = d.y.copyOf()
        repeat(60) { d.update(0f, dt) }
        assertTrue(abs(delta(rest[0], d.y[0])) < 0.001f) // parou
    }

    @Test fun positionsStayWrappedInUnitRange() {
        val d = DotDrift(6)
        repeat(2000) { d.update(if (it % 400 < 200) -1f else 1f, dt) }
        assertTrue(d.y.all { it >= 0f && it < 1f })
    }
}
