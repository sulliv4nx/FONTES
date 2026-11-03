# Fonte — carnet d'entraînement pour Android

Appli de musculation hors ligne, en français, thème sombre. Aucune pub, aucun compte : les données restent sur le téléphone.

## Ce que fait l'appli

- **Séance** : séance libre ou depuis un modèle. Pour chaque exercice, tu notes kg × répétitions et tu coches ✓ chaque série terminée. Les charges de la dernière fois sont reprises automatiquement et affichées sous le nom de l'exercice.
- **Minuteur de repos** : il démarre dès que tu coches une série et vibre à la fin (+30 s / Passer). L'écran reste allumé pendant la séance.
- **Records en direct** : un message s'affiche quand une série bat ton record. Le bilan de fin de séance liste les records battus.
- **Programmes** (charges calculées depuis ton max et arrondies) :
  - **Smolov Jr** : 3 semaines × 4 séances (6×6 à 70 %, 7×5 à 75 %, 8×4 à 80 %, 10×3 à 85 %), avec un ajout de charge réglable en semaines 2 et 3, puis une séance de test du max.
  - **5/3/1 Wendler** : 4 semaines sur squat, développé couché, soulevé de terre et développé militaire, avec une série au maximum de répétitions et l'option « Boring But Big » (5×10).
- **Modèles** : enregistre n'importe quelle séance comme modèle pour la relancer en un geste.
- **Historique** : séances groupées par mois (durée, séries, tonnage), avec le détail, « Refaire cette séance » et la suppression.
- **Records** : par exercice, 1RM estimé (formule d'Epley), charge max, répétitions max et courbe de progression.
- **Calculateur de max** : 1RM estimé et tableau des charges de 50 à 100 %.
- **Bibliothèque** : 37 exercices (poussée, tirage, jambes, haltérophilie, CrossFit, strongman), et tu peux ajouter les tiens.
- **Réglages** : durée du repos, vibration, pas d'arrondi des charges, export et import d'une sauvegarde (JSON).

## Installer l'appli sur ton téléphone

Compter une vingtaine de minutes la première fois, surtout pour les téléchargements.

1. Installe **Android Studio** (gratuit) : https://developer.android.com/studio
2. Dézippe l'archive, puis dans Android Studio : **Open** et choisis le dossier `Fonte`. Attends la fin de la synchronisation Gradle : la première fois, il télécharge le SDK Android.
3. Sur le téléphone (Samsung) : **Paramètres › À propos du téléphone › Informations sur le logiciel**, puis touche 7 fois **Numéro de version**. Ensuite, dans **Paramètres › Options de développement**, active **Débogage USB**.
4. Branche le téléphone en USB et accepte l'autorisation affichée. Choisis ton téléphone en haut d'Android Studio, puis clique sur **▶ Run**. L'appli s'installe et se lance.

**Sans câble** : menu **Build › Build APK(s)** (selon la version : *Build › Generate App Bundles or APKs › Generate APKs*). Copie ensuite `app/build/outputs/apk/debug/app-debug.apk` sur le téléphone et ouvre-le. Il faut autoriser l'installation d'applis inconnues pour l'appli qui ouvre le fichier.

### Option : APK compilé par GitHub, sans Android Studio

Le fichier `.github/workflows/build.yml` compile l'appli automatiquement. Dépose le projet dans un dépôt GitHub. À chaque envoi, l'onglet **Actions** produit un APK téléchargeable dans **Artifacts** (`fonte-apk`).

## Structure du code

```
app/src/main/java/com/fonte/carnet/
├── core/   logique pure (modèle, programmes, calculs de records), testée
├── data/   stockage local JSON (écriture atomique) + export/import
└── ui/     écrans (activité unique, interface construite en code)
app/src/test/   tests unitaires de la logique (./gradlew test)
```

- Java, SDK Android uniquement (aucune bibliothèque externe). Android 8.0 minimum (API 26), cible API 34.
- Données : `fonte.json` dans le stockage privé de l'appli. Il est inclus dans la sauvegarde Android automatique.

## Personnaliser

| Quoi | Où |
|---|---|
| Nom de l'appli | `app/src/main/res/values/strings.xml` |
| Couleurs | `ui/Ui.java` (constantes `ACCENT`, `BG`…) et `res/values/colors.xml` |
| Exercices intégrés | `core/Library.java` (ne jamais réattribuer un id existant) |
| Programmes | `core/Programs.java` |
| Icône | `res/drawable/ic_launcher_foreground.xml` |

## Sécurité

Les programmes à pourcentages (Smolov Jr surtout) sont exigeants. Échauffe-toi, garde une technique propre et travaille avec un pareur ou des sécurités sur les charges lourdes.
