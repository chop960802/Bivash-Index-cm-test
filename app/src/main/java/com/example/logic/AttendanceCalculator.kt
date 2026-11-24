package com.example.logic

import com.example.models.AttendanceRecord
import com.example.models.AttendanceStatus
import com.example.models.CalendarDateBreakdown
import com.example.models.CalendarDayCell
import com.example.models.CalendarDepartmentGroup
import com.example.models.CalendarDutyGroup
import com.example.models.CalendarShiftGroup
import com.example.models.DailyAttendanceRow
import com.example.models.DailyAttendanceSummary
import com.example.models.Department
import com.example.models.DutyType
import com.example.models.Employee
import com.example.models.RosterAssignment
import com.example.models.Shift

object AttendanceCalculator {

  const val AUTO_OFF_REMARK = "Auto-OFF (no shift assigned)"

  data class AutoOffSyncResult(
    val recordsToUpsert: List<AttendanceRecord>,
    val staleAutoOffIdsToDelete: List<String>,
  )

  fun buildAutoOffSyncForDate(
    date: String,
    employees: List<Employee>,
    assignmentsForDate: List<RosterAssignment>,
    existingAttendanceForDate: List<AttendanceRecord>,
    nowMillis: Long = System.currentTimeMillis(),
  ): AutoOffSyncResult {
    if (!DateUtils.isValidIsoDate(date) || assignmentsForDate.isEmpty()) {
      return AutoOffSyncResult(emptyList(), emptyList())
    }
    val assignedEmployeeIds = assignmentsForDate.map { it.employeeId }.toSet()
    val attendanceByEmp = existingAttendanceForDate.groupBy { it.employeeId }

    val toUpsert = mutableListOf<AttendanceRecord>()
    val staleIds = mutableListOf<String>()

    for (emp in employees) {
      if (!emp.isActive) continue
      val empRecords = attendanceByEmp[emp.id].orEmpty()
      val unscheduledRecord = empRecords.firstOrNull { it.shiftId.isBlank() }

      if (emp.id in assignedEmployeeIds) {
        if (
          unscheduledRecord != null &&
            unscheduledRecord.status == AttendanceStatus.OFF &&
            unscheduledRecord.remarks == AUTO_OFF_REMARK
        ) {
          staleIds.add(unscheduledRecord.id)
        }
      } else {
        if (empRecords.isEmpty()) {
          toUpsert.add(
            AttendanceRecord(
              id = "${date}_${emp.id}_",
              date = date,
              employeeId = emp.id,
              shiftId = "",
              dutyTypeId = null,
              status = AttendanceStatus.OFF,
              remarks = AUTO_OFF_REMARK,
              employeeCodeSnapshot = emp.code,
              employeeNameSnapshot = emp.name,
              designationSnapshot = emp.designation,
              shiftNameSnapshot = "Unscheduled",
              isNightShiftSnapshot = false,
              dutyTitleSnapshot = "",
              markedAt = nowMillis,
              updatedAt = nowMillis,
              departmentId = null,
              departmentNameSnapshot = "",
            )
          )
        }
      }
    }
    return AutoOffSyncResult(recordsToUpsert = toUpsert, staleAutoOffIdsToDelete = staleIds)
  }

  fun buildDailyRows(
    date: String,
    employees: List<Employee>,
    shifts: List<Shift>,
    duties: List<DutyType>,
    assignmentsForDate: List<RosterAssignment>,
    attendanceForDate: List<AttendanceRecord>,
    filterShiftId: String? = null,
    departments: List<Department> = emptyList(),
  ): List<DailyAttendanceRow> {
    val shiftMap = shifts.associateBy { it.id }
    val dutyMap = duties.associateBy { it.id }
    val deptMap = departments.associateBy { it.id }
    val assignmentsByEmp = assignmentsForDate.groupBy { it.employeeId }
    val attendanceByEmp = attendanceForDate.groupBy { it.employeeId }

    val relevantEmployees =
      employees
        .filter { emp ->
          emp.isActive ||
            assignmentsByEmp.containsKey(emp.id) ||
            attendanceByEmp.containsKey(emp.id)
        }
        .sortedWith(compareBy<Employee> { !it.isActive }.thenBy { it.code }.thenBy { it.name })

    val rows = mutableListOf<DailyAttendanceRow>()

    for (emp in relevantEmployees) {
      val empAssignments = assignmentsByEmp[emp.id].orEmpty()
      val empAttendance = attendanceByEmp[emp.id].orEmpty()
      val assignmentByShift = empAssignments.associateBy { it.shiftId }
      val attendanceByShift = empAttendance.associateBy { it.shiftId }

      val shiftKeys = (assignmentByShift.keys + attendanceByShift.keys).distinct()

      if (shiftKeys.isEmpty()) {
        rows.add(
          DailyAttendanceRow(
            rowKey = "${date}_${emp.id}_",
            employee = emp,
            shift = null,
            shiftId = "",
            duty = null,
            existingRecord = null,
            isScheduledForDate = false,
            isNightShift = false,
            department = null,
          )
        )
      } else {
        val sortedShiftKeys =
          shiftKeys.sortedWith(compareBy({ shiftMap[it]?.startTime ?: "99:99" }, { it }))
        for (sId in sortedShiftKeys) {
          val assignment = assignmentByShift[sId]
          val existing = attendanceByShift[sId]
          val shiftObj = if (sId.isNotBlank()) shiftMap[sId] else null
          val dutyId = existing?.dutyTypeId ?: assignment?.dutyTypeId
          val dutyObj = dutyId?.let { dutyMap[it] }
          val deptId =
            existing?.departmentId?.takeIf { it.isNotBlank() }
              ?: assignment?.departmentId?.takeIf { it.isNotBlank() }
              ?: dutyObj?.departmentId?.takeIf { it.isNotBlank() }
          val deptObj = deptId?.let { deptMap[it] }
          val isNight =
            shiftObj?.isNightShiftVisual() ?: existing?.isNightShiftSnapshot ?: false

          rows.add(
            DailyAttendanceRow(
              rowKey = "${date}_${emp.id}_${sId}",
              employee = emp,
              shift = shiftObj,
              shiftId = sId,
              duty = dutyObj,
              existingRecord = existing,
              isScheduledForDate = assignment != null,
              isNightShift = isNight,
              department = deptObj,
            )
          )
        }
      }
    }

    return if (filterShiftId.isNullOrBlank()) {
      rows
    } else {
      rows.filter { it.shiftId == filterShiftId }
    }
  }

  fun calculateSummary(
    date: String,
    employees: List<Employee>,
    assignmentsForDate: List<RosterAssignment>,
    attendanceForDate: List<AttendanceRecord>,
  ): DailyAttendanceSummary {
    val activeEmployees = employees.filter { it.isActive }
    val activeIds = activeEmployees.map { it.id }.toSet()

    val assignmentKeys =
      assignmentsForDate.map { "${it.employeeId}_${it.shiftId}" }.toSet()
    val attendanceByKey =
      attendanceForDate.associateBy { "${it.employeeId}_${it.shiftId}" }

    val allSlotKeys = (assignmentKeys + attendanceByKey.keys).toMutableSet()
    val employeesWithSlot =
      (assignmentsForDate.map { it.employeeId } + attendanceForDate.map { it.employeeId }).toSet()

    val unscheduledActiveCount = activeIds.count { it !in employeesWithSlot }

    var present = 0
    var absent = 0
    var late = 0
    var leave = 0
    var off = 0
    var unmarked = unscheduledActiveCount

    for (key in allSlotKeys) {
      val rec = attendanceByKey[key]
      when (rec?.status) {
        AttendanceStatus.PRESENT -> present++
        AttendanceStatus.ABSENT -> absent++
        AttendanceStatus.LATE -> late++
        AttendanceStatus.LEAVE -> leave++
        AttendanceStatus.OFF -> off++
        null -> unmarked++
      }
    }

    return DailyAttendanceSummary(
      date = date,
      totalActiveEmployees = activeEmployees.size,
      scheduledSlotsCount = assignmentsForDate.size,
      presentCount = present,
      absentCount = absent,
      lateCount = late,
      leaveCount = leave,
      offCount = off,
      unmarkedSlotsCount = unmarked,
    )
  }

  fun createUpsertedRecord(
    date: String,
    row: DailyAttendanceRow,
    status: AttendanceStatus,
    remarks: String,
    nowMillis: Long = System.currentTimeMillis(),
  ): AttendanceRecord {
    val existing = row.existingRecord
    val shiftName =
      row.shift?.name
        ?: existing?.shiftNameSnapshot?.takeIf { it.isNotBlank() }
        ?: if (row.shiftId.isBlank()) "Unscheduled" else row.shiftId
    val dutyTitle =
      row.duty?.title ?: existing?.dutyTitleSnapshot.orEmpty()
    val deptId =
      row.department?.id
        ?: row.duty?.departmentId?.takeIf { it.isNotBlank() }
        ?: existing?.departmentId
    val deptName =
      row.department?.name
        ?: existing?.departmentNameSnapshot.orEmpty()

    return AttendanceRecord(
      id = existing?.id ?: "${date}_${row.employee.id}_${row.shiftId}",
      date = date,
      employeeId = row.employee.id,
      shiftId = row.shiftId,
      dutyTypeId = row.duty?.id ?: existing?.dutyTypeId,
      status = status,
      remarks = remarks.trim(),
      employeeCodeSnapshot = row.employee.code,
      employeeNameSnapshot = row.employee.name,
      designationSnapshot = row.employee.designation,
      shiftNameSnapshot = shiftName,
      isNightShiftSnapshot = row.isNightShift,
      dutyTitleSnapshot = dutyTitle,
      markedAt = existing?.markedAt ?: nowMillis,
      updatedAt = nowMillis,
      departmentId = deptId,
      departmentNameSnapshot = deptName,
    )
  }

  fun buildBulkMarkPresentRecords(
    date: String,
    rows: List<DailyAttendanceRow>,
    onlyScheduled: Boolean,
    nowMillis: Long = System.currentTimeMillis(),
  ): List<AttendanceRecord> {
    return rows
      .filter { row ->
        row.employee.isActive &&
          row.existingRecord == null &&
          (!onlyScheduled || row.isScheduledForDate)
      }
      .map { row ->
        createUpsertedRecord(
          date = date,
          row = row,
          status = AttendanceStatus.PRESENT,
          remarks = "",
          nowMillis = nowMillis,
        )
      }
  }

  fun buildCalendarMonthCells(
    year: Int,
    month1Based: Int,
    employees: List<Employee>,
    shifts: List<Shift>,
    allRoster: List<RosterAssignment>,
    allAttendance: List<AttendanceRecord>,
  ): List<CalendarDayCell> {
    val rosterByDate = allRoster.groupBy { it.date }
    val attendanceByDate = allAttendance.groupBy { it.date }
    val shiftMap = shifts.associateBy { it.id }

    return DateUtils.buildCalendarMonthGrid(year, month1Based).map { (dateIso, dayOfMonth, inMonth) ->
      val dayRoster = rosterByDate[dateIso].orEmpty()
      val dayAttendance = attendanceByDate[dateIso].orEmpty()
      val summary =
        calculateSummary(
          date = dateIso,
          employees = employees,
          assignmentsForDate = dayRoster,
          attendanceForDate = dayAttendance,
        )
      val hasNightShift =
        dayRoster.any { shiftMap[it.shiftId]?.isNightShiftVisual() == true } ||
          dayAttendance.any {
            it.isNightShiftSnapshot || shiftMap[it.shiftId]?.isNightShiftVisual() == true
          }
      CalendarDayCell(
        dateIso = dateIso,
        dayOfMonth = dayOfMonth,
        isInCurrentMonth = inMonth,
        summary = summary,
        hasNightShift = hasNightShift,
      )
    }
  }

  fun buildCalendarDateBreakdown(rows: List<DailyAttendanceRow>): CalendarDateBreakdown {
    val absenceStatuses =
      setOf(AttendanceStatus.OFF, AttendanceStatus.LEAVE, AttendanceStatus.ABSENT)
    val offOrAbsenceRows = mutableListOf<DailyAttendanceRow>()
    val unmarkedUnscheduledRows = mutableListOf<DailyAttendanceRow>()
    val workingRows = mutableListOf<DailyAttendanceRow>()

    for (row in rows) {
      val status = row.existingRecord?.status
      when {
        status in absenceStatuses -> offOrAbsenceRows.add(row)
        !row.isScheduledForDate && status == null && row.shiftId.isBlank() ->
          unmarkedUnscheduledRows.add(row)
        else -> workingRows.add(row)
      }
    }

    val shiftGroups =
      workingRows
        .groupBy { it.shiftId.ifBlank { "__unscheduled__" } }
        .entries
        .map { (shiftKey, shiftRows) ->
          val sample = shiftRows.first()
          val shiftName =
            sample.shift?.name
              ?: sample.existingRecord?.shiftNameSnapshot?.ifBlank { null }
              ?: if (shiftKey == "__unscheduled__") "Open / Unscheduled" else shiftKey
          val timeWindow = sample.shift?.let { "${it.startTime}–${it.endTime}" } ?: "Flexible"
          val isNight = shiftRows.any { it.isNightShift }
          val startSort = sample.shift?.startTime ?: "99:99"

          val deptGroups =
            shiftRows
              .groupBy { row ->
                row.department?.id
                  ?: row.duty?.departmentId?.takeIf { it.isNotBlank() }
                  ?: row.existingRecord?.departmentId?.takeIf { it.isNotBlank() }
                  ?: "__no_dept__"
              }
              .entries
              .map { (deptKey, deptRows) ->
                val dSample = deptRows.first()
                val deptName =
                  dSample.department?.name
                    ?: dSample.existingRecord?.departmentNameSnapshot?.ifBlank { null }
                    ?: "General / Unassigned Dept"

                val dutyGroups =
                  deptRows
                    .groupBy { r ->
                      r.duty?.id ?: r.existingRecord?.dutyTypeId ?: "__no_duty__"
                    }
                    .entries
                    .map { (dutyKey, dutyRows) ->
                      val duSample = dutyRows.first()
                      val dutyTitle =
                        (duSample.duty?.title ?: duSample.existingRecord?.dutyTitleSnapshot.orEmpty())
                          .ifBlank { "General Duty" }
                      CalendarDutyGroup(
                        dutyKey = dutyKey,
                        dutyTitle = dutyTitle,
                        rows = dutyRows.sortedBy { it.employee.code },
                      )
                    }
                    .sortedBy { it.dutyTitle }

                CalendarDepartmentGroup(
                  departmentKey = deptKey,
                  departmentName = deptName,
                  dutyGroups = dutyGroups,
                  totalCount = deptRows.size,
                )
              }
              .sortedBy { it.departmentName }

          (startSort to
            CalendarShiftGroup(
              shiftKey = shiftKey,
              shiftName = shiftName,
              timeWindow = timeWindow,
              isNightShift = isNight,
              departmentGroups = deptGroups,
              totalCount = shiftRows.size,
            ))
        }
        .sortedWith(compareBy({ it.first }, { it.second.shiftName }))
        .map { it.second }

    return CalendarDateBreakdown(
      workingShiftGroups = shiftGroups,
      offOrAbsenceRows = offOrAbsenceRows.sortedBy { it.employee.code },
      unmarkedUnscheduledRows = unmarkedUnscheduledRows.sortedBy { it.employee.code },
    )
  }
}
