package com.focofixo

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.Surface
import android.view.WindowManager

/** Entrega aceleração (m/s²) nos eixos da TELA: x para a direita, y para cima, já corrigida pela rotação. */
class MotionSource(private val context: Context) : SensorEventListener {
    private val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private var onSample: ((Float, Float, Float) -> Unit)? = null
    private var lastNs = 0L

    /** onSample(ax, ay, dtSeconds). Retorna false se o aparelho não tem sensor de aceleração. */
    fun start(onSample: (Float, Float, Float) -> Unit): Boolean {
        val sensor = sm.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
            ?: sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) ?: return false
        this.onSample = onSample
        lastNs = 0L
        return sm.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
    }

    fun stop() {
        sm.unregisterListener(this)
        onSample = null
    }

    override fun onSensorChanged(e: SensorEvent) {
        val dt = if (lastNs == 0L) 0.016f else ((e.timestamp - lastNs) / 1e9f).coerceIn(0.001f, 0.1f)
        lastNs = e.timestamp
        val x = e.values[0]
        val y = e.values[1]
        @Suppress("DEPRECATION")
        val rot = (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.rotation
        when (rot) {
            Surface.ROTATION_90 -> onSample?.invoke(-y, x, dt)
            Surface.ROTATION_180 -> onSample?.invoke(-x, -y, dt)
            Surface.ROTATION_270 -> onSample?.invoke(y, -x, dt)
            else -> onSample?.invoke(x, y, dt)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
