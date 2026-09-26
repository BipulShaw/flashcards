package com.example.flashcards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.flashcards.ui.theme.FlashcardsTheme

/**
 * The endless feed. [searchBarModifier] and [fabModifier] let the caller attach the transitions
 * that expand the search bar into search and the + button into the editor. Controls for the whole
 * app float at the edges: search and settings at the top, + at the bottom. A card's own control,
 * delete, sits on the card.
 */
@Composable
fun FeedScreen(
  cards: List<Card>,
  initialIndex: Int,
  onCurrentIndexChange: (Int) -> Unit,
  onAddCard: () -> Unit,
  onOpenSearch: () -> Unit,
  onOpenSettings: () -> Unit,
  onDelete: (Card) -> Unit,
  showLinkPreviews: Boolean,
  modifier: Modifier = Modifier,
  searchBarModifier: Modifier = Modifier,
  fabModifier: Modifier = Modifier,
) {
  Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
    if (cards.isEmpty()) {
      // The pager is not composed at all on an empty deck, so its modulo indexing can never
      // divide by zero.
      Text(
        text = "No cards yet",
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.align(Alignment.Center),
      )
    } else {
      CardPager(
        cards = cards,
        initialIndex = initialIndex,
        onCurrentIndexChange = onCurrentIndexChange,
        onDelete = onDelete,
        showLinkPreviews = showLinkPreviews,
      )
    }
    // Drawn after the pager so it floats above the card; the card's text starts below it. Shown on
    // an empty deck as well, since it's also the way into settings.
    CollapsedSearchBar(
      onClick = onOpenSearch,
      onOpenSettings = onOpenSettings,
      modifier =
        Modifier.align(Alignment.TopStart)
          .safeDrawingPadding()
          .padding(start = 16.dp, top = 8.dp, end = 16.dp)
          .then(searchBarModifier)
          .fillMaxWidth(),
    )
    FloatingActionButton(
      onClick = onAddCard,
      modifier = Modifier.align(Alignment.BottomEnd).safeDrawingPadding().padding(24.dp).then(fabModifier),
    ) {
      Icon(painterResource(R.drawable.ic_add), contentDescription = "New card")
    }
  }
}

@Composable
private fun CardPager(
  cards: List<Card>,
  initialIndex: Int,
  onCurrentIndexChange: (Int) -> Unit,
  onDelete: (Card) -> Unit,
  showLinkPreviews: Boolean,
) {
  // A huge page count with modulo indexing makes the feed endless; starting in the middle, on a
  // multiple of the deck size, means the pager opens on cards[initialIndex] and swiping up or
  // down both work straight away.
  val middle = Int.MAX_VALUE / 2
  val start = middle - middle % cards.size + initialIndex.coerceIn(0, cards.lastIndex)
  val pagerState = rememberPagerState(initialPage = start) { Int.MAX_VALUE }

  // Reported back so leaving for another screen and returning lands on the same card.
  LaunchedEffect(pagerState, cards.size) {
    snapshotFlow { pagerState.currentPage }.collect { onCurrentIndexChange(it % cards.size) }
  }

  val cardColors = FlashcardsTheme.cardColors
  VerticalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
    val index = page % cards.size
    val card = cards[index]
    CardPage(
      card = card,
      tint = cardColors.tintAt(index),
      onDelete = { onDelete(card) },
      showLinkPreviews = showLinkPreviews,
    )
  }
}

@Composable
private fun CardPage(
  card: Card,
  tint: Color,
  onDelete: () -> Unit,
  showLinkPreviews: Boolean,
  modifier: Modifier = Modifier,
) {
  val cardColors = FlashcardsTheme.cardColors
  val type = MaterialTheme.typography

  // Links open in the browser; on a phone without one, a tap simply does nothing.
  val uriHandler = LocalUriHandler.current
  val openLink: (String) -> Unit = remember(uriHandler) { { url -> runCatching { uriHandler.openUri(url) } } }
  val primary = MaterialTheme.colorScheme.primary
  val linkStyles =
    remember(primary) {
      TextLinkStyles(
        style = SpanStyle(color = primary, textDecoration = TextDecoration.Underline),
        pressedStyle = SpanStyle(color = primary, background = primary.copy(alpha = 0.12f)),
      )
    }

  // The tint fills the whole page, including behind the system bars; only the text is inset.
  Box(modifier = modifier.fillMaxSize().background(tint)) {
    // Long-press selects and copies the card's text. A plain drag is not a long-press, so it
    // still reaches the pager and swipes the page as usual.
    SelectionContainer {
      Column(
        modifier =
          Modifier.safeDrawingPadding()
            // Clears the search bar floating above, and the delete and + buttons below.
            .padding(start = 28.dp, end = 28.dp, top = 88.dp, bottom = 96.dp)
      ) {
        Text(text = card.term, style = type.displaySmall, color = cardColors.ink)
        HorizontalDivider(color = cardColors.rule, modifier = Modifier.padding(vertical = 18.dp))
        Text(
          text = remember(card.gist, linkStyles) { linkify(card.gist, linkStyles, openLink) },
          style = type.titleMedium,
          lineHeight = 28.sp,
          color = cardColors.inkMuted,
        )
        card.details?.let { details ->
          Spacer(Modifier.height(24.dp))
          Text(
            text = remember(details, linkStyles) { linkify(details, linkStyles, openLink) },
            style = type.bodyLarge,
            lineHeight = 26.sp,
            color = cardColors.inkFaint,
          )
          if (showLinkPreviews) {
            // A preview under the body for each link in it, up to two.
            val urls = remember(details) { findLinks(details).map { it.url }.distinct().take(2) }
            // Tapped, not selected: long-press selection stays on the card's own words.
            DisableSelection {
              for (url in urls) {
                Spacer(Modifier.height(16.dp))
                LinkPreviewCard(url = url, onOpen = openLink)
              }
            }
          }
        }
      }
    }
    // Part of the page, so it travels with its card, level with the + button across the bottom.
    // One tap, not a long-press (which selects text): the snackbar that follows offers Undo.
    IconButton(
      onClick = onDelete,
      modifier = Modifier.align(Alignment.BottomStart).safeDrawingPadding().padding(start = 16.dp, bottom = 28.dp),
    ) {
      Icon(
        painterResource(R.drawable.ic_delete),
        contentDescription = "Delete card",
        tint = cardColors.inkFaint,
      )
    }
  }
}

/**
 * Looks like the Material search bar at rest: tapping it opens the full search screen, and its
 * trailing gear opens settings.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CollapsedSearchBar(onClick: () -> Unit, onOpenSettings: () -> Unit, modifier: Modifier = Modifier) {
  Surface(
    onClick = onClick,
    shape = SearchBarDefaults.inputFieldShape,
    color = SearchBarDefaults.colors().containerColor,
    modifier = modifier.height(SearchBarDefaults.InputFieldHeight),
  ) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 16.dp, end = 4.dp)) {
      Icon(
        painterResource(R.drawable.ic_search),
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(Modifier.width(16.dp))
      Text(
        text = "Search cards",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.weight(1f),
      )
      IconButton(onClick = onOpenSettings) {
        Icon(
          painterResource(R.drawable.ic_settings),
          contentDescription = "Settings",
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}
