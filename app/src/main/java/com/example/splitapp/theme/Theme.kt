package com.example.splitapp.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
  primary = LightPrimary, onPrimary = LightOnPrimary,
  primaryContainer = LightPrimaryContainer, onPrimaryContainer = LightOnPrimaryContainer,
  secondary = LightSecondary, onSecondary = Color.White,
  secondaryContainer = LightSecondaryContainer, onSecondaryContainer = LightOnSecondaryContainer,
  tertiary = LightTertiary, onTertiary = Color.White,
  tertiaryContainer = LightTertiaryContainer, onTertiaryContainer = LightOnTertiaryContainer,
  error = LightError, onError = Color.White,
  errorContainer = LightErrorContainer, onErrorContainer = LightOnErrorContainer,
  background = LightBackground, onBackground = LightOnSurface,
  surface = LightSurface, onSurface = LightOnSurface,
  surfaceVariant = LightSurfaceVariant, onSurfaceVariant = LightOnSurfaceVariant,
  outline = LightOutline, outlineVariant = LightOutlineVariant,
  // The tonal surfaces dialogs, sheets and menus use: brand-neutral instead of the default lavender tint.
  surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFF1F5F7), surfaceContainer = Color(0xFFEBF0F3),
  surfaceContainerHigh = Color(0xFFE5ECEF), surfaceContainerHighest = Color(0xFFDFE7EB),
)

private val DarkColorScheme = darkColorScheme(
  primary = DarkPrimary, onPrimary = DarkOnPrimary,
  primaryContainer = DarkPrimaryContainer, onPrimaryContainer = DarkOnPrimaryContainer,
  secondary = DarkSecondary, onSecondary = Color(0xFF00363F),
  secondaryContainer = DarkSecondaryContainer, onSecondaryContainer = DarkOnSecondaryContainer,
  tertiary = DarkTertiary, onTertiary = Color(0xFF0A2E10),
  tertiaryContainer = DarkTertiaryContainer, onTertiaryContainer = DarkOnTertiaryContainer,
  error = DarkError, onError = Color(0xFF3B0A0A),
  errorContainer = DarkErrorContainer, onErrorContainer = DarkOnErrorContainer,
  background = DarkBackground, onBackground = DarkOnSurface,
  surface = DarkSurface, onSurface = DarkOnSurface,
  surfaceVariant = DarkSurfaceVariant, onSurfaceVariant = DarkOnSurfaceVariant,
  outline = DarkOutline, outlineVariant = DarkOutlineVariant,
  surfaceContainerLowest = Color(0xFF0B181D), surfaceContainerLow = Color(0xFF152830), surfaceContainer = DarkSurface,
  surfaceContainerHigh = Color(0xFF213A44), surfaceContainerHighest = Color(0xFF28444F),
)

/**
 * One brand theme, light and dark. No dynamic (wallpaper) colour: it made the top bars purple or green next to the
 * fixed brand teal, so the app looked like three different apps.
 */
@Composable
fun SplitAppTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  MaterialTheme(colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme, typography = Typography, content = content)
}
