package com.focofixo

data class Vec2(val x: Float, val y: Float)

/** Aceleração (m/s²) -> deslocamento normalizado oposto, em [-maxOffset, maxOffset]. */
class MotionFilter(var sensitivity: Float, private val maxOffset: Float = 1f) {
    private var lpX = 0f; private var lpY = 0f // média lenta: remove componente constante (gravidade/curva longa)
    private var smX = 0f; private var smY = 0f // suavização rápida do resultado

    fun update(ax: Float, ay: Float, dt: Float): Vec2 {
        val x = if (ax.isFinite()) ax else 0f
        val y = if (ay.isFinite()) ay else 0f
        val slow = dt / (SLOW_TAU + dt)
        val fast = dt / (FAST_TAU + dt)
        lpX += slow * (x - lpX); lpY += slow * (y - lpY)
        smX += fast * ((x - lpX) - smX); smY += fast * ((y - lpY) - smY)
        val k = -sensitivity / FULL_SCALE
        return Vec2((smX * k).coerceIn(-maxOffset, maxOffset), (smY * k).coerceIn(-maxOffset, maxOffset))
    }

    private companion object {
        const val SLOW_TAU = 1f
        const val FAST_TAU = 0.1f
        const val FULL_SCALE = 4f // m/s² que levam o ponto ao limite com sensibilidade 1
    }
}
