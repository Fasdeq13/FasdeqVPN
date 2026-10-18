package app.fasdeq.vpn

import android.app.Activity
import android.content.Context
import android.content.Intent
import java.io.File

object UiEngine {
    const val COMPOSE = "compose"
    const val WEB = "web"

    private const val PREFS = "fasdeq"
    private const val KEY = "ui_engine"
    private const val PROBE = "ui_probe"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun marker(context: Context) = File(context.filesDir, "ui_ok")

    fun engine(context: Context): String =
        if (prefs(context).getString(KEY, WEB) == COMPOSE) COMPOSE else WEB

    fun setEngine(context: Context, value: String) {
        prefs(context).edit().putString(KEY, value).commit()
    }

    fun armProbe(context: Context) {
        if (marker(context).exists()) return
        prefs(context).edit().putBoolean(PROBE, true).commit()
    }

    fun markReady(context: Context) {
        try {
            marker(context).writeText("1")
        } catch (e: Throwable) {
        }
        prefs(context).edit().putBoolean(PROBE, false).commit()
    }

    fun failedLast(context: Context): Boolean {
        if (engine(context) != WEB) return false
        return prefs(context).getBoolean(PROBE, false) && !marker(context).exists()
    }

    fun fallBackToCompose(context: Context) {
        setEngine(context, COMPOSE)
        prefs(context).edit().putBoolean(PROBE, false).commit()
    }

    fun switchTo(activity: Activity, mode: String) {
        val target = if (mode == COMPOSE) COMPOSE else WEB
        setEngine(activity, target)
        if (target == WEB) {
            marker(activity).delete()
            armProbe(activity)
        }
        val cls = if (target == WEB) WebUiActivity::class.java else MainActivity::class.java
        activity.startActivity(
            Intent(activity, cls).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
        activity.finish()
    }
}
