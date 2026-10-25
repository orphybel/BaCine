# BaCine

Application Android TV pour parcourir les films à l'affiche en France, voir leurs
bandes-annonces et repérer les sorties récentes disponibles en VOD payante.
Conçue pour être pilotée à la télécommande, sur un vidéoprojecteur
**XGIMI MoGo 4** (Google TV) ou n'importe quel appareil Android TV.

## Ce que fait l'appli

- **À l'affiche au cinéma** — les films actuellement en salle en France.
- **Sorties récentes en VOD** — films récents réellement disponibles en location
  ou à l'achat en France. Le filtre de TMDB étant peu fiable sur ce point, chaque
  film est re-vérifié auprès de `/watch/providers` avant d'être affiché.
- **Catalogue filtrable** — tri par popularité, date de sortie, note ou titre ;
  filtres par genre (multi-sélection), année, note minimale et disponibilité
  (salle, location, achat, abonnement). Les derniers filtres sont conservés.
- **Fiche film** — affiche, synopsis, casting, durée, réalisation, et les
  plateformes VOD françaises avec leur type d'offre.
- **Bandes-annonces** — lecture intégrée, avec repli vers l'application YouTube.
- **Recherche** par titre.

Toute la navigation se fait au D-pad : chaque élément focalisable s'agrandit et
se souligne en doré, et le rail de gauche se déplie quand le focus y entre.

## Installation sur le vidéoprojecteur

L'APK est construit automatiquement par GitHub Actions.

### Option 1 — depuis une release (le plus simple)

1. Sur le MoGo 4 : **Paramètres → Confidentialité → Sécurité et restrictions →
   Sources inconnues**, autoriser l'application que tu utiliseras pour installer.
2. Installer **Downloader** (by AFTVnews) depuis le Play Store.
3. Y saisir l'URL de la dernière release de ce dépôt, télécharger `BaCine-debug.apk`,
   puis installer.

Pour publier une release, pousser un tag :

```bash
git tag v1.0.0 && git push origin v1.0.0
```

### Option 2 — depuis un artefact de build

Chaque push produit un artefact `BaCine-debug-apk` dans l'onglet **Actions**.
Il faut être connecté à GitHub pour le télécharger, ce qui est peu pratique
depuis un téléviseur — préférer l'option 1.

### Option 3 — en ADB sans fil

```bash
adb connect <ip-du-projecteur>:5555
adb install -r BaCine-debug.apk
```

L'APK est signé avec la clé debug, ce qui suffit pour un sideload.

## Clé API TMDB

L'application a besoin d'une clé TMDB, **gratuite**. Deux façons de la fournir,
la première l'emportant sur la seconde :

1. **Saisie dans l'application** — au premier lancement, BaCine demande la clé et
   la valide auprès de TMDB avant de l'enregistrer. Modifiable ensuite dans
   *Réglages*.
2. **Injection à la compilation** — définir un secret GitHub `TMDB_API_KEY` dans
   les paramètres du dépôt. La CI l'intègre alors à l'APK et aucune saisie n'est
   nécessaire. Sans ce secret, le build fonctionne quand même.

Pour obtenir une clé : créer un compte sur [themoviedb.org](https://www.themoviedb.org),
puis **Paramètres → API** et copier la clé « API Key (v3 auth) ».

En développement local, la clé peut aussi être placée dans `local.properties` :

```properties
tmdb.apiKey=votre_cle_ici
```

## Recherche dans Kodi (vStream)

Si **Kodi** est installé sur l'appareil, la fiche film affiche un bouton
**« Kodi »** (icône loupe). Il ouvre Kodi directement sur la recherche globale
de l'add-on **vStream**, catégorie Films, avec le titre du film.

Kodi ne sait pas ouvrir un dossier d'add-on depuis une autre application : tout
lien reçu de l'extérieur est traité comme une demande de lecture. Le bouton
passe donc par un petit add-on Kodi, **BaCine** (`plugin.video.bacine`, dans
[`kodi/`](kodi/plugin.video.bacine)), qui reçoit le titre et ouvre la recherche
vStream à sa place. vStream lui-même n'est pas modifié : ses mises à jour
automatiques continuent de s'appliquer normalement.

Installation de la passerelle, une fois :

1. Télécharger `plugin.video.bacine-<version>.zip` depuis la release (ou
   l'artefact de build) et le copier sur l'appareil.
2. Dans Kodi : **Add-ons → Installer depuis un fichier zip**, choisir le zip.
   Si Kodi le refuse, autoriser d'abord **Paramètres → Système → Add-ons →
   Sources inconnues** (déjà le cas si vStream est installé).

Sans vStream, la passerelle affiche simplement une notification dans Kodi.

## Compilation locale

Il faut un JDK 17 et le SDK Android (API 35).

```bash
./gradlew assembleDebug      # APK dans app/build/outputs/apk/debug/
./gradlew testDebugUnitTest  # tests unitaires
./gradlew lintDebug
```

## Architecture

```
app/src/main/java/com/orphybel/bacine/
├── AppContainer.kt          graphe de dépendances (pas de framework DI)
├── MainActivity.kt          point d'entrée, choisit l'écran de départ
├── data/
│   ├── image/               chargeur d'images maison sur OkHttp
│   ├── prefs/               DataStore : clé API, région, filtres
│   ├── remote/              client TMDB, DTO, mappers
│   └── repo/                MovieRepository et ses caches mémoire
├── domain/                  modèles métier et règles de tri
└── ui/
    ├── components/          primitives TV réutilisables (focus, cartes, images)
    ├── nav/                 rail de navigation et graphe
    └── home · catalog · detail · player · search · settings
```

Choix notables :

- **Compose + Material 3**, avec une couche de focus maison (`TvFocusable`)
  plutôt que `tv-material`, pour garder un contrôle direct sur le comportement
  du D-pad.
- **Pas de framework DI, pas de Retrofit, pas de bibliothèque d'images.** Le
  périmètre est réduit — quelques requêtes GET et des JPEG de CDN — et chaque
  dépendance en moins est un couplage de versions en moins.
- **Caches en mémoire uniquement** côté données, plus le cache disque HTTP
  d'OkHttp pour les images. Les données TMDB changent au plus une fois par jour.

## Limites connues

- La lecture des bandes-annonces passe par l'API IFrame de YouTube dans une
  WebView — l'API native Android étant dépréciée. C'est le point le plus
  fragile sur un appareil TV, et certaines vidéos sont bloquées à l'intégration
  par leur propriétaire. Le bouton **« Ouvrir dans YouTube »** prend alors le
  relais ; il apparaît automatiquement si le lecteur ne démarre pas.
- La disponibilité VOD dépend des données JustWatch relayées par TMDB, mises à
  jour quotidiennement : un film tout juste sorti en VOD peut manquer à l'appel.

## Attributions

Ce produit utilise l'API TMDB mais n'est ni approuvé ni certifié par
[TMDB](https://www.themoviedb.org). Les données de disponibilité VOD proviennent
de [JustWatch](https://www.justwatch.com) via TMDB.
