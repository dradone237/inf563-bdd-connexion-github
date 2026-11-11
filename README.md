# INF563 · Devoir 1 – BDD en Java : connexion à mon compte GitHub

[![ci](https://github.com/dradone237/inf563-bdd-connexion-github/actions/workflows/ci.yml/badge.svg)](https://github.com/dradone237/inf563-bdd-connexion-github/actions/workflows/ci.yml)

Master 2 professionnel SIGL · Vérification et validation de logiciel · Enseignant : Evaris Fomekong
Séance de référence : **« Niveaux, doublures et TDD/BDD »**, complétée par **« Bout en bout web : Playwright et page objects »**.

## Énoncé

Implémenter **en Java** un test **BDD** de la connexion à mon compte GitHub (ou Facebook), et faire en
sorte que **chaque push déclenche les tests**.

## Résultat

| Élément | Résultat |
|---|---|
| Scénarios Gherkin (Cucumber + Playwright) contre le vrai site github.com | 6 réussis, 1 ignoré tant que les identifiants ne sont pas fournis |
| Tests unitaires écrits en TDD (JUnit 5) | 23 réussis |
| Intégration continue | GitHub Actions à chaque `push` et `pull request` |

Rapport complet : [`docs/Rapport_Devoir1_BDD_connexion_GitHub.pdf`](docs/Rapport_Devoir1_BDD_connexion_GitHub.pdf).

## Structure

```
.github/workflows/ci.yml                     pipeline : push → tests
src/main/java/cm/inf563/github/securite/     Base32, Totp : code de double authentification (écrit en TDD)
src/test/java/cm/inf563/github/
  securite/        Base32Test, TotpTest : vecteurs officiels des RFC 4648 et 6238
  pages/           PageConnexionGitHub : page object (tous les sélecteurs)
  steps/           ConnexionSteps : phrases Gherkin → actions et assertions
  support/         Navigateur (cycle de vie Playwright), IdentifiantsGitHub (variables d'environnement)
  RunCucumberTest  point d'entrée JUnit 5 de Cucumber
src/test/resources/features/connexion_github.feature
```

## Choix techniques et justification

| Choix | Justification (cours) |
|---|---|
| **Java 21, Maven, JUnit 5** | Environnement du cours. |
| **Gherkin en français** (`# language: fr`) exécuté par **Cucumber** | « La spécification exécutable » : la feature se lit comme le besoin ; une définition d'étape par phrase type. |
| **Playwright pour Java** | Outil E2E du cours ; ses assertions **attendent automatiquement** : aucun `Thread.sleep`. |
| **Page object** | Les sélecteurs vivent dans une seule classe ; sélecteurs stables (`id`, rôles ARIA), jamais de XPath. |
| **Un contexte de navigateur neuf par scénario** | Cookies et session vides : scénarios **isolés** et **reproductibles** (F.I.R.S.T.). |
| **Capture d'écran jointe au rapport en cas d'échec** | « Captures, vidéos et traces à chaque échec ». |
| **Rapports publiés même en cas d'échec** (`if: always()`) | Comme le pipeline de la séance 9. |

**Pourquoi GitHub plutôt que Facebook ?** Facebook bloque activement l'automatisation (« checkpoint »,
captcha, blocage du compte) : le test échouerait pour une raison extérieure, il serait **instable**. GitHub
offre un formulaire stable (`#login_field`, `#password`), des messages d'erreur fixes, et publie
l'utilisateur connecté dans `<meta name="user-login">` : c'est un **oracle** fiable de l'état de connexion.

## Scénarios

[`connexion_github.feature`](src/test/resources/features/connexion_github.feature)

| Scénario | Ce qui est vérifié |
|---|---|
| La page de connexion présente le formulaire complet | champ identifiant, champ mot de passe de type `password`, bouton « Sign in », personne n'est connecté |
| Un formulaire vide n'est pas envoyé | contrainte HTML5 `required` (`validity.valueMissing`) ; on reste sur `/login` |
| Un mot de passe vide n'est pas envoyé | même contrainte sur le mot de passe |
| Plan : connexion refusée avec des identifiants invalides (compte inexistant, e-mail inconnu) | message « Incorrect username or password. », toujours non connecté |
| Le lien « mot de passe oublié » mène à la réinitialisation | navigation vers `/password_reset` |
| `@compte-reel` Connexion réussie à mon compte puis déconnexion | `meta user-login` = mon identifiant, puis vide après la déconnexion |

## Identifiants et double authentification

Les identifiants ne sont **jamais écrits dans le code**. Ils sont lus dans les variables d'environnement :

| Variable | Rôle |
|---|---|
| `GH_LOGIN` | identifiant (ou e-mail) du compte |
| `GH_PASSWORD` | mot de passe |
| `GH_TOTP_SECRET` | seulement si la 2FA est activée : la clé Base32 affichée par GitHub sous le QR code (« setup key ») |

Sans `GH_LOGIN` et `GH_PASSWORD`, le scénario `@compte-reel` est marqué **ignoré** (et non en échec).
En CI, ces valeurs sont des *secrets* du dépôt, masqués dans les journaux. Le préfixe `GITHUB_` étant
réservé par GitHub Actions, les variables commencent par `GH_`.

## Partie TDD : le code de double authentification

Pour passer la double authentification sans intervention humaine, le projet calcule lui-même le code à
6 chiffres de l'application d'authentification (TOTP, RFC 6238 : HMAC-SHA1, pas de 30 s).
Ce code a été écrit **test d'abord** (voir l'historique des commits : tests rouges, puis implémentation) :

- **oracle** : les **vecteurs de test officiels** des RFC 4648 (Base32) et 6238 (TOTP) ;
- **valeurs limites** : bornes de la fenêtre de 30 s (t = 30 et t = 59 donnent le même code, t = 60 le suivant), zéros non significatifs (`005924`) ;
- **classes invalides** : caractère hors alphabet Base32, secret vide ou `null`, nombre de chiffres hors de [6, 8] ;
- **horloge injectée** (`Clock.fixed`) comme dans le cours : les tests ne dépendent pas de l'heure d'exécution.

## Intégration continue

[`.github/workflows/ci.yml`](.github/workflows/ci.yml) se déclenche à chaque `push` (toutes branches), à
chaque *pull request* et à la demande (`workflow_dispatch`) :

1. sur une machine **Ubuntu 24.04 figée** (et non `ubuntu-latest`, qui a changé d'Ubuntu sans prévenir et cassé
   l'installation de Chromium) : `actions/checkout`, puis `actions/setup-java` (Temurin 21, cache Maven) ;
2. installation de Chromium par le CLI Playwright **de la même version** que la bibliothèque Java ;
3. `mvn -B verify` : tests unitaires puis scénarios Cucumber ;
4. publication des rapports Cucumber (HTML, avec captures des échecs) et Surefire, même en cas d'échec.

## Lancer en local

Prérequis : JDK 21 ou plus, Maven 3.9 ou plus.

```bash
# Une seule fois : télécharger Chromium pour Playwright
mvn exec:java -e -Dexec.mainClass=com.microsoft.playwright.CLI -Dexec.args="install chromium" -Dexec.classpathScope=test

mvn verify                                  # tests TDD + scénarios BDD
mvn verify -Dheadless=false -DslowMo=400    # démonstration : navigateur visible et ralenti
```

Avec mon compte (PowerShell) :

```powershell
$env:GH_LOGIN="mon-identifiant"; $env:GH_PASSWORD="..."; $env:GH_TOTP_SECRET="..."; mvn verify
```

Rapport lisible : `target/cucumber-reports/cucumber.html`.

## Lacunes de spécification rencontrées

| Question | Décision |
|---|---|
| Les textes de GitHub dépendent de la langue du navigateur. | Les tests forcent `en-US` : le message attendu ne varie pas selon le poste. |
| La page contient deux bandeaux `.flash-error` (dont un caché). | L'oracle exclut le bandeau caché `#ajax-error-message`. |
| La connexion réelle peut exiger la 2FA ou une vérification par e-mail. | 2FA par application gérée (TOTP) ; vérification par e-mail détectée avec un message explicite. |
| `GH_LOGIN` peut être un e-mail, alors que GitHub affiche le nom d'utilisateur. | Avec un e-mail, on vérifie seulement qu'un utilisateur est connecté. |

## Limites

- Les scénarios dépendent du **vrai site GitHub** : réseau, évolution de la page, limitation après des
  échecs répétés depuis une même adresse IP. C'est le prix d'un vrai test de bout en bout ; ils restent peu
  nombreux (pyramide des tests) et chaque échec est accompagné d'une capture d'écran.
- Pour le scénario sur le compte réel en CI, un **compte GitHub de test** dédié est préférable au compte
  principal : moins de risque d'alerte de sécurité ou de blocage.
