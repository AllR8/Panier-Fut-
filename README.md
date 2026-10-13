# Panier Futé 🛒

Application Android (Samsung Galaxy) qui, une fois par semaine :

1. **Vendredi 19 h** : te notifie de prendre en photo ton garde-manger, ton frigo et ton congélateur.
2. **Nuit du vendredi au samedi (00 h 30)** : fait **un seul appel** à l'IA DeepSeek avec :
   - tes photos,
   - ta liste d'ingrédients de base (farine, sel, poivre, mayo…), toujours considérés comme disponibles,
   - la circulaire du **Maxi, 955 boul. René-Lévesque O. (G1S 1T7)**.
3. **Samedi matin** : te notifie que ton plan est prêt :
   - les repas de la semaine, avec la recette, en priorisant ce que tu as déjà et les rabais ;
   - la liste d'épicerie, classée par rayon, avec les prix de la circulaire, à cocher en magasin et à partager.

Tu peux aussi lancer la génération à la main, à tout moment (bouton **Générer mon plan maintenant**).

## Coût

Le modèle `deepseek-flash` lit directement les images. Un plan typique (≈ 10 photos + ≈ 300 articles de circulaire) consomme environ 15 000 jetons en entrée et 5 000 en sortie.

- En heures creuses : **moins d'un cent US par semaine**.
- La génération de 00 h 30 tombe dans les heures creuses de DeepSeek (tarif réduit de moitié).
- Le coût réel de chaque appel est affiché dans l'app.

## 1. Obtenir une clé DeepSeek

1. Crée un compte sur <https://platform.deepseek.com>.
2. Va dans **API keys** et crée une clé (elle commence par `sk-`).
3. Ajoute un petit crédit (quelques dollars suffisent pour des années).
4. Dans l'app, ouvre **Réglages** et colle la clé, puis touche **Tester la clé**.

## 2. Installer l'app sur ton Galaxy

### Option A — Compilation sur GitHub (rien à installer sur ton PC)

1. Crée un dépôt **privé** sur <https://github.com/new> (ex. `panier-fute`).
2. Envoie-y le contenu de ce dossier, par exemple avec GitHub Desktop, ou par glisser-déposer dans « Add file › Upload files ». Le dossier `.github` doit être inclus.
3. Ouvre l'onglet **Actions**. La compilation « Compiler l'APK » démarre toute seule et dure environ 5 minutes.
4. Dans l'exécution terminée, télécharge l'artefact **PanierFute-apk** (un .zip contenant `app-debug.apk`).
5. Copie l'APK sur ton téléphone, ou ouvre le lien GitHub directement depuis le Galaxy.
6. Ouvre-le et autorise « Installer des applis inconnues » pour Mes fichiers ou Chrome.

### Option B — Android Studio (Windows)

1. Installe [Android Studio](https://developer.android.com/studio).
2. Fais **File › Open** sur ce dossier et attends la synchronisation Gradle.
3. Sur le Galaxy, active le débogage USB :
   - Paramètres › À propos du téléphone › Informations sur le logiciel : touche 7 fois **Numéro de version** ;
   - puis Options de développement › Débogage USB.
4. Branche le téléphone et clique ▶ **Run**.

Par défaut, chaque compilation sur GitHub signe l'APK avec une clé différente. Pour installer une nouvelle version, il faut alors désinstaller l'ancienne, ce qui efface tes réglages. Pour éviter ça, on peut ajouter une clé de signature fixe dans les « secrets » du dépôt (`PANIER_KEYSTORE_B64` et `PANIER_KEYSTORE_PASSWORD`). La clé n'est jamais versionnée dans le dépôt.

## 3. Réglages importants sur Samsung

Samsung met en veille les applis peu utilisées, ce qui peut retarder le rappel et la génération de nuit. Pour l'éviter :

- **Paramètres › Batterie › Limites d'utilisation en arrière-plan › Applications jamais en veille** : ajoute Panier Futé.
- Accepte les notifications au premier lancement.

## Fonctionnement détaillé

| Élément | Où dans le code |
|---|---|
| Lecture de la circulaire (Flipp, magasin choisi par code postal) | `data/Circulaire.kt` |
| Appel DeepSeek (un seul, images + texte, sortie JSON) | `data/DeepSeek.kt`, `data/GenerateurPlan.kt` |
| Prompt envoyé à l'IA | `GenerateurPlan.construireSysteme()` |
| Photos (redimensionnées à 1280 px, effacées après analyse) | `data/Photos.kt` |
| Ingrédients de base par défaut | `data/Reglages.kt` (`INGREDIENTS_BASE_DEFAUT`) |
| Rappel du vendredi et génération de nuit | `travail/Planificateur.kt`, `travail/Workers.kt` |
| Écrans | `ui/EcranSemaine.kt`, `ui/EcranEpicerie.kt`, `ui/EcranPhotos.kt`, `ui/EcranReglages.kt` |

Règles appliquées :

- **Une seule génération automatique par semaine.** Si tu as déjà généré à la main après le rappel du vendredi, la génération de la nuit est sautée.
- **Échec réseau la nuit** : nouvel essai automatique, jusqu'à 3 fois.
- **Aucune photo** : le plan est généré avec les ingrédients de base seulement. Désactive « Générer même sans photos » pour que l'app attende plutôt tes photos.
- **Circulaires utilisées** : celles qui sont en vigueur. Une circulaire Maxi va du jeudi au mercredi, donc fais ton épicerie avant mercredi soir pour avoir les prix affichés.

## Limites connues

- **Source de la circulaire** : l'app utilise l'API publique mais **non officielle** de Flipp, le service qui héberge les circulaires numériques de Maxi. Si Flipp la change, le bouton **Tester la circulaire** dans Réglages le montrera, et c'est `Circulaire.kt` qu'il faudra ajuster.
- **Prix hors rabais** : ils sont estimés par l'IA et précédés de « ≈ ».
- **Inventaire** : l'IA ne voit que ce qui est visible sur les photos.
- **Clé API** : elle est stockée dans l'espace privé de l'app sur ton téléphone. Elle n'est envoyée qu'à DeepSeek.
