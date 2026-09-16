package com.example.flashcards

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp

@Composable
fun AddCardScreen(onSave: (Card) -> Unit, onCancel: () -> Unit, modifier: Modifier = Modifier) {
  var term by remember { mutableStateOf("") }
  var gist by remember { mutableStateOf("") }
  var details by remember { mutableStateOf("") }
  val canSave = term.isNotBlank() && gist.isNotBlank()

  // System back leaves without saving.
  BackHandler(onBack = onCancel)

  val termFocus = remember { FocusRequester() }
  val keyboard = LocalSoftwareKeyboardController.current
  LaunchedEffect(Unit) {
    termFocus.requestFocus()
    keyboard?.show()
  }

  Column(
    modifier =
      modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .safeDrawingPadding()
        // Keeps the fields above the keyboard; scrolling covers the rest on short screens.
        .imePadding()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 24.dp, vertical = 24.dp)
  ) {
    Text(text = "New card", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(24.dp))

    OutlinedTextField(
      value = term,
      onValueChange = { term = it },
      label = { Text("Term") },
      singleLine = true,
      keyboardOptions =
        KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
      modifier = Modifier.fillMaxWidth().focusRequester(termFocus),
    )
    Spacer(Modifier.height(16.dp))

    OutlinedTextField(
      value = gist,
      onValueChange = { gist = it },
      label = { Text("Gist") },
      singleLine = true,
      keyboardOptions =
        KeyboardOptions(
          capitalization = KeyboardCapitalization.Sentences,
          imeAction = ImeAction.Next,
        ),
      modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(16.dp))

    OutlinedTextField(
      value = details,
      onValueChange = { details = it },
      label = { Text("Details (optional)") },
      minLines = 4,
      keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
      modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(24.dp))

    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
      TextButton(onClick = onCancel) { Text("Cancel") }
      Spacer(Modifier.height(0.dp))
      Button(
        onClick = {
          onSave(
            Card(
              term = term.trim(),
              gist = gist.trim(),
              details = details.trim().ifEmpty { null },
            )
          )
        },
        enabled = canSave,
        modifier = Modifier.padding(start = 8.dp),
      ) {
        Text("Save")
      }
    }
  }
}
