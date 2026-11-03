# Fonte 3.0 — sport, nutrition et suivi corporel pour Android

Appli d'entraînement hors ligne, en français, thème sombre. Aucune pub, aucun compte : les données restent sur le téléphone.

## Onglets

**Accueil · Sport (Muscu, CrossFit, Renfo) · Nutrition · Corps · Progrès (historique et records)**

### Accueil
Le récapitulatif du jour : calories restantes et macros, hydratation, calories dépensées, séance du jour ou programme, poids et sa courbe, semaine d'activité et derniers records.

### Nutrition
- **Besoins calculés** selon le profil (Mifflin-St Jeor) : objectif calorique (perte, maintien ou prise de masse), protéines et lipides en g/kg, glucides pour le reste, eau (35 ml/kg, plus 500 ml les jours d'entraînement).
- **Journal par repas** (petit-déjeuner, déjeuner, dîner, collations) : base d'environ 95 aliments courants, aliments récents, aliments perso.
- **Code-barres** : la photo du code est lue sur le téléphone, puis la fiche produit vient d'Open Food Facts (connexion nécessaire).
- **Étiquette** : la photo du tableau nutritionnel est lue sur le téléphone, et les valeurs sont pré-remplies.
- **Photo du repas** : jointe au journal. Elle ne calcule pas les calories : il faudrait un service d'IA en ligne.
- **Calories dépensées** : estimées (MET) à partir des séances, WOD et circuits, plus les activités ajoutées (course, vélo…) ou les calories de ta montre.

### Corps
Poids (courbe, variations sur 7 et 30 jours, IMC), mensurations (taille, hanches, poitrine, épaules, bras, cuisse, mollet, cou, masse grasse) avec historique, composition corporelle (masse grasse mesurée ou estimée par la méthode de l'US Navy).


### Muscu
- **Séance libre** ou lancée depuis une séance type, un modèle, une proposition ou la programmation du coach. Tu notes kg × répétitions et tu coches ✓ chaque série terminée. Les charges de la dernière fois sont reprises automatiquement.
- **Minuteur de repos** : il démarre quand tu coches une série et vibre à la fin. L'écran reste allumé pendant la séance.
- **Séances types** : full body A/B, haut/bas, push/pull/legs, bras, fessiers, épaules & abdos, force 5×5, strongman.
- **Séance par muscle** : tu choisis les muscles (pectoraux, dos, épaules, bras, jambes, abdos…), l'objectif (force, volume, endurance) et ton temps disponible, et l'appli propose une séance.
- **Programmation du coach** : prends une photo (tableau blanc, feuille, capture d'écran), choisis une image ou colle le texte. L'appli repère exercices, séries, répétitions, charges (kg ou % du max) et le WOD éventuel.
- **Programmes** : Smolov Jr et 5/3/1, avec des charges calculées depuis ton max.

### CrossFit
- **Chrono** : AMRAP (avec compteur de tours), EMOM / E2MOM, For Time (avec cap), Tabata et intervalles. Compte à rebours de départ, bips sur les 3 dernières secondes et à chaque changement de phase, vibrations.
- **WOD Heroes** : Murph, DT, JT, Michael, Nate, Randy, Tommy V, Badger, Josh, Daniel, Jason, Ryan, Mr. Joshua, Danny, Hansen, Luce, McGhee, The Seven, Whitten, Bull, Kalsu, Glen, Lumberjack 20…
- **The Girls** : Fran, Grace, Isabel, Helen, Cindy, Annie, Karen, Diane, Elizabeth, Jackie, Nancy, Amanda, Kelly, Eva, Barbara, Chelsea, Mary, Angie, Nicole.
- **WOD par objectif** : cardio, gymnastique, haltérophilie, force, jambes, haut du corps, sans matériel, court & intense, long & endurance.
- **Mes WOD** : crée les WOD de ta box. Le chrono adapté et l'enregistrement des scores (temps, tours + reps, RX ou adapté) sont inclus, avec ton record sur chaque WOD.

### Renfo
Circuits guidés : gainage & abdos, épaules (coiffe des rotateurs, mobilité), genoux & chevilles, dos & posture, hanches & fessiers, mobilité, full body, prévention course. L'appli enchaîne les exercices, les temps et les pauses ; les exercices en répétitions attendent que tu touches « Fait ✓ ».

### Historique et Records
Historique unifié (muscu, WOD, renfo), filtrable. Records : meilleurs scores de WOD, puis, par exercice, 1RM estimé, charge max, répétitions max et courbe de progression. Le calculateur de max est aussi dans Records.

## Obtenir l'APK

Dépose `Fonte-projet-android.zip` à la racine du dépôt GitHub (**Add file › Upload files**, puis **Commit changes**). Le workflow `import.yml` décompresse le projet, l'enregistre dans le dépôt et compile l'APK. Quand l'onglet **Actions** affiche une coche verte, le fichier `fonte-apk` est dans **Artifacts**. Décompresse-le et installe `app-debug.apk`. L'installation par-dessus la version précédente conserve tes données.

Avec Android Studio : **Open** sur le dossier du projet, puis **▶ Run** avec le téléphone branché en USB (débogage USB activé).

## Structure du code

```
app/src/main/java/com/fonte/carnet/
├── core/   logique pure, testée : modèle, programmes, chrono (Clock), WOD, circuits,
│           séances types, générateur par muscle, lecture de programmation (CoachParser)
├── data/   stockage local JSON (écriture atomique) + export/import
└── ui/     écrans (activité unique, interface construite en code)
app/src/test/   tests unitaires (./gradlew test)
```

- Java, SDK Android et ML Kit (lecture de texte et de codes-barres sur le téléphone, sans connexion). Android 8.0 minimum (API 26), cible API 34.
- Seule connexion : la recherche d'un code-barres sur Open Food Facts (aucun compte, aucune autre donnée envoyée).
- Données : `fonte.json` dans le stockage privé de l'appli. Il est inclus dans la sauvegarde Android automatique et peut être exporté depuis Réglages.

## Sécurité

Les WOD Heroes et les programmes à pourcentages sont exigeants. Échauffe-toi, adapte charges et volume, garde une technique propre et travaille avec un pareur ou des sécurités sur les charges lourdes.
