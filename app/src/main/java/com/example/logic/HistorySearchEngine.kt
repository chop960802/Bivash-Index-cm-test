package com.example.logic

import com.example.models.AttendanceRecord
import com.example.models.Department
import com.example.models.DutyType
import com.example.models.Employee
import com.example.models.HistoryFilterQuery
import com.example.models.HistorySearchEntry
import com.example.models.Shift

object HistorySearchEngine {

  fun search(
    query: HistoryFilterQuery,
    attendanceRecords: List<AttendanceRecord>,
    employees: List<Employee>,
    shifts: List<Shift>,
    duties: List<DutyType>,
    departments: List<Department> = emptyList(),
  ): List<HistorySearchEntry> {
    val empMap = employees.associateBy { it.id }
    val shiftMap = shifts.associateBy { it.id }
    val dutyMap = duties.associateBy { it.id }
    val deptMap = departments.associateBy { it.id }
    val needle = query.searchText.trim().lowercase()

    return attendanceRecords
      .asSequence()
      .filter { rec ->
        rec.date in query.startDate..query.endDate &&
          (query.employeeId == null || rec.employeeId == query.employeeId) &&
          (query.shiftId == null || rec.shiftId == query.shiftId) &&
          (query.dutyTypeId == null || rec.dutyTypeId == query.dutyTypeId) &&
          (query.status == null || rec.status == query.status)
      }
      .map { rec ->
        val emp = empMap[rec.employeeId]
        val shift = if (rec.shiftId.isNotBlank()) shiftMap[rec.shiftId] else null
        val duty = rec.dutyTypeId?.let { dutyMap[it] }
        val effectiveDeptId =
          rec.departmentId?.takeIf { it.isNotBlank() }
            ?: duty?.departmentId?.takeIf { it.isNotBlank() }
        val dept = effectiveDeptId?.let { deptMap[it] }

        val code = emp?.code ?: rec.employeeCodeSnapshot.ifBlank { "DELETED" }
        val name = emp?.name ?: rec.employeeNameSnapshot.ifBlank { "Removed Employee" }
        val designation = emp?.designation ?: rec.designationSnapshot.ifBlank { "—" }
        val shiftName =
          shift?.name
            ?: rec.shiftNameSnapshot.ifBlank {
              if (rec.shiftId.isBlank()) "Unscheduled" else "Removed Shift"
            }
        val isNight = shift?.isNightShiftVisual() ?: rec.isNightShiftSnapshot
        val dutyTitle = duty?.title ?: rec.dutyTitleSnapshot.ifBlank { "No Duty" }
        val deptName = dept?.name ?: rec.departmentNameSnapshot.ifBlank { "—" }
        val isDeletedRef =
          emp == null ||
            (rec.shiftId.isNotBlank() && shift == null) ||
            (rec.dutyTypeId != null && duty == null) ||
            (effectiveDeptId != null && dept == null)

        HistorySearchEntry(
          id = rec.id,
          date = rec.date,
          employeeCode = code,
          employeeName = name,
          designation = designation,
          shiftName = shiftName,
          isNightShift = isNight,
          dutyTitle = dutyTitle,
          status = rec.status,
          remarks = rec.remarks,
          isDeletedReference = isDeletedRef,
          departmentId = effectiveDeptId,
          departmentName = deptName,
        )
      }
      .filter { entry ->
        (query.departmentId == null || entry.departmentId == query.departmentId) &&
          (query.designation == null || entry.designation.equals(query.designation, ignoreCase = true)) &&
          (needle.isBlank() ||
            entry.employeeCode.lowercase().contains(needle) ||
            entry.employeeName.lowercase().contains(needle) ||
            entry.designation.lowercase().contains(needle) ||
            entry.departmentName.lowercase().contains(needle) ||
            entry.shiftName.lowercase().contains(needle) ||
            entry.dutyTitle.lowercase().contains(needle) ||
            entry.remarks.lowercase().contains(needle))
      }
      .sortedWith(compareByDescending<HistorySearchEntry> { it.date }.thenBy { it.employeeCode })
      .toList()
  }
}
