package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.FactCheck
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material.icons.outlined.FilterListOff
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.logic.DateUtils
import com.example.models.AttendanceDisplayMode
import com.example.models.AttendanceStatus
import com.example.models.DailyAttendanceRow
import com.example.models.EmployeeScheduleGroup
import com.example.models.HistoryFilterQuery
import com.example.models.HistorySearchEntry
import com.example.models.RosterViewMode
import com.example.models.UpcomingCoverageDay
import com.example.ui.components.AttendanceRemarksDialog
import com.example.ui.components.DateStepperBar
import com.example.ui.components.DetailMetaPair
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.FloralTextureOverlay
import com.example.ui.components.GradientAccentStrip
import com.example.ui.components.ReferenceDetailCard
import com.example.ui.components.ShiftFilterChip
import com.example.ui.components.ShiftHighlightBadge
import com.example.ui.components.SolidPillTag
import com.example.ui.components.StatusBadge
import com.example.ui.components.StatusFilterChip
import com.example.ui.components.TintedPillTag
import com.example.ui.components.VibrantFeatureBanner
import com.example.ui.components.rememberSyncedText
import com.example.ui.state.RegisterSubTab
import com.example.ui.state.RegisterUiState
import com.example.ui.theme.FloralVariant
import com.example.ui.theme.GradientPaletteRegistry
import com.example.ui.theme.LocalStatusPalette
import com.example.ui.theme.MonoLabelStyle

@Composable
fun RegisterScreen(
  state: RegisterUiState,
  onSelectSubTab: (RegisterSubTab) -> Unit,
  onSelectAttendanceDisplayMode: (AttendanceDisplayMode) -> Unit,
  onSelectAttendanceDate: (String) -> Unit,
  onStepCalendarMonth: (Int) -> Unit,
  onPreviousDay: () -> Unit,
  onNextDay: () -> Unit,
  onJumpToday: () -> Unit,
  onSelectShiftFilter: (String?) -> Unit,
  onMarkAttendance: (DailyAttendanceRow, AttendanceStatus, String) -> Unit,
  onBulkMarkPresent: (Boolean) -> Unit,
  onSelectRosterViewMode: (RosterViewMode) -> Unit,
  onOpenAssignSheet: (String, Set<String>) -> Unit,
  onRemoveAssignment: (String) -> Unit,
  onUpdateHistoryQuery: ((HistoryFilterQuery) -> HistoryFilterQuery) -> Unit,
  onResetHistoryFilters: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var editingRemarksRow by remember { mutableStateOf<DailyAttendanceRow?>(null) }

  editingRemarksRow?.let { row ->
    AttendanceRemarksDialog(
      row = row,
      date = state.selectedDate,
      onSave = { status, remarks ->
        onMarkAttendance(row, status, remarks)
        editingRemarksRow = null
      },
      onDismiss = { editingRemarksRow = null },
    )
  }

  Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    Column(
      modifier = Modifier.fillMaxSize().widthIn(max = 860.dp),
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        when (state.subTab) {
          RegisterSubTab.ATTENDANCE ->
            VibrantFeatureBanner(
              title = "Daily Attendance",
              subtitle =
                "${state.summary.presentCount} present · ${state.summary.lateCount} late · ${state.summary.unmarkedSlotsCount} unmarked",
              palette = GradientPaletteRegistry.paletteForCard("register_attendance", 0),
              floralVariant = GradientPaletteRegistry.floralVariantForCard("register_attendance", 0),
              icon = Icons.AutoMirrored.Outlined.FactCheck,
              badgeText = "${state.attendanceRows.size} SLOTS",
            )
          RegisterSubTab.ROSTER ->
            VibrantFeatureBanner(
              title = "Roster & Shifts",
              subtitle = "Flexible daily & multi-day duty scheduling",
              palette = GradientPaletteRegistry.paletteForCard("register_roster", 3),
              floralVariant = GradientPaletteRegistry.floralVariantForCard("register_roster", 3),
              icon = Icons.Outlined.CalendarMonth,
              badgeText = "${state.activeEmployees.size} STAFF",
            )
          RegisterSubTab.HISTORY ->
            VibrantFeatureBanner(
              title = "History & Search",
              subtitle = "Inspect past shifts, duties & attendance logs",
              palette = GradientPaletteRegistry.paletteForCard("register_history", 6),
              floralVariant = GradientPaletteRegistry.floralVariantForCard("register_history", 6),
              icon = Icons.Outlined.History,
              badgeText = "${state.historyEntries.size} LOGS",
            )
        }

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
          RegisterSubTab.entries.forEachIndexed { idx, tab ->
            SegmentedButton(
              selected = state.subTab == tab,
              onClick = { onSelectSubTab(tab) },
              shape = SegmentedButtonDefaults.itemShape(idx, RegisterSubTab.entries.size),
              modifier = Modifier.testTag("register_subtab_${tab.name.lowercase()}"),
            ) {
              Text(tab.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
          }
        }

        if (
          state.subTab == RegisterSubTab.ROSTER ||
            (state.subTab == RegisterSubTab.ATTENDANCE &&
              state.attendanceDisplayMode == AttendanceDisplayMode.DAILY_LIST)
        ) {
          DateStepperBar(
            selectedDate = state.selectedDate,
            onPreviousDay = onPreviousDay,
            onNextDay = onNextDay,
            onJumpToday = onJumpToday,
          )
        }
      }

      when (state.subTab) {
        RegisterSubTab.ATTENDANCE ->
          DailyAttendanceContent(
            state = state,
            onSelectDisplayMode = onSelectAttendanceDisplayMode,
            onSelectDate = onSelectAttendanceDate,
            onStepCalendarMonth = onStepCalendarMonth,
            onJumpToday = onJumpToday,
            onSelectShiftFilter = onSelectShiftFilter,
            onMarkStatus = { row, status ->
              onMarkAttendance(row, status, row.existingRecord?.remarks ?: "")
            },
            onEditRemarks = { row -> editingRemarksRow = row },
            onBulkMarkPresent = onBulkMarkPresent,
          )
        RegisterSubTab.ROSTER ->
          RosterAndAssignContent(
            state = state,
            onSelectRosterViewMode = onSelectRosterViewMode,
            onOpenAssignSheet = onOpenAssignSheet,
            onRemoveAssignment = onRemoveAssignment,
          )
        RegisterSubTab.HISTORY ->
          HistorySearchContent(
            state = state,
            onUpdateHistoryQuery = onUpdateHistoryQuery,
            onResetHistoryFilters = onResetHistoryFilters,
          )
      }
    }
  }
}

@Composable
private fun DailyAttendanceContent(
  state: RegisterUiState,
  onSelectDisplayMode: (AttendanceDisplayMode) -> Unit,
  onSelectDate: (String) -> Unit,
  onStepCalendarMonth: (Int) -> Unit,
  onJumpToday: () -> Unit,
  onSelectShiftFilter: (String?) -> Unit,
  onMarkStatus: (DailyAttendanceRow, AttendanceStatus) -> Unit,
  onEditRemarks: (DailyAttendanceRow) -> Unit,
  onBulkMarkPresent: (Boolean) -> Unit,
) {
  val palette = LocalStatusPalette.current
  val unmarkedCount = state.attendanceRows.count { it.employee.isActive && it.existingRecord == null }
  val scheduledUnmarked =
    state.attendanceRows.count {
      it.employee.isActive && it.existingRecord == null && it.isScheduledForDate
    }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    // View Switcher: Daily Register vs Calendar View
    item {
      SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        AttendanceDisplayMode.entries.forEachIndexed { idx, mode ->
          SegmentedButton(
            selected = state.attendanceDisplayMode == mode,
            onClick = { onSelectDisplayMode(mode) },
            shape = SegmentedButtonDefaults.itemShape(idx, AttendanceDisplayMode.entries.size),
            modifier = Modifier.testTag("attendance_mode_${mode.name.lowercase()}"),
          ) {
            Text(mode.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
          }
        }
      }
    }

    if (state.attendanceDisplayMode == AttendanceDisplayMode.CALENDAR) {
      item {
        AttendanceCalendarGridCard(
          year = state.calendarYear,
          month1Based = state.calendarMonth,
          selectedDate = state.selectedDate,
          cells = state.calendarCells,
          onStepMonth = onStepCalendarMonth,
          onJumpToday = onJumpToday,
          onSelectDate = onSelectDate,
          onOpenDailyRegister = { onSelectDisplayMode(AttendanceDisplayMode.DAILY_LIST) },
        )
      }

      item {
        AttendanceGistCounterCard(summary = state.summary)
      }

      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "Shift, Dept & Duty Breakdown · ${DateUtils.formatReadableDate(state.selectedDate)}",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.weight(1f),
          )
          TintedPillTag(
            text = "${state.attendanceRows.size} slot(s)",
            accentColor = MaterialTheme.colorScheme.primary,
          )
        }
      }

      item {
        CalendarDateBreakdownSection(
          breakdown = state.calendarBreakdown,
          selectedDate = state.selectedDate,
          onEditRemarks = onEditRemarks,
        )
      }
    } else {
      // Shift Filter Chips (with Night Shift highlight)
      item {
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          FilterChip(
            selected = state.filterShiftId == null,
            onClick = { onSelectShiftFilter(null) },
            label = { Text("All Shifts") },
          )
          state.activeShifts.forEach { shift ->
            ShiftFilterChip(
              label = "${shift.name} (${shift.startTime})",
              isNightShift = shift.isNightShiftVisual(),
              selected = state.filterShiftId == shift.id,
              onClick = {
                onSelectShiftFilter(if (state.filterShiftId == shift.id) null else shift.id)
              },
            )
          }
        }
      }

      // Attendance Gist & Counter Summary Card
      item {
        AttendanceGistCounterCard(summary = state.summary)
      }

      // Quick Bulk Mark Bar
      if (unmarkedCount > 0) {
        item {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            FilledTonalButton(
              onClick = { onBulkMarkPresent(false) },
              modifier = Modifier.weight(1f).testTag("bulk_mark_present_button"),
            ) {
              Icon(
                imageVector = Icons.Outlined.DoneAll,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text("Mark Remaining Present ($unmarkedCount)")
            }
            if (scheduledUnmarked in 1 until unmarkedCount) {
              OutlinedButton(
                onClick = { onBulkMarkPresent(true) },
                modifier = Modifier.testTag("bulk_mark_scheduled_button"),
              ) {
                Text("Scheduled ($scheduledUnmarked)")
              }
            }
          }
        }
      }

      if (state.attendanceRows.isEmpty()) {
        item {
          EmptyStateCard(
            title =
              if (state.allEmployees.isEmpty()) "No team members registered yet"
              else "No matching shift slots",
            message =
              if (state.allEmployees.isEmpty())
                "Open Settings to register employees and shifts, then return here to mark daily attendance."
              else "No active or historical shift slots match the selected filter on this date.",
            icon = Icons.Outlined.EventBusy,
          )
        }
      } else {
        itemsIndexed(state.attendanceRows, key = { _, row -> row.rowKey }) { index, row ->
          AttendanceEmployeeCard(
            row = row,
            paletteIndex = index,
            onMarkStatus = { status -> onMarkStatus(row, status) },
            onEditRemarks = { onEditRemarks(row) },
          )
        }
      }
    }

    item { Spacer(modifier = Modifier.height(16.dp)) }
  }
}

@Composable
private fun AttendanceEmployeeCard(
  row: DailyAttendanceRow,
  paletteIndex: Int,
  onMarkStatus: (AttendanceStatus) -> Unit,
  onEditRemarks: () -> Unit,
) {
  val palette = LocalStatusPalette.current
  val currentStatus = row.existingRecord?.status
  val statusColors = palette.forStatus(currentStatus)
  val cardPalette = GradientPaletteRegistry.paletteForCard(row.employee.id, paletteIndex)
  val isNight = row.isNightShift
  val accentColor =
    if (currentStatus != null) statusColors.text else cardPalette.colors.first()

  ReferenceDetailCard(
    accentColor = accentColor,
    isNightShift = isNight,
    tintContainerColor = if (currentStatus != null) statusColors.container else null,
    modifier = Modifier.testTag("attendance_card_${row.employee.code}_${row.shiftId}"),
  ) {
    // Reference Top Row: Title on Left + Status Pill & Utility Icon on Right
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = "${row.employee.name} (${row.employee.code})",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = if (isNight) palette.nightShift.text else MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(1f).padding(end = 8.dp),
      )
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
      ) {
        StatusBadge(status = currentStatus)
        IconButton(
          onClick = onEditRemarks,
          modifier = Modifier.minimumInteractiveComponentSize(),
        ) {
          Icon(
            imageVector = Icons.Outlined.EditNote,
            contentDescription = "Edit remarks for ${row.employee.name}",
            tint =
              if (isNight) palette.nightShift.iconTint
              else MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }

    // Reference Metadata Lines Below
    val shiftLabel =
      row.shift?.name
        ?: row.existingRecord?.shiftNameSnapshot?.ifBlank { null }
        ?: if (row.shiftId.isBlank()) "Open Day" else row.shiftId
    val shiftWindow = row.shift?.let { "${it.startTime}–${it.endTime}" } ?: "Unscheduled"
    val dutyTitle =
      (row.duty?.title ?: row.existingRecord?.dutyTitleSnapshot.orEmpty()).ifBlank { "No Duty" }
    val labelTint =
      if (isNight) Color(0xFFC4B5FD) else MaterialTheme.colorScheme.onSurfaceVariant
    val valueTint =
      if (isNight) palette.nightShift.text else MaterialTheme.colorScheme.onSurface

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(18.dp),
    ) {
      DetailMetaPair(
        label = "Shift",
        value = shiftLabel,
        labelColor = labelTint,
        valueColor = valueTint,
        modifier = Modifier.weight(1f),
      )
      DetailMetaPair(
        label = "Window",
        value = shiftWindow,
        labelColor = labelTint,
        valueColor = valueTint,
      )
    }

    val deptName =
      (row.department?.name ?: row.existingRecord?.departmentNameSnapshot.orEmpty()).ifBlank { null }
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(18.dp),
    ) {
      DetailMetaPair(
        label = "Duty",
        value = if (deptName != null) "$deptName · $dutyTitle" else dutyTitle,
        labelColor = labelTint,
        valueColor = valueTint,
        modifier = Modifier.weight(1f),
      )
      DetailMetaPair(
        label = "Role",
        value = row.employee.designation,
        labelColor = labelTint,
        valueColor = valueTint,
      )
    }

    if (isNight) {
      ShiftHighlightBadge(
        shiftName = shiftLabel,
        timeWindow = row.shift?.let { "${it.startTime}–${it.endTime}" },
        isNightShift = true,
      )
    }

    val remarks = row.existingRecord?.remarks.orEmpty()
    if (remarks.isNotBlank()) {
      Text(
        text = "Remarks: $remarks",
        style = MaterialTheme.typography.bodySmall,
        color = if (isNight) Color(0xFFDDD6FE) else MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }

    // Quick-Mark Status Action Row
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(5.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      AttendanceStatus.entries.forEach { st ->
        val selected = currentStatus == st
        val colors = palette.forStatus(st)
        val chipBg =
          when {
            selected -> colors.container
            isNight -> Color(0x26FFFFFF)
            else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
          }
        val chipBorder =
          when {
            selected -> colors.text
            isNight -> palette.nightShift.border.copy(alpha = 0.55f)
            else -> colors.text.copy(alpha = 0.40f)
          }
        val chipLabel =
          when {
            selected -> colors.text
            isNight -> Color(0xFFE0E7FF)
            else -> colors.text
          }
        Surface(
          onClick = { onMarkStatus(st) },
          shape = RoundedCornerShape(10.dp),
          color = chipBg,
          border =
            BorderStroke(
              width = if (selected) 1.75.dp else 1.dp,
              color = chipBorder,
            ),
          modifier =
            Modifier.weight(1f)
              .minimumInteractiveComponentSize()
              .testTag("mark_${row.employee.code}_${row.shiftId}_${st.name.lowercase()}"),
        ) {
          Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 2.dp),
          ) {
            Text(
              text = st.label,
              style =
                MaterialTheme.typography.labelSmall.copy(
                  fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold
                ),
              color = chipLabel,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun RosterAndAssignContent(
  state: RegisterUiState,
  onSelectRosterViewMode: (RosterViewMode) -> Unit,
  onOpenAssignSheet: (String, Set<String>) -> Unit,
  onRemoveAssignment: (String) -> Unit,
) {
  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.weight(1f)) {
          RosterViewMode.entries.forEachIndexed { idx, mode ->
            SegmentedButton(
              selected = state.rosterViewMode == mode,
              onClick = { onSelectRosterViewMode(mode) },
              shape = SegmentedButtonDefaults.itemShape(idx, RosterViewMode.entries.size),
            ) {
              Text(mode.label)
            }
          }
        }
        Button(
          onClick = { onOpenAssignSheet(state.selectedDate, emptySet()) },
          modifier = Modifier.height(48.dp).testTag("roster_assign_button"),
        ) {
          Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Assign")
        }
      }
    }

    if (state.activeEmployees.isEmpty()) {
      item {
        EmptyStateCard(
          title = "No staff available for roster",
          message = "Add employees and shifts in Settings first, then assign daily or multi-day rosters here.",
          icon = Icons.Outlined.EventBusy,
        )
      }
    } else {
      when (state.rosterViewMode) {
        RosterViewMode.BY_DATE -> {
          itemsIndexed(state.upcomingCoverage, key = { _, day -> day.date }) { idx, day ->
            RosterByDateCard(
              day = day,
              paletteIndex = idx,
              onAssignDay = { onOpenAssignSheet(day.date, emptySet()) },
              onAssignUnassigned = {
                onOpenAssignSheet(day.date, day.unassignedEmployees.map { it.id }.toSet())
              },
              onRemoveAssignment = onRemoveAssignment,
            )
          }
        }
        RosterViewMode.BY_EMPLOYEE -> {
          itemsIndexed(state.employeeSchedules, key = { _, g -> g.employee.id }) { idx, group ->
            RosterByEmployeeCard(
              group = group,
              paletteIndex = idx,
              onAssignForEmployee = {
                onOpenAssignSheet(state.selectedDate, setOf(group.employee.id))
              },
              onRemoveAssignment = onRemoveAssignment,
            )
          }
        }
      }
    }

    item { Spacer(modifier = Modifier.height(16.dp)) }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RosterByDateCard(
  day: UpcomingCoverageDay,
  paletteIndex: Int,
  onAssignDay: () -> Unit,
  onAssignUnassigned: () -> Unit,
  onRemoveAssignment: (String) -> Unit,
) {
  val palette = LocalStatusPalette.current
  val dayPalette = GradientPaletteRegistry.paletteForCard(day.date, paletteIndex)
  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, dayPalette.colors.first().copy(alpha = 0.42f)),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(modifier = Modifier.fillMaxWidth()) {
      GradientAccentStrip(palette = dayPalette, height = 4.dp)
      Column(
        modifier = Modifier.padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
              text = "${DateUtils.formatShortDateWithDay(day.date)} · ${day.date}",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            )
            TintedPillTag(
              text = "${day.coveredEmployeesCount}/${day.totalActiveEmployees} staff · ${day.totalShiftSlotsCount} shift slot(s)",
              accentColor = dayPalette.colors.first(),
            )
          }
          Spacer(modifier = Modifier.width(8.dp))
          FilledTonalButton(onClick = onAssignDay) { Text("Add / Edit") }
        }

        if (day.entries.isEmpty()) {
          Text(
            text = "No assignments scheduled for this date.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        } else {
          day.entries.forEach { entry ->
            val isNight = entry.shift?.isNightShiftVisual() == true
            val titleColor =
              if (isNight) palette.nightShift.text else MaterialTheme.colorScheme.onSurface
            val labelColor =
              if (isNight) palette.nightShift.text.copy(alpha = 0.78f)
              else MaterialTheme.colorScheme.onSurfaceVariant
            ReferenceDetailCard(
              accentColor = dayPalette.colors[1],
              isNightShift = isNight,
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Row(
                  modifier = Modifier.weight(1f).padding(end = 8.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                  if (isNight) {
                    Icon(
                      imageVector = Icons.Outlined.NightsStay,
                      contentDescription = "Night Shift",
                      tint = palette.nightShift.iconTint,
                      modifier = Modifier.size(16.dp),
                    )
                  }
                  Text(
                    text = "${entry.employee.code} · ${entry.employee.name}",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = titleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                  SolidPillTag(
                    text = entry.shift?.name ?: "Shift",
                    containerColor =
                      if (isNight) palette.nightShift.border else dayPalette.colors[1],
                  )
                  IconButton(
                    onClick = { onRemoveAssignment(entry.assignment.id) },
                    modifier = Modifier.minimumInteractiveComponentSize(),
                  ) {
                    Icon(
                      imageVector = Icons.Outlined.DeleteOutline,
                      contentDescription = "Remove assignment",
                      tint =
                        if (isNight) palette.nightShift.iconTint
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                }
              }

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
              ) {
                entry.shift?.let { sh ->
                  DetailMetaPair(
                    label = "Time",
                    value = "${sh.startTime}–${sh.endTime}",
                    labelColor = labelColor,
                    valueColor = titleColor,
                  )
                }
                entry.department?.let { dept ->
                  DetailMetaPair(
                    label = "Dept",
                    value = dept.name,
                    labelColor = labelColor,
                    valueColor = titleColor,
                  )
                }
                DetailMetaPair(
                  label = "Duty",
                  value = entry.duty?.title ?: "General",
                  labelColor = labelColor,
                  valueColor = titleColor,
                )
              }

              if (entry.assignment.notes.isNotBlank()) {
                DetailMetaPair(
                  label = "Notes",
                  value = entry.assignment.notes,
                  labelColor = labelColor,
                  valueColor = titleColor,
                )
              }
            }
          }
        }

        if (day.unassignedEmployees.isNotEmpty()) {
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text =
                "Unassigned (${day.unassignedEmployees.size}): " +
                  day.unassignedEmployees.joinToString(", ") { it.name.substringBefore(" ") },
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.tertiary,
              modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedButton(onClick = onAssignUnassigned) { Text("Assign") }
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RosterByEmployeeCard(
  group: EmployeeScheduleGroup,
  paletteIndex: Int,
  onAssignForEmployee: () -> Unit,
  onRemoveAssignment: (String) -> Unit,
) {
  val palette = LocalStatusPalette.current
  val empPalette = GradientPaletteRegistry.paletteForCard(group.employee.id, paletteIndex)
  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, empPalette.colors.first().copy(alpha = 0.42f)),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(modifier = Modifier.fillMaxWidth()) {
      GradientAccentStrip(palette = empPalette, height = 4.dp)
      Column(
        modifier = Modifier.padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              TintedPillTag(text = group.employee.code, accentColor = empPalette.colors.first())
              Text(
                text = group.employee.name,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
            Text(
              text = "${group.employee.designation} · Flexible shift & duty assignment",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Spacer(modifier = Modifier.width(8.dp))
          FilledTonalButton(onClick = onAssignForEmployee) { Text("Assign") }
        }

        if (group.upcomingEntries.isEmpty()) {
          Text(
            text = "No upcoming roster assignments.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        } else {
          group.upcomingEntries.forEach { entry ->
            val isNight = entry.shift?.isNightShiftVisual() == true
            val titleColor =
              if (isNight) palette.nightShift.text else MaterialTheme.colorScheme.onSurface
            val labelColor =
              if (isNight) palette.nightShift.text.copy(alpha = 0.78f)
              else MaterialTheme.colorScheme.onSurfaceVariant
            ReferenceDetailCard(
              accentColor = empPalette.colors[1],
              isNightShift = isNight,
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Row(
                  modifier = Modifier.weight(1f).padding(end = 8.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                  if (isNight) {
                    Icon(
                      imageVector = Icons.Outlined.NightsStay,
                      contentDescription = "Night Shift",
                      tint = palette.nightShift.iconTint,
                      modifier = Modifier.size(16.dp),
                    )
                  }
                  Text(
                    text =
                      "${DateUtils.formatShortDateWithDay(entry.assignment.date)} (${entry.assignment.date})",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = titleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                  SolidPillTag(
                    text = entry.shift?.name ?: "Shift",
                    containerColor =
                      if (isNight) palette.nightShift.border else empPalette.colors[1],
                  )
                  IconButton(
                    onClick = { onRemoveAssignment(entry.assignment.id) },
                    modifier = Modifier.minimumInteractiveComponentSize(),
                  ) {
                    Icon(
                      imageVector = Icons.Outlined.DeleteOutline,
                      contentDescription = "Remove assignment",
                      tint =
                        if (isNight) palette.nightShift.iconTint
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                }
              }

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
              ) {
                entry.shift?.let { sh ->
                  DetailMetaPair(
                    label = "Time",
                    value = "${sh.startTime}–${sh.endTime}",
                    labelColor = labelColor,
                    valueColor = titleColor,
                  )
                }
                entry.department?.let { dept ->
                  DetailMetaPair(
                    label = "Dept",
                    value = dept.name,
                    labelColor = labelColor,
                    valueColor = titleColor,
                  )
                }
                DetailMetaPair(
                  label = "Duty",
                  value = entry.duty?.title ?: "General",
                  labelColor = labelColor,
                  valueColor = titleColor,
                )
              }
            }
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HistorySearchContent(
  state: RegisterUiState,
  onUpdateHistoryQuery: ((HistoryFilterQuery) -> HistoryFilterQuery) -> Unit,
  onResetHistoryFilters: () -> Unit,
) {
  val query = state.historyQuery
  val palette = LocalStatusPalette.current
  val today = DateUtils.todayIso()

  val hasActiveFilters =
    query.searchText.isNotBlank() ||
      query.employeeId != null ||
      query.designation != null ||
      query.departmentId != null ||
      query.shiftId != null ||
      query.dutyTypeId != null ||
      query.status != null

  val filterPalette = GradientPaletteRegistry.paletteForCard("history_filters", 6)

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    // Search & Date Range Panel
    item {
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, filterPalette.colors.first().copy(alpha = 0.42f)),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Box(modifier = Modifier.fillMaxWidth()) {
          FloralTextureOverlay(
            variant = FloralVariant.CORNER_CLUSTER,
            motifColor = filterPalette.colors.first().copy(alpha = 0.25f),
            modifier = Modifier.matchParentSize(),
          )
          Column(modifier = Modifier.fillMaxWidth()) {
            GradientAccentStrip(palette = filterPalette, height = 4.dp)
            Column(
              modifier = Modifier.padding(14.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(
                  text = "Historical Duty & Shift Search",
                  style = MaterialTheme.typography.titleMedium,
                )
            if (hasActiveFilters) {
              TextButton(
                onClick = onResetHistoryFilters,
                modifier = Modifier.testTag("history_reset_filters_button"),
              ) {
                Icon(
                  imageVector = Icons.Outlined.FilterListOff,
                  contentDescription = null,
                  modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Reset")
              }
            }
          }

          val (syncedHistorySearch, onHistorySearchEdit) =
            rememberSyncedText(
              externalValue = query.searchText,
              onValueChange = { text -> onUpdateHistoryQuery { it.copy(searchText = text) } },
            )
          OutlinedTextField(
            value = syncedHistorySearch,
            onValueChange = onHistorySearchEdit,
            label = { Text("Search employee, designation, shift, duty, remarks") },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            trailingIcon =
              if (syncedHistorySearch.isNotBlank()) {
                {
                  IconButton(onClick = { onHistorySearchEdit("") }) {
                    Icon(Icons.Outlined.Clear, contentDescription = "Clear search")
                  }
                }
              } else null,
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("history_search_input"),
          )

          // Start Date & End Date Steppers
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            CompactDateAdjuster(
              label = "From Date",
              dateIso = query.startDate,
              onStep = { delta ->
                val nextStart = DateUtils.addDays(query.startDate, delta)
                onUpdateHistoryQuery {
                  it.copy(
                    startDate = nextStart,
                    endDate = if (nextStart > it.endDate) nextStart else it.endDate,
                  )
                }
              },
              modifier = Modifier.weight(1f),
            )
            CompactDateAdjuster(
              label = "To Date",
              dateIso = query.endDate,
              onStep = { delta ->
                val nextEnd = DateUtils.addDays(query.endDate, delta)
                onUpdateHistoryQuery {
                  it.copy(
                    startDate = if (nextEnd < it.startDate) nextEnd else it.startDate,
                    endDate = nextEnd,
                  )
                }
              },
              modifier = Modifier.weight(1f),
            )
          }

          // Quick Date Range Presets
          Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            val last7Start = DateUtils.addDays(today, -6)
            val last14Start = DateUtils.addDays(today, -14)
            val last30Start = DateUtils.addDays(today, -30)
            FilterChip(
              selected = query.startDate == today && query.endDate == today,
              onClick = { onUpdateHistoryQuery { it.copy(startDate = today, endDate = today) } },
              label = { Text("Today") },
            )
            FilterChip(
              selected = query.startDate == last7Start && query.endDate == today,
              onClick = { onUpdateHistoryQuery { it.copy(startDate = last7Start, endDate = today) } },
              label = { Text("Last 7 Days") },
            )
            FilterChip(
              selected = query.startDate == last14Start && query.endDate == today,
              onClick = { onUpdateHistoryQuery { it.copy(startDate = last14Start, endDate = today) } },
              label = { Text("Last 14 Days") },
            )
            FilterChip(
              selected = query.startDate == last30Start && query.endDate == today,
              onClick = { onUpdateHistoryQuery { it.copy(startDate = last30Start, endDate = today) } },
              label = { Text("Last 30 Days") },
            )
            FilterChip(
              selected = query.startDate == "2000-01-01" && query.endDate == "2099-12-31",
              onClick = {
                onUpdateHistoryQuery { it.copy(startDate = "2000-01-01", endDate = "2099-12-31") }
              },
              label = { Text("All Dates") },
            )
          }

          // Status Filter Chips
          Text("Attendance Status", style = MaterialTheme.typography.labelLarge)
          Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            FilterChip(
              selected = query.status == null,
              onClick = { onUpdateHistoryQuery { it.copy(status = null) } },
              label = { Text("All") },
            )
            AttendanceStatus.entries.forEach { st ->
              StatusFilterChip(
                status = st,
                selected = query.status == st,
                onClick = {
                  onUpdateHistoryQuery { it.copy(status = if (it.status == st) null else st) }
                },
              )
            }
          }

          // Shift Filter Chips
          Text("Shift", style = MaterialTheme.typography.labelLarge)
          Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            FilterChip(
              selected = query.shiftId == null,
              onClick = { onUpdateHistoryQuery { it.copy(shiftId = null) } },
              label = { Text("All Shifts") },
            )
            state.allShifts.forEach { sh ->
              ShiftFilterChip(
                label = sh.name,
                isNightShift = sh.isNightShiftVisual(),
                selected = query.shiftId == sh.id,
                onClick = {
                  onUpdateHistoryQuery { it.copy(shiftId = if (it.shiftId == sh.id) null else sh.id) }
                },
              )
            }
          }

          // Department Filter Chips
          if (state.allDepartments.isNotEmpty()) {
            Text("Department", style = MaterialTheme.typography.labelLarge)
            Row(
              modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              FilterChip(
                selected = query.departmentId == null,
                onClick = { onUpdateHistoryQuery { it.copy(departmentId = null) } },
                label = { Text("All Departments") },
                modifier = Modifier.testTag("history_dept_all"),
              )
              state.allDepartments.forEach { dept ->
                FilterChip(
                  selected = query.departmentId == dept.id,
                  onClick = {
                    onUpdateHistoryQuery {
                      it.copy(departmentId = if (it.departmentId == dept.id) null else dept.id)
                    }
                  },
                  label = { Text(dept.name) },
                  modifier = Modifier.testTag("history_dept_${dept.id}"),
                )
              }
            }
          }

          // Duty Filter Chips
          Text("Duty", style = MaterialTheme.typography.labelLarge)
          Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            FilterChip(
              selected = query.dutyTypeId == null,
              onClick = { onUpdateHistoryQuery { it.copy(dutyTypeId = null) } },
              label = { Text("All Duties") },
            )
            state.allDuties.forEach { du ->
              FilterChip(
                selected = query.dutyTypeId == du.id,
                onClick = {
                  onUpdateHistoryQuery {
                    it.copy(dutyTypeId = if (it.dutyTypeId == du.id) null else du.id)
                  }
                },
                label = { Text(du.title) },
              )
            }
          }

          // Employee & Designation Filter Chips
          Text("Employee & Designation", style = MaterialTheme.typography.labelLarge)
          Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            FilterChip(
              selected = query.employeeId == null,
              onClick = { onUpdateHistoryQuery { it.copy(employeeId = null) } },
              label = { Text("All Staff") },
            )
            state.allEmployees.forEach { emp ->
              FilterChip(
                selected = query.employeeId == emp.id,
                onClick = {
                  onUpdateHistoryQuery {
                    it.copy(employeeId = if (it.employeeId == emp.id) null else emp.id)
                  }
                },
                label = { Text("${emp.code} ${emp.name.substringBefore(" ")}") },
              )
            }
          }
          if (state.distinctDesignations.isNotEmpty()) {
            Row(
              modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              FilterChip(
                selected = query.designation == null,
                onClick = { onUpdateHistoryQuery { it.copy(designation = null) } },
                label = { Text("All Designations") },
              )
              state.distinctDesignations.forEach { desig ->
                FilterChip(
                  selected = query.designation == desig,
                  onClick = {
                    onUpdateHistoryQuery {
                      it.copy(designation = if (it.designation == desig) null else desig)
                    }
                  },
                  label = { Text(desig) },
                )
              }
            }
          }
        }
      }
    }
  }
}

    // Results Count Header
    item {
      TintedPillTag(
        text = "${state.historyEntries.size} matching historical record(s)",
        accentColor = filterPalette.colors.first(),
      )
    }

    if (state.historyEntries.isEmpty()) {
      item {
        EmptyStateCard(
          title = "No matching history records",
          message = "Adjust your date range, search query, or status/shift/duty filters.",
          icon = Icons.Outlined.History,
        )
      }
    } else {
      itemsIndexed(state.historyEntries, key = { _, entry -> entry.id }) { idx, entry ->
        HistoryEntryCard(entry = entry, paletteIndex = idx)
      }
    }

    item { Spacer(modifier = Modifier.height(16.dp)) }
  }
}

@Composable
private fun CompactDateAdjuster(
  label: String,
  dateIso: String,
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
      modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      IconButton(
        onClick = { onStep(-1) },
        modifier = Modifier.minimumInteractiveComponentSize(),
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
          contentDescription = "Previous $label",
        )
      }
      Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
        Text(
          text = label,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
          text = dateIso,
          style = MonoLabelStyle,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
      IconButton(
        onClick = { onStep(1) },
        modifier = Modifier.minimumInteractiveComponentSize(),
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
          contentDescription = "Next $label",
        )
      }
    }
  }
}

@Composable
private fun HistoryEntryCard(
  entry: HistorySearchEntry,
  paletteIndex: Int,
) {
  val palette = LocalStatusPalette.current
  val statusColors = palette.forStatus(entry.status)
  val isNight = entry.isNightShift
  val accentColor = statusColors.text

  ReferenceDetailCard(
    accentColor = accentColor,
    isNightShift = isNight,
    tintContainerColor = statusColors.container,
    modifier = Modifier.testTag("history_entry_${entry.id}"),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = "${entry.employeeName} (${entry.employeeCode})",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = if (isNight) palette.nightShift.text else MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(1f).padding(end = 8.dp),
      )
      StatusBadge(status = entry.status)
    }

    val labelTint =
      if (isNight) Color(0xFFC4B5FD) else MaterialTheme.colorScheme.onSurfaceVariant
    val valueTint =
      if (isNight) palette.nightShift.text else MaterialTheme.colorScheme.onSurface

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(18.dp),
    ) {
      DetailMetaPair(
        label = "Date",
        value = entry.date,
        labelColor = labelTint,
        valueColor = valueTint,
        modifier = Modifier.weight(1f),
      )
      DetailMetaPair(
        label = "Shift",
        value = entry.shiftName,
        labelColor = labelTint,
        valueColor = valueTint,
      )
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(18.dp),
    ) {
      DetailMetaPair(
        label = "Dept",
        value = entry.departmentName,
        labelColor = labelTint,
        valueColor = valueTint,
        modifier = Modifier.weight(1f),
      )
      DetailMetaPair(
        label = "Duty",
        value = entry.dutyTitle.ifBlank { "No Duty" },
        labelColor = labelTint,
        valueColor = valueTint,
        modifier = Modifier.weight(1f),
      )
      DetailMetaPair(
        label = "Role",
        value = entry.designation,
        labelColor = labelTint,
        valueColor = valueTint,
      )
    }

    if (isNight) {
      ShiftHighlightBadge(
        shiftName = entry.shiftName,
        timeWindow = null,
        isNightShift = true,
      )
    }

    if (entry.remarks.isNotBlank()) {
      Text(
        text = "Remarks: ${entry.remarks}",
        style = MaterialTheme.typography.bodySmall,
        color = if (isNight) Color(0xFFDDD6FE) else MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}
