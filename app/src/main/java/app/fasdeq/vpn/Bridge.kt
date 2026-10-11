package app.fasdeq.vpn

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import java.io.File

object VpnBridge {
    const val ACTION_STATE = "app.fasdeq.vpn.STATE"

    private fun file(context: Context) = File(context.filesDir, "vpn_state")

    fun write(context: Context, up: Boolean, id: String?) {
        try {
            file(context).writeText(if (up) "1\n" + (id ?: "") else "0\n")
        } catch (e: Throwable) {
        }
    }

    fun read(context: Context): Pair<Boolean, String?> {
        return try {
            val lines = file(context).readText().lines()
            Pair(lines.firstOrNull() == "1", lines.getOrNull(1)?.ifEmpty { null })
        } catch (e: Throwable) {
            Pair(false, null)
        }
    }

    fun live(context: Context): Boolean {
        return try {
            val cm = context.getSystemService(ConnectivityManager::class.java)
            val net = cm.activeNetwork ?: return false
            cm.getNetworkCapabilities(net)?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true
        } catch (e: Throwable) {
            false
        }
    }

    fun send(context: Context, up: Boolean, message: String?) {
        try {
            context.sendBroadcast(
                Intent(ACTION_STATE)
                    .setPackage(context.packageName)
                    .putExtra("up", up)
                    .putExtra("msg", message)
            )
        } catch (e: Throwable) {
        }
    }
}
