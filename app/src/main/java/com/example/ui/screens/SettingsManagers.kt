package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.logic.DateUtils
import com.example.models.Shift
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.FloralTextureOverlay
import com.example.ui.components.GradientAccentStrip
import com.example.ui.components.PillTag
import com.example.ui.components.ShiftHighlightBadge
import com.example.ui.components.TintedPillTag
import com.example.ui.components.VibrantFeatureBanner
import com.example.ui.theme.FloralVariant
import com.example.ui.theme.GradientPaletteRegistry
import com.example.ui.theme.LocalStatusPalette

@Composable
fun ShiftManagerPane(
  shifts: List<Shift>,
  onSaveShift: (String?, String, String, String, Boolean, Boolean) -> Unit,
  onToggleActive: (Shift) -> Unit,
  onDeleteShift: (Shift) -> Unit,
  modifier: Modifier = Modifier,
) {
  var editingShift by remember { mutableStateOf<Shift?>(null) }
  var deletingShift by remember { mutableStateOf<Shift?>(null) }
  var showCreateDialog by remember { mutableStateOf(false) }
  val palette = LocalStatusPalette.current

  if (showCreateDialog || editingShift != null) {
    ShiftEditDialog(
      initial = editingShift,
      onSave = { name, start, end, isNight, active ->
        onSaveShift(editingShift?.id, name, start, end, isNight, active)
        showCreateDialog = false
        editingShift = null
      },
      onDismiss = {
        showCreateDialog = false
        editingShift = null
      },
    )
  }

  deletingShift?.let { target ->
    DeleteConfirmDialog(
      title = "Delete '${target.name}'?",
      message =
        "Deletes this shift and clears future roster entries using it, while preserving historical attendance logs.",
      onConfirm = {
        onDeleteShift(target)
        deletingShift = null
      },
      onDismiss = { deletingShift = null },
    )
  }

  val headerPalette = GradientPaletteRegistry.paletteForCard("SHIFTS", 1)
  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      VibrantFeatureBanner(
        title = "Shift Windows",
        subtitle = "${shifts.size} configured · Night shifts highlighted",
        palette = headerPalette,
        floralVariant = FloralVariant.BOTANICAL_VINE,
        icon = Icons.Outlined.Schedule,
        trailingContent = {
          Button(
            onClick = { showCreateDialog = true },
            colors =
              ButtonDefaults.buttonColors(
                containerColor = Color(0xE6FFFFFF),
                contentColor = Color(0xFF0F172A),
              ),
            modifier = Modifier.testTag("add_shift_button"),
          ) {
            Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("New Shift", fontWeight = FontWeight.Bold)
          }
        },
      )
    }

    if (shifts.isEmpty()) {
      item {
        EmptyStateCard(
          title = "No shifts configured",
          message = "Tap New Shift to create working shift windows (including Night Shift highlights).",
          icon = Icons.Outlined.Schedule,
        )
      }
    }

    itemsIndexed(shifts, key = { _, shift -> shift.id }) { idx, shift ->
      val isNight = shift.isNightShiftVisual()
      val shiftPalette = GradientPaletteRegistry.paletteForCard(shift.id, idx)
      Card(
        shape = RoundedCornerShape(16.dp),
        colors =
          CardDefaults.cardColors(
            containerColor = if (isNight) Color.Transparent else MaterialTheme.colorScheme.surface
          ),
        border =
          BorderStroke(
            if (isNight) 1.75.dp else 1.dp,
            if (isNight) palette.nightShift.border else shiftPalette.colors.first().copy(alpha = 0.42f),
          ),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Box(
          modifier =
            Modifier.fillMaxWidth().let { base ->
              if (isNight) {
                base.background(
                  Brush.linearGradient(
                    colors = listOf(palette.nightShift.container, palette.nightShift.containerEnd),
                    start = Offset.Zero,
                    end = Offset(860f, 340f),
                  )
                )
              } else {
                base
              }
            }
        ) {
          if (isNight) {
            FloralTextureOverlay(
              variant = FloralVariant.RADIAL_PETALS,
              motifColor = palette.nightShift.iconTint.copy(alpha = 0.45f),
              modifier = Modifier.matchParentSize(),
            )
          }
          Column(modifier = Modifier.fillMaxWidth()) {
            if (isNight) {
              Box(
                modifier =
                  Modifier.fillMaxWidth()
                    .height(4.dp)
                    .background(
                      Brush.horizontalGradient(
                        colors = listOf(palette.nightShift.border, palette.nightShift.iconTint, palette.nightShift.border)
                      )
                    )
              )
            } else {
              GradientAccentStrip(palette = shiftPalette, height = 4.dp)
            }
            Row(
              modifier = Modifier.fillMaxWidth().padding(14.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ShiftHighlightBadge(
                  shiftName = shift.name,
                  timeWindow = "${shift.startTime}–${shift.endTime}",
                  isNightShift = isNight,
                )
                if (isNight) {
                  PillTag(
                    text = "Night Highlight",
                    containerColor = Color(0x3DFFFFFF),
                    contentColor = palette.nightShift.iconTint,
                    borderColor = palette.nightShift.border.copy(alpha = 0.65f),
                  )
                } else {
                  TintedPillTag(
                    text = if (shift.isActive) "Active Window" else "Inactive",
                    accentColor = shiftPalette.colors.first(),
                  )
                }
              }
              Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                  onClick = { editingShift = shift },
                  modifier = Modifier.minimumInteractiveComponentSize(),
                ) {
                  Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = "Edit or rename ${shift.name}",
                    tint =
                      if (isNight) palette.nightShift.iconTint
                      else MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                IconButton(
                  onClick = { deletingShift = shift },
                  modifier =
                    Modifier.minimumInteractiveComponentSize().testTag("delete_shift_${shift.id}"),
                ) {
                  Icon(
                    Icons.Outlined.DeleteOutline,
                    contentDescription = "Delete ${shift.name}",
                    tint =
                      if (isNight) Color(0xFFFCA5A5) else MaterialTheme.colorScheme.error,
                  )
                }
                Switch(checked = shift.isActive, onCheckedChange = { onToggleActive(shift) })
              }
            }
          }
        }
      }
    }
    item { Spacer(modifier = Modifier.height(16.dp)) }
  }
}

@Composable
private fun ShiftEditDialog(
  initial: Shift?,
  onSave: (String, String, String, Boolean, Boolean) -> Unit,
  onDismiss: () -> Unit,
) {
  var name by remember(initial) { mutableStateOf(initial?.name ?: "") }
  var startTime by remember(initial) { mutableStateOf(initial?.startTime ?: "08:00") }
  var endTime by remember(initial) { mutableStateOf(initial?.endTime ?: "16:00") }
  var isNightShift by remember(initial) { mutableStateOf(initial?.isNightShift ?: false) }
  var isActive by remember(initial) { mutableStateOf(initial?.isActive ?: true) }

  val validTime = DateUtils.isValidTime24h(startTime) && DateUtils.isValidTime24h(endTime)
  val isValid = name.isNotBlank() && validTime

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(if (initial == null) "Create Shift" else "Edit / Rename Shift") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
          value = name,
          onValueChange = { next ->
            name = next
            if (next.contains("night", ignoreCase = true)) {
              isNightShift = true
            }
          },
          label = { Text("Shift Name") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("shift_name_input"),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = startTime,
            onValueChange = { startTime = it },
            label = { Text("Start (HH:mm)") },
            singleLine = true,
            isError = !DateUtils.isValidTime24h(startTime),
            modifier = Modifier.weight(1f),
          )
          OutlinedTextField(
            value = endTime,
            onValueChange = { endTime = it },
            label = { Text("End (HH:mm)") },
            singleLine = true,
            isError = !DateUtils.isValidTime24h(endTime),
            modifier = Modifier.weight(1f),
          )
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text("Night Shift Visual Highlight", style = MaterialTheme.typography.bodyMedium)
            Text(
              text = "Applies distinct night styling across rosters & history",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Switch(checked = isNightShift, onCheckedChange = { isNightShift = it })
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text("Active", style = MaterialTheme.typography.bodyMedium)
          Switch(checked = isActive, onCheckedChange = { isActive = it })
        }
      }
    },
    confirmButton = {
      Button(
        onClick = { onSave(name, startTime, endTime, isNightShift, isActive) },
        enabled = isValid,
      ) {
        Text("Save")
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
  )
}

@Composable
internal fun DeleteConfirmDialog(
  title: String,
  message: String,
  onConfirm: () -> Unit,
  onDismiss: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(title) },
    text = { Text(message, style = MaterialTheme.typography.bodyMedium) },
    confirmButton = {
      Button(
        onClick = onConfirm,
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
        modifier = Modifier.testTag("confirm_delete_button"),
      ) {
        Text("Delete")
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
  )
}
