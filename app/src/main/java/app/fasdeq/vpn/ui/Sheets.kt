package app.fasdeq.vpn.ui

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.unit.dp
import app.fasdeq.vpn.AppState
import app.fasdeq.vpn.R
import app.fasdeq.vpn.displayName

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddServerSheet(state: AppState, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var name by remember { mutableStateOf("") }
    var text by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        var failure: String? = null
        var added = 0
        uris.forEach { uri ->
            val label = displayName(context, uri)
            val content = try {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
            } catch (e: Exception) {
                null
            }
            if (content == null) {
                failure = "Не удалось прочитать файл $label"
            } else {
                val result = state.addConfig(label, content)
                if (result != null) failure = "$label: $result" else added++
            }
        }
        if (failure != null) error = failure else if (added > 0) onDismiss()
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
            Text("Добавить сервер", style = MaterialTheme.typography.headlineSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilledTonalButton(
                    onClick = { picker.launch(arrayOf("*/*")) },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                ) {
                    Icon(painterResource(R.drawable.ic_file), null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Из файла")
                }
                FilledTonalButton(
                    onClick = {
                        text = clipboard.getText()?.text ?: ""
                        error = null
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                ) {
                    Icon(painterResource(R.drawable.ic_paste), null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Из буфера")
                }
            }
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Название") },
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = text,
                onValueChange = {
                    text = it
                    error = null
                },
                label = { Text("Конфигурация WireGuard") },
                minLines = 6,
                maxLines = 10,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            )
            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
            Button(
                onClick = {
                    val result = state.addConfig(name, text)
                    if (result == null) onDismiss() else error = result
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("Сохранить")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(state: AppState, onDismiss: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val dynamicSupported = Build.VERSION.SDK_INT >= 31
    val dynamicActive = dynamicSupported && state.dynamicColors
    val modes = listOf("Системная", "Светлая", "Тёмная")

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
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text("Оформление", style = MaterialTheme.typography.headlineSmall)

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Тема", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    modes.forEachIndexed { index, label ->
                        FilterChip(
                            selected = state.themeMode == index,
                            onClick = { state.setTheme(index) },
                            label = { Text(label) },
                            leadingIcon = if (state.themeMode == index) {
                                { Icon(painterResource(R.drawable.ic_check), null, Modifier.size(18.dp)) }
                            } else null
                        )
                    }
                }
            }

            if (dynamicSupported) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Цвета системы", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Material You: палитра из обоев",
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = state.dynamicColors, onCheckedChange = { state.setDynamic(it) })
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Акцентный цвет", style = MaterialTheme.typography.titleMedium)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.alpha(if (dynamicActive) 0.38f else 1f)
                ) {
                    items(Accents.names.indices.toList()) { index ->
                        Box(
                            Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(Accents.swatch(index))
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

            Text(
                "FasdeqVPN 1.0 · WireGuard",
                style = MaterialTheme.typography.labelMedium,
                color = scheme.onSurfaceVariant
            )
        }
    }
}
