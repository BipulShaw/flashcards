package com.example.flashcards

import android.content.Context
import java.io.File
import org.json.JSONArray
import org.json.JSONObject

/** Reads and writes the deck as JSON in filesDir/cards.json. */
class CardStore(context: Context) {
  private val file = File(context.filesDir, "cards.json")
  private val temp = File(context.filesDir, "cards.json.tmp")

  /** The stored deck, seeding the file with [sampleCards] the first time the app runs. */
  fun load(): List<Card> {
    if (!file.exists()) {
      save(sampleCards)
      return sampleCards
    }
    // A deck that cannot be read falls back to the samples rather than crashing on every launch.
    // The unreadable file is left alone, so nothing is destroyed before the next save.
    val stored = runCatching { parse(file.readText()) }.getOrNull()
    return if (stored.isNullOrEmpty()) sampleCards else stored
  }

  fun save(cards: List<Card>) {
    val array = JSONArray()
    for (card in cards) {
      val json = JSONObject()
      json.put("term", card.term)
      json.put("gist", card.gist)
      card.details?.let { json.put("details", it) }
      array.put(json)
    }
    // Write the whole file under a temporary name, then swap it in: a crash midway through
    // leaves the previous cards.json untouched instead of half a deck.
    temp.writeText(array.toString())
    if (!temp.renameTo(file)) temp.delete()
  }

  private fun parse(text: String): List<Card> {
    val array = JSONArray(text)
    return (0 until array.length()).map { i ->
      val json = array.getJSONObject(i)
      Card(
        term = json.getString("term"),
        gist = json.getString("gist"),
        // optString returns "" for both a missing key and an explicit null.
        details = json.optString("details").ifEmpty { null },
      )
    }
  }
}
