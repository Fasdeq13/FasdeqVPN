package app.fasdeq.vpn.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.fasdeq.vpn.AppState
import app.fasdeq.vpn.R
import app.fasdeq.vpn.displayName
import kotlinx.coroutines.launch

@Composable
private fun CoreRow(
    selected: Boolean,
    title: String,
    subtitle: String,
    onSelect: () -> Unit,
    onDelete: (() -> Unit)?
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(start = 10.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = null, modifier = Modifier.padding(8.dp))
        Spacer(Modifier.width(6.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle.isNotBlank()) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (onDelete != null) {
            IconButton(onClick = onDelete) {
                Icon(painterResource(R.drawable.ic_delete), "Удалить ядро", Modifier.size(22.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoresSheet(state: AppState, onDismiss: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var message by remember { mutableStateOf<String?>(null) }
    var showReleases by remember { mutableStateOf(false) }
    val busy = state.coreBusy != null

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                val label = displayName(context, uri).substringBeforeLast('.')
                message = state.importCore(uri, label) ?: "Ядро добавлено"
            }
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
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SheetHeader(R.drawable.ic_memory, "Ядро Xray", "Версия и собственный бинарник")
            Text(
                "Своё ядро запускается отдельным процессом: встроенное ядро создаёт туннель и отправляет трафик в запущенный Xray. Нужен исполняемый файл Xray для Android (arm64) или zip-архив с ним. Выбор сохраняется после перезапуска и применяется при следующем подключении.",
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            AnimatedVisibility(
                visible = busy,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text(state.coreBusy ?: "", style = MaterialTheme.typography.bodyMedium)
                }
            }

            SettingsGroup("Активное ядро") {
                CoreRow(
                    selected = state.activeCore.isEmpty(),
                    title = "Встроенное",
                    subtitle = if (state.builtinVersion.isNotBlank()) "Xray " + state.builtinVersion else "Идёт в комплекте с приложением",
                    onSelect = { state.selectCore("") },
                    onDelete = null
                )
                state.cores.forEach { core ->
                    CoreRow(
                        selected = state.activeCore == core.id,
                        title = core.label,
                        subtitle = if (core.version.isNotBlank()) "Xray " + core.version else "",
                        onSelect = { state.selectCore(core.id) },
                        onDelete = { state.deleteCore(core.id) }
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilledTonalButton(
                    onClick = { picker.launch(arrayOf("*/*")) },
                    enabled = !busy,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(painterResource(R.drawable.ic_add), null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Из файла")
                }
                FilledTonalButton(
                    onClick = {
                        showReleases = !showReleases
                        if (showReleases && state.releases == null) {
                            scope.launch { message = state.loadReleases() }
                        }
                    },
                    enabled = !busy,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(painterResource(R.drawable.ic_arrow_down), null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Скачать версию")
                }
            }

            AnimatedVisibility(
                visible = showReleases,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                val list = state.releases
                if (list == null) {
                    Text(
                        "Список версий загружается…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant
                    )
                } else {
                    SettingsGroup("Версии Xray-core") {
                        list.take(12).forEach { release ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 18.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(release.tag, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        String.format("%.1f МБ", release.size / 1048576.0),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = scheme.onSurfaceVariant
                                    )
                                }
                                ActionButton(
                                    icon = R.drawable.ic_arrow_down,
                                    text = "Скачать",
                                    onClick = { scope.launch { message = state.downloadCore(release) ?: "Ядро ${release.tag} установлено и выбрано" } },
                                    enabled = !busy
                                )
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = message != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Text(
                    message ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.primary,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
    }
}
