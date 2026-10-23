package com.focofixo

import kotlin.math.abs
import kotlin.math.sign

data class Vec2(val x: Float, val y: Float)

/** Aceleração (m/s²) -> deslocamento normalizado oposto, em [-maxOffset, maxOffset]. */
class MotionFilter(var sensitivity: Float, private val maxOffset: Float = 1f) {
    var threshold = 0f      // m/s² abaixo dos quais o movimento é ignorado
    var smoothingTau = 0.1f // s; maior = mais suave e mais lento

    private var lpX = 0f; private var lpY = 0f // média lenta: remove componente constante (gravidade/curva longa)
    private var smX = 0f; private var smY = 0f // suavização rápida do resultado

    fun update(ax: Float, ay: Float, dt: Float): Vec2 {
        val x = if (ax.isFinite()) ax else 0f
        val y = if (ay.isFinite()) ay else 0f
        val slow = dt / (SLOW_TAU + dt)
        val fast = dt / (smoothingTau + dt)
        lpX += slow * (x - lpX); lpY += slow * (y - lpY)
        smX += fast * ((x - lpX) - smX); smY += fast * ((y - lpY) - smY)
        val k = -sensitivity / FULL_SCALE
        return Vec2((deadZone(smX) * k).coerceIn(-maxOffset, maxOffset), (deadZone(smY) * k).coerceIn(-maxOffset, maxOffset))
    }

    private fun deadZone(v: Float) = if (abs(v) <= threshold) 0f else v - sign(v) * threshold

    private companion object {
        const val SLOW_TAU = 1f
        const val FULL_SCALE = 4f // m/s² que levam o ponto ao limite com sensibilidade 1
    }
}
