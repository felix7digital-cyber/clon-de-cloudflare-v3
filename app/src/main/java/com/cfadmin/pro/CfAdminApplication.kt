package com.cfadmin.pro

import android.app.Application
import android.content.Context
import android.util.Log
import com.cfadmin.pro.data.AppContainer
import java.io.PrintWriter
import java.io.StringWriter

class CfAdminApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        installCrashHandler(this)
        container = AppContainer(this)
    }

    private fun installCrashHandler(ctx: Context) {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val sw = StringWriter()
                throwable.printStackTrace(PrintWriter(sw))
                val stack = sw.toString()
                ctx.getSharedPreferences("cf_crash_logs", MODE_PRIVATE)
                    .edit()
                    .putString("last_crash", stack)
                    .putLong("last_crash_time", System.currentTimeMillis())
                    .apply()
                Log.e("CfAdmin", "CRASH: $stack")
            } catch (e: Throwable) {
                // no-op
            }
            previous?.uncaughtException(thread, throwable)
        }
    }
}
