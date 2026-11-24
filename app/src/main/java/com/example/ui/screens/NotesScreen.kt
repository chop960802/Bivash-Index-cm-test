package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.logic.DateUtils
import com.example.models.NoteEntry
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.FloralTextureOverlay
import com.example.ui.components.ReferenceDetailCard
import com.example.ui.components.TintedPillTag
import com.example.ui.components.VibrantFeatureBanner
import com.example.ui.components.rememberSyncedText
import com.example.ui.state.NotesUiState
import com.example.ui.theme.FloralVariant
import com.example.ui.theme.GradientPalette
import com.example.ui.theme.GradientPaletteRegistry

@Composable
fun NotesScreen(
  state: NotesUiState,
  onBack: () -> Unit,
  onSearchTextChange: (String) -> Unit,
  onSaveNote: (String?, String, String) -> Unit,
  onDeleteNote: (NoteEntry) -> Unit,
  modifier: Modifier = Modifier,
) {
  var showCreateDialog by remember { mutableStateOf(false) }
  var editingNote by remember { mutableStateOf<NoteEntry?>(null) }
  var deletingNote by remember { mutableStateOf<NoteEntry?>(null) }

  val bannerPalette = GradientPaletteRegistry.paletteForCard("notes", 4)

  if (showCreateDialog || editingNote != null) {
    NoteEditDialog(
      initial = editingNote,
      palette =
        editingNote?.let { note ->
          val idx = state.notes.indexOfFirst { it.id == note.id }.coerceAtLeast(0)
          GradientPaletteRegistry.paletteForCard(note.id, idx)
        } ?: bannerPalette,
      onSave = { title, body ->
        onSaveNote(editingNote?.id, title, body)
        showCreateDialog = false
        editingNote = null
      },
      onDismiss = {
        showCreateDialog = false
        editingNote = null
      },
    )
  }

  deletingNote?.let { target ->
    DeleteConfirmDialog(
      title = "Delete '${target.title}'?",
      message = "Remove this note permanently from your workspace?",
      onConfirm = {
        onDeleteNote(target)
        deletingNote = null
      },
      onDismiss = { deletingNote = null },
    )
  }

  Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    Column(modifier = Modifier.fillMaxSize().widthIn(max = 860.dp)) {
      Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        VibrantFeatureBanner(
          title = "Notes",
          subtitle = "${state.totalNotesCount} saved note(s)",
          palette = bannerPalette,
          floralVariant = FloralVariant.CORNER_CLUSTER,
          icon = Icons.Outlined.EditNote,
          leadingContent = {
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = Color(0x470B1020),
              border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
            ) {
              IconButton(
                onClick = onBack,
                modifier = Modifier.minimumInteractiveComponentSize().testTag("notes_back_button"),
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                  contentDescription = "Back to Dashboard",
                  tint = Color.White,
                )
              }
            }
          },
          trailingContent = {
            Button(
              onClick = { showCreateDialog = true },
              colors =
                ButtonDefaults.buttonColors(
                  containerColor = Color(0xE6FFFFFF),
                  contentColor = Color(0xFF0F172A),
                ),
              modifier = Modifier.height(44.dp).testTag("add_note_button"),
            ) {
              Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("New", fontWeight = FontWeight.Bold)
            }
          },
        )
      }

      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        item {
          val (syncedSearchText, onSearchEdit) =
            rememberSyncedText(
              externalValue = state.searchText,
              onValueChange = onSearchTextChange,
            )
          OutlinedTextField(
            value = syncedSearchText,
            onValueChange = onSearchEdit,
            label = { Text("Search notes by title or keyword") },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            trailingIcon =
              if (syncedSearchText.isNotBlank()) {
                {
                  IconButton(onClick = { onSearchEdit("") }) {
                    Icon(Icons.Outlined.Clear, contentDescription = "Clear search")
                  }
                }
              } else null,
            singleLine = true,
            colors =
              OutlinedTextFieldDefaults.colors(
                focusedBorderColor = bannerPalette.colors.first(),
                focusedLabelColor = bannerPalette.colors.first(),
              ),
            modifier = Modifier.fillMaxWidth().testTag("notes_search_input"),
          )
        }

        if (state.notes.isEmpty()) {
          item {
            EmptyStateCard(
              title = if (state.totalNotesCount == 0) "No notes yet" else "No matching notes",
              message =
                if (state.totalNotesCount == 0) "Tap New above to save a note."
                else "Try a different keyword or clear the search filter.",
              icon = Icons.Outlined.EditNote,
            )
          }
        } else {
          itemsIndexed(state.notes, key = { _, note -> note.id }) { idx, note ->
            val notePalette = GradientPaletteRegistry.paletteForCard(note.id, idx)
            val accent = notePalette.colors.first()
            ReferenceDetailCard(
              accentColor = accent,
              modifier =
                Modifier.clip(RoundedCornerShape(14.dp))
                  .clickable { editingNote = note }
                  .testTag("note_card_${note.id}"),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                  Text(
                    text = note.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                  )
                  Spacer(modifier = Modifier.height(4.dp))
                  TintedPillTag(
                    text = DateUtils.formatTimestamp(note.updatedAt),
                    accentColor = accent,
                  )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                  IconButton(
                    onClick = { editingNote = note },
                    modifier = Modifier.minimumInteractiveComponentSize(),
                  ) {
                    Icon(Icons.Outlined.Edit, contentDescription = "Edit ${note.title}")
                  }
                  IconButton(
                    onClick = { deletingNote = note },
                    modifier =
                      Modifier.minimumInteractiveComponentSize().testTag("delete_note_${note.id}"),
                  ) {
                    Icon(
                      imageVector = Icons.Outlined.DeleteOutline,
                      contentDescription = "Delete ${note.title}",
                      tint = MaterialTheme.colorScheme.error,
                    )
                  }
                }
              }

              if (note.body.isNotBlank()) {
                Text(
                  text = note.body.trim(),
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 2,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }
          }
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
      }
    }
  }
}

@Composable
private fun NoteEditDialog(
  initial: NoteEntry?,
  palette: GradientPalette,
  onSave: (String, String) -> Unit,
  onDismiss: () -> Unit,
) {
  var title by remember(initial) { mutableStateOf(initial?.title ?: "") }
  var body by remember(initial) { mutableStateOf(initial?.body ?: "") }
  val isValid = title.isNotBlank() || body.isNotBlank()
  val accent = palette.colors.first()
  val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(24.dp),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 6.dp,
      border = BorderStroke(1.25.dp, accent.copy(alpha = if (isDark) 0.70f else 0.55f)),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(modifier = Modifier.fillMaxWidth()) {
        Box(
          modifier =
            Modifier.fillMaxWidth()
              .background(
                Brush.linearGradient(
                  colors = palette.colors,
                  start = Offset.Zero,
                  end = Offset(820f, 340f),
                )
              )
        ) {
          FloralTextureOverlay(
            variant = FloralVariant.CORNER_CLUSTER,
            motifColor = Color.White,
            modifier = Modifier.matchParentSize(),
          )
          Box(
            modifier =
              Modifier.matchParentSize()
                .background(
                  Brush.horizontalGradient(
                    colors = listOf(Color(0xB30B1020), Color(0x6E0B1020))
                  )
                )
          )
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = Color(0x470B1020),
              border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
              modifier = Modifier.size(40.dp),
            ) {
              Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                  imageVector = Icons.Outlined.EditNote,
                  contentDescription = null,
                  tint = Color.White,
                  modifier = Modifier.size(22.dp),
                )
              }
            }
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = if (initial == null) "Create Note" else "Edit Note",
                style =
                  MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    shadow =
                      Shadow(
                        color = Color(0x66000000),
                        offset = Offset(0f, 1.5f),
                        blurRadius = 3f,
                      ),
                  ),
                color = Color.White,
              )
              Text(
                text = if (initial == null) "Save an offline field memo" else "View or update full note",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.90f),
              )
            }
          }
        }

        Column(
          modifier =
            Modifier.fillMaxWidth()
              .verticalScroll(rememberScrollState())
              .padding(18.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Title") },
            placeholder = { Text("Shift handover reminder") },
            singleLine = true,
            colors =
              OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accent,
                focusedLabelColor = accent,
              ),
            modifier = Modifier.fillMaxWidth().testTag("note_title_input"),
          )
          OutlinedTextField(
            value = body,
            onValueChange = { body = it },
            label = { Text("Note Body") },
            minLines = 4,
            maxLines = 8,
            colors =
              OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accent,
                focusedLabelColor = accent,
              ),
            modifier = Modifier.fillMaxWidth().testTag("note_body_input"),
          )
          Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            TextButton(onClick = onDismiss) { Text("Cancel") }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
              onClick = { onSave(title, body) },
              enabled = isValid,
              colors =
                ButtonDefaults.buttonColors(
                  containerColor = accent,
                  contentColor = if (accent.luminance() > 0.55f) Color(0xFF0F172A) else Color.White,
                ),
              modifier = Modifier.minimumInteractiveComponentSize().testTag("save_note_button"),
            ) {
              Text("Save", fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }
  }
}
