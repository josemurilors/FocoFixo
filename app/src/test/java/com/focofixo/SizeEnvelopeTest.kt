package com.focofixo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SizeEnvelopeTest {
    private val dt = 0.016f

    @Test fun growsWithForwardOrBackwardAccelAndReleasesSlowly() {
        val e = SizeEnvelope()
        var b = 1f
        repeat(10) { b = e.update(-3f, dt) } // frear/acelerar: o sinal não importa
        assertTrue(b > 1.5f)
        val peak = b
        repeat(10) { b = e.update(0f, dt) }
        assertTrue(b < peak && b > 1.2f) // decai devagar
        repeat(300) { b = e.update(0f, dt) }
        assertEquals(1f, b, 0.01f)
    }

    @Test fun isCapped() {
        val e = SizeEnvelope()
        var b = 1f
        repeat(100) { b = e.update(100f, dt) }
        assertTrue(b <= 2.5f)
    }

    @Test fun nonFiniteIsIgnored() {
        assertEquals(1f, SizeEnvelope().update(Float.NaN, dt), 1e-6f)
    }
}
