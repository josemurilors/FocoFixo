package com.focofixo

import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Aceleração linear (a) e gravidade (g) no referencial do aparelho -> Vec2(lateral, frente):
 * lateral = para a direita da tela; frente = direção horizontal para onde a tela "olha para longe"
 * (o sentido do veículo, com o aparelho em pé diante do usuário). rotation = Surface.ROTATION_* (0..3).
 */
fun screenAccel(ax: Float, ay: Float, az: Float, gx: Float, gy: Float, gz: Float, rotation: Int): Vec2 {
    if (!(ax.isFinite() && ay.isFinite() && az.isFinite() && gx.isFinite() && gy.isFinite() && gz.isFinite())) {
        return Vec2(0f, 0f)
    }
    val lateral = when (rotation) { 1 -> -ay; 2 -> -ax; 3 -> ay; else -> ax }
    val up = when (rotation) { 1 -> ax; 2 -> -ay; 3 -> -ax; else -> ay }
    val g2 = gx * gx + gy * gy + gz * gz
    if (g2 < 1e-6f) return Vec2(lateral, 0f)
    // f = (0,0,-1) sem a componente ao longo da gravidade: horizontal, para longe do usuário
    val fx = gz * gx / g2
    val fy = gz * gy / g2
    val fz = -1f + gz * gz / g2
    val fn = sqrt(fx * fx + fy * fy + fz * fz)
    // shortcut: aparelho quase deitado (|f| pequeno) usa o eixo "cima da tela"; upgrade se incomodar
    val forward = if (fn < FLAT_LIMIT) up else (ax * fx + ay * fy + az * fz) / fn
    return Vec2(lateral, if (abs(forward) < 1e-9f) 0f else forward)
}

private const val FLAT_LIMIT = 0.35f
