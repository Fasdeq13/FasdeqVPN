package app.fasdeq.vpn

import java.net.DatagramPacket
import java.util.concurrent.TimeUnit
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.PortUnreachableException
import java.net.Socket
import java.net.SocketTimeoutException
import java.util.Random

data class PingResult(
    val tcp: Int? = null,
    val udp: Int? = null,
    val busy: Boolean = false,
    val icmp: Int? = null
) {
    fun best(): Int {
        val values = listOfNotNull(tcp, udp, icmp).filter { it > 0 }
        return values.minOrNull() ?: Int.MAX_VALUE
    }
}

object Ping {
    const val FAIL = -1
    const val SILENT = -2

    fun target(endpoint: String): Pair<String, Int>? {
        val e = endpoint.trim()
        if (e.isEmpty()) return null
        val host: String
        val port: Int
        if (e.startsWith("[")) {
            val end = e.indexOf(']')
            if (end < 0) return null
            host = e.substring(1, end)
            port = e.substring(end + 1).removePrefix(":").toIntOrNull() ?: return null
        } else {
            val c = e.lastIndexOf(':')
            if (c <= 0) return null
            host = e.substring(0, c)
            port = e.substring(c + 1).toIntOrNull() ?: return null
        }
        return if (host.isEmpty() || port !in 1..65535) null else Pair(host, port)
    }

    fun tcp(host: String, port: Int, timeout: Int): Int {
        return try {
            val addr = InetAddress.getByName(host)
            val socket = Socket()
            try {
                val t0 = System.nanoTime()
                socket.connect(InetSocketAddress(addr, port), timeout)
                ((System.nanoTime() - t0) / 1_000_000L).toInt().coerceAtLeast(1)
            } finally {
                try {
                    socket.close()
                } catch (e: Exception) {
                }
            }
        } catch (e: Exception) {
            FAIL
        }
    }

    fun udp(host: String, port: Int, timeout: Int): Int {
        return try {
            val addr = InetAddress.getByName(host)
            val socket = DatagramSocket()
            try {
                socket.soTimeout = timeout
                socket.connect(addr, port)
                val payload = ByteArray(16)
                Random().nextBytes(payload)
                val t0 = System.nanoTime()
                socket.send(DatagramPacket(payload, payload.size))
                try {
                    socket.receive(DatagramPacket(ByteArray(512), 512))
                    ((System.nanoTime() - t0) / 1_000_000L).toInt().coerceAtLeast(1)
                } catch (e: PortUnreachableException) {
                    ((System.nanoTime() - t0) / 1_000_000L).toInt().coerceAtLeast(1)
                } catch (e: SocketTimeoutException) {
                    SILENT
                }
            } finally {
                socket.close()
            }
        } catch (e: Exception) {
            FAIL
        }
    }

    fun icmp(host: String, timeout: Int): Int {
        return try {
            val seconds = (timeout / 1000).coerceAtLeast(1)
            val process = ProcessBuilder("/system/bin/ping", "-c", "1", "-W", seconds.toString(), host)
                .redirectErrorStream(true)
                .start()
            val out = StringBuilder()
            val reader = Thread {
                try {
                    out.append(process.inputStream.bufferedReader().readText())
                } catch (e: Exception) {
                }
            }
            reader.start()
            if (!process.waitFor(seconds + 2L, TimeUnit.SECONDS)) process.destroyForcibly()
            reader.join(500)
            val m = Regex("time[=<]\\s*([0-9.]+)").find(out.toString())
            if (m == null) FAIL else m.groupValues[1].toDouble().toInt().coerceAtLeast(1)
        } catch (e: Exception) {
            FAIL
        }
    }

    fun measure(server: Server, mode: Int, timeout: Int): PingResult {
        val t = target(server.endpoint) ?: return PingResult(tcp = FAIL)
        val udpNative = server.conf.contains("[Interface]", ignoreCase = true)
        return when (mode) {
            1 -> PingResult(tcp = tcp(t.first, t.second, timeout))
            2 -> {
                val u = udp(t.first, t.second, timeout)
                if (u == SILENT) {
                    val i = icmp(t.first, timeout)
                    if (i > 0) PingResult(icmp = i) else PingResult(udp = u)
                } else PingResult(udp = u)
            }
            3 -> PingResult(
                tcp = tcp(t.first, t.second, timeout),
                udp = udp(t.first, t.second, timeout)
            )
            4 -> PingResult(icmp = icmp(t.first, timeout))
            else -> if (udpNative) {
                val i = icmp(t.first, timeout)
                if (i > 0) PingResult(icmp = i) else PingResult(udp = udp(t.first, t.second, timeout))
            } else PingResult(tcp = tcp(t.first, t.second, timeout))
        }
    }
}
