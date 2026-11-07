package tr.zil.app

import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.io.File

object QrCard {

    /** Yazdırılabilir kart: üstte zil adı, ortada QR, altta açıklama. */
    fun make(link: String, title: String): Bitmap {
        val qrSize = 860
        val hints = HashMap<EncodeHintType, Any>()
        hints[EncodeHintType.MARGIN] = 0
        hints[EncodeHintType.ERROR_CORRECTION] = ErrorCorrectionLevel.M
        hints[EncodeHintType.CHARACTER_SET] = "UTF-8"
        val m = QRCodeWriter().encode(link, BarcodeFormat.QR_CODE, qrSize, qrSize, hints)

        val qr = Bitmap.createBitmap(qrSize, qrSize, Bitmap.Config.ARGB_8888)
        val px = IntArray(qrSize * qrSize)
        for (y in 0 until qrSize) {
            for (x in 0 until qrSize) {
                px[y * qrSize + x] = if (m.get(x, y)) Color.BLACK else Color.WHITE
            }
        }
        qr.setPixels(px, 0, qrSize, 0, 0, qrSize, qrSize)

        val w = 1000
        val h = 1380
        val card = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(card)
        c.drawColor(Color.WHITE)

        val bar = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FF6D00") }
        c.drawRect(0f, 0f, w.toFloat(), 14f, bar)
        c.drawRect(0f, h - 14f, w.toFloat(), h.toFloat(), bar)

        val t = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#2B1B0E")
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        t.textSize = 96f
        c.drawText("🔔", w / 2f, 170f, t)

        // Uzun adları sığdır
        t.textSize = 84f
        while (t.measureText(title) > w - 100 && t.textSize > 36f) t.textSize -= 4f
        c.drawText(title, w / 2f, 290f, t)

        c.drawBitmap(qr, (w - qrSize) / 2f, 340f, null)

        val s = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#7A6552")
            textAlign = Paint.Align.CENTER
            textSize = 52f
        }
        t.textSize = 56f
        c.drawText("Zile basmak için", w / 2f, 1254f, t)
        c.drawText("telefon kameranızla okutun", w / 2f, 1320f, s)
        return card
    }

    fun toCacheFile(ctx: Context, bmp: Bitmap, id: String): File {
        val dir = File(ctx.cacheDir, "qr").apply { mkdirs() }
        val f = File(dir, "zil-$id.png")
        f.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return f
    }

    fun uri(ctx: Context, f: File): Uri =
        FileProvider.getUriForFile(ctx, "${ctx.packageName}.files", f)

    private val WHATSAPP = listOf("com.whatsapp", "com.whatsapp.w4b")

    /** QR görselini önce WhatsApp'a, yoksa genel paylaşım menüsüne gönderir. */
    fun shareImage(ctx: Context, uri: Uri, caption: String) {
        val i = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, caption)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        sendPreferWhatsApp(ctx, i)
    }

    fun shareText(ctx: Context, text: String) {
        val i = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        sendPreferWhatsApp(ctx, i)
    }

    private fun sendPreferWhatsApp(ctx: Context, i: Intent) {
        for (pkg in WHATSAPP) {
            try {
                ctx.startActivity(Intent(i).setPackage(pkg))
                return
            } catch (_: ActivityNotFoundException) {
            }
        }
        ctx.startActivity(Intent.createChooser(i, "Gönder").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    /** Galeriye kaydet (Android 10+). Başarılıysa true. */
    fun saveToGallery(ctx: Context, bmp: Bitmap, name: String): Boolean {
        if (Build.VERSION.SDK_INT < 29) return false
        return try {
            val v = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "zil-${name.replace(Regex("[^A-Za-z0-9ÇĞİÖŞÜçğıöşü_-]"), "_")}.png")
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Zil")
            }
            val u = ctx.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, v)
                ?: return false
            ctx.contentResolver.openOutputStream(u)?.use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
            true
        } catch (_: Exception) {
            false
        }
    }
}
