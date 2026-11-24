package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.models.IctCategory
import com.example.models.IctSubCategory
import com.example.ui.components.DetailMetaPair
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ReferenceDetailCard
import com.example.ui.components.SolidPillTag
import com.example.ui.components.VibrantFeatureBanner
import com.example.ui.theme.FloralVariant
import com.example.ui.theme.GradientPaletteRegistry

@Composable
fun IctSettingsPane(
  categories: List<IctCategory>,
  subCategories: List<IctSubCategory>,
  entriesCountByCategory: Map<String, Int>,
  onSaveCategory: (String?, String, String) -> Unit,
  onDeleteCategory: (IctCategory) -> Unit,
  onSaveSubCategory: (String?, String, String, String) -> Unit,
  onDeleteSubCategory: (IctSubCategory) -> Unit,
  modifier: Modifier = Modifier,
) {
  var showAddCategory by remember { mutableStateOf(false) }
  var editingCategory by remember { mutableStateOf<IctCategory?>(null) }
  var deletingCategory by remember { mutableStateOf<IctCategory?>(null) }

  var selectedFilterCategoryId by remember { mutableStateOf<String?>(null) }
  var showAddSubCategory by remember { mutableStateOf(false) }
  var editingSubCategory by remember { mutableStateOf<IctSubCategory?>(null) }
  var deletingSubCategory by remember { mutableStateOf<IctSubCategory?>(null) }

  val activeFilterCategoryId =
    selectedFilterCategoryId?.takeIf { fid -> categories.any { it.id == fid } }

  if (showAddCategory || editingCategory != null) {
    IctCategoryDialog(
      initial = editingCategory,
      onSave = { name, desc ->
        onSaveCategory(editingCategory?.id, name, desc)
        showAddCategory = false
        editingCategory = null
      },
      onDismiss = {
        showAddCategory = false
        editingCategory = null
      },
    )
  }

  deletingCategory?.let { target ->
    val linkedSubs = subCategories.count { it.categoryId == target.id }
    val linkedEntries = entriesCountByCategory[target.id] ?: 0
    AlertDialog(
      onDismissRequest = { deletingCategory = null },
      title = { Text("Delete ICT Category?") },
      text = {
        Text(
          text =
            "Deleting '${target.name}' will also remove $linkedSubs linked sub-category(ies) and $linkedEntries logged entry(ies) under it.",
          style = MaterialTheme.typography.bodyMedium,
        )
      },
      confirmButton = {
        Button(
          onClick = {
            onDeleteCategory(target)
            deletingCategory = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
          modifier = Modifier.testTag("confirm_delete_ict_category"),
        ) {
          Text("Delete")
        }
      },
      dismissButton = { TextButton(onClick = { deletingCategory = null }) { Text("Cancel") } },
    )
  }

  if (showAddSubCategory || editingSubCategory != null) {
    IctSubCategoryDialog(
      initial = editingSubCategory,
      categories = categories,
      defaultCategoryId = activeFilterCategoryId ?: categories.firstOrNull()?.id ?: "",
      onSave = { catId, name, unit ->
        onSaveSubCategory(editingSubCategory?.id, catId, name, unit)
        showAddSubCategory = false
        editingSubCategory = null
      },
      onDismiss = {
        showAddSubCategory = false
        editingSubCategory = null
      },
    )
  }

  deletingSubCategory?.let { target ->
    AlertDialog(
      onDismissRequest = { deletingSubCategory = null },
      title = { Text("Delete Sub-Category?") },
      text = {
        Text(
          text =
            "Remove sub-category '${target.name}'? Existing logged ICT entries remain kept under their parent category.",
          style = MaterialTheme.typography.bodyMedium,
        )
      },
      confirmButton = {
        Button(
          onClick = {
            onDeleteSubCategory(target)
            deletingSubCategory = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
          modifier = Modifier.testTag("confirm_delete_ict_subcategory"),
        ) {
          Text("Delete")
        }
      },
      dismissButton = { TextButton(onClick = { deletingSubCategory = null }) { Text("Cancel") } },
    )
  }

  val ictPalette = GradientPaletteRegistry.paletteForCard("ICT_DATA_SETTINGS", 7)
  val catMap = categories.associateBy { it.id }
  val visibleSubCategories =
    if (activeFilterCategoryId == null) subCategories
    else subCategories.filter { it.categoryId == activeFilterCategoryId }

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      VibrantFeatureBanner(
        title = "ICT Categories",
        subtitle = "${categories.size} categories · ${subCategories.size} sub-categories",
        palette = ictPalette,
        floralVariant = FloralVariant.BOTANICAL_VINE,
        icon = Icons.Outlined.Category,
        trailingContent = {
          Button(
            onClick = { showAddCategory = true },
            colors =
              ButtonDefaults.buttonColors(
                containerColor = androidx.compose.ui.graphics.Color(0xE6FFFFFF),
                contentColor = androidx.compose.ui.graphics.Color(0xFF0F172A),
              ),
            modifier = Modifier.testTag("add_ict_category_button"),
          ) {
            Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Category", fontWeight = FontWeight.Bold)
          }
        },
      )
    }

    item {
      Text(
        text = "ICT Categories (${categories.size})",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
      )
    }

    if (categories.isEmpty()) {
      item {
        EmptyStateCard(
          title = "No ICT Categories configured",
          message = "Add an ICT Category above, then attach linked Sub-Categories for structured data entry.",
          icon = Icons.Outlined.Category,
        )
      }
    } else {
      itemsIndexed(categories, key = { _, cat -> cat.id }) { idx, cat ->
        val cardPalette = GradientPaletteRegistry.paletteForCard(cat.id, idx)
        val accent = cardPalette.colors.first()
        val subCount = subCategories.count { it.categoryId == cat.id }
        val entCount = entriesCountByCategory[cat.id] ?: 0
        ReferenceDetailCard(
          accentColor = accent,
          modifier = Modifier.testTag("ict_category_card_${cat.id}"),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = cat.name,
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f).padding(end = 8.dp),
            )
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
              SolidPillTag(
                text = "$subCount Sub",
                containerColor = accent,
              )
              IconButton(
                onClick = { editingCategory = cat },
                modifier = Modifier.minimumInteractiveComponentSize().testTag("edit_ict_cat_${cat.id}"),
              ) {
                Icon(Icons.Outlined.Edit, contentDescription = "Rename or edit ${cat.name}")
              }
              IconButton(
                onClick = { deletingCategory = cat },
                modifier = Modifier.minimumInteractiveComponentSize().testTag("delete_ict_cat_${cat.id}"),
              ) {
                Icon(
                  Icons.Outlined.DeleteOutline,
                  contentDescription = "Delete ${cat.name}",
                  tint = MaterialTheme.colorScheme.error,
                )
              }
            }
          }
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
          ) {
            DetailMetaPair(label = "Sub-Categories", value = "$subCount")
            DetailMetaPair(label = "Entries", value = "$entCount")
          }
          if (cat.description.isNotBlank()) {
            DetailMetaPair(label = "Notes", value = cat.description)
          }
        }
      }
    }

    item {
      Spacer(modifier = Modifier.height(6.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "Linked Sub-Categories (${visibleSubCategories.size})",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
          modifier = Modifier.weight(1f),
        )
        FilledTonalButton(
          onClick = { showAddSubCategory = true },
          enabled = categories.isNotEmpty(),
          modifier = Modifier.testTag("add_ict_subcategory_button"),
        ) {
          Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Sub-Category")
        }
      }
    }

    if (categories.isNotEmpty()) {
      item {
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          FilterChip(
            selected = activeFilterCategoryId == null,
            onClick = { selectedFilterCategoryId = null },
            label = { Text("All Categories") },
          )
          categories.forEach { cat ->
            FilterChip(
              selected = activeFilterCategoryId == cat.id,
              onClick = {
                selectedFilterCategoryId =
                  if (activeFilterCategoryId == cat.id) null else cat.id
              },
              label = { Text(cat.name) },
            )
          }
        }
      }
    }

    if (visibleSubCategories.isEmpty()) {
      item {
        OutlinedButton(
          onClick = { showAddSubCategory = true },
          enabled = categories.isNotEmpty(),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(
            if (categories.isEmpty()) "Create an ICT Category first to add sub-categories"
            else "Add first linked Sub-Category"
          )
        }
      }
    } else {
      itemsIndexed(visibleSubCategories, key = { _, sub -> sub.id }) { idx, sub ->
        val parentName = catMap[sub.categoryId]?.name ?: "Unlinked"
        val subPalette = GradientPaletteRegistry.paletteForCard(sub.categoryId, idx + 3)
        val accent = subPalette.colors[1]
        ReferenceDetailCard(
          accentColor = accent,
          modifier = Modifier.testTag("ict_subcategory_card_${sub.id}"),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = sub.name,
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f).padding(end = 8.dp),
            )
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
              SolidPillTag(
                text = parentName,
                containerColor = accent,
              )
              IconButton(
                onClick = { editingSubCategory = sub },
                modifier = Modifier.minimumInteractiveComponentSize().testTag("edit_ict_sub_${sub.id}"),
              ) {
                Icon(Icons.Outlined.Edit, contentDescription = "Rename or edit ${sub.name}")
              }
              IconButton(
                onClick = { deletingSubCategory = sub },
                modifier = Modifier.minimumInteractiveComponentSize().testTag("delete_ict_sub_${sub.id}"),
              ) {
                Icon(
                  Icons.Outlined.DeleteOutline,
                  contentDescription = "Delete ${sub.name}",
                  tint = MaterialTheme.colorScheme.error,
                )
              }
            }
          }
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
          ) {
            DetailMetaPair(label = "Category", value = parentName)
            DetailMetaPair(
              label = "Unit",
              value = sub.unitLabel.ifBlank { "Standard" },
            )
          }
        }
      }
    }

    item { Spacer(modifier = Modifier.height(16.dp)) }
  }
}

@Composable
private fun IctCategoryDialog(
  initial: IctCategory?,
  onSave: (String, String) -> Unit,
  onDismiss: () -> Unit,
) {
  var name by remember(initial) { mutableStateOf(initial?.name ?: "") }
  var description by remember(initial) { mutableStateOf(initial?.description ?: "") }
  val isValid = name.isNotBlank()

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(if (initial == null) "Add ICT Category" else "Edit / Rename ICT Category") },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("Category Name") },
          placeholder = { Text("e.g. Network Links, Terminals, Power") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("ict_category_name_input"),
        )
        OutlinedTextField(
          value = description,
          onValueChange = { description = it },
          label = { Text("Description (optional)") },
          maxLines = 2,
          modifier = Modifier.fillMaxWidth().testTag("ict_category_desc_input"),
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { onSave(name, description) },
        enabled = isValid,
        modifier = Modifier.testTag("save_ict_category_button"),
      ) {
        Text("Save")
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
  )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IctSubCategoryDialog(
  initial: IctSubCategory?,
  categories: List<IctCategory>,
  defaultCategoryId: String,
  onSave: (String, String, String) -> Unit,
  onDismiss: () -> Unit,
) {
  var categoryId by remember(initial, categories) {
    mutableStateOf(
      initial?.categoryId?.takeIf { cid -> categories.any { it.id == cid } }
        ?: defaultCategoryId.takeIf { cid -> categories.any { it.id == cid } }
        ?: categories.firstOrNull()?.id
        ?: ""
    )
  }
  var name by remember(initial) { mutableStateOf(initial?.name ?: "") }
  var unitLabel by remember(initial) { mutableStateOf(initial?.unitLabel ?: "") }
  val isValid = categoryId.isNotBlank() && name.isNotBlank()

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(if (initial == null) "Add Linked Sub-Category" else "Edit / Rename Sub-Category")
    },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Text("Parent ICT Category", style = MaterialTheme.typography.labelLarge)
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          categories.forEach { cat ->
            FilterChip(
              selected = categoryId == cat.id,
              onClick = { categoryId = cat.id },
              label = { Text(cat.name) },
            )
          }
        }
        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("Sub-Category Name") },
          placeholder = { Text("e.g. Uptime %, Bandwidth, Scanners") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("ict_subcategory_name_input"),
        )
        OutlinedTextField(
          value = unitLabel,
          onValueChange = { unitLabel = it },
          label = { Text("Unit / Format Label (optional)") },
          placeholder = { Text("e.g. Mbps, count, status") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("ict_subcategory_unit_input"),
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { onSave(categoryId, name, unitLabel) },
        enabled = isValid,
        modifier = Modifier.testTag("save_ict_subcategory_button"),
      ) {
        Text("Save")
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
  )
}
