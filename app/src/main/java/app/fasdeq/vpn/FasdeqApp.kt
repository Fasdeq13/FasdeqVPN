package app.fasdeq.vpn

import android.app.Application

class FasdeqApp : Application() {
    override fun onCreate() {
        super.onCreate()
        state = AppState(this)
    }

    companion object {
        lateinit var state: AppState
    }
}
