package tr.zil.app

import android.content.Context
import android.content.Intent
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.security.SecureRandom
import java.util.UUID

data class Bell(
    val id: String,
    var name: String,
    var topic: String,
    var enabled: Boolean = true
)

object Store {
    const val DEFAULT_SERVER = "https://ntfy.sh"

    private fun prefs(c: Context) = c.getSharedPreferences("zil", Context.MODE_PRIVATE)

    fun bells(c: Context): MutableList<Bell> {
        val out = mutableListOf<Bell>()
        try {
            val a = JSONArray(prefs(c).getString("bells", "[]") ?: "[]")
            for (i in 0 until a.length()) {
                val o = a.getJSONObject(i)
                out.add(
                    Bell(
                        o.getString("id"),
                        o.getString("name"),
                        o.getString("topic"),
                        o.optBoolean("enabled", true)
                    )
                )
            }
        } catch (_: Exception) {
        }
        return out
    }

    fun saveBells(c: Context, list: List<Bell>) {
        val a = JSONArray()
        for (b in list) {
            a.put(
                JSONObject()
                    .put("id", b.id)
                    .put("name", b.name)
                    .put("topic", b.topic)
                    .put("enabled", b.enabled)
            )
        }
        prefs(c).edit().putString("bells", a.toString()).apply()
    }

    fun newBell(name: String) = Bell(UUID.randomUUID().toString(), name, newTopic(), true)

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

    /** Kullanıcı elle girmediyse derlemeye gömülü varsayılan sayfa adresi kullanılır. */
    fun pageUrl(c: Context): String {
        val saved = (prefs(c).getString("pageUrl", "") ?: "").trim()
        // "@" içeren (e-posta gibi) ya da alan adı olmayan değerleri kabul etme
        val valid = Regex("^(https?://)?[A-Za-z0-9.-]+\\.[A-Za-z]{2,}(:\\d+)?(/[^\\s@]*)?$").matches(saved)
        return if (saved.isNotEmpty() && valid) saved else BuildConfig.DEFAULT_PAGE_URL
    }
    fun setPageUrl(c: Context, v: String) = prefs(c).edit().putString("pageUrl", v.trim()).apply()

    fun server(c: Context): String {
        val s = (prefs(c).getString("server", DEFAULT_SERVER) ?: DEFAULT_SERVER).trim().trimEnd('/')
        return if (s.isEmpty()) DEFAULT_SERVER else s
    }

    fun setServer(c: Context, v: String) =
        prefs(c).edit().putString("server", v.trim().trimEnd('/')).apply()

    /** QR koduna yazılacak ziyaretçi bağlantısı. Sayfa adresi girilmemişse null. */
    fun link(c: Context, b: Bell): String? {
        var base = pageUrl(c)
        if (base.isEmpty()) return null
        if (!base.startsWith("http")) base = "https://$base"
        if (!base.endsWith("/") && !base.endsWith(".html")) base += "/"
        val sb = StringBuilder(base)
        sb.append("#t=").append(b.topic)
        sb.append("&n=").append(URLEncoder.encode(b.name, "UTF-8"))
        val srv = server(c)
        if (srv != DEFAULT_SERVER) sb.append("&s=").append(URLEncoder.encode(srv, "UTF-8"))
        return sb.toString()
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
