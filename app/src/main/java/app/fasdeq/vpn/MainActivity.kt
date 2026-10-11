package app.fasdeq.vpn

import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import app.fasdeq.vpn.ui.FasdeqRoot

class MainActivity : ComponentActivity() {

    private val vpnPermission = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == RESULT_OK) FasdeqApp.state.connect()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FasdeqRoot(FasdeqApp.state, ::onPower)
        }
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
                if (intent != null) vpnPermission.launch(intent) else st.connect()
            }
            Status.On -> st.disconnect()
            else -> Unit
        }
    }
}
