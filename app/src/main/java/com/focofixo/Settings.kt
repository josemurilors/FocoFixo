package com.focofixo

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val DOT_COLORS = listOf(
    "Preto" to 0xFF000000.toInt(), "Branco" to 0xFFFFFFFF.toInt(), "Vermelho" to 0xFFE53935.toInt(),
    "Amarelo" to 0xFFFDD835.toInt(), "Azul" to 0xFF1E88E5.toInt(), "Verde" to 0xFF43A047.toInt(),
)
val SAMPLE_RATES = listOf(30, 60, 120)
const val PATTERN_GRID = 1
const val PATTERN_HORIZON = 2
const val PATTERN_FIELD = 3
val PATTERNS = listOf("Pontos", "Grade", "Horizonte", "Campo") // índice = valor de AnchorSettings.pattern
// "desligado" é o serviço parado; o modo salvo diz como religar (tile, notificação)
const val MODE_ON = 1
const val MODE_AUTO = 2

data class AnchorSettings(
    val sensitivity: Float = 1f,
    val dotSizeDp: Float = 14f,
    val opacity: Float = 0.6f,
    val threshold: Float = 0.3f, // m/s² ignorados (anti-tremor)
    val damping: Float = 0.175f, // 0..1 -> constante de suavização
    val colorIndex: Int = 0,
    val sampleHz: Int = 60,
    val fadeIdle: Boolean = true, // pontos somem com pouco movimento
    val speed: Float = 1f,        // multiplicador da velocidade do deslize
    val dotCount: Int = 6,        // pontos por borda
    val pattern: Int = 0,         // índice em PATTERNS
    val invertLateral: Boolean = false, // pontos vão para o lado da curva, em vez do oposto
    val sizeBoost: Boolean = true,      // pontos crescem ao acelerar e frear
    val mode: Int = MODE_ON,
    val autoSpeedKmh: Float = 20f,      // no modo Auto, a âncora aparece a partir desta velocidade
)

private val Context.store by preferencesDataStore("focofixo")
private val SENS = floatPreferencesKey("sensitivity")
private val SIZE = floatPreferencesKey("dot_size")
private val ALPHA = floatPreferencesKey("opacity")
private val THRESHOLD = floatPreferencesKey("threshold")
private val DAMPING = floatPreferencesKey("damping")
private val COLOR = intPreferencesKey("color")
private val HZ = intPreferencesKey("sample_hz")
private val FADE = booleanPreferencesKey("fade_idle")
private val SPEED = floatPreferencesKey("speed")
private val COUNT = intPreferencesKey("dot_count")
private val PATTERN = intPreferencesKey("pattern")
private val INVERT = booleanPreferencesKey("invert_lateral")
private val BOOST = booleanPreferencesKey("size_boost")
private val MODE = intPreferencesKey("mode")
private val AUTO_SPEED = floatPreferencesKey("auto_speed_kmh")

class SettingsRepo(private val context: Context) {
    val flow: Flow<AnchorSettings> = context.store.data.map { p ->
        val d = AnchorSettings()
        AnchorSettings(
            (p[SENS] ?: d.sensitivity).coerceIn(0f, 3f),
            (p[SIZE] ?: d.dotSizeDp).coerceIn(6f, 32f),
            (p[ALPHA] ?: d.opacity).coerceIn(0.1f, 1f),
            (p[THRESHOLD] ?: d.threshold).coerceIn(0f, 2f),
            (p[DAMPING] ?: d.damping).coerceIn(0f, 1f),
            (p[COLOR] ?: d.colorIndex).coerceIn(0, DOT_COLORS.lastIndex),
            (p[HZ] ?: d.sampleHz).takeIf { it in SAMPLE_RATES } ?: d.sampleHz,
            p[FADE] ?: d.fadeIdle,
            (p[SPEED] ?: d.speed).coerceIn(0.2f, 2.5f),
            (p[COUNT] ?: d.dotCount).coerceIn(3, 12),
            (p[PATTERN] ?: d.pattern).coerceIn(0, PATTERNS.lastIndex),
            p[INVERT] ?: d.invertLateral,
            p[BOOST] ?: d.sizeBoost,
            (p[MODE] ?: d.mode).takeIf { it == MODE_ON || it == MODE_AUTO } ?: d.mode,
            (p[AUTO_SPEED] ?: d.autoSpeedKmh).coerceIn(10f, 60f),
        )
    }

    suspend fun save(s: AnchorSettings) {
        context.store.edit {
            it[SENS] = s.sensitivity; it[SIZE] = s.dotSizeDp; it[ALPHA] = s.opacity
            it[THRESHOLD] = s.threshold; it[DAMPING] = s.damping; it[COLOR] = s.colorIndex; it[HZ] = s.sampleHz; it[FADE] = s.fadeIdle
            it[SPEED] = s.speed; it[COUNT] = s.dotCount; it[PATTERN] = s.pattern; it[INVERT] = s.invertLateral; it[BOOST] = s.sizeBoost
            it[MODE] = s.mode; it[AUTO_SPEED] = s.autoSpeedKmh
        }
    }
}
