package com.focofixo

import kotlin.math.abs
import kotlin.math.exp

/** Multiplicador de tamanho dos pontos (1..2.5): sobe na hora com a aceleração para frente/trás e desce em cerca de 1 s. */
class SizeEnvelope {
    private var extra = 0f

    fun update(forwardAccel: Float, dt: Float): Float {
        val target = if (forwardAccel.isFinite()) (abs(forwardAccel) * GAIN).coerceAtMost(MAX_EXTRA) else 0f
        if (target > extra) extra = target
        extra *= exp(-dt / RELEASE_SEC)
        return 1f + extra
    }

    private companion object {
        const val GAIN = 0.5f // m/s² -> acréscimo de tamanho
        const val MAX_EXTRA = 1.5f
        const val RELEASE_SEC = 0.9f
    }
}
