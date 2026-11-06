package cm.inf563.github.securite;

import java.io.ByteArrayOutputStream;
import java.util.Locale;
import java.util.Objects;

/**
 * Décodeur Base32 (RFC 4648), le format dans lequel GitHub affiche la clé
 * secrète d'une application d'authentification (2FA).
 */
public final class Base32 {

    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

    private Base32() {
    }

    public static byte[] decoder(String texte) {
        Objects.requireNonNull(texte, "texte");
        String propre = texte.replaceAll("[\\s=-]", "").toUpperCase(Locale.ROOT);

        ByteArrayOutputStream octets = new ByteArrayOutputStream();
        int tampon = 0;
        int bitsDisponibles = 0;
        for (char c : propre.toCharArray()) {
            int valeur = ALPHABET.indexOf(c);
            if (valeur < 0) {
                throw new IllegalArgumentException("Caractère Base32 invalide : '" + c + "'");
            }
            tampon = (tampon << 5) | valeur;
            bitsDisponibles += 5;
            if (bitsDisponibles >= 8) {
                bitsDisponibles -= 8;
                octets.write((tampon >> bitsDisponibles) & 0xFF);
                tampon &= (1 << bitsDisponibles) - 1;
            }
        }
        return octets.toByteArray();
    }
}
