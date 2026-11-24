package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.logic.DateUtils
import com.example.models.AttendanceStatus
import com.example.models.BatchRosterAssignmentRequest
import com.example.models.BatchShiftAssignmentItem
import com.example.models.DailyAttendanceRow
import com.example.models.Department
import com.example.models.DutyType
import com.example.models.Employee
import com.example.models.EmployeeCarryForward
import com.example.models.OfflineUpdatePack
import com.example.models.OfflineUpdatePreview
import com.example.models.RosterAssignmentRequest
import com.example.models.Shift
import com.example.models.UpdateItemAction
import com.example.ui.state.AssignSheetFocus
import com.example.ui.state.AssignmentSheetState
import com.example.ui.state.PendingBackupRestoreState
import com.example.ui.state.PendingConflictState
import com.example.ui.theme.FloralVariant
import com.example.ui.theme.GradientPaletteRegistry
import com.example.ui.theme.LocalStatusPalette
import com.example.ui.theme.MonoLabelStyle

private data class StagedShiftBatch(
  val shiftId: String,
  val employeeIds: Set<String> = emptySet(),
  val departmentId: String? = null,
  val dutyTypeId: String? = null,
  val notes: String = "",
  val manualDeptDutyOverride: Boolean = false,
  val carriedFromDate: String? = null,
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RosterAssignmentBottomSheet(
  sheetState: AssignmentSheetState,
  employees: List<Employee>,
  shifts: List<Shift>,
  duties: List<DutyType>,
  departments: List<Department> = emptyList(),
  carryForwardByEmployee: Map<String, EmployeeCarryForward> = emptyMap(),
  onDismiss: () -> Unit,
  onSubmit: (RosterAssignmentRequest) -> Unit,
  onSubmitBatch: (BatchRosterAssignmentRequest) -> Unit,
) {
  if (!sheetState.isVisible) return

  val modalState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var isDateRange by remember(sheetState) { mutableStateOf(false) }
  var startDate by remember(sheetState) { mutableStateOf(sheetState.startDate) }
  var endDate by remember(sheetState) { mutableStateOf(sheetState.endDate) }

  val initialShiftId = shifts.firstOrNull()?.id ?: ""
  var activeShiftId by remember(sheetState, shifts) { mutableStateOf(initialShiftId) }

  fun resolveGroupCarryForward(selectedIds: Set<String>, preferredEmpId: String? = null): EmployeeCarryForward? {
    preferredEmpId?.let { id -> carryForwardByEmployee[id]?.let { return it } }
    return selectedIds.firstNotNullOfOrNull { id -> carryForwardByEmployee[id] }
  }

  // Single Date staged batches keyed by shiftId
  var stagedBatches by
    remember(sheetState, shifts, carryForwardByEmployee) {
      val initialMap =
        if (sheetState.preselectedEmployeeIds.isNotEmpty() && initialShiftId.isNotBlank()) {
          val cf = resolveGroupCarryForward(sheetState.preselectedEmployeeIds)
          mapOf(
            initialShiftId to
              StagedShiftBatch(
                shiftId = initialShiftId,
                employeeIds = sheetState.preselectedEmployeeIds,
                departmentId = cf?.departmentId,
                dutyTypeId = cf?.dutyTypeId,
                carriedFromDate = cf?.sourceDate,
              )
          )
        } else {
          emptyMap()
        }
      mutableStateOf(initialMap)
    }

  // Date Range single-shift state
  val initialRangeEmployees =
    remember(sheetState, employees) {
      if (sheetState.preselectedEmployeeIds.isNotEmpty()) sheetState.preselectedEmployeeIds
      else employees.take(1).map { it.id }.toSet()
    }
  val initialRangeCf =
    remember(sheetState, carryForwardByEmployee, initialRangeEmployees) {
      resolveGroupCarryForward(initialRangeEmployees)
    }
  var rangeEmployeeIds by remember(sheetState) { mutableStateOf(initialRangeEmployees) }
  var rangeDepartmentId by remember(sheetState) { mutableStateOf(initialRangeCf?.departmentId) }
  var rangeDutyId by remember(sheetState) { mutableStateOf(initialRangeCf?.dutyTypeId) }
  var rangeManualOverride by remember(sheetState) { mutableStateOf(false) }
  var rangeCarriedFromDate by remember(sheetState) { mutableStateOf(initialRangeCf?.sourceDate) }
  var rangeNotes by remember(sheetState) { mutableStateOf("") }

  var showDiscardDialog by remember(sheetState) { mutableStateOf(false) }

  val nonEmptyBatches =
    stagedBatches.values.filter { it.shiftId.isNotBlank() && it.employeeIds.isNotEmpty() }
  val totalStagedSlots = nonEmptyBatches.sumOf { it.employeeIds.size }

  fun requestDismiss() {
    if (!isDateRange && totalStagedSlots > 0) {
      showDiscardDialog = true
    } else {
      onDismiss()
    }
  }

  val currentBatch =
    stagedBatches[activeShiftId] ?: StagedShiftBatch(shiftId = activeShiftId)
  val selectedEmployeeIds =
    if (isDateRange) rangeEmployeeIds else currentBatch.employeeIds
  val selectedDepartmentId =
    if (isDateRange) rangeDepartmentId else currentBatch.departmentId
  val selectedDutyId =
    if (isDateRange) rangeDutyId else currentBatch.dutyTypeId
  val carriedFromDate =
    if (isDateRange) rangeCarriedFromDate else currentBatch.carriedFromDate
  val notes =
    if (isDateRange) rangeNotes else currentBatch.notes

  fun updateCurrentEmployeeIds(nextIds: Set<String>, newlyAddedId: String? = null) {
    if (isDateRange) {
      rangeEmployeeIds = nextIds
      if (!rangeManualOverride) {
        val cf = resolveGroupCarryForward(nextIds, newlyAddedId)
        rangeDepartmentId = cf?.departmentId
        rangeDutyId = cf?.dutyTypeId
        rangeCarriedFromDate = cf?.sourceDate
      }
    } else if (activeShiftId.isNotBlank()) {
      val updated =
        if (!currentBatch.manualDeptDutyOverride) {
          val cf = resolveGroupCarryForward(nextIds, newlyAddedId)
          currentBatch.copy(
            shiftId = activeShiftId,
            employeeIds = nextIds,
            departmentId = cf?.departmentId,
            dutyTypeId = cf?.dutyTypeId,
            carriedFromDate = cf?.sourceDate,
          )
        } else {
          currentBatch.copy(shiftId = activeShiftId, employeeIds = nextIds)
        }
      stagedBatches =
        if (updated.employeeIds.isEmpty() && updated.dutyTypeId == null && updated.departmentId == null && updated.notes.isBlank()) {
          stagedBatches - activeShiftId
        } else {
          stagedBatches + (activeShiftId to updated)
        }
    }
  }

  fun updateCurrentDepartmentAndDuty(
    nextDeptId: String?,
    nextDutyId: String?,
    isManual: Boolean = true,
  ) {
    if (isDateRange) {
      rangeDepartmentId = nextDeptId
      rangeDutyId = nextDutyId
      if (isManual) {
        rangeManualOverride = true
        rangeCarriedFromDate = null
      }
    } else if (activeShiftId.isNotBlank()) {
      val updated =
        currentBatch.copy(
          shiftId = activeShiftId,
          departmentId = nextDeptId,
          dutyTypeId = nextDutyId,
          manualDeptDutyOverride = if (isManual) true else currentBatch.manualDeptDutyOverride,
          carriedFromDate = if (isManual) null else currentBatch.carriedFromDate,
        )
      stagedBatches = stagedBatches + (activeShiftId to updated)
    }
  }

  fun updateCurrentNotes(nextNotes: String) {
    if (isDateRange) {
      rangeNotes = nextNotes
    } else if (activeShiftId.isNotBlank()) {
      val updated = currentBatch.copy(shiftId = activeShiftId, notes = nextNotes)
      stagedBatches = stagedBatches + (activeShiftId to updated)
    }
  }

  if (showDiscardDialog) {
    AlertDialog(
      onDismissRequest = { showDiscardDialog = false },
      icon = {
        Icon(
          imageVector = Icons.Outlined.WarningAmber,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.tertiary,
        )
      },
      title = { Text("Discard Staged Assignments?") },
      text = {
        Text(
          text =
            "You have $totalStagedSlots staged assignment(s) across ${nonEmptyBatches.size} shift(s) that have not been assigned yet.",
          style = MaterialTheme.typography.bodyMedium,
        )
      },
      confirmButton = {
        Button(
          onClick = {
            showDiscardDialog = false
            onDismiss()
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
          modifier = Modifier.testTag("confirm_discard_staged_button"),
        ) {
          Text("Discard")
        }
      },
      dismissButton = {
        TextButton(onClick = { showDiscardDialog = false }) { Text("Keep Editing") }
      },
    )
  }

  val title =
    when (sheetState.focus) {
      AssignSheetFocus.DUTY_FIRST -> "Assign Duty & Shift"
      AssignSheetFocus.SHIFT_FIRST -> "Assign Shift Schedule"
      AssignSheetFocus.BOTH -> "Assign Shift & Duty"
    }

  val sheetPalette = GradientPaletteRegistry.paletteForCard("assign_sheet", 8)
  val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
  val statusPalette = LocalStatusPalette.current
  val empMap = employees.associateBy { it.id }
  val shiftMap = shifts.associateBy { it.id }
  val deptMap = departments.associateBy { it.id }
  val dutyMap = duties.associateBy { it.id }

  ModalBottomSheet(
    onDismissRequest = { requestDismiss() },
    sheetState = modalState,
  ) {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 20.dp)
          .padding(bottom = 28.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      VibrantFeatureBanner(
        title = title,
        subtitle =
          if (isDateRange) "Assign one shift across a date range"
          else "Stage multiple shifts & employees, then commit with Assign",
        palette = sheetPalette,
        floralVariant = FloralVariant.LEAF_GARLAND,
        icon = Icons.Outlined.Schedule,
        badgeText =
          if (isDateRange) "${selectedEmployeeIds.size} STAFF"
          else "$totalStagedSlots STAGED",
      )

      SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        SegmentedButton(
          selected = !isDateRange,
          onClick = {
            isDateRange = false
            endDate = startDate
          },
          shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
          modifier = Modifier.testTag("assign_mode_single_date"),
        ) {
          Text("Single Date")
        }
        SegmentedButton(
          selected = isDateRange,
          onClick = {
            isDateRange = true
            if (endDate <= startDate) {
              endDate = DateUtils.addDays(startDate, 2)
            }
          },
          shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
          modifier = Modifier.testTag("assign_mode_date_range"),
        ) {
          Text("Date Range")
        }
      }

      DateStepperBar(
        selectedDate = startDate,
        onPreviousDay = {
          startDate = DateUtils.addDays(startDate, -1)
          if (!isDateRange || endDate < startDate) endDate = startDate
        },
        onNextDay = {
          startDate = DateUtils.addDays(startDate, 1)
          if (!isDateRange || endDate < startDate) endDate = startDate
        },
        onJumpToday = {
          startDate = DateUtils.todayIso()
          if (!isDateRange || endDate < startDate) endDate = startDate
        },
      )

      if (isDateRange) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = "End Date (Inclusive)",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          DateStepperBar(
            selectedDate = endDate,
            onPreviousDay = {
              val prev = DateUtils.addDays(endDate, -1)
              if (prev >= startDate) endDate = prev
            },
            onNextDay = { endDate = DateUtils.addDays(endDate, 1) },
            onJumpToday = {
              val today = DateUtils.todayIso()
              endDate = if (today >= startDate) today else startDate
            },
          )
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(1 to "2 Days", 2 to "3 Days", 6 to "7 Days").forEach { (offset, label) ->
              FilterChip(
                selected = endDate == DateUtils.addDays(startDate, offset),
                onClick = { endDate = DateUtils.addDays(startDate, offset) },
                label = { Text(label) },
              )
            }
          }
        }
      }

      // Shift Selection (switch shifts freely without losing staged selections in Single Date mode)
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
          text =
            if (isDateRange) "Shift Slot"
            else "Shift Slot (switch to stage multiple shifts)",
          style = MaterialTheme.typography.titleSmall,
        )
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          shifts.forEach { shift ->
            val stagedCount =
              if (isDateRange) 0 else stagedBatches[shift.id]?.employeeIds?.size ?: 0
            val suffix = if (stagedCount > 0) " · $stagedCount" else ""
            ShiftFilterChip(
              label = "${shift.name} (${shift.startTime}–${shift.endTime})$suffix",
              isNightShift = shift.isNightShiftVisual(),
              selected = activeShiftId == shift.id,
              onClick = { activeShiftId = shift.id },
              modifier = Modifier.testTag("assign_shift_chip_${shift.id}"),
            )
          }
        }
      }

      // Employee Selection for currently active shift
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          val activeShiftLabel = shiftMap[activeShiftId]?.name ?: "Shift"
          Text(
            text =
              if (isDateRange) "Team Members (${selectedEmployeeIds.size} selected)"
              else "Staff for $activeShiftLabel (${selectedEmployeeIds.size})",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.weight(1f),
          )
          Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            TextButton(onClick = { updateCurrentEmployeeIds(employees.map { it.id }.toSet()) }) {
              Text("All")
            }
            TextButton(onClick = { updateCurrentEmployeeIds(emptySet()) }) { Text("Clear") }
          }
        }
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          employees.forEachIndexed { idx, emp ->
            val selected = emp.id in selectedEmployeeIds
            val empAccent = GradientPaletteRegistry.paletteForCard(emp.id, idx).colors.first()
            FilterChip(
              selected = selected,
              onClick = {
                val next =
                  if (selected) selectedEmployeeIds - emp.id else selectedEmployeeIds + emp.id
                updateCurrentEmployeeIds(next, newlyAddedId = if (!selected) emp.id else null)
              },
              label = { Text("${emp.code} · ${emp.name}") },
              leadingIcon =
                if (selected) {
                  { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                } else null,
              border =
                FilterChipDefaults.filterChipBorder(
                  enabled = true,
                  selected = selected,
                  borderColor = empAccent.copy(alpha = 0.35f),
                  selectedBorderColor = empAccent,
                ),
              colors =
                FilterChipDefaults.filterChipColors(
                  selectedContainerColor = empAccent.copy(alpha = if (isDark) 0.32f else 0.18f),
                  selectedLabelColor = MaterialTheme.colorScheme.onSurface,
                ),
              modifier = Modifier.testTag("assign_emp_chip_${emp.id}"),
            )
          }
        }
      }

      // Carry-forward banner when auto-populated
      if (carriedFromDate != null && (selectedDepartmentId != null || selectedDutyId != null)) {
        val cfLabel =
          if (carriedFromDate == "default") {
            "Pre-filled from employee default dept (tap Dept/Duty to override or Clear)"
          } else {
            "Carried forward from $carriedFromDate (tap Dept/Duty to override or Clear)"
          }
        TintedPillTag(
          text = cfLabel,
          accentColor = sheetPalette.colors.first(),
        )
      }

      // Department Selection (optional, auto-populated via carry-forward)
      if (departments.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(text = "Department (optional)", style = MaterialTheme.typography.titleSmall)
            if (selectedDepartmentId != null || selectedDutyId != null) {
              TextButton(
                onClick = { updateCurrentDepartmentAndDuty(null, null, isManual = true) },
                modifier = Modifier.testTag("clear_dept_duty_button"),
              ) {
                Text("Clear Dept/Duty")
              }
            }
          }
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            FilterChip(
              selected = selectedDepartmentId == null,
              onClick = {
                val keepDuty =
                  selectedDutyId?.takeIf { dId -> dutyMap[dId]?.departmentId.isNullOrBlank() }
                updateCurrentDepartmentAndDuty(null, keepDuty, isManual = true)
              },
              label = { Text("Any Department") },
              modifier = Modifier.testTag("assign_dept_chip_any"),
            )
            departments.forEachIndexed { idx, dept ->
              val deptAccent = GradientPaletteRegistry.paletteForCard(dept.id, idx).colors.first()
              val selected = selectedDepartmentId == dept.id
              FilterChip(
                selected = selected,
                onClick = {
                  val keepDuty =
                    selectedDutyId?.takeIf { dId -> dutyMap[dId]?.departmentId == dept.id }
                  updateCurrentDepartmentAndDuty(dept.id, keepDuty, isManual = true)
                },
                label = { Text(dept.name) },
                border =
                  FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selected,
                    borderColor = deptAccent.copy(alpha = 0.35f),
                    selectedBorderColor = deptAccent,
                  ),
                colors =
                  FilterChipDefaults.filterChipColors(
                    selectedContainerColor = deptAccent.copy(alpha = if (isDark) 0.32f else 0.18f),
                    selectedLabelColor = MaterialTheme.colorScheme.onSurface,
                  ),
                modifier = Modifier.testTag("assign_dept_chip_${dept.id}"),
              )
            }
          }
        }
      }

      // Duty Selection (scoped to selected Department if chosen, or all active duties)
      val visibleDuties =
        if (selectedDepartmentId != null) {
          duties.filter { it.departmentId == selectedDepartmentId || it.departmentId.isBlank() }
        } else {
          duties
        }
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
          text =
            selectedDepartmentId?.let { dId ->
              "Duty in ${deptMap[dId]?.name ?: "Department"} (optional)"
            } ?: "Duty Template (optional)",
          style = MaterialTheme.typography.titleSmall,
        )
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          FilterChip(
            selected = selectedDutyId == null,
            onClick = { updateCurrentDepartmentAndDuty(selectedDepartmentId, null, isManual = true) },
            label = { Text("No Specific Duty") },
          )
          visibleDuties.forEachIndexed { idx, duty ->
            val dutyAccent = GradientPaletteRegistry.paletteForCard(duty.id, idx).colors.first()
            val selected = selectedDutyId == duty.id
            FilterChip(
              selected = selected,
              onClick = {
                val targetDept =
                  duty.departmentId.takeIf { it.isNotBlank() } ?: selectedDepartmentId
                updateCurrentDepartmentAndDuty(targetDept, duty.id, isManual = true)
              },
              label = { Text(duty.title) },
              border =
                FilterChipDefaults.filterChipBorder(
                  enabled = true,
                  selected = selected,
                  borderColor = dutyAccent.copy(alpha = 0.35f),
                  selectedBorderColor = dutyAccent,
                ),
              colors =
                FilterChipDefaults.filterChipColors(
                  selectedContainerColor = dutyAccent.copy(alpha = if (isDark) 0.32f else 0.18f),
                  selectedLabelColor = MaterialTheme.colorScheme.onSurface,
                ),
              modifier = Modifier.testTag("assign_duty_chip_${duty.id}"),
            )
          }
        }
      }

      OutlinedTextField(
        value = notes,
        onValueChange = { updateCurrentNotes(it) },
        label = { Text("Assignment Notes (optional)") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
      )

      // Staged multi-shift summary review box (Single Date mode)
      if (!isDateRange && nonEmptyBatches.isNotEmpty()) {
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.surfaceContainerLow,
          border = BorderStroke(1.dp, sheetPalette.colors.first().copy(alpha = 0.45f)),
          modifier = Modifier.fillMaxWidth().testTag("staged_batches_summary"),
        ) {
          Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Text(
              text = "Staged for $startDate ($totalStagedSlots slot(s) across ${nonEmptyBatches.size} shift(s))",
              style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            )
            nonEmptyBatches.forEach { batch ->
              val sh = shiftMap[batch.shiftId]
              val isNight = sh?.isNightShiftVisual() == true
              val staffNames =
                batch.employeeIds.mapNotNull { eid -> empMap[eid]?.name }.joinToString(", ")
              val deptName = batch.departmentId?.let { deptMap[it]?.name }
              val dutyTitle = batch.dutyTypeId?.let { dutyMap[it]?.title }
              Surface(
                shape = RoundedCornerShape(10.dp),
                color =
                  if (isNight) statusPalette.nightShift.container
                  else MaterialTheme.colorScheme.surface,
                border =
                  BorderStroke(
                    1.dp,
                    if (isNight) statusPalette.nightShift.border
                    else MaterialTheme.colorScheme.outlineVariant,
                  ),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                  ) {
                    Text(
                      text =
                        "${sh?.name ?: batch.shiftId} (${batch.employeeIds.size})" +
                          (deptName?.let { " · $it" } ?: "") +
                          (dutyTitle?.let { " · $it" } ?: ""),
                      style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                      color =
                        if (isNight) statusPalette.nightShift.text
                        else MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                      text = staffNames,
                      style = MaterialTheme.typography.bodySmall,
                      color =
                        if (isNight) statusPalette.nightShift.iconTint
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                  IconButton(
                    onClick = { stagedBatches = stagedBatches - batch.shiftId },
                  ) {
                    Icon(
                      imageVector = Icons.Outlined.Close,
                      contentDescription = "Clear ${sh?.name ?: "shift"} staged items",
                      tint =
                        if (isNight) statusPalette.nightShift.iconTint
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                }
              }
            }
          }
        }
      }

      val canSubmit =
        if (isDateRange) rangeEmployeeIds.isNotEmpty() && activeShiftId.isNotBlank()
        else totalStagedSlots > 0

      if (!canSubmit) {
        Text(
          text =
            if (isDateRange) "Select at least one employee and a Shift slot."
            else "Select a Shift and stage at least one team member.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.error,
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        OutlinedButton(
          onClick = { requestDismiss() },
          modifier = Modifier.weight(1f).testTag("cancel_assign_button"),
        ) {
          Text("Cancel")
        }
        Button(
          onClick = {
            if (isDateRange) {
              onSubmit(
                RosterAssignmentRequest(
                  employeeIds = rangeEmployeeIds.toList(),
                  startDate = startDate,
                  endDate = endDate,
                  shiftId = activeShiftId,
                  dutyTypeId = rangeDutyId,
                  notes = rangeNotes,
                  overwriteConflicts = false,
                  departmentId = rangeDepartmentId,
                )
              )
            } else {
              onSubmitBatch(
                BatchRosterAssignmentRequest(
                  date = startDate,
                  batches =
                    nonEmptyBatches.map { b ->
                      BatchShiftAssignmentItem(
                        shiftId = b.shiftId,
                        employeeIds = b.employeeIds.toList(),
                        dutyTypeId = b.dutyTypeId,
                        notes = b.notes,
                        departmentId = b.departmentId,
                      )
                    },
                  overwriteConflicts = false,
                )
              )
            }
          },
          enabled = canSubmit,
          modifier = Modifier.weight(1f).testTag("confirm_assign_button"),
        ) {
          Text(if (!isDateRange && totalStagedSlots > 0) "Assign ($totalStagedSlots)" else "Assign")
        }
      }
    }
  }
}

@Composable
fun ConflictResolutionDialog(
  pending: PendingConflictState,
  onOverwrite: () -> Unit,
  onDismiss: () -> Unit,
) {
  val dialogPalette = GradientPaletteRegistry.paletteForCard("conflict_dialog", 10)
  AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
      Icon(
        imageVector = Icons.Outlined.WarningAmber,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.tertiary,
      )
    },
    title = { Text("Shift Slot Conflict (${pending.conflicts.size})") },
    text = {
      Column(
        modifier = Modifier.heightIn(max = 280.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        GradientAccentStrip(
          palette = dialogPalette,
          height = 4.dp,
          modifier = Modifier.clip(RoundedCornerShape(4.dp)),
        )
        Text(
          text =
            "These employees already have an assignment for the exact same date & shift. Overwrite their duty/notes?",
          style = MaterialTheme.typography.bodyMedium,
        )
        pending.conflicts.take(10).forEach { c ->
          Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            border = BorderStroke(1.dp, dialogPalette.colors.first().copy(alpha = 0.35f)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Text(
                text = "${c.date} · ${c.employeeName} (${c.employeeCode})",
                style = MaterialTheme.typography.labelLarge,
              )
              TintedPillTag(
                text =
                  "Shift: ${c.shiftName}" +
                    (c.existingDutyTitle?.let { " · Duty: $it" } ?: ""),
                accentColor = dialogPalette.colors[1],
              )
            }
          }
        }
        if (pending.conflicts.size > 10) {
          Text(
            text = "+ ${pending.conflicts.size - 10} more conflicts",
            style = MaterialTheme.typography.labelMedium,
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = onOverwrite,
        modifier = Modifier.testTag("overwrite_conflicts_button"),
      ) {
        Text("Overwrite & Apply")
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
  )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AttendanceRemarksDialog(
  row: DailyAttendanceRow,
  date: String,
  onSave: (AttendanceStatus, String) -> Unit,
  onDismiss: () -> Unit,
) {
  var status by remember(row) {
    mutableStateOf(row.existingRecord?.status ?: AttendanceStatus.PRESENT)
  }
  var remarks by remember(row) { mutableStateOf(row.existingRecord?.remarks ?: "") }
  val dialogPalette = GradientPaletteRegistry.paletteForCard("remarks_dialog", 7)

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("${row.employee.name} (${row.employee.code})") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        GradientAccentStrip(
          palette = dialogPalette,
          height = 4.dp,
          modifier = Modifier.clip(RoundedCornerShape(4.dp)),
        )
        Text(
          text = "${row.employee.designation} · $date",
          style = MonoLabelStyle,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ShiftHighlightBadge(
          shiftName = row.shift?.name ?: "Unscheduled Shift",
          timeWindow = row.shift?.let { "${it.startTime}–${it.endTime}" },
          isNightShift = row.isNightShift,
        )
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          AttendanceStatus.entries.forEach { st ->
            StatusFilterChip(
              status = st,
              selected = status == st,
              onClick = { status = st },
            )
          }
        }
        OutlinedTextField(
          value = remarks,
          onValueChange = { remarks = it },
          label = { Text("Remarks (optional)") },
          placeholder = { Text("e.g. Late arrival, double shift, rotational off") },
          maxLines = 3,
          modifier = Modifier.fillMaxWidth().testTag("attendance_remarks_input"),
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { onSave(status, remarks) },
        modifier = Modifier.testTag("save_attendance_remarks_button"),
      ) {
        Text("Save Entry")
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
  )
}

@Composable
fun BackupRestoreValidationDialog(
  pending: PendingBackupRestoreState,
  onConfirmRestore: () -> Unit,
  onDismiss: () -> Unit,
) {
  val v = pending.validation
  val bundle = v.bundle
  val backupPalette = GradientPaletteRegistry.paletteForCard("backup_dialog", 3)

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(if (v.isValid) "Restore Verified Backup" else "Invalid Backup File") },
    text = {
      Column(
        modifier = Modifier.heightIn(max = 340.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        GradientAccentStrip(
          palette = backupPalette,
          height = 4.dp,
          modifier = Modifier.clip(RoundedCornerShape(4.dp)),
        )
        if (bundle != null) {
          Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            border = BorderStroke(1.dp, backupPalette.colors.first().copy(alpha = 0.40f)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(
              modifier = Modifier.padding(12.dp),
              verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Text(
                text = "Schema v${bundle.schemaVersion} · App v${bundle.appVersion}",
                style = MaterialTheme.typography.labelLarge,
              )
              Text(
                text = "Exported: ${DateUtils.formatTimestamp(bundle.exportedAt)}",
                style = MaterialTheme.typography.bodySmall,
              )
              Text(
                text =
                  "${bundle.recordCounts.employees} employees · ${bundle.recordCounts.shifts} shifts · ${bundle.recordCounts.dutyTypes} duties · ${bundle.recordCounts.rosterAssignments} roster · ${bundle.recordCounts.attendanceRecords} attendance",
                style = MaterialTheme.typography.bodySmall,
              )
              TintedPillTag(
                text = "SHA-256: ${bundle.checksum.take(16)}...",
                accentColor = backupPalette.colors.first(),
              )
            }
          }
        }

        if (v.errors.isNotEmpty()) {
          v.errors.forEach { err ->
            Text(
              text = "• $err",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.error,
            )
          }
        }
        if (v.warnings.isNotEmpty()) {
          v.warnings.forEach { warn ->
            Text(
              text = "Warning: $warn",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.tertiary,
            )
          }
        }
        if (v.isValid) {
          Text(
            text =
              "An automatic pre-restore safety snapshot will be saved locally before replacing data inside an atomic transaction.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    },
    confirmButton = {
      if (v.isValid) {
        Button(
          onClick = onConfirmRestore,
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
          modifier = Modifier.testTag("confirm_restore_button"),
        ) {
          Text("Create Snapshot & Restore")
        }
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } },
  )
}

@Composable
fun OfflineUpdatePreviewDialog(
  preview: OfflineUpdatePreview,
  onApply: (OfflineUpdatePack) -> Unit,
  onDismiss: () -> Unit,
) {
  val pack = preview.pack
  val updatePalette = GradientPaletteRegistry.paletteForCard("update_dialog", 4)

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        if (preview.isCompatible && pack != null) "Offline Update Preview (${pack.packVersion})"
        else "Incompatible Update Pack"
      )
    },
    text = {
      Column(
        modifier = Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        GradientAccentStrip(
          palette = updatePalette,
          height = 4.dp,
          modifier = Modifier.clip(RoundedCornerShape(4.dp)),
        )
        if (pack != null) {
          Text(text = pack.releaseNotes, style = MaterialTheme.typography.bodyMedium)
          Text(
            text = "Pack ID: ${pack.packId} · Schema v${pack.minSchemaVersion}–v${pack.maxSchemaVersion}",
            style = MonoLabelStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }

        if (preview.errors.isNotEmpty()) {
          preview.errors.forEach { err ->
            Text(
              text = "• $err",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.error,
            )
          }
        }

        if (preview.isCompatible) {
          Surface(
            color = updatePalette.colors.first().copy(alpha = 0.14f),
            border = BorderStroke(1.dp, updatePalette.colors.first().copy(alpha = 0.45f)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(
              modifier = Modifier.padding(12.dp),
              verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Text(
                text =
                  "Shifts: +${preview.newShiftsCount} new, ${preview.updatedShiftsCount} updated",
                style = MaterialTheme.typography.labelLarge,
              )
              Text(
                text =
                  "Duty Templates: +${preview.newDutiesCount} new, ${preview.updatedDutiesCount} updated",
                style = MaterialTheme.typography.labelLarge,
              )
              Text(
                text = "Reference Rules: ${preview.rulesCount} rule(s)",
                style = MaterialTheme.typography.labelLarge,
              )
            }
          }

          HorizontalDivider()

          preview.items.forEachIndexed { idx, item ->
            val itemPalette = GradientPaletteRegistry.paletteForCard(item.title, idx)
            Surface(
              shape = RoundedCornerShape(10.dp),
              border = BorderStroke(1.dp, itemPalette.colors.first().copy(alpha = 0.35f)),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Row(
                modifier = Modifier.padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = "${item.category}: ${item.title}",
                    style = MaterialTheme.typography.labelLarge,
                  )
                  Text(
                    text = item.detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                Spacer(modifier = Modifier.size(8.dp))
                TintedPillTag(
                  text = item.action.label,
                  accentColor =
                    if (item.action == UpdateItemAction.ADD) updatePalette.colors[0]
                    else updatePalette.colors[1],
                )
              }
            }
          }

          Text(
            text =
              "Applying this pack preserves all employees, roster assignments, and attendance history.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    },
    confirmButton = {
      if (preview.isCompatible && pack != null) {
        Button(
          onClick = { onApply(pack) },
          modifier = Modifier.testTag("apply_offline_update_button"),
        ) {
          Text("Apply Atomically")
        }
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } },
  )
}

@Composable
fun ExitConfirmationDialog(
  hasUnsavedChanges: Boolean,
  onStay: () -> Unit,
  onConfirmExit: () -> Unit,
) {
  val exitPalette = GradientPaletteRegistry.paletteForCard("exit_confirmation", 14)
  val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
  val accentColor = exitPalette.colors.first()

  Dialog(onDismissRequest = onStay) {
    Surface(
      shape = RoundedCornerShape(20.dp),
      color = MaterialTheme.colorScheme.surface,
      border = BorderStroke(1.25.dp, accentColor.copy(alpha = if (isDark) 0.65f else 0.45f)),
      tonalElevation = 6.dp,
      modifier = Modifier.fillMaxWidth().testTag("exit_confirmation_dialog"),
    ) {
      Column(modifier = Modifier.fillMaxWidth()) {
        Box(
          modifier =
            Modifier.fillMaxWidth()
              .background(
                Brush.linearGradient(
                  colors = exitPalette.colors,
                  start = Offset.Zero,
                  end = Offset(820f, 320f),
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
                    colors = listOf(Color(0xA60B1020), Color(0x5E0B1020), Color(0x2E0B1020))
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
              modifier = Modifier.size(42.dp),
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.AutoMirrored.Outlined.ExitToApp,
                  contentDescription = null,
                  tint = Color.White,
                  modifier = Modifier.size(22.dp),
                )
              }
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
              Text(
                text = "Exit Bivash Index?",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
              )
              Text(
                text =
                  if (hasUnsavedChanges) "Unsaved staged edits are still open"
                  else "Offline workspace ready",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.90f),
              )
            }
          }
        }

        Column(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp),
          verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
          Text(
            text =
              if (hasUnsavedChanges) {
                "You have pending edits or confirmation prompts open. Exiting now will discard any unapplied selections."
              } else {
                "All saved records are stored safely offline on this device. Would you like to close the app?"
              },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            OutlinedButton(
              onClick = onStay,
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("exit_stay_button"),
            ) {
              Text("Stay", fontWeight = FontWeight.SemiBold)
            }
            Button(
              onClick = onConfirmExit,
              shape = RoundedCornerShape(12.dp),
              colors =
                ButtonDefaults.buttonColors(
                  containerColor = MaterialTheme.colorScheme.error,
                  contentColor = MaterialTheme.colorScheme.onError,
                ),
              modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("exit_confirm_button"),
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Outlined.ExitToApp,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text("Exit", fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }
  }
}
