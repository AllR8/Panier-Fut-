package ca.panierfute.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight

// Palette « marché » : vert feuille, paprika, et jaune étiquette pour les rabais.
private val Clair = lightColorScheme(
    primary = Color(0xFF2F6B45),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFCDEBD3),
    onPrimaryContainer = Color(0xFF0B2614),
    secondary = Color(0xFFA9531F),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFDCC6),
    onSecondaryContainer = Color(0xFF361400),
    tertiary = Color(0xFF7A6000),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFE07A),
    onTertiaryContainer = Color(0xFF261D00),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    background = Color(0xFFFAF7F0),
    onBackground = Color(0xFF1C1C17),
    surface = Color(0xFFFAF7F0),
    onSurface = Color(0xFF1C1C17),
    surfaceVariant = Color(0xFFE5E3D6),
    onSurfaceVariant = Color(0xFF47483F),
    outline = Color(0xFF77786E),
    outlineVariant = Color(0xFFC8C7BB),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF5F2EA),
    surfaceContainer = Color(0xFFEFECE3),
    surfaceContainerHigh = Color(0xFFE9E6DD),
    surfaceContainerHighest = Color(0xFFE3E1D7)
)

private val Sombre = darkColorScheme(
    primary = Color(0xFF9FD4AE),
    onPrimary = Color(0xFF00391C),
    primaryContainer = Color(0xFF16512E),
    onPrimaryContainer = Color(0xFFCDEBD3),
    secondary = Color(0xFFFFB68E),
    onSecondary = Color(0xFF552100),
    secondaryContainer = Color(0xFF773300),
    onSecondaryContainer = Color(0xFFFFDCC6),
    tertiary = Color(0xFFE9C449),
    onTertiary = Color(0xFF3F2E00),
    tertiaryContainer = Color(0xFF5C4800),
    onTertiaryContainer = Color(0xFFFFE07A),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),
    background = Color(0xFF12140F),
    onBackground = Color(0xFFE4E3DA),
    surface = Color(0xFF12140F),
    onSurface = Color(0xFFE4E3DA),
    surfaceVariant = Color(0xFF45483F),
    onSurfaceVariant = Color(0xFFC7C8BB),
    outline = Color(0xFF919286),
    outlineVariant = Color(0xFF45483F),
    surfaceContainerLowest = Color(0xFF0D0F0A),
    surfaceContainerLow = Color(0xFF1A1C17),
    surfaceContainer = Color(0xFF1E201B),
    surfaceContainerHigh = Color(0xFF282A24),
    surfaceContainerHighest = Color(0xFF33352F)
)

private val Base = Typography()
private val Typo = Base.copy(
    headlineMedium = Base.headlineMedium.copy(fontWeight = FontWeight.Bold),
    titleLarge = Base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = Base.titleMedium.copy(fontWeight = FontWeight.SemiBold)
)

@Composable
fun ThemePanier(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) Sombre else Clair,
        typography = Typo,
        content = content
    )
}
