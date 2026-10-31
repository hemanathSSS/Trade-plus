package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val TradePulseDarkColorScheme = darkColorScheme(
  primary = EmeraldAccent,
  onPrimary = DarkNavyBg,
  primaryContainer = EmeraldDark.copy(alpha = 0.2f),
  onPrimaryContainer = EmeraldLight,
  secondary = CyanAccent,
  onSecondary = DarkNavyBg,
  secondaryContainer = CyanDark.copy(alpha = 0.2f),
  onSecondaryContainer = CyanAccent,
  tertiary = AmberAlert,
  onTertiary = DarkNavyBg,
  tertiaryContainer = AmberDark.copy(alpha = 0.2f),
  onTertiaryContainer = AmberAlert,
  background = DarkNavyBg,
  onBackground = TextPrimary,
  surface = SurfaceCard,
  onSurface = TextPrimary,
  surfaceVariant = SurfaceCardLight,
  onSurfaceVariant = TextSecondary,
  outline = SurfaceBorder,
  error = CoralRed,
  onError = Color.White
)

@Composable
fun TradePulseTheme(
  darkTheme: Boolean = true, // Default to sleek financial terminal dark mode
  content: @Composable () -> Unit
) {
  MaterialTheme(
    colorScheme = TradePulseDarkColorScheme,
    typography = Typography,
    content = content
  )
}
