package app.fasdeq.vpn

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import app.fasdeq.vpn.ui.FasdeqRoot

class MainActivity : ComponentActivity() {

    private val vpnPermission = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == RESULT_OK) {
            askBattery()
            FasdeqApp.state.connect()
        }
    }

    private val batteryAsk = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { }

    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (UiEngine.failedLast(this)) {
            UiEngine.fallBackToCompose(this)
            android.widget.Toast.makeText(this, "Новый интерфейс не запустился, включён обычный", android.widget.Toast.LENGTH_LONG).show()
        }
        if (UiEngine.engine(this) == UiEngine.WEB) {
            UiEngine.armProbe(this)
            startActivity(Intent(this, WebUiActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
            finish()
            return
        }
        enableEdgeToEdge()
        askNotifications()
        if (savedInstanceState == null) {
            val st = FasdeqApp.state
            if (st.autoConnect && st.status == Status.Off && st.selected != null && VpnService.prepare(this) == null) {
                st.connect()
            }
        }
        setContent {
            FasdeqRoot(FasdeqApp.state, ::onPower)
        }
    }

    override fun onResume() {
        super.onResume()
        FasdeqApp.state.reloadShared()
        FasdeqApp.state.syncExternal()
    }

    private fun askBattery() {
        val prefs = getSharedPreferences("fasdeq", Context.MODE_PRIVATE)
        if (prefs.getBoolean("asked_battery", false)) return
        val power = getSystemService(PowerManager::class.java)
        if (power.isIgnoringBatteryOptimizations(packageName)) return
        prefs.edit().putBoolean("asked_battery", true).apply()
        try {
            batteryAsk.launch(
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                    .setData(Uri.parse("package:$packageName"))
            )
        } catch (e: Exception) {
        }
    }

    private fun askNotifications() {
        if (Build.VERSION.SDK_INT < 33) return
        val prefs = getSharedPreferences("fasdeq", Context.MODE_PRIVATE)
        if (prefs.getBoolean("asked_notifications", false)) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            == PackageManager.PERMISSION_GRANTED
        ) return
        prefs.edit().putBoolean("asked_notifications", true).apply()
        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun onPower() {
        val st = FasdeqApp.state
        when (st.status) {
            Status.Off -> {
                if (st.selected == null) {
                    st.error = "Сначала добавьте сервер"
                    return
                }
                val intent = VpnService.prepare(this)
                if (intent != null) {
                    vpnPermission.launch(intent)
                } else {
                    askBattery()
                    st.connect()
                }
            }
            Status.On -> st.disconnect()
            else -> Unit
        }
    }
}
