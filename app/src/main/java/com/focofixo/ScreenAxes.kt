package com.focofixo

import kotlin.math.sqrt

/**
 * Direção horizontal para onde a tela "olha para longe" (com o aparelho em pé diante do usuário, é o sentido do
 * veículo), como vetor unitário no referencial do aparelho. Null com o aparelho quase deitado, onde ela não é definida.
 */
fun facing(gx: Float, gy: Float, gz: Float): FloatArray? {
    val g2 = gx * gx + gy * gy + gz * gz
    if (g2 < 1e-6f) return null
    // (0,0,-1) sem a componente ao longo da gravidade
    val fx = gz * gx / g2
    val fy = gz * gy / g2
    val fz = -1f + gz * gz / g2
    val fn = sqrt(fx * fx + fy * fy + fz * fz)
    return if (fn < FLAT_LIMIT) null else floatArrayOf(fx / fn, fy / fn, fz / fn)
}

/**
 * Aceleração linear (a) e gravidade (g) no referencial do aparelho -> Vec2(lateral, frente):
 * lateral = para a direita da tela; frente = sentido de [facing]. rotation = Surface.ROTATION_* (0..3).
 */
fun screenAccel(ax: Float, ay: Float, az: Float, gx: Float, gy: Float, gz: Float, rotation: Int): Vec2 {
    if (!(ax.isFinite() && ay.isFinite() && az.isFinite() && gx.isFinite() && gy.isFinite() && gz.isFinite())) {
        return Vec2(0f, 0f)
    }
    val lateral = when (rotation) { 1 -> -ay; 2 -> -ax; 3 -> ay; else -> ax }
    val up = when (rotation) { 1 -> ax; 2 -> -ay; 3 -> -ax; else -> ay }
    if (gx * gx + gy * gy + gz * gz < 1e-6f) return Vec2(lateral, 0f)
    // shortcut: aparelho quase deitado usa o eixo "cima da tela"; upgrade se incomodar
    val f = facing(gx, gy, gz)
    val forward = if (f == null) up else ax * f[0] + ay * f[1] + az * f[2]
    return Vec2(lateral, forward)
}

private const val FLAT_LIMIT = 0.35f
