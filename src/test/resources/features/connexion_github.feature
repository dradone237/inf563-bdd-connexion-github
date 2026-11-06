# language: fr
@github
Fonctionnalité: Connexion à mon compte GitHub
  Afin d'accéder à mes dépôts et de pousser mon code
  En tant que propriétaire d'un compte GitHub
  Je veux me connecter avec mon identifiant et mon mot de passe
  et que toute tentative avec de mauvais identifiants soit refusée

  Contexte:
    Soit je suis sur la page de connexion de GitHub

  Scénario: La page de connexion présente le formulaire complet
    Alors je vois le champ identifiant
    Et je vois le champ mot de passe
    Et je vois le bouton "Sign in"
    Et je ne suis pas connecté

  Scénario: Un formulaire vide n'est pas envoyé
    Quand je valide le formulaire sans rien saisir
    Alors le champ identifiant est signalé comme obligatoire
    Et je reste sur la page de connexion
    Et je ne suis pas connecté

  Scénario: Un mot de passe vide n'est pas envoyé
    Quand je saisis l'identifiant "inf563-etudiant" et le mot de passe ""
    Et je valide le formulaire
    Alors le champ mot de passe est signalé comme obligatoire
    Et je reste sur la page de connexion

  Plan du scénario: Connexion refusée avec des identifiants invalides (<cas>)
    Quand je saisis l'identifiant "<identifiant>" et le mot de passe "<mot de passe>"
    Et je valide le formulaire
    Alors le message d'erreur "Incorrect username or password." s'affiche
    Et je ne suis pas connecté

    Exemples:
      | cas                    | identifiant                        | mot de passe    |
      | compte inexistant      | inf563-compte-inexistant-bdd-2026  | MotDePasse!2026 |
      | adresse e-mail inconnue | inf563.inconnu@example.invalid     | MotDePasse!2026 |

  Scénario: Le lien « mot de passe oublié » mène à la réinitialisation
    Quand je clique sur le lien "Forgot password?"
    Alors je suis sur la page de réinitialisation du mot de passe

  # Ce scénario utilise MON vrai compte : identifiants lus dans les variables
  # d'environnement GH_LOGIN, GH_PASSWORD et (si 2FA activée) GH_TOTP_SECRET.
  # Sans ces variables, il est marqué « ignoré » (skipped) et non en échec.
  @compte-reel
  Scénario: Connexion réussie à mon compte puis déconnexion
    Quand je me connecte avec mes identifiants personnels
    Alors je suis connecté à mon compte GitHub
    Quand je me déconnecte
    Alors je ne suis pas connecté
