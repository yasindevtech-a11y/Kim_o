package tr.zil.app;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECFieldFp;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPrivateKeySpec;
import java.security.spec.ECPublicKeySpec;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Uçtan uca şifreleme (ziyaretçi sayfası ve uygulama arasında).
 *
 * Her zilin bir EC P-256 anahtar çifti vardır:
 *  - genel anahtar (65 bayt, kapıdaki QR'da)  -> ziyaretçi sayfası mesajı buna şifreler
 *  - özel anahtar (32 bayt, aile/yedek kodunda) -> yalnızca ev sahibinin telefonları çözer
 *
 * Ziyaretçi -> ev sahibi  ("v2.<geçici genel anahtar>.<iv+şifreli>"):
 *   anahtar = SHA-256("kimo-v2|" + ECDH(geçici özel, zilin genel))   AES-256-GCM, AAD = zil kodu
 * Ev sahibi -> ziyaretçi  ("r2.<iv+şifreli>"):
 *   anahtar = ziyaretçinin gönderdiği oturum anahtarı (32 bayt)       AES-256-GCM, AAD = "zilkodu-ziyaretçikimliği"
 *
 * Bu dosya yalnızca standart Java sınıflarını kullanır; bilgisayarda da aynen derlenip test edilebilir.
 */
public final class Crypto {

    private Crypto() {
    }

    private static final SecureRandom RNG = new SecureRandom();
    private static final byte[] DOMAIN = "kimo-v2|".getBytes(StandardCharsets.UTF_8);
    private static ECParameterSpec params;

    private static synchronized ECParameterSpec p256() throws GeneralSecurityException {
        if (params == null) {
            KeyPairGenerator g = KeyPairGenerator.getInstance("EC");
            g.initialize(new ECGenParameterSpec("secp256r1"));
            params = ((ECPublicKey) g.generateKeyPair().getPublic()).getParams();
        }
        return params;
    }

    public static String b64u(byte[] b) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }

    public static byte[] unb64u(String s) {
        return Base64.getUrlDecoder().decode(s);
    }

    private static byte[] fixed32(BigInteger v) {
        byte[] b = v.toByteArray();
        byte[] out = new byte[32];
        if (b.length > 32) {
            System.arraycopy(b, b.length - 32, out, 0, 32);
        } else {
            System.arraycopy(b, 0, out, 32 - b.length, b.length);
        }
        return out;
    }

    private static byte[] rawPoint(ECPoint w) {
        byte[] out = new byte[65];
        out[0] = 4;
        System.arraycopy(fixed32(w.getAffineX()), 0, out, 1, 32);
        System.arraycopy(fixed32(w.getAffineY()), 0, out, 33, 32);
        return out;
    }

    private static ECPublicKey pubFromRaw(byte[] raw) throws GeneralSecurityException {
        if (raw.length != 65 || raw[0] != 4) {
            throw new InvalidKeyException("bad public key");
        }
        BigInteger x = new BigInteger(1, Arrays.copyOfRange(raw, 1, 33));
        BigInteger y = new BigInteger(1, Arrays.copyOfRange(raw, 33, 65));
        // Nokta gerçekten P-256 eğrisinin üzerinde olmalı (geçersiz eğri saldırısına karşı):  y² = x³ + ax + b  (mod p)
        ECParameterSpec ps = p256();
        BigInteger p = ((ECFieldFp) ps.getCurve().getField()).getP();
        BigInteger lhs = y.multiply(y).mod(p);
        BigInteger rhs = x.pow(3).add(ps.getCurve().getA().multiply(x)).add(ps.getCurve().getB()).mod(p);
        if (x.compareTo(p) >= 0 || y.compareTo(p) >= 0 || !lhs.equals(rhs)) {
            throw new InvalidKeyException("point not on curve");
        }
        return (ECPublicKey) KeyFactory.getInstance("EC")
                .generatePublic(new ECPublicKeySpec(new ECPoint(x, y), p256()));
    }

    private static ECPrivateKey privFromScalar(byte[] d) throws GeneralSecurityException {
        return (ECPrivateKey) KeyFactory.getInstance("EC")
                .generatePrivate(new ECPrivateKeySpec(new BigInteger(1, d), p256()));
    }

    /** {genel anahtar (65 bayt, base64url), özel anahtar (32 bayt, base64url)} */
    public static String[] newKeyPair() throws GeneralSecurityException {
        KeyPairGenerator g = KeyPairGenerator.getInstance("EC");
        g.initialize(new ECGenParameterSpec("secp256r1"));
        KeyPair kp = g.generateKeyPair();
        String pub = b64u(rawPoint(((ECPublicKey) kp.getPublic()).getW()));
        String priv = b64u(fixed32(((ECPrivateKey) kp.getPrivate()).getS()));
        return new String[]{pub, priv};
    }

    private static SecretKeySpec deriveKey(byte[] shared) throws GeneralSecurityException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(DOMAIN);
        md.update(shared);
        return new SecretKeySpec(md.digest(), "AES");
    }

    private static byte[] gcm(boolean encrypt, SecretKeySpec key, byte[] iv, byte[] aad, byte[] data)
            throws GeneralSecurityException {
        Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
        c.init(encrypt ? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, iv));
        c.updateAAD(aad);
        return c.doFinal(data);
    }

    /** Ziyaretçi mesajını çözer. Başarısızsa (yanlış anahtar, bozuk veri) null döner. */
    public static String decryptVisitor(String privB64, String topic, String message) {
        try {
            String[] parts = message.split("\\.");
            if (parts.length != 3 || !"v2".equals(parts[0])) {
                return null;
            }
            ECPublicKey eph = pubFromRaw(unb64u(parts[1]));
            byte[] box = unb64u(parts[2]);
            if (box.length < 12 + 16) {
                return null;
            }
            KeyAgreement ka = KeyAgreement.getInstance("ECDH");
            ka.init(privFromScalar(unb64u(privB64)));
            ka.doPhase(eph, true);
            SecretKeySpec key = deriveKey(ka.generateSecret());
            byte[] pt = gcm(false, key, Arrays.copyOfRange(box, 0, 12),
                    topic.getBytes(StandardCharsets.UTF_8), Arrays.copyOfRange(box, 12, box.length));
            return new String(pt, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }

    // ---------------------------------------------------------------- doğrulama

    private static boolean b64Len(String s, int chars) {
        if (s == null || s.length() != chars) {
            return false;
        }
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            boolean ok = (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '-' || c == '_';
            if (!ok) {
                return false;
            }
        }
        return true;
    }

    /** Zilin genel anahtarı (65 bayt = 87 karakter) geçerli bir P-256 noktası mı? */
    public static boolean isPub(String s) {
        try {
            if (!b64Len(s, 87)) {
                return false;
            }
            pubFromRaw(unb64u(s));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** Zilin özel anahtarı (32 bayt = 43 karakter) biçimce geçerli mi? */
    public static boolean isPriv(String s) {
        try {
            if (!b64Len(s, 43)) {
                return false;
            }
            byte[] d = unb64u(s);
            return d.length == 32 && new BigInteger(1, d).signum() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    /** Ziyaretçi oturum anahtarı (32 bayt = 43 karakter) biçimce geçerli mi? */
    public static boolean isSessionKey(String s) {
        try {
            return b64Len(s, 43) && unb64u(s).length == 32;
        } catch (Exception e) {
            return false;
        }
    }

    /** Genel ve özel anahtar aynı çifte mi ait? (içe aktarmadan önce sınanır) */
    public static boolean pairMatches(String pubB64, String privB64, String topic) {
        String boxed = encryptVisitor(pubB64, topic, "kimo");
        return boxed != null && "kimo".equals(decryptVisitor(privB64, topic, boxed));
    }

    // ---------------------------------------------------------------- şifreleme

    /**
     * Ziyaretçi sayfasının yaptığı işin aynısı: metni zilin genel anahtarına şifreler.
     * Uygulama bunu "Test" zili ve içe aktarma sınaması için kullanır. Başarısızsa null.
     */
    public static String encryptVisitor(String pubB64, String topic, String text) {
        try {
            ECPublicKey owner = pubFromRaw(unb64u(pubB64));
            KeyPairGenerator g = KeyPairGenerator.getInstance("EC");
            g.initialize(new ECGenParameterSpec("secp256r1"));
            KeyPair eph = g.generateKeyPair();
            KeyAgreement ka = KeyAgreement.getInstance("ECDH");
            ka.init(eph.getPrivate());
            ka.doPhase(owner, true);
            SecretKeySpec key = deriveKey(ka.generateSecret());
            byte[] iv = new byte[12];
            RNG.nextBytes(iv);
            byte[] ct = gcm(true, key, iv, topic.getBytes(StandardCharsets.UTF_8), text.getBytes(StandardCharsets.UTF_8));
            byte[] box = new byte[12 + ct.length];
            System.arraycopy(iv, 0, box, 0, 12);
            System.arraycopy(ct, 0, box, 12, ct.length);
            return "v2." + b64u(rawPoint(((ECPublicKey) eph.getPublic()).getW())) + "." + b64u(box);
        } catch (Exception e) {
            return null;
        }
    }

    /** Yanıtı çözer (ziyaretçi sayfasının yaptığı işin aynısı; sınama içindir). Başarısızsa null. */
    public static String decryptReply(String sessionKeyB64, String aad, String message) {
        try {
            if (message == null || !message.startsWith("r2.")) {
                return null;
            }
            byte[] sk = unb64u(sessionKeyB64);
            byte[] box = unb64u(message.substring(3));
            if (sk.length != 32 || box.length < 12 + 16) {
                return null;
            }
            byte[] pt = gcm(false, new SecretKeySpec(sk, "AES"), Arrays.copyOfRange(box, 0, 12),
                    aad.getBytes(StandardCharsets.UTF_8), Arrays.copyOfRange(box, 12, box.length));
            return new String(pt, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }

    /** Yanıtı ziyaretçinin oturum anahtarıyla şifreler. Başarısızsa null. */
    public static String encryptReply(String sessionKeyB64, String aad, String text) {
        try {
            byte[] sk = unb64u(sessionKeyB64);
            if (sk.length != 32) {
                return null;
            }
            byte[] iv = new byte[12];
            RNG.nextBytes(iv);
            byte[] ct = gcm(true, new SecretKeySpec(sk, "AES"), iv,
                    aad.getBytes(StandardCharsets.UTF_8), text.getBytes(StandardCharsets.UTF_8));
            byte[] box = new byte[12 + ct.length];
            System.arraycopy(iv, 0, box, 0, 12);
            System.arraycopy(ct, 0, box, 12, ct.length);
            return "r2." + b64u(box);
        } catch (Exception e) {
            return null;
        }
    }
}
