package com.focofixo

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    private var active by mutableStateOf(false)

    // o serviço também liga/desliga pela notificação e pelo tile
    override fun onResume() {
        super.onResume()
        active = AnchorService.running
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 0)
        val repo = SettingsRepo(this)
        setContent {
            MaterialTheme {
                val s by repo.flow.collectAsState(AnchorSettings())
                val scope = rememberCoroutineScope()
                fun save(n: AnchorSettings) = scope.launch { repo.save(n) }
                Column(Modifier.verticalScroll(rememberScrollState()).padding(24.dp)) {
                    Text("FocoFixo", style = MaterialTheme.typography.headlineMedium)
                    Text("Pontos de referência na tela para aliviar o enjoo de movimento.")
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Ativar âncora", Modifier.weight(1f))
                        Switch(active, onCheckedChange = { toggle(it) })
                    }
                    Text("Padrão visual")
                    Row {
                        PATTERNS.forEachIndexed { i, name ->
                            FilterChip(s.pattern == i, { save(s.copy(pattern = i)) }, { Text(name) }, Modifier.padding(end = 8.dp))
                        }
                    }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Pontos vão para o lado da curva", Modifier.weight(1f))
                        Switch(s.invertLateral, { save(s.copy(invertLateral = it)) })
                    }
                    Text("Sensibilidade")
                    Slider(s.sensitivity, { save(s.copy(sensitivity = it)) }, valueRange = 0f..3f)
                    Text("Tamanho dos pontos")
                    Slider(s.dotSizeDp, { save(s.copy(dotSizeDp = it)) }, valueRange = 6f..32f)
                    Text("Velocidade do movimento")
                    Slider(s.speed, { save(s.copy(speed = it)) }, valueRange = 0.2f..2.5f)
                    Text("Quantidade de pontos por lado: ${s.dotCount}")
                    Slider(s.dotCount.toFloat(), { save(s.copy(dotCount = it.roundToInt())) }, valueRange = 3f..12f, steps = 8)
                    Text("Opacidade")
                    Slider(s.opacity, { save(s.copy(opacity = it)) }, valueRange = 0.1f..1f)
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Sumir com pouco movimento", Modifier.weight(1f))
                        Switch(s.fadeIdle, { save(s.copy(fadeIdle = it)) })
                    }
                    Text("Limiar de movimento (ignora tremor)")
                    Slider(s.threshold, { save(s.copy(threshold = it)) }, valueRange = 0f..2f)
                    Text("Suavização")
                    Slider(s.damping, { save(s.copy(damping = it)) }, valueRange = 0f..1f)
                    Text("Cor")
                    Row(Modifier.horizontalScroll(rememberScrollState())) {
                        DOT_COLORS.forEachIndexed { i, (name, _) ->
                            FilterChip(s.colorIndex == i, { save(s.copy(colorIndex = i)) }, { Text(name) }, Modifier.padding(end = 8.dp))
                        }
                    }
                    Text("Taxa do sensor (menos Hz economiza bateria)")
                    Row {
                        SAMPLE_RATES.forEach { hz ->
                            FilterChip(s.sampleHz == hz, { save(s.copy(sampleHz = hz)) }, { Text("$hz Hz") }, Modifier.padding(end = 8.dp))
                        }
                    }
                }
            }
        }
    }

    private fun toggle(on: Boolean) {
        if (!on) { AnchorService.stop(this); active = false; return }
        if (!Settings.canDrawOverlays(this)) {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            return
        }
        AnchorService.start(this)
        active = true
    }
}
