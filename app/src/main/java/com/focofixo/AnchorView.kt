package com.focofixo

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.View

/**
 * Referência visual: pontos ou grade nas bordas, horizonte artificial, ou campo de pontos na tela inteira.
 * Reage ao `offset` (em [-1,1], y para cima) e às taxas de giro.
 */
class AnchorView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap = Paint.Cap.ROUND }
    private val border = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap = Paint.Cap.ROUND }

    private var drift = DotDrift(AnchorSettings().dotCount)
    private var dots = newField(AnchorSettings().dotCount)
    private var offset = Vec2(0f, 0f)

    /**
     * Um passo de animação: o offset lateral desloca os pontos; o vertical e as taxas de giro (yaw, pitch em rad/s)
     * os fazem deslizar com inércia.
     */
    fun step(o: Vec2, rates: Vec2, dt: Float) {
        val side = if (settings.invertLateral) -1f else 1f
        offset = Vec2(o.x * side, o.y)
        drift.update(o.y, dt, settings.speed)
        // virar para a esquerda (yaw > 0) faz a paisagem passar para a direita; inclinar o topo para longe, para baixo
        dots.update(o.x * side, o.y, rates.x * YAW_GAIN * side, rates.y * PITCH_GAIN, dt, settings.speed)
        invalidate()
    }
    var presence = 1f  // 0..1, vindo do MotionFilter
    var intensity = 1f // 0..1: movimento mais brusco acende mais pontos
    var sizeBoost = 1f // 1..2.5: aceleração para frente/trás aumenta os pontos
    private var baseAlpha = 0
    private var baseR = 0f
    var settings: AnchorSettings = AnchorSettings()
        set(v) {
            if (v.dotCount != field.dotCount) { drift = DotDrift(v.dotCount); dots = newField(v.dotCount) }
            field = v
            invalidate()
        }

    override fun onDraw(canvas: Canvas) {
        val d = resources.displayMetrics.density
        baseR = settings.dotSizeDp * d / 2
        val r = baseR * (if (settings.sizeBoost) sizeBoost else 1f)
        val fade = if (settings.fadeIdle) presence else 1f
        val a = (settings.opacity * fade * 255).toInt()
        if (a == 0) return
        val color = DOT_COLORS[settings.colorIndex].second
        paint.color = color
        // contorno de cor oposta: o desenho fica visível em fundo claro e escuro
        border.color = if (color == 0xFFFFFFFF.toInt()) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
        baseAlpha = a
        paint.alpha = a
        border.alpha = a
        when (settings.pattern) {
            PATTERN_HORIZON -> drawHorizon(canvas, r, d)
            PATTERN_FIELD -> drawField(canvas, r, d)
            else -> drawEdges(canvas, r, d)
        }
    }

    /** Campo na tela inteira: os pontos somem no centro e ficam inteiros nas bordas e cantos (modo Focus). */
    private fun drawField(canvas: Canvas, r: Float, d: Float) {
        val f = dots // cópia local: o campo pode ser trocado quando a quantidade muda
        for (i in f.u.indices) {
            val s = focusScale(f.u[i], f.v[i])
            if (s <= 0.01f) continue
            val x = width * f.u[i]
            val y = height * f.v[i]
            canvas.drawCircle(x, y, (r + d) * s, border)
            canvas.drawCircle(x, y, r * s, paint)
        }
    }

    private fun drawEdges(canvas: Canvas, r: Float, d: Float) {
        val reach = width * 0.12f
        val margin = baseR * 2.5f // a margem não acompanha o crescimento dos pontos
        val grid = settings.pattern == PATTERN_GRID
        paint.strokeWidth = 2f * d
        border.strokeWidth = 4f * d
        val ys = drift.y // cópia local: o drift pode ser trocado quando a quantidade muda
        for (i in ys.indices) {
            // metade dos pontos sempre; o resto acende (um a um, suave) conforme o movimento fica brusco
            val lit = (ys.size * (0.5f + 0.5f * intensity) - i).coerceIn(0f, 1f)
            if (lit <= 0f) continue
            paint.alpha = (baseAlpha * lit).toInt()
            border.alpha = paint.alpha
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

    private companion object {
        const val MAX_TILT_DEG = 25f
        const val YAW_GAIN = 0.5f   // telas/s por rad/s
        const val PITCH_GAIN = 0.5f
        fun newField(perSide: Int) = DotField(cols = perSide, rows = perSide * 2) // telas são cerca de 2x mais altas que largas
    }
}
