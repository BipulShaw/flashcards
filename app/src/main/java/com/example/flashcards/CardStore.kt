package com.example.flashcards

import android.content.Context
import java.io.File
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

/** Reads and writes the deck as JSON in filesDir/cards.json. */
class CardStore(context: Context) {
  private val file = File(context.filesDir, "cards.json")
  private val temp = File(context.filesDir, "cards.json.tmp")

  private class Parsed(val cards: List<Card>, val missingIds: Boolean)

  /** The stored deck, seeding the file with [sampleCards] the first time the app runs. */
  fun load(): List<Card> {
    if (!file.exists()) {
      save(sampleCards)
      return sampleCards
    }
    // A deck that cannot be read falls back to the samples rather than crashing on every launch.
    // The unreadable file is left alone, so nothing is destroyed before the next save.
    val parsed = runCatching { parse(file.readText()) }.getOrNull() ?: return sampleCards
    // Cards written before ids existed get one now, saved straight back so it stays stable.
    if (parsed.missingIds) save(parsed.cards)
    // An empty list here means every card was deleted, which is a real state worth keeping:
    // only a missing or unreadable file falls back to the samples.
    return parsed.cards
  }

  fun save(cards: List<Card>) {
    val array = JSONArray()
    for (card in cards) {
      val json = JSONObject()
      json.put("id", card.id)
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

  private fun parse(text: String): Parsed {
    val array = JSONArray(text)
    var missingIds = false
    val cards =
      (0 until array.length()).map { i ->
        val json = array.getJSONObject(i)
        // optString returns "" for both a missing key and an explicit null.
        val id = json.optString("id")
        if (id.isEmpty()) missingIds = true
        Card(
          term = json.getString("term"),
          gist = json.getString("gist"),
          details = json.optString("details").ifEmpty { null },
          id = id.ifEmpty { UUID.randomUUID().toString() },
        )
      }
    return Parsed(cards, missingIds)
  }
}
