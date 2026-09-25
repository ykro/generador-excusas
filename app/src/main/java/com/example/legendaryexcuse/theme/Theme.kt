package com.example.legendaryexcuse.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
  primary = Royal,
  onPrimary = Color.White,
  primaryContainer = RoyalContainer,
  onPrimaryContainer = Color(0xFF1E0F5C),
  secondary = Gold,
  onSecondary = Color.White,
  secondaryContainer = GoldContainer,
  onSecondaryContainer = Color(0xFF3D2A00),
  background = Parchment,
  surface = Parchment,
)

private val DarkColors = darkColorScheme(
  primary = RoyalLight,
  onPrimary = Color(0xFF2A1670),
  primaryContainer = RoyalContainerDark,
  onPrimaryContainer = RoyalContainer,
  secondary = GoldLight,
  onSecondary = Color(0xFF3D2A00),
  secondaryContainer = GoldContainerDark,
  onSecondaryContainer = GoldContainer,
  background = Night,
  surface = Night,
  surfaceVariant = NightSurface,
)

@Composable
fun ExcusaLegendariaTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
  MaterialTheme(colorScheme = if (darkTheme) DarkColors else LightColors, typography = Typography, content = content)
}
