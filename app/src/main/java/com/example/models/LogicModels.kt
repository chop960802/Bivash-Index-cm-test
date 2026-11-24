package com.example.models

data class DailyAttendanceSummary(
  val date: String,
  val totalActiveEmployees: Int,
  val scheduledSlotsCount: Int,
  val presentCount: Int,
  val absentCount: Int,
  val lateCount: Int,
  val leaveCount: Int,
  val offCount: Int,
  val unmarkedSlotsCount: Int,
)

data class DailyAttendanceRow(
  val rowKey: String,
  val employee: Employee,
  val shift: Shift?,
  val shiftId: String,
  val duty: DutyType?,
  val existingRecord: AttendanceRecord?,
  val isScheduledForDate: Boolean,
  val isNightShift: Boolean,
  val department: Department? = null,
)

data class RosterConflict(
  val date: String,
  val employeeId: String,
  val employeeName: String,
  val employeeCode: String,
  val shiftId: String,
  val shiftName: String,
  val existingDutyTitle: String?,
)

data class RosterAssignmentRequest(
  val employeeIds: List<String>,
  val startDate: String,
  val endDate: String,
  val shiftId: String,
  val dutyTypeId: String?,
  val notes: String,
  val overwriteConflicts: Boolean,
  val departmentId: String? = null,
)

data class RosterApplyOutcome(
  val assignmentsToUpsert: List<RosterAssignment>,
  val conflictsFound: List<RosterConflict>,
  val skippedDueToConflictCount: Int,
  val appliedCount: Int,
  val requiresConfirmation: Boolean,
)

data class UpcomingCoverageEntry(
  val assignment: RosterAssignment,
  val employee: Employee,
  val shift: Shift?,
  val duty: DutyType?,
  val department: Department? = null,
)

data class UpcomingCoverageDay(
  val date: String,
  val totalActiveEmployees: Int,
  val coveredEmployeesCount: Int,
  val totalShiftSlotsCount: Int,
  val unassignedEmployeesCount: Int,
  val entries: List<UpcomingCoverageEntry>,
  val unassignedEmployees: List<Employee>,
)

data class EmployeeScheduleGroup(
  val employee: Employee,
  val upcomingEntries: List<UpcomingCoverageEntry>,
)

data class EmployeeCarryForward(
  val employeeId: String,
  val departmentId: String?,
  val dutyTypeId: String?,
  val sourceDate: String,
)

data class HistoryFilterQuery(
  val startDate: String,
  val endDate: String,
  val searchText: String = "",
  val employeeId: String? = null,
  val designation: String? = null,
  val shiftId: String? = null,
  val dutyTypeId: String? = null,
  val status: AttendanceStatus? = null,
  val departmentId: String? = null,
)

data class HistorySearchEntry(
  val id: String,
  val date: String,
  val employeeCode: String,
  val employeeName: String,
  val designation: String,
  val shiftName: String,
  val isNightShift: Boolean,
  val dutyTitle: String,
  val status: AttendanceStatus,
  val remarks: String,
  val isDeletedReference: Boolean,
  val departmentId: String? = null,
  val departmentName: String = "—",
)

data class BatchShiftAssignmentItem(
  val shiftId: String,
  val employeeIds: List<String>,
  val dutyTypeId: String?,
  val notes: String,
  val departmentId: String? = null,
)

data class BatchRosterAssignmentRequest(
  val date: String,
  val batches: List<BatchShiftAssignmentItem>,
  val overwriteConflicts: Boolean = false,
)

enum class AttendanceDisplayMode(val label: String) {
  DAILY_LIST("Register"),
  CALENDAR("Calendar"),
}

data class CalendarDayCell(
  val dateIso: String,
  val dayOfMonth: Int,
  val isInCurrentMonth: Boolean,
  val summary: DailyAttendanceSummary,
  val hasNightShift: Boolean,
)

data class CalendarDutyGroup(
  val dutyKey: String,
  val dutyTitle: String,
  val rows: List<DailyAttendanceRow>,
)

data class CalendarDepartmentGroup(
  val departmentKey: String,
  val departmentName: String,
  val dutyGroups: List<CalendarDutyGroup>,
  val totalCount: Int,
)

data class CalendarShiftGroup(
  val shiftKey: String,
  val shiftName: String,
  val timeWindow: String,
  val isNightShift: Boolean,
  val departmentGroups: List<CalendarDepartmentGroup>,
  val totalCount: Int,
)

data class CalendarDateBreakdown(
  val workingShiftGroups: List<CalendarShiftGroup>,
  val offOrAbsenceRows: List<DailyAttendanceRow>,
  val unmarkedUnscheduledRows: List<DailyAttendanceRow>,
)

enum class UniversalSearchModule(val label: String) {
  ALL("All"),
  ATTENDANCE("Attendance"),
  ROSTER("Roster"),
  EMPLOYEES("Employees"),
  SHIFTS_DUTIES("Shifts & Duties"),
  ICT_DATA("ICT Data"),
  OFFICE_INDEX("Office Index"),
  TASKS("Tasks"),
  NOTES("Notes"),
}

data class UniversalSearchQuery(
  val module: UniversalSearchModule = UniversalSearchModule.ALL,
  val startDate: String? = null,
  val endDate: String? = null,
  val searchText: String = "",
  val employeeId: String? = null,
  val designation: String? = null,
  val shiftId: String? = null,
  val nightShiftOnly: Boolean = false,
  val departmentId: String? = null,
  val dutyTypeId: String? = null,
  val status: AttendanceStatus? = null,
  val ictCategoryId: String? = null,
  val ictSubCategoryId: String? = null,
  val officeCategory: String? = null,
  val officeSubCategory: String? = null,
  val taskCategoryId: String? = null,
  val taskCompletedFilter: Boolean? = null,
) {
  val useDateRange: Boolean
    get() = startDate != null || endDate != null

  val activeFilterCount: Int
    get() =
      listOfNotNull(
          module.takeIf { it != UniversalSearchModule.ALL },
          startDate ?: endDate,
          employeeId,
          designation,
          shiftId,
          true.takeIf { nightShiftOnly },
          departmentId,
          dutyTypeId,
          status,
          ictCategoryId,
          ictSubCategoryId,
          officeCategory,
          officeSubCategory,
          taskCategoryId,
          taskCompletedFilter,
        )
        .size
}

data class UniversalSearchResult(
  val id: String,
  val module: UniversalSearchModule,
  val date: String,
  val title: String,
  val subtitle: String,
  val detailLine: String = "",
  val valueOrBadge: String = "",
  val tags: List<String> = emptyList(),
  val attendanceStatus: AttendanceStatus? = null,
  val isNightShift: Boolean = false,
  val isDeletedReference: Boolean = false,
  val targetCategoryId: String? = null,
)

data class UniversalSearchFacets(
  val designations: List<String> = emptyList(),
  val officeCategories: List<String> = emptyList(),
  val officeSubCategories: List<String> = emptyList(),
)

