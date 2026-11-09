package tr.zil.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.GlobalHistogramBinarizer
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader

/** Galeriden seçilen bir görselin içindeki QR kodu okur (aile/yedek kodunu içe aktarmak için). */
object QrReader {

    /** Görselde QR varsa içindeki metni, yoksa ya da okunamazsa null döner. */
    fun decode(c: Context, uri: Uri): String? {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            c.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            val longest = maxOf(bounds.outWidth, bounds.outHeight)
            if (longest <= 0) return null

            // Çok büyük fotoğrafları belleği doldurmadan küçülterek aç
            var sample = 1
            while (longest / sample > 2200) sample *= 2
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            val bmp = c.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
                ?: return null

            decodeBitmap(bmp)
                ?: decodeBitmap(shrink(bmp, 1000))
                ?: decodeBitmap(shrink(bmp, 600))
        } catch (_: Throwable) {
            null
        }
    }

    private fun shrink(b: Bitmap, maxSide: Int): Bitmap {
        val big = maxOf(b.width, b.height)
        if (big <= maxSide) return b
        val f = maxSide.toFloat() / big
        return Bitmap.createScaledBitmap(b, (b.width * f).toInt().coerceAtLeast(1), (b.height * f).toInt().coerceAtLeast(1), true)
    }

    private fun decodeBitmap(bmp: Bitmap): String? {
        val w = bmp.width
        val h = bmp.height
        val px = IntArray(w * h)
        bmp.getPixels(px, 0, w, 0, 0, w, h)
        val src = RGBLuminanceSource(w, h, px)

        val hints = HashMap<DecodeHintType, Any>()
        hints[DecodeHintType.TRY_HARDER] = true
        hints[DecodeHintType.POSSIBLE_FORMATS] = listOf(BarcodeFormat.QR_CODE)

        val tries = listOf(
            BinaryBitmap(HybridBinarizer(src)),
            BinaryBitmap(GlobalHistogramBinarizer(src)),
            BinaryBitmap(HybridBinarizer(src.invert()))
        )
        for (t in tries) {
            try {
                return QRCodeReader().decode(t, hints).text
            } catch (_: Exception) {
                // bu yöntemle okunamadı, sıradakini dene
            }
        }
        return null
    }
}
