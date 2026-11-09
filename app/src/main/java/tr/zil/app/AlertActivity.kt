package tr.zil.app

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Kilit ekranında bile açılan "Biri zile bastı" ekranı. Ziyaretçinin notunu gösterir, yanıt yazdırır. */
class AlertActivity : Activity() {

    companion object {
        const val ACTION_CLOSE = "tr.zil.app.CLOSE_ALERT"
    }

    private lateinit var nameView: TextView
    private lateinit var timeView: TextView
    private lateinit var noteView: TextView
    private lateinit var replyBox: LinearLayout

    private var topic = ""
    private var sid = ""
    private var sk = ""      // ziyaretçinin bu oturum için yolladığı yanıt anahtarı (yanıtlar bununla şifrelenir)
    private var vlang = ""   // ziyaretçinin dili ("tr", "en" ya da bilinmiyor)

    private val closer = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            finish()
        }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun pill(text: String, filled: Boolean, onClick: () -> Unit): Button {
        val b = Button(this)
        b.text = text
        b.isAllCaps = false
        b.textSize = 15f
        b.setTypeface(null, Typeface.BOLD)
        b.setTextColor(if (filled) Color.parseColor("#E65100") else Color.WHITE)
        b.background = GradientDrawable().apply {
            if (filled) setColor(Color.WHITE) else {
                setColor(Color.TRANSPARENT)
                setStroke(dp(2), Color.WHITE)
            }
            cornerRadius = dp(28).toFloat()
        }
        b.stateListAnimator = null
        b.setOnClickListener { onClick() }
        return b
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            // Android 8.0: bu iki işlev yok, aynı işi pencere bayraklarıyla yap
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(24), dp(40), dp(24), dp(24))
        }

        root.addView(TextView(this).apply {
            text = "🔔"
            textSize = 80f
            gravity = Gravity.CENTER
        })

        nameView = TextView(this).apply {
            textSize = 32f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }
        root.addView(nameView)

        root.addView(TextView(this).apply {
            text = L.t("Biri zile bastı")
            textSize = 20f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, dp(6), 0, 0)
        })

        timeView = TextView(this).apply {
            textSize = 15f
            setTextColor(Color.parseColor("#FFE0B2"))
            gravity = Gravity.CENTER
            setPadding(0, dp(4), 0, dp(18))
        }
        root.addView(timeView)

        // Ziyaretçinin bıraktığı not
        noteView = TextView(this).apply {
            textSize = 20f
            setTextColor(Color.parseColor("#2B1B0E"))
            gravity = Gravity.CENTER
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = dp(16).toFloat()
            }
            setTextIsSelectable(true)
        }
        root.addView(
            noteView,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                .apply { bottomMargin = dp(18) }
        )

        // Yanıt alanı (ziyaretçi notla geldiyse veya sayfa açıksa)
        replyBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        val quick = listOf(
            "Geliyorum" to "I'm coming",
            "Bir dakika bekleyin" to "Please wait a minute",
            "Evde yokuz, sonra gelin" to "We're not home, please come back later"
        )
        for ((trText, enText) in quick) {
            replyBox.addView(
                pill(L.t(trText), false) {
                    val toEn = if (vlang.isNotEmpty()) vlang == "en" else L.isEn()
                    sendReply(if (toEn) enText else trText)
                },
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48)).apply { topMargin = dp(8) }
            )
        }

        val input = EditText(this).apply {
            hint = L.t("Kendi yanıtınızı yazın")
            setHintTextColor(Color.parseColor("#FFE0B2"))
            setTextColor(Color.WHITE)
            textSize = 16f
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            setSingleLine()
            filters = arrayOf(InputFilter.LengthFilter(200))
            backgroundTintList = android.content.res.ColorStateList.valueOf(Color.WHITE)
        }
        replyBox.addView(
            input,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                .apply { topMargin = dp(12) }
        )
        replyBox.addView(
            pill(L.t("Yanıt gönder"), true) {
                val t = input.text.toString()
                if (t.isNotBlank()) {
                    sendReply(t)
                    input.setText("")
                }
            },
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48)).apply { topMargin = dp(8) }
        )
        root.addView(replyBox)

        val stop = Button(this).apply {
            text = L.t("SUSTUR")
            textSize = 20f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#E65100"))
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = dp(32).toFloat()
            }
            stateListAnimator = null
            setOnClickListener {
                startService(
                    Intent(this@AlertActivity, ListenService::class.java)
                        .setAction(ListenService.ACTION_STOP_RING)
                )
                finish()
            }
        }
        root.addView(
            stop,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(64)).apply { topMargin = dp(22) }
        )

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            setBackgroundColor(Color.parseColor("#FF6D00"))
            fitsSystemWindows = true
            addView(root, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }
        setContentView(scroll)
        show(intent)

        ContextCompat.registerReceiver(
            this, closer, IntentFilter(ACTION_CLOSE), ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        show(intent)
    }

    private fun show(i: Intent?) {
        nameView.text = i?.getStringExtra("name") ?: L.t("Zil")
        val t = i?.getLongExtra("time", System.currentTimeMillis()) ?: System.currentTimeMillis()
        timeView.text = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(t))

        val note = i?.getStringExtra("note") ?: ""
        noteView.text = note
        noteView.visibility = if (note.isEmpty()) android.view.View.GONE else android.view.View.VISIBLE

        topic = i?.getStringExtra("topic") ?: ""
        sid = i?.getStringExtra("sid") ?: ""
        sk = i?.getStringExtra("sk") ?: ""
        vlang = i?.getStringExtra("vlang") ?: ""
        // Ziyaretçi kimliği ya da yanıt anahtarı yoksa (eski sayfa / test) yanıt gönderilemez
        replyBox.visibility =
            if (topic.isEmpty() || sid.isEmpty() || sk.isEmpty()) android.view.View.GONE else android.view.View.VISIBLE
    }

    private fun sendReply(text: String) {
        val t = text.trim().take(200)
        if (t.isEmpty() || topic.isEmpty() || sid.isEmpty() || sk.isEmpty()) return
        val url = "${Store.server(this)}/$topic-$sid"
        // Yanıt uçtan uca şifrelenir: yalnızca bu ziyaretçinin tarayıcısı çözebilir
        val body = Crypto.encryptReply(sk, "$topic-$sid", t)
        if (body == null) {
            Toast.makeText(this, L.t("Gönderilemedi"), Toast.LENGTH_SHORT).show()
            return
        }

        Thread {
            val ok = try {
                val c = URL(url).openConnection() as HttpURLConnection
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
                Toast.makeText(this, if (ok) L.t("Yanıt gönderildi") else L.t("Gönderilemedi"), Toast.LENGTH_SHORT).show()
            }
        }.start()

        // Yanıt verince zil sesi susar
        try {
            startService(
                Intent(this, ListenService::class.java).setAction(ListenService.ACTION_STOP_RING)
            )
        } catch (_: Exception) {
        }
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(closer)
        } catch (_: Exception) {
        }
        super.onDestroy()
    }
}
