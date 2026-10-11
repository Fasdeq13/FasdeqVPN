package app.fasdeq.vpn

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.Tunnel
import com.wireguard.config.Config
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
import java.io.ByteArrayInputStream
import kotlin.random.Random

enum class Status { Off, Connecting, On, Stopping }

data class Server(
    val id: String,
    val name: String,
    val conf: String,
    val endpoint: String
)

private class WgTunnel(private val ifaceName: String) : Tunnel {
    override fun getName(): String = ifaceName
    override fun onStateChange(newState: Tunnel.State) {}
}

class AppState(app: Application) {
    private val prefs = app.getSharedPreferences("fasdeq", Context.MODE_PRIVATE)
    private val backend = GoBackend(app)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val tunnels = HashMap<String, WgTunnel>()
    private var pollJob: Job? = null
    private var since = 0L
    private var activeId: String? = null

    val servers = mutableStateListOf<Server>()
    var selectedId by mutableStateOf<String?>(prefs.getString("selected", null))
        private set
    var themeMode by mutableIntStateOf(prefs.getInt("theme", 0))
        private set
    var dynamicColors by mutableStateOf(prefs.getBoolean("dynamic", Build.VERSION.SDK_INT >= 31))
        private set
    var accent by mutableIntStateOf(prefs.getInt("accent", 0))
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

    init {
        servers.addAll(load())
        if (servers.none { it.id == selectedId }) selectedId = servers.firstOrNull()?.id
    }

    fun setTheme(value: Int) {
        themeMode = value
        prefs.edit().putInt("theme", value).apply()
    }

    fun setDynamic(value: Boolean) {
        dynamicColors = value
        prefs.edit().putBoolean("dynamic", value).apply()
    }

    fun applyAccent(value: Int) {
        accent = value
        prefs.edit().putInt("accent", value).apply()
    }

    fun addConfig(name: String, text: String): String? {
        val conf = text.trim().replace("\r\n", "\n")
        if (conf.isEmpty()) return "Конфигурация пустая"
        try {
            Config.parse(ByteArrayInputStream(conf.toByteArray()))
        } catch (e: Exception) {
            return "Неверная конфигурация: " + (e.message ?: "ошибка разбора")
        }
        val endpoint = Regex("(?im)^\\s*Endpoint\\s*=\\s*(\\S+)").find(conf)?.groupValues?.get(1) ?: ""
        val id = "fq" + Integer.toHexString(Random.nextInt(0x100000, 0xFFFFFF))
        val title = name.trim().ifEmpty { "Сервер ${servers.size + 1}" }
        servers.add(Server(id, title, conf, endpoint))
        if (selectedId == null) selectedId = id
        save()
        return null
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
        val id = activeId ?: return
        if (status != Status.On) return
        status = Status.Stopping
        pollJob?.cancel()
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    backend.setState(tunnel(id), Tunnel.State.DOWN, null)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                error = e.message ?: "Не удалось отключиться"
            }
            activeId = null
            downSpeed = 0
            upSpeed = 0
            status = Status.Off
        }
    }

    private fun launchSwitch(target: Server) {
        error = null
        pollJob?.cancel()
        status = Status.Connecting
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val old = activeId
                    if (old != null) {
                        try {
                            backend.setState(tunnel(old), Tunnel.State.DOWN, null)
                        } catch (e: Throwable) {
                        }
                        activeId = null
                    }
                    val cfg = Config.parse(ByteArrayInputStream(target.conf.toByteArray()))
                    backend.setState(tunnel(target.id), Tunnel.State.UP, cfg)
                }
                activeId = target.id
                since = SystemClock.elapsedRealtime()
                rx = 0
                tx = 0
                downSpeed = 0
                upSpeed = 0
                uptime = 0
                status = Status.On
                startPolling(target.id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                activeId = null
                status = Status.Off
                error = e.message ?: "Не удалось подключиться"
            }
        }
    }

    private fun startPolling(id: String) {
        pollJob?.cancel()
        pollJob = scope.launch {
            var lastRx = -1L
            var lastTx = 0L
            while (isActive && status == Status.On) {
                val stats = try {
                    withContext(Dispatchers.IO) { backend.getStatistics(tunnel(id)) }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Throwable) {
                    null
                }
                if (stats != null) {
                    val r = stats.totalRx()
                    val t = stats.totalTx()
                    if (lastRx >= 0) {
                        downSpeed = maxOf(0L, r - lastRx)
                        upSpeed = maxOf(0L, t - lastTx)
                    }
                    lastRx = r
                    lastTx = t
                    rx = r
                    tx = t
                }
                uptime = (SystemClock.elapsedRealtime() - since) / 1000
                delay(1000)
            }
        }
    }

    private fun tunnel(id: String): WgTunnel = tunnels.getOrPut(id) { WgTunnel(id) }

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
