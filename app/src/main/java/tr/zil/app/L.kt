package tr.zil.app

import android.content.Context
import java.util.Locale

/**
 * Basit çeviri. Anahtar Türkçe metnin kendisidir; İngilizce karşılığı yoksa Türkçe gösterilir.
 * Dil seçimi: Ayarlar'dan "Otomatik" (telefon Türkçeyse Türkçe, değilse İngilizce), "Türkçe" ya da "English".
 */
object L {
    @Volatile
    private var ctx: Context? = null

    fun init(c: Context) {
        ctx = c.applicationContext
    }

    fun isEn(): Boolean {
        val c = ctx ?: return false
        return when (Store.lang(c)) {
            "en" -> true
            "tr" -> false
            else -> Locale.getDefault().language != "tr"
        }
    }

    fun t(tr: String): String = if (isEn()) (EN[tr] ?: tr) else tr

    /** Biçimli metin: %d / %s yer tutucuları kullanılır. */
    fun f(tr: String, vararg args: Any): String = String.format(Locale.US, t(tr), *args)

    private val EN: Map<String, String> = mapOf(
        "Biri zile bastı" to "Someone rang the bell",
        "Kendi yanıtınızı yazın" to "Write your own reply",
        "Yanıt gönder" to "Send reply",
        "SUSTUR" to "SILENCE",
        "Zil" to "Bell",
        "Yanıt gönderildi" to "Reply sent",
        "Gönderilemedi" to "Could not send",
        "Dinleme durumu" to "Listening status",
        "Zil uyarısı" to "Bell alert",
        "Dinlenecek aktif zil yok" to "No active bell to listen to",
        "Dinleniyor • %d zil aktif" to "Listening • %d bells active",
        "Bağlanıyor… • %d zil aktif" to "Connecting… • %d bells active",
        "Sustur" to "Silence",
        "Evet" to "Yes",
        "Vazgeç" to "Cancel",
        "Ayarlar" to "Settings",
        "Zilleri dinle" to "Listen for bells",
        "+  Yeni zil ekle" to "+  Add new bell",
        "Henüz zil yok.\nEv kapısı, bahçe, araba… istediğiniz kadar zil ekleyin; her birinin ayrı QR kodu olur." to "No bells yet.\nAdd as many as you like: front door, garden, car… each one gets its own QR code.",
        "Kapalı — zil çalmaz" to "Off — bells won't ring",
        "Aktif zil yok" to "No active bells",
        "● Dinleniyor • %d zil aktif" to "● Listening • %d bells active",
        "○ Bağlanıyor… • %d zil aktif" to "○ Connecting… • %d bells active",
        "Önce ziyaretçi sayfasının adresini girin. QR kodlar bu adrese göre oluşur." to "First enter the visitor page address. QR codes are built from it.",
        "Bildirim izni kapalı. Uyarı ekranı görünmez." to "Notification permission is off. The alert screen won't appear.",
        "İzin ver" to "Allow",
        "Pil tasarrufu uygulamayı uyutabilir. Açılan listede Kim_O'yu bulup \"Kısıtlama yok\" ya da \"Optimize etme\" seçin." to "Battery saver may put the app to sleep. In the list that opens, find Kim_O and choose \"Unrestricted\" or \"Don't optimize\".",
        "Ayarı aç" to "Open setting",
        "Kilit ekranında tam ekran uyarı kapalı." to "Full-screen alert on the lock screen is off.",
        "Aç" to "Turn on",
        "QR / Paylaş" to "QR / Share",
        "Düzenle" to "Edit",
        "Test" to "Test",
        "Önce üstteki \"Zilleri dinle\" anahtarını açın" to "First turn on the \"Listen for bells\" switch above",
        "Test zili gönderildi" to "Test bell sent",
        "Gönderilemedi — internet var mı?" to "Could not send — is the internet on?",
        "Telefonun zil sesi" to "Phone ringtone",
        "Telefondan seç…" to "Pick from phone…",
        "Zil sesi" to "Bell sound",
        "Bu telefonda ses seçici açılamadı" to "The sound picker could not be opened on this phone",
        "Tamam" to "OK",
        "İptal" to "Cancel",
        "Örn: Ev kapısı, Araba, Depo" to "e.g. Front door, Car, Shed",
        "Sesi değiştir" to "Change sound",
        "Kodu yenile (eski QR çalışmaz olur)" to "Renew code (old QR stops working)",
        "Yeni kod üretilsin mi?" to "Generate a new code?",
        "Yeni kod üretildi. QR'ı yeniden paylaşın." to "New code generated. Share the QR again.",
        "Bu zili sil" to "Delete this bell",
        "\"%s\" silinsin mi?" to "Delete \"%s\"?",
        "Bu zilin QR kodu artık çalışmaz." to "This bell's QR code will stop working.",
        "Yeni zil" to "New bell",
        "Zili düzenle" to "Edit bell",
        "Kaydet" to "Save",
        "Bir ad yazın" to "Enter a name",
        "Önce sayfa adresini girin" to "Enter the page address first",
        "QR kod üretilemedi" to "Could not create the QR code",
        "WhatsApp ile gönder (QR görseli)" to "Send via WhatsApp (QR image)",
        "WhatsApp ile gönder (bağlantı)" to "Send via WhatsApp (link)",
        "Galeriye kaydet (yazdırmak için)" to "Save to gallery (to print)",
        "Galeri › Zil klasörüne kaydedildi" to "Saved to Gallery › Zil folder",
        "Kaydedilemedi (Android 10+ gerekir). WhatsApp ile gönderin." to "Could not save (Android 10+ required). Send it via WhatsApp.",
        "Bağlantıyı kopyala" to "Copy link",
        "Kopyalandı" to "Copied",
        "Kapat" to "Close",
        "Sayfa adresi bulunamadı" to "Page address not found",
        "Tarayıcı açılamadı" to "Could not open the browser",
        "Dil" to "Language",
        "Otomatik (telefonun dili)" to "Automatic (phone language)",
        "Bildirim" to "Notification",
        "Android, arka planda dinleyen uygulamalar için sessiz bir bildirim gösterir. Buradan gizleyebilirsiniz; zil yine çalışır." to "Android shows a silent notification for apps that listen in the background. You can hide it here; the bell will still ring.",
        "Kalıcı bildirimi gizle / ayarla" to "Hide / adjust the permanent notification",
        "Gizlilik ve reklamlar" to "Privacy and ads",
        "Gizlilik politikası" to "Privacy policy",
        "Reklam gizlilik seçenekleri" to "Ad privacy options",
        "Hakkında" to "About",
        "Reklamlar: gerçek" to "Ads: live",
        "Reklamlar: test" to "Ads: test",
        "Ziyaretçi sayfası adresi (derlemeden otomatik gelir, genelde değiştirmeyin)" to "Visitor page address (comes automatically from the build, usually leave as is)",
        "ntfy sunucusu (genelde değiştirmeyin)" to "ntfy server (usually leave as is)",
        "Varsayılana sıfırla" to "Reset to default",
        "Gelişmiş ayarlar ▾" to "Advanced settings ▾",
        "Sayfa adresi geçersiz, eski adres korundu" to "Page address is invalid, the old address was kept",
        "Sunucu adresi https:// ile başlamalı, eski adres korundu" to "Server address must start with https://, the old address was kept",
        "Seçilen ses" to "Selected sound",
        "Klasik zil" to "Classic bell",
        "Geliyorum" to "I'm coming",
        "Bir dakika bekleyin" to "Please wait a minute",
        "Evde yokuz, sonra gelin" to "We're not home, please come back later",
        "Ding-dong" to "Ding-dong",
        "Çan" to "Chime",
        "Eski QR kod, aile/yedek kodu ve bağlantılar çalışmaz olur. Yeni QR'ı yeniden paylaşmanız gerekir." to "The old QR code, family/backup code and links will stop working. You will need to share the new QR again.",
        "Görselde QR kod okunamadı" to "Could not read a QR code in the image",
        "İçe aktar (aile / yedek kodu)" to "Import (family / backup code)",
        "🔒 Notlar ve yanıtlar uçtan uca şifrelidir. Bu kapı kodu yalnızca şifrelemek için gereken genel anahtarı taşır; okutan kişi başkalarının mesajlarını okuyamaz." to "🔒 Notes and replies are end-to-end encrypted. This door code only carries the public key needed to encrypt; whoever scans it cannot read other people's messages.",
        "🔐 Aile / yedek kodu (gizli)" to "🔐 Family / backup code (private)",
        "🔐 GİZLİ: Bu kod zilin özel anahtarını içerir. Kapıya yapıştırmayın, yalnızca aile bireylerine gönderin. Telefon sıfırlanırsa ya da ailedeki biri zili kendi telefonuna eklemek isterse, Kim_O'da \"İçe aktar\" ile bu kodu kullanır. Galeriye kaydederseniz telefonunuzun fotoğraf yedeklemesi bu görseli de yedekleyebilir." to "🔐 PRIVATE: This code contains the bell's private key. Do not stick it on the door; send it only to family members. If a phone is reset, or someone in the family wants the bell on their own phone, they use this code with \"Import\" in Kim_O. If you save it to the gallery, your phone's photo backup may back this image up too.",
        "aile / yedek kodu (gizli)" to "family / backup code (private)",
        "Kim_O aile / yedek kodu. Gizli tutun. Kim_O'da \"İçe aktar\"a yapıştırın ya da bağlantıyı açıp \"Uygulamaya ekle\"ye basın:" to "Kim_O family / backup code. Keep it private. Paste it into \"Import\" in Kim_O, or open the link and tap \"Add to the app\":",
        "Başka bir telefondan ya da sıfırlanmadan önce aldığınız aile / yedek kodunu ekleyin: bağlantıyı yapıştırın ya da QR görselini seçin. Aynı zili ailenizdeki birkaç telefon birlikte kullanabilir." to "Add a family / backup code you got from another phone or saved before a reset: paste the link or pick the QR image. Several phones in your family can use the same bell together.",
        "Aile / yedek bağlantısı" to "Family / backup link",
        "Panodan yapıştır" to "Paste from clipboard",
        "Pano boş" to "Clipboard is empty",
        "Galeriden QR görseli seç" to "Pick a QR image from the gallery",
        "Bu telefonda görsel seçici açılamadı" to "The image picker could not be opened on this phone",
        "Zili içe aktar" to "Import bell",
        "Ekle" to "Add",
        "Geçerli bir Kim_O zil kodu bulunamadı. Zilin sahibinin telefonundaki \"QR / Paylaş › Aile / yedek kodu\" bağlantısını ya da görselini kullanın." to "No valid Kim_O bell code found. Use the link or image from \"QR / Share › Family / backup code\" on the bell owner's phone.",
        "Bu, kapıdaki ziyaretçi kodu; zili devralmak için yetmez (güvenlik için özel anahtar içermez). Zilin sahibinin telefonunda \"QR / Paylaş › Aile / yedek kodu\" bölümünü açıp onu kullanın." to "This is the visitor code from the door; it is not enough to take over the bell (for security it has no private key). On the bell owner's phone open \"QR / Share › Family / backup code\" and use that instead.",
        "Bu zil farklı bir ntfy sunucusu kullanıyor. Ayarlar › Gelişmiş ayarlar'dan sunucuyu aynı yapıp tekrar deneyin." to "This bell uses a different ntfy server. Set the same server under Settings › Advanced settings and try again.",
        "Bu kod bozuk ya da eksik; anahtarlar birbirine uymuyor. Kodu yeniden paylaşmasını isteyin." to "This code is damaged or incomplete; the keys do not match. Ask for it to be shared again.",
        "Bu zil bu telefonda zaten ekli" to "This bell is already on this phone",
        "Bu zil bu telefona eklenecek ve burada dinlenecek. Ailedeki diğer telefonlar da aynı kodla aynı zili dinleyebilir. Yalnızca tanıdığınız birinden aldıysanız ekleyin. Adı isterseniz değiştirin." to "This bell will be added to this phone and listened to here. Other phones in the family can listen to the same bell with the same code. Only add it if you got it from someone you know. Change the name if you like.",
        "Zil eklensin mi?" to "Add this bell?",
        "\"%s\" eklendi" to "\"%s\" added",
        "Melodi" to "Melody"
    )
}
