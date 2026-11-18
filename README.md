# Panier Futé

Application Android (Samsung Galaxy) qui, une fois par semaine :

1. **Rappel des photos** (par défaut vendredi 19 h) : te demande de photographier ton garde-manger, ton frigo et ton congélateur.
2. **Génération de nuit** (par défaut 0 h 30) : fait **un seul appel** à l'IA de ton choix avec :
   - tes photos ;
   - ta liste d'ingrédients de base (farine, sel, poivre, mayo…), toujours considérés comme disponibles ;
   - la circulaire de **ton épicerie**, choisie par code postal (Canada et États-Unis).
3. **Plan prêt** : une notification t'avertit, avec les dates des spéciaux. L'app te donne :
   - les repas de la semaine, avec les recettes, en priorisant ce que tu as déjà et les spéciaux ;
   - la liste d'épicerie classée par rayon, avec les prix de la circulaire, les taxes et le total estimé, à cocher en magasin et à partager.

Tu peux aussi lancer la génération à la main, à tout moment.

## Nouveautés de la version 2

- **N'importe quelle épicerie** : entre ton code postal, l'app liste les épiceries qui publient une circulaire près de chez toi (Maxi, IGA, Metro, Super C, Loblaws, Walmart, etc.).
- **Dates de la circulaire** : une bande indique la période des spéciaux et les meilleurs jours pour y aller (les 3 premiers jours, avant que les produits en spécial partent). Un bouton cale le rappel et la génération sur le jour où la circulaire change.
- **N'importe quel fournisseur d'IA** : DeepSeek, OpenAI, Claude (Anthropic), Gemini, Mistral, OpenRouter, ou **Autre** pour toute API compatible OpenAI (Groq, xAI, serveur local, etc.). Chaque fournisseur garde sa clé et son modèle. Le bouton **Tester** vérifie la clé, le modèle et la lecture des images.
- **Trois langues** : français, anglais, espagnol. L'interface, les notifications et les réponses de l'IA (recettes, liste d'épicerie) suivent la langue choisie.
- **Taxes selon la région** : taux de la province (ou 7 % par défaut aux États-Unis), modifiable.
- **Nouvelle interface** : typographie d'affiche d'épicerie (Barlow), prix en étiquettes, jaune réservé aux spéciaux.

Toujours là : correction de l'inventaire que l'IA a vu, et régénération du plan avec de nouvelles préférences.

## Coût

Avec DeepSeek (`deepseek-flash`), un plan typique (≈ 10 photos + ≈ 300 articles de circulaire) consomme environ 15 000 jetons en entrée et 5 000 en sortie : **moins d'un cent US par semaine**. La génération de 0 h 30 tombe dans les heures creuses de DeepSeek. Pour les autres fournisseurs, l'app affiche le nombre de jetons utilisés ; le prix dépend de leur tarif.

## 1. Configurer l'IA

1. Crée une clé d'API chez le fournisseur choisi (DeepSeek : <https://platform.deepseek.com>, API keys).
2. Ajoute un petit crédit au compte.
3. Dans l'app : **Réglages › Intelligence artificielle**, choisis le fournisseur, colle la clé, puis touche **Tester**.

Le modèle doit pouvoir lire des images. Les modèles suggérés sont modifiables.

## 2. Installer l'app sur ton Galaxy

Chaque envoi sur la branche `main` compile l'APK et le publie sur la page Releases du dépôt.

1. Sur le Galaxy, ouvre Chrome et connecte-toi à github.com (le dépôt est privé).
2. Ouvre <https://github.com/AllR8/Panier-Fut-/releases/latest/download/PanierFute.apk>.
3. Ouvre le fichier téléchargé et touche **Installer** (autorise Chrome à installer des applis inconnues si Android le demande).

Si l'installation est bloquée sans explication, désactive temporairement le **Blocage automatique** de Samsung (Paramètres › Sécurité et confidentialité › Blocage automatique).

Chaque compilation sur GitHub signe l'APK avec une clé différente : pour installer une nouvelle version, il faut désinstaller l'ancienne, ce qui efface tes réglages. Une clé de signature fixe dans les « secrets » du dépôt (`PANIER_KEYSTORE_B64` et `PANIER_KEYSTORE_PASSWORD`) évite ça.

## 3. Réglages importants sur Samsung

- **Paramètres › Batterie › Limites d'utilisation en arrière-plan › Applications jamais en veille** : ajoute Panier Futé, sinon le rappel et la génération de nuit peuvent être retardés.
- Accepte les notifications au premier lancement.

## Fonctionnement détaillé

| Élément | Où dans le code |
|---|---|
| Liste des épiceries et lecture de la circulaire (Flipp) | `data/Circulaire.kt` |
| Région, taxes et langues | `data/Region.kt` |
| Appel à l'IA, tous fournisseurs (format OpenAI ou Anthropic) | `data/ClientIA.kt` |
| Demande envoyée à l'IA | `data/GenerateurPlan.kt` (`construireSysteme()`) |
| Photos (redimensionnées à 1280 px, effacées après analyse) | `data/Photos.kt` |
| Réglages et ingrédients de base par langue | `data/Reglages.kt` |
| Rappel et génération de nuit | `travail/Planificateur.kt`, `travail/Workers.kt` |
| Textes en français, anglais, espagnol | `i18n/TextesFr.kt`, `i18n/TextesEn.kt`, `i18n/TextesEs.kt` |
| Couleurs, typographie, composants | `ui/Theme.kt`, `ui/Composants.kt` |
| Écrans | `ui/EcranSemaine.kt`, `ui/EcranEpicerie.kt`, `ui/EcranPhotos.kt`, `ui/EcranReglages.kt` |
| Captures d'écran automatiques | `app/src/test/.../CapturesTest.kt` (tâche « captures » sur GitHub) |

Règles appliquées :

- **Une seule génération automatique par semaine.** Si tu as déjà généré à la main après le rappel, la génération de la nuit est sautée.
- **Échec réseau la nuit** : nouvel essai automatique, jusqu'à 3 fois.
- **Aucune photo** : le plan est généré avec les ingrédients de base. Désactive « Générer même sans photos » pour que l'app attende plutôt tes photos.
- **Circulaire utilisée** : celle en vigueur ; si elle finit dans moins de 2 jours et que la suivante est publiée, la suivante.

## Limites connues

- **Source des circulaires** : l'API publique mais **non officielle** de Flipp. Si Flipp la change, c'est `Circulaire.kt` qu'il faudra ajuster. Avant une diffusion publique de l'app, il faudrait une entente ou une source officielle.
- **Une clé d'API par utilisateur** : chaque personne doit avoir sa propre clé. Pour distribuer l'app largement, un petit serveur qui détient la clé serait préférable.
- **Prix hors spéciaux** : estimés par l'IA, précédés de « ≈ ».
- **Taxes** : estimation ; le taux et les articles taxables varient selon la province ou l'État.
- **Clé d'API** : stockée dans l'espace privé de l'app sur le téléphone, envoyée seulement au fournisseur choisi.

## Licences

Polices Barlow et Barlow Condensed : SIL Open Font License 1.1 (`licences/Barlow-OFL.txt`).
