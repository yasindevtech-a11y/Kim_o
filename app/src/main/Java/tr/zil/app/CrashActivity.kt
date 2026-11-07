package tr.zil.app

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

/**
 * Uygulama çökünce ayrı bir işlemde açılır. Bilerek çok basit tutuldu:
 * özel çizim, ortak kod yok, böylece kendisi çökmez.
 */
class CrashActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val text = intent?.getStringExtra("text") ?: "(hata metni alınamadı)"
        val d = resources.displayMetrics.density
        val pad = (16 * d).toInt()

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(Color.WHITE)
        root.setPadding(pad, pad * 2, pad, pad)

        val h = TextView(this)
        h.text = "Kim_O bir hata verdi"
        h.textSize = 20f
        h.setTextColor(Color.parseColor("#C62828"))
        h.setTypeface(null, Typeface.BOLD)
        root.addView(h)

        val info = TextView(this)
        info.text = "Aşağıdaki yazıyı gönderin (Kopyala ya da WhatsApp düğmesi), hemen düzeltelim."
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

        val copy = Button(this)
        copy.text = "Kopyala"
        copy.setOnClickListener {
            try {
                val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("hata", text))
                Toast.makeText(this, "Kopyalandı", Toast.LENGTH_SHORT).show()
            } catch (_: Throwable) {
            }
        }
        root.addView(copy)

        val send = Button(this)
        send.text = "WhatsApp / paylaş"
        send.setOnClickListener {
            try {
                val i = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
                startActivity(Intent.createChooser(i, "Gönder"))
            } catch (_: Throwable) {
            }
        }
        root.addView(send)

        val close = Button(this)
        close.text = "Kapat"
        close.setOnClickListener { finishAndRemoveTask() }
        root.addView(close)

        setContentView(root)
    }
}
