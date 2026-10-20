package app.fasdeq.vpn

import android.content.Context
import android.os.Build
import java.net.HttpURLConnection
import java.security.SecureRandom

object Identity {
    val names = listOf("Tunn", "Happ", "v2RayTun", "Incy")
    private val agents = listOf("Tunn/1.8", "Happ/2.0.0/android", "v2raytun/android", "INCY/1.0 (Android)")

    private fun prefs() = FasdeqApp.appContext.getSharedPreferences("fasdeq", Context.MODE_PRIVATE)

    fun mask(): Int = prefs().getInt("mask", 1).coerceIn(0, names.size - 1)

    fun setMask(value: Int) {
        prefs().edit().putInt("mask", value.coerceIn(0, names.size - 1)).apply()
    }

    fun hwid(): String {
        val p = prefs()
        val cur = p.getString("hwid", null)
        if (!cur.isNullOrEmpty()) return cur
        return resetHwid()
    }

    fun resetHwid(): String {
        val bytes = ByteArray(8)
        SecureRandom().nextBytes(bytes)
        val value = bytes.joinToString("") { "%02x".format(it) }
        prefs().edit().putString("hwid", value).apply()
        return value
    }

    fun apply(c: HttpURLConnection) {
        c.setRequestProperty("User-Agent", agents[mask()])
        c.setRequestProperty("x-hwid", hwid())
        c.setRequestProperty("x-device-os", "Android")
        c.setRequestProperty("x-ver-os", Build.VERSION.RELEASE ?: "")
        c.setRequestProperty("x-device-model", Build.MODEL ?: "")
    }
}
