package com.focofixo

import android.Manifest
import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    private var active by mutableStateOf(false)
    // incrementado a cada retorno, para reavaliar a permissão de sobreposição vinda das Configurações
    private var resumeTick by mutableIntStateOf(0)
    private var pendingAuto = false // Auto escolhido, esperando a resposta do pedido de localização
    private lateinit var repo: SettingsRepo

    // o serviço também liga/desliga pela notificação e pelo tile
    override fun onResume() {
        super.onResume()
        if (pendingAuto) {
            pendingAuto = false
            if (AnchorService.hasLocationPermission(this)) restart()
        }
        active = AnchorService.running
        resumeTick++
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 0)
        repo = SettingsRepo(this)
        setContent {
            FocoTheme {
                val s by repo.flow.collectAsState(AnchorSettings())
                val scope = rememberCoroutineScope()
                var tab by rememberSaveable { mutableIntStateOf(0) }
                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(tab == 0, { tab = 0 }, { Icon(Icons.Filled.Home, null) }, label = { Text("Início") })
                            NavigationBarItem(tab == 1, { tab = 1 }, { Icon(Icons.Filled.Settings, null) }, label = { Text("Configurações") })
                        }
                    }
                ) { padding ->
                    Column(
                        Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (tab == 0) HomeScreen(active, s, resumeTick, { select(it, s) }, { requestLocation() })
                        else SettingsScreen(s) { n -> scope.launch { repo.save(n) } }
                    }
                }
            }
        }
    }

    /** mode: 0 = desligado, ou MODE_ON / MODE_AUTO. */
    private fun select(mode: Int, s: AnchorSettings) {
        if (mode == 0) { AnchorService.stop(this); active = false; return }
        if (!Settings.canDrawOverlays(this)) { openOverlaySettings(this); return }
        lifecycleScope.launch {
            repo.save(s.copy(mode = mode)) // o serviço lê o modo ao iniciar
            if (mode == MODE_AUTO && !AnchorService.hasLocationPermission(this@MainActivity)) requestLocation()
            else restart()
        }
    }

    private fun requestLocation() {
        pendingAuto = true
        requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 1)
    }

    // o tipo do serviço em primeiro plano (com ou sem localização) só é definido ao iniciar
    private fun restart() {
        AnchorService.stop(this)
        AnchorService.start(this)
        active = true
    }
}

private fun openOverlaySettings(ctx: Context) =
    ctx.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${ctx.packageName}")))

@Composable
private fun FocoTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val dark = isSystemInDarkTheme()
    // cores do papel de parede existem a partir do Android 12; antes, a paleta do ícone (azul-petróleo/âmbar)
    val colors = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> darkColorScheme(
            primary = Color(0xFFFFC247), onPrimary = Color(0xFF3F2E00),
            primaryContainer = Color(0xFF0B3A5B), onPrimaryContainer = Color(0xFFCDE5FF),
        )
        else -> lightColorScheme(
            primary = Color(0xFF0B3A5B), onPrimary = Color.White,
            primaryContainer = Color(0xFFCDE5FF), onPrimaryContainer = Color(0xFF001D33),
        )
    }
    MaterialTheme(colorScheme = colors, content = content)
}

@Composable
private fun Header(title: String) {
    Text(title, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 16.dp, bottom = 4.dp))
}

@Composable
private fun HomeScreen(
    active: Boolean, s: AnchorSettings, resumeTick: Int, onSelect: (Int) -> Unit, onRequestLocation: () -> Unit,
) {
    val context = LocalContext.current
    Header("FocoFixo")

    val auto = s.mode == MODE_AUTO
    val colors = if (active) CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    else CardDefaults.cardColors()
    Card(colors = colors, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(R.drawable.ic_tile), null, Modifier.size(48.dp))
            Column(Modifier.weight(1f).padding(start = 16.dp)) {
                Text(
                    when { !active -> "Âncora desligada"; auto -> "Modo Auto"; else -> "Âncora ativa" },
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    when {
                        !active -> "Ative para mostrar pontos de referência que aliviam o enjoo de movimento."
                        auto -> "Os pontos aparecem sozinhos acima de ${s.autoSpeedKmh.roundToInt()} km/h."
                        else -> "Os pontos acompanham o movimento do veículo sobre qualquer app."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
    Choices(listOf("Desligado", "Ligado", "Auto"), if (active) s.mode else 0, onSelect)

    val overlayOk = remember(resumeTick) { Settings.canDrawOverlays(context) }
    if (!overlayOk) {
        WarningCard(
            "Permissão de sobreposição",
            "O FocoFixo precisa aparecer sobre outros apps para desenhar os pontos.",
            "Permitir",
        ) { openOverlaySettings(context) }
    }
    if (auto) {
        val locationOk = remember(resumeTick) { AnchorService.hasLocationPermission(context) }
        if (!locationOk) {
            WarningCard(
                "Permissão de localização",
                "O modo Auto usa a velocidade do GPS para ligar e desligar a âncora sozinho.",
                "Permitir",
                onRequestLocation,
            )
        }
        val batteryOk = remember(resumeTick) {
            context.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(context.packageName)
        }
        if (!batteryOk) {
            WarningCard(
                "Otimização de bateria",
                "Sem a isenção, o Android pode encerrar o modo Auto em segundo plano.",
                "Permitir",
            ) {
                context.startActivity(
                    Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${context.packageName}")),
                )
            }
        }
    }
}

@Composable
private fun WarningCard(title: String, text: String, action: String, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp, bottom = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Warning, null)
                Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 12.dp))
            }
            Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp, end = 12.dp))
            TextButton(onClick = onClick, modifier = Modifier.align(Alignment.End)) { Text(action) }
        }
    }
}

@Composable
private fun SettingsScreen(s: AnchorSettings, save: (AnchorSettings) -> Unit) {
    Header("Configurações")

    Section("Aparência") {
        Column(Modifier.padding(16.dp)) {
            Text("Padrão visual", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            Choices(PATTERNS, s.pattern) { save(s.copy(pattern = it)) }
            Spacer(Modifier.height(16.dp))
            Text("Cor", style = MaterialTheme.typography.titleMedium)
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(top = 8.dp)) {
                DOT_COLORS.forEachIndexed { i, (name, argb) ->
                    FilterChip(
                        s.colorIndex == i, { save(s.copy(colorIndex = i)) }, { Text(name) },
                        Modifier.padding(end = 8.dp),
                        leadingIcon = {
                            Box(Modifier.size(16.dp).background(MaterialTheme.colorScheme.outline, CircleShape).padding(1.dp)
                                .background(Color(argb), CircleShape))
                        },
                    )
                }
            }
        }
        SettingSlider("Tamanho dos pontos", s.dotSizeDp, 6f..32f) { save(s.copy(dotSizeDp = it)) }
        SettingSlider("Pontos por lado: ${s.dotCount}", s.dotCount.toFloat(), 3f..12f, steps = 8) {
            save(s.copy(dotCount = it.roundToInt()))
        }
        SettingSlider("Opacidade", s.opacity, 0.1f..1f) { save(s.copy(opacity = it)) }
        SettingSwitch("Sumir com pouco movimento", "Os pontos aparecem só quando o veículo se move.", s.fadeIdle) {
            save(s.copy(fadeIdle = it))
        }
    }

    Section("Movimento") {
        SettingSlider("Sensibilidade", s.sensitivity, 0f..3f) { save(s.copy(sensitivity = it)) }
        SettingSlider("Velocidade do movimento", s.speed, 0.2f..2.5f) { save(s.copy(speed = it)) }
        SettingSwitch("Pontos vão para o lado da curva", "Desligado, eles vão para o lado oposto.", s.invertLateral) {
            save(s.copy(invertLateral = it))
        }
        SettingSwitch("Pontos crescem ao acelerar e frear", "Reforça a sensação de aceleração.", s.sizeBoost) {
            save(s.copy(sizeBoost = it))
        }
        SettingSlider(
            "Modo Auto liga a partir de ${s.autoSpeedKmh.roundToInt()} km/h", s.autoSpeedKmh, 10f..60f,
            "Desliga após 3 min devagar.", steps = 9,
        ) { save(s.copy(autoSpeedKmh = it)) }
    }

    Section("Ajuste fino") {
        SettingSlider("Limiar de movimento", s.threshold, 0f..2f, "Ignora tremores pequenos.") { save(s.copy(threshold = it)) }
        SettingSlider("Suavização", s.damping, 0f..1f) { save(s.copy(damping = it)) }
        Column(Modifier.padding(16.dp)) {
            Text("Taxa do sensor", style = MaterialTheme.typography.titleMedium)
            Text(
                "Menos Hz economiza bateria.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            Choices(SAMPLE_RATES.map { "$it Hz" }, SAMPLE_RATES.indexOf(s.sampleHz)) { save(s.copy(sampleHz = SAMPLE_RATES[it])) }
        }
    }

    Section("Sistema") { TileSetting() }
    Spacer(Modifier.height(8.dp))
}

/** O pedido de "adicionar" existe a partir do Android 13; antes, o usuário edita o painel rápido. */
@Composable
private fun TileSetting() {
    val context = LocalContext.current
    val canRequest = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    ListItem(
        headlineContent = { Text("Botão no painel rápido") },
        supportingContent = {
            Text(if (canRequest) "Liga e desliga a âncora sem abrir o app." else "Edite o painel rápido e arraste o botão \"FocoFixo\" para ele.")
        },
        trailingContent = { if (canRequest) OutlinedButton(onClick = { requestTile(context) }) { Text("Adicionar") } },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    )
}

@androidx.annotation.RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun requestTile(context: Context) {
    context.getSystemService(StatusBarManager::class.java).requestAddTileService(
        ComponentName(context, AnchorTileService::class.java),
        "FocoFixo",
        Icon.createWithResource(context, R.drawable.ic_tile),
        context.mainExecutor,
    ) {}
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Text(
        title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 8.dp),
    )
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth(),
    ) { Column(Modifier.padding(vertical = 4.dp)) { content() } }
}

@Composable
private fun SettingSwitch(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        trailingContent = { Switch(checked = checked, onCheckedChange = null) },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.toggleable(checked, role = Role.Switch, onValueChange = onChange),
    )
}

@Composable
private fun SettingSlider(
    title: String, value: Float, range: ClosedFloatingPointRange<Float>,
    subtitle: String? = null, steps: Int = 0, onChange: (Float) -> Unit,
) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        if (subtitle != null) {
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Slider(value, onChange, valueRange = range, steps = steps)
    }
}

@Composable
private fun Choices(labels: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        labels.forEachIndexed { i, label ->
            SegmentedButton(
                selected = i == selected,
                onClick = { onSelect(i) },
                shape = SegmentedButtonDefaults.itemShape(i, labels.size),
            ) { Text(label, maxLines = 1) }
        }
    }
}
