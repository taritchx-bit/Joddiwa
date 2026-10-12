package com.tarit.slipledger.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tarit.slipledger.R
import com.tarit.slipledger.data.ThemeMode

val IncomeColor = Color(0xFF12B76A)
val ExpenseColor = Color(0xFFF04438)
val TransferColor = Color(0xFF8B8D98)
val WarningColor = Color(0xFFF79009)

/** Signature lime, used for fills: the add button, graph line, highlights. Text uses primary (darker) for contrast. */
val Lime = Color(0xFF9AD936)
val LimeDeep = Color(0xFF4E8A12)

/** Deep green "ink" for hero panels and charts, in both themes. */
val InkTop = Color(0xFF15301F)
val InkBottom = Color(0xFF0C1C13)
val HeroGradient = Brush.linearGradient(listOf(InkTop, InkBottom))

private val Plex = FontFamily(
    Font(R.font.plex_regular, FontWeight.Normal),
    Font(R.font.plex_medium, FontWeight.Medium),
    Font(R.font.plex_semibold, FontWeight.SemiBold),
    Font(R.font.plex_bold, FontWeight.Bold),
)

private fun Typography.withFont(f: FontFamily): Typography {
    fun TextStyle.f() = copy(fontFamily = f)
    return copy(
        displayLarge = displayLarge.f(), displayMedium = displayMedium.f(),
        displaySmall = displaySmall.f().copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
        headlineLarge = headlineLarge.f(), headlineMedium = headlineMedium.f().copy(fontWeight = FontWeight.Bold),
        headlineSmall = headlineSmall.f().copy(fontWeight = FontWeight.Bold),
        titleLarge = titleLarge.f().copy(fontWeight = FontWeight.SemiBold), titleMedium = titleMedium.f().copy(fontWeight = FontWeight.SemiBold),
        titleSmall = titleSmall.f().copy(fontWeight = FontWeight.SemiBold),
        bodyLarge = bodyLarge.f(), bodyMedium = bodyMedium.f(), bodySmall = bodySmall.f(),
        labelLarge = labelLarge.f().copy(fontWeight = FontWeight.Medium), labelMedium = labelMedium.f(), labelSmall = labelSmall.f(),
    )
}

private val Light = lightColorScheme(
    primary = LimeDeep,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE5F6CC),
    onPrimaryContainer = Color(0xFF213D05),
    secondary = Color(0xFF6172F3),
    onSecondary = Color.White,
    // Selected chips and segmented buttons use this, so it follows the lime brand.
    secondaryContainer = Color(0xFFE5F6CC),
    onSecondaryContainer = Color(0xFF213D05),
    tertiary = Color(0xFFF79009),
    tertiaryContainer = Color(0xFFFEF0C7),
    onTertiaryContainer = Color(0xFF7A2E0E),
    background = Color(0xFFF1F4EE),
    onBackground = Color(0xFF111A12),
    surface = Color(0xFFF1F4EE),
    onSurface = Color(0xFF111A12),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color(0xFFEAEFE6),
    surfaceContainerHigh = Color(0xFFE2E8DD),
    surfaceContainerHighest = Color(0xFFD8DFD2),
    onSurfaceVariant = Color(0xFF68756A),
    outline = Color(0xFFD0D8CC),
    outlineVariant = Color(0xFFE8EDE4),
)

private val Dark = darkColorScheme(
    primary = Lime,
    onPrimary = Color(0xFF142406),
    primaryContainer = Color(0xFF2A4510),
    onPrimaryContainer = Color(0xFFDDF5B8),
    secondary = Color(0xFF8098F9),
    secondaryContainer = Color(0xFF2A4510),
    onSecondaryContainer = Color(0xFFDDF5B8),
    tertiary = Color(0xFFFDB022),
    tertiaryContainer = Color(0xFF4E2A0B),
    onTertiaryContainer = Color(0xFFFEF0C7),
    background = Color(0xFF0A0E0B),
    surface = Color(0xFF0A0E0B),
    surfaceContainerLowest = Color(0xFF141A15),
    surfaceContainerLow = Color(0xFF141A15),
    surfaceContainer = Color(0xFF1B221C),
    surfaceContainerHigh = Color(0xFF242C25),
    surfaceContainerHighest = Color(0xFF2D362E),
    onSurfaceVariant = Color(0xFF97A398),
    outline = Color(0xFF38423A),
    outlineVariant = Color(0xFF1F2720),
)
@Composable
fun SlipLedgerTheme(mode: ThemeMode = ThemeMode.SYSTEM, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    MaterialTheme(
        colorScheme = if (dark) Dark else Light,
        typography = Typography().withFont(Plex),
        shapes = Shapes(
            extraSmall = RoundedCornerShape(8.dp),
            small = RoundedCornerShape(14.dp),
            medium = RoundedCornerShape(24.dp),
            large = RoundedCornerShape(30.dp),
            extraLarge = RoundedCornerShape(32.dp),
        ),
        content = content,
    )
}
