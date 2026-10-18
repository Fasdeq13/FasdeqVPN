package app.fasdeq.vpn

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.HapticFeedbackConstants
import android.webkit.JavascriptInterface
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.util.concurrent.FutureTask

class WebBridge(private val host: WebUiActivity) {
    private val main = Handler(Looper.getMainLooper())
    private val prefs = host.getSharedPreferences("fasdeq", Context.MODE_PRIVATE)
    private val app get() = FasdeqApp.state

    private fun <T> onMain(block: () -> T): T {
        if (Looper.myLooper() == Looper.getMainLooper()) return block()
        val task = FutureTask { block() }
        main.post(task)
        return task.get()
    }

    @JavascriptInterface
    fun state(): String = onMain { snapshot() }

    @JavascriptInterface
    fun ready() {
        UiEngine.markReady(host)
    }

    @JavascriptInterface
    fun bars(dark: Boolean) {
        main.post { host.applyBars(dark) }
    }

    @JavascriptInterface
    fun act(name: String, payload: String, id: Int) {
        main.post {
            try {
                handle(name, payload, id)
            } catch (e: Throwable) {
                reply(id, JSONObject().put("error", e.message ?: "Ошибка"))
            }
        }
    }

    fun reply(id: Int, value: JSONObject = JSONObject()) {
        host.runJs("window.__fasdeqResult(" + id + "," + JSONObject.quote(value.toString()) + ")")
    }

    fun push() {
        host.runJs("window.__fasdeqPush(" + JSONObject.quote(snapshot()) + ")")
    }

    private fun parse(raw: String): Any? {
        if (raw.isEmpty()) return null
        return try {
            JSONTokener(raw).nextValue()
        } catch (e: Throwable) {
            raw
        }
    }

    private fun handle(name: String, raw: String, id: Int) {
        val value = parse(raw)
        val text = (value as? String) ?: ""
        when (name) {
            "connect" -> {
                host.requestConnect()
                reply(id)
            }
            "disconnect" -> {
                app.disconnect()
                reply(id)
            }
            "select" -> {
                app.select(text)
                reply(id)
            }
            "remove" -> {
                app.servers.firstOrNull { it.id == text }?.let { app.remove(it) }
                reply(id)
            }
            "removeGroup" -> {
                app.removeGroup(text)
                reply(id)
            }
            "ping" -> {
                app.pingAll()
                reply(id)
            }
            "pingGroup" -> {
                app.pingGroup(text)
                reply(id)
            }
            "add" -> host.lifecycleScope.launch {
                val before = app.servers.size
                val error = app.importText(text)
                val out = JSONObject().put("added", maxOf(0, app.servers.size - before))
                if (error != null) out.put("error", error)
                reply(id, out)
            }
            "refreshGroup" -> host.lifecycleScope.launch {
                val error = app.refreshGroup(text)
                val out = JSONObject()
                if (error != null) out.put("error", error)
                reply(id, out)
            }
            "clipboard" -> reply(id, JSONObject().put("text", clipboard()))
            "haptic" -> {
                host.window.decorView.performHapticFeedback(
                    if (text == "strong") HapticFeedbackConstants.LONG_PRESS else HapticFeedbackConstants.VIRTUAL_KEY
                )
                reply(id)
            }
            "open" -> {
                try {
                    host.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(text)))
                } catch (e: Throwable) {
                }
                reply(id)
            }
            "switchUi" -> {
                UiEngine.switchTo(host, text)
                reply(id)
            }
            "set" -> {
                val o = value as JSONObject
                setting(o.getString("path"), o.opt("value"))
                reply(id)
                push()
            }
            else -> reply(id)
        }
    }

    private fun clipboard(): String {
        val cm = host.getSystemService(ClipboardManager::class.java)
        val clip = cm.primaryClip ?: return ""
        if (clip.itemCount == 0) return ""
        return clip.getItemAt(0).coerceToText(host)?.toString() ?: ""
    }

    private fun int(v: Any?): Int = (v as? Number)?.toInt() ?: 0
    private fun bool(v: Any?): Boolean = v as? Boolean ?: false

    private fun setting(path: String, v: Any?) {
        val st = app
        when (path) {
            "theme" -> st.setTheme(int(v))
            "dynamic" -> st.setDynamic(bool(v))
            "amoled" -> st.changeAmoled(bool(v))
            "autoConnect" -> st.changeAutoConnect(bool(v))
            "dns" -> st.changeDnsPreset(int(v))
            "dnsCustom" -> st.changeDnsCustom(v as? String ?: "")
            "mtu" -> st.changeMtu(int(v))
            "pingMode" -> st.changePingMode(int(v))
            "pingTimeout" -> st.changePingTimeout(int(v))
            "autoPing" -> st.changeAutoPing(bool(v))
            "sortPing" -> st.changeSortPing(bool(v))
            else -> {
                if (path.startsWith("tuning.")) {
                    val json = JSONObject(st.tuning.toJson())
                    json.put(path.removePrefix("tuning."), v ?: JSONObject.NULL)
                    st.changeTuning(Tuning.fromJson(json.toString()))
                } else {
                    val root = blob()
                    val keys = path.split('.')
                    var node = root
                    for (i in 0 until keys.size - 1) {
                        val next = node.optJSONObject(keys[i]) ?: JSONObject()
                        node.put(keys[i], next)
                        node = next
                    }
                    node.put(keys.last(), v ?: JSONObject.NULL)
                    prefs.edit().putString("web_ui", root.toString()).apply()
                }
            }
        }
    }

    private fun blob(): JSONObject = try {
        JSONObject(prefs.getString("web_ui", "{}") ?: "{}")
    } catch (e: Throwable) {
        JSONObject()
    }

    private fun protoOf(conf: String): String {
        if (conf.contains("[Interface]", ignoreCase = true)) return "WireGuard"
        return when (val scheme = conf.substringBefore("://", "").lowercase()) {
            "vless" -> "VLESS"
            "vmess" -> "VMess"
            "trojan" -> "Trojan"
            "ss" -> "Shadowsocks"
            "hysteria2", "hy2" -> "Hysteria2"
            "hysteria" -> "Hysteria"
            "tuic" -> "TUIC"
            "socks", "socks5" -> "SOCKS"
            "" -> "Профиль"
            else -> scheme.uppercase()
        }
    }

    private fun systemColors(): JSONArray {
        val out = JSONArray()
        if (Build.VERSION.SDK_INT < 31) return out
        val dark = (host.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val names = if (dark) {
            listOf(
                "system_accent1_200", "system_accent1_800", "system_accent1_700", "system_accent1_100",
                "system_accent2_700", "system_accent2_100", "system_accent3_200", "system_neutral1_900"
            )
        } else {
            listOf(
                "system_accent1_600", "system_accent1_0", "system_accent1_100", "system_accent1_900",
                "system_accent2_100", "system_accent2_900", "system_accent3_600", "system_neutral1_10"
            )
        }
        val r = android.R.color::class.java
        for (n in names) {
            val c = try {
                host.getColor(r.getField(n).getInt(null))
            } catch (e: Throwable) {
                0
            }
            out.put(c and 0xFFFFFF)
        }
        return out
    }

    private fun snapshot(): String {
        val st = app
        val o = JSONObject()
        o.put("status", st.status.name)
        o.put("selected", st.selectedId ?: JSONObject.NULL)
        o.put("rx", st.rx)
        o.put("tx", st.tx)
        o.put("down", st.downSpeed)
        o.put("up", st.upSpeed)
        o.put("uptime", st.uptime)
        o.put("error", st.error ?: JSONObject.NULL)
        o.put("dark", (host.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES)
        o.put("colors", systemColors())
        val servers = JSONArray()
        for (s in st.servers) {
            servers.put(
                JSONObject()
                    .put("id", s.id)
                    .put("name", s.name)
                    .put("proto", protoOf(s.conf))
                    .put("endpoint", s.endpoint)
                    .put("group", s.group)
            )
        }
        o.put("servers", servers)
        val groups = JSONArray()
        for (g in st.groups) groups.put(JSONObject().put("name", g.name).put("url", g.url))
        o.put("groups", groups)
        val pings = JSONObject()
        for (s in st.servers) {
            val p = st.pings[s.id]
            val best = p?.best() ?: Int.MAX_VALUE
            pings.put(
                s.id,
                JSONObject()
                    .put("ms", if (best == Int.MAX_VALUE || best <= 0) JSONObject.NULL else best)
                    .put("busy", p?.busy ?: false)
            )
        }
        o.put("pings", pings)
        val settings = blob()
        settings.put("theme", st.themeMode)
        settings.put("dynamic", st.dynamicColors)
        settings.put("amoled", st.amoled)
        settings.put("autoConnect", st.autoConnect)
        settings.put("dns", st.dnsPreset)
        settings.put("dnsCustom", st.dnsCustom)
        settings.put("mtu", st.mtu)
        settings.put("pingMode", st.pingMode)
        settings.put("pingTimeout", st.pingTimeout)
        settings.put("autoPing", st.autoPing)
        settings.put("sortPing", st.sortPing)
        settings.put("tuning", JSONObject(st.tuning.toJson()))
        o.put("settings", settings)
        return o.toString()
    }
}
