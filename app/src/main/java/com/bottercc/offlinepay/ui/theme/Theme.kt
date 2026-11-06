package com.bottercc.offlinepay.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** iOS-style system colours, resolved for the current appearance. */
data class IosColors(
    val accent: Color,
    val green: Color,
    val red: Color,
    val blue: Color,
    val indigo: Color,
    val teal: Color,
    val purple: Color,
    val orange: Color,
    val gray: Color,
    /** systemGroupedBackground / secondarySystemGroupedBackground / tertiarySystemFill */
    val groupedBg: Color,
    val card: Color,
    val fill: Color,
    val label: Color,
    val secondaryLabel: Color,
    val separator: Color,
)

private val LightIos = IosColors(
    accent = Color(0xFFFF6A00), green = Color(0xFF34C759), red = Color(0xFFFF3B30), blue = Color(0xFF007AFF),
    indigo = Color(0xFF5856D6), teal = Color(0xFF30B0C7), purple = Color(0xFFAF52DE), orange = Color(0xFFFF9500),
    gray = Color(0xFF8E8E93),
    groupedBg = Color(0xFFF2F2F7), card = Color.White, fill = Color(0xFFE9E9EE),
    label = Color(0xFF000000), secondaryLabel = Color(0xFF8A8A8E), separator = Color(0xFFD1D1D6),
)

private val DarkIos = IosColors(
    accent = Color(0xFFFF7A1F), green = Color(0xFF30D158), red = Color(0xFFFF453A), blue = Color(0xFF0A84FF),
    indigo = Color(0xFF5E5CE6), teal = Color(0xFF40C8E0), purple = Color(0xFFBF5AF2), orange = Color(0xFFFF9F0A),
    gray = Color(0xFF8E8E93),
    groupedBg = Color(0xFF000000), card = Color(0xFF1C1C1E), fill = Color(0xFF2C2C2E),
    label = Color.White, secondaryLabel = Color(0xFF8E8E93), separator = Color(0xFF38383A),
)

val LocalIos = staticCompositionLocalOf { LightIos }

/** Whether the app is currently drawn dark (follows the user's theme setting). */
val LocalDarkTheme = staticCompositionLocalOf { false }

// Brand gradient for the home header.
val Brand = Color(0xFFFC5502)
val BrandDeep = Color(0xFFD94300)

// Kept for code that colours statuses.
val Success = Color(0xFF34C759)
val Danger = Color(0xFFFF3B30)
val Amber = Color(0xFFFF9500)

private fun scheme(c: IosColors, dark: Boolean) = (if (dark) darkColorScheme() else lightColorScheme()).copy(
    primary = c.accent,
    onPrimary = Color.White,
    primaryContainer = c.fill,
    onPrimaryContainer = c.label,
    secondary = c.green,
    onSecondary = Color.White,
    secondaryContainer = c.fill,
    onSecondaryContainer = c.label,
    tertiary = c.blue,
    background = c.groupedBg,
    onBackground = c.label,
    surface = c.card,
    onSurface = c.label,
    surfaceVariant = c.fill,
    onSurfaceVariant = c.secondaryLabel,
    surfaceContainer = c.card,
    surfaceContainerHigh = c.card,
    surfaceContainerHighest = c.fill,
    outline = c.separator,
    outlineVariant = c.separator,
    error = c.red,
)

// iOS text styles: Large Title 34, Title 2 22, Headline 17 semibold, Body 17, Subheadline 15, Footnote 13, Caption 12.
private val IosType = Typography(
    displaySmall = TextStyle(fontSize = 34.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.2.sp),
    headlineMedium = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold),
    headlineSmall = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 17.sp),
    bodyMedium = TextStyle(fontSize = 15.sp),
    bodySmall = TextStyle(fontSize = 13.sp),
    labelLarge = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = 11.sp),
)

@Composable
fun OfflinePayTheme(dark: Boolean, content: @Composable () -> Unit) {
    val ios = if (dark) DarkIos else LightIos
    CompositionLocalProvider(LocalDarkTheme provides dark, LocalIos provides ios) {
        MaterialTheme(colorScheme = scheme(ios, dark), typography = IosType, content = content)
    }
}
