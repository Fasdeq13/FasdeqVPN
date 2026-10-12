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
import android.system.Os
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
    private var wake: PowerManager.WakeLock? = null

    private fun handler(gen: Int) = object : CoreCallbackHandler {
        override fun startup(): Long = 0

        override fun shutdown(): Long {
            if (gen == generation && !stopping) halt("Соединение прервано")
            return 0
        }

        override fun onEmitStatus(code: Long, message: String?): Long = 0
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            halt(null)
            return START_NOT_STICKY
        }
        var config: String? = null
        if (intent?.action == ACTION_START) {
            config = intent.getStringExtra(EXTRA_CONFIG)
            serverName = intent.getStringExtra(EXTRA_NAME) ?: ""
            serverId = intent.getStringExtra(EXTRA_ID)
        } else if (intent == null && prefs.getBoolean(KEY_RUN, false)) {
            config = prefs.getString(KEY_CONFIG, null)
            serverName = prefs.getString(KEY_NAME, "") ?: ""
            serverId = prefs.getString(KEY_ID, null)
        }
        enterForeground(false)
        if (config == null) {
            stopForegroundCompat()
            stopSelf()
            return START_NOT_STICKY
        }
        epoch++
        halted = false
        prefs.edit()
            .putBoolean(KEY_RUN, true)
            .putString(KEY_CONFIG, config)
            .putString(KEY_NAME, serverName)
            .putString(KEY_ID, serverId)
            .commit()
        start(config)
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

    private fun start(config: String) {
        Thread {
            synchronized(lock) {
                try {
                    closeCore(3000)
                    generation++
                    stopping = false
                    val gen = generation
                    val tun = Builder()
                        .setSession("FasdeqVPN")
                        .setMtu(1500)
                        .addAddress("172.19.0.1", 30)
                        .addAddress("fdfe:dcba:9876::1", 126)
                        .addRoute("0.0.0.0", 0)
                        .addRoute("::", 0)
                        .addDnsServer("1.1.1.1")
                        .addDnsServer("8.8.8.8")
                        .addDisallowedApplication(packageName)
                        .establish()
                        ?: throw IllegalStateException("Не удалось создать VPN-интерфейс")
                    iface = tun
                    val core = Libv2ray.newCoreController(handler(gen))
                    controller = core
                    val fd = tun.dup().detachFd()
                    coreFd = fd
                    core.startLoop(config, fd)
                    connectedAt = System.currentTimeMillis()
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
    }

    private fun halt(message: String?) {
        synchronized(this) {
            if (halted) return
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
                Thread.sleep(120)
            } catch (e: Throwable) {
            }
            if (mine == epoch) Process.killProcess(Process.myPid())
        }.start()
    }

    override fun onRevoke() {
        halt("VPN отключён системой")
    }

    override fun onDestroy() {
        if (!halted) halt(null)
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "app.fasdeq.vpn.START"
        const val ACTION_STOP = "app.fasdeq.vpn.STOP"
        const val EXTRA_CONFIG = "config"
        const val EXTRA_NAME = "name"
        const val EXTRA_ID = "id"
        private const val KEY_RUN = "run"
        private const val KEY_CONFIG = "config"
        private const val KEY_NAME = "name"
        private const val KEY_ID = "id"
        private const val CHANNEL = "vpn_state"
        private const val NOTIFICATION_ID = 1
    }
}
