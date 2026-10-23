package com.focofixo

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.WindowManager

/** accel = Vec2(lateral, frente) em m/s² (ver [screenAccel]); rates = Vec2(yaw, pitch) em rad/s (ver [GyroFusion]). */
class MotionSample(val accel: Vec2, val rates: Vec2, val dt: Float)

class MotionSource(private val context: Context) : SensorEventListener {
    private val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val linear = sm.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
    private val gravitySensor = sm.getDefaultSensor(Sensor.TYPE_GRAVITY)
    private val accel = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyro = sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val gravity = FloatArray(3)
    private val w = FloatArray(3)
    private val fusion = GyroFusion()
    private var onSample: ((MotionSample) -> Unit)? = null
    private var lastNs = 0L

    /** Retorna false se o aparelho não tem sensores suficientes. Sem giroscópio, yaw e pitch ficam em zero. */
    fun start(hz: Int, onSample: (MotionSample) -> Unit): Boolean {
        val period = 1_000_000 / hz
        val ok = if (linear != null && gravitySensor != null) {
            sm.registerListener(this, gravitySensor, period) && sm.registerListener(this, linear, period)
        } else if (accel != null) {
            sm.registerListener(this, accel, period)
        } else return false
        if (!ok) { sm.unregisterListener(this); return false }
        gyro?.let { sm.registerListener(this, it, period) }
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
            Sensor.TYPE_GRAVITY -> e.values.copyInto(gravity, endIndex = 3)
            Sensor.TYPE_GYROSCOPE -> e.values.copyInto(w, endIndex = 3)
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
        val a = screenAccel(ax, ay, az, gravity[0], gravity[1], gravity[2], rot)
        val rates = fusion.update(w[0], w[1], w[2], ax, ay, az, gravity[0], gravity[1], gravity[2], dt)
        onSample?.invoke(MotionSample(a, rates, dt))
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
