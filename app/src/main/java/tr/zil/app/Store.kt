package tr.zil.app

import android.content.Context
import android.content.Intent
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLDecoder
import java.net.URLEncoder
import java.security.SecureRandom
import java.util.UUID

/**
 * Bir zil. [pub] kapıdaki QR'da durur (herkes görebilir, yalnızca şifreler);
 * [priv] yalnızca ev sahibinin telefonlarındadır (mesajları çözer, aile/yedek koduyla taşınır).
 */
data class Bell(
    val id: String,
    var name: String,
    var topic: String,
    var enabled: Boolean = true,
    var sound: String = Sounds.DEFAULT,
    var pub: String = "",
    var priv: String = ""
)

/** İçe aktarılacak zil bilgisi (bağlantıdan / QR'dan okunur). [priv] boşsa bu yalnızca kapı QR'ıdır. */
data class Invite(
    val name: String,
    val topic: String,
    val pub: String,
    val priv: String,
    val server: String
)

object Store {
    const val DEFAULT_SERVER = "https://ntfy.sh"

    private val PAGE_REGEX = Regex("^(https?://)?[A-Za-z0-9.-]+\\.[A-Za-z]{2,}(:\\d+)?(/[^\\s@]*)?$")
    private val TOPIC_REGEX = Regex("^[A-Za-z0-9_-]{8,64}$")
    private val SERVER_REGEX = Regex("^https://[A-Za-z0-9.-]+(:\\d{1,5})?$")

    private val lock = Any()

    // Doğrulanmış anahtar çiftleri: bells() her 2 saniyede çağrıldığından eğri denetimi her seferinde yinelenmesin
    private val validPairs = HashSet<String>()

    private fun keysOk(b: Bell): Boolean {
        val k = b.pub + "|" + b.priv
        if (validPairs.contains(k)) return true
        val ok = Crypto.isPub(b.pub) && Crypto.isPriv(b.priv)
        if (ok) validPairs.add(k)
        return ok
    }

    private fun prefs(c: Context) = c.getSharedPreferences("zil", Context.MODE_PRIVATE)

    /** Yeni anahtar çifti: [genel, özel]. Üretilemezse boş (zil o zaman yalnızca "zil çal" modunda çalışır). */
    private fun newKeys(): Array<String> = try {
        Crypto.newKeyPair()
    } catch (_: Throwable) {
        arrayOf("", "")
    }

    fun bells(c: Context): MutableList<Bell> = synchronized(lock) {
        val out = mutableListOf<Bell>()
        var migrated = false
        try {
            val a = JSONArray(prefs(c).getString("bells", "[]") ?: "[]")
            for (i in 0 until a.length()) {
                val o = a.getJSONObject(i)
                val b = Bell(
                    o.getString("id"),
                    o.getString("name"),
                    o.getString("topic"),
                    o.optBoolean("enabled", true),
                    o.optString("sound", Sounds.DEFAULT).ifEmpty { Sounds.DEFAULT },
                    o.optString("pub"),
                    o.optString("priv")
                )
                // Eski sürümden gelen (anahtarı olmayan) ziller için bir kez anahtar çifti üret
                if (!keysOk(b)) {
                    val k = newKeys()
                    if (k[0].isNotEmpty()) {
                        b.pub = k[0]
                        b.priv = k[1]
                        migrated = true
                    }
                }
                out.add(b)
            }
        } catch (_: Exception) {
        }
        if (migrated) saveBells(c, out)
        out
    }

    fun saveBells(c: Context, list: List<Bell>) = synchronized(lock) {
        val a = JSONArray()
        for (b in list) {
            a.put(
                JSONObject()
                    .put("id", b.id)
                    .put("name", b.name)
                    .put("topic", b.topic)
                    .put("enabled", b.enabled)
                    .put("sound", b.sound)
                    .put("pub", b.pub)
                    .put("priv", b.priv)
            )
        }
        prefs(c).edit().putString("bells", a.toString()).apply()
    }

    fun newBell(name: String, sound: String = Sounds.DEFAULT): Bell {
        val k = newKeys()
        return Bell(UUID.randomUUID().toString(), name, newTopic(), true, sound, k[0], k[1])
    }

    /** "Kodu yenile": yeni zil kodu + yeni anahtar çifti. Eski QR ve eski aile kodları geçersiz olur. */
    fun renew(b: Bell) {
        val k = newKeys()
        b.topic = newTopic()
        b.pub = k[0]
        b.priv = k[1]
    }

    /** Tahmin edilemez, rastgele zil kodu (ntfy konu adı). */
    fun newTopic(): String {
        val chars = "abcdefghijklmnopqrstuvwxyz0123456789"
        val r = SecureRandom()
        val sb = StringBuilder("zil-")
        repeat(16) { sb.append(chars[r.nextInt(chars.length)]) }
        return sb.toString()
    }

    fun enabled(c: Context) = prefs(c).getBoolean("enabled", true)
    fun setEnabled(c: Context, v: Boolean) = prefs(c).edit().putBoolean("enabled", v).apply()

    /** "auto" (telefonun dili), "tr" veya "en". */
    fun lang(c: Context): String = prefs(c).getString("lang", "auto") ?: "auto"
    fun setLang(c: Context, v: String) = prefs(c).edit().putString("lang", v).apply()

    fun isValidPage(s: String) = PAGE_REGEX.matches(s.trim())

    /** Kullanıcı elle girmediyse derlemeye gömülü varsayılan sayfa adresi kullanılır. */
    fun pageUrl(c: Context): String {
        val saved = (prefs(c).getString("pageUrl", "") ?: "").trim()
        // "@" içeren (e-posta gibi) ya da alan adı olmayan değerleri kabul etme
        return if (saved.isNotEmpty() && isValidPage(saved)) saved else BuildConfig.DEFAULT_PAGE_URL
    }

    fun setPageUrl(c: Context, v: String) = prefs(c).edit().putString("pageUrl", v.trim()).apply()

    fun server(c: Context): String {
        val s = (prefs(c).getString("server", DEFAULT_SERVER) ?: DEFAULT_SERVER).trim().trimEnd('/')
        return if (s.isEmpty()) DEFAULT_SERVER else s
    }

    fun setServer(c: Context, v: String) =
        prefs(c).edit().putString("server", v.trim().trimEnd('/')).apply()

    private fun pageBase(c: Context): String? {
        var base = pageUrl(c)
        if (base.isEmpty()) return null
        if (!base.startsWith("http")) base = "https://$base"
        if (!base.endsWith("/") && !base.endsWith(".html")) base += "/"
        return base
    }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    /**
     * Kapıdaki QR koduna yazılacak ziyaretçi bağlantısı (yalnızca GENEL anahtar içerir).
     * Sayfa adresi girilmemişse null.
     */
    fun link(c: Context, b: Bell): String? {
        val base = pageBase(c) ?: return null
        val sb = StringBuilder(base)
        sb.append("#t=").append(b.topic)
        sb.append("&n=").append(enc(b.name))
        if (Crypto.isPub(b.pub)) sb.append("&p=").append(b.pub)
        val srv = server(c)
        if (srv != DEFAULT_SERVER) sb.append("&s=").append(enc(srv))
        return sb.toString()
    }

    /**
     * Aile / yedek bağlantısı: özel anahtarı da içerir, GİZLİ tutulmalıdır. Başka bir telefona
     * ya da telefon sıfırlandıktan sonra zili geri yüklemek için kullanılır. Anahtar yoksa null.
     */
    fun familyLink(c: Context, b: Bell): String? {
        val base = pageBase(c) ?: return null
        if (!Crypto.isPub(b.pub) || !Crypto.isPriv(b.priv)) return null
        val sb = StringBuilder(base)
        sb.append("#t=").append(b.topic)
        sb.append("&n=").append(enc(b.name))
        sb.append("&p=").append(b.pub)
        sb.append("&d=").append(b.priv)
        val srv = server(c)
        if (srv != DEFAULT_SERVER) sb.append("&s=").append(enc(srv))
        return sb.toString()
    }

    /**
     * Yapıştırılan metinden / okunan QR'dan zil bilgisini çıkarır. Şunları kabul eder:
     * https://sayfa/#t=…&n=…&p=…&d=…   kimo://add#t=…   ya da bir WhatsApp iletisinin içindeki bağlantı.
     * Geçerli bir zil bağlantısı bulunamazsa null.
     */
    fun parseInvite(text: String): Invite? {
        val s = text.trim()
        val at = s.indexOf("#t=")
        val frag = when {
            at >= 0 -> s.substring(at + 1)
            s.startsWith("t=") -> s
            else -> return null
        }.takeWhile { !it.isWhitespace() }

        val map = HashMap<String, String>()
        for (part in frag.split('&')) {
            val eq = part.indexOf('=')
            if (eq <= 0) continue
            val k = part.substring(0, eq)
            val v = try {
                URLDecoder.decode(part.substring(eq + 1), "UTF-8")
            } catch (_: Exception) {
                return null
            }
            if (!map.containsKey(k)) map[k] = v
        }

        val topic = map["t"] ?: return null
        if (!TOPIC_REGEX.matches(topic)) return null
        val pub = map["p"] ?: ""
        val priv = map["d"] ?: ""
        if (pub.isNotEmpty() && !Crypto.isPub(pub)) return null
        if (priv.isNotEmpty() && !Crypto.isPriv(priv)) return null
        val server = map["s"] ?: DEFAULT_SERVER
        if (!SERVER_REGEX.matches(server)) return null
        val name = (map["n"] ?: "").trim().take(60).ifEmpty { "Zil" }
        return Invite(name, topic, pub, priv, server)
    }

    /** Sitenin klasör adresi (gizlilik sayfası gibi yan sayfalar için). */
    fun siteDir(c: Context): String? {
        var base = pageUrl(c).trim()
        if (base.isEmpty()) return null
        if (!base.startsWith("http")) base = "https://$base"
        val cut = base.indexOfFirst { it == '#' || it == '?' }
        if (cut >= 0) base = base.substring(0, cut)
        base = if (base.endsWith(".html")) base.substringBeforeLast('/') + "/"
        else if (base.endsWith("/")) base
        else "$base/"
        return base
    }
}

object ServiceCtl {
    fun start(c: Context) {
        try {
            c.startForegroundService(
                Intent(c, ListenService::class.java).setAction(ListenService.ACTION_START)
            )
        } catch (_: Exception) {
        }
    }

    fun reload(c: Context) {
        if (Store.enabled(c)) start(c)
    }

    fun stop(c: Context) {
        try {
            c.stopService(Intent(c, ListenService::class.java))
        } catch (_: Exception) {
        }
    }
}
