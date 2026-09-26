package com.example.flashcards

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** App-wide settings, in a sheet over the feed. A new choice applies at once, while it's open. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(themeMode: ThemeMode, onThemeModeChange: (ThemeMode) -> Unit, onDismiss: () -> Unit) {
  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
  ) {
    Column(modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 32.dp)) {
      Text(text = "Settings", style = MaterialTheme.typography.headlineSmall)
      Spacer(Modifier.height(24.dp))

      Text(
        text = "Theme",
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(Modifier.height(12.dp))
      SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        ThemeMode.entries.forEachIndexed { index, mode ->
          SegmentedButton(
            selected = mode == themeMode,
            onClick = { onThemeModeChange(mode) },
            shape = SegmentedButtonDefaults.itemShape(index = index, count = ThemeMode.entries.size),
            label = { Text(mode.label) },
          )
        }
      }
      Spacer(Modifier.height(8.dp))
      Text(
        text = "System follows your phone’s light or dark setting.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}
