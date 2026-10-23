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

class MainActivity : ComponentActivity() {
    // shortcut: estado do switch não reflete o serviço se ele for desligado pela notificação; sincronizar se incomodar
    private var active by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 0)
        val repo = SettingsRepo(this)
        setContent {
            MaterialTheme {
                val s by repo.flow.collectAsState(AnchorSettings())
                val scope = rememberCoroutineScope()
                fun save(n: AnchorSettings) = scope.launch { repo.save(n) }
                Column(Modifier.padding(24.dp)) {
                    Text("FocoFixo", style = MaterialTheme.typography.headlineMedium)
                    Text("Pontos de referência na tela para aliviar o enjoo de movimento.")
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Ativar âncora", Modifier.weight(1f))
                        Switch(active, onCheckedChange = { toggle(it) })
                    }
                    Text("Sensibilidade")
                    Slider(s.sensitivity, { save(s.copy(sensitivity = it)) }, valueRange = 0f..3f)
                    Text("Tamanho dos pontos")
                    Slider(s.dotSizeDp, { save(s.copy(dotSizeDp = it)) }, valueRange = 6f..32f)
                    Text("Opacidade")
                    Slider(s.opacity, { save(s.copy(opacity = it)) }, valueRange = 0.1f..1f)
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
