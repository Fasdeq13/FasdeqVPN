package app.fasdeq.vpn

import android.app.Application
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.TrafficStats
import android.os.Build
import android.os.Process
import android.os.SystemClock
import android.service.quicksettings.TileService
import androidx.core.content.ContextCompat
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import kotlin.random.Random

enum class Status { Off, Connecting, On, Stopping }

data class Server(
    val id: String,
    val name: String,
    val conf: String,
    val endpoint: String
)

class AppState(private val app: Application) {
    private val prefs = app.getSharedPreferences("fasdeq", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var pollJob: Job? = null
    private var timeoutJob: Job? = null
    private var since = 0L
    private var baseRx = 0L
    private var baseTx = 0L
    private var activeId: String? = null
    private var pendingId: String? = null

    val servers = mutableStateListOf<Server>()
    var selectedId by mutableStateOf<String?>(prefs.getString("selected", null))
        private set
    var themeMode by mutableIntStateOf(prefs.getInt("theme", 0))
        private set
    var dynamicColors by mutableStateOf(prefs.getBoolean("dynamic", Build.VERSION.SDK_INT >= 31))
        private set
    var accent by mutableIntStateOf(prefs.getInt("accent", 0))
        private set
    var burnIn by mutableStateOf(prefs.getBoolean("burnin", false))
        private set
    var palette by mutableIntStateOf(prefs.getInt("palette", 0))
        private set
    var amoled by mutableStateOf(prefs.getBoolean("amoled", false))
        private set
    var status by mutableStateOf(Status.Off)
        private set
    var error by mutableStateOf<String?>(null)
    var rx by mutableLongStateOf(0L)
        private set
    var tx by mutableLongStateOf(0L)
        private set
    var downSpeed by mutableLongStateOf(0L)
        private set
    var upSpeed by mutableLongStateOf(0L)
        private set
    var uptime by mutableLongStateOf(0L)
        private set

    val selected: Server?
        get() = servers.firstOrNull { it.id == selectedId }

    val activeServerId: String?
        get() = if (status == Status.On) activeId else null

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            onCoreState(intent.getBooleanExtra("up", false), intent.getStringExtra("msg"))
        }
    }

    init {
        servers.addAll(load())
        if (servers.none { it.id == selectedId }) selectedId = servers.firstOrNull()?.id
        ContextCompat.registerReceiver(
            app,
            receiver,
            IntentFilter(VpnBridge.ACTION_STATE),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        syncExternal()
        scope.launch {
            while (true) {
                delay(1500)
                syncExternal()
            }
        }
    }

    fun syncExternal() {
        val saved = VpnBridge.read(app)
        val live = saved.first && VpnBridge.live(app)
        if (live && status == Status.Off) {
            pendingId = saved.second ?: selectedId
            goUp()
        } else if (!live && status == Status.On) {
            finishDown(null)
        }
    }

    fun changePalette(value: Int) {
        palette = value
        prefs.edit().putInt("palette", value).apply()
    }

    fun changeAmoled(value: Boolean) {
        amoled = value
        prefs.edit().putBoolean("amoled", value).apply()
    }

    fun setTheme(value: Int) {
        themeMode = value
        prefs.edit().putInt("theme", value).apply()
    }

    fun setDynamic(value: Boolean) {
        dynamicColors = value
        prefs.edit().putBoolean("dynamic", value).apply()
    }

    fun changeBurnIn(value: Boolean) {
        burnIn = value
        prefs.edit().putBoolean("burnin", value).apply()
    }

    private fun update(value: Status) {
        status = value
        try {
            TileService.requestListeningState(app, ComponentName(app, FasdeqTileService::class.java))
        } catch (e: Throwable) {
        }
    }

    fun applyAccent(value: Int) {
        accent = value
        prefs.edit().putInt("accent", value).apply()
    }

    suspend fun importText(text: String, nameHint: String = ""): String? {
        val input = text.trim()
        if (input.isEmpty()) return "Вставьте ссылку или конфигурацию"
        val items = ArrayList<String>()
        for (item in Links.extract(input)) {
            if (item.startsWith("http://", true) || item.startsWith("https://", true)) {
                val body = try {
                    withContext(Dispatchers.IO) { Links.fetch(item) }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    return "Не удалось загрузить список: " + (e.message ?: "ошибка сети")
                }
                items.addAll(Links.extract(Links.decodeSubscription(body)))
            } else {
                items.add(item)
            }
        }
        var added = 0
        var failure: String? = null
        for (item in items) {
            try {
                val parsed = Links.parse(item)
                if (servers.none { it.conf == item }) {
                    val hint = if (items.size == 1) nameHint else ""
                    val title = hint.ifBlank { parsed.name }.ifBlank { "Сервер ${servers.size + 1}" }
                    val id = "fq" + Integer.toHexString(Random.nextInt(0x100000, 0xFFFFFF))
                    servers.add(Server(id, title, item, parsed.endpoint))
                    if (selectedId == null) selectedId = id
                }
                added++
            } catch (e: Exception) {
                if (failure == null) failure = e.message ?: "Не удалось распознать"
            }
        }
        save()
        return if (added == 0) (failure ?: "Не удалось распознать") else null
    }

    fun remove(server: Server) {
        if (activeId == server.id && status == Status.On) disconnect()
        servers.removeAll { it.id == server.id }
        if (selectedId == server.id) selectedId = servers.firstOrNull()?.id
        save()
    }

    fun select(id: String) {
        if (selectedId == id) return
        selectedId = id
        save()
        val target = servers.firstOrNull { it.id == id } ?: return
        if (status == Status.On) launchSwitch(target)
    }

    fun connect() {
        val target = selected
        if (target == null) {
            error = "Сначала добавьте сервер"
            return
        }
        if (status != Status.Off) return
        launchSwitch(target)
    }

    fun disconnect() {
        if (status != Status.On && status != Status.Connecting) return
        update(Status.Stopping)
        pollJob?.cancel()
        app.stopService(Intent(app, FasdeqVpnService::class.java))
        timeoutJob?.cancel()
        timeoutJob = scope.launch {
            delay(6000)
            if (status == Status.Stopping) finishDown(null)
        }
    }

    fun onCoreState(up: Boolean, message: String?) {
        scope.launch {
            if (up) {
                if (status == Status.Connecting || status == Status.Off) {
                    timeoutJob?.cancel()
                    goUp()
                }
            } else {
                timeoutJob?.cancel()
                finishDown(message)
            }
        }
    }

    private fun goUp() {
        activeId = pendingId ?: selectedId
        since = SystemClock.elapsedRealtime()
        val uid = Process.myUid()
        baseRx = TrafficStats.getUidRxBytes(uid)
        baseTx = TrafficStats.getUidTxBytes(uid)
        rx = 0
        tx = 0
        downSpeed = 0
        upSpeed = 0
        uptime = 0
        update(Status.On)
        startPolling()
    }

    private fun finishDown(message: String?) {
        pollJob?.cancel()
        activeId = null
        downSpeed = 0
        upSpeed = 0
        rx = 0
        tx = 0
        uptime = 0
        update(Status.Off)
        if (message != null) error = message
    }

    private fun launchSwitch(target: Server) {
        val config = try {
            Links.buildConfig(Links.parse(target.conf).outbound)
        } catch (e: Exception) {
            error = e.message ?: "Неверная конфигурация"
            return
        }
        error = null
        pollJob?.cancel()
        pendingId = target.id
        update(Status.Connecting)
        val intent = Intent(app, FasdeqVpnService::class.java)
            .setAction(FasdeqVpnService.ACTION_START)
            .putExtra(FasdeqVpnService.EXTRA_CONFIG, config)
            .putExtra(FasdeqVpnService.EXTRA_NAME, target.name)
            .putExtra(FasdeqVpnService.EXTRA_ID, target.id)
        try {
            ContextCompat.startForegroundService(app, intent)
        } catch (e: Exception) {
            update(Status.Off)
            error = e.message ?: "Не удалось запустить службу"
            return
        }
        timeoutJob?.cancel()
        timeoutJob = scope.launch {
            delay(20000)
            if (status == Status.Connecting && pendingId == target.id) {
                app.stopService(Intent(app, FasdeqVpnService::class.java))
                finishDown("Не удалось подключиться: истекло время ожидания")
            }
        }
    }

    private fun startPolling() {
        pollJob?.cancel()
        val uid = Process.myUid()
        pollJob = scope.launch {
            var lastRx = 0L
            var lastTx = 0L
            while (isActive && status == Status.On) {
                val r = (TrafficStats.getUidRxBytes(uid) - baseRx).coerceAtLeast(0L)
                val t = (TrafficStats.getUidTxBytes(uid) - baseTx).coerceAtLeast(0L)
                downSpeed = (r - lastRx).coerceAtLeast(0L)
                upSpeed = (t - lastTx).coerceAtLeast(0L)
                lastRx = r
                lastTx = t
                rx = r
                tx = t
                uptime = (SystemClock.elapsedRealtime() - since) / 1000
                delay(1000)
            }
        }
    }

    private fun load(): List<Server> {
        val raw = prefs.getString("servers", null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map {
                val o = arr.getJSONObject(it)
                Server(o.getString("id"), o.getString("name"), o.getString("conf"), o.optString("endpoint"))
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun save() {
        val arr = JSONArray()
        servers.forEach {
            arr.put(
                JSONObject()
                    .put("id", it.id)
                    .put("name", it.name)
                    .put("conf", it.conf)
                    .put("endpoint", it.endpoint)
            )
        }
        prefs.edit().putString("servers", arr.toString()).putString("selected", selectedId).apply()
    }
}
