package com.example.flashcards.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import java.io.File

/**
 * Terms are set in the platform's own serif, Noto Serif, so no font is downloaded or bundled.
 *
 * It is read from its file rather than requested as FontFamily.Serif, because some manufacturers'
 * system-font settings (ColorOS's, for one) substitute their own sans for every family, serif
 * included. Where the file isn't at the standard path, FontFamily.Serif is the fallback.
 */
val TermFamily: FontFamily = platformSerif() ?: FontFamily.Serif

private fun platformSerif(): FontFamily? {
  val regular = File("/system/fonts/NotoSerif-Regular.ttf")
  if (!regular.canRead()) return null
  val bold = File("/system/fonts/NotoSerif-Bold.ttf")
  val fonts = buildList {
    add(Font(regular, FontWeight.Normal))
    if (bold.canRead()) add(Font(bold, FontWeight.Bold))
  }
  return FontFamily(fonts)
}

private val Base = Typography()

private fun TextStyle.asTerm() = copy(fontFamily = TermFamily, fontWeight = FontWeight.Normal)

// Display and headline sizes carry terms and the few large headings, so they are serif. Title,
// body and label stay in the default sans, which keeps running text and controls easy to read.
val Typography =
  Base.copy(
    displayLarge = Base.displayLarge.asTerm(),
    displayMedium = Base.displayMedium.asTerm(),
    displaySmall = Base.displaySmall.asTerm(),
    headlineLarge = Base.headlineLarge.asTerm(),
    headlineMedium = Base.headlineMedium.asTerm(),
    headlineSmall = Base.headlineSmall.asTerm(),
  )

/** A term at a title size, e.g. in a list, where the scale's title styles are sans. */
fun TextStyle.term(): TextStyle = copy(fontFamily = TermFamily)
