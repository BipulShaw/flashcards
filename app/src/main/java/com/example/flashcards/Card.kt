package com.example.flashcards

import java.util.UUID

/**
 * One flashcard in the feed. [details] is optional: a card may be just a term and its gist.
 *
 * [id] is stable for the life of a card and is what deletion matches on, so two cards with
 * identical text remain distinguishable.
 */
data class Card(
  val term: String,
  val gist: String,
  val details: String? = null,
  val id: String = UUID.randomUUID().toString(),
)
