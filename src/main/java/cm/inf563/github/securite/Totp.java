package cm.inf563.github.securite;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.time.Clock;
import java.util.Objects;

/**
 * Générateur de codes à usage unique basés sur le temps (TOTP, RFC 6238),
 * identique à Google Authenticator : HMAC-SHA1, pas de 30 secondes.
 * <p>
 * L'horloge est injectée (cf. cours, séance 9 : « maîtriser le temps ») :
 * les tests utilisent {@code Clock.fixed(...)}, la production {@code Clock.systemUTC()}.
 */
public final class Totp {

    private static final long PAS_SECONDES = 30;
    private static final String ALGORITHME = "HmacSHA1";

    private final byte[] secret;
    private final Clock horloge;
    private final int chiffres;
    private final int modulo;

    public Totp(byte[] secret, Clock horloge) {
        this(secret, horloge, 6);
    }

    public Totp(byte[] secret, Clock horloge, int chiffres) {
        Objects.requireNonNull(secret, "secret");
        if (secret.length == 0) {
            throw new IllegalArgumentException("Le secret TOTP est vide");
        }
        if (chiffres < 6 || chiffres > 8) {
            throw new IllegalArgumentException("Un code TOTP a entre 6 et 8 chiffres, pas " + chiffres);
        }
        this.secret = secret.clone();
        this.horloge = Objects.requireNonNull(horloge, "horloge");
        this.chiffres = chiffres;
        this.modulo = (int) Math.pow(10, chiffres);
    }

    public static Totp depuisSecretBase32(String secretBase32, Clock horloge) {
        return new Totp(Base32.decoder(secretBase32), horloge);
    }

    public String codeCourant() {
        long compteur = horloge.instant().getEpochSecond() / PAS_SECONDES;
        byte[] empreinte = hmac(ByteBuffer.allocate(Long.BYTES).putLong(compteur).array());

        // Troncature dynamique (RFC 4226, section 5.3)
        int decalage = empreinte[empreinte.length - 1] & 0x0F;
        int binaire = ((empreinte[decalage] & 0x7F) << 24)
                | ((empreinte[decalage + 1] & 0xFF) << 16)
                | ((empreinte[decalage + 2] & 0xFF) << 8)
                | (empreinte[decalage + 3] & 0xFF);

        return String.format("%0" + chiffres + "d", binaire % modulo);
    }

    private byte[] hmac(byte[] message) {
        try {
            Mac mac = Mac.getInstance(ALGORITHME);
            mac.init(new SecretKeySpec(secret, ALGORITHME));
            return mac.doFinal(message);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HMAC-SHA1 indisponible dans cette JVM", e);
        }
    }
}
