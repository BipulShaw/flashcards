package com.example.flashcards

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.flashcards.ui.theme.FlashcardsTheme
import com.example.flashcards.ui.theme.term

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
  cards: List<Card>,
  onSelect: (Card) -> Unit,
  onCreate: (term: String) -> Unit,
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
  createButtonModifier: Modifier = Modifier,
) {
  var query by remember { mutableStateOf("") }
  val trimmed = query.trim()

  val input = remember { FocusRequester() }
  val keyboard = LocalSoftwareKeyboardController.current
  LaunchedEffect(Unit) {
    input.requestFocus()
    keyboard?.show()
  }

  // A card's tint comes from its position in the feed's current order.
  val cardColors = FlashcardsTheme.cardColors
  val deckIndex = remember(cards) { cards.withIndex().associate { (i, card) -> card.id to i } }
  val tintOf = { card: Card -> cardColors.tintAt(deckIndex[card.id] ?: 0) }

  // Recomputed whenever the deck changes, so a deleted card never shows up here.
  val results =
    remember(cards, trimmed) {
      if (trimmed.isEmpty()) cards.sortedBy { it.term.lowercase() } else cards.filter { it.matches(trimmed) }
    }

  // Always expanded: the collapsed bar lives on the feed, and expands into this screen.
  // SearchBar handles system back itself, reporting it as a collapse.
  SearchBar(
    inputField = {
      SearchBarDefaults.InputField(
        query = query,
        onQueryChange = { query = it },
        onSearch = { keyboard?.hide() },
        expanded = true,
        onExpandedChange = { expanded -> if (!expanded) onBack() },
        placeholder = { Text("Search cards") },
        leadingIcon = {
          IconButton(onClick = onBack) {
            Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = "Back")
          }
        },
        trailingIcon =
          if (query.isEmpty()) null
          else {
            {
              IconButton(onClick = { query = "" }) {
                Icon(painterResource(R.drawable.ic_close), contentDescription = "Clear search")
              }
            }
          },
        modifier = Modifier.focusRequester(input),
      )
    },
    expanded = true,
    onExpandedChange = { expanded -> if (!expanded) onBack() },
    modifier = modifier,
  ) {
    when {
      trimmed.isEmpty() -> AllCards(sorted = results, tintOf = tintOf, onSelect = onSelect)
      results.isEmpty() ->
        NoMatches(query = trimmed, onCreate = onCreate, createButtonModifier = createButtonModifier)
      else ->
        ResultList {
          items(results, key = { it.id }) { card ->
            ResultCard(card = card, query = trimmed, tint = tintOf(card), onClick = { onSelect(card) })
          }
        }
    }
  }
}

@Composable
private fun ResultList(content: LazyListScope.() -> Unit) {
  LazyColumn(
    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
    modifier = Modifier.fillMaxSize().imePadding(),
    content = content,
  )
}

/** Every card, A to Z, under letter headers that stay pinned while their section scrolls. */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun AllCards(sorted: List<Card>, tintOf: (Card) -> Color, onSelect: (Card) -> Unit) {
  val sections =
    remember(sorted) {
      sorted.groupBy { card -> card.term.firstOrNull()?.uppercaseChar()?.takeIf { it.isLetter() } ?: '#' }
    }
  // Opaque, so cards scrolling under a pinned header don't show through it.
  val headerBackground = SearchBarDefaults.colors().containerColor

  ResultList {
    item(key = "count") {
      Text(
        text = "All cards · ${sorted.size}",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp),
      )
    }
    sections.forEach { (letter, section) ->
      stickyHeader(key = "letter-$letter") {
        Text(
          text = letter.toString(),
          style = MaterialTheme.typography.titleMedium.term(),
          color = MaterialTheme.colorScheme.primary,
          modifier = Modifier.fillMaxWidth().background(headerBackground).padding(start = 4.dp, top = 8.dp, bottom = 4.dp),
        )
      }
      items(section, key = { it.id }) { card ->
        ResultCard(card = card, query = "", tint = tintOf(card), onClick = { onSelect(card) })
      }
    }
  }
}

@Composable
private fun ResultCard(card: Card, query: String, tint: Color, onClick: () -> Unit) {
  val details = card.details
  val inDetails = query.isNotEmpty() && details != null && details.contains(query, ignoreCase = true)
  val colors = MaterialTheme.colorScheme

  Surface(
    onClick = onClick,
    shape = MaterialTheme.shapes.medium,
    color = FlashcardsTheme.cardColors.sheet,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 16.dp)) {
      // Filled with exactly the colour this card has in the feed. Those tints are close in lightness
      // to the sheet (pale in light mode, deep in dark), so the ring is the same hue pushed
      // towards the text colour, which keeps the dot visible without changing its colour.
      Box(
        modifier =
          Modifier.padding(top = 5.dp)
            .size(14.dp)
            .clip(CircleShape)
            .background(tint)
            .border(1.5.dp, lerp(tint, colors.onSurface, 0.4f), CircleShape)
      )
      Spacer(Modifier.width(14.dp))
      Column {
        HighlightedText(
          text = card.term,
          query = query,
          style = MaterialTheme.typography.titleMedium.term(),
          color = colors.onSurface,
          maxLines = 1,
        )
        Spacer(Modifier.height(2.dp))
        HighlightedText(
          text = card.gist,
          query = query,
          style = MaterialTheme.typography.bodyMedium,
          color = colors.onSurfaceVariant,
          maxLines = 2,
        )
        if (inDetails) {
          Spacer(Modifier.height(6.dp))
          HighlightedText(
            text = snippetAround(details, query),
            query = query,
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant.copy(alpha = 0.8f),
            maxLines = 1,
          )
        }
      }
    }
  }
}

@Composable
private fun NoMatches(query: String, onCreate: (String) -> Unit, createButtonModifier: Modifier) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
    modifier = Modifier.fillMaxSize().imePadding().padding(horizontal = 32.dp),
  ) {
    Icon(
      painter = painterResource(R.drawable.ic_search_off),
      contentDescription = null,
      tint = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.size(56.dp),
    )
    Spacer(Modifier.height(16.dp))
    Text(
      text = "No cards match ‘$query’",
      style = MaterialTheme.typography.bodyLarge,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(24.dp))
    FilledTonalButton(onClick = { onCreate(query) }, modifier = createButtonModifier) {
      Icon(painterResource(R.drawable.ic_add), contentDescription = null, modifier = Modifier.size(18.dp))
      Spacer(Modifier.width(8.dp))
      Text(text = "Create ‘$query’", maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
  }
}

/**
 * [text] with every occurrence of [query] marked by a rounded primaryContainer box. Span
 * backgrounds can only be rectangles, so the boxes are drawn behind the text from its layout, one
 * per line when a match wraps.
 */
@Composable
private fun HighlightedText(
  text: String,
  query: String,
  style: TextStyle,
  color: Color,
  maxLines: Int,
  modifier: Modifier = Modifier,
) {
  val box = MaterialTheme.colorScheme.primaryContainer
  val onBox = MaterialTheme.colorScheme.onPrimaryContainer
  val matches = remember(text, query) { matchRanges(text, query) }
  val annotated =
    remember(text, matches, onBox) {
      buildAnnotatedString {
        append(text)
        for (match in matches) addStyle(SpanStyle(color = onBox), match.first, match.last + 1)
      }
    }
  var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
  // A truncated line reports hidden characters at the ellipsis, so marks there must stop short of it.
  val measurer = rememberTextMeasurer()
  val ellipsisWidth = remember(style, measurer) { measurer.measure("\u2026", style).size.width.toFloat() }

  Text(
    text = annotated,
    style = style,
    color = color,
    maxLines = maxLines,
    overflow = TextOverflow.Ellipsis,
    onTextLayout = { layout = it },
    modifier =
      modifier.drawBehind {
        val lines = layout ?: return@drawBehind
        val padX = 2.dp.toPx()
        val radius = CornerRadius(5.dp.toPx())
        for (match in matches) {
          val end = match.last + 1
          val firstLine = lines.getLineForOffset(match.first)
          val lastLine = lines.getLineForOffset(match.last)
          for (line in firstLine..lastLine) {
            val from = maxOf(match.first, lines.getLineStart(line))
            val to = minOf(end, lines.getLineEnd(line))
            if (from >= to) continue
            val visibleRight =
              if (lines.isLineEllipsized(line)) lines.getLineRight(line) - ellipsisWidth else size.width
            val left = lines.getHorizontalPosition(from, usePrimaryDirection = true)
            val right = minOf(lines.getHorizontalPosition(to, usePrimaryDirection = true), visibleRight)
            // A match that starts under or after the ellipsis has nothing visible to mark.
            if (right <= left) continue
            val top = lines.getLineTop(line)
            drawRoundRect(
              color = box,
              topLeft = Offset(left - padX, top),
              size = Size(right - left + 2 * padX, lines.getLineBottom(line) - top),
              cornerRadius = radius,
            )
          }
        }
      },
  )
}

private fun Card.matches(query: String): Boolean =
  term.contains(query, ignoreCase = true) ||
    gist.contains(query, ignoreCase = true) ||
    details?.contains(query, ignoreCase = true) == true

private fun matchRanges(text: String, query: String): List<IntRange> {
  if (query.isEmpty()) return emptyList()
  val ranges = mutableListOf<IntRange>()
  var from = 0
  while (true) {
    val at = text.indexOf(query, from, ignoreCase = true)
    if (at < 0) return ranges
    ranges += at until at + query.length
    from = at + query.length
  }
}

/**
 * Details cut to start a few words before the first match, so the match lands early on the one
 * line that is shown. The line's end is left to the text's ellipsis.
 */
private fun snippetAround(details: String, query: String): String {
  val at = details.indexOf(query, ignoreCase = true)
  if (at <= 24) return details
  val lead = at - 24
  val wordStart = details.indexOf(' ', lead).let { if (it in lead until at) it + 1 else lead }
  return "…" + details.substring(wordStart)
}
