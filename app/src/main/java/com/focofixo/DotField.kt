package com.focofixo

import kotlin.math.hypot

/** 0 no centro da tela, 1 nas bordas e cantos: mantém o conteúdo livre e os pontos na visão periférica. */
fun focusScale(u: Float, v: Float): Float {
    val d = (hypot(u - 0.5f, v - 0.5f) / HALF_DIAGONAL).coerceIn(0f, 1f)
    val t = ((d - FOCUS_INNER) / (FOCUS_OUTER - FOCUS_INNER)).coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

private val HALF_DIAGONAL = hypot(0.5f, 0.5f)
private const val FOCUS_INNER = 0.3f
private const val FOCUS_OUTER = 0.85f

/**
 * Grade hexagonal de pontos que cobre a tela e dá a volta nas bordas. Posições em frações da tela (0..1, v para baixo).
 * Acelerar vira velocidade com atrito (inércia); as taxas de giro rolam o campo diretamente.
 */
class DotField(val cols: Int, val rows: Int) {
    val u = FloatArray(cols * rows)
    val v = FloatArray(cols * rows)
    private val homeU = FloatArray(cols * rows)
    private val homeV = FloatArray(cols * rows)
    private var velX = 0f
    private var velDown = 0f
    // double: o deslocamento só cresce, e float perderia precisão em viagens longas
    private var offX = 0.0
    private var offDown = 0.0

    init {
        for (r in 0 until rows) for (c in 0 until cols) {
            val i = r * cols + c
            homeU[i] = (c + 0.5f + if (r % 2 == 1) 0.5f else 0f) / cols
            homeV[i] = (r + 0.5f) / rows
        }
        place()
    }

    /**
     * driveX/driveY = offset do MotionFilter (y para cima); rateX = telas/s para a direita; rateDown = telas/s para baixo.
     */
    fun update(driveX: Float, driveY: Float, rateX: Float, rateDown: Float, dt: Float, speed: Float = 1f) {
        velX += (driveX * GAIN * speed - velX / FRICTION_TAU) * dt
        velDown += (-driveY * GAIN * speed - velDown / FRICTION_TAU) * dt
        offX += (velX + rateX * speed) * dt
        offDown += (velDown + rateDown * speed) * dt
        place()
    }

    private fun place() {
        for (i in u.indices) {
            val g = dotGain(i) // paralaxe: cada ponto anda um pouco diferente
            u[i] = wrap(homeU[i] + (offX * g).toFloat())
            v[i] = wrap(homeV[i] + (offDown * g).toFloat())
        }
    }

    private fun wrap(x: Float): Float {
        val r = x % 1f
        return if (r < 0f) r + 1f else r
    }

    private companion object {
        const val GAIN = 0.6f
        const val FRICTION_TAU = 1.2f
    }
}
