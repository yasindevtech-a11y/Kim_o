package tr.zil.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.drawable.Icon
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Arka planda ntfy sunucusunu dinler. Ziyaretçi sayfasından "zile bas"
 * mesajı gelince zil sesini çalar ve tam ekran uyarı gösterir.
 */
class ListenService : Service() {

    companion object {
        const val ACTION_START = "tr.zil.app.START"
        const val ACTION_STOP_RING = "tr.zil.app.STOP_RING"
        const val CH_LISTEN = "listen"
        const val CH_ALERT = "alert"
        const val NOTIF_LISTEN = 1

        private const val MAX_AGE_SEC = 180L      // 3 dakikadan eski zil mesajlarını yok say
        private const val MAX_SESSION_SEC = 190L  // ziyaretçi oturumu en fazla ~3 dk
        private const val MIN_GAP_SEC = 10L      // aynı zilde art arda çalmayı engelle
        private const val RING_MS = 25_000L       // zil en fazla bu kadar çalar

        @Volatile
        var connected = false
    }

    private val handler = Handler(Looper.getMainLooper())
    // Süre dolunca sadece ses/titreşim durur; uyarı bildirimi kullanıcı görene kadar kalır
    private val stopRingRunnable = Runnable { stopSound() }

    @Volatile private var generation = 0
    @Volatile private var currentConn: HttpURLConnection? = null
    private var worker: Thread? = null

    private var player: MediaPlayer? = null
    private var wake: PowerManager.WakeLock? = null

    private val seen = LinkedHashSet<String>()
    private val lastRing = HashMap<String, Long>()
    @Volatile private var lastTime = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_RING) {
            stopRing()
            return START_STICKY
        }
        try {
            goForeground()
        } catch (e: Throwable) {
            CrashLog.save(this, "ListenService.goForeground", e)
            stopSelf()
            return START_NOT_STICKY
        }
        restartListener()
        return START_STICKY
    }

    override fun onDestroy() {
        generation++
        currentConn?.disconnect()
        worker?.interrupt()
        stopRing()
        connected = false
        super.onDestroy()
    }

    // ---------------------------------------------------------------- bildirimler

    private fun createChannels() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CH_LISTEN, "Dinleme durumu", NotificationManager.IMPORTANCE_LOW)
                .apply { setShowBadge(false) }
        )
        nm.createNotificationChannel(
            NotificationChannel(CH_ALERT, "Zil uyarısı", NotificationManager.IMPORTANCE_HIGH).apply {
                setSound(null, null)
                enableVibration(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
        )
    }

    private fun listenNotification(text: String): Notification {
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return Notification.Builder(this, CH_LISTEN)
            .setSmallIcon(R.drawable.ic_bell)
            .setContentTitle("Kim_O")
            .setContentText(text)
            .setOngoing(true)
            .setContentIntent(open)
            .build()
    }

    private fun goForeground() {
        val n = listenNotification(statusText())
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIF_LISTEN, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIF_LISTEN, n)
        }
    }

    private fun statusText(): String {
        val n = Store.bells(this).count { it.enabled }
        return if (n == 0) "Dinlenecek aktif zil yok"
        else if (connected) "Dinleniyor • $n zil aktif"
        else "Bağlanıyor… • $n zil aktif"
    }

    private fun refreshStatus() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIF_LISTEN, listenNotification(statusText()))
    }

    // ---------------------------------------------------------------- dinleme

    private fun restartListener() {
        generation++
        val gen = generation
        currentConn?.disconnect()
        worker?.interrupt()
        worker = Thread {
            try {
                listenLoop(gen)
            } catch (e: Throwable) {
                CrashLog.save(this, "listenLoop", e)
            }
        }.also {
            it.isDaemon = true
            it.start()
        }
    }

    private fun listenLoop(gen: Int) {
        var backoff = 2_000L
        while (gen == generation) {
            val topics = Store.bells(this).filter { it.enabled }.joinToString(",") { it.topic }
            if (topics.isEmpty()) {
                connected = false
                handler.post { refreshStatus() }
                sleepQuiet(3_000)
                continue
            }

            val since = if (lastTime > 0) lastTime.toString() else "3m"
            val url = URL("${Store.server(this)}/$topics/json?since=$since")
            var conn: HttpURLConnection? = null
            try {
                conn = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15_000
                    readTimeout = 90_000   // ntfy ~45 sn'de bir "keepalive" yollar
                    setRequestProperty("Accept", "application/x-ndjson")
                }
                currentConn = conn
                if (conn.responseCode != 200) throw IOException("HTTP ${conn.responseCode}")

                // Telefon saati bozuksa bile doğru "3 dk" hesabı için sunucu saatiyle farkı bul
                val serverDate = conn.date
                val offsetMs = if (serverDate > 0) serverDate - System.currentTimeMillis() else 0L

                connected = true
                backoff = 2_000L
                handler.post { refreshStatus() }

                conn.inputStream.bufferedReader().use { reader ->
                    while (gen == generation) {
                        val line = reader.readLine() ?: break
                        if (line.isBlank()) continue
                        handleLine(line, offsetMs)
                    }
                }
            } catch (_: Exception) {
                // bağlantı koptu / zaman aşımı → aşağıda yeniden dene
            } finally {
                conn?.disconnect()
            }

            connected = false
            if (gen != generation) break
            handler.post { refreshStatus() }
            sleepQuiet(backoff)
            backoff = minOf(backoff * 2, 30_000L)
        }
    }

    private fun sleepQuiet(ms: Long) {
        try {
            Thread.sleep(ms)
        } catch (_: InterruptedException) {
        }
    }

    private fun handleLine(line: String, offsetMs: Long) {
        val o = try {
            JSONObject(line)
        } catch (_: Exception) {
            return
        }
        if (o.optString("event") != "message") return

        val id = o.optString("id")
        val time = o.optLong("time")
        val topic = o.optString("topic")
        if (time > lastTime) lastTime = time

        if (id.isNotEmpty()) {
            if (!seen.add(id)) return
            if (seen.size > 300) seen.remove(seen.first())
        }

        val nowSec = (System.currentTimeMillis() + offsetMs) / 1000
        if (nowSec - time > MAX_AGE_SEC) return

        // Ziyaretçi sayfası JSON yollar: {"v":1,"e":<sayfa açılalı sn>,"s":<ziyaretçi kimliği>,"n":<not>}
        // Eski sürüm sayfalar "zil <sn>" yollar; ikisi de desteklenir.
        val raw = o.optString("message").trim()
        var elapsed: Long? = null
        var sid = ""
        var note = ""
        if (raw.startsWith("{")) {
            try {
                val j = JSONObject(raw)
                if (j.has("e")) elapsed = j.optLong("e")
                val s = j.optString("s")
                if (Regex("^[a-z0-9]{6,20}$").matches(s)) sid = s
                note = j.optString("n").trim().take(200)
            } catch (_: Exception) {
            }
        } else {
            elapsed = raw.split(" ").getOrNull(1)?.toLongOrNull()
        }
        // 3 dakikayı aşmış oturumda zil çalma
        if (elapsed != null && elapsed > MAX_SESSION_SEC) return

        val bell = Store.bells(this).firstOrNull { it.topic == topic && it.enabled } ?: return

        // Notlu mesajlar her zaman gösterilir; notsuz art arda basışlar 10 sn içinde tek sayılır
        val last = lastRing[topic] ?: 0L
        if (note.isEmpty() && nowSec - last < MIN_GAP_SEC) return
        lastRing[topic] = nowSec

        handler.post { ring(bell.name, topic, sid, note) }
    }

    // ---------------------------------------------------------------- zil

    private fun ring(name: String, topic: String, sid: String, note: String) {
        startSound()
        showAlert(name, topic, sid, note)
    }

    private fun startSound() {
        stopSound()
        try {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            wake = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "zil:ring")
            wake?.acquire(RING_MS + 5_000)
        } catch (_: Exception) {
        }

        try {
            val mp = MediaPlayer()
            val afd = resources.openRawResourceFd(R.raw.doorbell)
            mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            afd.close()
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            mp.isLooping = true
            mp.prepare()
            mp.start()
            player = mp
        } catch (_: Exception) {
        }

        try {
            @Suppress("DEPRECATION")
            val v = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            v.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 600, 400), 0))
        } catch (_: Exception) {
        }

        handler.removeCallbacks(stopRingRunnable)
        handler.postDelayed(stopRingRunnable, RING_MS)
    }

    private fun stopSound() {
        try {
            player?.stop()
        } catch (_: Exception) {
        }
        try {
            player?.release()
        } catch (_: Exception) {
        }
        player = null
        try {
            @Suppress("DEPRECATION")
            (getSystemService(Context.VIBRATOR_SERVICE) as Vibrator).cancel()
        } catch (_: Exception) {
        }
        try {
            if (wake?.isHeld == true) wake?.release()
        } catch (_: Exception) {
        }
        wake = null
    }

    private fun stopRing() {
        handler.removeCallbacks(stopRingRunnable)
        stopSound()
        try {
            val nm = getSystemService(NotificationManager::class.java)
            for (sn in nm.activeNotifications) {
                if (sn.id != NOTIF_LISTEN) nm.cancel(sn.id)
            }
        } catch (_: Exception) {
        }
        sendBroadcast(Intent(AlertActivity.ACTION_CLOSE).setPackage(packageName))
    }

    private fun showAlert(name: String, topic: String, sid: String, note: String) {
        val full = Intent(this, AlertActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("name", name)
            putExtra("time", System.currentTimeMillis())
            putExtra("topic", topic)
            putExtra("sid", sid)
            putExtra("note", note)
        }
        val fullPi = PendingIntent.getActivity(
            this, 100, full, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val stopPi = PendingIntent.getService(
            this, 101,
            Intent(this, ListenService::class.java).setAction(ACTION_STOP_RING),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val stopAction = Notification.Action.Builder(
            Icon.createWithResource(this, R.drawable.ic_bell), "Sustur", stopPi
        ).build()

        val n = Notification.Builder(this, CH_ALERT)
            .setSmallIcon(R.drawable.ic_bell)
            .setContentTitle("🔔 $name")
            .setContentText(if (note.isNotEmpty()) note else "Biri zile bastı")
            .setStyle(Notification.BigTextStyle().bigText(if (note.isNotEmpty()) note else "Biri zile bastı"))
            .setCategory(Notification.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(fullPi)
            .setFullScreenIntent(fullPi, true)
            .addAction(stopAction)
            .build()

        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(1000 + (name.hashCode() and 0xFFFF), n)
    }
}
