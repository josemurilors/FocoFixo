package com.focofixo

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.View

/** Pontos nas bordas esquerda e direita; `offset` (em [-1,1], y para cima) os desloca. */
class AnchorView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val border = Paint(Paint.ANTI_ALIAS_FLAG)

    private val drift = DotDrift(ROWS)
    private var offset = Vec2(0f, 0f)

    /** Um passo de animação: o offset lateral desloca os pontos; o vertical os faz deslizar com inércia. */
    fun step(o: Vec2, dt: Float) {
        offset = o
        drift.update(o.y, dt)
        invalidate()
    }
    var presence = 1f // 0..1, vindo do MotionFilter
    var settings: AnchorSettings = AnchorSettings()
        set(v) { field = v; invalidate() }

    override fun onDraw(canvas: Canvas) {
        val d = resources.displayMetrics.density
        val r = settings.dotSizeDp * d / 2
        val fade = if (settings.fadeIdle) presence else 1f
        val a = (settings.opacity * fade * 255).toInt()
        if (a == 0) return
        val color = DOT_COLORS[settings.colorIndex].second
        paint.color = color
        // contorno de cor oposta: o ponto fica visível em fundo claro e escuro
        border.color = if (color == 0xFFFFFFFF.toInt()) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
        paint.alpha = a
        border.alpha = a
        val reach = width * 0.12f
        val margin = r * 2.5f
        repeat(ROWS) { i ->
            // ganho por ponto (0.7..1.3): cada ponto se move um pouco diferente, como no recurso da Apple
            val gain = 0.7f + 0.6f * ((i * 3) % ROWS) / (ROWS - 1)
            val dx = offset.x * reach * gain
            val y = height * drift.y[i]
            for (x in floatArrayOf(margin + dx, width - margin + dx)) {
                canvas.drawCircle(x, y, r + d, border)
                canvas.drawCircle(x, y, r, paint)
            }
        }
    }

    private companion object { const val ROWS = 6 }
}
