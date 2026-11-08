package tr.zil.app

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

object CrashLog {
    fun file(c: Context) = File(c.filesDir, "crash.txt")

    fun build(where: String, e: Throwable): String {
        val sw = StringWriter()
        e.printStackTrace(PrintWriter(sw))
        return "Kim_O ${BuildConfig.VERSION_NAME}\n" +
            "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n" +
            "${Build.MANUFACTURER} ${Build.MODEL}\n" +
            "Yer: $where\n\n$sw"
    }

    fun save(c: Context, where: String, e: Throwable) {
        try {
            file(c).writeText(build(where, e))
        } catch (_: Throwable) {
        }
    }
}

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        L.init(this)

        // Hata ekranının kendi işleminde bu kancayı kurma (sonsuz döngüyü önler)
        try {
            if (Build.VERSION.SDK_INT >= 28 && Application.getProcessName().endsWith(":crash")) return
        } catch (_: Throwable) {
        }

        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            try {
                val text = CrashLog.build("thread ${t.name}", e).take(6000)
                CrashLog.save(this, "thread ${t.name}", e)
                startActivity(
                    Intent(this, CrashActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        .putExtra("text", text)
                )
                Thread.sleep(500)
            } catch (_: Throwable) {
            }
            android.os.Process.killProcess(android.os.Process.myPid())
            System.exit(10)
        }
    }
}
