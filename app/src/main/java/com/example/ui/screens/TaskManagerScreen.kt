package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.models.TaskCategory
import com.example.models.TaskCategoryGroup
import com.example.models.TaskInputParser
import com.example.models.TaskItem
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.FloralTextureOverlay
import com.example.ui.components.GradientAccentStrip
import com.example.ui.components.TintedPillTag
import com.example.ui.components.VibrantFeatureBanner
import com.example.ui.state.TaskManagerUiState
import com.example.ui.theme.FloralVariant
import com.example.ui.theme.GradientPalette
import com.example.ui.theme.GradientPaletteRegistry

private data class GlobalTaskItemHit(
  val category: TaskCategory,
  val categoryIndex: Int,
  val item: TaskItem,
)

@Composable
fun TaskManagerScreen(
  state: TaskManagerUiState,
  onBack: () -> Unit,
  onSelectCategory: (String?) -> Unit,
  onAddCategories: (String, Boolean) -> Unit,
  onRenameCategory: (TaskCategory, String) -> Unit,
  onDeleteCategory: (TaskCategory) -> Unit,
  onAddItems: (String, String, Boolean) -> Unit,
  onToggleItem: (TaskItem) -> Unit,
  onRenameItem: (TaskItem, String) -> Unit,
  onDeleteItem: (TaskItem) -> Unit,
  modifier: Modifier = Modifier,
) {
  var isSearchOpen by rememberSaveable { mutableStateOf(false) }
  var searchQuery by rememberSaveable { mutableStateOf("") }
  var useCommaSeparator by rememberSaveable { mutableStateOf(true) }

  var showAddCategoryDialog by remember { mutableStateOf(false) }
  var renamingCategory by remember { mutableStateOf<TaskCategory?>(null) }
  var deletingCategory by remember { mutableStateOf<TaskCategory?>(null) }
  var addItemTargetCategory by remember { mutableStateOf<TaskCategory?>(null) }
  var renamingItem by remember { mutableStateOf<TaskItem?>(null) }
  var deletingItem by remember { mutableStateOf<TaskItem?>(null) }

  val bannerPalette = GradientPaletteRegistry.paletteForCard("task_manager", 3)
  val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

  val openedGroupIndex =
    state.categoryGroups.indexOfFirst { it.category.id == state.selectedCategoryId }
  val openedGroup = if (openedGroupIndex >= 0) state.categoryGroups[openedGroupIndex] else null

  // Handle system back cleanly across Search -> Item Page -> Category Page -> Dashboard
  BackHandler(enabled = isSearchOpen) {
    isSearchOpen = false
  }
  BackHandler(enabled = !isSearchOpen && openedGroup != null) {
    onSelectCategory(null)
  }

  if (showAddCategoryDialog) {
    VibrantTaskInputDialog(
      title = "Add Task Categories",
      subtitle = "Create a checklist category",
      fieldLabel =
        if (useCommaSeparator) "Category name(s), comma-separated"
        else "Category name",
      placeholder = if (useCommaSeparator) "Server, Network, Daily" else "Daily Operations",
      icon = Icons.Outlined.Category,
      palette = bannerPalette,
      floralVariant = FloralVariant.RADIAL_PETALS,
      useCommaSeparator = useCommaSeparator,
      onToggleCommaSeparator = { useCommaSeparator = it },
      onSave = { raw ->
        onAddCategories(raw, useCommaSeparator)
        showAddCategoryDialog = false
      },
      onDismiss = { showAddCategoryDialog = false },
      testTagInput = "task_category_input",
      testTagConfirm = "confirm_add_task_category",
    )
  }

  renamingCategory?.let { cat ->
    val catIndex = state.categoryGroups.indexOfFirst { it.category.id == cat.id }.coerceAtLeast(0)
    VibrantRenameDialog(
      title = "Rename Category",
      subtitle = "Update checklist category title",
      initialValue = cat.name,
      fieldLabel = "Category Name",
      icon = Icons.Outlined.Category,
      palette = GradientPaletteRegistry.paletteForCard(cat.id, catIndex),
      floralVariant = GradientPaletteRegistry.floralVariantForCard(cat.id, catIndex),
      onSave = { newName ->
        onRenameCategory(cat, newName)
        renamingCategory = null
      },
      onDismiss = { renamingCategory = null },
    )
  }

  deletingCategory?.let { cat ->
    DeleteConfirmDialog(
      title = "Delete '${cat.name}'?",
      message = "Deletes this category and all task items inside it.",
      onConfirm = {
        onDeleteCategory(cat)
        deletingCategory = null
      },
      onDismiss = { deletingCategory = null },
    )
  }

  addItemTargetCategory?.let { targetCat ->
    val catIndex =
      state.categoryGroups.indexOfFirst { it.category.id == targetCat.id }.coerceAtLeast(0)
    VibrantTaskInputDialog(
      title = "Add Task Items",
      subtitle = "Category: ${targetCat.name}",
      fieldLabel =
        if (useCommaSeparator) "Task item(s), comma-separated"
        else "Task item",
      placeholder =
        if (useCommaSeparator) "Check UPS, Verify logs, Lock gate"
        else "Verify evening handover",
      icon = Icons.Outlined.Checklist,
      palette = GradientPaletteRegistry.paletteForCard(targetCat.id, catIndex),
      floralVariant = GradientPaletteRegistry.floralVariantForCard(targetCat.id, catIndex),
      useCommaSeparator = useCommaSeparator,
      onToggleCommaSeparator = { useCommaSeparator = it },
      onSave = { raw ->
        onAddItems(targetCat.id, raw, useCommaSeparator)
        addItemTargetCategory = null
      },
      onDismiss = { addItemTargetCategory = null },
      testTagInput = "task_item_input",
      testTagConfirm = "confirm_add_task_item",
    )
  }

  renamingItem?.let { item ->
    VibrantRenameDialog(
      title = "Edit Task Item",
      subtitle = "Update checklist entry",
      initialValue = item.title,
      fieldLabel = "Task Title",
      icon = Icons.Outlined.Checklist,
      palette = GradientPaletteRegistry.paletteForCard(item.id, 1),
      floralVariant = FloralVariant.CORNER_CLUSTER,
      onSave = { newTitle ->
        onRenameItem(item, newTitle)
        renamingItem = null
      },
      onDismiss = { renamingItem = null },
    )
  }

  deletingItem?.let { item ->
    DeleteConfirmDialog(
      title = "Delete '${item.title}'?",
      message = "Remove this task item permanently?",
      onConfirm = {
        onDeleteItem(item)
        deletingItem = null
      },
      onDismiss = { deletingItem = null },
    )
  }

  when {
    isSearchOpen -> {
      TaskManagerGlobalSearchPage(
        state = state,
        searchQuery = searchQuery,
        bannerPalette = bannerPalette,
        isDark = isDark,
        onSearchQueryChange = { searchQuery = it },
        onCloseSearch = { isSearchOpen = false },
        onOpenCategory = { categoryId ->
          isSearchOpen = false
          onSelectCategory(categoryId)
        },
        onToggleItem = onToggleItem,
        onRenameItem = { renamingItem = it },
        onDeleteItem = { deletingItem = it },
        modifier = modifier,
      )
    }

    openedGroup != null -> {
      TaskCategoryItemsPage(
        group = openedGroup,
        categoryIndex = openedGroupIndex,
        isDark = isDark,
        useCommaSeparator = useCommaSeparator,
        onToggleCommaSeparator = { useCommaSeparator = it },
        onBackToCategories = { onSelectCategory(null) },
        onOpenSearch = { isSearchOpen = true },
        onAddItems = { addItemTargetCategory = openedGroup.category },
        onRenameCategory = { renamingCategory = openedGroup.category },
        onDeleteCategory = { deletingCategory = openedGroup.category },
        onToggleItem = onToggleItem,
        onRenameItem = { renamingItem = it },
        onDeleteItem = { deletingItem = it },
        modifier = modifier,
      )
    }

    else -> {
      TaskCategoryListPage(
        state = state,
        bannerPalette = bannerPalette,
        isDark = isDark,
        useCommaSeparator = useCommaSeparator,
        onToggleCommaSeparator = { useCommaSeparator = it },
        onBackToDashboard = onBack,
        onOpenSearch = { isSearchOpen = true },
        onAddCategories = { showAddCategoryDialog = true },
        onOpenCategory = { cat -> onSelectCategory(cat.id) },
        onRenameCategory = { renamingCategory = it },
        onDeleteCategory = { deletingCategory = it },
        modifier = modifier,
      )
    }
  }
}

@Composable
private fun TaskCategoryListPage(
  state: TaskManagerUiState,
  bannerPalette: GradientPalette,
  isDark: Boolean,
  useCommaSeparator: Boolean,
  onToggleCommaSeparator: (Boolean) -> Unit,
  onBackToDashboard: () -> Unit,
  onOpenSearch: () -> Unit,
  onAddCategories: () -> Unit,
  onOpenCategory: (TaskCategory) -> Unit,
  onRenameCategory: (TaskCategory) -> Unit,
  onDeleteCategory: (TaskCategory) -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    Column(modifier = Modifier.fillMaxSize().widthIn(max = 860.dp)) {
      Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        VibrantFeatureBanner(
          title = "Task Manager",
          subtitle =
            "${state.categories.size} categories · ${state.completedItemsCount}/${state.totalItemsCount} checked",
          palette = bannerPalette,
          floralVariant = FloralVariant.RADIAL_PETALS,
          icon = Icons.Outlined.Checklist,
          leadingContent = {
            BannerFrostedIconButton(
              icon = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back to Dashboard",
              onClick = onBackToDashboard,
              testTag = "task_manager_back_button",
            )
          },
          trailingContent = {
            Row(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              BannerFrostedIconButton(
                icon = Icons.Outlined.Search,
                contentDescription = "Search Task Manager",
                onClick = onOpenSearch,
                testTag = "task_manager_search_button",
              )
              Button(
                onClick = onAddCategories,
                colors =
                  ButtonDefaults.buttonColors(
                    containerColor = Color(0xE6FFFFFF),
                    contentColor = Color(0xFF0F172A),
                  ),
                modifier = Modifier.height(44.dp).testTag("add_task_category_button"),
              ) {
                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Category", fontWeight = FontWeight.Bold)
              }
            }
          },
        )
      }

      LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("task_category_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        item {
          CommaSeparatorOptionCard(
            palette = bannerPalette,
            useCommaSeparator = useCommaSeparator,
            onToggleCommaSeparator = onToggleCommaSeparator,
          )
        }

        if (state.categoryGroups.isEmpty()) {
          item {
            EmptyStateCard(
              title = "No task categories yet",
              message =
                "Tap Category above to create a task list (try comma-separated names when Comma Separator is ON).",
              icon = Icons.Outlined.Category,
            )
          }
        } else {
          // Only categories shown here in a clean single-column vertical list
          itemsIndexed(
            state.categoryGroups,
            key = { _, group -> group.category.id },
          ) { idx, group ->
            val cat = group.category
            val catPalette = GradientPaletteRegistry.paletteForCard(cat.id, idx)
            val floralVariant = GradientPaletteRegistry.floralVariantForCard(cat.id, idx)
            val accent = catPalette.colors.first()

            CategorySummaryCard(
              group = group,
              palette = catPalette,
              floralVariant = floralVariant,
              accent = accent,
              isDark = isDark,
              searchNeedle = "",
              onOpen = { onOpenCategory(cat) },
              onRename = { onRenameCategory(cat) },
              onDelete = { onDeleteCategory(cat) },
            )
          }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
      }
    }
  }
}

@Composable
private fun TaskCategoryItemsPage(
  group: TaskCategoryGroup,
  categoryIndex: Int,
  isDark: Boolean,
  useCommaSeparator: Boolean,
  onToggleCommaSeparator: (Boolean) -> Unit,
  onBackToCategories: () -> Unit,
  onOpenSearch: () -> Unit,
  onAddItems: () -> Unit,
  onRenameCategory: () -> Unit,
  onDeleteCategory: () -> Unit,
  onToggleItem: (TaskItem) -> Unit,
  onRenameItem: (TaskItem) -> Unit,
  onDeleteItem: (TaskItem) -> Unit,
  modifier: Modifier = Modifier,
) {
  val cat = group.category
  val catPalette = GradientPaletteRegistry.paletteForCard(cat.id, categoryIndex)
  val floralVariant = GradientPaletteRegistry.floralVariantForCard(cat.id, categoryIndex)
  val accent = catPalette.colors.first()

  Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    Column(modifier = Modifier.fillMaxSize().widthIn(max = 860.dp)) {
      Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        VibrantFeatureBanner(
          title = cat.name,
          subtitle = "${group.completedCount}/${group.items.size} checked",
          palette = catPalette,
          floralVariant = floralVariant,
          icon = Icons.Outlined.Checklist,
          leadingContent = {
            BannerFrostedIconButton(
              icon = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back to Categories",
              onClick = onBackToCategories,
              testTag = "task_items_back_button",
            )
          },
          trailingContent = {
            Row(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              BannerFrostedIconButton(
                icon = Icons.Outlined.Search,
                contentDescription = "Search Task Manager",
                onClick = onOpenSearch,
                testTag = "task_manager_search_button",
              )
              Button(
                onClick = onAddItems,
                colors =
                  ButtonDefaults.buttonColors(
                    containerColor = Color(0xE6FFFFFF),
                    contentColor = Color(0xFF0F172A),
                  ),
                modifier = Modifier.height(44.dp).testTag("add_task_item_button"),
              ) {
                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Item", fontWeight = FontWeight.Bold)
              }
            }
          },
        )
      }

      LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("task_items_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        item {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              OutlinedButton(
                onClick = onRenameCategory,
                modifier = Modifier.minimumInteractiveComponentSize(),
              ) {
                Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Rename Category")
              }
              IconButton(
                onClick = onDeleteCategory,
                modifier =
                  Modifier.minimumInteractiveComponentSize().testTag("delete_task_category_button"),
              ) {
                Icon(
                  imageVector = Icons.Outlined.DeleteOutline,
                  contentDescription = "Delete ${cat.name}",
                  tint = MaterialTheme.colorScheme.error,
                )
              }
            }

            TintedPillTag(
              text = "${group.completedCount}/${group.items.size}",
              accentColor = accent,
            )
          }
        }

        item {
          CommaSeparatorOptionCard(
            palette = catPalette,
            useCommaSeparator = useCommaSeparator,
            onToggleCommaSeparator = onToggleCommaSeparator,
          )
        }

        if (group.items.isEmpty()) {
          item {
            EmptyStateCard(
              title = "No items in '${cat.name}'",
              message = "Tap Item above to add checkbox tasks manually or using Comma Separator.",
              icon = Icons.Outlined.Checklist,
            )
          }
        } else {
          itemsIndexed(group.items, key = { _, item -> item.id }) { idx, item ->
            val itemPalette = GradientPaletteRegistry.paletteForCard(item.id, idx)
            TaskChecklistItemRow(
              item = item,
              accentColor = itemPalette.colors.first(),
              isDark = isDark,
              searchNeedle = "",
              categoryBadge = null,
              onToggle = { onToggleItem(item) },
              onRename = { onRenameItem(item) },
              onDelete = { onDeleteItem(item) },
              onOpenCategory = null,
            )
          }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
      }
    }
  }
}

@Composable
private fun TaskManagerGlobalSearchPage(
  state: TaskManagerUiState,
  searchQuery: String,
  bannerPalette: GradientPalette,
  isDark: Boolean,
  onSearchQueryChange: (String) -> Unit,
  onCloseSearch: () -> Unit,
  onOpenCategory: (String) -> Unit,
  onToggleItem: (TaskItem) -> Unit,
  onRenameItem: (TaskItem) -> Unit,
  onDeleteItem: (TaskItem) -> Unit,
  modifier: Modifier = Modifier,
) {
  val focusRequester = remember { FocusRequester() }
  LaunchedEffect(Unit) {
    focusRequester.requestFocus()
  }

  val needle = searchQuery.trim()
  val matchingCategories =
    remember(state.categoryGroups, needle) {
      if (needle.isEmpty()) emptyList()
      else {
        state.categoryGroups.mapIndexedNotNull { idx, group ->
          if (group.category.name.contains(needle, ignoreCase = true)) idx to group else null
        }
      }
    }

  val matchingItems =
    remember(state.categoryGroups, needle) {
      if (needle.isEmpty()) emptyList()
      else {
        buildList {
          state.categoryGroups.forEachIndexed { idx, group ->
            group.items.forEach { item ->
              if (
                item.title.contains(needle, ignoreCase = true) ||
                  group.category.name.contains(needle, ignoreCase = true)
              ) {
                add(GlobalTaskItemHit(category = group.category, categoryIndex = idx, item = item))
              }
            }
          }
        }
      }
    }

  Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    Column(modifier = Modifier.fillMaxSize().widthIn(max = 860.dp)) {
      Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        VibrantFeatureBanner(
          title = "Search Task Manager",
          subtitle = "Search across all ${state.categories.size} categories & ${state.totalItemsCount} items",
          palette = bannerPalette,
          floralVariant = FloralVariant.SCATTERED_BLOSSOMS,
          icon = Icons.Outlined.Search,
          leadingContent = {
            BannerFrostedIconButton(
              icon = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back",
              onClick = onCloseSearch,
              testTag = "task_search_back_button",
            )
          },
        )
      }

      LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("task_global_search_results"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        item {
          OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            label = { Text("Search any category or task item") },
            placeholder = { Text("Type keyword...") },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            trailingIcon =
              if (searchQuery.isNotEmpty()) {
                {
                  IconButton(
                    onClick = { onSearchQueryChange("") },
                    modifier =
                      Modifier.minimumInteractiveComponentSize().testTag("clear_task_search_button"),
                  ) {
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
            modifier =
              Modifier.fillMaxWidth()
                .focusRequester(focusRequester)
                .testTag("task_manager_search_input"),
          )
        }

        if (needle.isEmpty()) {
          item {
            EmptyStateCard(
              title = "Search Entire Task Manager",
              message =
                "Enter a keyword to find matching categories or checklist items across all ${state.categories.size} categories.",
              icon = Icons.Outlined.Search,
            )
          }
        } else if (matchingCategories.isEmpty() && matchingItems.isEmpty()) {
          item {
            EmptyStateCard(
              title = "No matching categories or items",
              message = "Try a different keyword or clear the search box.",
              icon = Icons.Outlined.Search,
            )
          }
        } else {
          if (matchingCategories.isNotEmpty()) {
            item {
              Text(
                text = "Categories (${matchingCategories.size})",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 4.dp),
              )
            }
            itemsIndexed(
              matchingCategories,
              key = { _, (_, group) -> "search_cat_${group.category.id}" },
            ) { _, (origIdx, group) ->
              val cat = group.category
              val catPalette = GradientPaletteRegistry.paletteForCard(cat.id, origIdx)
              val floralVariant = GradientPaletteRegistry.floralVariantForCard(cat.id, origIdx)
              CategorySummaryCard(
                group = group,
                palette = catPalette,
                floralVariant = floralVariant,
                accent = catPalette.colors.first(),
                isDark = isDark,
                searchNeedle = needle,
                onOpen = { onOpenCategory(cat.id) },
                onRename = null,
                onDelete = null,
              )
            }
          }

          if (matchingItems.isNotEmpty()) {
            item {
              Text(
                text = "Task Items (${matchingItems.size})",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 6.dp),
              )
            }
            itemsIndexed(
              matchingItems,
              key = { _, hit -> "search_item_${hit.item.id}" },
            ) { idx, hit ->
              val itemPalette = GradientPaletteRegistry.paletteForCard(hit.item.id, idx)
              TaskChecklistItemRow(
                item = hit.item,
                accentColor = itemPalette.colors.first(),
                isDark = isDark,
                searchNeedle = needle,
                categoryBadge = hit.category.name,
                onToggle = { onToggleItem(hit.item) },
                onRename = { onRenameItem(hit.item) },
                onDelete = { onDeleteItem(hit.item) },
                onOpenCategory = { onOpenCategory(hit.category.id) },
              )
            }
          }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
      }
    }
  }
}

@Composable
private fun BannerFrostedIconButton(
  icon: ImageVector,
  contentDescription: String,
  onClick: () -> Unit,
  testTag: String,
) {
  Surface(
    shape = RoundedCornerShape(12.dp),
    color = Color(0x470B1020),
    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
  ) {
    IconButton(
      onClick = onClick,
      modifier = Modifier.minimumInteractiveComponentSize().testTag(testTag),
    ) {
      Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        tint = Color.White,
      )
    }
  }
}

@Composable
private fun CommaSeparatorOptionCard(
  palette: GradientPalette,
  useCommaSeparator: Boolean,
  onToggleCommaSeparator: (Boolean) -> Unit,
) {
  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, palette.colors.first().copy(alpha = 0.40f)),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(modifier = Modifier.fillMaxWidth()) {
      GradientAccentStrip(palette = palette, height = 3.dp)
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
          Text(
            text = "Comma Separator",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
          )
          Text(
            text =
              if (useCommaSeparator) "ON: 'A, B, C' creates separate entries in one go"
              else "OFF: Adds a single entry preserving commas",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Switch(
          checked = useCommaSeparator,
          onCheckedChange = onToggleCommaSeparator,
          modifier = Modifier.testTag("task_comma_separator_toggle"),
        )
      }
    }
  }
}

@Composable
private fun CategorySummaryCard(
  group: TaskCategoryGroup,
  palette: GradientPalette,
  floralVariant: FloralVariant,
  accent: Color,
  isDark: Boolean,
  searchNeedle: String,
  onOpen: () -> Unit,
  onRename: (() -> Unit)?,
  onDelete: (() -> Unit)?,
) {
  val cat = group.category
  val cardShape = RoundedCornerShape(18.dp)

  Surface(
    shape = cardShape,
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.15.dp, accent.copy(alpha = if (isDark) 0.58f else 0.52f)),
    modifier =
      Modifier.fillMaxWidth()
        .clip(cardShape)
        .clickable(onClick = onOpen)
        .testTag("task_cat_chip_${cat.id}"),
  ) {
    Column(modifier = Modifier.fillMaxWidth()) {
      GradientAccentStrip(palette = palette, height = 4.dp)

      Box(
        modifier =
          Modifier.fillMaxWidth()
            .background(accent.copy(alpha = if (isDark) 0.14f else 0.07f))
      ) {
        FloralTextureOverlay(
          variant = floralVariant,
          motifColor = accent.copy(alpha = if (isDark) 0.22f else 0.16f),
          modifier = Modifier.matchParentSize(),
        )

        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = accent.copy(alpha = if (isDark) 0.28f else 0.18f),
            border = BorderStroke(1.dp, accent.copy(alpha = 0.45f)),
            modifier = Modifier.size(40.dp),
          ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
              Icon(
                imageVector = Icons.Outlined.Category,
                contentDescription = null,
                tint = if (isDark) Color.White else accent,
                modifier = Modifier.size(20.dp),
              )
            }
          }

          Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
              text =
                buildHighlightedText(
                  text = cat.name,
                  needle = searchNeedle,
                  highlightContainer = accent.copy(alpha = if (isDark) 0.45f else 0.24f),
                  highlightContent = MaterialTheme.colorScheme.onSurface,
                ),
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 2,
              overflow = TextOverflow.Ellipsis,
            )
            TintedPillTag(
              text = "${group.completedCount}/${group.items.size} items",
              accentColor = accent,
            )
          }

          if (onRename != null) {
            IconButton(
              onClick = onRename,
              modifier = Modifier.minimumInteractiveComponentSize(),
            ) {
              Icon(
                imageVector = Icons.Outlined.Edit,
                contentDescription = "Rename ${cat.name}",
              )
            }
          }
          if (onDelete != null) {
            IconButton(
              onClick = onDelete,
              modifier =
                Modifier.minimumInteractiveComponentSize().testTag("delete_task_category_button"),
            ) {
              Icon(
                imageVector = Icons.Outlined.DeleteOutline,
                contentDescription = "Delete ${cat.name}",
                tint = MaterialTheme.colorScheme.error,
              )
            }
          }
          Icon(
            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = "Open ${cat.name}",
            tint = accent,
          )
        }
      }
    }
  }
}

@Composable
private fun TaskChecklistItemRow(
  item: TaskItem,
  accentColor: Color,
  isDark: Boolean,
  searchNeedle: String,
  categoryBadge: String?,
  onToggle: () -> Unit,
  onRename: () -> Unit,
  onDelete: () -> Unit,
  onOpenCategory: (() -> Unit)?,
) {
  val rowBorder =
    if (item.isCompleted) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)
    else accentColor.copy(alpha = if (isDark) 0.55f else 0.45f)
  val rowBg =
    if (item.isCompleted) MaterialTheme.colorScheme.surfaceContainerLow
    else accentColor.copy(alpha = if (isDark) 0.14f else 0.08f)

  Surface(
    shape = RoundedCornerShape(14.dp),
    color = rowBg,
    border = BorderStroke(1.dp, rowBorder),
    modifier =
      Modifier.fillMaxWidth()
        .clip(RoundedCornerShape(14.dp))
        .clickable(onClick = onToggle)
        .testTag("task_item_card_${item.id}"),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Checkbox(
        checked = item.isCompleted,
        onCheckedChange = { onToggle() },
        colors =
          CheckboxDefaults.colors(
            checkedColor = accentColor,
            uncheckedColor = accentColor.copy(alpha = 0.85f),
            checkmarkColor =
              if (accentColor.luminance() > 0.55f) Color(0xFF0F172A) else Color.White,
          ),
        modifier =
          Modifier.minimumInteractiveComponentSize().testTag("task_item_checkbox_${item.id}"),
      )
      Column(
        modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Text(
          text =
            buildHighlightedText(
              text = item.title,
              needle = searchNeedle,
              highlightContainer = accentColor.copy(alpha = if (isDark) 0.42f else 0.24f),
              highlightContent = MaterialTheme.colorScheme.onSurface,
            ),
          style =
            MaterialTheme.typography.bodyLarge.copy(
              fontWeight = if (item.isCompleted) FontWeight.Normal else FontWeight.Medium,
              textDecoration =
                if (item.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
            ),
          color =
            if (item.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
            else MaterialTheme.colorScheme.onSurface,
          maxLines = 3,
          overflow = TextOverflow.Ellipsis,
        )
        if (!categoryBadge.isNullOrBlank()) {
          TintedPillTag(
            text = categoryBadge,
            accentColor = accentColor,
            modifier =
              if (onOpenCategory != null) {
                Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onOpenCategory)
              } else {
                Modifier
              },
          )
        }
      }
      if (onOpenCategory != null) {
        IconButton(
          onClick = onOpenCategory,
          modifier = Modifier.minimumInteractiveComponentSize(),
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = "Open category",
            tint = accentColor,
          )
        }
      }
      IconButton(
        onClick = onRename,
        modifier = Modifier.minimumInteractiveComponentSize(),
      ) {
        Icon(Icons.Outlined.Edit, contentDescription = "Edit ${item.title}")
      }
      IconButton(
        onClick = onDelete,
        modifier = Modifier.minimumInteractiveComponentSize(),
      ) {
        Icon(
          imageVector = Icons.Outlined.DeleteOutline,
          contentDescription = "Delete ${item.title}",
          tint = MaterialTheme.colorScheme.error,
        )
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VibrantTaskInputDialog(
  title: String,
  subtitle: String,
  fieldLabel: String,
  placeholder: String,
  icon: ImageVector,
  palette: GradientPalette,
  floralVariant: FloralVariant,
  useCommaSeparator: Boolean,
  onToggleCommaSeparator: (Boolean) -> Unit,
  onSave: (String) -> Unit,
  onDismiss: () -> Unit,
  testTagInput: String,
  testTagConfirm: String,
) {
  var rawInput by remember { mutableStateOf("") }
  val parsed =
    remember(rawInput, useCommaSeparator) {
      TaskInputParser.parseEntries(rawInput, useCommaSeparator)
    }
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
            variant = floralVariant,
            motifColor = Color.White,
            modifier = Modifier.matchParentSize(),
          )
          Box(
            modifier =
              Modifier.matchParentSize()
                .background(
                  Brush.horizontalGradient(
                    colors =
                      listOf(
                        Color(0xB30B1020),
                        Color(0x730B1020),
                        Color(0x3D0B1020),
                      )
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
              border = BorderStroke(1.dp, Color.White.copy(alpha = 0.38f)),
              modifier = Modifier.size(42.dp),
            ) {
              Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                  imageVector = icon,
                  contentDescription = null,
                  tint = Color.White,
                  modifier = Modifier.size(22.dp),
                )
              }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
              Text(
                text = title,
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
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.92f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
        }

        Column(
          modifier =
            Modifier.fillMaxWidth()
              .verticalScroll(rememberScrollState())
              .padding(horizontal = 18.dp, vertical = 16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          OutlinedTextField(
            value = rawInput,
            onValueChange = { rawInput = it },
            label = { Text(fieldLabel) },
            placeholder = { Text(placeholder) },
            minLines = 2,
            maxLines = 5,
            colors =
              OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accent,
                focusedLabelColor = accent,
              ),
            modifier = Modifier.fillMaxWidth().testTag(testTagInput),
          )

          Surface(
            shape = RoundedCornerShape(14.dp),
            color = accent.copy(alpha = if (isDark) 0.18f else 0.10f),
            border = BorderStroke(1.dp, accent.copy(alpha = 0.40f)),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(
                  text = "Comma Separator",
                  style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                )
                Text(
                  text =
                    if (useCommaSeparator) "Splits commas into separate entries"
                    else "Creates one single entry",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              Switch(
                checked = useCommaSeparator,
                onCheckedChange = onToggleCommaSeparator,
              )
            }
          }

          if (parsed.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Text(
                text = "Preview (${parsed.size})",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                parsed.take(8).forEachIndexed { idx, entry ->
                  TintedPillTag(
                    text = entry,
                    accentColor = palette.colors[idx % palette.colors.size],
                  )
                }
                if (parsed.size > 8) {
                  TintedPillTag(
                    text = "+${parsed.size - 8} more",
                    accentColor = accent,
                  )
                }
              }
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            TextButton(onClick = onDismiss) { Text("Cancel") }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
              onClick = { onSave(rawInput) },
              enabled = parsed.isNotEmpty(),
              colors =
                ButtonDefaults.buttonColors(
                  containerColor = accent,
                  contentColor = if (accent.luminance() > 0.55f) Color(0xFF0F172A) else Color.White,
                ),
              modifier = Modifier.minimumInteractiveComponentSize().testTag(testTagConfirm),
            ) {
              Text(
                text = if (parsed.size > 1) "Add (${parsed.size})" else "Add",
                fontWeight = FontWeight.Bold,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun VibrantRenameDialog(
  title: String,
  subtitle: String,
  initialValue: String,
  fieldLabel: String,
  icon: ImageVector,
  palette: GradientPalette,
  floralVariant: FloralVariant,
  onSave: (String) -> Unit,
  onDismiss: () -> Unit,
) {
  var value by remember(initialValue) { mutableStateOf(initialValue) }
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
            variant = floralVariant,
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
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = Color(0x470B1020),
              border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
              modifier = Modifier.size(38.dp),
            ) {
              Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
              }
            }
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
              )
              Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.90f),
              )
            }
          }
        }

        Column(
          modifier = Modifier.fillMaxWidth().padding(18.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
          OutlinedTextField(
            value = value,
            onValueChange = { value = it },
            label = { Text(fieldLabel) },
            singleLine = true,
            colors =
              OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accent,
                focusedLabelColor = accent,
              ),
            modifier = Modifier.fillMaxWidth(),
          )
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            TextButton(onClick = onDismiss) { Text("Cancel") }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
              onClick = { onSave(value) },
              enabled = value.isNotBlank(),
              colors =
                ButtonDefaults.buttonColors(
                  containerColor = accent,
                  contentColor = if (accent.luminance() > 0.55f) Color(0xFF0F172A) else Color.White,
                ),
            ) {
              Text("Save", fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }
  }
}

private fun buildHighlightedText(
  text: String,
  needle: String,
  highlightContainer: Color,
  highlightContent: Color,
): AnnotatedString {
  if (needle.isBlank()) return AnnotatedString(text)
  val lowerText = text.lowercase()
  val lowerNeedle = needle.lowercase()
  return buildAnnotatedString {
    append(text)
    var index = lowerText.indexOf(lowerNeedle)
    while (index >= 0) {
      addStyle(
        style =
          SpanStyle(
            background = highlightContainer,
            color = highlightContent,
            fontWeight = FontWeight.Bold,
          ),
        start = index,
        end = (index + lowerNeedle.length).coerceAtMost(text.length),
      )
      index = lowerText.indexOf(lowerNeedle, index + lowerNeedle.length)
    }
  }
}
