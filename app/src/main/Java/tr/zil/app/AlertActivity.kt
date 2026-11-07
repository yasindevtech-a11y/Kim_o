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
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Kilit ekranında bile açılan "Biri zile bastı" uyarı ekranı. */
class AlertActivity : Activity() {

    companion object {
        const val ACTION_CLOSE = "tr.zil.app.CLOSE_ALERT"
    }

    private lateinit var nameView: TextView
    private lateinit var timeView: TextView

    private val closer = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            finish()
        }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#FF6D00"))
            setPadding(dp(24), dp(24), dp(24), dp(24))
        }

        root.addView(TextView(this).apply {
            text = "🔔"
            textSize = 96f
            gravity = Gravity.CENTER
        })

        nameView = TextView(this).apply {
            textSize = 34f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }
        root.addView(nameView)

        root.addView(TextView(this).apply {
            text = "Biri zile bastı"
            textSize = 22f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, 0)
        })

        timeView = TextView(this).apply {
            textSize = 16f
            setTextColor(Color.parseColor("#FFE0B2"))
            gravity = Gravity.CENTER
            setPadding(0, dp(6), 0, dp(40))
        }
        root.addView(timeView)

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
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(64))
        )

        setContentView(root)
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
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(closer)
        } catch (_: Exception) {
        }
        super.onDestroy()
    }
}
