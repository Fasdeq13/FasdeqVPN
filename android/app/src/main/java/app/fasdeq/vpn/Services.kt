package app.fasdeq.vpn

import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

data class ServiceCheck(
    val ms: List<Int?> = listOf(null, null, null),
    val busy: Boolean = false
) {
    fun with(index: Int, value: Int?): ServiceCheck {
        val list = ms.toMutableList()
        list[index] = value
        return copy(ms = list)
    }
}

object Services {
    val names = listOf("Discord", "Telegram", "YouTube")
    val urls = listOf(
        "https://discord.com/api/gateway",
        "https://telegram.org/",
        "https://www.youtube.com/generate_204"
    )
    const val UNSUPPORTED = -3

    private val pool = Executors.newCachedThreadPool()

    fun probeConfig(outbound: JSONObject): String {
        outbound.put("tag", "proxy")
        return JSONObject()
            .put("log", JSONObject().put("loglevel", "none"))
            .put(
                "outbounds",
                JSONArray()
                    .put(outbound)
                    .put(JSONObject().put("tag", "direct").put("protocol", "freedom"))
            )
            .toString()
    }

    fun delay(config: String, url: String, timeoutSeconds: Long = 12): Int {
        val method = try {
            Class.forName("libv2ray.Libv2ray")
                .getMethod("measureOutboundDelay", String::class.java, String::class.java)
        } catch (e: Throwable) {
            return UNSUPPORTED
        }
        val future = pool.submit(
            Callable {
                try {
                    (method.invoke(null, config, url) as Long).toInt()
                } catch (e: Throwable) {
                    -1
                }
            }
        )
        return try {
            val value = future.get(timeoutSeconds, TimeUnit.SECONDS)
            if (value >= 0) value.coerceAtLeast(1) else -1
        } catch (e: TimeoutException) {
            future.cancel(true)
            -1
        } catch (e: Exception) {
            -1
        }
    }
}
