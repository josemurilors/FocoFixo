package com.focofixo

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.WindowManager

/** Entrega Vec2(lateral, frente) em m/s² (ver [screenAccel]) a cada leitura do sensor. */
class MotionSource(private val context: Context) : SensorEventListener {
    private val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val linear = sm.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
    private val gravitySensor = sm.getDefaultSensor(Sensor.TYPE_GRAVITY)
    private val accel = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gravity = FloatArray(3)
    private var onSample: ((Vec2, Float) -> Unit)? = null
    private var lastNs = 0L

    /** onSample(aceleração, dtSegundos). Retorna false se o aparelho não tem sensores suficientes. */
    fun start(hz: Int, onSample: (Vec2, Float) -> Unit): Boolean {
        val period = 1_000_000 / hz
        val ok = if (linear != null && gravitySensor != null) {
            sm.registerListener(this, gravitySensor, period) && sm.registerListener(this, linear, period)
        } else if (accel != null) {
            sm.registerListener(this, accel, period)
        } else return false
        if (!ok) { sm.unregisterListener(this); return false }
        this.onSample = onSample
        lastNs = 0L
        return true
    }

    fun stop() {
        sm.unregisterListener(this)
        onSample = null
    }

    override fun onSensorChanged(e: SensorEvent) {
        when (e.sensor.type) {
            Sensor.TYPE_GRAVITY -> { e.values.copyInto(gravity, endIndex = 3); return }
            Sensor.TYPE_ACCELEROMETER -> { // sem sensores fusionados: gravidade = média lenta, linear = resto
                for (i in 0..2) gravity[i] += 0.02f * (e.values[i] - gravity[i])
                emit(e, e.values[0] - gravity[0], e.values[1] - gravity[1], e.values[2] - gravity[2])
            }
            else -> emit(e, e.values[0], e.values[1], e.values[2])
        }
    }

    private fun emit(e: SensorEvent, ax: Float, ay: Float, az: Float) {
        val dt = if (lastNs == 0L) 0.016f else ((e.timestamp - lastNs) / 1e9f).coerceIn(0.001f, 0.1f)
        lastNs = e.timestamp
        @Suppress("DEPRECATION")
        val rot = (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.rotation
        onSample?.invoke(screenAccel(ax, ay, az, gravity[0], gravity[1], gravity[2], rot), dt)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
