package com.focofixo

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.View

/** Pontos nas bordas esquerda e direita; `offset` (em [-1,1], y para cima) os desloca. */
class AnchorView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF000000.toInt() }
    // contorno branco: o ponto fica visível em fundo claro e escuro
    private val border = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt() }

    var offset: Vec2 = Vec2(0f, 0f)
        set(v) { field = v; invalidate() }
    var settings: AnchorSettings = AnchorSettings()
        set(v) { field = v; invalidate() }

    override fun onDraw(canvas: Canvas) {
        val d = resources.displayMetrics.density
        val r = settings.dotSizeDp * d / 2
        val a = (settings.opacity * 255).toInt()
        paint.alpha = a
        border.alpha = a
        val reach = width * 0.12f
        val dx = offset.x * reach
        val dy = -offset.y * reach
        val margin = r * 2.5f
        repeat(ROWS) { i ->
            val y = height * (i + 1f) / (ROWS + 1f) + dy
            for (x in floatArrayOf(margin + dx, width - margin + dx)) {
                canvas.drawCircle(x, y, r + d, border)
                canvas.drawCircle(x, y, r, paint)
            }
        }
    }

    private companion object { const val ROWS = 4 }
}
