package com.example.flashcards

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.flashcards.ui.theme.FlashcardsTheme

/**
 * A link written on a card, shown as a small preview of the page: thumbnail, title, a short summary
 * and the site. Until the page answers (or if it never does) it shows the address, so it's always
 * a working link. Tapping it calls [onOpen].
 */
@Composable
fun LinkPreviewCard(url: String, onOpen: (String) -> Unit, modifier: Modifier = Modifier) {
  val context = LocalContext.current
  val preview by
    produceState(initialValue = LinkPreviews.cached(url), url) {
      if (value == null) value = LinkPreviews.load(context, url)
    }
  val cardColors = FlashcardsTheme.cardColors
  val type = MaterialTheme.typography

  Surface(
    onClick = { onOpen(url) },
    shape = MaterialTheme.shapes.medium,
    // A paper insert on the card, whatever the card's tint.
    color = cardColors.sheet.copy(alpha = 0.72f),
    border = BorderStroke(1.dp, cardColors.ink.copy(alpha = 0.08f)),
    modifier = modifier.fillMaxWidth(),
  ) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(10.dp)) {
      Thumbnail(preview = preview, modifier = Modifier.size(64.dp))
      Spacer(Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = preview?.title ?: displayUrl(url),
          style = type.titleSmall,
          color = cardColors.ink,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
        preview?.summary?.let { summary ->
          Spacer(Modifier.height(2.dp))
          Text(
            text = summary,
            style = type.bodySmall,
            color = cardColors.inkMuted,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
          )
        }
        Spacer(Modifier.height(4.dp))
        Text(
          text = preview?.site ?: LinkPreviews.hostOf(url),
          style = type.labelSmall,
          color = cardColors.inkFaint,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

@Composable
private fun Thumbnail(preview: LinkPreview?, modifier: Modifier = Modifier) {
  val cardColors = FlashcardsTheme.cardColors
  Box(
    contentAlignment = Alignment.Center,
    modifier = modifier.clip(MaterialTheme.shapes.small).background(cardColors.ink.copy(alpha = 0.06f)),
  ) {
    val image = preview?.image
    when {
      image == null ->
        Icon(painterResource(R.drawable.ic_link), contentDescription = null, tint = cardColors.inkFaint)
      // A site icon is shown whole, with room around it; a picture of the page fills the square.
      preview.imageIsIcon ->
        Image(image, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize().padding(14.dp))
      else -> Image(image, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
    }
  }
}
