package tr.zil.app

import android.app.Application
import android.content.Context
import android.os.Build
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

/** Çökme olursa hatayı dosyaya yazar; uygulama bir sonraki açılışta bunu ekranda gösterir. */
object CrashLog {
    fun file(c: Context) = File(c.filesDir, "crash.txt")

    fun save(c: Context, where: String, e: Throwable) {
        try {
            val sw = StringWriter()
            e.printStackTrace(PrintWriter(sw))
            file(c).writeText(
                "Kim_O 1.2\n" +
                    "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n" +
                    "${Build.MANUFACTURER} ${Build.MODEL}\n" +
                    "Yer: $where\n\n$sw"
            )
        } catch (_: Throwable) {
        }
    }
}

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            CrashLog.save(this, "thread ${t.name}", e)
            previous?.uncaughtException(t, e)
        }
    }
}
