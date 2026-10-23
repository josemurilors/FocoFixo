package com.focofixo

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.WindowManager
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class AnchorService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val filter = MotionFilter(sensitivity = 1f)
    private lateinit var motion: MotionSource
    private var view: AnchorView? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) { stopSelf(); return START_NOT_STICKY }
        if (view != null) return START_NOT_STICKY // já ativo: um só overlay
        // startForegroundService exige startForeground logo; fazemos antes de qualquer saída
        enterForeground()
        if (!Settings.canDrawOverlays(this)) { stopSelf(); return START_NOT_STICKY }

        val v = AnchorView(this)
        motion = MotionSource(this)
        if (!startMotion(v, SAMPLE_RATES[1])) {
            Toast.makeText(this, "Este aparelho não tem sensor de movimento.", Toast.LENGTH_LONG).show()
            stopSelf(); return START_NOT_STICKY
        }
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        )
        (getSystemService(WINDOW_SERVICE) as WindowManager).addView(v, lp)
        view = v
        running = true
        scope.launch {
            SettingsRepo(this@AnchorService).flow.collect { s ->
                filter.sensitivity = s.sensitivity
                filter.threshold = s.threshold
                filter.smoothingTau = 0.03f + 0.4f * s.damping
                v.settings = s
                if (s.sampleHz != hz) { motion.stop(); startMotion(v, s.sampleHz) }
            }
        }
        return START_NOT_STICKY
    }

    private var hz = 0

    private fun startMotion(v: AnchorView, rate: Int): Boolean {
        hz = rate
        return motion.start(rate) { a, dt ->
            val o = filter.update(a.x, a.y, dt)
            v.presence = filter.presence
            v.step(o, dt)
        }
    }

    private fun enterForeground() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CHANNEL, "FocoFixo", NotificationManager.IMPORTANCE_LOW))
        val stop = PendingIntent.getService(
            this, 0, Intent(this, AnchorService::class.java).setAction(ACTION_STOP), PendingIntent.FLAG_IMMUTABLE,
        )
        val n = Notification.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentTitle("FocoFixo ativo")
            .addAction(Notification.Action.Builder(null, "Desligar", stop).build())
            .build()
        if (Build.VERSION.SDK_INT >= 34) startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        else startForeground(1, n)
    }

    override fun onDestroy() {
        if (::motion.isInitialized) motion.stop()
        view?.let { (getSystemService(WINDOW_SERVICE) as WindowManager).removeView(it) }
        view = null
        running = false
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL = "focofixo"
        private const val ACTION_STOP = "com.focofixo.STOP"
        @Volatile var running = false
            private set
        fun start(ctx: Context) { ctx.startForegroundService(Intent(ctx, AnchorService::class.java)) }
        fun stop(ctx: Context) { ctx.stopService(Intent(ctx, AnchorService::class.java)) }
    }
}
