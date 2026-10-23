package com.focofixo

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Decide se a âncora aparece no modo Auto a partir dos fixes de localização.
 * Liga com 2 fixes seguidos acima do limite; desliga após [OFF_AFTER_MS] abaixo de 60% dele (semáforo não apaga).
 */
class SpeedGate(var limitKmh: Float = 20f) {
    var visible = false
        private set
    /** Velocidade perto do limite (ou âncora visível): vale pedir GPS rápido. */
    var fast = false
        private set
    private var above = 0
    private var slowSinceMs = -1L
    private var lastLat = 0.0
    private var lastLon = 0.0
    private var lastMs = -1L

    /** speedMs = velocidade do fix em m/s, ou null se o provedor não informar (rede): usa distância/tempo. */
    fun update(speedMs: Float?, lat: Double, lon: Double, timeMs: Long): Boolean {
        val dtMs = timeMs - lastMs
        val kmh = when {
            speedMs != null && speedMs.isFinite() -> speedMs * 3.6f
            lastMs >= 0 && dtMs > 0 -> (distanceM(lastLat, lastLon, lat, lon) / dtMs * 3600.0).toFloat()
            else -> 0f
        }
        lastLat = lat; lastLon = lon; lastMs = timeMs

        above = if (kmh >= limitKmh) above + 1 else 0
        if (above >= 2) visible = true
        if (kmh >= limitKmh * 0.6f) slowSinceMs = -1L
        else if (slowSinceMs < 0) slowSinceMs = timeMs
        if (visible && slowSinceMs >= 0 && timeMs - slowSinceMs >= OFF_AFTER_MS) visible = false
        fast = visible || kmh >= limitKmh * 0.5f
        return visible
    }

    private companion object {
        const val OFF_AFTER_MS = 3 * 60_000L

        fun distanceM(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
            val dLat = Math.toRadians(lat2 - lat1)
            val dLon = Math.toRadians(lon2 - lon1)
            val h = sin(dLat / 2).let { it * it } +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).let { it * it }
            return 2 * 6_371_000.0 * asin(sqrt(h))
        }
    }
}
