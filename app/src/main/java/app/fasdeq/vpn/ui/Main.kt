package app.fasdeq.vpn.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.scrollBy
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.fasdeq.vpn.AppState
import app.fasdeq.vpn.R
import app.fasdeq.vpn.Server
import app.fasdeq.vpn.Status
import app.fasdeq.vpn.formatSpeed
import app.fasdeq.vpn.formatTime
import kotlinx.coroutines.launch

private const val STATS = "\u0000stats"

private sealed class Entry(val key: String) {
    class Header(
        val name: String,
        val servers: List<Server>,
        val url: String,
        val ungrouped: Boolean
    ) : Entry("g:" + if (ungrouped) "\u0000" else name)

    class Row(val server: Server) : Entry(server.id)
}

private sealed class Confirm {
    object Selected : Confirm()
    class DeleteGroup(val name: String) : Confirm()
    class ClearGroup(val name: String) : Confirm()
}

private fun buildEntries(state: AppState, collapsed: Set<String>): List<Entry> {
    fun order(list: List<Server>): List<Server> =
        if (state.sortPing) list.sortedBy { state.pings[it.id]?.best() ?: Int.MAX_VALUE } else list
    val result = ArrayList<Entry>()
    val names = state.groups.map { it.name }.toSet()
    val loose = state.servers.filter { it.group.isBlank() || it.group !in names }
    if (loose.isNotEmpty()) {
        if (names.isNotEmpty()) {
            val header = Entry.Header("", loose, "", true)
            result.add(header)
            if (header.key !in collapsed) order(loose).forEach { result.add(Entry.Row(it)) }
        } else {
            order(loose).forEach { result.add(Entry.Row(it)) }
        }
    }
    for (group in state.groups) {
        val members = state.servers.filter { it.group == group.name }
        val header = Entry.Header(group.name, members, group.url, false)
        result.add(header)
        if (header.key !in collapsed) order(members).forEach { result.add(Entry.Row(it)) }
    }
    return result
}

private fun hit(listState: LazyListState, spacing: Float, y: Float): Int? =
    listState.layoutInfo.visibleItemsInfo
        .firstOrNull { y >= it.offset && y < it.offset + it.size + spacing }
        ?.index

private fun rangeKeys(list: List<Entry>, a: Int, b: Int): Set<String> {
    val lo = minOf(a, b)
    val hi = maxOf(a, b)
    val keys = HashSet<String>()
    for (i in lo..hi) {
        val row = list.getOrNull(i) as? Entry.Row
        if (row != null) keys.add(row.server.id)
    }
    return keys
}

private fun Modifier.selectionGestures(
    listState: LazyListState,
    spacing: Float,
    entries: () -> List<Entry>,
    selection: () -> Set<String>,
    onSelection: (Set<String>) -> Unit,
    onTap: (Int) -> Unit,
    onHaptic: () -> Unit,
    scroll: (Float) -> Unit
): Modifier = pointerInput(Unit) {
    val slop = viewConfiguration.touchSlop
    val longPress = viewConfiguration.longPressTimeoutMillis
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        if (down.isConsumed) return@awaitEachGesture
        val start = hit(listState, spacing, down.position.y) ?: return@awaitEachGesture
        var tapped = false
        var cancelled = false
        val timedOut = withTimeoutOrNull(longPress) {
            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull { it.id == down.id }
                if (change == null || change.isConsumed) {
                    cancelled = true
                    break
                }
                if (change.changedToUp()) {
                    tapped = true
                    break
                }
                if ((change.position - down.position).getDistance() > slop) {
                    cancelled = true
                    break
                }
            }
        } == null
        if (tapped) {
            onTap(start)
            return@awaitEachGesture
        }
        if (cancelled || !timedOut) return@awaitEachGesture
        val entry = entries().getOrNull(start)
        onHaptic()
        if (entry is Entry.Header) {
            onSelection(selection() + entry.servers.map { it.id })
            return@awaitEachGesture
        }
        val row = entry as? Entry.Row ?: return@awaitEachGesture
        val base = selection()
        onSelection(base + row.server.id)
        val edge = 96.dp.toPx()
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val change = event.changes.firstOrNull { it.id == down.id } ?: break
            change.consume()
            if (!change.pressed) break
            val y = change.position.y
            val index = hit(listState, spacing, y)
            if (index != null) onSelection(base + rangeKeys(entries(), start, index))
            if (y < edge) {
                scroll(-(edge - y) / 3f)
            } else if (y > size.height - edge) {
                scroll((y - (size.height - edge)) / 3f)
            }
        }
    }
}

@Composable
fun FasdeqRoot(state: AppState, onPower: () -> Unit) {
    FasdeqTheme(
        state.themeMode,
        state.dynamicColors,
        state.accent,
        state.palette,
        state.amoled,
        state.contrast,
        state.animSpeed
    ) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Box(Modifier.fillMaxSize()) {
                BurnInGuard(state.burnIn) { Home(state, onPower) }
                ConnectWave(state.status)
            }
        }
    }
}

@Composable
private fun Home(state: AppState, onPower: () -> Unit) {
    var showAdd by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var selection by remember { mutableStateOf(setOf<String>()) }
    var collapsed by remember { mutableStateOf(setOf<String>()) }
    var confirm by remember { mutableStateOf<Confirm?>(null) }
    var refreshing by remember { mutableStateOf<String?>(null) }
    val scheme = MaterialTheme.colorScheme
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val selecting = selection.isNotEmpty()
    val entries = buildEntries(state, collapsed)
    val currentEntries by rememberUpdatedState(entries)
    val spacing = with(androidx.compose.ui.platform.LocalDensity.current) { 10.dp.toPx() }

    BackHandler(enabled = selecting) { selection = emptySet() }

    val glow by animateFloatAsState(
        if (state.status == Status.On) 0.22f else 0f,
        tween(Motion.d(900)),
        label = "glow"
    )

    Column(Modifier.fillMaxSize()) {
        Box {
        HeroBackdrop(Modifier.matchParentSize())
        Column(
            Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(scheme.primary.copy(alpha = glow), Color.Transparent)))
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
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.weight(1f)
                )
                FilledTonalIconButton(onClick = { showSettings = true }) {
                    Icon(painterResource(R.drawable.ic_settings), "Настройки", Modifier.size(22.dp))
                }
            }
            Hero(state, onPower)
        }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp),
            color = scheme.surfaceContainerLow
        ) {
            Box(Modifier.fillMaxSize()) {
            Column {
                AnimatedContent(
                    targetState = selecting,
                    transitionSpec = {
                        (fadeIn(tween(Motion.d(250))) + slideInVertically(tween(Motion.d(250))) { -it / 2 }) togetherWith
                            (fadeOut(tween(Motion.d(150))) + slideOutVertically(tween(Motion.d(150))) { it / 2 })
                    },
                    label = "listBar"
                ) { selectingNow ->
                    if (selectingNow) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(start = 12.dp, end = 12.dp, top = 16.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { selection = emptySet() }) {
                                Icon(painterResource(R.drawable.ic_close), "Отмена", Modifier.size(22.dp))
                            }
                            Text(
                                "Выбрано: ${selection.size}",
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { selection = state.servers.map { it.id }.toSet() }) {
                                Icon(painterResource(R.drawable.ic_select_all), "Выбрать все", Modifier.size(22.dp))
                            }
                            FilledTonalIconButton(
                                onClick = { confirm = Confirm.Selected },
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = scheme.errorContainer,
                                    contentColor = scheme.onErrorContainer
                                )
                            ) {
                                Icon(painterResource(R.drawable.ic_delete), "Удалить", Modifier.size(22.dp))
                            }
                        }
                    } else {
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
                            if (state.servers.isNotEmpty()) {
                                FilledTonalIconButton(onClick = { state.pingAll() }) {
                                    Icon(painterResource(R.drawable.ic_bolt), "Проверить пинг", Modifier.size(20.dp))
                                }
                                Spacer(Modifier.width(8.dp))
                            }
                        }
                    }
                }
                if (state.servers.isEmpty() && state.groups.isEmpty()) {
                    EmptyServers { showAdd = true }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .selectionGestures(
                                listState = listState,
                                spacing = spacing,
                                entries = { currentEntries },
                                selection = { selection },
                                onSelection = { selection = it },
                                onTap = { index ->
                                    when (val e = currentEntries.getOrNull(index)) {
                                        is Entry.Row -> {
                                            if (selection.isNotEmpty()) {
                                                selection = if (e.server.id in selection) selection - e.server.id else selection + e.server.id
                                            } else {
                                                state.select(e.server.id)
                                            }
                                        }
                                        is Entry.Header -> {
                                            if (selection.isNotEmpty()) {
                                                val ids = e.servers.map { it.id }
                                                selection = if (ids.isNotEmpty() && selection.containsAll(ids)) {
                                                    selection - ids.toSet()
                                                } else {
                                                    selection + ids
                                                }
                                            } else {
                                                collapsed = if (e.key in collapsed) collapsed - e.key else collapsed + e.key
                                            }
                                        }
                                        null -> Unit
                                    }
                                },
                                onHaptic = { haptic.performHapticFeedback(HapticFeedbackType.LongPress) },
                                scroll = { delta -> scope.launch { listState.scrollBy(delta) } }
                            ),
                        contentPadding = PaddingValues(bottom = navBottom + 16.dp, top = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(entries, key = { it.key }) { entry ->
                            val index = entries.indexOf(entry)
                            val delay = (index.coerceAtMost(8) * 45).toLong()
                            when (entry) {
                                is Entry.Header -> {
                                    val ids = entry.servers.map { it.id }
                                    GroupHeader(
                                        name = entry.name,
                                        count = entry.servers.size,
                                        collapsed = entry.key in collapsed,
                                        checked = if (selecting) (ids.isNotEmpty() && selection.containsAll(ids)) else null,
                                        canRefresh = entry.url.isNotBlank(),
                                        refreshing = refreshing == entry.name,
                                        onPing = { state.ping(entry.servers) },
                                        onRefresh = {
                                            refreshing = entry.name
                                            scope.launch {
                                                val err = state.refreshGroup(entry.name)
                                                if (err != null) state.error = err
                                                refreshing = null
                                            }
                                        },
                                        onClear = { confirm = Confirm.ClearGroup(entry.name) },
                                        onDelete = { confirm = Confirm.DeleteGroup(entry.name) },
                                        enterDelay = delay,
                                        modifier = Modifier.animateItem()
                                    )
                                }
                                is Entry.Row -> ServerCard(
                                    server = entry.server,
                                    selected = entry.server.id == state.selectedId,
                                    active = entry.server.id == state.activeServerId,
                                    ping = state.pings[entry.server.id],
                                    checked = if (selecting) entry.server.id in selection else null,
                                    onPing = { state.ping(listOf(entry.server)) },
                                    enterDelay = delay,
                                    modifier = Modifier.animateItem()
                                )
                            }
                        }
                    }
                }
            }
            val fabScale by animateFloatAsState(
                if (selecting) 0f else 1f,
                tween(Motion.d(300)),
                label = "fab"
            )
            if (fabScale > 0.01f) {
                ExtendedFloatingActionButton(
                    onClick = { showAdd = true },
                    expanded = listState.firstVisibleItemIndex == 0,
                    icon = { Icon(painterResource(R.drawable.ic_add), null, Modifier.size(22.dp)) },
                    text = { Text("Добавить") },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 20.dp, bottom = navBottom + 20.dp)
                        .graphicsLayer {
                            scaleX = fabScale
                            scaleY = fabScale
                            alpha = fabScale
                        }
                )
            }
            }
        }
    }

    if (showAdd) AddServerSheet(state) { showAdd = false }
    if (showSettings) SettingsSheet(state) { showSettings = false }

    confirm?.let { c ->
        val title: String
        val text: String
        when (c) {
            is Confirm.Selected -> {
                title = "Удалить выбранное?"
                text = "Серверов к удалению: ${selection.size}. Вместе с конфигурацией."
            }
            is Confirm.DeleteGroup -> {
                title = "Удалить группу?"
                text = "Группа «${c.name}» и все серверы в ней будут удалены."
            }
            is Confirm.ClearGroup -> {
                title = "Очистить группу?"
                text = "Все серверы из «${c.name}» будут удалены, сама группа останется."
            }
        }
        AlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text(title) },
            text = { Text(text) },
            confirmButton = {
                TextButton(onClick = {
                    when (c) {
                        is Confirm.Selected -> {
                            state.removeMany(selection)
                            selection = emptySet()
                        }
                        is Confirm.DeleteGroup -> state.removeGroup(c.name)
                        is Confirm.ClearGroup -> state.clearGroup(c.name)
                    }
                    confirm = null
                }) {
                    Icon(painterResource(R.drawable.ic_delete), null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirm = null }) {
                    Icon(painterResource(R.drawable.ic_close), null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Отмена")
                }
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
        PowerButton(state.status, state.launchStyle, onPower)
        val titleColor by animateColorAsState(
            if (on) scheme.primary else scheme.onSurface,
            tween(Motion.d(500)),
            label = "titleColor"
        )
        AnimatedContent(
            targetState = title,
            transitionSpec = {
                (fadeIn(tween(Motion.d(300))) + slideInVertically(tween(Motion.d(300))) { it / 3 }) togetherWith
                    (fadeOut(tween(Motion.d(200))) + slideOutVertically(tween(Motion.d(200))) { -it / 3 })
            },
            label = "title"
        ) { value ->
            Text(value, style = MaterialTheme.typography.headlineMedium, color = titleColor)
        }
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
            AnimatedContent(
                targetState = state.error ?: if (on) STATS else null,
                transitionSpec = {
                    (fadeIn(tween(Motion.d(300))) + scaleIn(tween(Motion.d(300)), initialScale = 0.9f)) togetherWith
                        fadeOut(tween(Motion.d(200)))
                },
                label = "info"
            ) { value ->
                if (value == null) {
                    Spacer(Modifier.height(1.dp))
                } else if (value == STATS) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        StatPill(null, formatTime(state.uptime))
                        StatPill(R.drawable.ic_arrow_down, formatSpeed(state.downSpeed))
                        StatPill(R.drawable.ic_arrow_up, formatSpeed(state.upSpeed))
                    }
                } else {
                    Surface(
                        onClick = { state.error = null },
                        shape = RoundedCornerShape(20.dp),
                        color = scheme.errorContainer,
                        contentColor = scheme.onErrorContainer
                    ) {
                        Text(
                            value,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.labelLarge,
                            textAlign = TextAlign.Center,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
