package cm.inf563.github.support;

import java.util.Optional;

/**
 * Identifiants du vrai compte GitHub, jamais écrits dans le code :
 * lus dans les variables d'environnement (secrets GitHub Actions en CI).
 * <p>
 * GH_LOGIN et GH_PASSWORD sont obligatoires pour le scénario @compte-reel ;
 * GH_TOTP_SECRET (clé Base32 de l'application d'authentification) seulement si la 2FA est activée.
 * Le préfixe GITHUB_ est réservé par GitHub Actions, d'où GH_.
 */
public record IdentifiantsGitHub(String identifiant, String motDePasse, Optional<String> secretTotp) {

    public static Optional<IdentifiantsGitHub> depuisEnvironnement() {
        String identifiant = lire("GH_LOGIN");
        String motDePasse = lire("GH_PASSWORD");
        if (identifiant == null || motDePasse == null) {
            return Optional.empty();
        }
        return Optional.of(new IdentifiantsGitHub(identifiant, motDePasse, Optional.ofNullable(lire("GH_TOTP_SECRET"))));
    }

    private static String lire(String nom) {
        String valeur = System.getenv(nom);
        if (valeur == null || valeur.isBlank()) {
            valeur = System.getProperty(nom);
        }
        return valeur == null || valeur.isBlank() ? null : valeur.trim();
    }

    @Override
    public String toString() {
        return "IdentifiantsGitHub[" + identifiant + ", mot de passe masqué]";
    }
}
