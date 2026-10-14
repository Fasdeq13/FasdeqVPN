package app.fasdeq.vpn.ui

import android.Manifest
import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Icon as GraphicIcon
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import app.fasdeq.vpn.AppState
import app.fasdeq.vpn.DnsPresets
import app.fasdeq.vpn.FasdeqTileService
import app.fasdeq.vpn.PingOptions
import app.fasdeq.vpn.R
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

private const val AUTHOR_URL = "https://github.com/fasdeq13/"
private const val REPO_URL = "https://github.com/Fasdeq13/FasdeqVPN"
private const val SETTINGS_TAU = (2.0 * PI).toFloat()

@Composable
internal fun SettingsGroup(title: String, content: @Composable () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            color = scheme.primary,
            modifier = Modifier.padding(start = 12.dp)
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = scheme.surfaceContainerHigh
        ) {
            Column(Modifier.padding(vertical = 6.dp)) { content() }
        }
    }
}

@Composable
internal fun SettingItem(
    icon: Int,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null,
    below: Boolean = false,
    trailing: (@Composable () -> Unit)? = null
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .requiredSize(44.dp)
                .clip(CircleShape)
                .background(scheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painterResource(icon),
                null,
                Modifier.requiredSize(22.dp),
                tint = scheme.onSecondaryContainer
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant
            )
            if (below && trailing != null) {
                Spacer(Modifier.height(10.dp))
                trailing()
            }
        }
        if (!below && trailing != null) {
            Spacer(Modifier.width(10.dp))
            trailing()
        }
    }
}

@Composable
internal fun StateSwitch(checked: Boolean, onChange: (Boolean) -> Unit) {
    Switch(
        checked = checked,
        onCheckedChange = onChange,
        thumbContent = if (checked) {
            { Icon(painterResource(R.drawable.ic_check), null, Modifier.size(SwitchDefaults.IconSize)) }
        } else null
    )
}

@Composable
private fun SegmentedChoice(
    options: List<String>,
    selected: Int,
    enabled: Boolean = true,
    icons: List<Int>? = null,
    onSelect: (Int) -> Unit
) {
    SingleChoiceSegmentedButtonRow(
        Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.38f)
    ) {
        options.forEachIndexed { index, label ->
            SegmentedButton(
                selected = selected == index,
                onClick = { if (enabled) onSelect(index) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
                icon = {
                    if (icons != null) {
                        Icon(
                            painterResource(icons[index]),
                            null,
                            Modifier.size(SegmentedButtonDefaults.IconSize)
                        )
                    }
                }
            ) {
                Text(
                    label,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

private fun requestTile(context: Context) {
    if (Build.VERSION.SDK_INT < 33) return
    try {
        val manager = context.getSystemService(StatusBarManager::class.java)
        manager.requestAddTileService(
            ComponentName(context, FasdeqTileService::class.java),
            "FasdeqVPN",
            GraphicIcon.createWithResource(context, R.drawable.ic_power),
            context.mainExecutor
        ) { }
    } catch (e: Throwable) {
    }
}

private fun openUrl(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: Exception) {
    }
}

@Composable
private fun CookieLogo() {
    val scheme = MaterialTheme.colorScheme
    val transition = rememberInfiniteTransition(label = "logo")
    val spin by transition.animateFloat(
        0f,
        SETTINGS_TAU,
        infiniteRepeatable(tween(Motion.d(9000), easing = LinearEasing), RepeatMode.Restart),
        label = "spin"
    )
    val pulse by transition.animateFloat(
        0.04f,
        0.10f,
        infiniteRepeatable(tween(Motion.d(1800)), RepeatMode.Reverse),
        label = "pulse"
    )
    val float by transition.animateFloat(
        -4f,
        4f,
        infiniteRepeatable(tween(Motion.d(2200)), RepeatMode.Reverse),
        label = "float"
    )
    val primary = scheme.primary
    val tertiary = scheme.tertiary
    Box(
        Modifier
            .size(96.dp)
            .graphicsLayer { translationY = float.dp.toPx() },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val base = size.minDimension * 0.4f
            fun blob(lobes: Float, amp: Float, offset: Float): Path {
                val path = Path()
                val steps = 120
                for (i in 0..steps) {
                    val th = i * SETTINGS_TAU / steps
                    val rr = base * (1f + amp * cos(lobes * th + offset))
                    val x = cx + rr * cos(th)
                    val y = cy + rr * sin(th)
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close()
                return path
            }
            drawPath(blob(6f, pulse, -spin * 1.4f), tertiary.copy(alpha = 0.45f))
            drawPath(blob(8f, pulse, spin), primary)
        }
        Icon(
            painterResource(R.drawable.ic_power),
            null,
            Modifier.size(40.dp),
            tint = scheme.onPrimary
        )
    }
}

@Composable
private fun LinkRow(icon: Int, title: String, subtitle: String, url: String, delayMs: Long) {
    val scheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(delayMs)
        appear.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = Motion.k(180f)))
    }
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val press by animateFloatAsState(
        if (pressed) 0.95f else 1f,
        spring(dampingRatio = 0.4f, stiffness = Motion.k(420f)),
        label = "linkPress"
    )
    val transition = rememberInfiniteTransition(label = "arrow")
    val nudge by transition.animateFloat(
        0f,
        4f,
        infiniteRepeatable(tween(Motion.d(800)), RepeatMode.Reverse),
        label = "nudge"
    )
    Surface(
        onClick = { openUrl(context, url) },
        interactionSource = interaction,
        shape = RoundedCornerShape(22.dp),
        color = scheme.secondaryContainer,
        contentColor = scheme.onSecondaryContainer,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = appear.value
                translationY = (1f - appear.value) * 36.dp.toPx()
                val s = (0.9f + 0.1f * appear.value) * press
                scaleX = s
                scaleY = s
            }
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .requiredSize(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(scheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(painterResource(icon), null, Modifier.size(22.dp), tint = scheme.onPrimary)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSecondaryContainer.copy(alpha = 0.75f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                painterResource(R.drawable.ic_open),
                null,
                Modifier
                    .size(22.dp)
                    .graphicsLayer {
                        translationX = nudge.dp.toPx()
                        translationY = -nudge.dp.toPx()
                    }
            )
        }
    }
}

@Composable
private fun AboutCard() {
    val scheme = MaterialTheme.colorScheme
    val transition = rememberInfiniteTransition(label = "about")
    val angle by transition.animateFloat(
        0f,
        360f,
        infiniteRepeatable(tween(Motion.d(6000), easing = LinearEasing), RepeatMode.Restart),
        label = "angle"
    )
    val shimmer by transition.animateFloat(
        -300f,
        700f,
        infiniteRepeatable(tween(Motion.d(2600), easing = LinearEasing), RepeatMode.Restart),
        label = "shimmer"
    )
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) { appear.animateTo(1f, spring(dampingRatio = 0.65f, stiffness = Motion.k(120f))) }
    val c1 = scheme.primary
    val c2 = scheme.tertiary
    val c3 = scheme.secondary
    Box(
        Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = appear.value
                val s = 0.85f + 0.15f * appear.value
                scaleX = s
                scaleY = s
            }
            .clip(RoundedCornerShape(34.dp))
            .drawBehind {
                val center = Offset(size.width / 2f, size.height / 2f)
                rotate(angle, center) {
                    drawCircle(
                        brush = Brush.sweepGradient(listOf(c1, c2, c3, c1), center),
                        radius = max(size.width, size.height),
                        center = center
                    )
                }
            }
            .padding(2.5.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = scheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                Modifier.padding(horizontal = 18.dp, vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CookieLogo()
                Text(
                    "FasdeqVPN",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        brush = Brush.linearGradient(
                            listOf(c1, c2, c3, c1),
                            start = Offset(shimmer, 0f),
                            end = Offset(shimmer + 320f, 80f)
                        )
                    )
                )
                Text(
                    "Версия 1.4",
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                LinkRow(
                    R.drawable.ic_person,
                    "Автор",
                    "github.com/fasdeq13",
                    AUTHOR_URL,
                    350
                )
                Spacer(Modifier.height(2.dp))
                LinkRow(
                    R.drawable.ic_code,
                    "Репозиторий",
                    "Fasdeq13/FasdeqVPN",
                    REPO_URL,
                    550
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(state: AppState, onDismiss: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    val dynamicSupported = Build.VERSION.SDK_INT >= 31
    val dynamicActive = dynamicSupported && state.dynamicColors
    val modes = listOf("Системная", "Светлая", "Тёмная")
    val power = remember { context.getSystemService(PowerManager::class.java) }
    var showCores by remember { mutableStateOf(false) }
    var showReset by remember { mutableStateOf(false) }

    fun notificationsGranted(): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    fun batteryFree(): Boolean = power.isIgnoringBatteryOptimizations(context.packageName)

    var notifications by remember { mutableStateOf(notificationsGranted()) }
    var battery by remember { mutableStateOf(batteryFree()) }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { notifications = notificationsGranted() }

    val batteryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { battery = batteryFree() }

    val mtuValues = listOf(1280, 1400, 1500)
    val activeCoreInfo = state.cores.firstOrNull { it.id == state.activeCore }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            SheetHeader(R.drawable.ic_settings, "Настройки", "Тема, анимации, подключение и ядро")

            SettingsGroup("Оформление") {
                Column(
                    Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Тема", style = MaterialTheme.typography.titleMedium)
                    SegmentedChoice(
                        modes,
                        state.themeMode,
                        icons = listOf(R.drawable.ic_theme_auto, R.drawable.ic_sun, R.drawable.ic_moon)
                    ) { state.setTheme(it) }
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        scheme.primary,
                        scheme.primaryContainer,
                        scheme.secondary,
                        scheme.secondaryContainer,
                        scheme.tertiary,
                        scheme.tertiaryContainer
                    ).forEach { swatch ->
                        val tone by animateColorAsState(swatch, tween(Motion.d(500)), label = "tone")
                        Box(
                            Modifier
                                .weight(1f)
                                .height(36.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(tone)
                        )
                    }
                }
                if (dynamicSupported) {
                    SettingItem(
                        icon = R.drawable.ic_settings,
                        title = "Цвета системы",
                        subtitle = "Material You: палитра из обоев",
                        onClick = { state.setDynamic(!state.dynamicColors) }
                    ) { StateSwitch(state.dynamicColors) { state.setDynamic(it) } }
                }
                Column(
                    Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Стиль палитры", style = MaterialTheme.typography.titleMedium)
                    SegmentedChoice(Accents.styles, state.palette, enabled = !dynamicActive) {
                        state.changePalette(it)
                    }
                    Text("Фон главного экрана", style = MaterialTheme.typography.titleMedium)
                    SegmentedChoice(listOf("Живой", "Как раньше", "Простой"), state.bgStyle) {
                        state.changeBgStyle(it)
                    }
                    Text("Контраст", style = MaterialTheme.typography.titleMedium)
                    SegmentedChoice(listOf("Стандарт", "Средний", "Высокий"), state.contrast) {
                        state.changeContrast(it)
                    }
                    Text("Акцентный цвет", style = MaterialTheme.typography.titleMedium)
                    if (dynamicActive) {
                        Text(
                            "Недоступно, пока включены цвета системы",
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.onSurfaceVariant
                        )
                    }
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.alpha(if (dynamicActive) 0.38f else 1f)
                    ) {
                        items(Accents.names.indices.toList()) { index ->
                            val chosen = state.accent == index
                            val size by animateFloatAsState(
                                if (chosen) 56f else 48f,
                                spring(dampingRatio = 0.5f, stiffness = Motion.k(400f)),
                                label = "swatch"
                            )
                            Box(
                                Modifier
                                    .size(56.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    Modifier
                                        .size(size.dp)
                                        .clip(if (chosen) RoundedCornerShape(18.dp) else CircleShape)
                                        .background(Accents.swatch(index, state.palette))
                                        .clickable(enabled = !dynamicActive) { state.applyAccent(index) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (chosen) {
                                        Icon(
                                            painterResource(R.drawable.ic_check),
                                            contentDescription = Accents.names[index],
                                            tint = Color.White,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            SettingsGroup("Анимации") {
                Column(
                    Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Анимация запуска VPN", style = MaterialTheme.typography.titleMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(LaunchStyles.names.indices.toList()) { index ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                LaunchPreview(index, state.launchStyle == index) { state.changeLaunchStyle(index) }
                                Text(
                                    LaunchStyles.names[index],
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (state.launchStyle == index) scheme.primary else scheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    Text("Размер кнопки запуска", style = MaterialTheme.typography.titleMedium)
                    Slider(
                        value = state.powerScale,
                        onValueChange = { state.changePowerScale(it) },
                        valueRange = 0.6f..1.4f
                    )
                    Text("Размер текста", style = MaterialTheme.typography.titleMedium)
                    Slider(
                        value = state.textScale,
                        onValueChange = { state.changeTextScale(it) },
                        valueRange = 0.8f..1.5f
                    )
                    Text("Скорость анимаций", style = MaterialTheme.typography.titleMedium)
                    SegmentedChoice(Motion.names, state.animSpeed) { state.changeAnimSpeed(it) }
                    Text(
                        "«Минимум» почти убирает движение, «Плавно» замедляет всё в полтора раза",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant
                    )
                }
            }

            SettingsGroup("Экран") {
                SettingItem(
                    icon = R.drawable.ic_screen,
                    title = "Чёрный фон",
                    subtitle = "Чистый чёрный в тёмной теме. Экономит заряд на OLED",
                    onClick = { state.changeAmoled(!state.amoled) }
                ) { StateSwitch(state.amoled) { state.changeAmoled(it) } }
                SettingItem(
                    icon = R.drawable.ic_screen,
                    title = "Защита от выгорания",
                    subtitle = "Интерфейс медленно смещается на несколько пикселей. Для OLED-экранов",
                    onClick = { state.changeBurnIn(!state.burnIn) }
                ) { StateSwitch(state.burnIn) { state.changeBurnIn(it) } }
            }

            SettingsGroup("Подключение") {
                SettingItem(
                    icon = R.drawable.ic_power,
                    title = "Автоподключение",
                    subtitle = "Подключаться при запуске приложения",
                    onClick = { state.changeAutoConnect(!state.autoConnect) }
                ) { StateSwitch(state.autoConnect) { state.changeAutoConnect(it) } }
                Column(
                    Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("DNS", style = MaterialTheme.typography.titleMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(DnsPresets.names.indices.toList()) { index ->
                            FilterChip(
                                selected = state.dnsPreset == index,
                                onClick = { state.changeDnsPreset(index) },
                                label = { Text(DnsPresets.names[index]) }
                            )
                        }
                    }
                    if (state.dnsPreset !in DnsPresets.servers.indices) {
                        OutlinedTextField(
                            value = state.dnsCustom,
                            onValueChange = { state.changeDnsCustom(it) },
                            label = { Text("Адреса через запятую") },
                            placeholder = { Text("1.1.1.1, 8.8.8.8") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text(
                            DnsPresets.servers[state.dnsPreset].joinToString(", "),
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.onSurfaceVariant
                        )
                    }
                    Text("MTU", style = MaterialTheme.typography.titleMedium)
                    SegmentedChoice(
                        mtuValues.map { it.toString() },
                        mtuValues.indexOf(state.mtu).coerceAtLeast(0)
                    ) { state.changeMtu(mtuValues[it]) }
                    Text(
                        "Изменения применяются при следующем подключении",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant
                    )
                }
                SettingItem(
                    icon = R.drawable.ic_memory,
                    title = "Ядро Xray",
                    subtitle = (activeCoreInfo?.let { it.label + (if (it.version.isNotBlank()) " · " + it.version else "") }
                        ?: ("Встроенное" + if (state.builtinVersion.isNotBlank()) " · " + state.builtinVersion else "")),
                    onClick = { showCores = true }
                )
            }

            SettingsGroup("Проверка пинга") {
                Column(
                    Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Метод", style = MaterialTheme.typography.titleMedium)
                    SegmentedChoice(PingOptions.modes, state.pingMode) { state.changePingMode(it) }
                    Text(
                        when (state.pingMode) {
                            1 -> "Время установки TCP-соединения с сервером"
                            2 -> "UDP-проба. Если сервер молчит, вместо неё проверяется ICMP"
                            3 -> "Проверяются оба протокола, показываются оба значения"
                            4 -> "Обычный ping (ICMP). Некоторые серверы его блокируют"
                            else -> "WireGuard проверяется через ICMP (запасной вариант UDP), остальные по TCP"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant
                    )
                    Text("Таймаут", style = MaterialTheme.typography.titleMedium)
                    SegmentedChoice(PingOptions.timeoutNames, state.pingTimeout) { state.changePingTimeout(it) }
                }
                SettingItem(
                    icon = R.drawable.ic_bolt,
                    title = "Пинг при запуске",
                    subtitle = "Проверять все серверы при открытии приложения",
                    onClick = { state.changeAutoPing(!state.autoPing) }
                ) { StateSwitch(state.autoPing) { state.changeAutoPing(it) } }
                Column(
                    Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Проверка сервисов при включённом VPN", style = MaterialTheme.typography.titleMedium)
                    var intervalText by remember(state.resetTick) {
                        mutableStateOf(state.checkInterval.toString().removeSuffix(".0"))
                    }
                    OutlinedTextField(
                        value = intervalText,
                        onValueChange = {
                            intervalText = it
                            val v = it.replace(',', '.').trim().toFloatOrNull()
                            if (v != null && v >= 0.1f) state.changeCheckInterval(v)
                        },
                        label = { Text("Интервал, минут") },
                        placeholder = { Text("60") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "Можно дробные значения: 0.1, 0.5, 2. Минимум 0.1 минуты",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant
                    )
                }
                SettingItem(
                    icon = R.drawable.ic_bolt,
                    title = "Сортировка по пингу",
                    subtitle = "Быстрые серверы выше внутри каждой группы",
                    onClick = { state.changeSortPing(!state.sortPing) }
                ) { StateSwitch(state.sortPing) { state.changeSortPing(it) } }
            }

            SettingsGroup("Работа в фоне") {
                SettingItem(
                    icon = R.drawable.ic_tile,
                    title = "Плитка в шторке",
                    below = true,
                    subtitle = if (Build.VERSION.SDK_INT >= 33) {
                        "Быстрое включение и выключение из верхней шторки"
                    } else {
                        "Опустите шторку, нажмите карандаш и перетащите плитку FasdeqVPN"
                    }
                ) {
                    if (Build.VERSION.SDK_INT >= 33) {
                        ActionButton(R.drawable.ic_add, "Добавить", { requestTile(context) })
                    }
                }
                SettingItem(
                    icon = R.drawable.ic_bell,
                    title = "Уведомление",
                    below = true,
                    subtitle = if (notifications) {
                        "Показывает состояние и кнопку отключения"
                    } else {
                        "Нужно разрешение, чтобы видеть состояние VPN"
                    }
                ) {
                    if (notifications) {
                        Icon(
                            painterResource(R.drawable.ic_check),
                            null,
                            Modifier.size(24.dp),
                            tint = scheme.primary
                        )
                    } else {
                        ActionButton(R.drawable.ic_bell, "Разрешить", {
                            if (Build.VERSION.SDK_INT >= 33) {
                                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        })
                    }
                }
                SettingItem(
                    icon = R.drawable.ic_battery,
                    title = "Без ограничений батареи",
                    below = true,
                    subtitle = if (battery) {
                        "Система не будет усыплять соединение"
                    } else {
                        "Рекомендуется, чтобы VPN не выгружался в фоне"
                    }
                ) {
                    if (battery) {
                        Icon(
                            painterResource(R.drawable.ic_check),
                            null,
                            Modifier.size(24.dp),
                            tint = scheme.primary
                        )
                    } else {
                        ActionButton(R.drawable.ic_battery, "Выключить", {
                            try {
                                batteryLauncher.launch(
                                    Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                                        .setData(Uri.parse("package:" + context.packageName))
                                )
                            } catch (e: Exception) {
                                try {
                                    batteryLauncher.launch(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                                } catch (e2: Exception) {
                                }
                            }
                        })
                    }
                }
            }

            SettingsGroup("Сброс") {
                SettingItem(
                    icon = R.drawable.ic_refresh,
                    title = "Настройки по умолчанию",
                    subtitle = "Вернуть внешний вид, пинг, DNS и размеры к исходным. Серверы не затрагиваются",
                    onClick = { showReset = true }
                )
            }

            AboutCard()
        }
    }

    if (showReset) {
        AlertDialog(
            onDismissRequest = { showReset = false },
            title = { Text("Сбросить настройки?") },
            text = { Text("Все настройки вернутся к значениям по умолчанию. Серверы и группы останутся.") },
            confirmButton = {
                TextButton(onClick = {
                    state.resetSettings()
                    showReset = false
                }) { Text("Сбросить") }
            },
            dismissButton = {
                TextButton(onClick = { showReset = false }) { Text("Отмена") }
            }
        )
    }

    if (showCores) CoresSheet(state) { showCores = false }
}
