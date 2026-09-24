package com.example.flashcards

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.example.flashcards.ui.theme.FlashcardsTheme

private const val GIST_LIMIT = 140

/**
 * Writing a card on the card itself: borderless fields sit directly on a surface in [tint], the
 * colour the new card will have in the feed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCardScreen(
  tint: Color,
  onSave: (Card) -> Unit,
  onClose: () -> Unit,
  modifier: Modifier = Modifier,
  initialTerm: String = "",
) {
  var term by remember { mutableStateOf(initialTerm) }
  var gist by remember { mutableStateOf("") }
  var details by remember { mutableStateOf("") }
  val canSave = term.isNotBlank() && gist.isNotBlank()

  // System back closes without saving.
  BackHandler(onBack = onClose)

  val termFocus = remember { FocusRequester() }
  val gistFocus = remember { FocusRequester() }
  val detailsFocus = remember { FocusRequester() }
  val keyboard = LocalSoftwareKeyboardController.current
  LaunchedEffect(Unit) {
    // Arriving with the term already written (from search's Create), carry on at the gist.
    (if (initialTerm.isBlank()) termFocus else gistFocus).requestFocus()
    keyboard?.show()
  }

  val cardColors = FlashcardsTheme.cardColors
  val type = MaterialTheme.typography

  Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
    Column {
      TopAppBar(
        title = { Text("New card") },
        navigationIcon = {
          IconButton(onClick = onClose) {
            Icon(painterResource(R.drawable.ic_close), contentDescription = "Close")
          }
        },
        actions = {
          Button(
            onClick = {
              onSave(Card(term = term.trim(), gist = gist.trim(), details = details.trim().ifEmpty { null }))
            },
            enabled = canSave,
            modifier = Modifier.padding(end = 12.dp),
          ) {
            Text("Save")
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
      )

      Column(
        modifier =
          Modifier.fillMaxSize()
            .navigationBarsPadding()
            // Shrinks the scrolling area to the space above the keyboard, and the focused field
            // scrolls itself into that space.
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp)
      ) {
        Surface(shape = MaterialTheme.shapes.large, color = tint, modifier = Modifier.fillMaxWidth()) {
          Column(modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 20.dp)) {
            CardField(
              value = term,
              onValueChange = { term = it },
              placeholder = "What\u2019s the word?",
              style = type.headlineLarge,
              color = cardColors.ink,
              capitalization = KeyboardCapitalization.Words,
              onNext = { gistFocus.requestFocus() },
              modifier = Modifier.focusRequester(termFocus),
            )
            HorizontalDivider(color = cardColors.rule, modifier = Modifier.padding(vertical = 16.dp))

            CardField(
              value = gist,
              onValueChange = { gist = it },
              placeholder = "Explain it in one line, in your own words",
              style = type.titleMedium,
              color = cardColors.inkMuted,
              onNext = { detailsFocus.requestFocus() },
              modifier = Modifier.focusRequester(gistFocus),
            )
            Text(
              text = "${gist.length}/$GIST_LIMIT",
              style = type.labelSmall,
              color = if (gist.length > GIST_LIMIT) MaterialTheme.colorScheme.error else cardColors.inkFaint,
              modifier = Modifier.align(Alignment.End).padding(top = 6.dp),
            )
            Spacer(Modifier.height(16.dp))

            CardField(
              value = details,
              onValueChange = { details = it },
              placeholder = "Examples, context, where you heard it…",
              style = type.bodyLarge,
              // Stronger than the inkFaint the feed uses, so what's written never reads as a hint.
              color = cardColors.inkMuted,
              minLines = 6,
              ruled = true,
              modifier = Modifier.focusRequester(detailsFocus),
            )
          }
        }
      }
    }
  }
}

/**
 * A borderless field drawn straight onto the card.
 *
 * With [onNext], the field is one paragraph that may wrap: Enter moves on instead of breaking the
 * line, and pasted line breaks become spaces. [ruled] draws faint writing lines under the text.
 */
@Composable
private fun CardField(
  value: String,
  onValueChange: (String) -> Unit,
  placeholder: String,
  style: TextStyle,
  color: Color,
  modifier: Modifier = Modifier,
  capitalization: KeyboardCapitalization = KeyboardCapitalization.Sentences,
  onNext: (() -> Unit)? = null,
  minLines: Int = 1,
  ruled: Boolean = false,
) {
  val placeholderColor = FlashcardsTheme.cardColors.inkFaint
  val ruleColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
  var layout by remember { mutableStateOf<TextLayoutResult?>(null) }

  BasicTextField(
    value = value,
    onValueChange = { new ->
      when {
        onNext == null -> onValueChange(new)
        // Only a line break was added: that was Enter.
        '\n' in new && new.replace("\n", "") == value -> onNext()
        else -> onValueChange(new.replace('\n', ' '))
      }
    },
    textStyle = style.copy(color = color),
    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
    keyboardOptions =
      KeyboardOptions(
        capitalization = capitalization,
        imeAction = if (onNext != null) ImeAction.Next else ImeAction.Default,
      ),
    keyboardActions = KeyboardActions(onNext = { onNext?.invoke() }),
    minLines = minLines,
    onTextLayout = { layout = it },
    modifier =
      modifier.fillMaxWidth().then(
        if (!ruled) Modifier
        else
          Modifier.drawBehind {
            val text = layout ?: return@drawBehind
            // One line per line of text, a few pixels under each baseline, down to the bottom.
            val step = style.lineHeight.toPx()
            var y = text.getLineBaseline(0) + 6.dp.toPx()
            while (y < size.height) {
              drawLine(ruleColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
              y += step
            }
          }
      ),
    decorationBox = { field ->
      Box {
        if (value.isEmpty()) Text(text = placeholder, style = style, color = placeholderColor)
        field()
      }
    },
  )
}
