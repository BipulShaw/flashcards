package com.example.flashcards

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/** App-wide settings, in a sheet over the feed. A new choice applies at once, while it's open. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
  themeMode: ThemeMode,
  onThemeModeChange: (ThemeMode) -> Unit,
  linkPreviews: Boolean,
  onLinkPreviewsChange: (Boolean) -> Unit,
  onDismiss: () -> Unit,
) {
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
      Spacer(Modifier.height(28.dp))

      // The whole row toggles, not just the switch.
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
          Modifier.fillMaxWidth()
            .toggleable(value = linkPreviews, role = Role.Switch, onValueChange = onLinkPreviewsChange),
      ) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
          Text(text = "Link previews", style = MaterialTheme.typography.bodyLarge)
          Spacer(Modifier.height(2.dp))
          Text(
            text =
              "A picture and summary under each link, fetched from the linked page. " +
                "Off, the app makes no network requests.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Switch(checked = linkPreviews, onCheckedChange = null)
      }
      Spacer(Modifier.height(28.dp))
      // Which build this is, for anyone reporting a problem.
      Text(
        text = "Flashcards ${BuildConfig.VERSION_NAME}",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}
