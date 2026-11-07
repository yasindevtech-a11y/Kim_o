package tr.zil.app

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.NotificationManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : Activity() {

    private val ORANGE = Color.parseColor("#FF6D00")
    private val ORANGE_D = Color.parseColor("#E65100")
    private val TEXT = Color.parseColor("#2B1B0E")
    private val MUTED = Color.parseColor("#7A6552")
    private val GREEN = Color.parseColor("#2E7D32")
    private val RED = Color.parseColor("#C62828")

    private val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
    private val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT

    private lateinit var warnBox: LinearLayout
    private lateinit var listBox: LinearLayout
    private lateinit var statusView: TextView
    private lateinit var master: Switch

    private val handler = Handler(Looper.getMainLooper())
    private val ticker = object : Runnable {
        override fun run() {
            updateStatus()
            handler.postDelayed(this, 2000)
        }
    }

    // ------------------------------------------------------------ yardımcılar

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun toast(t: String) = Toast.makeText(this, t, Toast.LENGTH_LONG).show()

    private fun bg(color: Int, radius: Int = 16, stroke: Int = 0) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
        if (stroke != 0) setStroke(dp(1), stroke)
    }

    private fun button(text: String, filled: Boolean, onClick: () -> Unit): Button {
        val b = Button(this)
        b.text = text
        b.isAllCaps = false
        b.textSize = 14f
        b.setTextColor(if (filled) Color.WHITE else ORANGE_D)
        b.background = bg(if (filled) ORANGE else Color.WHITE, 12, if (filled) 0 else ORANGE)
        b.stateListAnimator = null
        b.minHeight = 0
        b.minimumHeight = 0
        b.setPadding(dp(14), dp(10), dp(14), dp(10))
        b.setOnClickListener { onClick() }
        return b
    }

    private fun linkText(text: String, color: Int, onClick: () -> Unit): TextView {
        val t = TextView(this)
        t.text = text
        t.textSize = 15f
        t.setTextColor(color)
        t.setPadding(0, dp(14), 0, dp(6))
        t.setOnClickListener { onClick() }
        return t
    }

    private fun confirm(title: String, msg: String, onYes: () -> Unit) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(msg)
            .setPositiveButton("Evet") { _, _ -> onYes() }
            .setNegativeButton("Vazgeç", null)
            .show()
    }

    // ------------------------------------------------------------ ekran

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
        render()
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
        if (Store.enabled(this) && Store.bells(this).isNotEmpty()) ServiceCtl.start(this)
    }

    override fun onResume() {
        super.onResume()
        refreshWarnings()
        handler.post(ticker)
    }

    override fun onPause() {
        handler.removeCallbacks(ticker)
        super.onPause()
    }

    private fun buildUi() {
        val scroll = ScrollView(this)
        scroll.isFillViewport = true
        scroll.fitsSystemWindows = true

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(dp(16), dp(12), dp(16), dp(32))
        scroll.addView(root, ViewGroup.LayoutParams(MATCH, WRAP))

        // Başlık satırı
        val head = LinearLayout(this)
        head.orientation = LinearLayout.HORIZONTAL
        head.gravity = Gravity.CENTER_VERTICAL
        val title = TextView(this)
        title.text = "🔔 Kim_O"
        title.textSize = 28f
        title.setTypeface(null, Typeface.BOLD)
        title.setTextColor(ORANGE_D)
        head.addView(title, LinearLayout.LayoutParams(0, WRAP, 1f))
        head.addView(button("Ayarlar", false) { settingsDialog() })
        root.addView(head)

        // Ana anahtar kartı
        val card = LinearLayout(this)
        card.orientation = LinearLayout.HORIZONTAL
        card.gravity = Gravity.CENTER_VERTICAL
        card.background = bg(Color.WHITE)
        card.setPadding(dp(16), dp(14), dp(16), dp(14))
        val left = LinearLayout(this)
        left.orientation = LinearLayout.VERTICAL
        val t1 = TextView(this)
        t1.text = "Zilleri dinle"
        t1.textSize = 18f
        t1.setTypeface(null, Typeface.BOLD)
        t1.setTextColor(TEXT)
        statusView = TextView(this)
        statusView.textSize = 14f
        statusView.setTextColor(MUTED)
        left.addView(t1)
        left.addView(statusView)
        card.addView(left, LinearLayout.LayoutParams(0, WRAP, 1f))
        master = Switch(this)
        card.addView(master)
        root.addView(card, LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(14) })

        warnBox = LinearLayout(this)
        warnBox.orientation = LinearLayout.VERTICAL
        root.addView(warnBox, LinearLayout.LayoutParams(MATCH, WRAP))

        listBox = LinearLayout(this)
        listBox.orientation = LinearLayout.VERTICAL
        root.addView(listBox, LinearLayout.LayoutParams(MATCH, WRAP))

        val add = button("+  Yeni zil ekle", true) { editDialog(null) }
        add.textSize = 16f
        root.addView(add, LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(18) })

        setContentView(scroll)
    }

    private fun render() {
        refreshWarnings()

        master.setOnCheckedChangeListener(null)
        master.isChecked = Store.enabled(this)
        master.setOnCheckedChangeListener { _, on ->
            Store.setEnabled(this, on)
            if (on) ServiceCtl.start(this) else ServiceCtl.stop(this)
            updateStatus()
        }

        listBox.removeAllViews()
        val bells = Store.bells(this)
        if (bells.isEmpty()) {
            val empty = TextView(this)
            empty.text = "Henüz zil yok.\nEv kapısı, bahçe, araba… istediğiniz kadar zil ekleyin; her birinin ayrı QR kodu olur."
            empty.textSize = 15f
            empty.setTextColor(MUTED)
            empty.setPadding(dp(4), dp(24), dp(4), dp(8))
            listBox.addView(empty)
        }
        for (b in bells) listBox.addView(bellCard(b))
        updateStatus()
    }

    private fun updateStatus() {
        val n = Store.bells(this).count { it.enabled }
        when {
            !Store.enabled(this) -> {
                statusView.text = "Kapalı — zil çalmaz"
                statusView.setTextColor(RED)
            }
            n == 0 -> {
                statusView.text = "Aktif zil yok"
                statusView.setTextColor(MUTED)
            }
            ListenService.connected -> {
                statusView.text = "● Dinleniyor • $n zil aktif"
                statusView.setTextColor(GREEN)
            }
            else -> {
                statusView.text = "○ Bağlanıyor… • $n zil aktif"
                statusView.setTextColor(MUTED)
            }
        }
    }

    private fun refreshWarnings() {
        warnBox.removeAllViews()

        if (Store.pageUrl(this).isBlank()) {
            warn("Önce ziyaretçi sayfasının adresini girin. QR kodlar bu adrese göre oluşur.", "Ayarlar") {
                settingsDialog()
            }
        }
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            warn("Bildirim izni kapalı. Uyarı ekranı görünmez.", "İzin ver") {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
            }
        }
        val pm = getSystemService(PowerManager::class.java)
        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            warn("Pil tasarrufu uygulamayı uyutabilir. Zilin her zaman çalışması için kısıtlamayı kaldırın.", "Kaldır") {
                startActivity(
                    Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$packageName"))
                )
            }
        }
        if (Build.VERSION.SDK_INT >= 34) {
            val nm = getSystemService(NotificationManager::class.java)
            if (!nm.canUseFullScreenIntent()) {
                warn("Kilit ekranında tam ekran uyarı kapalı.", "Aç") {
                    startActivity(
                        Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.parse("package:$packageName"))
                    )
                }
            }
        }
    }

    private fun warn(text: String, action: String, onClick: () -> Unit) {
        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        box.background = bg(Color.parseColor("#FFF3E0"), 14, Color.parseColor("#FFB74D"))
        box.setPadding(dp(14), dp(12), dp(14), dp(12))
        val t = TextView(this)
        t.text = text
        t.textSize = 14f
        t.setTextColor(TEXT)
        box.addView(t)
        val b = button(action, true, onClick)
        box.addView(b, LinearLayout.LayoutParams(WRAP, WRAP).apply { topMargin = dp(8) })
        warnBox.addView(box, LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(10) })
    }

    // ------------------------------------------------------------ zil kartı

    private fun bellCard(b: Bell): LinearLayout {
        val card = LinearLayout(this)
        card.orientation = LinearLayout.VERTICAL
        card.background = bg(Color.WHITE)
        card.setPadding(dp(16), dp(14), dp(16), dp(14))
        card.layoutParams = LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(12) }

        val top = LinearLayout(this)
        top.orientation = LinearLayout.HORIZONTAL
        top.gravity = Gravity.CENTER_VERTICAL
        val name = TextView(this)
        name.text = b.name
        name.textSize = 19f
        name.setTypeface(null, Typeface.BOLD)
        name.setTextColor(TEXT)
        top.addView(name, LinearLayout.LayoutParams(0, WRAP, 1f))
        val sw = Switch(this)
        sw.isChecked = b.enabled
        sw.setOnCheckedChangeListener { _, on ->
            val all = Store.bells(this)
            all.firstOrNull { it.id == b.id }?.enabled = on
            Store.saveBells(this, all)
            ServiceCtl.reload(this)
            updateStatus()
        }
        top.addView(sw)
        card.addView(top)

        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        val lp = { LinearLayout.LayoutParams(0, WRAP, 1f).apply { setMargins(dp(3), 0, dp(3), 0) } }
        row.addView(button("QR / Paylaş", true) { qrDialog(b) }, lp())
        row.addView(button("Düzenle", false) { editDialog(b) }, lp())
        row.addView(button("Test", false) { testRing(b) }, lp())
        card.addView(row, LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(12) })
        return card
    }

    private fun testRing(b: Bell) {
        if (!Store.enabled(this)) {
            toast("Önce üstteki \"Zilleri dinle\" anahtarını açın")
            return
        }
        val server = Store.server(this)
        Thread {
            val ok = try {
                val c = URL("$server/${b.topic}").openConnection() as HttpURLConnection
                c.requestMethod = "POST"
                c.doOutput = true
                c.connectTimeout = 10_000
                c.readTimeout = 10_000
                c.outputStream.use { it.write("zil".toByteArray()) }
                val code = c.responseCode
                c.disconnect()
                code in 200..299
            } catch (_: Exception) {
                false
            }
            runOnUiThread {
                toast(if (ok) "Test zili gönderildi" else "Gönderilemedi — internet var mı?")
            }
        }.start()
    }

    // ------------------------------------------------------------ düzenle / ekle

    private fun editDialog(existing: Bell?) {
        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        box.setPadding(dp(22), dp(10), dp(22), dp(4))

        val input = EditText(this)
        input.setText(existing?.name ?: "")
        input.hint = "Örn: Ev kapısı, Araba, Depo"
        input.setSingleLine()
        input.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        box.addView(input)

        var dlg: AlertDialog? = null

        if (existing != null) {
            box.addView(linkText("Kodu yenile (eski QR çalışmaz olur)", MUTED) {
                confirm(
                    "Yeni kod üretilsin mi?",
                    "Eski QR kod ve bağlantılar çalışmaz olur. Yeni QR'ı yeniden paylaşmanız gerekir."
                ) {
                    val all = Store.bells(this)
                    all.firstOrNull { it.id == existing.id }?.topic = Store.newTopic()
                    Store.saveBells(this, all)
                    ServiceCtl.reload(this)
                    dlg?.dismiss()
                    render()
                    toast("Yeni kod üretildi. QR'ı yeniden paylaşın.")
                }
            })
            box.addView(linkText("Bu zili sil", RED) {
                confirm("\"${existing.name}\" silinsin mi?", "Bu zilin QR kodu artık çalışmaz.") {
                    val all = Store.bells(this)
                    all.removeAll { it.id == existing.id }
                    Store.saveBells(this, all)
                    ServiceCtl.reload(this)
                    dlg?.dismiss()
                    render()
                }
            })
        }

        val d = AlertDialog.Builder(this)
            .setTitle(if (existing == null) "Yeni zil" else "Zili düzenle")
            .setView(box)
            .setPositiveButton("Kaydet") { _, _ ->
                val nm = input.text.toString().trim()
                if (nm.isEmpty()) {
                    toast("Bir ad yazın")
                } else if (existing == null) {
                    val nb = Store.newBell(nm)
                    val all = Store.bells(this)
                    all.add(nb)
                    Store.saveBells(this, all)
                    ServiceCtl.reload(this)
                    render()
                    qrDialog(nb)
                } else {
                    val all = Store.bells(this)
                    all.firstOrNull { it.id == existing.id }?.name = nm
                    Store.saveBells(this, all)
                    render()
                }
            }
            .setNegativeButton("İptal", null)
            .create()
        dlg = d
        d.show()
    }

    // ------------------------------------------------------------ QR / paylaş

    private fun qrDialog(b: Bell) {
        val link = Store.link(this, b)
        if (link == null) {
            toast("Önce sayfa adresini girin")
            settingsDialog()
            return
        }
        val bmp = try {
            QrCard.make(link, b.name)
        } catch (_: Exception) {
            toast("QR kod üretilemedi")
            return
        }

        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        box.setPadding(dp(18), dp(8), dp(18), dp(4))

        val iv = ImageView(this)
        iv.setImageBitmap(bmp)
        iv.adjustViewBounds = true
        box.addView(iv, LinearLayout.LayoutParams(MATCH, WRAP))

        fun add(text: String, filled: Boolean, onClick: () -> Unit) {
            box.addView(
                button(text, filled, onClick),
                LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(8) }
            )
        }

        add("WhatsApp ile gönder (QR görseli)", true) {
            val f = QrCard.toCacheFile(this, bmp, b.id)
            QrCard.shareImage(this, QrCard.uri(this, f), "${b.name} – zil QR kodu")
        }
        add("WhatsApp ile gönder (bağlantı)", false) {
            QrCard.shareText(this, "🔔 ${b.name}: zile basmak için bağlantıya dokunun\n$link")
        }
        add("Galeriye kaydet (yazdırmak için)", false) {
            toast(
                if (QrCard.saveToGallery(this, bmp, b.name)) "Galeri › Zil klasörüne kaydedildi"
                else "Kaydedilemedi (Android 10+ gerekir). WhatsApp ile gönderin."
            )
        }
        add("Bağlantıyı kopyala", false) {
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("zil", link))
            toast("Kopyalandı")
        }

        val sv = ScrollView(this)
        sv.addView(box)
        AlertDialog.Builder(this)
            .setTitle(b.name)
            .setView(sv)
            .setNegativeButton("Kapat", null)
            .show()
    }

    // ------------------------------------------------------------ ayarlar

    private fun settingsDialog() {
        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        box.setPadding(dp(22), dp(10), dp(22), dp(4))

        fun label(t: String) {
            val v = TextView(this)
            v.text = t
            v.textSize = 13f
            v.setTextColor(MUTED)
            v.setPadding(0, dp(10), 0, 0)
            box.addView(v)
        }

        label("Ziyaretçi sayfası adresi (derlemeden otomatik gelir, genelde değiştirmeyin)")
        val page = EditText(this)
        page.setText(Store.pageUrl(this))
        page.hint = "https://kullanici.github.io/zil/"
        page.setSingleLine()
        page.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
        box.addView(page)

        label("ntfy sunucusu (genelde değiştirmeyin)")
        val srv = EditText(this)
        srv.setText(Store.server(this))
        srv.setSingleLine()
        srv.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
        box.addView(srv)

        val sv = ScrollView(this)
        sv.addView(box)
        AlertDialog.Builder(this)
            .setTitle("Ayarlar")
            .setView(sv)
            .setPositiveButton("Kaydet") { _, _ ->
                val oldServer = Store.server(this)
                Store.setPageUrl(this, page.text.toString())
                Store.setServer(this, srv.text.toString().ifBlank { Store.DEFAULT_SERVER })
                if (Store.server(this) != oldServer) ServiceCtl.reload(this)
                render()
            }
            .setNegativeButton("İptal", null)
            .show()
    }
}
