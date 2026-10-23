package com.focofixo

import kotlin.math.abs
import kotlin.math.hypot
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
        val out = Vec2((deadZone(smX) * k).coerceIn(-maxOffset, maxOffset), (deadZone(smY) * k).coerceIn(-maxOffset, maxOffset))
        // sobe rápido com movimento, desce devagar em repouso
        val target = (hypot(out.x, out.y) / PRESENCE_FULL).coerceAtMost(1f)
        val tau = if (target > presence) ATTACK_TAU else RELEASE_TAU
        presence += dt / (tau + dt) * (target - presence)
        val hard = (hypot(out.x, out.y) / INTENSITY_FULL).coerceAtMost(1f)
        intensity += dt / ((if (hard > intensity) ATTACK_TAU else RELEASE_TAU) + dt) * (hard - intensity)
        return out
    }

    /** 0 = movimento suave, 1 = brusco; define quantos pontos aparecem. */
    var intensity = 0f
        private set

    /** 0 = parado (pontos somem), 1 = movimento claro. */
    var presence = 0f
        private set

    private fun deadZone(v: Float) = if (abs(v) <= threshold) 0f else v - sign(v) * threshold

    private companion object {
        const val SLOW_TAU = 1f
        const val FULL_SCALE = 4f // m/s² que levam o ponto ao limite com sensibilidade 1
        const val PRESENCE_FULL = 0.1f // deslocamento normalizado que já deixa os pontos totalmente visíveis
        const val INTENSITY_FULL = 0.6f // deslocamento normalizado considerado "brusco"
        const val ATTACK_TAU = 0.2f
        const val RELEASE_TAU = 2f
    }
}
