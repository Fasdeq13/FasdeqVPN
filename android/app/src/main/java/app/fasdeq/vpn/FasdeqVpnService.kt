package app.fasdeq.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.drawable.Icon
import android.net.ConnectivityManager
import android.net.Network
import android.net.VpnService
import android.os.Handler
import android.os.Looper
import android.os.Build
import android.os.ParcelFileDescriptor
import android.os.PowerManager
import android.os.Process
import android.os.SystemClock
import android.system.Os
import org.json.JSONObject
import java.io.File
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import libv2ray.CoreCallbackHandler
import libv2ray.CoreController
import libv2ray.Libv2ray

class FasdeqVpnService : VpnService() {

    private val lock = Any()
    private val prefs by lazy { getSharedPreferences("vpn_service", Context.MODE_PRIVATE) }

    @Volatile private var iface: ParcelFileDescriptor? = null
    @Volatile private var controller: CoreController? = null
    @Volatile private var coreFd = -1
    @Volatile private var pluginHandle = 0L
    @Volatile private var stopping = false
    @Volatile private var halted = false
    @Volatile private var generation = 0
    @Volatile private var epoch = 0
    @Volatile private var serverName = ""
    @Volatile private var serverId: String? = null
    @Volatile private var connectedAt = 0L
    @Volatile private var child: java.lang.Process? = null
    @Volatile private var lastParams: Params? = null
    @Volatile private var userStop = false
    private val recoveries = ArrayList<Long>()
    private val mainHandler = Handler(Looper.getMainLooper())
    @Volatile private var netCallback: ConnectivityManager.NetworkCallback? = null
    @Volatile private var netCurrent: Network? = null
    @Volatile private var netJob: Runnable? = null
    private var wake: PowerManager.WakeLock? = null

    private class Params(
        val outbound: String,
        val core: String,
        val dns: List<String>,
        val mtu: Int,
        val tuning: Tuning = Tuning(),
        val rules: String = "",
        val pluginLib: String = "",
        val pluginConf: String = ""
    )

    private fun handler(gen: Int) = object : CoreCallbackHandler {
        override fun startup(): Long = 0

        override fun shutdown(): Long {
            if (gen == generation && !stopping) recover("Соединение прервано")
            return 0
        }

        override fun onEmitStatus(code: Long, message: String?): Long = 0
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            userStop = true
            halt(null, true)
            return START_NOT_STICKY
        }
        var params: Params? = null
        if (intent?.action == ACTION_START) {
            val outbound = intent.getStringExtra(EXTRA_OUTBOUND)
            if (outbound != null) {
                params = Params(
                    outbound,
                    intent.getStringExtra(EXTRA_CORE) ?: "",
                    (intent.getStringExtra(EXTRA_DNS) ?: "1.1.1.1,8.8.8.8").split(',').filter { it.isNotBlank() },
                    intent.getIntExtra(EXTRA_MTU, 1500),
                    Tuning.fromJson(intent.getStringExtra(EXTRA_TUNING)),
                    intent.getStringExtra(EXTRA_RULES) ?: "",
                    intent.getStringExtra(EXTRA_PLUGIN_LIB) ?: "",
                    intent.getStringExtra(EXTRA_PLUGIN_CONF) ?: ""
                )
            }
            serverName = intent.getStringExtra(EXTRA_NAME) ?: ""
            serverId = intent.getStringExtra(EXTRA_ID)
        } else if (intent == null && prefs.getBoolean(KEY_RUN, false)) {
            val outbound = prefs.getString(KEY_CONFIG, null)
            if (outbound != null) {
                params = Params(
                    outbound,
                    prefs.getString(KEY_CORE, "") ?: "",
                    (prefs.getString(KEY_DNS, "1.1.1.1,8.8.8.8") ?: "1.1.1.1,8.8.8.8").split(',').filter { it.isNotBlank() },
                    prefs.getInt(KEY_MTU, 1500),
                    Tuning.fromJson(prefs.getString(KEY_TUNING, null)),
                    prefs.getString(KEY_RULES, "") ?: "",
                    prefs.getString(KEY_PLUGIN_LIB, "") ?: "",
                    prefs.getString(KEY_PLUGIN_CONF, "") ?: ""
                )
            }
            serverName = prefs.getString(KEY_NAME, "") ?: ""
            serverId = prefs.getString(KEY_ID, null)
        }
        enterForeground(false)
        if (params == null) {
            stopForegroundCompat()
            stopSelf()
            return START_NOT_STICKY
        }
        epoch++
        halted = false
        userStop = false
        lastParams = params
        synchronized(recoveries) { recoveries.clear() }
        prefs.edit()
            .putBoolean(KEY_RUN, true)
            .putString(KEY_CONFIG, params.outbound)
            .putString(KEY_CORE, params.core)
            .putString(KEY_DNS, params.dns.joinToString(","))
            .putInt(KEY_MTU, params.mtu)
            .putString(KEY_TUNING, params.tuning.toJson())
            .putString(KEY_RULES, params.rules)
            .putString(KEY_PLUGIN_LIB, params.pluginLib)
            .putString(KEY_PLUGIN_CONF, params.pluginConf)
            .putString(KEY_NAME, serverName)
            .putString(KEY_ID, serverId)
            .commit()
        start(params)
        return START_STICKY
    }

    private fun buildNotification(connected: Boolean): Notification {
        val channel = NotificationChannel(CHANNEL, "Состояние VPN", NotificationManager.IMPORTANCE_LOW)
        channel.setShowBadge(false)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val stop = PendingIntent.getService(
            this,
            1,
            Intent(this, FasdeqVpnService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val target = if (serverName.isBlank()) "серверу" else serverName
        val builder = Notification.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_power)
            .setContentTitle("FasdeqVPN")
            .setContentText(if (connected) "Вы подключены к $target" else "Подключение к $target…")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setContentIntent(open)
            .addAction(
                Notification.Action.Builder(
                    Icon.createWithResource(this, R.drawable.ic_power),
                    "Отключить",
                    stop
                ).build()
            )
        if (connected && connectedAt > 0) {
            builder.setShowWhen(true).setWhen(connectedAt).setUsesChronometer(true)
        }
        return builder.build()
    }

    private fun enterForeground(connected: Boolean) {
        val notification = buildNotification(connected)
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun stopForegroundCompat() {
        try {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } catch (e: Throwable) {
        }
    }

    private fun acquireWake() {
        releaseWake()
        try {
            val pm = getSystemService(PowerManager::class.java)
            val lockObj = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "fasdeq:vpn")
            lockObj.setReferenceCounted(false)
            lockObj.acquire()
            wake = lockObj
        } catch (e: Throwable) {
        }
    }

    private fun releaseWake() {
        try {
            wake?.let { if (it.isHeld) it.release() }
        } catch (e: Throwable) {
        }
        wake = null
    }

    private fun pidOf(p: java.lang.Process): Int = try {
        val f = p.javaClass.getDeclaredField("pid")
        f.isAccessible = true
        f.getInt(p)
    } catch (e: Throwable) {
        -1
    }

    private fun pidFile() = File(filesDir, "core.pid")

    private fun killOrphan() {
        try {
            val f = pidFile()
            if (f.exists()) {
                val pid = f.readText().trim().toIntOrNull()
                if (pid != null && pid > 1) Process.killProcess(pid)
                f.delete()
            }
        } catch (e: Throwable) {
        }
    }

    private fun killChild() {
        val p = child
        child = null
        if (p != null) {
            try {
                p.destroyForcibly()
            } catch (e: Throwable) {
            }
            val pid = pidOf(p)
            if (pid > 1) {
                try {
                    Process.killProcess(pid)
                } catch (e: Throwable) {
                }
            }
        }
        try {
            pidFile().delete()
        } catch (e: Throwable) {
        }
    }

    private fun alive(p: java.lang.Process): Boolean = try {
        p.exitValue()
        false
    } catch (e: IllegalThreadStateException) {
        true
    }

    private fun startChild(path: String, outbound: JSONObject): Int {
        killOrphan()
        val port = ServerSocket(0).use { it.localPort }
        Links.resolve(outbound)
        val cfg = File(filesDir, "core_run.json")
        cfg.writeText(Links.buildInner(outbound, port))
        val process = ProcessBuilder(listOf(Cores.linker(), path, "run", "-c", cfg.absolutePath))
            .redirectErrorStream(true)
            .directory(filesDir)
            .start()
        child = process
        val pid = pidOf(process)
        if (pid > 1) {
            try {
                pidFile().writeText(pid.toString())
            } catch (e: Throwable) {
            }
        }
        val tail = ArrayList<String>()
        Thread {
            try {
                process.inputStream.bufferedReader().forEachLine {
                    synchronized(tail) {
                        tail.add(it)
                        if (tail.size > 8) tail.removeAt(0)
                    }
                }
            } catch (e: Throwable) {
            }
        }.start()
        val deadline = SystemClock.elapsedRealtime() + 7000
        while (SystemClock.elapsedRealtime() < deadline) {
            if (!alive(process)) {
                val last = synchronized(tail) { tail.lastOrNull() } ?: "без вывода"
                throw IllegalStateException("Ядро Xray завершилось: $last")
            }
            try {
                Socket().use { it.connect(InetSocketAddress("127.0.0.1", port), 200) }
                Thread {
                    try {
                        process.waitFor()
                    } catch (e: Throwable) {
                    }
                    if (child === process && !stopping && !halted) recover("Ядро Xray остановилось")
                }.start()
                return port
            } catch (e: Exception) {
                Thread.sleep(150)
            }
        }
        throw IllegalStateException("Ядро Xray не запустилось")
    }

    private fun start(params: Params) {
        Thread {
            synchronized(lock) {
                try {
                    closeCore(3000)
                    generation++
                    stopping = false
                    val gen = generation
                    if (params.pluginLib.isNotEmpty()) {
                        startPlugin(params)
                        return@synchronized
                    }
                    val rules = parseRules(params.rules)
                    val outbound = JSONObject(params.outbound)
                    val config: String
                    if (params.core.isNotEmpty() && File(params.core).exists()) {
                        val port = startChild(params.core, outbound)
                        config = Links.buildConfig(Links.socksOutbound(port), params.mtu, params.tuning, rules)
                    } else {
                        config = Links.buildConfig(outbound, params.mtu, params.tuning, rules)
                    }
                    val t = params.tuning
                    val builder = Builder()
                        .setSession("FasdeqVPN")
                        .setMtu(params.mtu)
                        .addAddress("172.19.0.1", 30)
                        .addRoute("0.0.0.0", 0)
                    if (t.ipv6) {
                        builder.addAddress("fdfe:dcba:9876::1", 126).addRoute("::", 0)
                    }
                    if (Build.VERSION.SDK_INT >= 29) builder.setMetered(!t.unmetered)
                    applyApps(builder, t, rules)
                    for (dns in params.dns) {
                        try {
                            builder.addDnsServer(dns)
                        } catch (e: Exception) {
                        }
                    }
                    val tun = builder.establish()
                        ?: throw IllegalStateException("Не удалось создать VPN-интерфейс")
                    iface = tun
                    val core = Libv2ray.newCoreController(handler(gen))
                    controller = core
                    val fd = tun.dup().detachFd()
                    coreFd = fd
                    core.startLoop(config, fd)
                    connectedAt = System.currentTimeMillis()
                    watchdog(gen, core, outbound.optString("protocol") == "wireguard")
                    registerNet(gen)
                    acquireWake()
                    VpnBridge.write(this, true, serverId)
                    VpnBridge.send(this, true, null)
                    getSystemService(NotificationManager::class.java)
                        .notify(NOTIFICATION_ID, buildNotification(true))
                } catch (e: Throwable) {
                    closeCore(1500)
                    releaseWake()
                    prefs.edit().putBoolean(KEY_RUN, false).remove(KEY_CONFIG).commit()
                    VpnBridge.write(this, false, null)
                    VpnBridge.send(this, false, e.message ?: "Не удалось запустить")
                    stopForegroundCompat()
                    stopSelf()
                }
            }
        }.start()
    }

    private fun parseRules(raw: String): JSONObject = try {
        if (raw.isBlank()) JSONObject() else JSONObject(raw)
    } catch (e: Throwable) {
        JSONObject()
    }

    private fun strings(a: org.json.JSONArray?): List<String> =
        if (a == null) emptyList() else (0 until a.length()).map { a.optString(it).trim() }.filter { it.isNotEmpty() }

    private fun applyApps(builder: Builder, t: Tuning, rules: JSONObject) {
        val apps = t.appList()
        val only = if (t.appMode == 0) strings(rules.optJSONArray("onlyApps")) else emptyList()
        val bypass = strings(rules.optJSONArray("bypassApps"))
        if ((t.appMode == 2 && apps.isNotEmpty()) || only.isNotEmpty()) {
            for (a in (if (t.appMode == 2) apps else only)) {
                try {
                    builder.addAllowedApplication(a)
                } catch (e: Exception) {
                }
            }
        } else {
            try {
                builder.addDisallowedApplication(packageName)
            } catch (e: Exception) {
            }
            val list = (if (t.appMode == 1) apps else emptyList()) + bypass
            for (a in list) {
                try {
                    builder.addDisallowedApplication(a)
                } catch (e: Exception) {
                }
            }
        }
    }

    private fun cidr(text: String): Pair<String, Int>? {
        val i = text.indexOf('/')
        val host = (if (i < 0) text else text.substring(0, i)).trim()
        if (host.isEmpty()) return null
        val prefix = if (i < 0) (if (host.contains(':')) 128 else 32) else text.substring(i + 1).trim().toIntOrNull() ?: return null
        return Pair(host, prefix)
    }

    private fun startPlugin(params: Params) {
        val opened = JSONObject(Native.pluginOpen(params.pluginLib))
        if (!opened.optBoolean("ok")) throw IllegalStateException(opened.optString("error", "Не удалось загрузить плагин"))
        val handle = opened.getLong("handle")
        pluginHandle = handle
        val prep = JSONObject(Native.pluginCall(handle, "prepare", params.pluginConf, -1, 0L))
        if (!prep.optBoolean("ok")) throw IllegalStateException(prep.optString("error", "Плагин не подготовил туннель"))
        val v = prep.getJSONObject("value")
        if (v.has("error")) throw IllegalStateException(v.optString("error"))
        val t = params.tuning
        val builder = Builder().setSession("FasdeqVPN").setMtu(v.optInt("mtu", params.mtu))
        val addresses = strings(v.optJSONArray("address"))
        if (addresses.isEmpty()) builder.addAddress("172.19.0.1", 30)
        for (a in addresses) {
            val c = cidr(a) ?: continue
            try {
                builder.addAddress(c.first, c.second)
            } catch (e: Exception) {
            }
        }
        val routes = strings(v.optJSONArray("routes")).ifEmpty { if (t.ipv6) listOf("0.0.0.0/0", "::/0") else listOf("0.0.0.0/0") }
        for (r in routes) {
            val c = cidr(r) ?: continue
            try {
                builder.addRoute(c.first, c.second)
            } catch (e: Exception) {
            }
        }
        if (Build.VERSION.SDK_INT >= 29) builder.setMetered(!t.unmetered)
        applyApps(builder, t, parseRules(params.rules))
        val dns = strings(v.optJSONArray("dns")).ifEmpty { params.dns }
        for (d in dns) {
            try {
                builder.addDnsServer(d)
            } catch (e: Exception) {
            }
        }
        val tun = builder.establish() ?: throw IllegalStateException("Не удалось создать VPN-интерфейс")
        iface = tun
        val fd = tun.dup().detachFd()
        coreFd = fd
        val started = JSONObject(Native.pluginCall(handle, "start", params.pluginConf, fd, 0L))
        if (!started.optBoolean("ok")) throw IllegalStateException(started.optString("error", "Плагин не запустился"))
        connectedAt = System.currentTimeMillis()
        pluginWatch(generation, handle)
        registerNet(generation)
        acquireWake()
        VpnBridge.write(this, true, serverId)
        VpnBridge.send(this, true, null)
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, buildNotification(true))
    }

    private fun probe(core: CoreController): Boolean {
        for (u in listOf("https://www.gstatic.com/generate_204", "https://cp.cloudflare.com/generate_204")) {
            val ok = try {
                core.measureDelay(u) > 0
            } catch (e: Throwable) {
                false
            }
            if (ok) return true
        }
        return false
    }

    private fun watchdog(gen: Int, core: CoreController, wireguard: Boolean) {
        Thread {
            var misses = 0
            try {
                Thread.sleep(15_000L)
                while (gen == generation && !stopping && !halted && !userStop) {
                    if (netCurrent == null) {
                        misses = 0
                        Thread.sleep(5_000L)
                        continue
                    }
                    if (probe(core)) misses = 0 else misses++
                    if (misses >= (if (wireguard) 2 else 3)) {
                        if (gen == generation && !stopping) {
                            recover(if (wireguard) "WireGuard перестал отвечать, переподключаю" else "Соединение зависло, переподключаю")
                        }
                        return@Thread
                    }
                    Thread.sleep(if (misses > 0) 4_000L else 20_000L)
                }
            } catch (e: Throwable) {
            }
        }.start()
    }

    private fun pluginStats(handle: Long): JSONObject? = try {
        val raw = JSONObject(Native.pluginCall(handle, "stats", "", -1, 0L))
        if (raw.optBoolean("ok")) JSONObject(raw.getString("value")) else null
    } catch (e: Throwable) {
        null
    }

    private fun pluginWatch(gen: Int, handle: Long) {
        Thread {
            try {
                val begin = SystemClock.elapsedRealtime()
                var lastRx = -1L
                var lastTx = 0L
                var stallSince = 0L
                var shaken = false
                while (gen == generation && !stopping && !halted && !userStop) {
                    Thread.sleep(3_000L)
                    val v = pluginStats(handle) ?: continue
                    val now = SystemClock.elapsedRealtime()
                    val hs = v.optLong("hs", 0L)
                    val rx = v.optLong("rx", 0L)
                    val tx = v.optLong("tx", 0L)
                    if (hs > 0L) shaken = true
                    if (!shaken && now - begin > 25_000L) {
                        halt("Нет рукопожатия с сервером. Проверьте ключи, порт и параметры AmneziaWG (Jc, S1, S2, H1-H4)")
                        return@Thread
                    }
                    if (shaken) {
                        if (rx != lastRx) {
                            lastRx = rx
                            stallSince = 0L
                        } else if (tx - lastTx > 4096L && netCurrent != null) {
                            if (stallSince == 0L) stallSince = now
                            if (now - stallSince > 45_000L) {
                                recover("Туннель перестал отвечать, переподключаю")
                                return@Thread
                            }
                        }
                        lastTx = tx
                    }
                }
            } catch (e: Throwable) {
            }
        }.start()
    }

    private fun applyUnderlying(network: Network?) {
        try {
            setUnderlyingNetworks(if (network == null) null else arrayOf(network))
        } catch (e: Throwable) {
        }
    }

    private fun registerNet(gen: Int) {
        unregisterNet()
        val cm: ConnectivityManager? = try {
            getSystemService(ConnectivityManager::class.java)
        } catch (e: Throwable) {
            null
        }
        if (cm == null) return
        netCurrent = try {
            cm.activeNetwork
        } catch (e: Throwable) {
            null
        }
        applyUnderlying(netCurrent)
        val cb = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                val prev = netCurrent
                netCurrent = network
                applyUnderlying(network)
                if (prev != null && prev != network && gen == generation && !halted && !userStop && !stopping) {
                    scheduleReconnect(gen)
                }
            }

            override fun onLost(network: Network) {
                if (network == netCurrent) netCurrent = null
            }
        }
        try {
            cm.registerDefaultNetworkCallback(cb)
            netCallback = cb
        } catch (e: Throwable) {
        }
    }

    private fun unregisterNet() {
        val cb = netCallback
        netCallback = null
        netJob?.let { mainHandler.removeCallbacks(it) }
        netJob = null
        if (cb != null) {
            try {
                getSystemService(ConnectivityManager::class.java).unregisterNetworkCallback(cb)
            } catch (e: Throwable) {
            }
        }
    }

    private fun scheduleReconnect(gen: Int) {
        netJob?.let { mainHandler.removeCallbacks(it) }
        val job = Runnable {
            if (gen == generation && !halted && !userStop && !stopping) reconnect()
        }
        netJob = job
        mainHandler.postDelayed(job, 2_000L)
    }

    private fun reconnect() {
        val params = lastParams ?: return
        Thread {
            if (!halted && !userStop) start(params)
        }.start()
    }

    private fun closeTun() {
        val fd = coreFd
        coreFd = -1
        if (fd >= 0) {
            try {
                val link = Os.readlink("/proc/self/fd/$fd")
                if (link.contains("tun")) ParcelFileDescriptor.adoptFd(fd).close()
            } catch (e: Throwable) {
            }
        }
        try {
            iface?.close()
        } catch (e: Throwable) {
        }
        iface = null
    }

    private fun closeCore(wait: Long) {
        stopping = true
        unregisterNet()
        val core = controller
        controller = null
        val plugin = pluginHandle
        pluginHandle = 0L
        if (plugin != 0L) {
            val stopper = Thread {
                try {
                    Native.pluginCall(plugin, "stop", "", -1, 0L)
                    Native.pluginClose(plugin)
                } catch (e: Throwable) {
                }
            }
            stopper.start()
            stopper.join(wait)
        }
        if (core != null) {
            val worker = Thread {
                try {
                    core.stopLoop()
                } catch (e: Throwable) {
                }
            }
            worker.start()
            worker.join(wait)
        }
        closeTun()
        killChild()
    }

    private fun recover(message: String) {
        val params = lastParams
        val now = SystemClock.elapsedRealtime()
        var allowed = false
        synchronized(recoveries) {
            recoveries.removeAll { now - it > 120_000L }
            allowed = params != null && params.tuning.reconnect && recoveries.size < params.tuning.retries
            if (allowed) recoveries.add(now)
        }
        if (params == null || halted || userStop || !allowed) {
            halt(message)
            return
        }
        Thread {
            try {
                Thread.sleep(params.tuning.retryDelay * 1000L)
            } catch (e: Throwable) {
            }
            if (!halted && !userStop) start(params)
        }.start()
    }

    private fun halt(message: String?, force: Boolean = false) {
        synchronized(this) {
            if (halted && !force) return
            halted = true
        }
        val mine = epoch
        stopping = true
        prefs.edit().putBoolean(KEY_RUN, false).remove(KEY_CONFIG).commit()
        VpnBridge.write(this, false, null)
        VpnBridge.send(this, false, message)
        releaseWake()
        stopForegroundCompat()
        Thread {
            try {
                closeCore(1500)
            } catch (e: Throwable) {
            }
            try {
                Thread.sleep(120)
            } catch (e: Throwable) {
            }
            if (mine == epoch) Process.killProcess(Process.myPid())
        }.start()
    }

    override fun onRevoke() {
        userStop = true
        halt("VPN отключён: соединение занято другим VPN-приложением или системой")
    }

    override fun onDestroy() {
        userStop = true
        if (!halted) halt(null)
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "app.fasdeq.vpn.START"
        const val ACTION_STOP = "app.fasdeq.vpn.STOP"
        const val EXTRA_OUTBOUND = "outbound"
        const val EXTRA_CORE = "core"
        const val EXTRA_DNS = "dns"
        const val EXTRA_MTU = "mtu"
        const val EXTRA_TUNING = "tuning"
        const val EXTRA_NAME = "name"
        const val EXTRA_ID = "id"
        const val EXTRA_RULES = "rules"
        const val EXTRA_PLUGIN_LIB = "plugin_lib"
        const val EXTRA_PLUGIN_CONF = "plugin_conf"
        private const val KEY_RUN = "run"
        private const val KEY_CONFIG = "config"
        private const val KEY_CORE = "core"
        private const val KEY_DNS = "dns"
        private const val KEY_MTU = "mtu"
        private const val KEY_TUNING = "tuning"
        private const val KEY_NAME = "name"
        private const val KEY_ID = "id"
        private const val KEY_RULES = "rules"
        private const val KEY_PLUGIN_LIB = "plugin_lib"
        private const val KEY_PLUGIN_CONF = "plugin_conf"
        private const val CHANNEL = "vpn_state"
        private const val NOTIFICATION_ID = 1
    }
}
