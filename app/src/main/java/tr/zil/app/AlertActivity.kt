package tr.zil.app

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
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
import org.json.JSONObject
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
        setShowWhenLocked(true)
        setTurnScreenOn(true)
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
            text = "Biri zile bastı"
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

        val quick = listOf("Geliyorum", "Bir dakika bekleyin", "Evde yokuz, sonra gelin")
        for (q in quick) {
            replyBox.addView(
                pill(q, false) { sendReply(q) },
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48)).apply { topMargin = dp(8) }
            )
        }

        val input = EditText(this).apply {
            hint = "Kendi yanıtınızı yazın"
            setHintTextColor(Color.parseColor("#FFE0B2"))
            setTextColor(Color.WHITE)
            textSize = 16f
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            setSingleLine()
            backgroundTintList = android.content.res.ColorStateList.valueOf(Color.WHITE)
        }
        replyBox.addView(
            input,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                .apply { topMargin = dp(12) }
        )
        replyBox.addView(
            pill("Yanıt gönder", true) {
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
            text = "SUSTUR"
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
        nameView.text = i?.getStringExtra("name") ?: "Zil"
        val t = i?.getLongExtra("time", System.currentTimeMillis()) ?: System.currentTimeMillis()
        timeView.text = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(t))

        val note = i?.getStringExtra("note") ?: ""
        noteView.text = note
        noteView.visibility = if (note.isEmpty()) android.view.View.GONE else android.view.View.VISIBLE

        topic = i?.getStringExtra("topic") ?: ""
        sid = i?.getStringExtra("sid") ?: ""
        // Ziyaretçi kimliği yoksa (eski sayfa / test) yanıt gönderilemez
        replyBox.visibility = if (topic.isEmpty() || sid.isEmpty()) android.view.View.GONE else android.view.View.VISIBLE
    }

    private fun sendReply(text: String) {
        val t = text.trim().take(200)
        if (t.isEmpty() || topic.isEmpty() || sid.isEmpty()) return
        val url = "${Store.server(this)}/$topic-$sid"

        Thread {
            val ok = try {
                val c = URL(url).openConnection() as HttpURLConnection
                c.requestMethod = "POST"
                c.doOutput = true
                c.connectTimeout = 10_000
                c.readTimeout = 10_000
                c.outputStream.use { it.write(JSONObject().put("r", t).toString().toByteArray()) }
                val code = c.responseCode
                c.disconnect()
                code in 200..299
            } catch (_: Exception) {
                false
            }
            runOnUiThread {
                Toast.makeText(this, if (ok) "Yanıt gönderildi" else "Gönderilemedi", Toast.LENGTH_SHORT).show()
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
