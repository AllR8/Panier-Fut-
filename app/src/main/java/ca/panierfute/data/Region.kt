package ca.panierfute.data

import java.util.Locale

/** Langues offertes dans l'app. */
enum class Langue(val code: String, val nom: String, val locale: Locale, val nomAnglais: String) {
    FR("fr", "Français", Locale.CANADA_FRENCH, "Canadian French"),
    EN("en", "English", Locale.CANADA, "English"),
    ES("es", "Español", Locale("es", "US"), "Spanish");

    companion object {
        fun depuisCode(code: String?): Langue =
            entries.firstOrNull { it.code == code } ?: parDefaut()

        /** Langue du téléphone si elle est offerte, sinon l'anglais. */
        fun parDefaut(): Langue = when (Locale.getDefault().language) {
            "fr" -> FR
            "es" -> ES
            else -> EN
        }
    }
}

/**
 * Région déduite du code postal : pays, province, taux de taxe et devise.
 * Au Canada, les aliments de base sont détaxés partout ; le taux ne s'applique
 * qu'aux articles taxables (boissons gazeuses, bonbons, grignotines, non-alimentaire).
 */
data class Region(
    val pays: String,          // "CA", "US" ou "" si inconnu
    val province: String,      // ex. "QC" ; vide hors Canada
    val nom: String,           // ex. "Québec, Canada" (en anglais : envoyé à l'IA)
    val tauxTaxes: Double,     // taux combiné sur les articles taxables
    val devise: String         // "CAD" ou "USD"
) {
    companion object {
        private val CP_CANADA = Regex("^[A-Z][0-9][A-Z][0-9][A-Z][0-9]$")
        private val ZIP_USA = Regex("^[0-9]{5}$")

        // Taux 2026 (TPS/TVH/TVP/TVQ combinées), source : taxtips.ca.
        private val PROVINCES = mapOf(
            'A' to Triple("NL", "Newfoundland and Labrador", 0.15),
            'B' to Triple("NS", "Nova Scotia", 0.14),
            'C' to Triple("PE", "Prince Edward Island", 0.15),
            'E' to Triple("NB", "New Brunswick", 0.15),
            'G' to Triple("QC", "Québec", 0.14975),
            'H' to Triple("QC", "Québec", 0.14975),
            'J' to Triple("QC", "Québec", 0.14975),
            'K' to Triple("ON", "Ontario", 0.13),
            'L' to Triple("ON", "Ontario", 0.13),
            'M' to Triple("ON", "Ontario", 0.13),
            'N' to Triple("ON", "Ontario", 0.13),
            'P' to Triple("ON", "Ontario", 0.13),
            'R' to Triple("MB", "Manitoba", 0.12),
            'S' to Triple("SK", "Saskatchewan", 0.11),
            'T' to Triple("AB", "Alberta", 0.05),
            'V' to Triple("BC", "British Columbia", 0.12),
            'X' to Triple("NT", "Northwest Territories / Nunavut", 0.05),
            'Y' to Triple("YT", "Yukon", 0.05)
        )

        fun normaliserCodePostal(cp: String): String = cp.replace(" ", "").replace("-", "").uppercase()

        fun codePostalValide(cp: String): Boolean {
            val n = normaliserCodePostal(cp)
            return CP_CANADA.matches(n) || ZIP_USA.matches(n)
        }

        fun depuisCodePostal(cp: String): Region {
            val n = normaliserCodePostal(cp)
            if (CP_CANADA.matches(n)) {
                val (code, nom, taux) = PROVINCES[n[0]] ?: Triple("", "Canada", 0.05)
                return Region("CA", code, if (code.isEmpty()) "Canada" else "$nom, Canada", taux, "CAD")
            }
            if (ZIP_USA.matches(n)) {
                // Aux États-Unis, la taxe varie selon l'État et la ville : 7 % est une moyenne,
                // modifiable dans les réglages.
                return Region("US", "", "United States, ZIP $n", 0.07, "USD")
            }
            return Region("", "", "", 0.0, "CAD")
        }

        /** Code postal affiché proprement : « G1S 1T7 ». */
        fun afficherCodePostal(cp: String): String {
            val n = normaliserCodePostal(cp)
            return if (CP_CANADA.matches(n)) n.substring(0, 3) + " " + n.substring(3) else n
        }
    }
}
