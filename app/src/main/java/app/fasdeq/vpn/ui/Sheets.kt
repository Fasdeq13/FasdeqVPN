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
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import app.fasdeq.vpn.AppState
import app.fasdeq.vpn.FasdeqTileService
import app.fasdeq.vpn.R
import app.fasdeq.vpn.displayName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddServerSheet(state: AppState, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    var text by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }

    fun submit(value: String) {
        if (loading) return
        scope.launch {
            loading = true
            error = null
            val result = state.importText(value)
            loading = false
            if (result == null) onDismiss() else error = result
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        scope.launch {
            loading = true
            error = null
            var failure: String? = null
            for (uri in uris) {
                val label = displayName(context, uri)
                val content = withContext(Dispatchers.IO) {
                    try {
                        context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
                    } catch (e: Exception) {
                        null
                    }
                }
                if (content == null) {
                    failure = "Не удалось прочитать файл"
                    continue
                }
                val result = state.importText(content, label)
                if (result != null) failure = result
            }
            loading = false
            if (failure != null) error = failure else if (uris.isNotEmpty()) onDismiss()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Добавить конфигурацию", style = MaterialTheme.typography.headlineSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilledTonalButton(
                    onClick = {
                        val clip = clipboard.getText()?.text ?: ""
                        if (clip.isBlank()) {
                            error = "Буфер обмена пуст"
                        } else {
                            text = clip
                            submit(clip)
                        }
                    },
                    enabled = !loading,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                ) {
                    Icon(painterResource(R.drawable.ic_paste), null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Из буфера")
                }
                FilledTonalButton(
                    onClick = { picker.launch(arrayOf("*/*")) },
                    enabled = !loading,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                ) {
                    Icon(painterResource(R.drawable.ic_file), null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Из файла")
                }
            }
            OutlinedTextField(
                value = text,
                onValueChange = {
                    text = it
                    error = null
                },
                label = { Text("Ссылка или конфигурация") },
                supportingText = { Text("Можно вставить сразу несколько ссылок или ссылку на список") },
                minLines = 4,
                maxLines = 8,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            )
            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
            Button(
                onClick = { submit(text) },
                enabled = !loading && text.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 3.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Добавить")
                }
            }
        }
    }
}

@Composable
private fun SettingsGroup(title: String, content: @Composable () -> Unit) {
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
private fun SettingItem(
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
private fun StateSwitch(checked: Boolean, onChange: (Boolean) -> Unit) {
    Switch(
        checked = checked,
        onCheckedChange = onChange,
        thumbContent = if (checked) {
            { Icon(painterResource(R.drawable.ic_check), null, Modifier.size(SwitchDefaults.IconSize)) }
        } else null
    )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(state: AppState, onDismiss: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    val dynamicSupported = Build.VERSION.SDK_INT >= 31
    val dynamicActive = dynamicSupported && state.dynamicColors
    val modes = listOf("Системная", "Светлая", "Тёмная")
    val power = remember { context.getSystemService(PowerManager::class.java) }

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
            Text(
                "Настройки",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(start = 4.dp)
            )

            SettingsGroup("Оформление") {
                Column(
                    Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Тема", style = MaterialTheme.typography.titleMedium)
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        modes.forEachIndexed { index, label ->
                            SegmentedButton(
                                selected = state.themeMode == index,
                                onClick = { state.setTheme(index) },
                                shape = SegmentedButtonDefaults.itemShape(index, modes.size),
                                icon = { }
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
                        val tone by animateColorAsState(swatch, tween(500), label = "tone")
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
                    SingleChoiceSegmentedButtonRow(
                        Modifier
                            .fillMaxWidth()
                            .alpha(if (dynamicActive) 0.38f else 1f)
                    ) {
                        Accents.styles.forEachIndexed { index, label ->
                            SegmentedButton(
                                selected = state.palette == index,
                                onClick = { if (!dynamicActive) state.changePalette(index) },
                                shape = SegmentedButtonDefaults.itemShape(index, Accents.styles.size),
                                icon = { }
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
                            Box(
                                Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(Accents.swatch(index, state.palette))
                                    .clickable(enabled = !dynamicActive) { state.applyAccent(index) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (state.accent == index) {
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
                        FilledTonalButton(onClick = { requestTile(context) }) { Text("Добавить") }
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
                        FilledTonalButton(onClick = {
                            if (Build.VERSION.SDK_INT >= 33) {
                                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }) { Text("Разрешить") }
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
                        FilledTonalButton(onClick = {
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
                        }) { Text("Выключить") }
                    }
                }
            }

            SettingsGroup("О приложении") {
                SettingItem(
                    icon = R.drawable.ic_info,
                    title = "FasdeqVPN 1.2",
                    subtitle = "VLESS, VMess, Trojan, Shadowsocks, WireGuard"
                )
            }
        }
    }
}
