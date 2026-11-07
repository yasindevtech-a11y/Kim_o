# 🔔 Kim_O – QR kodlu kapı zili

Kapıya QR kod yapıştırırsınız. Ziyaretçi okutunca tarayıcıda **"Zile Bas"** butonu açılır
(uygulama kurmaz). Butona basınca sizin telefonunuzda uygulama zil sesi çalar ve ekranda
**"Biri zile bastı"** uyarısı çıkar. Ziyaretçi sayfası 3 dakika sonra kendini siler.

```
Ziyaretçi telefonu ──► docs/index.html (GitHub Pages) ──► ntfy.sh ──► Sizin telefondaki Kim_O uygulaması
```

Firebase yok, kendi sunucunuz yok, hesap yok. Ücretsizdir.

---

## Kurulum (bir kez, ~15 dakika)

### 1) GitHub'a yükleyin
1. github.com'da **New repository** → adı `zil` → **Public** → oluşturun.
2. Bu klasörün **tüm içeriğini** (`app`, `docs`, `.github`, `build.gradle.kts`… hepsi) repoya yükleyin.
   Tarayıcıdan: **Add file → Upload files**. `.github` klasörü gizli olabilir; yüklendiğinden emin olun.

### 2) Ziyaretçi sayfasını yayınlayın
1. Repoda **Settings → Pages**.
2. **Source:** *Deploy from a branch* → **Branch:** `main` → klasör **/docs** → **Save**.
3. Bir dakika sonra adresiniz hazır olur: `https://KULLANICI_ADINIZ.github.io/zil/`

### 3) APK'yı derleyin
1. Repoda **Actions** sekmesi → **APK derle** → **Run workflow** (ilk yüklemede kendiliğinden de başlar).
2. 5–8 dakika sonra yeşil tik çıkar.
3. Repo ana sayfasında sağdaki **Releases** bölümüne girin → **Zil v1** → `app-debug.apk` dosyasını telefonunuzdan indirin.
4. Açın ve kurun ("Bilinmeyen kaynaklardan yüklemeye izin ver" sorarsa izin verin).

### 4) Uygulamayı ayarlayın
1. Kim_O uygulamasını açın. Ziyaretçi sayfası adresi APK'ya **otomatik gömülüdür**, ayar girmeniz gerekmez.
   (Başkasına vermek için de aynı APK'yı gönderirsiniz, o da hiçbir ayar yapmadan kullanır.)
2. Ana ekrandaki uyarı kartlarındaki izinleri verin:
   - **Bildirim izni**
   - **Pil kısıtlamasını kaldır** (çok önemli, yoksa telefon uygulamayı uyutur)
   - **Tam ekran uyarı** (Android 14+)
3. **+ Yeni zil ekle** → `Ev kapısı`, `Bahçe kapısı`, `Araba`… istediğiniz adı yazın.
4. Zil eklenince QR ekranı açılır:
   - **WhatsApp ile gönder (QR görseli)** – QR'ı WhatsApp'tan gönderir
   - **WhatsApp ile gönder (bağlantı)** – sadece linki gönderir
   - **Galeriye kaydet** – çıktı alıp kapıya yapıştırmak için
5. **Test** düğmesi ile zili sınayın.

> Kendi telefonunuzda ziyaretçi gibi denemek için QR'ı başka bir telefonla okutun ya da
> **Bağlantıyı kopyala** ile linki tarayıcıda açın.

## Günlük kullanım
- **Düzenle**: adı değiştirin, zili silin, ya da **Kodu yenile** (QR sızdıysa eski QR'lar çalışmaz olur).
- Her zilin yanındaki anahtar o zili geçici olarak kapatır.
- Üstteki **Zilleri dinle** anahtarı hepsini birden kapatır.
- Telefon yeniden açılınca uygulama kendiliğinden dinlemeye başlar.

---

## Başkaları kullanınca (veri ve gizlilik)

- Her kullanıcı APK'yı kurar, kendi zillerini ekler ve adlandırır (Ev kapısı, Araba, Köydeki ev…).
- Her zilin kodu o telefonda rastgele üretilir ve **yalnızca o telefonda saklanır**. Kullanıcılar birbirinden bağımsızdır.
- Sizin bir sunucunuz ve veritabanınız yoktur; zil adı, konum, kişi bilgisi toplanmaz, kimseye gönderilmez.
- Ortak tek parça `docs/index.html` sayfasıdır. Herkese aynıdır, hiçbir şey kaydetmez. Zil kodu adresin `#` kısmında durur,
  bu kısım sayfayı barındıran sunucuya gitmez.
- Bildirimi taşıyan ntfy sunucusu yalnızca rastgele zil kodunu ve "zil" yazısını görür.
- Ziyaretçinin IP adresi, her web sitesinde olduğu gibi, sayfayı ve ntfy'yi barındıran sunuculara düşer.
  Uygulamanızı Play Store'da yayınlarsanız KVKK açısından bir hukukçuya danışın.

## Bilmeniz gerekenler (dürüst notlar)

- **İki telefonda da internet gerekir.** Ziyaretçide mobil veri, sizde Wi-Fi veya mobil veri.
- **3 dakika kuralı ziyaretçinin tarayıcısında çalışır.** Süre dolunca buton kaybolur, adres çubuğundaki
  zil kodu ve saklanan süre silinir; yenileyince çalışmaz, QR'ı tekrar okutmak gerekir.
  Ancak QR'ı çok kez okutan biri yeniden süre alır, bunu sunucusuz engelleyemeyiz.
- **Mesajlar ntfy.sh üzerinden geçer.** İçerikte sadece "zil" yazar; kişisel veri yoktur. ntfy.sh mesajları
  yaklaşık 12 saat saklayabilir, bunu siz siliyor olamazsınız. Uygulama 3 dakikadan eski mesajları yok sayar.
- **Zil kodu tahmin edilemez** (rastgele 16 karakter) ama QR'ı gören herkes zili çalabilir; bu zaten amaç.
  Biri kötüye kullanırsa **Düzenle → Kodu yenile** yapın. Aynı zilde art arda basışlar 10 saniye içinde tek sayılır.
- **Ses alarm sesi seviyesinden çalar.** Telefonun *alarm sesi* kapalıysa zil duyulmaz; bir kez kontrol edin.
- **Bazı telefonlar (Xiaomi, Huawei, Oppo, Samsung…)** arka plan uygulamalarını agresif kapatır.
  Uygulamayı "otomatik başlat" listesine ekleyin, son uygulamalarda kilitleyin. Ayrıntı: dontkillmyapp.com
- **Güncelleme:** yeni APK'yı eskisinin üzerine kurabilirsiniz (imza sabit, `app/zil.keystore`).
  Repo herkese açık olduğundan bu anahtar da görünür; kişisel kullanım için sorun değil.
- Kendi ntfy sunucunuz varsa **Ayarlar → ntfy sunucusu** alanına yazabilirsiniz.

## Klasör yapısı
```
app/                 Android uygulaması (Kotlin, ek kütüphane: ZXing QR)
  src/main/java/tr/zil/app/
    MainActivity.kt  zil listesi, ekle/düzenle/sil, QR ve WhatsApp paylaşımı
    ListenService.kt arka plan dinleme, ses, bildirim
    AlertActivity.kt kilit ekranında "Biri zile bastı" ekranı
    QrCard.kt        QR kartı üretme + paylaşma
    Store.kt         kayıt ve ayarlar
docs/index.html      ziyaretçinin gördüğü sayfa (GitHub Pages)
.github/workflows/   APK'yı otomatik derleyen iş akışı
```
