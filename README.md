# Paradoxe Training Assistant 4.6 — sport, nutrition et suivi corporel pour Android

(Anciennement « Fonte », puis « Paradoxe Training ». Le nom affiché change, l'identifiant `com.fonte.carnet` reste : chaque version s'installe par-dessus et garde les données.)

Icône : le logo PRDX de la marque Paradoxe, en jaune fluo sur charcoal. Style : fond charcoal, boutons blancs, rose électrique pour les heures, les liens et les protéines, jaune fluo pour l'anneau des calories.

Appli d'entraînement hors ligne, en français, thème sombre. Aucune pub, aucun compte : les données restent sur le téléphone.

## Onglets

**Accueil · Sport (Muscu, CrossFit, Renfo) · Nutrition · Corps · Progrès (historique et records)**

### Accueil
Le récapitulatif du jour : calories restantes et macros, hydratation, calories dépensées, séance du jour ou programme, poids et sa courbe, semaine d'activité et derniers records.

### Nutrition
- **Besoins calculés** avec la méthode la mieux étayée par les études (version 4.3) :
  1. métabolisme de base de **Ten Haaf** (la plus précise chez les sportifs), ou Cunningham / Ten Haaf masse maigre si ta masse grasse est connue, ou Mifflin-St Jeor au choix ;
  2. × **activité du quotidien sans le sport** (bureau ×1,4, debout ×1,55, travail physique ×1,75, d'après les fourchettes FAO/OMS) ;
  3. **objectif en rythme de poids** : perte de 0,5 à 1 % du poids par semaine, prise de 0,25 à 0,5 %, converti avec 7 700 kcal par kg ;
  4. **+ les calories nettes des séances** du jour (MET − 1, le repos étant déjà compté). Les jours d'entraînement prévus, une séance type est comptée tant que la séance n'est pas notée ;
  5. **protéines en g/kg** (1,6 à 2,2 g/kg, 2,6 g/kg de masse maigre en sèche), **lipides** de 20 à 35 % (jamais sous 0,5 g/kg), **glucides** = le reste, plus la séance les jours d'entraînement ;
  6. **réglage par la balance** : après 4 pesées sur 2 semaines, l'appli compare ta courbe au rythme visé et propose de corriger l'objectif de 100 à 200 kcal.
  Les répartitions en % de l'article « Calculer ses macros » restent disponibles (mode « En pourcentages »). Eau : 35 ml/kg, plus 500 ml par heure de séance.
- **Journal par repas** (petit-déjeuner, déjeuner, dîner, collations), avec une **recherche multisource** (version 4.4) : une seule barre de recherche fouille à la fois
  - tes aliments perso et tes récents (en tête de liste) ;
  - la base de l'appli (environ 100 aliments courants, dont des produits réunionnais) et ses plats (61 recettes, créoles comprises) ;
  - la **table Ciqual 2025** de l'ANSES : 3 400 aliments français bruts et cuisinés, dans le téléphone, sans réseau ;
  - **Open Food Facts** en ligne : les produits de marque, cherchés par nom ; ceux vendus à La Réunion et en France passent en premier, et un produit choisi est gardé dans tes aliments.
  Les résultats sont fusionnés sans doublons, classés par pertinence (sans tenir compte des accents ni des pluriels), avec leur source et des filtres Aliments / Marques / Plats. Sans réseau, la recherche continue dans les sources du téléphone.
- **Quantité** : portions toutes prêtes (100 g, la portion du produit ou une portion courante : 1 filet, 1 œuf, 1 pot…, et « Ma portion », la dernière quantité notée), réglage au gramme, calories et macros en direct.
- **Un plat comme repas** : nombre d'assiettes, ingrédients modifiables ou remplaçables, ajout en un seul plat ou ingrédient par ingrédient, « Garder comme mon plat ».
- **Code-barres** : la photo du code est lue sur le téléphone, puis la fiche produit vient d'Open Food Facts (connexion nécessaire).
- **Étiquette** : la photo du tableau nutritionnel est lue sur le téléphone, et les valeurs sont pré-remplies.
- **Photo du repas** : jointe au journal. Elle ne calcule pas les calories : il faudrait un service d'IA en ligne.
- **Calories dépensées** : calories nettes estimées (MET − 1) à partir des séances, WOD et circuits, plus les activités ajoutées (course, vélo…) ou les calories actives de ta montre. Elles s'ajoutent à l'objectif du jour.

### Mes repas (Nutrition › Mes repas)
Deux modes, au choix en haut de l'écran : **Journée type** (par défaut depuis la 4.6) ou **Plats de la semaine**.

**Journée type** (version 4.5), sur le principe d'une diète de préparateur :
- **Repas à heures fixes** calculés d'après l'heure du lever et de la séance (4, 5 ou 6 repas) : le repas d'avant la séance tombe 2 h 30 avant, celui d'après 2 h après, et un fruit 30 minutes avant. Chaque heure peut être fixée à la main.
- **Chaque repas est fait de blocs** : glucides, protéines, légumes ou fruit, lipides, avec la quantité en grammes (poids cuit et équivalent cru pour la viande, le poisson et le riz), en blancs d'œuf ou en cuillères pour l'huile.
- **Équivalences** sous chaque aliment, à la même quantité du nutriment principal (par exemple 70 g de glucides : 250 g de riz cuit, 400 g de patate douce ou 170 g de pain complet). Touche un aliment pour en choisir un autre, de la liste ou de Ciqual (manioc, songe, fruit à pain…), pour ce jour seulement ou pour toujours à ce repas ; le reste du repas s'ajuste.
- **Jour de séance ou de repos** : les jours de séance ont plus de glucides, surtout avant et après la séance ; les jours de repos, le repas d'avant la séance devient une collation sans féculent. Protéines et lipides ne bougent pas.
- Les quantités sont calculées pour viser tes calories, protéines, glucides et lipides du jour (moindres carrés pondérés, arrondis à 5 g).
- **« Mangé »** ajoute le repas au journal. **Résumé du jour** : totaux, différences repos / séance, part de chaque repas, **Mes règles** (eau, pesée, repas libre…) modifiables, et partage du jour en texte. **Liste de courses** des 7 prochains jours.
- **Réglages** : horaires, jours de séance, aliments proposés par famille, rappels à afficher avec un repas (compléments notés par toi, sans dose proposée).

**Plats de la semaine** :
- **Plan de 7 jours** : petit-déjeuner, collations, déjeuner, dîner et shaker, choisis parmi une cinquantaine de recettes. Chaque recette est ajustée (protéines, féculents, matières grasses) pour viser tes calories, protéines et lipides du jour. Les jours d'entraînement visent plus de calories, en glucides.
- **Réglages** : 3, 4 ou 5 prises par jour, shaker les jours d'entraînement (repérés dans ton historique) ou tous les jours, sans porc, végétarien, sans lactose, poisson 2 fois par semaine, cuisine créole, petit budget, aliments à éviter, cuisine en lot (un plat pour 2 jours), variété.
- **Chaque plat** : quantités en grammes (poids cuit et équivalent cru ou sec), macros, étapes de préparation. « Mangé » l'ajoute au journal du jour.
- **Remplacer un plat** : 3 propositions aux mêmes calories et protéines (±10 %), ou toutes les recettes au choix.
- **Liste de courses** : calculée sur la semaine, rangée par rayon, en poids cru ou sec, à cocher, à compléter, à partager ou copier.
- **Composer avec mes ingrédients** : tu choisis ce que tu as (poulet, pâtes, œufs, épinards, morue, brèdes…) et l'appli propose des repas : recettes qui les utilisent (avec équivalences, par exemple brèdes ou épinards) et assiettes composées, ajustées à ton objectif. Tu valides ceux qui te tentent, puis tu remplis ta semaine ou tu places un repas précis. Ce qui manque s'ajoute à la liste de courses.
- **Cuisine réunionnaise** : cari poulet, rougail saucisse, rougail morue, rougail z'œufs, rougail thon, rougail poulet, massalé poulet et cabri, cari camarons, civet zourite, cari ti jacques, zembrocal, gratin chouchou, lentilles de Cilaos, poisson aux brèdes…

### Corps
Poids (courbe, variations sur 7 et 30 jours, IMC), mensurations (taille, hanches, poitrine, épaules, bras, cuisse, mollet, cou, masse grasse) avec historique, composition corporelle (masse grasse mesurée ou estimée par la méthode de l'US Navy).


### Muscu
- **Séance libre** ou lancée depuis une séance type, un modèle, une proposition ou la programmation du coach. Tu notes kg × répétitions et tu coches ✓ chaque série terminée. Les charges de la dernière fois sont reprises automatiquement.
- **Cardio / ergo** (version 4.6) : dans une séance, « + Cardio / ergo » note à la main un vélo d'appartement, un air bike (Assault, Echo), un rameur, un SkiErg, de la course, de la corde à sauter, etc. : durée, intensité (facile, modéré, intense), distance et calories affichées par l'ergo si tu les as. Les calories nettes s'ajoutent au jour sans compter deux fois le temps de muscu. Une séance de cardio seul s'enregistre aussi.
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
- **Cardio / ergo** : la tuile du chrono note un vélo, un rameur ou un SkiErg fait à part, avec les mêmes informations qu'en muscu.
- **Mes WOD** : crée les WOD de ta box. Le chrono adapté et l'enregistrement des scores (temps, tours + reps, RX ou adapté) sont inclus, avec ton record sur chaque WOD.

### Renfo
Circuits guidés : gainage & abdos, épaules (coiffe des rotateurs, mobilité), genoux & chevilles, dos & posture, hanches & fessiers, mobilité, full body, prévention course. L'appli enchaîne les exercices, les temps et les pauses ; les exercices en répétitions attendent que tu touches « Fait ✓ ». « + Cardio / ergo » note aussi un vélo ou un rameur fait à côté.

### Historique et Records
Historique unifié (muscu, WOD, renfo, cardio), filtrable. Records : meilleurs scores de WOD, puis, par exercice, 1RM estimé, charge max, répétitions max et courbe de progression. Le calculateur de max est aussi dans Records.

## Obtenir l'APK

Dépose `Fonte-projet-android.zip` à la racine du dépôt GitHub (**Add file › Upload files**, puis **Commit changes**). Le workflow `import.yml` décompresse le projet, l'enregistre dans le dépôt et compile l'APK. Quand l'onglet **Actions** affiche une coche verte, le fichier `fonte-apk` est dans **Artifacts**. Décompresse-le et installe `app-debug.apk`. L'installation par-dessus la version précédente conserve tes données.

Avec Android Studio : **Open** sur le dossier du projet, puis **▶ Run** avec le téléphone branché en USB (débogage USB activé).

## Structure du code

```
app/src/main/java/com/fonte/carnet/
├── core/   logique pure, testée : modèle, programmes, chrono (Clock), WOD, circuits,
│           séances types, générateur par muscle, lecture de programmation (CoachParser),
│           recettes, plan de repas (MealPlanner), journée type (DayPlanner) et liste de courses
├── data/   stockage local JSON (écriture atomique) + export/import
└── ui/     écrans (activité unique, interface construite en code)
app/src/test/   tests unitaires (./gradlew test)
```

- Java, SDK Android et ML Kit (lecture de texte et de codes-barres sur le téléphone, sans connexion). Android 8.0 minimum (API 26), cible API 34.
- Seule connexion : Open Food Facts, pour la fiche d'un code-barres et la recherche d'aliments en ligne par nom (aucun compte, aucune autre donnée envoyée).
- Données : `fonte.json` dans le stockage privé de l'appli. Il est inclus dans la sauvegarde Android automatique et peut être exporté depuis Réglages.

## Sécurité

Les WOD Heroes et les programmes à pourcentages sont exigeants. Échauffe-toi, adapte charges et volume, garde une technique propre et travaille avec un pareur ou des sécurités sur les charges lourdes.

## Sources des données

- **Table Ciqual 2025**, ANSES (https://ciqual.anses.fr), licence ouverte Etalab 2.0 / CC-BY 4.0. Elle est téléchargée sur Zenodo et convertie à chaque compilation par `tools/CiqualImport.java` (tâche Gradle `ciqualImport`) ; si le téléchargement échoue, la compilation échoue.
- **Open Food Facts** (https://world.openfoodfacts.org), base ouverte sous licence ODbL : fiches produits par code-barres et recherche par nom, au plus une recherche toutes les 6 secondes.
