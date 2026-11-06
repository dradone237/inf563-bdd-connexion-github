package cm.inf563.github.pages;

import cm.inf563.github.securite.Totp;
import cm.inf563.github.support.IdentifiantsGitHub;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.TimeoutError;
import com.microsoft.playwright.options.AriaRole;

import java.time.Clock;
import java.util.regex.Pattern;

/**
 * Page object de la connexion GitHub (cours, séance 12) : tous les sélecteurs
 * sont ici, les définitions d'étapes ne manipulent que des intentions.
 * Sélecteurs stables : identifiants HTML et rôles ARIA, jamais de XPath.
 */
public class PageConnexionGitHub {

    public static final String URL_ACCUEIL = "https://github.com/";
    public static final String URL_CONNEXION = URL_ACCUEIL + "login";
    private static final String URL_2FA_APPLICATION = URL_ACCUEIL + "sessions/two-factor/app";
    private static final String URL_DECONNEXION = URL_ACCUEIL + "logout";

    private final Page page;

    public PageConnexionGitHub(Page page) {
        this.page = page;
    }

    public void ouvrir() {
        page.navigate(URL_CONNEXION);
    }

    public Locator champIdentifiant() {
        return page.locator("#login_field");
    }

    public Locator champMotDePasse() {
        return page.locator("#password");
    }

    public Locator bouton(String libelle) {
        return page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(libelle).setExact(true));
    }

    public Locator lien(String libelle) {
        return page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName(libelle).setExact(true));
    }

    public Locator messageErreur() {
        // La page contient aussi un bandeau .flash-error caché (#ajax-error-message) : on l'exclut
        return page.locator(".flash-error:not(#ajax-error-message)");
    }

    /** GitHub publie l'utilisateur connecté dans {@code <meta name="user-login">} (vide si anonyme). */
    public Locator utilisateurConnecte() {
        return page.locator("meta[name='user-login']");
    }

    public void saisir(String identifiant, String motDePasse) {
        champIdentifiant().fill(identifiant);
        champMotDePasse().fill(motDePasse);
    }

    public void valider() {
        bouton("Sign in").click();
    }

    /** Contrainte HTML5 {@code required} non satisfaite : le navigateur bloque l'envoi. */
    public boolean estSignaleObligatoire(Locator champ) {
        return (Boolean) champ.evaluate("champ => champ.validity.valueMissing");
    }

    public void seConnecter(IdentifiantsGitHub identifiants, Clock horloge) {
        saisir(identifiants.identifiant(), identifiants.motDePasse());
        valider();
        page.waitForURL(url -> !url.endsWith("/login"));

        if (page.url().contains("/sessions/verified-device")) {
            throw new IllegalStateException("GitHub demande un code de vérification envoyé par e-mail. "
                    + "Activez la 2FA par application sur le compte et renseignez GH_TOTP_SECRET.");
        }
        if (page.url().contains("/sessions/two-factor")) {
            saisirCodeDoubleAuthentification(identifiants, horloge);
        }
    }

    private void saisirCodeDoubleAuthentification(IdentifiantsGitHub identifiants, Clock horloge) {
        String secret = identifiants.secretTotp().orElseThrow(() -> new IllegalStateException(
                "Le compte est protégé par la 2FA : définissez GH_TOTP_SECRET (clé Base32 de l'application)."));
        if (!page.url().startsWith(URL_2FA_APPLICATION)) {
            // La méthode par défaut peut être une clé de sécurité : on choisit l'application TOTP
            page.navigate(URL_2FA_APPLICATION);
        }
        page.locator("#app_totp").fill(Totp.depuisSecretBase32(secret, horloge).codeCourant());
        try {
            // GitHub valide le formulaire automatiquement dès le 6e chiffre
            page.waitForURL(url -> !url.contains("/sessions/two-factor"), new Page.WaitForURLOptions().setTimeout(5_000));
        } catch (TimeoutError pasDeValidationAutomatique) {
            bouton("Verify").click();
            page.waitForURL(url -> !url.contains("/sessions/two-factor"));
        }
    }

    public void seDeconnecter() {
        page.navigate(URL_DECONNEXION);
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(Pattern.compile("^Sign out"))).first().click();
        page.waitForURL(url -> !url.contains("/logout"));
    }
}
