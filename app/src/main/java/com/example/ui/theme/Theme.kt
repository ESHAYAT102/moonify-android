package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val CatppuccinMochaColorScheme = darkColorScheme(
  primary = MochaMauve,
  onPrimary = MochaCrust,
  primaryContainer = MochaSurface0,
  onPrimaryContainer = MochaMauve,
  secondary = MochaLavender,
  onSecondary = MochaCrust,
  tertiary = MochaYellow,
  onTertiary = MochaCrust,
  background = MochaBase,
  onBackground = MochaText,
  surface = MochaBase,
  onSurface = MochaText,
  surfaceVariant = MochaSurface0,
  onSurfaceVariant = MochaSubtext1,
  outline = MochaSurface1,
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Force dark theme for the starry, cosmic theme
  dynamicColor: Boolean = false, // Disable dynamic colors to stick to Catppuccin Mauve
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) {
    CatppuccinMochaColorScheme
  } else {
    // Elegant fallbacks (always use mocha for consistency)
    CatppuccinMochaColorScheme
  }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

