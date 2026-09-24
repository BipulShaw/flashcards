package com.example.flashcards.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

// Paper and ink: warm neutrals for the page, fountain-pen blue as the accent, and the red of an
// index card's header rule as the tertiary colour.

internal val LightColors =
  lightColorScheme(
    primary = Color(0xFF3B5B8A),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD5E3FF),
    onPrimaryContainer = Color(0xFF173559),
    inversePrimary = Color(0xFFA8C7FA),
    secondary = Color(0xFF6D5D4B),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF4E1CB),
    onSecondaryContainer = Color(0xFF3F3021),
    tertiary = Color(0xFF9C4238),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFDAD5),
    onTertiaryContainer = Color(0xFF7D2B22),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF93000A),
    background = Color(0xFFFBF8F3),
    onBackground = Color(0xFF1E1B17),
    surface = Color(0xFFFBF8F3),
    onSurface = Color(0xFF1E1B17),
    surfaceVariant = Color(0xFFEEE7DD),
    onSurfaceVariant = Color(0xFF4B463E),
    surfaceTint = Color(0xFF3B5B8A),
    inverseSurface = Color(0xFF33302B),
    inverseOnSurface = Color(0xFFF5F0E9),
    outline = Color(0xFF7C766C),
    outlineVariant = Color(0xFFCEC6BB),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFFFBF8F3),
    surfaceDim = Color(0xFFDCD8D1),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF6F2EC),
    surfaceContainer = Color(0xFFF0ECE5),
    surfaceContainerHigh = Color(0xFFEAE6DF),
    surfaceContainerHighest = Color(0xFFE4E0D9),
  )

internal val DarkColors =
  darkColorScheme(
    primary = Color(0xFFA8C7FA),
    onPrimary = Color(0xFF0A305F),
    primaryContainer = Color(0xFF24466F),
    onPrimaryContainer = Color(0xFFD5E3FF),
    inversePrimary = Color(0xFF3B5B8A),
    secondary = Color(0xFFD8C4AD),
    onSecondary = Color(0xFF3B2F20),
    secondaryContainer = Color(0xFF544535),
    onSecondaryContainer = Color(0xFFF4E1CB),
    tertiary = Color(0xFFFFB4A9),
    onTertiary = Color(0xFF5E1510),
    tertiaryContainer = Color(0xFF7D2B22),
    onTertiaryContainer = Color(0xFFFFDAD5),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF16130F),
    onBackground = Color(0xFFE9E2D9),
    surface = Color(0xFF16130F),
    onSurface = Color(0xFFE9E2D9),
    surfaceVariant = Color(0xFF4B463E),
    onSurfaceVariant = Color(0xFFCEC6BB),
    surfaceTint = Color(0xFFA8C7FA),
    inverseSurface = Color(0xFFE9E2D9),
    inverseOnSurface = Color(0xFF33302B),
    outline = Color(0xFF979084),
    outlineVariant = Color(0xFF4B463E),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFF3D3934),
    surfaceDim = Color(0xFF16130F),
    surfaceContainerLowest = Color(0xFF110E0A),
    surfaceContainerLow = Color(0xFF1E1B17),
    surfaceContainer = Color(0xFF231F1A),
    surfaceContainerHigh = Color(0xFF2D2A24),
    surfaceContainerHighest = Color(0xFF38342E),
  )

/**
 * Colours for the cards themselves, which sit outside the Material roles: every card has its own
 * tint, and text on a tint uses ink rather than onSurface.
 */
@Immutable
class CardColors(
  /** One soft tint per card, walked around the hue wheel so neighbouring cards look distinct. */
  val tints: List<Color>,
  /** Terms. */
  val ink: Color,
  /** Gists. */
  val inkMuted: Color,
  /** Details, placeholders and quiet icons. */
  val inkFaint: Color,
  /** The ruled line under a card's term, like the red header line on an index card. */
  val rule: Color,
  /** A card drawn as a list item, e.g. a search result, on the search screen's background. */
  val sheet: Color,
) {
  /** The tint of the card at [deckIndex] in the feed's current order. */
  fun tintAt(deckIndex: Int): Color = tints[Math.floorMod(deckIndex, tints.size)]
}

internal val LightCardColors =
  CardColors(
    tints =
      listOf(
        Color(0xFFFFE0E0),
        Color(0xFFFFEBCB),
        Color(0xFFFCF6C9),
        Color(0xFFE2F0CB),
        Color(0xFFC8EDDF),
        Color(0xFFCDE7F5),
        Color(0xFFD7DCF7),
        Color(0xFFE6D9F2),
        Color(0xFFF7D9EB),
        Color(0xFFE8E0D5),
      ),
    ink = Color(0xFF1E1B17),
    inkMuted = Color(0xFF1E1B17).copy(alpha = 0.82f),
    inkFaint = Color(0xFF1E1B17).copy(alpha = 0.6f),
    rule = Color(0xFF9C4238).copy(alpha = 0.35f),
    sheet = Color(0xFFFFFDF9),
  )

// The same hues, deep and muted, so a dark feed keeps its variety without glowing.
internal val DarkCardColors =
  CardColors(
    tints =
      listOf(
        Color(0xFF3D2A2B),
        Color(0xFF3D3122),
        Color(0xFF393520),
        Color(0xFF2E3624),
        Color(0xFF213832),
        Color(0xFF213443),
        Color(0xFF2A2F47),
        Color(0xFF332A40),
        Color(0xFF3D2837),
        Color(0xFF36312A),
      ),
    ink = Color(0xFFEDE5DA),
    inkMuted = Color(0xFFEDE5DA).copy(alpha = 0.85f),
    inkFaint = Color(0xFFEDE5DA).copy(alpha = 0.62f),
    rule = Color(0xFFFFB4A9).copy(alpha = 0.35f),
    sheet = Color(0xFF3B362F),
  )
