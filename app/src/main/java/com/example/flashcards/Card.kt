package com.example.flashcards

/** One flashcard in the feed. [details] is optional: a card may be just a term and its gist. */
data class Card(val term: String, val gist: String, val details: String? = null)
