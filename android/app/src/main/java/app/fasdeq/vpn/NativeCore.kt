package app.fasdeq.vpn

import org.json.JSONObject

object NativeCore {
    private val loaded: Boolean = try {
        Class.forName("app.fasdeq.vpn.Native")
        true
    } catch (e: Throwable) {
        false
    }

    fun call(block: () -> String): JSONObject? {
        if (!loaded) return null
        return try {
            val out = JSONObject(block())
            if (out.optBoolean("ok", true)) out else null
        } catch (e: Throwable) {
            null
        }
    }

    fun ping(conf: String, endpoint: String, mode: Int, timeout: Int): PingResult? {
        if (!loaded) return null
        return try {
            val o = JSONObject(Native.ping(conf, endpoint, mode, timeout.toLong()))
            PingResult(
                tcp = if (o.isNull("tcp")) null else o.getInt("tcp"),
                udp = if (o.isNull("udp")) null else o.getInt("udp"),
                icmp = if (o.isNull("icmp")) null else o.getInt("icmp")
            )
        } catch (e: Throwable) {
            null
        }
    }
}
