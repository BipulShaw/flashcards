package com.example.flashcards.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

private val LocalCardColors = staticCompositionLocalOf { LightCardColors }

/**
 * Follows the system light/dark setting. Dynamic colour is deliberately off: the paper-and-ink
 * palette is the look, and wallpaper colours would replace it.
 */
@Composable
fun FlashcardsTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
  CompositionLocalProvider(LocalCardColors provides if (darkTheme) DarkCardColors else LightCardColors) {
    MaterialTheme(
      colorScheme = if (darkTheme) DarkColors else LightColors,
      typography = Typography,
      shapes = Shapes,
      content = content,
    )
  }
}

object FlashcardsTheme {
  val cardColors: CardColors
    @Composable @ReadOnlyComposable get() = LocalCardColors.current
}
