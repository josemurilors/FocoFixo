package com.focofixo

import kotlin.math.abs
import kotlin.math.sign
import kotlin.math.sqrt

/**
 * Giroscópio (rad/s) -> Vec2(yaw, pitch) no mundo: yaw = giro em torno da vertical (positivo = virar para a esquerda,
 * a paisagem passa para a direita da tela); pitch = giro em torno do eixo horizontal lateral à tela (positivo = topo
 * do aparelho se afastando do usuário). Estima o viés do giroscópio enquanto o aparelho está parado, para que o campo
 * não derive sozinho.
 */
class GyroFusion {
    private var bx = 0f; private var by = 0f; private var bz = 0f
    private var stillSec = 0f
    private var yaw = 0f; private var pitch = 0f

    /** w = giroscópio, a = aceleração linear, g = gravidade (leitura que aponta para cima), todos no aparelho. */
    fun update(
        wx: Float, wy: Float, wz: Float,
        ax: Float, ay: Float, az: Float,
        gx: Float, gy: Float, gz: Float,
        dt: Float,
    ): Vec2 {
        if (!(wx.isFinite() && wy.isFinite() && wz.isFinite() && ax.isFinite() && ay.isFinite() && az.isFinite() &&
                gx.isFinite() && gy.isFinite() && gz.isFinite())
        ) return Vec2(deadband(yaw), deadband(pitch))

        val still = ax * ax + ay * ay + az * az < STILL_ACCEL_SQ && wx * wx + wy * wy + wz * wz < STILL_GYRO_SQ
        if (still) {
            stillSec += dt
            if (stillSec > STILL_SETTLE_SEC) {
                val k = dt / (BIAS_TAU + dt)
                bx += k * (wx - bx); by += k * (wy - by); bz += k * (wz - bz)
            }
        } else stillSec = 0f

        val x = wx - bx; val y = wy - by; val z = wz - bz
        var yawNow = 0f
        var pitchNow = 0f
        val g = sqrt(gx * gx + gy * gy + gz * gz)
        if (g > 1e-3f) {
            val ux = gx / g; val uy = gy / g; val uz = gz / g
            yawNow = x * ux + y * uy + z * uz
            facing(gx, gy, gz)?.let { f ->
                // eixo lateral = cima x frente
                val sx = uy * f[2] - uz * f[1]
                val sy = uz * f[0] - ux * f[2]
                val sz = ux * f[1] - uy * f[0]
                pitchNow = x * sx + y * sy + z * sz
            }
        }
        val k = dt / (SMOOTH_TAU + dt)
        yaw += k * (yawNow - yaw)
        pitch += k * (pitchNow - pitch)
        return Vec2(deadband(yaw), deadband(pitch))
    }

    private fun deadband(v: Float) = if (abs(v) <= DEADBAND) 0f else v - sign(v) * DEADBAND

    private companion object {
        const val STILL_ACCEL_SQ = 0.09f // (0,3 m/s²)²
        const val STILL_GYRO_SQ = 0.0025f // (0,05 rad/s)²
        const val STILL_SETTLE_SEC = 0.8f
        const val BIAS_TAU = 1.5f
        const val SMOOTH_TAU = 0.03f
        const val DEADBAND = 0.01f // rad/s abaixo disso conta como zero
    }
}
