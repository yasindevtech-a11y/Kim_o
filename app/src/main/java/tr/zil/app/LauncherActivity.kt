package tr.zil.app

import android.app.Activity
import android.app.ActivityManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

/**
 * Uygulamanın giriş kapısı. Sade tutuldu, özel kod kullanmaz.
 * Önceki açılış tamamlanmadan kapandıysa (çökme) ana ekran yerine
 * hatanın ne olduğunu gösterir.
 */
class LauncherActivity : Activity() {

    private fun prefs() = getSharedPreferences("zil_boot", Context.MODE_PRIVATE)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val crash = CrashLog.file(this)
        val failedBefore = prefs().getBoolean("starting", false)
        val crashText = try {
            if (crash.exists()) crash.readText() else ""
        } catch (_: Throwable) {
            ""
        }

        if (crashText.isEmpty() && !failedBefore) {
            openMain()
            return
        }
        showReport(crashText)
    }

    private fun openMain() {
        prefs().edit().putBoolean("starting", true).apply()
        val go = Intent(this, MainActivity::class.java)
        // kimo://add#… ile açıldıysa (aile / yedek kodu), bağlantıyı ana ekrana ilet
        val link = intent?.data
        if (intent?.action == Intent.ACTION_VIEW && link != null) {
            go.action = Intent.ACTION_VIEW
            go.data = link
            go.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(go)
        finish()
    }

    private fun exitInfo(): String {
        if (Build.VERSION.SDK_INT < 30) return ""
        return try {
            val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val sb = StringBuilder("\nSon kapanma kayıtları:\n")
            for (r in am.getHistoricalProcessExitReasons(packageName, 0, 4)) {
                sb.append("- işlem=${r.processName} neden=${r.reason} durum=${r.status} açıklama=${r.description}\n")
            }
            sb.toString()
        } catch (_: Throwable) {
            ""
        }
    }

    private fun showReport(crashText: String) {
        val head = "Kim_O ${BuildConfig.VERSION_NAME} • Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}) • ${Build.MANUFACTURER} ${Build.MODEL}\n"
        val text = head +
            (if (crashText.isNotEmpty()) "\n$crashText" else "\n(Hata dosyası yok: uygulama Java hatası vermeden kapanmış.)\n") +
            exitInfo()

        val d = resources.displayMetrics.density
        val pad = (16 * d).toInt()
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(Color.WHITE)
        root.setPadding(pad, pad * 2, pad, pad)

        val h = TextView(this)
        h.text = "Kim_O geçen sefer açılamadı"
        h.textSize = 20f
        h.setTextColor(Color.parseColor("#C62828"))
        h.setTypeface(null, Typeface.BOLD)
        root.addView(h)

        val info = TextView(this)
        info.text = "Aşağıdaki yazıyı Kopyala'ya basıp bana gönderin, hemen düzeltelim."
        info.textSize = 14f
        info.setTextColor(Color.BLACK)
        info.setPadding(0, pad / 2, 0, pad / 2)
        root.addView(info)

        val body = TextView(this)
        body.text = text
        body.textSize = 11f
        body.setTextColor(Color.BLACK)
        body.typeface = Typeface.MONOSPACE
        body.setTextIsSelectable(true)
        val sv = ScrollView(this)
        sv.addView(body)
        root.addView(sv, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

        fun add(label: String, onClick: () -> Unit) {
            val b = Button(this)
            b.text = label
            b.setOnClickListener { onClick() }
            root.addView(b)
        }

        add("Kopyala") {
            try {
                val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("hata", text))
                Toast.makeText(this, "Kopyalandı", Toast.LENGTH_SHORT).show()
            } catch (_: Throwable) {
            }
        }
        add("WhatsApp / paylaş") {
            try {
                val i = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
                startActivity(Intent.createChooser(i, "Gönder"))
            } catch (_: Throwable) {
            }
        }
        add("Temizle ve uygulamayı yine de aç") {
            try {
                CrashLog.file(this).delete()
            } catch (_: Throwable) {
            }
            openMain()
        }

        setContentView(root)
    }
}
