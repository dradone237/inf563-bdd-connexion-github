package cm.inf563.github.steps;

import cm.inf563.github.pages.PageConnexionGitHub;
import cm.inf563.github.support.IdentifiantsGitHub;
import cm.inf563.github.support.Navigateur;
import io.cucumber.java.Before;
import io.cucumber.java.fr.Alors;
import io.cucumber.java.fr.Etantdonné;
import io.cucumber.java.fr.Quand;
import org.junit.jupiter.api.Assumptions;

import java.time.Clock;
import java.util.Optional;
import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Définitions d'étapes : chaque phrase Gherkin est reliée à une action du page object.
 * Les assertions Playwright attendent automatiquement : aucun Thread.sleep.
 */
public class ConnexionSteps {

    private final Navigateur navigateur;
    private final Optional<IdentifiantsGitHub> identifiants = IdentifiantsGitHub.depuisEnvironnement();
    private PageConnexionGitHub connexion;

    public ConnexionSteps(Navigateur navigateur) {
        this.navigateur = navigateur;
    }

    @Before(value = "@compte-reel", order = 1)
    public void exigerLesIdentifiantsDuCompteReel() {
        // Sans identifiants, le scénario est ignoré (skipped) au lieu d'échouer
        Assumptions.assumeTrue(identifiants.isPresent(),
                "GH_LOGIN et GH_PASSWORD non définis : scénario sur le compte réel ignoré");
    }

    @Etantdonné("je suis sur la page de connexion de GitHub")
    public void jeSuisSurLaPageDeConnexion() {
        connexion = new PageConnexionGitHub(navigateur.page());
        connexion.ouvrir();
        assertThat(navigateur.page()).hasTitle(Pattern.compile("Sign in to GitHub"));
    }

    @Alors("je vois le champ identifiant")
    public void jeVoisLeChampIdentifiant() {
        assertThat(connexion.champIdentifiant()).isVisible();
        assertThat(connexion.champIdentifiant()).isEditable();
    }

    @Alors("je vois le champ mot de passe")
    public void jeVoisLeChampMotDePasse() {
        assertThat(connexion.champMotDePasse()).isVisible();
        assertThat(connexion.champMotDePasse()).hasAttribute("type", "password");
    }

    @Alors("je vois le bouton {string}")
    public void jeVoisLeBouton(String libelle) {
        assertThat(connexion.bouton(libelle)).isEnabled();
    }

    @Quand("je valide le formulaire sans rien saisir")
    public void jeValideLeFormulaireSansRienSaisir() {
        connexion.valider();
    }

    @Quand("je saisis l'identifiant {string} et le mot de passe {string}")
    public void jeSaisis(String identifiant, String motDePasse) {
        connexion.saisir(identifiant, motDePasse);
    }

    @Quand("je valide le formulaire")
    public void jeValideLeFormulaire() {
        connexion.valider();
    }

    @Alors("le champ identifiant est signalé comme obligatoire")
    public void identifiantObligatoire() {
        assertTrue(connexion.estSignaleObligatoire(connexion.champIdentifiant()),
                "le navigateur aurait dû bloquer l'envoi : identifiant vide");
    }

    @Alors("le champ mot de passe est signalé comme obligatoire")
    public void motDePasseObligatoire() {
        assertTrue(connexion.estSignaleObligatoire(connexion.champMotDePasse()),
                "le navigateur aurait dû bloquer l'envoi : mot de passe vide");
    }

    @Alors("je reste sur la page de connexion")
    public void jeResteSurLaPageDeConnexion() {
        assertThat(navigateur.page()).hasURL(PageConnexionGitHub.URL_CONNEXION);
    }

    @Alors("le message d'erreur {string} s'affiche")
    public void leMessageDErreurSAffiche(String message) {
        assertThat(connexion.messageErreur()).containsText(message);
    }

    @Quand("je clique sur le lien {string}")
    public void jeCliqueSurLeLien(String libelle) {
        connexion.lien(libelle).click();
    }

    @Alors("je suis sur la page de réinitialisation du mot de passe")
    public void pageDeReinitialisation() {
        assertThat(navigateur.page()).hasURL(Pattern.compile("github\\.com/password_reset"));
    }

    @Quand("je me connecte avec mes identifiants personnels")
    public void jeMeConnecte() {
        connexion.seConnecter(identifiants.orElseThrow(), Clock.systemUTC());
    }

    @Alors("je suis connecté à mon compte GitHub")
    public void jeSuisConnecte() {
        String attendu = identifiants.orElseThrow().identifiant();
        // GH_LOGIN peut être une adresse e-mail : on vérifie alors seulement qu'un utilisateur est connecté
        Pattern utilisateur = attendu.contains("@")
                ? Pattern.compile(".+")
                : Pattern.compile("^" + Pattern.quote(attendu) + "$", Pattern.CASE_INSENSITIVE);
        assertThat(connexion.utilisateurConnecte()).hasAttribute("content", utilisateur);
    }

    @Quand("je me déconnecte")
    public void jeMeDeconnecte() {
        connexion.seDeconnecter();
    }

    @Alors("je ne suis pas connecté")
    public void jeNeSuisPasConnecte() {
        assertThat(connexion.utilisateurConnecte()).hasAttribute("content", "");
    }
}
