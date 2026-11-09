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
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import org.json.JSONObject
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

    private val REQ_RINGTONE = 77
    private val REQ_IMPORT_IMG = 78

    private lateinit var warnBox: LinearLayout
    private lateinit var listBox: LinearLayout
    private lateinit var adBox: LinearLayout
    private lateinit var statusView: TextView
    private lateinit var master: Switch

    // Telefondan zil sesi seçilince sonucu bekleyen işlev
    private var onSoundPicked: ((String) -> Unit)? = null

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
            .setPositiveButton(L.t("Evet")) { _, _ -> onYes() }
            .setNegativeButton(L.t("Vazgeç"), null)
            .show()
    }

    // ------------------------------------------------------------ ekran

    private var ready = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            buildUi()
            render()
            if (Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
            }
            if (Store.enabled(this) && Store.bells(this).isNotEmpty()) ServiceCtl.start(this)
            Ads.setup(this, adBox)
            ready = true
        } catch (e: Throwable) {
            CrashLog.save(this, "MainActivity.onCreate", e)
            throw e
        }
        // Bağlantıyla açıldıysa, ekran çizildikten sonra içe aktarma penceresini göster
        val first = intent
        handler.post { handleIncoming(first) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (ready) handleIncoming(intent)
    }

    /** kimo://add#t=… bağlantısıyla (ör. aile kodu sayfasındaki düğmeyle) açıldıysa zili içe aktarmayı dener. */
    private fun handleIncoming(i: Intent?) {
        if (i == null || i.action != Intent.ACTION_VIEW) return
        val data = i.dataString ?: return
        i.data = null   // ekran dönünce aynı bağlantı yeniden işlenmesin
        handleInvite(data)
    }

    override fun onResume() {
        super.onResume()
        if (!ready) return
        refreshWarnings()
        handler.removeCallbacks(ticker)
        handler.post(ticker)
        try {
            getSharedPreferences("zil_boot", Context.MODE_PRIVATE).edit().putBoolean("starting", false).apply()
        } catch (_: Throwable) {
        }
    }

    override fun onPause() {
        handler.removeCallbacks(ticker)
        Sounds.stopPreview()
        super.onPause()
    }

    override fun onDestroy() {
        handler.removeCallbacks(ticker)
        Sounds.stopPreview()
        if (::adBox.isInitialized) Ads.destroy(adBox)
        super.onDestroy()
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_RINGTONE && resultCode == RESULT_OK) {
            val uri: Uri? = if (Build.VERSION.SDK_INT >= 33) {
                data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            } else {
                data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            }
            if (uri != null) onSoundPicked?.invoke("u:$uri")
        }
        if (requestCode == REQ_IMPORT_IMG && resultCode == RESULT_OK) {
            val uri: Uri? = data?.data
            if (uri == null) {
                toast(L.t("Görselde QR kod okunamadı"))
            } else {
                Thread {
                    val text = QrReader.decode(this, uri)
                    runOnUiThread {
                        if (isFinishing || isDestroyed) return@runOnUiThread
                        if (text == null) toast(L.t("Görselde QR kod okunamadı")) else handleInvite(text)
                    }
                }.start()
            }
        }
    }

    private fun buildUi() {
        val scroll = ScrollView(this)
        scroll.isFillViewport = true

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(dp(16), dp(12), dp(16), dp(24))
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
        head.addView(button(L.t("Ayarlar"), false) { settingsDialog() })
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
        t1.text = L.t("Zilleri dinle")
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

        val add = button(L.t("+  Yeni zil ekle"), true) { editDialog(null) }
        add.textSize = 16f
        root.addView(add, LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(18) })

        val imp = button(L.t("İçe aktar (aile / yedek kodu)"), false) { importDialog() }
        imp.textSize = 15f
        root.addView(imp, LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(10) })

        // Alt kısım: reklam alanı (onay alınmadan ya da hata olursa boş kalır)
        adBox = LinearLayout(this)
        adBox.orientation = LinearLayout.VERTICAL
        adBox.gravity = Gravity.CENTER_HORIZONTAL

        val outer = LinearLayout(this)
        outer.orientation = LinearLayout.VERTICAL
        outer.setBackgroundColor(Color.parseColor("#F5F5F5"))
        outer.fitsSystemWindows = true
        outer.addView(scroll, LinearLayout.LayoutParams(MATCH, 0, 1f))
        outer.addView(adBox, LinearLayout.LayoutParams(MATCH, WRAP))
        setContentView(outer)
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
            empty.text = L.t("Henüz zil yok.\nEv kapısı, bahçe, araba… istediğiniz kadar zil ekleyin; her birinin ayrı QR kodu olur.")
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
                statusView.text = L.t("Kapalı — zil çalmaz")
                statusView.setTextColor(RED)
            }
            n == 0 -> {
                statusView.text = L.t("Aktif zil yok")
                statusView.setTextColor(MUTED)
            }
            ListenService.connected -> {
                statusView.text = L.f("● Dinleniyor • %d zil aktif", n)
                statusView.setTextColor(GREEN)
            }
            else -> {
                statusView.text = L.f("○ Bağlanıyor… • %d zil aktif", n)
                statusView.setTextColor(MUTED)
            }
        }
    }

    private fun refreshWarnings() {
        warnBox.removeAllViews()

        if (Store.pageUrl(this).isBlank()) {
            warn(L.t("Önce ziyaretçi sayfasının adresini girin. QR kodlar bu adrese göre oluşur."), L.t("Ayarlar")) {
                settingsDialog()
            }
        }
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            warn(L.t("Bildirim izni kapalı. Uyarı ekranı görünmez."), L.t("İzin ver")) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
            }
        }
        val pm = getSystemService(PowerManager::class.java)
        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            warn(
                L.t("Pil tasarrufu uygulamayı uyutabilir. Açılan listede Kim_O'yu bulup \"Kısıtlama yok\" ya da \"Optimize etme\" seçin."),
                L.t("Ayarı aç")
            ) {
                try {
                    startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                } catch (_: Throwable) {
                    try {
                        startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
                        )
                    } catch (_: Throwable) {
                    }
                }
            }
        }
        if (Build.VERSION.SDK_INT >= 34) {
            val nm = getSystemService(NotificationManager::class.java)
            if (!nm.canUseFullScreenIntent()) {
                warn(L.t("Kilit ekranında tam ekran uyarı kapalı."), L.t("Aç")) {
                    try {
                        startActivity(
                            Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.parse("package:$packageName"))
                        )
                    } catch (_: Throwable) {
                    }
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

        val nameBox = LinearLayout(this)
        nameBox.orientation = LinearLayout.VERTICAL
        val name = TextView(this)
        name.text = b.name
        name.textSize = 19f
        name.setTypeface(null, Typeface.BOLD)
        name.setTextColor(TEXT)
        nameBox.addView(name)
        val snd = TextView(this)
        snd.text = "🔊 " + Sounds.label(this, b.sound)
        snd.textSize = 13f
        snd.setTextColor(MUTED)
        nameBox.addView(snd)
        top.addView(nameBox, LinearLayout.LayoutParams(0, WRAP, 1f))

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
        row.addView(button(L.t("QR / Paylaş"), true) { qrDialog(b) }, lp())
        row.addView(button(L.t("Düzenle"), false) { editDialog(b) }, lp())
        row.addView(button(L.t("Test"), false) { testRing(b) }, lp())
        card.addView(row, LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(12) })
        return card
    }

    /**
     * Deneme zili, ziyaretçi sayfasıyla aynı yoldan gider: zilin genel anahtarına şifrelenir, uygulama
     * özel anahtarla çözer. Böylece "Test" şifreleme zincirinin tamamını sınar. Anahtar yoksa düz "zil".
     */
    private fun testBody(b: Bell): String {
        if (Crypto.isPub(b.pub)) {
            val plain = JSONObject()
                .put("e", 0)
                .put("n", L.t("Test"))
                .put("l", if (L.isEn()) "en" else "tr")
                .toString()
            val boxed = Crypto.encryptVisitor(b.pub, b.topic, plain)
            if (boxed != null) return boxed
        }
        return "zil"
    }

    private fun testRing(b: Bell) {
        if (!Store.enabled(this)) {
            toast(L.t("Önce üstteki \"Zilleri dinle\" anahtarını açın"))
            return
        }
        val server = Store.server(this)
        val body = testBody(b)
        Thread {
            val ok = try {
                val c = URL("$server/${b.topic}").openConnection() as HttpURLConnection
                c.requestMethod = "POST"
                c.doOutput = true
                c.connectTimeout = 10_000
                c.readTimeout = 10_000
                c.outputStream.use { it.write(body.toByteArray()) }
                val code = c.responseCode
                c.disconnect()
                code in 200..299
            } catch (_: Exception) {
                false
            }
            runOnUiThread {
                toast(if (ok) L.t("Test zili gönderildi") else L.t("Gönderilemedi — internet var mı?"))
            }
        }.start()
    }

    // ------------------------------------------------------------ zil sesi seçimi

    private fun chooseSound(current: String, onChosen: (String) -> Unit) {
        val specs = ArrayList<String>()
        val names = ArrayList<String>()
        for ((code, trName) in Sounds.BUILTIN) {
            specs.add("b:$code")
            names.add(L.t(trName))
        }
        specs.add(Sounds.PHONE)
        names.add(L.t("Telefonun zil sesi"))
        names.add(L.t("Telefondan seç…"))

        var picked = current
        val dlg = AlertDialog.Builder(this)
            .setTitle(L.t("Zil sesi"))
            .setSingleChoiceItems(names.toTypedArray(), specs.indexOf(current)) { d, which ->
                if (which < specs.size) {
                    picked = specs[which]
                    Sounds.preview(this, picked)
                } else {
                    Sounds.stopPreview()
                    onSoundPicked = { spec -> onChosen(spec) }
                    val i = Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
                    i.putExtra(
                        RingtoneManager.EXTRA_RINGTONE_TYPE,
                        RingtoneManager.TYPE_RINGTONE or RingtoneManager.TYPE_NOTIFICATION or RingtoneManager.TYPE_ALARM
                    )
                    i.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, false)
                    i.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                    try {
                        @Suppress("DEPRECATION")
                        startActivityForResult(i, REQ_RINGTONE)
                    } catch (_: Throwable) {
                        toast(L.t("Bu telefonda ses seçici açılamadı"))
                    }
                    d.dismiss()
                }
            }
            .setPositiveButton(L.t("Tamam")) { _, _ -> onChosen(picked) }
            .setNegativeButton(L.t("İptal"), null)
            .create()
        dlg.setOnDismissListener { Sounds.stopPreview() }
        dlg.show()
    }

    // ------------------------------------------------------------ düzenle / ekle

    private fun editDialog(existing: Bell?) {
        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        box.setPadding(dp(22), dp(10), dp(22), dp(4))

        val input = EditText(this)
        input.setText(existing?.name ?: "")
        input.hint = L.t("Örn: Ev kapısı, Araba, Depo")
        input.setSingleLine()
        input.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        box.addView(input)

        // Zil sesi
        var sound = existing?.sound ?: Sounds.DEFAULT
        val soundText = TextView(this)
        soundText.textSize = 14f
        soundText.setTextColor(TEXT)
        soundText.setPadding(0, dp(16), 0, dp(6))
        fun refreshSound() {
            soundText.text = "🔊 " + L.t("Zil sesi") + ": " + Sounds.label(this, sound)
        }
        refreshSound()
        box.addView(soundText)
        box.addView(
            button(L.t("Sesi değiştir"), false) {
                chooseSound(sound) { s ->
                    sound = s
                    refreshSound()
                }
            },
            LinearLayout.LayoutParams(WRAP, WRAP)
        )

        var dlg: AlertDialog? = null

        if (existing != null) {
            box.addView(linkText(L.t("Kodu yenile (eski QR çalışmaz olur)"), MUTED) {
                confirm(
                    L.t("Yeni kod üretilsin mi?"),
                    L.t("Eski QR kod, aile/yedek kodu ve bağlantılar çalışmaz olur. Yeni QR'ı yeniden paylaşmanız gerekir.")
                ) {
                    val all = Store.bells(this)
                    all.firstOrNull { it.id == existing.id }?.let { Store.renew(it) }
                    Store.saveBells(this, all)
                    ServiceCtl.reload(this)
                    dlg?.dismiss()
                    render()
                    toast(L.t("Yeni kod üretildi. QR'ı yeniden paylaşın."))
                }
            })
            box.addView(linkText(L.t("Bu zili sil"), RED) {
                confirm(
                    L.f("\"%s\" silinsin mi?", existing.name),
                    L.t("Bu zilin QR kodu artık çalışmaz.")
                ) {
                    val all = Store.bells(this)
                    all.removeAll { it.id == existing.id }
                    Store.saveBells(this, all)
                    ServiceCtl.reload(this)
                    dlg?.dismiss()
                    render()
                }
            })
        }

        val sv = ScrollView(this)
        sv.addView(box)

        val d = AlertDialog.Builder(this)
            .setTitle(if (existing == null) L.t("Yeni zil") else L.t("Zili düzenle"))
            .setView(sv)
            .setPositiveButton(L.t("Kaydet")) { _, _ ->
                val nm = input.text.toString().trim()
                if (nm.isEmpty()) {
                    toast(L.t("Bir ad yazın"))
                } else if (existing == null) {
                    val nb = Store.newBell(nm, sound)
                    val all = Store.bells(this)
                    all.add(nb)
                    Store.saveBells(this, all)
                    ServiceCtl.reload(this)
                    render()
                    qrDialog(nb)
                } else {
                    val all = Store.bells(this)
                    val target = all.firstOrNull { it.id == existing.id }
                    target?.name = nm
                    target?.sound = sound
                    Store.saveBells(this, all)
                    render()
                }
            }
            .setNegativeButton(L.t("İptal"), null)
            .create()
        dlg = d
        d.show()
    }

    // ------------------------------------------------------------ QR / paylaş

    private fun qrDialog(b: Bell) {
        val link = Store.link(this, b)
        if (link == null) {
            toast(L.t("Önce sayfa adresini girin"))
            settingsDialog()
            return
        }
        val bmp = try {
            QrCard.make(link, b.name)
        } catch (_: Exception) {
            toast(L.t("QR kod üretilemedi"))
            return
        }

        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        box.setPadding(dp(18), dp(8), dp(18), dp(4))

        val iv = ImageView(this)
        iv.setImageBitmap(bmp)
        iv.adjustViewBounds = true
        box.addView(iv, LinearLayout.LayoutParams(MATCH, WRAP))

        val hint = TextView(this)
        hint.text = L.t("🔒 Notlar ve yanıtlar uçtan uca şifrelidir. Bu kapı kodu yalnızca şifrelemek için gereken genel anahtarı taşır; okutan kişi başkalarının mesajlarını okuyamaz.")
        hint.textSize = 13f
        hint.setTextColor(MUTED)
        hint.setPadding(0, dp(8), 0, 0)
        box.addView(hint)

        fun add(text: String, filled: Boolean, onClick: () -> Unit) {
            box.addView(
                button(text, filled, onClick),
                LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(8) }
            )
        }

        add(L.t("WhatsApp ile gönder (QR görseli)"), true) {
            val f = QrCard.toCacheFile(this, bmp, b.id)
            QrCard.shareImage(this, QrCard.uri(this, f), "${b.name} – QR")
        }
        add(L.t("WhatsApp ile gönder (bağlantı)"), false) {
            // Bu metni ziyaretçiler okur: iki dilli
            QrCard.shareText(this, "🔔 ${b.name}\nZile basmak için / To ring the bell:\n$link")
        }
        add(L.t("Galeriye kaydet (yazdırmak için)"), false) {
            toast(
                if (QrCard.saveToGallery(this, bmp, b.name)) L.t("Galeri › Zil klasörüne kaydedildi")
                else L.t("Kaydedilemedi (Android 10+ gerekir). WhatsApp ile gönderin.")
            )
        }
        add(L.t("Bağlantıyı kopyala"), false) {
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("zil", link))
            toast(L.t("Kopyalandı"))
        }
        add(L.t("🔐 Aile / yedek kodu (gizli)"), false) { familyDialog(b) }

        val sv = ScrollView(this)
        sv.addView(box)
        AlertDialog.Builder(this)
            .setTitle(b.name)
            .setView(sv)
            .setNegativeButton(L.t("Kapat"), null)
            .show()
    }

    // ------------------------------------------------------------ aile / yedek kodu

    /** Zilin özel anahtarını da taşıyan GİZLİ kod: ikinci aile telefonu ya da sıfırlama sonrası geri yükleme için. */
    private fun familyDialog(b: Bell) {
        val link = Store.familyLink(this, b)
        if (link == null) {
            toast(L.t("Önce sayfa adresini girin"))
            return
        }
        val bmp = try {
            QrCard.make(link, b.name, true)
        } catch (_: Exception) {
            toast(L.t("QR kod üretilemedi"))
            return
        }

        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        box.setPadding(dp(18), dp(8), dp(18), dp(4))

        val warn = TextView(this)
        warn.text = L.t("🔐 GİZLİ: Bu kod zilin özel anahtarını içerir. Kapıya yapıştırmayın, yalnızca aile bireylerine gönderin. Telefon sıfırlanırsa ya da ailedeki biri zili kendi telefonuna eklemek isterse, Kim_O'da \"İçe aktar\" ile bu kodu kullanır. Galeriye kaydederseniz telefonunuzun fotoğraf yedeklemesi bu görseli de yedekleyebilir.")
        warn.textSize = 13f
        warn.setTextColor(RED)
        warn.setPadding(0, 0, 0, dp(8))
        box.addView(warn)

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

        add(L.t("WhatsApp ile gönder (QR görseli)"), true) {
            val f = QrCard.toCacheFile(this, bmp, b.id + "-aile")
            QrCard.shareImage(this, QrCard.uri(this, f), "${b.name} – " + L.t("aile / yedek kodu (gizli)"))
        }
        add(L.t("WhatsApp ile gönder (bağlantı)"), false) {
            QrCard.shareText(
                this,
                "🔐 ${b.name}\n" + L.t("Kim_O aile / yedek kodu. Gizli tutun. Kim_O'da \"İçe aktar\"a yapıştırın ya da bağlantıyı açıp \"Uygulamaya ekle\"ye basın:") + "\n$link"
            )
        }
        add(L.t("Galeriye kaydet (yazdırmak için)"), false) {
            toast(
                if (QrCard.saveToGallery(this, bmp, b.name + "-aile")) L.t("Galeri › Zil klasörüne kaydedildi")
                else L.t("Kaydedilemedi (Android 10+ gerekir). WhatsApp ile gönderin.")
            )
        }
        add(L.t("Bağlantıyı kopyala"), false) {
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("zil", link))
            toast(L.t("Kopyalandı"))
        }

        val sv = ScrollView(this)
        sv.addView(box)
        AlertDialog.Builder(this)
            .setTitle("🔐 " + b.name)
            .setView(sv)
            .setNegativeButton(L.t("Kapat"), null)
            .show()
    }

    // ------------------------------------------------------------ içe aktar / geri yükle

    private fun info(msg: String) {
        AlertDialog.Builder(this)
            .setMessage(msg)
            .setPositiveButton(L.t("Tamam"), null)
            .show()
    }

    private fun importDialog() {
        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        box.setPadding(dp(22), dp(10), dp(22), dp(4))

        val note = TextView(this)
        note.text = L.t("Başka bir telefondan ya da sıfırlanmadan önce aldığınız aile / yedek kodunu ekleyin: bağlantıyı yapıştırın ya da QR görselini seçin. Aynı zili ailenizdeki birkaç telefon birlikte kullanabilir.")
        note.textSize = 14f
        note.setTextColor(TEXT)
        box.addView(note)

        val input = EditText(this)
        input.hint = L.t("Aile / yedek bağlantısı")
        input.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
        input.minLines = 2
        input.maxLines = 4
        box.addView(input, LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(10) })

        var dlg: AlertDialog? = null

        box.addView(
            button(L.t("Panodan yapıştır"), false) {
                val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = cm.primaryClip
                val t = if (clip != null && clip.itemCount > 0) clip.getItemAt(0).coerceToText(this)?.toString() else null
                if (t.isNullOrBlank()) toast(L.t("Pano boş")) else input.setText(t)
            },
            LinearLayout.LayoutParams(WRAP, WRAP).apply { topMargin = dp(8) }
        )
        box.addView(
            button(L.t("Galeriden QR görseli seç"), false) {
                dlg?.dismiss()
                try {
                    @Suppress("DEPRECATION")
                    startActivityForResult(
                        Intent(Intent.ACTION_GET_CONTENT).setType("image/*").addCategory(Intent.CATEGORY_OPENABLE),
                        REQ_IMPORT_IMG
                    )
                } catch (_: Throwable) {
                    toast(L.t("Bu telefonda görsel seçici açılamadı"))
                }
            },
            LinearLayout.LayoutParams(WRAP, WRAP).apply { topMargin = dp(8) }
        )

        val sv = ScrollView(this)
        sv.addView(box)
        val d = AlertDialog.Builder(this)
            .setTitle(L.t("Zili içe aktar"))
            .setView(sv)
            .setPositiveButton(L.t("Ekle")) { _, _ -> handleInvite(input.text.toString()) }
            .setNegativeButton(L.t("İptal"), null)
            .create()
        dlg = d
        d.show()
    }

    /** Yapıştırılan / okunan / kimo:// ile gelen kodu denetler; uygunsa onay penceresini açar. */
    private fun handleInvite(text: String) {
        val inv = Store.parseInvite(text)
        when {
            inv == null ->
                info(L.t("Geçerli bir Kim_O zil kodu bulunamadı. Zilin sahibinin telefonundaki \"QR / Paylaş › Aile / yedek kodu\" bağlantısını ya da görselini kullanın."))
            inv.priv.isEmpty() || inv.pub.isEmpty() ->
                info(L.t("Bu, kapıdaki ziyaretçi kodu; zili devralmak için yetmez (güvenlik için özel anahtar içermez). Zilin sahibinin telefonunda \"QR / Paylaş › Aile / yedek kodu\" bölümünü açıp onu kullanın."))
            inv.server != Store.server(this) ->
                info(L.t("Bu zil farklı bir ntfy sunucusu kullanıyor. Ayarlar › Gelişmiş ayarlar'dan sunucuyu aynı yapıp tekrar deneyin."))
            !Crypto.pairMatches(inv.pub, inv.priv, inv.topic) ->
                info(L.t("Bu kod bozuk ya da eksik; anahtarlar birbirine uymuyor. Kodu yeniden paylaşmasını isteyin."))
            Store.bells(this).any { it.topic == inv.topic } ->
                toast(L.t("Bu zil bu telefonda zaten ekli"))
            else -> confirmInvite(inv)
        }
    }

    private fun confirmInvite(inv: Invite) {
        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        box.setPadding(dp(22), dp(10), dp(22), dp(4))

        val msg = TextView(this)
        msg.text = L.t("Bu zil bu telefona eklenecek ve burada dinlenecek. Ailedeki diğer telefonlar da aynı kodla aynı zili dinleyebilir. Yalnızca tanıdığınız birinden aldıysanız ekleyin. Adı isterseniz değiştirin.")
        msg.textSize = 14f
        msg.setTextColor(TEXT)
        box.addView(msg)

        val input = EditText(this)
        input.setText(inv.name)
        input.setSingleLine()
        input.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        box.addView(input, LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(10) })

        AlertDialog.Builder(this)
            .setTitle(L.t("Zil eklensin mi?"))
            .setView(box)
            .setPositiveButton(L.t("Ekle")) { _, _ ->
                val nm = input.text.toString().trim().ifEmpty { inv.name }.take(60)
                val all = Store.bells(this)
                if (all.any { it.topic == inv.topic }) {
                    toast(L.t("Bu zil bu telefonda zaten ekli"))
                } else {
                    all.add(
                        Bell(java.util.UUID.randomUUID().toString(), nm, inv.topic, true, Sounds.DEFAULT, inv.pub, inv.priv)
                    )
                    Store.saveBells(this, all)
                    ServiceCtl.reload(this)
                    render()
                    toast(L.f("\"%s\" eklendi", nm))
                }
            }
            .setNegativeButton(L.t("Vazgeç"), null)
            .show()
    }

    // ------------------------------------------------------------ ayarlar

    private fun openListenChannelSettings() {
        try {
            startActivity(
                Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                    .putExtra(Settings.EXTRA_CHANNEL_ID, ListenService.CH_LISTEN)
            )
        } catch (_: Throwable) {
            try {
                startActivity(
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                )
            } catch (_: Throwable) {
            }
        }
    }

    private fun openPrivacy() {
        val dir = Store.siteDir(this)
        if (dir == null) {
            toast(L.t("Sayfa adresi bulunamadı"))
            return
        }
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(dir + "gizlilik.html")))
        } catch (_: Throwable) {
            toast(L.t("Tarayıcı açılamadı"))
        }
    }

    private fun settingsDialog() {
        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        box.setPadding(dp(22), dp(6), dp(22), dp(4))

        fun section(t: String) {
            val v = TextView(this)
            v.text = t
            v.textSize = 13f
            v.setTypeface(null, Typeface.BOLD)
            v.setTextColor(ORANGE_D)
            v.setPadding(0, dp(18), 0, dp(4))
            box.addView(v)
        }

        fun note(t: String) {
            val v = TextView(this)
            v.text = t
            v.textSize = 13f
            v.setTextColor(MUTED)
            v.setPadding(0, dp(2), 0, dp(6))
            box.addView(v)
        }

        // --- Dil
        section(L.t("Dil"))
        var langSel = Store.lang(this)
        val rg = RadioGroup(this)
        rg.orientation = LinearLayout.VERTICAL
        val opts = listOf(
            "auto" to L.t("Otomatik (telefonun dili)"),
            "tr" to "Türkçe",
            "en" to "English"
        )
        for ((code, label) in opts) {
            val rb = RadioButton(this)
            rb.text = label
            rb.id = View.generateViewId()
            rb.isChecked = (code == langSel)
            rb.setOnClickListener { langSel = code }
            rg.addView(rb)
        }
        box.addView(rg)

        // --- Bildirim
        section(L.t("Bildirim"))
        note(L.t("Android, arka planda dinleyen uygulamalar için sessiz bir bildirim gösterir. Buradan gizleyebilirsiniz; zil yine çalışır."))
        box.addView(
            button(L.t("Kalıcı bildirimi gizle / ayarla"), false) { openListenChannelSettings() },
            LinearLayout.LayoutParams(WRAP, WRAP)
        )

        // --- Gizlilik ve reklamlar
        section(L.t("Gizlilik ve reklamlar"))
        box.addView(
            button(L.t("Gizlilik politikası"), false) { openPrivacy() },
            LinearLayout.LayoutParams(WRAP, WRAP)
        )
        if (Ads.privacyOptionsRequired(this)) {
            box.addView(
                button(L.t("Reklam gizlilik seçenekleri"), false) { Ads.showPrivacyOptions(this) },
                LinearLayout.LayoutParams(WRAP, WRAP).apply { topMargin = dp(8) }
            )
        }

        // --- Hakkında
        section(L.t("Hakkında"))
        note(
            "Kim_O ${BuildConfig.VERSION_NAME} • " +
                if (BuildConfig.ADMOB_LIVE) L.t("Reklamlar: gerçek") else L.t("Reklamlar: test")
        )

        // --- Gelişmiş (varsayılan olarak gizli)
        val adv = LinearLayout(this)
        adv.orientation = LinearLayout.VERTICAL
        adv.visibility = View.GONE

        val page = EditText(this)
        val srv = EditText(this)

        fun advLabel(t: String) {
            val v = TextView(this)
            v.text = t
            v.textSize = 13f
            v.setTextColor(MUTED)
            v.setPadding(0, dp(10), 0, 0)
            adv.addView(v)
        }

        advLabel(L.t("Ziyaretçi sayfası adresi (derlemeden otomatik gelir, genelde değiştirmeyin)"))
        page.setText(Store.pageUrl(this))
        page.hint = "https://kullanici.github.io/depo/"
        page.setSingleLine()
        page.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
        adv.addView(page)

        advLabel(L.t("ntfy sunucusu (genelde değiştirmeyin)"))
        srv.setText(Store.server(this))
        srv.setSingleLine()
        srv.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
        adv.addView(srv)

        adv.addView(linkText(L.t("Varsayılana sıfırla"), RED) {
            page.setText(BuildConfig.DEFAULT_PAGE_URL)
            srv.setText(Store.DEFAULT_SERVER)
        })

        box.addView(linkText(L.t("Gelişmiş ayarlar ▾"), MUTED) {
            adv.visibility = if (adv.visibility == View.GONE) View.VISIBLE else View.GONE
        })
        box.addView(adv)

        val sv = ScrollView(this)
        sv.addView(box)
        AlertDialog.Builder(this)
            .setTitle(L.t("Ayarlar"))
            .setView(sv)
            .setPositiveButton(L.t("Kaydet")) { _, _ ->
                var langChanged = false
                if (langSel != Store.lang(this)) {
                    Store.setLang(this, langSel)
                    langChanged = true
                }

                val p = page.text.toString().trim()
                if (p.isEmpty() || p == BuildConfig.DEFAULT_PAGE_URL) {
                    Store.setPageUrl(this, "")
                } else if (Store.isValidPage(p)) {
                    Store.setPageUrl(this, p)
                } else {
                    toast(L.t("Sayfa adresi geçersiz, eski adres korundu"))
                }

                val oldServer = Store.server(this)
                val s = srv.text.toString().trim().trimEnd('/')
                if (s.isEmpty()) {
                    Store.setServer(this, Store.DEFAULT_SERVER)
                } else if (s.startsWith("https://")) {
                    Store.setServer(this, s)
                } else {
                    toast(L.t("Sunucu adresi https:// ile başlamalı, eski adres korundu"))
                }
                if (Store.server(this) != oldServer) ServiceCtl.reload(this)

                if (langChanged) recreate() else render()
            }
            .setNegativeButton(L.t("İptal"), null)
            .show()
    }
}
