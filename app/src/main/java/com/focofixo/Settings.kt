package com.focofixo

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class AnchorSettings(val sensitivity: Float = 1f, val dotSizeDp: Float = 14f, val opacity: Float = 0.6f)

private val Context.store by preferencesDataStore("focofixo")
private val SENS = floatPreferencesKey("sensitivity")
private val SIZE = floatPreferencesKey("dot_size")
private val ALPHA = floatPreferencesKey("opacity")

class SettingsRepo(private val context: Context) {
    val flow: Flow<AnchorSettings> = context.store.data.map { p ->
        val d = AnchorSettings()
        AnchorSettings(
            (p[SENS] ?: d.sensitivity).coerceIn(0f, 3f),
            (p[SIZE] ?: d.dotSizeDp).coerceIn(6f, 32f),
            (p[ALPHA] ?: d.opacity).coerceIn(0.1f, 1f),
        )
    }

    suspend fun save(s: AnchorSettings) {
        context.store.edit { it[SENS] = s.sensitivity; it[SIZE] = s.dotSizeDp; it[ALPHA] = s.opacity }
    }
}
