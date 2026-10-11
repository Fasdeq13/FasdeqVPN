package app.fasdeq.vpn

import android.app.Application
import libv2ray.Libv2ray
import java.security.SecureRandom
import java.util.Base64

class FasdeqApp : Application() {
    override fun onCreate() {
        super.onCreate()
        state = AppState(this)
        try {
            val prefs = getSharedPreferences("fasdeq", MODE_PRIVATE)
            var key = prefs.getString("core_key", null)
            if (key == null) {
                val bytes = ByteArray(32)
                SecureRandom().nextBytes(bytes)
                key = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
                prefs.edit().putString("core_key", key).apply()
            }
            Libv2ray.initCoreEnv(filesDir.absolutePath, key)
        } catch (e: Throwable) {
        }
    }

    companion object {
        lateinit var state: AppState
    }
}
