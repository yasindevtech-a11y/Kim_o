# Play Store'a yükleme rehberi (Kim_O)

Bu rehber, uygulamayı Google Play'e çıkarmak için yapılacakları sırayla anlatır.
Kodla ilgili her şey (API 36, AAB, dil, reklam onayı, gizlilik sayfası) hazır; aşağıdakilerin çoğu **sizin yapacağınız hesap işleridir**.

> Bu rehber hukuki tavsiye değildir. Veri ve gizlilik formlarını siz doldurur, doğruluğundan siz sorumlu olursunuz.

---

## 0) Yayınlamadan önce karar verilecek 3 şey

1. **Paket adı** (`applicationId`): şu an `tr.zil.app`. Play'de paket adı **bir kez seçilir, sonra değiştirilemez**.
   Daha benzersiz bir ad istiyorsanız (örn. `tr.kimo.app`) yayınlamadan **önce** `app/build.gradle.kts` içindeki
   `applicationId` satırını değiştirin. Değişirse telefondaki mevcut Kim_O ile yan yana ayrı bir uygulama olarak kurulur.
2. **Gizlilik sayfasındaki iletişim bilgisi:** `docs/gizlilik.html` içine ad (Yasin DEMİRGAN), e-posta (yasindev.tech@gmail.com)
   ve adres yazıldı. **Bu sayfa herkese açıktır; adres de herkese görünür.** Ev adresinizi göstermek istemiyorsanız
   dosyadaki iki `Adres:` satırını (Türkçe ve İngilizce bölümde birer tane) silin. Play Console geliştirici hesabı için ayrıca adres isteyecektir;
   mağazada hangi iletişim bilgilerinin herkese gösterileceği hesap türüne göre değişir, Console'daki açıklamaya bakın.
3. **Hesap türü:** Kişisel hesapta zorunlu test süreci var (aşağıda). Şirket/kurum hesabında yok.

## 1) Geliştirici hesabı

1. play.google.com/console → Geliştirici hesabı oluşturun. Tek seferlik **25 USD**.
2. Kimlik doğrulaması istenir (kimlik belgesi, adres, telefon). Birkaç gün sürebilir.
3. **Kişisel hesap ve 13 Kasım 2023'ten sonra açıldıysa:** yayına çıkmadan önce **en az 12 test kullanıcısıyla,
   14 gün kesintisiz kapalı test** yapmanız gerekir. Sonra Play Console'dan "üretim erişimi" başvurusu yaparsınız
   (Google genelde 7 gün içinde yanıtlar). Test kullanıcıları Gmail adresleri olmalı ve testten çıkmamalı.

## 2) Yükleme anahtarı (imza) — önemli

- GitHub'ın ürettiği **AAB** dosyası `Actions → son derleme → Artifacts → kimo-aab-play-store` içindedir.
- Anahtar tanımlamazsanız AAB, depodaki **herkese açık test anahtarıyla** imzalanır. Bunu Play'e *deneme için* yükleyebilirsiniz
  ama kalıcı kullanmayın: parolası depoda açıkça yazıdır.
- **Sizin için hazır bir yükleme anahtarı üretildi** (`upload.keystore`, adınıza, alias `upload`). Size ayrıca `GITHUB_SECRETS.txt`
  dosyası verildi: içindeki 4 değeri aşağıdaki tabloya göre GitHub'a ekleyin. Aşağıdaki `keytool` komutu yalnızca kendi anahtarınızı
  ayrıca üretmek isterseniz gereklidir.
- Kendi anahtarınızı üretmek isterseniz (bilgisayarda, bir kez):

  ```
  keytool -genkeypair -v -keystore upload.keystore -alias upload -keyalg RSA -keysize 2048 -validity 10000
  base64 -w0 upload.keystore        # macOS'ta: base64 -i upload.keystore
  ```
  Bilgisayarınız yoksa telefonda Termux ile de yapılabilir (`pkg install openjdk-17`, aynı keytool komutu).
- GitHub'da repo → **Settings → Secrets and variables → Actions → New repository secret** ile şu 4 gizli değeri ekleyin:

  | Ad | Değer |
  |---|---|
  | `UPLOAD_KEYSTORE_BASE64` | `base64` komutunun çıktısı (tek satır) |
  | `UPLOAD_STORE_PASSWORD` | keystore parolası |
  | `UPLOAD_KEY_ALIAS` | `upload` |
  | `UPLOAD_KEY_PASSWORD` | anahtar parolası |

  Bundan sonra AAB otomatik olarak bu anahtarla imzalanır. **`upload.keystore` dosyasını ve parolaları kaybetmeyin, repoya koymayın.**
- Play Console'da **Play Uygulama İmzalama (Play App Signing)** açıktır ve önerilir: asıl imza anahtarını Google saklar,
  siz yalnızca yükleme anahtarını kullanırsınız. Yükleme anahtarı kaybolursa Google üzerinden sıfırlanabilir.
- **Dikkat:** Play'den kurulan sürüm, GitHub'dan indirdiğiniz APK ile **farklı imzalıdır**; ikisi üst üste güncellenmez.
  Play'e geçince eski APK'yı silip Play sürümünü kurun.

## 3) Uygulamayı Play Console'da oluşturma

1. **Uygulama oluştur**: ad `Kim_O`, dil Türkçe, tür *Uygulama*, ücretsiz.
2. **Mağaza girişi:** metinler `store/tr.md` ve `store/en.md` dosyalarında hazır (kısa açıklama, uzun açıklama, etiketler).
   Görseller sizden: **512×512 simge**, **1024×500 öne çıkan görsel**, **en az 2 telefon ekran görüntüsü**.
3. **Gizlilik politikası URL'si:** `https://yasindevtech-a11y.github.io/Kim_o/gizlilik.html`
   (önce iletişim alanlarını doldurun, sayfanın açıldığını kontrol edin).
4. **Hedef kitle:** 18 yaş ve üzeri (çocuklara yönelik değil). Çocuklara yönelik seçerseniz AdMob kısıtlanır.
5. **İçerik derecelendirme** anketi: şiddet/cinsellik/kumar yok; kullanıcılar arası mesajlaşma (not) var.
6. **Reklamlar:** "Evet, uygulamam reklam içeriyor".
7. **Hesap silme:** uygulamada hesap yok, "hesap oluşturulmuyor" olarak işaretleyin.

## 4) Veri güvenliği formu (taslak cevaplar)

Uygulama hesap/rehber/konum/fotoğraf toplamaz; ama iki şey cihazdan dışarı çıkar:

| Veri | Neden | Ne yapılır |
|---|---|---|
| **Reklam kimliği / cihaz kimlikleri** (AdMob) | Reklam | *Toplanıyor, üçüncü taraflarla paylaşılıyor (Google AdMob)*, amaç: reklam, isteğe bağlı değil |
| **Yaklaşık konum / IP** (AdMob) | Reklam, kötüye kullanımı önleme | AdMob'un verdiği bilgiye göre işaretleyin |
| **Diğer uygulama içi mesajlar** (ziyaretçi notu ve ev sahibi yanıtı) | Zilin işlevi | *Toplanıyor* (ntfy.sh'a **uçtan uca şifreli** iletiliyor; geliştirici ve ntfy içeriği okuyamaz), *paylaşılmıyor* (hizmet sağlayıcı), amaç: uygulama işlevi |

- **Aktarım sırasında şifreleme:** Evet (HTTPS; notlar ve yanıtlar ayrıca uçtan uca şifreli).
- **Silme isteği yolu:** geliştiricide kullanıcı verisi tutulmadığı için "yok" / e-posta ile ulaşım.
- Formdaki cevaplar, Google AdMob'un güncel "veri açıklama" sayfasıyla ve gizlilik politikanızla **uyumlu** olmalıdır; tutarsızlık reddedilme sebebidir.

## 5) İzin ve özel kullanım beyanları — reddedilme riski burada

Kim_O arka planda sürekli bağlantı tuttuğu için Play inceleme ekibinin dikkatini çeken iki şeyi kullanıyor:

- **Ön plan hizmeti (özel kullanım / specialUse):** Console'da bir **beyan formu** ve genelde kısa bir **ekran videosu** ister.
  Gerekçe: "Kapı zilini ev sahibine anında iletmek için gelen bildirimleri sürekli dinler; kullanıcıya sürekli bildirim gösterilir."
- **Tam ekran bildirim (`USE_FULL_SCREEN_INTENT`):** Play bu izni yalnızca **arama ve alarm** uygulamalarına varsayılan olarak verir.
  Kapı zili "alarm benzeri" sayılmazsa izin verilmeyebilir. Bu durumda uygulama çökmez; zil çaldığında yine yüksek öncelikli
  bildirim ve ses gelir, yalnızca kilit ekranını kendiliğinden açma özelliği çalışmaz.

Dürüst değerlendirme: bu mimari (sürekli açık bağlantı, ntfy.sh) Play'de **onaylanabilir ama garanti değil**.
Reddedilirse en sağlam alternatif, bildirimi **Firebase Cloud Messaging (FCM)** ile iletmektir: kalıcı bildirim ve pil izni
gerekmez. Dezavantajı: Google'ın bildirim sunucusu devreye girer ve bir küçük aracı sunucu gerekir.
Gerekirse bu geçişi birlikte yaparız.

## 6) AdMob (reklam)

1. admob.google.com → uygulamanız ekli (Kim_O, Android) ve bir **banner reklam birimi** oluşturulmuş.
2. Şu an `admob.properties` içinde **`USE_TEST=true`**: Google'ın TEST reklamları gösterilir. Geliştirirken böyle kalsın;
   kendi gerçek reklamınıza tıklamak hesabınızın kapanmasına yol açabilir.
3. Uygulama Play'de yayınlanınca:
   - AdMob'da uygulamayı **Play Store sayfasına bağlayın**.
   - AdMob → Uygulamalar → Kim_O → **Uygulama kimliği**ni kopyalayın (`ca-app-pub-…~…`, içinde `~` var).
   - GitHub'da `admob.properties` dosyasını düzenleyin: `APP_ID=` satırına yapıştırın, `USE_TEST=false` yapın.
   - Yeni APK/AAB derlenir; artık gerçek reklam gelir.
4. **Onay (UMP):** AB/İngiltere/İsviçre'de yayınlayacaksanız AdMob → **Gizlilik ve mesajlaşma** bölümünde bir
   *Avrupa düzenlemeleri* onay mesajı oluşturun ve yayınlayın. Yalnızca Türkiye'de yayınlıyorsanız şart değildir.
   Uygulama onay formunu zaten gösterecek şekilde hazırdır.

## 7) Sürüm yükleme sırası

1. Console → **Test et ve yayınla → Kapalı test** → yeni sürüm → `app-release.aab` yükleyin.
2. 12 test kullanıcısını ekleyin, davet bağlantısını gönderin, 14 gün bekleyin.
3. **Üretim erişimi için başvurun** → onaylanınca **Üretim → Yeni sürüm** olarak yayınlayın.
4. Sonraki güncellemelerde `app/build.gradle.kts` içinde `versionCode` ve `versionName` değerlerini artırmayı unutmayın.

## Kontrol listesi

- [ ] Paket adı kesinleşti
- [ ] `gizlilik.html` açılıyor; ad/e-posta/adres doğru (adresi göstermek istemiyorsanız satırları silin)
- [x] Yükleme anahtarı üretildi (`upload.keystore`, `GITHUB_SECRETS.txt`) ve güvenli yere yedeklendi
- [ ] 4 gizli değer GitHub → Settings → Secrets and variables → Actions'a eklendi
- [ ] AdMob uygulaması Play sayfasına bağlandı, `admob.properties` güncellendi
- [ ] Simge, öne çıkan görsel, ekran görüntüleri hazır
- [ ] Veri güvenliği formu, içerik derecelendirme, hedef kitle dolduruldu
- [ ] Ön plan hizmeti beyanı ve (gerekirse) video yüklendi
- [ ] 12 kullanıcı × 14 gün kapalı test tamamlandı
