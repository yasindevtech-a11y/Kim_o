# 🔔 Kim_O – QR kodlu kapı zili

Kapıya QR kod yapıştırırsınız. Ziyaretçi okutunca tarayıcıda **"Zile Bas"** butonu açılır
(uygulama kurmaz). Butona basınca sizin telefonunuzda uygulama zil sesi çalar ve ekranda
**"Biri zile bastı"** uyarısı çıkar. Ziyaretçi sayfası 3 dakika sonra kendini siler.

```
Ziyaretçi telefonu ──► docs/index.html (GitHub Pages) ──► ntfy.sh ──► Sizin telefondaki Kim_O uygulaması
```

Firebase yok, kendi sunucunuz yok, hesap yok. Ücretsizdir (ana ekranda küçük bir AdMob banner reklamı vardır; ayrıntı aşağıda).

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
1. Repoda **Actions** sekmesi → **APK ve AAB derle** → **Run workflow** (ilk yüklemede kendiliğinden de başlar).
2. 5–8 dakika sonra yeşil tik çıkar.
3. Repo ana sayfasında sağdaki **Releases** bölümüne girin → en yeni **Kim_O vN** → `app-debug.apk` dosyasını telefonunuzdan indirin.
   (Play Store paketi olan `.aab` dosyası aynı derlemenin **Artifacts → kimo-aab-play-store** bölümündedir.)
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

## Not bırakma ve yanıt (iki yönlü, uçtan uca şifreli)

- Ziyaretçi sayfasında zile basmadan önce isteğe bağlı kısa bir **not** yazılabilir (en fazla 200 karakter, örn. "Kargo getirdim").
- Not, zil çalarken ekranda ve bildirimde görünür.
- Ev sahibi aynı ekrandan **hazır yanıt** (Geliyorum / Bir dakika bekleyin / Evde yokuz) ya da kendi yazdığı yanıtı gönderebilir.
  Yanıt, ziyaretçinin açık sayfasında anında görünür.
- **Not ve yanıtlar şifrelidir.** Ziyaretçi sayfası notu, kapıdaki QR'ın içindeki *genel anahtara* şifreler; yalnızca ev sahibinin
  telefonundaki *özel anahtar* çözer. Yanıt ise ziyaretçinin o oturum için ürettiği tek kullanımlık anahtarla şifrelenir.
  ntfy.sh, GitHub ve siz (geliştirici) içeriği okuyamazsınız. (AES-256-GCM + P-256 ECDH, tarayıcının kendi WebCrypto'su.)
- Her ziyaretçiye ayrı rastgele bir kimlik verilir. Yanıt yalnızca o ziyaretçinin sayfasına gider; aynı QR'ı okutan başka biri göremez,
  sahte yanıt da gönderemez (şifreyi çözemeyen yanıtlar sayfada gösterilmez).
- ntfy yine de şunları görür: rastgele zil kodu, rastgele ziyaretçi kimliği, mesajın zamanı/boyutu, IP. İçerik görünmez.

### Eski QR kodlar
Sürüm 1.7'den önce basılmış QR'lar genel anahtar içermez. Okutulunca zil **yine çalar**, ama not ve yanıt kapalıdır.
Not/yanıtı açmak için zilin **QR / Paylaş** ekranındaki yeni QR'ı yazdırın (zil kodu aynıdır, eski QR çalışmaya devam eder).

## Aile ve yedek kodu (aynı zili birkaç telefonda kullanma, telefon sıfırlanınca geri yükleme)

Kapıdaki QR **herkese açıktır**, bu yüzden içinde özel anahtar yoktur. Zili başka bir telefona taşımak için ayrı, **gizli** bir kod vardır:

1. Zilin **QR / Paylaş** ekranında **🔐 Aile / yedek kodu (gizli)** düğmesine basın. Kırmızı şeritli ayrı bir QR kartı açılır.
2. **WhatsApp ile gönder** ile aile bireylerine yollayın. Kendiniz için de saklayın (WhatsApp'ta "Kendime" sohbeti ya da galeri).
   Kapıya **yapıştırmayın**; kapıya yapıştırılacak olan, turuncu şeritli "Zile basmak için okutun" kartıdır.
3. Diğer telefonda Kim_O'yu kurun, ana ekranda **İçe aktar (aile / yedek kodu)**'na basın ve şunlardan birini yapın:
   - **Galeriden QR görseli seç** (WhatsApp'tan gelen görseli galeriye kaydedip seçin),
   - ya da bağlantıyı **Panodan yapıştır**,
   - ya da QR'ı telefonun kamerasıyla okutun → açılan sayfadaki **Kim_O uygulamasına ekle** düğmesine basın.
4. Onay penceresinde **Ekle**'ye basın. Zil o telefonda da dinlenir; ayrı bir hesap ya da kayıt gerekmez.

Telefon sıfırlanırsa aynı yolla zil, **aynı QR ile geri yüklenir**: kapıdaki QR değişmez, tekrar basmanıza gerek kalmaz.

Önemli:
- Aile kodunu alan herkes zili kendi telefonunda dinleyebilir ve yanıt verebilir. Yalnızca güvendiklerinize gönderin.
- Galeriye kaydedilen görsel, telefonunuzun fotoğraf yedeğine (örn. Google Fotoğraflar) girebilir. Bunu istemiyorsanız WhatsApp'ta "Kendime" sohbetinde saklayın.
- Kod yanlış ellere geçerse **Düzenle → Kodu yenile**: yeni zil kodu + yeni anahtarlar üretilir; eski QR ve eski aile kodları çalışmaz olur.
- Aile kodu olmadan (yalnızca kapıdaki QR ile) zil devralınamaz; bu bilerek böyledir.

## Sayfa açılmıyorsa (404 hatası)

QR okutunca "404" çıkıyorsa GitHub Pages henüz açık değildir:
1. Repoda **Settings → Pages** bölümüne girin.
2. **Source**: *Deploy from a branch*, **Branch**: `main`, klasör **/docs** seçip **Save** deyin.
3. 1–2 dakika bekleyin, sonra sayfa adresini tarayıcıda doğrudan açıp deneyin
   (`https://KULLANICI.github.io/REPO/`). Açıldığında "Zil" sayfası görünmelidir; bağlantı kodu olmadan açılırsa
   "Geçersiz bağlantı" yazması normaldir.
4. Repo **Public** olmalıdır (ücretsiz hesapta Pages için gereklidir).

## Ayarlar ve özellikler

- **Zil sesi (zil başına ayrı):** Zili düzenlerken **Sesi değiştir** → Klasik zil, Ding-dong, Çan, Melodi, telefonun zil sesi
  ya da **Telefondan seç…**. Seçince 4 saniyelik önizleme çalar.
- **Dil:** Ayarlar → Dil: Otomatik (telefon Türkçeyse Türkçe, değilse İngilizce), Türkçe, English.
  Ziyaretçi sayfası da ziyaretçinin telefon diline göre açılır. Ev sahibinin hazır yanıtları ziyaretçinin dilinde gider.
- **Kalıcı bildirim:** Android, arka planda dinleyen uygulama için bir bildirim göstermek zorundadır; bunu tamamen
  kaldırmak mümkün değil. Kim_O bunu en sessiz seviyede gösterir (durum çubuğunda simge yok, kilit ekranında gizli).
  Ayarlar → Bildirim → **Kalıcı bildirimi gizle / ayarla** ile Android'in kendi ayarından tamamen kapatabilirsiniz; zil yine çalışır.
- **Gizlilik politikası:** Ayarlar → Gizlilik ve reklamlar. Sayfa `docs/gizlilik.html`; ad, e-posta ve adres bilgileriniz içine yazılıdır.
  Ev adresinizin herkese açık bir sayfada durmasını istemiyorsanız o dosyadaki iki `Adres:` satırını silin.
- **Gelişmiş ayarlar:** sayfa adresi ve ntfy sunucusu burada, varsayılan olarak gizlidir.

## Reklam (AdMob)

- Ana ekranın altında küçük bir banner vardır; zil uyarı ekranında ve ziyaretçi sayfasında reklam yoktur.
- Reklamdan önce Google'ın onay formu (UMP) gösterilir (gereken bölgelerde).
- `admob.properties` dosyasında şu an `USE_TEST=true`: **test reklamları** görünür. Play'de yayınlandıktan sonra
  `APP_ID` yazıp `USE_TEST=false` yapın. Adımlar: [PLAY_STORE.md](PLAY_STORE.md) bölüm 6.

## Play Store

Hedef API **36**'dır (Play'in Ağustos 2026'dan beri istediği düzey). Yükleme adımları, imza anahtarı, veri güvenliği formu ve
dürüst risk değerlendirmesi için: **[PLAY_STORE.md](PLAY_STORE.md)**. Mağaza metinleri: `store/tr.md`, `store/en.md`.

## Başkaları kullanınca (veri ve gizlilik)

- Her kullanıcı APK'yı kurar, kendi zillerini ekler ve adlandırır (Ev kapısı, Araba, Köydeki ev…).
- Her zilin kodu o telefonda rastgele üretilir ve **yalnızca o telefonda saklanır**. Kullanıcılar birbirinden bağımsızdır.
- Sizin bir sunucunuz ve veritabanınız yoktur; zil adı, konum, kişi bilgisi toplanmaz, geliştiriciye gönderilmez.
  **İstisna:** reklam gösteren Google AdMob, kendi politikasına göre reklam kimliği gibi verileri işler (gizlilik sayfasında yazılıdır).
- Ortak tek parça `docs/index.html` sayfasıdır. Herkese aynıdır, hiçbir şey kaydetmez. Zil kodu adresin `#` kısmında durur,
  bu kısım sayfayı barındıran sunucuya gitmez.
- Bildirimi taşıyan ntfy sunucusu rastgele zil kodunu, rastgele ziyaretçi kimliğini, mesajın zamanını/boyutunu görür; not ve yanıtlar şifreli olduğundan içeriği göremez.
- Ziyaretçinin IP adresi, her web sitesinde olduğu gibi, sayfayı ve ntfy'yi barındıran sunuculara düşer.
  Uygulamanızı Play Store'da yayınlarsanız KVKK açısından bir hukukçuya danışın.

## Bilmeniz gerekenler (dürüst notlar)

- **İki telefonda da internet gerekir.** Ziyaretçide mobil veri, sizde Wi-Fi veya mobil veri.
- **3 dakika kuralı ziyaretçinin tarayıcısında çalışır.** Süre dolunca buton kaybolur, adres çubuğundaki
  zil kodu ve saklanan süre silinir; yenileyince çalışmaz, QR'ı tekrar okutmak gerekir.
  Ancak QR'ı çok kez okutan biri yeniden süre alır, bunu sunucusuz engelleyemeyiz.
- **Mesajlar ntfy.sh üzerinden geçer, ama not ve yanıtlar şifrelidir.** ntfy.sh şifreli mesajı yaklaşık 12 saat saklayabilir;
  içeriği okuyamaz. Zil kodu, ziyaretçi kimliği ve "bir mesaj geldi" bilgisi gizlenemez. Uygulama 3 dakikadan eski mesajları yok sayar.
- **Zil kodu tahmin edilemez** (rastgele 16 karakter) ama kapıdaki QR'ı gören herkes zili çalabilir; bu zaten amaç.
  Başkalarının notunu/yanıtını okuyamazlar. Biri kötüye kullanırsa **Düzenle → Kodu yenile** yapın. Aynı zilde art arda basışlar 10 saniye içinde tek sayılır.
- **Ses alarm sesi seviyesinden çalar.** Telefonun *alarm sesi* kapalıysa zil duyulmaz; bir kez kontrol edin.
- **Bazı telefonlar (Xiaomi, Huawei, Oppo, Samsung…)** arka plan uygulamalarını agresif kapatır.
  Uygulamayı "otomatik başlat" listesine ekleyin, son uygulamalarda kilitleyin. Ayrıntı: dontkillmyapp.com
- **Güncelleme:** yeni APK'yı eskisinin üzerine kurabilirsiniz (imza sabit, `app/zil.keystore`).
  Repo herkese açık olduğundan bu anahtar da görünür; kişisel kullanım için sorun değil.
- Kendi ntfy sunucunuz varsa **Ayarlar → Gelişmiş ayarlar → ntfy sunucusu** alanına yazabilirsiniz.

## Klasör yapısı
```
app/                 Android uygulaması (Kotlin, ek kütüphane: ZXing QR)
  src/main/java/tr/zil/app/
    MainActivity.kt  zil listesi, ekle/düzenle/sil, QR ve WhatsApp paylaşımı, aile kodu, içe aktar
    Crypto.java      uçtan uca şifreleme (P-256 ECDH + AES-256-GCM); ziyaretçi sayfasıyla birebir uyumlu
    QrReader.kt      galerideki QR görselini okur (içe aktarma)
    ListenService.kt arka plan dinleme, ses, bildirim
    AlertActivity.kt kilit ekranında "Biri zile bastı" ekranı
    QrCard.kt        QR kartı üretme + paylaşma
    Store.kt         kayıt ve ayarlar
    Sounds.kt        zil sesi seçimi ve önizleme
    L.kt             Türkçe / İngilizce çeviri
    Ads.kt           AdMob banner + onay formu
    LauncherActivity.kt / CrashActivity.kt   açılış kapısı ve hata raporu
docs/index.html      ziyaretçinin gördüğü sayfa (GitHub Pages)
docs/gizlilik.html   gizlilik politikası (Türkçe + English)
admob.properties     reklam ayarları (test/gerçek)
store/               Play Store mağaza metinleri
PLAY_STORE.md        Play Store yükleme rehberi
.github/workflows/   APK'yı otomatik derleyen iş akışı
```
