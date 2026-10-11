package app.fasdeq.vpn

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import libv2ray.CoreCallbackHandler
import libv2ray.CoreController
import libv2ray.Libv2ray

class FasdeqVpnService : VpnService() {

    private var iface: ParcelFileDescriptor? = null
    private var controller: CoreController? = null
    private var stopping = false
    private val lock = Any()

    private val callbacks = object : CoreCallbackHandler {
        override fun startup(): Long = 0

        override fun shutdown(): Long {
            if (!stopping) {
                FasdeqApp.state.onCoreState(false, "Соединение прервано")
                stopSelf()
            }
            return 0
        }

        override fun onEmitStatus(code: Long, message: String?): Long = 0
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_START) {
            val config = intent.getStringExtra(EXTRA_CONFIG)
            if (config != null) start(config)
        }
        return START_NOT_STICKY
    }

    private fun start(config: String) {
        Thread {
            synchronized(lock) {
                try {
                    closeCore()
                    stopping = false
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
                    val core = Libv2ray.newCoreController(callbacks)
                    controller = core
                    val fd = tun.dup().detachFd()
                    core.startLoop(config, fd)
                    FasdeqApp.state.onCoreState(true, null)
                } catch (e: Throwable) {
                    closeCore()
                    FasdeqApp.state.onCoreState(false, e.message ?: "Не удалось запустить")
                    stopSelf()
                }
            }
        }.start()
    }

    private fun closeCore() {
        stopping = true
        try {
            controller?.stopLoop()
        } catch (e: Throwable) {
        }
        controller = null
        try {
            iface?.close()
        } catch (e: Throwable) {
        }
        iface = null
    }

    override fun onRevoke() {
        synchronized(lock) { closeCore() }
        FasdeqApp.state.onCoreState(false, "VPN отключён системой")
        stopSelf()
    }

    override fun onDestroy() {
        synchronized(lock) { closeCore() }
        FasdeqApp.state.onCoreState(false, null)
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "app.fasdeq.vpn.START"
        const val EXTRA_CONFIG = "config"
    }
}
