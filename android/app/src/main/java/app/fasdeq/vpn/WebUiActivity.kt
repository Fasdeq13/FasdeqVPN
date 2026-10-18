package app.fasdeq.vpn

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.snapshotFlow
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import androidx.webkit.WebViewAssetLoader
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

class WebUiActivity : ComponentActivity() {
    private lateinit var web: WebView
    private lateinit var bridge: WebBridge
    private var topInset = 0
    private var bottomInset = 0

    private val vpnPermission = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) {
            askBattery()
            FasdeqApp.state.connect()
        }
    }

    private val batteryAsk = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { }

    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    @SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        askNotifications()
        if (savedInstanceState == null) {
            val st = FasdeqApp.state
            if (st.autoConnect && st.status == Status.Off && st.selected != null && VpnService.prepare(this) == null) {
                st.connect()
            }
        }

        val loader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()

        web = WebView(this)
        web.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        web.setBackgroundColor(Color.parseColor("#101414"))
        web.overScrollMode = WebView.OVER_SCROLL_NEVER
        web.isVerticalScrollBarEnabled = false
        web.isHorizontalScrollBarEnabled = false
        web.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = false
            allowContentAccess = false
            mediaPlaybackRequiresUserGesture = true
            cacheMode = WebSettings.LOAD_NO_CACHE
            textZoom = 100
        }
        WebView.setWebContentsDebuggingEnabled(true)

        bridge = WebBridge(this)
        web.addJavascriptInterface(bridge, "FasdeqNative")
        web.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? =
                loader.shouldInterceptRequest(request.url)

            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                if (request.url.host == "appassets.androidplatform.net") return false
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, request.url))
                } catch (e: Throwable) {
                }
                return true
            }

            override fun onPageFinished(view: WebView, url: String) {
                applyInsets()
            }
        }
        setContentView(web)

        ViewCompat.setOnApplyWindowInsetsListener(web) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            val density = resources.displayMetrics.density
            topInset = (bars.top / density).toInt()
            bottomInset = (bars.bottom / density).toInt()
            applyInsets()
            insets
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                web.evaluateJavascript("(window.__back ? window.__back() : false)") { result ->
                    if (result != "true") finish()
                }
            }
        })

        web.loadUrl("https://appassets.androidplatform.net/assets/web/index.html")

        lifecycleScope.launch {
            snapshotFlow { FasdeqApp.state.let { s ->
                listOf(
                    s.status, s.selectedId, s.rx, s.tx, s.downSpeed, s.upSpeed, s.uptime, s.error,
                    s.servers.toList(), s.groups.toList(), s.pings.toMap(),
                    s.themeMode, s.dynamicColors, s.amoled, s.autoConnect, s.dnsPreset, s.dnsCustom, s.mtu,
                    s.pingMode, s.pingTimeout, s.autoPing, s.sortPing, s.tuning
                ).hashCode()
            } }
                .distinctUntilChanged()
                .conflate()
                .collect {
                    bridge.push()
                    delay(60)
                }
        }
    }

    private fun applyInsets() {
        runJs(
            "document.documentElement.style.setProperty('--safe-top','" + topInset + "px');" +
                "document.documentElement.style.setProperty('--safe-bottom','" + bottomInset + "px');"
        )
    }

    fun runJs(code: String) {
        runOnUiThread {
            if (::web.isInitialized) web.evaluateJavascript(code, null)
        }
    }

    fun applyBars(dark: Boolean) {
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.isAppearanceLightStatusBars = !dark
        controller.isAppearanceLightNavigationBars = !dark
        web.setBackgroundColor(if (dark) Color.parseColor("#101414") else Color.parseColor("#F5FAFA"))
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        if (::bridge.isInitialized) bridge.push()
    }

    override fun onResume() {
        super.onResume()
        FasdeqApp.state.reloadShared()
        FasdeqApp.state.syncExternal()
        if (::bridge.isInitialized) bridge.push()
    }

    override fun onDestroy() {
        if (::web.isInitialized) {
            (web.parent as? ViewGroup)?.removeView(web)
            web.destroy()
        }
        super.onDestroy()
    }

    fun requestConnect() {
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
            else -> Unit
        }
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
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) return
        prefs.edit().putBoolean("asked_notifications", true).apply()
        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
