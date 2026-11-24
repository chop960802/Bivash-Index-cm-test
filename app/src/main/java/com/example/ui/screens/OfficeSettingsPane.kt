package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.logic.DateUtils
import com.example.models.RegisteredOffice
import com.example.ui.components.DetailMetaPair
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.FloralTextureOverlay
import com.example.ui.components.GradientAccentStrip
import com.example.ui.components.PillTag
import com.example.ui.components.SolidPillTag
import com.example.ui.components.VibrantFeatureBanner
import com.example.ui.theme.FloralVariant
import com.example.ui.theme.GradientPaletteRegistry

@Composable
fun OfficeSettingsPane(
  offices: List<RegisteredOffice>,
  onSaveOffice: (String?, String, String, Boolean) -> Unit,
  onToggleActive: (RegisteredOffice) -> Unit,
  onDeleteOffice: (RegisteredOffice) -> Unit,
  modifier: Modifier = Modifier,
) {
  var showCreateDialog by remember { mutableStateOf(false) }
  var editingOffice by remember { mutableStateOf<RegisteredOffice?>(null) }
  var deletingOffice by remember { mutableStateOf<RegisteredOffice?>(null) }

  if (showCreateDialog || editingOffice != null) {
    OfficeEditDialog(
      initial = editingOffice,
      onSave = { name, shortCode, isActive ->
        onSaveOffice(editingOffice?.id, name, shortCode, isActive)
        showCreateDialog = false
        editingOffice = null
      },
      onDismiss = {
        showCreateDialog = false
        editingOffice = null
      },
    )
  }

  deletingOffice?.let { target ->
    AlertDialog(
      onDismissRequest = { deletingOffice = null },
      title = { Text("Delete '${target.officeName}'?") },
      text = {
        Text(
          text =
            "Removes '${target.officeName}' (${target.shortCode}) from Settings. Existing Office Index FIR records retain their office name & short code snapshot.",
          style = MaterialTheme.typography.bodyMedium,
        )
      },
      confirmButton = {
        Button(
          onClick = {
            onDeleteOffice(target)
            deletingOffice = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
          modifier = Modifier.testTag("confirm_delete_office_button"),
        ) {
          Text("Delete")
        }
      },
      dismissButton = { TextButton(onClick = { deletingOffice = null }) { Text("Cancel") } },
    )
  }

  val officePalette = GradientPaletteRegistry.paletteForCard("OFFICE_MANAGEMENT", 5)

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      VibrantFeatureBanner(
        title = "Office Management",
        subtitle =
          "${offices.size} registered · ${offices.count { it.isActive }} active for Office Index",
        palette = officePalette,
        floralVariant = FloralVariant.CORNER_CLUSTER,
        icon = Icons.Outlined.AccountBalance,
        trailingContent = {
          Button(
            onClick = { showCreateDialog = true },
            colors =
              ButtonDefaults.buttonColors(
                containerColor = Color(0xE6FFFFFF),
                contentColor = Color(0xFF0F172A),
              ),
            modifier = Modifier.testTag("add_office_button"),
          ) {
            Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Add Office", fontWeight = FontWeight.Bold)
          }
        },
      )
    }

    if (offices.isEmpty()) {
      item {
        EmptyStateCard(
          title = "No offices registered yet",
          message =
            "Tap + Add Office to register an Office Name and Short Code (e.g., HQ, PS-01) for Office Index.",
          icon = Icons.Outlined.AccountBalance,
        )
      }
    } else {
      itemsIndexed(offices, key = { _, office -> office.id }) { idx, office ->
        val cardPalette = GradientPaletteRegistry.paletteForCard(office.id, idx)
        val accent = cardPalette.colors.first()
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, accent.copy(alpha = 0.45f)),
          modifier = Modifier.fillMaxWidth().testTag("office_card_${office.id}"),
        ) {
          Box(modifier = Modifier.fillMaxWidth()) {
            FloralTextureOverlay(
              variant = GradientPaletteRegistry.floralVariantForCard(office.id, idx),
              motifColor = accent.copy(alpha = 0.16f),
              modifier = Modifier.matchParentSize(),
            )
            Column(modifier = Modifier.fillMaxWidth()) {
              GradientAccentStrip(palette = cardPalette, height = 4.dp)
              Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Column(
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                  ) {
                    Row(
                      horizontalArrangement = Arrangement.spacedBy(8.dp),
                      verticalAlignment = Alignment.CenterVertically,
                    ) {
                      SolidPillTag(text = office.shortCode, containerColor = accent)
                      Text(
                        text = office.officeName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                      )
                    }
                    Row(
                      horizontalArrangement = Arrangement.spacedBy(10.dp),
                      verticalAlignment = Alignment.CenterVertically,
                    ) {
                      DetailMetaPair(
                        label = "Updated",
                        value = DateUtils.formatTimestamp(office.updatedAt),
                      )
                      if (!office.isActive) {
                        PillTag(
                          text = "Inactive",
                          containerColor = MaterialTheme.colorScheme.surfaceVariant,
                          contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                      }
                    }
                  }
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                      onClick = { editingOffice = office },
                      modifier =
                        Modifier.minimumInteractiveComponentSize()
                          .testTag("edit_office_${office.id}"),
                    ) {
                      Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = "Edit or rename ${office.officeName}",
                      )
                    }
                    IconButton(
                      onClick = { deletingOffice = office },
                      modifier =
                        Modifier.minimumInteractiveComponentSize()
                          .testTag("delete_office_${office.id}"),
                    ) {
                      Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = "Delete ${office.officeName}",
                        tint = MaterialTheme.colorScheme.error,
                      )
                    }
                    Switch(
                      checked = office.isActive,
                      onCheckedChange = { onToggleActive(office) },
                    )
                  }
                }
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
private fun OfficeEditDialog(
  initial: RegisteredOffice?,
  onSave: (String, String, Boolean) -> Unit,
  onDismiss: () -> Unit,
) {
  var officeName by remember(initial) { mutableStateOf(initial?.officeName ?: "") }
  var shortCode by remember(initial) { mutableStateOf(initial?.shortCode ?: "") }
  var isActive by remember(initial) { mutableStateOf(initial?.isActive ?: true) }
  val isValid = officeName.isNotBlank() && shortCode.isNotBlank()

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(if (initial == null) "Register Office" else "Edit / Rename Office") },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        OutlinedTextField(
          value = officeName,
          onValueChange = { officeName = it },
          label = { Text("Office Name") },
          placeholder = { Text("e.g. Central Police Station, District HQ") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("office_name_input"),
        )
        OutlinedTextField(
          value = shortCode,
          onValueChange = { shortCode = it.uppercase() },
          label = { Text("Short Code") },
          placeholder = { Text("e.g. PS-01, HQ, CYB") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("office_short_code_input"),
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text("Active for new records", style = MaterialTheme.typography.bodyMedium)
          Switch(checked = isActive, onCheckedChange = { isActive = it })
        }
      }
    },
    confirmButton = {
      Button(
        onClick = { onSave(officeName, shortCode, isActive) },
        enabled = isValid,
        modifier = Modifier.testTag("save_office_button"),
      ) {
        Text("Save")
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
  )
}
