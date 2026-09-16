package com.example.flashcards

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.flashcards.theme.FlashcardsTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Cards are always pale, so keep dark system-bar icons even when the device is in dark mode.
    enableEdgeToEdge(
      statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
      navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
    )

    val store = CardStore(applicationContext)

    setContent {
      // Pinned light: the feed paints its own pale backgrounds and the system bar icons are dark,
      // so the add screen's text fields have to be light-themed too.
      FlashcardsTheme(darkTheme = false, dynamicColor = false) {
        // Shuffled once per launch. A new card is appended, so it lands at the end of the deck.
        var cards by remember { mutableStateOf(store.load().shuffled()) }
        var currentIndex by remember { mutableIntStateOf(0) }
        var adding by remember { mutableStateOf(false) }

        if (adding) {
          AddCardScreen(
            onSave = { card ->
              val updated = cards + card
              cards = updated
              store.save(updated)
              currentIndex = updated.lastIndex
              adding = false
            },
            onCancel = { adding = false },
          )
        } else {
          FeedScreen(
            cards = cards,
            initialIndex = currentIndex,
            onCurrentIndexChange = { currentIndex = it },
            onAddCard = { adding = true },
          )
        }
      }
    }
  }
}
