package cm.inf563.github.securite;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Codes à usage unique TOTP (RFC 6238, HMAC-SHA1, pas de 30 s)")
class TotpTest {

    // Secret de référence de la RFC 6238 (annexe B) : "12345678901234567890" en ASCII
    private static final byte[] SECRET_RFC = "12345678901234567890".getBytes(StandardCharsets.US_ASCII);
    private static final String SECRET_RFC_BASE32 = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ";

    private static Clock horlogeA(long secondesEpoch) {
        return Clock.fixed(Instant.ofEpochSecond(secondesEpoch), ZoneOffset.UTC);
    }

    // Vecteurs de l'annexe B de la RFC 6238 (SHA1, 8 chiffres)
    @ParameterizedTest(name = "t = {0} s → {1}")
    @CsvSource({
            "59,          94287082",
            "1111111109,  07081804",
            "1111111111,  14050471",
            "1234567890,  89005924",
            "2000000000,  69279037",
            "20000000000, 65353130"
    })
    void vecteursDeLaRfcSurHuitChiffres(long t, String attendu) {
        assertEquals(attendu, new Totp(SECRET_RFC, horlogeA(t), 8).codeCourant());
    }

    // GitHub utilise 6 chiffres : ce sont les 6 derniers chiffres des vecteurs ci-dessus
    @ParameterizedTest(name = "t = {0} s → {1}")
    @CsvSource({
            "59,         287082",
            "1111111109, 081804",
            "1234567890, 005924"
    })
    void sixChiffresAvecZerosNonSignificatifs(long t, String attendu) {
        assertEquals(attendu, new Totp(SECRET_RFC, horlogeA(t)).codeCourant());
    }

    @Test
    @DisplayName("Bornes d'une fenêtre de 30 s : 30 et 59 → même code, 60 → code suivant")
    void fenetreDeTrenteSecondes() {
        String debut = new Totp(SECRET_RFC, horlogeA(30)).codeCourant();
        String fin = new Totp(SECRET_RFC, horlogeA(59)).codeCourant();
        String suivant = new Totp(SECRET_RFC, horlogeA(60)).codeCourant();
        assertEquals(debut, fin);
        assertNotEquals(fin, suivant);
    }

    @Test
    @DisplayName("Un secret Base32 (format GitHub) donne les mêmes codes que le secret brut")
    void depuisSecretBase32() {
        Totp totp = Totp.depuisSecretBase32(SECRET_RFC_BASE32, horlogeA(59));
        assertEquals("287082", totp.codeCourant());
    }

    @Test
    @DisplayName("Secret vide refusé")
    void secretVide() {
        assertThrows(IllegalArgumentException.class, () -> new Totp(new byte[0], horlogeA(59)));
    }

    @Test
    @DisplayName("Nombre de chiffres hors de [6, 8] refusé")
    void nombreDeChiffresInvalide() {
        assertThrows(IllegalArgumentException.class, () -> new Totp(SECRET_RFC, horlogeA(59), 5));
        assertThrows(IllegalArgumentException.class, () -> new Totp(SECRET_RFC, horlogeA(59), 9));
    }
}
