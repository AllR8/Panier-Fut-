package ca.panierfute.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import ca.panierfute.R

/*
 * Identité « étiquette de tablette » : la typographie des affiches d'épicerie
 * (Barlow Condensed, très grasse) pour les prix et les titres, Barlow pour le reste.
 * Le jaune étiquette est réservé aux spéciaux : il ne sert à rien d'autre.
 */

val Barlow = FontFamily(
    Font(R.font.barlow_regular, FontWeight.Normal),
    Font(R.font.barlow_medium, FontWeight.Medium),
    Font(R.font.barlow_semibold, FontWeight.SemiBold)
)

val BarlowCondensed = FontFamily(
    Font(R.font.barlow_condensed_semibold, FontWeight.SemiBold),
    Font(R.font.barlow_condensed_extrabold, FontWeight.ExtraBold)
)

@Immutable
data class Couleurs(
    val sombre: Boolean,
    val papier: Color,       // fond de l'app
    val carton: Color,       // surfaces surélevées (feuilles, dialogues, champs)
    val encre: Color,        // texte principal
    val encreDouce: Color,   // texte secondaire
    val trait: Color,        // séparateurs, contours
    val action: Color,       // boutons principaux, étiquette du total
    val surAction: Color,
    val etiquette: Color,    // spéciaux uniquement
    val surEtiquette: Color,
    val tomate: Color,       // erreurs, « à utiliser vite »
    val celeri: Color,       // « en stock »
    val celeriPale: Color
)

private val Clair = Couleurs(
    sombre = false,
    papier = Color(0xFFF2F4EF),
    carton = Color(0xFFFFFFFF),
    encre = Color(0xFF173A2C),
    encreDouce = Color(0xFF56665C),
    trait = Color(0xFFD7DDD5),
    action = Color(0xFF173A2C),
    surAction = Color(0xFFFFFFFF),
    etiquette = Color(0xFFFFD23F),
    surEtiquette = Color(0xFF173A2C),
    tomate = Color(0xFFC23B22),
    celeri = Color(0xFF3F7D4F),
    celeriPale = Color(0xFFDDEBDF)
)

private val Sombre = Couleurs(
    sombre = true,
    papier = Color(0xFF0F1813),
    carton = Color(0xFF18231C),
    encre = Color(0xFFE3ECE5),
    encreDouce = Color(0xFF9CAAA1),
    trait = Color(0xFF2A3A31),
    action = Color(0xFFDCEBE0),
    surAction = Color(0xFF123024),
    etiquette = Color(0xFFFFD23F),
    surEtiquette = Color(0xFF173A2C),
    tomate = Color(0xFFFF8A70),
    celeri = Color(0xFF8CC79B),
    celeriPale = Color(0xFF20372A)
)

/** Échelle typographique de l'app. */
@Immutable
data class Typo(
    val titreEcran: TextStyle,
    val prixHero: TextStyle,
    val titreSection: TextStyle,
    val chiffreJour: TextStyle,
    val titreLigne: TextStyle,
    val corps: TextStyle,
    val corpsFort: TextStyle,
    val petit: TextStyle,
    val libelle: TextStyle,
    val bouton: TextStyle
)

private val TYPO = Typo(
    titreEcran = TextStyle(fontFamily = BarlowCondensed, fontWeight = FontWeight.ExtraBold, fontSize = 36.sp, lineHeight = 38.sp),
    prixHero = TextStyle(fontFamily = BarlowCondensed, fontWeight = FontWeight.ExtraBold, fontSize = 54.sp, lineHeight = 54.sp),
    titreSection = TextStyle(fontFamily = BarlowCondensed, fontWeight = FontWeight.SemiBold, fontSize = 23.sp, lineHeight = 26.sp),
    chiffreJour = TextStyle(fontFamily = BarlowCondensed, fontWeight = FontWeight.SemiBold, fontSize = 32.sp, lineHeight = 32.sp),
    titreLigne = TextStyle(fontFamily = Barlow, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 22.sp),
    corps = TextStyle(fontFamily = Barlow, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 21.sp),
    corpsFort = TextStyle(fontFamily = Barlow, fontWeight = FontWeight.Medium, fontSize = 15.sp, lineHeight = 21.sp),
    petit = TextStyle(fontFamily = Barlow, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp),
    libelle = TextStyle(fontFamily = Barlow, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 16.sp),
    bouton = TextStyle(fontFamily = Barlow, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 18.sp)
)

val LocalCouleurs = staticCompositionLocalOf { Clair }
val LocalTypo = staticCompositionLocalOf { TYPO }

/** Raccourcis : P.c = couleurs, P.t = typographie. */
object P {
    val c: Couleurs @Composable get() = LocalCouleurs.current
    val t: Typo @Composable get() = LocalTypo.current
}

private fun typoMaterial(): Typography {
    val b = Typography()
    fun TextStyle.barlow() = copy(fontFamily = Barlow)
    return Typography(
        displayLarge = b.displayLarge.copy(fontFamily = BarlowCondensed),
        displayMedium = b.displayMedium.copy(fontFamily = BarlowCondensed),
        displaySmall = b.displaySmall.copy(fontFamily = BarlowCondensed),
        headlineLarge = b.headlineLarge.copy(fontFamily = BarlowCondensed, fontWeight = FontWeight.ExtraBold),
        headlineMedium = b.headlineMedium.copy(fontFamily = BarlowCondensed, fontWeight = FontWeight.ExtraBold),
        headlineSmall = b.headlineSmall.copy(fontFamily = BarlowCondensed, fontWeight = FontWeight.SemiBold),
        titleLarge = b.titleLarge.copy(fontFamily = BarlowCondensed, fontWeight = FontWeight.SemiBold),
        titleMedium = b.titleMedium.barlow().copy(fontWeight = FontWeight.SemiBold),
        titleSmall = b.titleSmall.barlow().copy(fontWeight = FontWeight.SemiBold),
        bodyLarge = b.bodyLarge.barlow(),
        bodyMedium = b.bodyMedium.barlow(),
        bodySmall = b.bodySmall.barlow(),
        labelLarge = b.labelLarge.barlow().copy(fontWeight = FontWeight.SemiBold),
        labelMedium = b.labelMedium.barlow(),
        labelSmall = b.labelSmall.barlow()
    )
}

@Composable
fun ThemePanier(sombre: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val c = if (sombre) Sombre else Clair
    val schema = if (sombre) {
        darkColorScheme(
            primary = c.action, onPrimary = c.surAction,
            secondary = c.celeri, onSecondary = c.papier,
            tertiary = c.etiquette, onTertiary = c.surEtiquette,
            background = c.papier, onBackground = c.encre,
            surface = c.carton, onSurface = c.encre,
            surfaceVariant = c.trait, onSurfaceVariant = c.encreDouce,
            surfaceContainerLowest = c.papier, surfaceContainerLow = c.carton,
            surfaceContainer = c.carton, surfaceContainerHigh = c.carton, surfaceContainerHighest = c.carton,
            outline = c.encreDouce, outlineVariant = c.trait,
            error = c.tomate, onError = c.papier
        )
    } else {
        lightColorScheme(
            primary = c.action, onPrimary = c.surAction,
            secondary = c.celeri, onSecondary = c.carton,
            tertiary = c.etiquette, onTertiary = c.surEtiquette,
            background = c.papier, onBackground = c.encre,
            surface = c.carton, onSurface = c.encre,
            surfaceVariant = c.trait, onSurfaceVariant = c.encreDouce,
            surfaceContainerLowest = c.carton, surfaceContainerLow = c.carton,
            surfaceContainer = c.carton, surfaceContainerHigh = c.carton, surfaceContainerHighest = c.carton,
            outline = c.encreDouce, outlineVariant = c.trait,
            error = c.tomate, onError = c.carton
        )
    }
    CompositionLocalProvider(LocalCouleurs provides c, LocalTypo provides TYPO) {
        MaterialTheme(colorScheme = schema, typography = typoMaterial(), content = content)
    }
}
