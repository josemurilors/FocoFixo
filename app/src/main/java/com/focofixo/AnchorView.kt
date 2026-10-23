package com.focofixo

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.View

/** Referência visual nas bordas (pontos ou grade) ou horizonte artificial; reage ao `offset` (em [-1,1], y para cima). */
class AnchorView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap = Paint.Cap.ROUND }
    private val border = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap = Paint.Cap.ROUND }

    private var drift = DotDrift(AnchorSettings().dotCount)
    private var offset = Vec2(0f, 0f)

    /** Um passo de animação: o offset lateral desloca os pontos; o vertical os faz deslizar com inércia. */
    fun step(o: Vec2, dt: Float) {
        offset = o
        drift.update(o.y, dt, settings.speed)
        invalidate()
    }
    var presence = 1f // 0..1, vindo do MotionFilter
    var settings: AnchorSettings = AnchorSettings()
        set(v) {
            if (v.dotCount != field.dotCount) drift = DotDrift(v.dotCount)
            field = v
            invalidate()
        }

    override fun onDraw(canvas: Canvas) {
        val d = resources.displayMetrics.density
        val r = settings.dotSizeDp * d / 2
        val fade = if (settings.fadeIdle) presence else 1f
        val a = (settings.opacity * fade * 255).toInt()
        if (a == 0) return
        val color = DOT_COLORS[settings.colorIndex].second
        paint.color = color
        // contorno de cor oposta: o desenho fica visível em fundo claro e escuro
        border.color = if (color == 0xFFFFFFFF.toInt()) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
        paint.alpha = a
        border.alpha = a
        if (settings.pattern == PATTERN_HORIZON) drawHorizon(canvas, r, d) else drawEdges(canvas, r, d)
    }

    private fun drawEdges(canvas: Canvas, r: Float, d: Float) {
        val reach = width * 0.12f
        val margin = r * 2.5f
        val grid = settings.pattern == PATTERN_GRID
        paint.strokeWidth = 2f * d
        border.strokeWidth = 4f * d
        val ys = drift.y // cópia local: o drift pode ser trocado quando a quantidade muda
        for (i in ys.indices) {
            val dx = offset.x * reach * dotGain(i)
            val y = height * ys[i]
            for (x in floatArrayOf(margin + dx, width - margin + dx)) {
                if (grid) { // risco horizontal curto, como uma régua nas laterais
                    canvas.drawLine(x - r * 1.5f, y, x + r * 1.5f, y, border)
                    canvas.drawLine(x - r * 1.5f, y, x + r * 1.5f, y, paint)
                } else {
                    canvas.drawCircle(x, y, r + d, border)
                    canvas.drawCircle(x, y, r, paint)
                }
            }
        }
    }

    /** Linha que inclina com a curva e sobe/desce com acelerar/frear. */
    private fun drawHorizon(canvas: Canvas, r: Float, d: Float) {
        paint.strokeWidth = (r * 0.3f).coerceAtLeast(2f * d)
        border.strokeWidth = paint.strokeWidth + 2f * d
        canvas.save()
        canvas.rotate(offset.x * MAX_TILT_DEG, width / 2f, height / 2f)
        val y = height / 2f - offset.y * height * 0.1f
        canvas.drawLine(-width.toFloat(), y, width * 2f, y, border)
        canvas.drawLine(-width.toFloat(), y, width * 2f, y, paint)
        canvas.restore()
    }

    private companion object { const val MAX_TILT_DEG = 25f }
}
