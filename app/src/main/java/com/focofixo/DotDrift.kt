package com.focofixo

/**
 * Posição vertical (fração 0..1, de cima para baixo) de cada ponto de uma borda. A aceleração vira velocidade
 * (inércia) com atrito, e a posição dá a volta na borda: os pontos deslizam em fluxo, sem ficar presos a um pixel.
 */
class DotDrift(count: Int) {
    val y = FloatArray(count) { (it + 0.5f) / count }
    private val v = FloatArray(count)

    /** driveY = offset.y do MotionFilter (positivo = para cima); negativo faz os pontos descerem. */
    fun update(driveY: Float, dt: Float) {
        for (i in y.indices) {
            // ganho por ponto (0.7..1.3): velocidades diferentes dão o efeito de paralaxe
            val gain = 0.7f + 0.6f * ((i * 5) % y.size) / (y.size - 1).coerceAtLeast(1)
            v[i] += (-driveY * GAIN * gain - v[i] / FRICTION_TAU) * dt
            val n = (y[i] + v[i] * dt) % 1f
            y[i] = if (n < 0f) n + 1f else n
        }
    }

    private companion object {
        const val GAIN = 0.6f        // telas/s² por unidade de offset
        const val FRICTION_TAU = 1.2f // s que o ponto leva para perder a velocidade
    }
}
