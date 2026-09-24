package com.example.flashcards

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.example.flashcards.ui.theme.FlashcardsTheme
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

private enum class Screen {
  Feed,
  Add,
  Search,
}

// Shared-bounds keys: the element on each side of a transition that grows into the other.
private const val EditorKey = "editor"
private const val SearchKey = "search"

/** Material's emphasized easing, for motion that begins and ends on screen. */
private val Emphasized = CubicBezierEasing(0.2f, 0f, 0f, 1f)
private const val MorphMillis = 400

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Transparent system bars whose icons follow the system theme, as the app itself does.
    enableEdgeToEdge()

    val store = CardStore(applicationContext)

    setContent {
      FlashcardsTheme {
        // Shuffled once per launch. A new card is appended, so it lands at the end of the deck.
        var cards by remember { mutableStateOf(store.load().shuffled()) }
        var currentIndex by remember { mutableIntStateOf(0) }
        var screen by remember { mutableStateOf(Screen.Feed) }
        // The editor's starting term: empty from the + button, the query from search's Create.
        var draftTerm by remember { mutableStateOf("") }

        val snackbars = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()

        val deleteCard: (Card) -> Unit = { card ->
          // Matched by id, never by content, so identical cards stay distinct.
          val removedAt = cards.indexOfFirst { it.id == card.id }
          if (removedAt >= 0) {
            val updated = cards.toMutableList().apply { removeAt(removedAt) }
            cards = updated
            store.save(updated)
            // The freed slot now holds the following card; past the end, wrap to the first.
            currentIndex = if (updated.isEmpty()) 0 else removedAt % updated.size

            scope.launch {
              // Indefinite plus a timeout gives a roughly five second window to undo.
              val outcome =
                withTimeoutOrNull(5_000L) {
                  snackbars.showSnackbar(
                    message = "Card deleted",
                    actionLabel = "Undo",
                    duration = SnackbarDuration.Indefinite,
                  )
                }
              if (outcome == SnackbarResult.ActionPerformed) {
                val restored =
                  cards.toMutableList().apply { add(removedAt.coerceAtMost(size), card) }
                cards = restored
                store.save(restored)
                currentIndex = removedAt
              }
            }
          }
        }

        val cardColors = FlashcardsTheme.cardColors

        Box(modifier = Modifier.fillMaxSize()) {
          SharedTransitionLayout {
            AnimatedContent(
              targetState = screen,
              transitionSpec = {
                fadeIn(tween(MorphMillis / 2, delayMillis = MorphMillis / 4)) togetherWith
                  fadeOut(tween(MorphMillis / 2))
              },
              label = "screen",
            ) { target ->
              val visibility = this
              val editor = rememberSharedContentState(EditorKey)
              val search = rememberSharedContentState(SearchKey)
              // A screen growing out of a button keeps rounded corners until it fills the display.
              val corner by
                transition.animateDp(transitionSpec = { tween(MorphMillis, easing = Emphasized) }, label = "corner") {
                  if (it == EnterExitState.Visible) 0.dp else 28.dp
                }

              // By default the content is laid out afresh at each in-between size, so the container
              // itself grows around it.
              fun Modifier.morph(
                state: SharedTransitionScope.SharedContentState,
                clipCorners: Boolean = false,
                resize: SharedTransitionScope.ResizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
              ): Modifier {
                val enter = fadeIn(tween(MorphMillis / 2, delayMillis = MorphMillis / 4))
                val exit = fadeOut(tween(MorphMillis / 3))
                val bounds = BoundsTransform { _, _ -> tween(MorphMillis, easing = Emphasized) }
                return if (clipCorners) {
                  sharedBounds(
                    sharedContentState = state,
                    animatedVisibilityScope = visibility,
                    enter = enter,
                    exit = exit,
                    boundsTransform = bounds,
                    resizeMode = resize,
                    clipInOverlayDuringTransition = OverlayClip(RoundedCornerShape(corner)),
                  )
                } else {
                  sharedBounds(state, visibility, enter, exit, bounds, resize)
                }
              }

              when (target) {
                Screen.Add ->
                  AddCardScreen(
                    // The tint this card will have in the feed, where it is appended at the end.
                    tint = cardColors.tintAt(cards.size),
                    initialTerm = draftTerm,
                    onSave = { card ->
                      val updated = cards + card
                      cards = updated
                      store.save(updated)
                      currentIndex = updated.lastIndex
                      screen = Screen.Feed
                    },
                    onClose = { screen = Screen.Feed },
                    settled = transition.currentState == EnterExitState.Visible,
                    modifier = Modifier.morph(editor, clipCorners = true),
                  )
                Screen.Search ->
                  SearchScreen(
                    cards = cards,
                    onSelect = { card ->
                      val at = cards.indexOfFirst { it.id == card.id }
                      if (at >= 0) currentIndex = at
                      screen = Screen.Feed
                    },
                    onCreate = { term ->
                      draftTerm = term
                      screen = Screen.Add
                    },
                    onBack = { screen = Screen.Feed },
                    // Laid out once at full size and scaled instead: the Material search bar positions
                    // its input from its own layout, and re-measured at every in-between size it drifts
                    // down the screen, then jumps back to the top when the transition ends.
                    modifier =
                      Modifier.morph(
                        search,
                        clipCorners = true,
                        resize =
                          SharedTransitionScope.ResizeMode.scaleToBounds(ContentScale.FillWidth, Alignment.TopCenter),
                      ),
                    createButtonModifier = Modifier.morph(editor),
                  )
                // Keyed on the deck size: adding, deleting or undoing rebuilds the pager, which
                // lands it on currentIndex at once rather than animating across every page between.
                Screen.Feed ->
                  key(cards.size) {
                    FeedScreen(
                      cards = cards,
                      initialIndex = currentIndex,
                      onCurrentIndexChange = { currentIndex = it },
                      onAddCard = {
                        draftTerm = ""
                        screen = Screen.Add
                      },
                      onOpenSearch = { screen = Screen.Search },
                      onDelete = deleteCard,
                      searchBarModifier = Modifier.morph(search),
                      fabModifier = Modifier.morph(editor),
                    )
                  }
              }
            }
          }
          SnackbarHost(
            hostState = snackbars,
            modifier = Modifier.align(Alignment.BottomCenter).safeDrawingPadding(),
          )
        }
      }
    }
  }
}
