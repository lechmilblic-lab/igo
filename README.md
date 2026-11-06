# iGO Map Reader

Projet Android Studio Kotlin, Android 6+ (minSdk 23), cible Android 16 / API 36. ABI incluses : `armeabi-v7a` (ARM 32 bits) et `arm64-v8a`.

Cette première version permet de sélectionner un ZIP et d'inventorier les fichiers iGO (.FBL, .FDA, .FPA, .FTR, .FSP, .POI, etc.) ainsi que les formats ouverts GPX/KML/GeoJSON.

**Limite :** les cartes iGO sont des formats propriétaires. L'application ne les déchiffre pas et ne dessine pas leur géométrie. Une visualisation cartographique réelle exige des données ouvertes (GPX/KML/GeoJSON) ou un SDK/API autorisé.

## Compilation
Ouvre le dossier dans Android Studio récent, installe Android SDK Platform 36 et JDK 17, synchronise Gradle puis choisis Build > Build APK(s). L'APK debug sera dans `app/build/outputs/apk/debug/`.
