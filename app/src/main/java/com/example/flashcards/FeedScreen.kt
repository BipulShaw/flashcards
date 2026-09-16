package com.example.flashcards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** One soft background per card, walked around the hue wheel so neighbouring swipes look distinct. */
private val cardColors =
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
  )

/** Text sits on pale backgrounds in either system theme, so the ink colour is fixed. */
private val Ink = Color(0xFF17171B)

@Composable
fun FeedScreen(
  cards: List<Card>,
  initialIndex: Int,
  onCurrentIndexChange: (Int) -> Unit,
  onAddCard: () -> Unit,
  modifier: Modifier = Modifier,
) {
  // A huge page count with modulo indexing makes the feed endless; starting in the middle, on a
  // multiple of the deck size, means the pager opens on cards[initialIndex] and swiping up or
  // down both work straight away.
  val middle = Int.MAX_VALUE / 2
  val start = middle - middle % cards.size + initialIndex.coerceIn(0, cards.lastIndex)
  val pagerState = rememberPagerState(initialPage = start) { Int.MAX_VALUE }

  // Reported back so leaving for the add screen and returning lands on the same card.
  LaunchedEffect(pagerState, cards.size) {
    snapshotFlow { pagerState.currentPage }.collect { onCurrentIndexChange(it % cards.size) }
  }

  Box(modifier = modifier.fillMaxSize()) {
    VerticalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
      val index = page % cards.size
      CardPage(card = cards[index], background = cardColors[index % cardColors.size])
    }
    FloatingActionButton(
      onClick = onAddCard,
      modifier = Modifier.align(Alignment.BottomEnd).safeDrawingPadding().padding(24.dp),
    ) {
      Text(text = "+", style = MaterialTheme.typography.headlineMedium)
    }
  }
}

@Composable
private fun CardPage(card: Card, background: Color, modifier: Modifier = Modifier) {
  // The colour fills the whole page, including behind the system bars; only the text is inset.
  Box(modifier = modifier.fillMaxSize().background(background)) {
    Column(modifier = Modifier.safeDrawingPadding().padding(horizontal = 28.dp, vertical = 36.dp)) {
      Text(
        text = card.term,
        style = MaterialTheme.typography.displaySmall,
        fontWeight = FontWeight.Bold,
        color = Ink,
      )
      Spacer(Modifier.height(20.dp))
      Text(
        text = card.gist,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Medium,
        lineHeight = 28.sp,
        color = Ink.copy(alpha = 0.85f),
      )
      card.details?.let { details ->
        Spacer(Modifier.height(28.dp))
        Text(
          text = details,
          style = MaterialTheme.typography.bodyLarge,
          lineHeight = 26.sp,
          color = Ink.copy(alpha = 0.66f),
        )
      }
    }
  }
}
