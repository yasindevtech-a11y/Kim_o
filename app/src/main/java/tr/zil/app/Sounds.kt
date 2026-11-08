package tr.zil.app

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Handler
import android.os.Looper

/**
 * Zil sesi seçimi. Kayıt biçimi ("spec"):
 *   b:<ad>   uygulamayla gelen ses (doorbell, dingdong, chime, melody)
 *   d:ringtone  telefonun varsayılan zil sesi
 *   u:<uri>  telefondan seçilen ses
 */
object Sounds {
    const val DEFAULT = "b:doorbell"
    const val PHONE = "d:ringtone"

    /** (kod, görünen Türkçe ad) */
    val BUILTIN = listOf(
        "doorbell" to "Klasik zil",
        "dingdong" to "Ding-dong",
        "chime" to "Çan",
        "melody" to "Melodi"
    )

    private fun rawId(name: String): Int = when (name) {
        "dingdong" -> R.raw.dingdong
        "chime" -> R.raw.chime
        "melody" -> R.raw.melody
        else -> R.raw.doorbell
    }

    fun label(c: Context, spec: String): String {
        return when {
            spec.startsWith("b:") -> {
                val tr = BUILTIN.firstOrNull { it.first == spec.substring(2) }?.second ?: "Klasik zil"
                L.t(tr)
            }
            spec == PHONE -> L.t("Telefonun zil sesi")
            spec.startsWith("u:") -> {
                try {
                    val r = RingtoneManager.getRingtone(c, Uri.parse(spec.substring(2)))
                    r?.getTitle(c) ?: L.t("Seçilen ses")
                } catch (_: Throwable) {
                    L.t("Seçilen ses")
                }
            }
            else -> L.t("Klasik zil")
        }
    }

    private fun attrs(): AudioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ALARM)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    private fun fromRaw(c: Context, name: String, loop: Boolean): MediaPlayer? {
        val mp = MediaPlayer()
        return try {
            val afd = c.resources.openRawResourceFd(rawId(name))
            mp.setAudioAttributes(attrs())
            mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            afd.close()
            mp.isLooping = loop
            mp.prepare()
            mp
        } catch (_: Throwable) {
            try {
                mp.release()
            } catch (_: Throwable) {
            }
            null
        }
    }

    private fun fromUri(c: Context, u: Uri, loop: Boolean): MediaPlayer? {
        val mp = MediaPlayer()
        return try {
            mp.setAudioAttributes(attrs())
            mp.setDataSource(c, u)
            mp.isLooping = loop
            mp.prepare()
            mp
        } catch (_: Throwable) {
            try {
                mp.release()
            } catch (_: Throwable) {
            }
            null
        }
    }

    /** Hazırlanmış oynatıcı döndürür. Seçilen ses açılamazsa Klasik zile döner. */
    fun create(c: Context, spec: String, loop: Boolean): MediaPlayer? {
        val mp = when {
            spec.startsWith("b:") -> fromRaw(c, spec.substring(2), loop)
            spec == PHONE -> fromUri(c, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE), loop)
            spec.startsWith("u:") -> fromUri(c, Uri.parse(spec.substring(2)), loop)
            else -> null
        }
        return mp ?: fromRaw(c, "doorbell", loop)
    }

    // ---------------------------------------------------------------- önizleme

    private var preview: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())
    private val stopper = Runnable { stopPreview() }

    fun preview(c: Context, spec: String) {
        stopPreview()
        val mp = create(c, spec, false) ?: return
        preview = mp
        try {
            mp.start()
        } catch (_: Throwable) {
            stopPreview()
            return
        }
        handler.postDelayed(stopper, 4000)
    }

    fun stopPreview() {
        handler.removeCallbacks(stopper)
        try {
            preview?.stop()
        } catch (_: Throwable) {
        }
        try {
            preview?.release()
        } catch (_: Throwable) {
        }
        preview = null
    }
}
