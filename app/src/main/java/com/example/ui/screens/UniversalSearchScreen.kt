package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.logic.DateUtils
import com.example.models.AttendanceStatus
import com.example.models.UniversalSearchModule
import com.example.models.UniversalSearchQuery
import com.example.models.UniversalSearchResult
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.GradientAccentStrip
import com.example.ui.components.ReferenceDetailCard
import com.example.ui.components.ShiftFilterChip
import com.example.ui.components.ShiftHighlightBadge
import com.example.ui.components.StatusBadge
import com.example.ui.components.StatusFilterChip
import com.example.ui.components.TintedPillTag
import com.example.ui.components.VibrantFeatureBanner
import com.example.ui.components.rememberSyncedText
import com.example.ui.state.UniversalSearchUiState
import com.example.ui.theme.FloralVariant
import com.example.ui.theme.GradientPaletteRegistry
import com.example.ui.theme.LocalStatusPalette
import com.example.ui.theme.MonoLabelStyle

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun UniversalSearchScreen(
  state: UniversalSearchUiState,
  onBack: () -> Unit,
  onUpdateQuery: ((UniversalSearchQuery) -> UniversalSearchQuery) -> Unit,
  onResetFilters: () -> Unit,
  onOpenResult: (UniversalSearchResult) -> Unit,
  modifier: Modifier = Modifier,
) {
  var showFiltersPanel by remember { mutableStateOf(true) }
  var pickingStartDate by remember { mutableStateOf(false) }
  var pickingEndDate by remember { mutableStateOf(false) }

  val searchPalette = GradientPaletteRegistry.paletteForCard("universal_search", 2)
  val q = state.query
  val today = DateUtils.todayIso()

  if (pickingStartDate || pickingEndDate) {
    val isStart = pickingStartDate
    val initialIso = (if (isStart) q.startDate else q.endDate) ?: today
    val initialUtcMillis =
      DateUtils.isoToUtcMillis(initialIso) ?: DateUtils.isoToUtcMillis(today)
    val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialUtcMillis)

    DatePickerDialog(
      onDismissRequest = {
        pickingStartDate = false
        pickingEndDate = false
      },
      confirmButton = {
        TextButton(
          onClick = {
            val selectedIso = pickerState.selectedDateMillis?.let { DateUtils.utcMillisToIso(it) }
            if (selectedIso != null) {
              onUpdateQuery { curr ->
                val rawStart = if (isStart) selectedIso else curr.startDate
                val rawEnd = if (isStart) curr.endDate else selectedIso
                val (normStart, normEnd) = DateUtils.normalizeDateRange(rawStart, rawEnd)
                curr.copy(startDate = normStart, endDate = normEnd)
              }
            }
            pickingStartDate = false
            pickingEndDate = false
          },
          modifier = Modifier.testTag("universal_date_picker_confirm"),
        ) {
          Text("Apply")
        }
      },
      dismissButton = {
        Row {
          TextButton(
            onClick = {
              onUpdateQuery { curr ->
                if (isStart) curr.copy(startDate = null) else curr.copy(endDate = null)
              }
              pickingStartDate = false
              pickingEndDate = false
            },
            modifier = Modifier.testTag("universal_date_picker_clear"),
          ) {
            Text("Clear")
          }
          TextButton(
            onClick = {
              pickingStartDate = false
              pickingEndDate = false
            }
          ) {
            Text("Cancel")
          }
        }
      },
    ) {
      DatePicker(
        state = pickerState,
        title = {
          Text(
            text = if (isStart) "Select From Date" else "Select To Date",
            modifier = Modifier.padding(start = 24.dp, top = 16.dp),
          )
        },
      )
    }
  }

  val dutiesForSelectedDept =
    remember(state.duties, q.departmentId) {
      if (q.departmentId == null) state.duties
      else state.duties.filter { it.departmentId == q.departmentId }
    }

  val ictSubCategoriesForSelectedCat =
    remember(state.ictSubCategories, q.ictCategoryId) {
      if (q.ictCategoryId == null) state.ictSubCategories
      else state.ictSubCategories.filter { it.categoryId == q.ictCategoryId }
    }

  Column(
    modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
  ) {
    Surface(
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 1.dp,
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(modifier = Modifier.fillMaxWidth()) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          IconButton(
            onClick = onBack,
            modifier =
              Modifier.minimumInteractiveComponentSize().testTag("universal_search_back_button"),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back to Dashboard",
            )
          }
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Universal Search",
              style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
            )
            Text(
              text = "${state.results.size} matching record(s) across all modules",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          IconButton(
            onClick = { showFiltersPanel = !showFiltersPanel },
            modifier =
              Modifier.minimumInteractiveComponentSize().testTag("universal_search_toggle_filters"),
          ) {
            Icon(
              imageVector = Icons.Outlined.FilterList,
              contentDescription = if (showFiltersPanel) "Hide Filters" else "Show Filters",
              tint =
                if (showFiltersPanel) searchPalette.colors.first()
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
        GradientAccentStrip(palette = searchPalette, height = 3.dp)
      }
    }

    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
      LazyColumn(
        modifier = Modifier.fillMaxSize().widthIn(max = 880.dp),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        item {
          VibrantFeatureBanner(
            title = "Search All Workspace Data",
            subtitle =
              "Attendance, Roster, Staff, Shifts/Duties, ICT, Office Index, Tasks & Notes",
            palette = searchPalette,
            floralVariant = FloralVariant.RADIAL_PETALS,
            icon = Icons.Outlined.Search,
            badgeText = "${state.results.size} HITS",
          )
        }

        item {
          val (syncedSearchText, onSearchTextEdit) =
            rememberSyncedText(
              externalValue = q.searchText,
              onValueChange = { txt -> onUpdateQuery { it.copy(searchText = txt) } },
            )
          OutlinedTextField(
            value = syncedSearchText,
            onValueChange = onSearchTextEdit,
            label = { Text("Search name, shift, duty, tasks, notes, or values") },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            trailingIcon = {
              if (syncedSearchText.isNotBlank()) {
                IconButton(onClick = { onSearchTextEdit("") }) {
                  Icon(Icons.Outlined.Close, contentDescription = "Clear search")
                }
              }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("universal_search_input"),
          )
        }

        item {
          Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            UniversalSearchModule.entries.forEach { mod ->
              FilterChip(
                selected = q.module == mod,
                onClick = { onUpdateQuery { it.copy(module = mod) } },
                label = { Text(mod.label) },
                modifier = Modifier.testTag("universal_module_${mod.name.lowercase()}"),
              )
            }
          }
        }

        if (showFiltersPanel) {
          item {
            Card(
              shape = RoundedCornerShape(16.dp),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
              border = BorderStroke(1.dp, searchPalette.colors.first().copy(alpha = 0.40f)),
              modifier = Modifier.fillMaxWidth().testTag("universal_filters_card"),
            ) {
              Column(modifier = Modifier.fillMaxWidth()) {
                GradientAccentStrip(palette = searchPalette, height = 3.dp)
                Column(
                  modifier = Modifier.padding(14.dp),
                  verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Text(
                      text = "Date Range & Filters (${q.activeFilterCount} active)",
                      style =
                        MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    )
                    TextButton(
                      onClick = onResetFilters,
                      modifier = Modifier.testTag("universal_search_reset"),
                    ) {
                      Icon(
                        imageVector = Icons.Outlined.RestartAlt,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                      )
                      Spacer(modifier = Modifier.width(4.dp))
                      Text("Reset All")
                    }
                  }

                  // Date range presets
                  Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                  ) {
                    FilterChip(
                      selected = !q.useDateRange,
                      onClick = {
                        onUpdateQuery { it.copy(startDate = null, endDate = null) }
                      },
                      label = { Text("All Dates") },
                      modifier = Modifier.testTag("universal_date_all"),
                    )
                    FilterChip(
                      selected = q.startDate == today && q.endDate == today,
                      onClick = {
                        onUpdateQuery { it.copy(startDate = today, endDate = today) }
                      },
                      label = { Text("Today") },
                    )
                    FilterChip(
                      selected =
                        q.startDate == DateUtils.addDays(today, -6) && q.endDate == today,
                      onClick = {
                        onUpdateQuery {
                          it.copy(startDate = DateUtils.addDays(today, -6), endDate = today)
                        }
                      },
                      label = { Text("7 Days") },
                    )
                    FilterChip(
                      selected =
                        q.startDate == DateUtils.addDays(today, -30) && q.endDate == today,
                      onClick = {
                        onUpdateQuery {
                          it.copy(startDate = DateUtils.addDays(today, -30), endDate = today)
                        }
                      },
                      label = { Text("30 Days") },
                    )
                    FilterChip(
                      selected =
                        q.startDate == DateUtils.addDays(today, -365) && q.endDate == today,
                      onClick = {
                        onUpdateQuery {
                          it.copy(startDate = DateUtils.addDays(today, -365), endDate = today)
                        }
                      },
                      label = { Text("1 Year") },
                    )
                  }

                  // Calendar pickers & steppers (always visible; tap date box for calendar dialog)
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                  ) {
                    DateBoundPickerTile(
                      label = "From",
                      dateIso = q.startDate,
                      onPickCalendar = { pickingStartDate = true },
                      onClear = { onUpdateQuery { it.copy(startDate = null) } },
                      onStep = { delta ->
                        onUpdateQuery { curr ->
                          val base = curr.startDate ?: today
                          val nextStart = DateUtils.addDays(base, delta)
                          curr.copy(
                            startDate = nextStart,
                            endDate =
                              if (curr.endDate != null && nextStart > curr.endDate) nextStart
                              else curr.endDate,
                          )
                        }
                      },
                      modifier = Modifier.weight(1f).testTag("universal_from_date_picker"),
                    )
                    DateBoundPickerTile(
                      label = "To",
                      dateIso = q.endDate,
                      onPickCalendar = { pickingEndDate = true },
                      onClear = { onUpdateQuery { it.copy(endDate = null) } },
                      onStep = { delta ->
                        onUpdateQuery { curr ->
                          val base = curr.endDate ?: today
                          val nextEnd = DateUtils.addDays(base, delta)
                          curr.copy(
                            endDate = nextEnd,
                            startDate =
                              if (curr.startDate != null && nextEnd < curr.startDate) nextEnd
                              else curr.startDate,
                          )
                        }
                      },
                      modifier = Modifier.weight(1f).testTag("universal_to_date_picker"),
                    )
                  }

                  // Employee & Designation dropdowns
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                  ) {
                    SearchFilterDropdown(
                      label = "Employee",
                      selectedLabel =
                        state.employees.firstOrNull { it.id == q.employeeId }?.let {
                          "${it.name} (${it.code})"
                        } ?: "All Employees",
                      options =
                        listOf(null to "All Employees") +
                          state.employees.map { it.id to "${it.name} (${it.code})" },
                      onSelect = { empId -> onUpdateQuery { it.copy(employeeId = empId) } },
                      modifier = Modifier.weight(1f),
                    )
                    SearchFilterDropdown(
                      label = "Designation",
                      selectedLabel = q.designation ?: "All Designations",
                      options =
                        listOf(null to "All Designations") +
                          state.facets.designations.map { it to it },
                      onSelect = { desig -> onUpdateQuery { it.copy(designation = desig) } },
                      modifier = Modifier.weight(1f),
                    )
                  }

                  // Department & Duty dropdowns
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                  ) {
                    SearchFilterDropdown(
                      label = "Department",
                      selectedLabel =
                        state.departments.firstOrNull { it.id == q.departmentId }?.name
                          ?: "All Departments",
                      options =
                        listOf(null to "All Departments") +
                          state.departments.map { it.id to it.name },
                      onSelect = { deptId ->
                        onUpdateQuery { curr ->
                          val clearDuty =
                            curr.dutyTypeId != null &&
                              deptId != null &&
                              state.duties.firstOrNull { it.id == curr.dutyTypeId }?.departmentId !=
                                deptId
                          curr.copy(
                            departmentId = deptId,
                            dutyTypeId = if (clearDuty) null else curr.dutyTypeId,
                          )
                        }
                      },
                      modifier = Modifier.weight(1f),
                    )
                    SearchFilterDropdown(
                      label = "Duty",
                      selectedLabel =
                        state.duties.firstOrNull { it.id == q.dutyTypeId }?.title ?: "All Duties",
                      options =
                        listOf(null to "All Duties") +
                          dutiesForSelectedDept.map { it.id to it.title },
                      onSelect = { dutyId -> onUpdateQuery { it.copy(dutyTypeId = dutyId) } },
                      modifier = Modifier.weight(1f),
                    )
                  }

                  // Shift chips + Night Shift dedicated highlight
                  Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Shift", style = MaterialTheme.typography.labelMedium)
                    FlowRow(
                      horizontalArrangement = Arrangement.spacedBy(8.dp),
                      verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                      FilterChip(
                        selected = q.shiftId == null && !q.nightShiftOnly,
                        onClick = {
                          onUpdateQuery { it.copy(shiftId = null, nightShiftOnly = false) }
                        },
                        label = { Text("All Shifts") },
                      )
                      ShiftFilterChip(
                        label = "Night Shift Only",
                        isNightShift = true,
                        selected = q.nightShiftOnly,
                        onClick = {
                          onUpdateQuery { curr ->
                            curr.copy(
                              nightShiftOnly = !curr.nightShiftOnly,
                              shiftId = if (!curr.nightShiftOnly) null else curr.shiftId,
                            )
                          }
                        },
                        modifier = Modifier.testTag("universal_night_shift_only"),
                      )
                      state.shifts.forEach { shift ->
                        ShiftFilterChip(
                          label = shift.name,
                          isNightShift = shift.isNightShiftVisual(),
                          selected = q.shiftId == shift.id,
                          onClick = {
                            onUpdateQuery { curr ->
                              val nextId = if (curr.shiftId == shift.id) null else shift.id
                              curr.copy(
                                shiftId = nextId,
                                nightShiftOnly = if (nextId != null) false else curr.nightShiftOnly,
                              )
                            }
                          },
                        )
                      }
                    }
                  }

                  // Attendance Status chips
                  Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Attendance Status", style = MaterialTheme.typography.labelMedium)
                    FlowRow(
                      horizontalArrangement = Arrangement.spacedBy(8.dp),
                      verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                      FilterChip(
                        selected = q.status == null,
                        onClick = { onUpdateQuery { it.copy(status = null) } },
                        label = { Text("All") },
                      )
                      AttendanceStatus.entries.forEach { status ->
                        StatusFilterChip(
                          status = status,
                          selected = q.status == status,
                          onClick = {
                            onUpdateQuery { curr ->
                              curr.copy(status = if (curr.status == status) null else status)
                            }
                          },
                        )
                      }
                    }
                  }

                  // ICT Category & Sub-Category dropdowns
                  if (state.ictCategories.isNotEmpty() || state.ictSubCategories.isNotEmpty()) {
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                      SearchFilterDropdown(
                        label = "ICT Category",
                        selectedLabel =
                          state.ictCategories.firstOrNull { it.id == q.ictCategoryId }?.name
                            ?: "All ICT Categories",
                        options =
                          listOf(null to "All ICT Categories") +
                            state.ictCategories.map { it.id to it.name },
                        onSelect = { catId ->
                          onUpdateQuery {
                            it.copy(ictCategoryId = catId, ictSubCategoryId = null)
                          }
                        },
                        modifier = Modifier.weight(1f),
                      )
                      SearchFilterDropdown(
                        label = "ICT Sub-Category",
                        selectedLabel =
                          state.ictSubCategories.firstOrNull { it.id == q.ictSubCategoryId }?.name
                            ?: "All Sub-Categories",
                        options =
                          listOf(null to "All Sub-Categories") +
                            ictSubCategoriesForSelectedCat.map { it.id to it.name },
                        onSelect = { subId ->
                          onUpdateQuery { it.copy(ictSubCategoryId = subId) }
                        },
                        modifier = Modifier.weight(1f),
                      )
                    }
                  }

                  // Office Index Category & Sub-Category dropdowns
                  if (
                    state.facets.officeCategories.isNotEmpty() ||
                      state.facets.officeSubCategories.isNotEmpty()
                  ) {
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                      SearchFilterDropdown(
                        label = "Office Category",
                        selectedLabel = q.officeCategory ?: "All Office Categories",
                        options =
                          listOf(null to "All Office Categories") +
                            state.facets.officeCategories.map { it to it },
                        onSelect = { cat ->
                          onUpdateQuery {
                            it.copy(officeCategory = cat, officeSubCategory = null)
                          }
                        },
                        modifier = Modifier.weight(1f),
                      )
                      SearchFilterDropdown(
                        label = "Office Sub-Cat",
                        selectedLabel = q.officeSubCategory ?: "All Office Sub-Cats",
                        options =
                          listOf(null to "All Office Sub-Cats") +
                            state.facets.officeSubCategories.map { it to it },
                        onSelect = { sub -> onUpdateQuery { it.copy(officeSubCategory = sub) } },
                        modifier = Modifier.weight(1f),
                      )
                    }
                  }

                  // Task Manager filters
                  if (
                    state.taskCategories.isNotEmpty() ||
                      q.module == UniversalSearchModule.TASKS ||
                      q.taskCategoryId != null ||
                      q.taskCompletedFilter != null
                  ) {
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.spacedBy(8.dp),
                      verticalAlignment = Alignment.CenterVertically,
                    ) {
                      SearchFilterDropdown(
                        label = "Task Category",
                        selectedLabel =
                          state.taskCategories.firstOrNull { it.id == q.taskCategoryId }?.name
                            ?: "All Task Categories",
                        options =
                          listOf(null to "All Task Categories") +
                            state.taskCategories.map { it.id to it.name },
                        onSelect = { catId ->
                          onUpdateQuery { it.copy(taskCategoryId = catId) }
                        },
                        modifier = Modifier.weight(1f),
                      )
                      FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f),
                      ) {
                        FilterChip(
                          selected = q.taskCompletedFilter == null,
                          onClick = { onUpdateQuery { it.copy(taskCompletedFilter = null) } },
                          label = { Text("All") },
                        )
                        FilterChip(
                          selected = q.taskCompletedFilter == false,
                          onClick = {
                            onUpdateQuery {
                              it.copy(
                                taskCompletedFilter =
                                  if (it.taskCompletedFilter == false) null else false
                              )
                            }
                          },
                          label = { Text("Pending") },
                        )
                        FilterChip(
                          selected = q.taskCompletedFilter == true,
                          onClick = {
                            onUpdateQuery {
                              it.copy(
                                taskCompletedFilter =
                                  if (it.taskCompletedFilter == true) null else true
                              )
                            }
                          },
                          label = { Text("Done") },
                        )
                      }
                    }
                  }
                }
              }
            }
          }
        }

        if (state.results.isEmpty()) {
          item {
            EmptyStateCard(
              title = "No matching records",
              message =
                "Try expanding the date range (or tapping All Dates), switching module filter to All, or clearing active filters.",
              icon = Icons.Outlined.Search,
            )
          }
        } else {
          itemsIndexed(state.results, key = { _, item -> item.id }) { idx, result ->
            UniversalSearchResultCard(
              result = result,
              paletteIndex = idx,
              onClick = { onOpenResult(result) },
            )
          }
        }
      }
    }
  }
}

@Composable
private fun DateBoundPickerTile(
  label: String,
  dateIso: String?,
  onPickCalendar: () -> Unit,
  onClear: () -> Unit,
  onStep: (Int) -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(12.dp),
    color = MaterialTheme.colorScheme.surfaceContainerLow,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      IconButton(onClick = { onStep(-1) }, modifier = Modifier.size(34.dp)) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
          contentDescription = "Previous $label day",
        )
      }
      Column(
        modifier =
          Modifier.weight(1f)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onPickCalendar)
            .padding(horizontal = 4.dp, vertical = 4.dp),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Icon(
            imageVector = Icons.Outlined.CalendarToday,
            contentDescription = "Pick $label date",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(12.dp),
          )
          Text(
            text = label,
            style = MonoLabelStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Text(
          text = dateIso ?: "Any",
          style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
          color =
            if (dateIso != null) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
      if (dateIso != null) {
        IconButton(onClick = onClear, modifier = Modifier.size(28.dp)) {
          Icon(
            imageVector = Icons.Outlined.Close,
            contentDescription = "Clear $label date",
            modifier = Modifier.size(15.dp),
          )
        }
      }
      IconButton(onClick = { onStep(1) }, modifier = Modifier.size(34.dp)) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
          contentDescription = "Next $label day",
        )
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchFilterDropdown(
  label: String,
  selectedLabel: String,
  options: List<Pair<String?, String>>,
  onSelect: (String?) -> Unit,
  modifier: Modifier = Modifier,
) {
  var expanded by remember { mutableStateOf(false) }
  ExposedDropdownMenuBox(
    expanded = expanded,
    onExpandedChange = { expanded = !expanded },
    modifier = modifier,
  ) {
    OutlinedTextField(
      value = selectedLabel,
      onValueChange = {},
      readOnly = true,
      label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
      trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
      singleLine = true,
      modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
    )
    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
      options.forEach { (id, display) ->
        DropdownMenuItem(
          text = { Text(display) },
          onClick = {
            onSelect(id)
            expanded = false
          },
        )
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun UniversalSearchResultCard(
  result: UniversalSearchResult,
  paletteIndex: Int,
  onClick: () -> Unit,
) {
  val statusPalette = LocalStatusPalette.current
  val cardPalette = GradientPaletteRegistry.paletteForCard(result.module.name, paletteIndex)
  val accentColor = cardPalette.colors.first()

  ReferenceDetailCard(
    accentColor = accentColor,
    isNightShift = result.isNightShift,
    modifier =
      Modifier.clickable(onClick = onClick).testTag("universal_result_${result.id}"),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.Top,
    ) {
      Column(
        modifier = Modifier.weight(1f).padding(end = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
      ) {
        Row(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          TintedPillTag(text = result.module.label, accentColor = accentColor)
          Text(
            text = DateUtils.formatReadableDate(result.date),
            style = MaterialTheme.typography.labelMedium,
            color =
              if (result.isNightShift) statusPalette.nightShift.text
              else MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Text(
          text = result.title,
          style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
          color =
            if (result.isNightShift) statusPalette.nightShift.text
            else MaterialTheme.colorScheme.onSurface,
        )
        Text(
          text = result.subtitle,
          style = MaterialTheme.typography.bodySmall,
          color =
            if (result.isNightShift) statusPalette.nightShift.text.copy(alpha = 0.88f)
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      if (result.attendanceStatus != null) {
        StatusBadge(status = result.attendanceStatus)
      } else if (result.valueOrBadge.isNotBlank()) {
        TintedPillTag(text = result.valueOrBadge, accentColor = accentColor)
      }
    }

    FlowRow(
      horizontalArrangement = Arrangement.spacedBy(6.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      if (result.isNightShift) {
        ShiftHighlightBadge(
          shiftName = "Night Shift",
          timeWindow = null,
          isNightShift = true,
        )
      }
      result.tags.forEach { tag ->
        TintedPillTag(text = tag, accentColor = accentColor)
      }
      if (result.isDeletedReference) {
        TintedPillTag(text = "Archived ref", accentColor = MaterialTheme.colorScheme.outline)
      }
    }

    if (result.detailLine.isNotBlank()) {
      Text(
        text = result.detailLine,
        style = MaterialTheme.typography.bodySmall,
        color =
          if (result.isNightShift) statusPalette.nightShift.text.copy(alpha = 0.90f)
          else MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}
