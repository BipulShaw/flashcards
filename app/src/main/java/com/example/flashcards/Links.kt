package com.example.flashcards

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString

/** A web link found in a card's text: where it sits in the text, and the address it opens. */
class FoundLink(val range: IntRange, val url: String)

// Explicit web addresses only (http://, https:// or www.), so names like "Node.js" in a card
// never turn into links by accident.
private val WebAddress = Regex("""(?:\bhttps?://|\bwww\.)[^\s<>"]+""", RegexOption.IGNORE_CASE)

/** Every web link in [text], in order. */
fun findLinks(text: String): List<FoundLink> =
  WebAddress.findAll(text)
    .mapNotNull { match ->
      val address = match.value.trimEndPunctuation()
      val rest = address.substringAfter("://", address).removePrefix("www.").removePrefix("WWW.")
      // A bare "https://" or "www." with nothing after it isn't a link.
      if (rest.none { it.isLetterOrDigit() }) return@mapNotNull null
      val start = match.range.first
      val url =
        if (address.startsWith("www.", ignoreCase = true)) "https://$address"
        // Keyboards capitalise the start of a sentence ("Https://"), but Android only hands a
        // lower-case scheme to the browser. The rest of the address is left exactly as written.
        else address.substringBefore("://").lowercase() + "://" + address.substringAfter("://")
      FoundLink(start until start + address.length, url)
    }
    .toList()

/** [text] with each web link in it underlined in [styles], calling [onOpen] when tapped. */
fun linkify(text: String, styles: TextLinkStyles, onOpen: (String) -> Unit): AnnotatedString =
  buildAnnotatedString {
    append(text)
    for (link in findLinks(text)) {
      addLink(LinkAnnotation.Url(link.url, styles) { onOpen(link.url) }, link.range.first, link.range.last + 1)
    }
  }

/** [url] as a person would write it: no scheme, no "www.". */
fun displayUrl(url: String): String = url.substringAfter("://").removePrefix("www.")

/**
 * Drops punctuation that belongs to the sentence around a link rather than to the link, such as a
 * closing full stop. A closing bracket stays when the link opened it, as in Wikipedia's
 * "Mercury_(planet)".
 */
private fun String.trimEndPunctuation(): String {
  var end = length
  while (end > 0) {
    val last = this[end - 1]
    val link = substring(0, end)
    val trailing =
      when (last) {
        ')' -> link.count { it == '(' } < link.count { it == ')' }
        ']' -> link.count { it == '[' } < link.count { it == ']' }
        else -> last in ".,;:!?'*"
      }
    if (!trailing) break
    end--
  }
  return substring(0, end)
}
