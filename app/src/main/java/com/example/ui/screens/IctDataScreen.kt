package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.logic.DateUtils
import com.example.models.IctCategory
import com.example.models.IctEntry
import com.example.models.IctSubCategory
import com.example.ui.components.DetailMetaPair
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ReferenceDetailCard
import com.example.ui.components.SolidPillTag
import com.example.ui.components.TintedPillTag
import com.example.ui.components.VibrantFeatureBanner
import com.example.ui.components.rememberSyncedText
import com.example.ui.state.IctDataUiState
import com.example.ui.theme.FloralVariant
import com.example.ui.theme.GradientPaletteRegistry

@Composable
fun IctDataScreen(
  state: IctDataUiState,
  onBack: () -> Unit,
  onOpenSettings: () -> Unit,
  onSelectCategory: (String?) -> Unit,
  onSelectSubCategory: (String?) -> Unit,
  onSearchTextChange: (String) -> Unit,
  onSaveEntry: (String?, String, String?, String, String) -> Unit,
  onDeleteEntry: (IctEntry) -> Unit,
  modifier: Modifier = Modifier,
) {
  var showCreateDialog by remember { mutableStateOf(false) }
  var editingEntry by remember { mutableStateOf<IctEntry?>(null) }
  var deletingEntry by remember { mutableStateOf<IctEntry?>(null) }

  if ((showCreateDialog || editingEntry != null) && state.categories.isNotEmpty()) {
    IctEntryDialog(
      initial = editingEntry,
      categories = state.categories,
      subCategories = state.subCategories,
      defaultCategoryId = state.selectedCategoryId ?: state.categories.first().id,
      defaultSubCategoryId = state.selectedSubCategoryId,
      onSave = { catId, subId, valueText, note ->
        onSaveEntry(editingEntry?.id, catId, subId, valueText, note)
        showCreateDialog = false
        editingEntry = null
      },
      onDismiss = {
        showCreateDialog = false
        editingEntry = null
      },
    )
  }

  deletingEntry?.let { target ->
    AlertDialog(
      onDismissRequest = { deletingEntry = null },
      title = { Text("Delete ICT Entry?") },
      text = {
        Text(
          text = "Remove value '${target.valueText}' (${target.categoryNameSnapshot})?",
          style = MaterialTheme.typography.bodyMedium,
        )
      },
      confirmButton = {
        Button(
          onClick = {
            onDeleteEntry(target)
            deletingEntry = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
          modifier = Modifier.testTag("confirm_delete_ict_entry_button"),
        ) {
          Text("Delete")
        }
      },
      dismissButton = { TextButton(onClick = { deletingEntry = null }) { Text("Cancel") } },
    )
  }

  val bannerPalette = GradientPaletteRegistry.paletteForCard("ict_data", 2)
  val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
  val catMap = state.categories.associateBy { it.id }
  val subMap = state.subCategories.associateBy { it.id }
  val visibleSubFilters =
    if (state.selectedCategoryId == null) state.subCategories
    else state.subCategories.filter { it.categoryId == state.selectedCategoryId }

  Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    Column(modifier = Modifier.fillMaxSize().widthIn(max = 860.dp)) {
      Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        VibrantFeatureBanner(
          title = "ICT Data",
          subtitle = "${state.categories.size} categories · ${state.totalEntriesCount} entries",
          palette = bannerPalette,
          floralVariant = FloralVariant.RADIAL_PETALS,
          icon = Icons.Outlined.Analytics,
          leadingContent = {
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = Color(0x470B1020),
              border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
            ) {
              IconButton(
                onClick = onBack,
                modifier = Modifier.minimumInteractiveComponentSize().testTag("ict_data_back_button"),
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
            Row(
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0x470B1020),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
              ) {
                IconButton(
                  onClick = onOpenSettings,
                  modifier =
                    Modifier.minimumInteractiveComponentSize().testTag("ict_data_settings_button"),
                ) {
                  Icon(
                    imageVector = Icons.Outlined.Tune,
                    contentDescription = "Customize ICT Categories",
                    tint = Color.White,
                  )
                }
              }
              Button(
                onClick = { showCreateDialog = true },
                enabled = state.categories.isNotEmpty(),
                colors =
                  ButtonDefaults.buttonColors(
                    containerColor = Color(0xE6FFFFFF),
                    contentColor = Color(0xFF0F172A),
                  ),
                modifier = Modifier.height(44.dp).testTag("add_ict_entry_button"),
              ) {
                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add", fontWeight = FontWeight.Bold)
              }
            }
          },
        )
      }

      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        if (state.categories.isEmpty()) {
          item {
            EmptyStateCard(
              title = "No ICT Categories configured yet",
              message = "Customize ICT Categories & linked Sub-Categories in Settings before logging ICT values.",
              icon = Icons.Outlined.Analytics,
            )
          }
          item {
            Button(
              onClick = onOpenSettings,
              modifier = Modifier.fillMaxWidth().testTag("open_ict_settings_button"),
            ) {
              Icon(Icons.Outlined.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text("Configure ICT Categories in Settings")
            }
          }
        } else {
          item {
            val (syncedSearchText, onSearchEdit) =
              rememberSyncedText(
                externalValue = state.searchText,
                onValueChange = onSearchTextChange,
              )
            OutlinedTextField(
              value = syncedSearchText,
              onValueChange = onSearchEdit,
              label = { Text("Search ICT value, note, category, or sub-category") },
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
              modifier = Modifier.fillMaxWidth().testTag("ict_data_search_input"),
            )
          }

          // Configured Categories Filter Strip
          item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Text(
                text = "Categories",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                FilterChip(
                  selected = state.selectedCategoryId == null,
                  onClick = { onSelectCategory(null) },
                  label = { Text("All (${state.categories.size})") },
                )
                state.categories.forEachIndexed { idx, cat ->
                  val accent = GradientPaletteRegistry.paletteForCard(cat.id, idx).colors.first()
                  val selected = state.selectedCategoryId == cat.id
                  FilterChip(
                    selected = selected,
                    onClick = { onSelectCategory(if (selected) null else cat.id) },
                    label = {
                      Text(
                        text = cat.name,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                      )
                    },
                    border =
                      FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selected,
                        borderColor = accent.copy(alpha = 0.45f),
                        selectedBorderColor = accent,
                        borderWidth = 1.dp,
                        selectedBorderWidth = 1.5.dp,
                      ),
                    colors =
                      FilterChipDefaults.filterChipColors(
                        containerColor = accent.copy(alpha = if (isDark) 0.18f else 0.10f),
                        selectedContainerColor = accent.copy(alpha = if (isDark) 0.34f else 0.22f),
                        labelColor = MaterialTheme.colorScheme.onSurface,
                        selectedLabelColor = MaterialTheme.colorScheme.onSurface,
                      ),
                  )
                }
              }
            }
          }

          // Linked Sub-Categories Strip
          if (visibleSubFilters.isNotEmpty()) {
            item {
              Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                  text = "Linked Sub-Categories",
                  style = MaterialTheme.typography.labelLarge,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                  modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  FilterChip(
                    selected = state.selectedSubCategoryId == null,
                    onClick = { onSelectSubCategory(null) },
                    label = { Text("All Sub-Categories") },
                  )
                  visibleSubFilters.forEachIndexed { idx, sub ->
                    val accent =
                      GradientPaletteRegistry.paletteForCard(sub.categoryId, idx + 2).colors[1]
                    val selected = state.selectedSubCategoryId == sub.id
                    FilterChip(
                      selected = selected,
                      onClick = { onSelectSubCategory(if (selected) null else sub.id) },
                      label = {
                        Text(
                          text =
                            if (sub.unitLabel.isNotBlank()) "${sub.name} (${sub.unitLabel})"
                            else sub.name,
                          fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        )
                      },
                      border =
                        FilterChipDefaults.filterChipBorder(
                          enabled = true,
                          selected = selected,
                          borderColor = accent.copy(alpha = 0.45f),
                          selectedBorderColor = accent,
                        ),
                      colors =
                        FilterChipDefaults.filterChipColors(
                          containerColor = accent.copy(alpha = if (isDark) 0.16f else 0.08f),
                          selectedContainerColor = accent.copy(alpha = if (isDark) 0.32f else 0.20f),
                        ),
                    )
                  }
                }
              }
            }
          }

          // Configured Structure Overview Pills
          item {
            OutlinedButton(
              onClick = onOpenSettings,
              modifier = Modifier.fillMaxWidth().testTag("manage_ict_categories_button"),
            ) {
              Icon(Icons.Outlined.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Manage ICT Categories & Sub-Categories in Settings")
            }
          }

          if (state.entries.isEmpty()) {
            item {
              EmptyStateCard(
                title = "No ICT data entries yet",
                message = "Tap Add in the top bar to save a numeric or text value for your configured ICT categories.",
                icon = Icons.Outlined.Analytics,
              )
            }
          } else {
            itemsIndexed(state.entries, key = { _, e -> e.id }) { idx, entry ->
              val catObj = catMap[entry.categoryId]
              val subObj = entry.subCategoryId?.let { subMap[it] }
              val categoryName = catObj?.name ?: entry.categoryNameSnapshot.ifBlank { "Category" }
              val subCategoryName =
                subObj?.name ?: entry.subCategoryNameSnapshot.ifBlank { "General" }
              val unit = subObj?.unitLabel.orEmpty()
              val accent = GradientPaletteRegistry.paletteForCard(entry.categoryId, idx).colors.first()

              ReferenceDetailCard(
                accentColor = accent,
                modifier = Modifier.testTag("ict_entry_card_${entry.id}"),
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  val displayTitle =
                    if (unit.isNotBlank()) "${entry.valueText} $unit" else entry.valueText
                  Text(
                    text = displayTitle,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                  )
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                  ) {
                    SolidPillTag(
                      text = categoryName,
                      containerColor = accent,
                    )
                    IconButton(
                      onClick = { editingEntry = entry },
                      modifier = Modifier.minimumInteractiveComponentSize(),
                    ) {
                      Icon(Icons.Outlined.Edit, contentDescription = "Edit entry")
                    }
                    IconButton(
                      onClick = { deletingEntry = entry },
                      modifier = Modifier.minimumInteractiveComponentSize(),
                    ) {
                      Icon(
                        Icons.Outlined.DeleteOutline,
                        contentDescription = "Delete entry",
                        tint = MaterialTheme.colorScheme.error,
                      )
                    }
                  }
                }

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                  DetailMetaPair(label = "Sub-Category", value = subCategoryName)
                  DetailMetaPair(
                    label = "Recorded",
                    value = DateUtils.formatTimestamp(entry.recordedAt),
                  )
                }

                if (entry.note.isNotBlank()) {
                  TintedPillTag(text = entry.note, accentColor = accent)
                }
              }
            }
          }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IctEntryDialog(
  initial: IctEntry?,
  categories: List<IctCategory>,
  subCategories: List<IctSubCategory>,
  defaultCategoryId: String,
  defaultSubCategoryId: String?,
  onSave: (String, String?, String, String) -> Unit,
  onDismiss: () -> Unit,
) {
  var selectedCategoryId by remember(initial, categories) {
    mutableStateOf(
      initial?.categoryId?.takeIf { cid -> categories.any { it.id == cid } }
        ?: defaultCategoryId.takeIf { cid -> categories.any { it.id == cid } }
        ?: categories.first().id
    )
  }
  val linkedSubs = subCategories.filter { it.categoryId == selectedCategoryId }
  var selectedSubCategoryId by remember(initial, selectedCategoryId) {
    mutableStateOf(
      initial?.subCategoryId?.takeIf { sid -> linkedSubs.any { it.id == sid } }
        ?: defaultSubCategoryId?.takeIf { sid -> linkedSubs.any { it.id == sid } }
        ?: linkedSubs.firstOrNull()?.id
    )
  }
  var valueText by remember(initial) { mutableStateOf(initial?.valueText ?: "") }
  var note by remember(initial) { mutableStateOf(initial?.note ?: "") }
  val isValid = selectedCategoryId.isNotBlank() && valueText.isNotBlank()

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(if (initial == null) "Log ICT Value" else "Edit ICT Value") },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Text("Category", style = MaterialTheme.typography.labelLarge)
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          categories.forEach { cat ->
            FilterChip(
              selected = selectedCategoryId == cat.id,
              onClick = {
                selectedCategoryId = cat.id
                val nextSubs = subCategories.filter { it.categoryId == cat.id }
                selectedSubCategoryId = nextSubs.firstOrNull()?.id
              },
              label = { Text(cat.name) },
            )
          }
        }

        Text("Linked Sub-Category", style = MaterialTheme.typography.labelLarge)
        if (linkedSubs.isEmpty()) {
          Text(
            text = "No sub-categories linked to this category yet (saving under General).",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        } else {
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            FilterChip(
              selected = selectedSubCategoryId == null,
              onClick = { selectedSubCategoryId = null },
              label = { Text("General") },
            )
            linkedSubs.forEach { sub ->
              FilterChip(
                selected = selectedSubCategoryId == sub.id,
                onClick = { selectedSubCategoryId = sub.id },
                label = {
                  Text(if (sub.unitLabel.isNotBlank()) "${sub.name} (${sub.unitLabel})" else sub.name)
                },
              )
            }
          }
        }

        OutlinedTextField(
          value = valueText,
          onValueChange = { valueText = it },
          label = { Text("Value (number or text)") },
          placeholder = { Text("e.g. 99.4, 12, Active") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("ict_entry_value_input"),
        )

        OutlinedTextField(
          value = note,
          onValueChange = { note = it },
          label = { Text("Optional Note") },
          maxLines = 3,
          modifier = Modifier.fillMaxWidth().testTag("ict_entry_note_input"),
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { onSave(selectedCategoryId, selectedSubCategoryId, valueText, note) },
        enabled = isValid,
        modifier = Modifier.testTag("save_ict_entry_button"),
      ) {
        Text("Save")
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
  )
}
