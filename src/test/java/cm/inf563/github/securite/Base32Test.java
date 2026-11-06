package cm.inf563.github.securite;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Décodage Base32 (RFC 4648) des secrets 2FA")
class Base32Test {

    // Vecteurs officiels de la RFC 4648, section 10
    @ParameterizedTest(name = "\"{0}\" → \"{1}\"")
    @CsvSource({
            "MY======,         f",
            "MZXQ====,         fo",
            "MZXW6===,         foo",
            "MZXW6YQ=,         foob",
            "MZXW6YTB,         fooba",
            "MZXW6YTBOI======, foobar"
    })
    void vecteursDeLaRfc(String encode, String attendu) {
        assertArrayEquals(attendu.getBytes(StandardCharsets.US_ASCII), Base32.decoder(encode));
    }

    @Test
    @DisplayName("Chaîne vide → tableau vide")
    void chaineVide() {
        assertArrayEquals(new byte[0], Base32.decoder(""));
    }

    @Test
    @DisplayName("Minuscules, espaces et tirets acceptés (format affiché par GitHub)")
    void formatSaisiParUnHumain() {
        assertArrayEquals(Base32.decoder("MZXW6YTBOI"), Base32.decoder("mzxw 6ytb-oi"));
    }

    @Test
    @DisplayName("Caractère hors alphabet (1, 8, 0…) → refusé avec un message clair")
    void caractereInvalide() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> Base32.decoder("MZXW1"));
        assertTrue(e.getMessage().contains("'1'"));
    }

    @Test
    @DisplayName("null → NullPointerException explicite")
    void secretNull() {
        assertThrows(NullPointerException.class, () -> Base32.decoder(null));
    }
}
