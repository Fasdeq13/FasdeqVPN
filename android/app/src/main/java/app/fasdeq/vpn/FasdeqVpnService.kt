package app.fasdeq.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.drawable.Icon
import android.net.VpnService
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
    private var wake: PowerManager.WakeLock? = null

    private class Params(
        val outbound: String,
        val core: String,
        val dns: List<String>,
        val mtu: Int,
        val tuning: Tuning = Tuning()
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
                    Tuning.fromJson(intent.getStringExtra(EXTRA_TUNING))
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
                    Tuning.fromJson(prefs.getString(KEY_TUNING, null))
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
                    val outbound = JSONObject(params.outbound)
                    val config: String
                    if (params.core.isNotEmpty() && File(params.core).exists()) {
                        val port = startChild(params.core, outbound)
                        config = Links.buildConfig(Links.socksOutbound(port), params.mtu, params.tuning)
                    } else {
                        config = Links.buildConfig(outbound, params.mtu, params.tuning)
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
                    val apps = t.appList()
                    if (t.appMode == 2 && apps.isNotEmpty()) {
                        for (a in apps) {
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
                        if (t.appMode == 1) {
                            for (a in apps) {
                                try {
                                    builder.addDisallowedApplication(a)
                                } catch (e: Exception) {
                                }
                            }
                        }
                    }
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
                    if (outbound.optString("protocol") == "wireguard") watchdog(gen, core)
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

    private fun watchdog(gen: Int, core: CoreController) {
        Thread {
            var misses = 0
            try {
                Thread.sleep(20_000L)
                while (gen == generation && !stopping && !halted && !userStop) {
                    val ok = try {
                        core.measureDelay("https://www.gstatic.com/generate_204") > 0
                    } catch (e: Throwable) {
                        false
                    }
                    if (ok) misses = 0 else misses++
                    if (misses >= 2) {
                        if (gen == generation && !stopping) recover("WireGuard перестал отвечать, переподключаю")
                        return@Thread
                    }
                    Thread.sleep(if (misses > 0) 5_000L else 25_000L)
                }
            } catch (e: Throwable) {
            }
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
        val core = controller
        controller = null
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
        private const val KEY_RUN = "run"
        private const val KEY_CONFIG = "config"
        private const val KEY_CORE = "core"
        private const val KEY_DNS = "dns"
        private const val KEY_MTU = "mtu"
        private const val KEY_TUNING = "tuning"
        private const val KEY_NAME = "name"
        private const val KEY_ID = "id"
        private const val CHANNEL = "vpn_state"
        private const val NOTIFICATION_ID = 1
    }
}
