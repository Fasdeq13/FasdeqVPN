package app.fasdeq.vpn.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.fasdeq.vpn.AppState
import app.fasdeq.vpn.R
import app.fasdeq.vpn.Server
import app.fasdeq.vpn.Status
import app.fasdeq.vpn.formatSpeed
import app.fasdeq.vpn.formatTime

@Composable
fun FasdeqRoot(state: AppState, onPower: () -> Unit) {
    FasdeqTheme(state.themeMode, state.dynamicColors, state.accent) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Home(state, onPower)
        }
    }
}

@Composable
private fun Home(state: AppState, onPower: () -> Unit) {
    var showAdd by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var toDelete by remember { mutableStateOf<Server?>(null) }
    val scheme = MaterialTheme.colorScheme
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 16.dp, top = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "FasdeqVPN",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f)
                )
                FilledTonalIconButton(onClick = { showSettings = true }) {
                    Icon(painterResource(R.drawable.ic_settings), "Настройки", Modifier.size(22.dp))
                }
            }
            Hero(state, onPower)
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp),
            color = scheme.surfaceContainerLow
        ) {
            Column {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 16.dp, top = 20.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Серверы",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f)
                    )
                    FilledTonalButton(onClick = { showAdd = true }) {
                        Icon(painterResource(R.drawable.ic_add), null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Добавить")
                    }
                }
                if (state.servers.isEmpty()) {
                    EmptyServers { showAdd = true }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = navBottom + 16.dp, top = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(state.servers, key = { it.id }) { server ->
                            ServerCard(
                                server = server,
                                selected = server.id == state.selectedId,
                                active = server.id == state.activeServerId,
                                onClick = { state.select(server.id) },
                                onDelete = { toDelete = server }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAdd) AddServerSheet(state) { showAdd = false }
    if (showSettings) SettingsSheet(state) { showSettings = false }

    toDelete?.let { server ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Удалить сервер?") },
            text = { Text("«${server.name}» будет удалён вместе с конфигурацией.") },
            confirmButton = {
                TextButton(onClick = {
                    state.remove(server)
                    toDelete = null
                }) { Text("Удалить") }
            },
            dismissButton = {
                TextButton(onClick = { toDelete = null }) { Text("Отмена") }
            }
        )
    }
}

@Composable
private fun Hero(state: AppState, onPower: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val on = state.status == Status.On
    val title = when (state.status) {
        Status.Off -> "Отключено"
        Status.Connecting -> "Подключение…"
        Status.On -> "Защищено"
        Status.Stopping -> "Отключение…"
    }
    Column(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PowerButton(state.status, onPower)
        Text(
            title,
            style = MaterialTheme.typography.headlineMedium,
            color = if (on) scheme.primary else scheme.onSurface
        )
        Text(
            state.selected?.name ?: "Сервер не выбран",
            style = MaterialTheme.typography.bodyLarge,
            color = scheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
        Spacer(Modifier.height(12.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 40.dp)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            val err = state.error
            if (err != null) {
                Surface(
                    onClick = { state.error = null },
                    shape = RoundedCornerShape(20.dp),
                    color = scheme.errorContainer,
                    contentColor = scheme.onErrorContainer
                ) {
                    Text(
                        err,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelLarge,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else if (on) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StatPill(null, formatTime(state.uptime))
                    StatPill(R.drawable.ic_arrow_down, formatSpeed(state.downSpeed))
                    StatPill(R.drawable.ic_arrow_up, formatSpeed(state.upSpeed))
                }
            }
        }
    }
}
