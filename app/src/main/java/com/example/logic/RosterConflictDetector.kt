package com.example.logic

import com.example.models.AttendanceRecord
import com.example.models.BatchRosterAssignmentRequest
import com.example.models.Department
import com.example.models.DutyType
import com.example.models.Employee
import com.example.models.EmployeeCarryForward
import com.example.models.EmployeeScheduleGroup
import com.example.models.RosterApplyOutcome
import com.example.models.RosterAssignment
import com.example.models.RosterAssignmentRequest
import com.example.models.RosterConflict
import com.example.models.Shift
import com.example.models.UpcomingCoverageDay
import com.example.models.UpcomingCoverageEntry

object RosterConflictDetector {

  private data class CarryForwardCandidate(
    val employeeId: String,
    val date: String,
    val updatedAt: Long,
    val priority: Int,
    val departmentId: String?,
    val dutyTypeId: String?,
  )

  fun resolveCarryForwardDefaults(
    employees: List<Employee>,
    departments: List<Department>,
    duties: List<DutyType>,
    assignments: List<RosterAssignment>,
    attendanceRecords: List<AttendanceRecord> = emptyList(),
    beforeDate: String? = null,
  ): Map<String, EmployeeCarryForward> {
    val deptIds = departments.map { it.id }.toSet()
    val dutyMap = duties.associateBy { it.id }

    fun normalize(rawDeptId: String?, rawDutyId: String?): Pair<String?, String?> {
      val validDuty = rawDutyId?.takeIf { dutyMap.containsKey(it) }
      val inferredDept =
        rawDeptId?.takeIf { it in deptIds }
          ?: validDuty?.let { dId -> dutyMap[dId]?.departmentId?.takeIf { it in deptIds } }
      return inferredDept to validDuty
    }

    val candidatesByEmp = mutableMapOf<String, MutableList<CarryForwardCandidate>>()

    for (assign in assignments) {
      val (deptId, dutyId) = normalize(assign.departmentId, assign.dutyTypeId)
      if (deptId == null && dutyId == null) continue
      candidatesByEmp
        .getOrPut(assign.employeeId) { mutableListOf() }
        .add(
          CarryForwardCandidate(
            employeeId = assign.employeeId,
            date = assign.date,
            updatedAt = assign.updatedAt,
            priority = 2,
            departmentId = deptId,
            dutyTypeId = dutyId,
          )
        )
    }

    for (rec in attendanceRecords) {
      val (deptId, dutyId) = normalize(rec.departmentId, rec.dutyTypeId)
      if (deptId == null && dutyId == null) continue
      candidatesByEmp
        .getOrPut(rec.employeeId) { mutableListOf() }
        .add(
          CarryForwardCandidate(
            employeeId = rec.employeeId,
            date = rec.date,
            updatedAt = rec.updatedAt,
            priority = 1,
            departmentId = deptId,
            dutyTypeId = dutyId,
          )
        )
    }

    val result = mutableMapOf<String, EmployeeCarryForward>()
    val comparator =
      compareByDescending<CarryForwardCandidate> { it.date }
        .thenByDescending { it.updatedAt }
        .thenByDescending { it.priority }

    for (emp in employees) {
      val defaultDeptId = emp.defaultDepartmentId?.takeIf { it in deptIds }
      val list = candidatesByEmp[emp.id]
      if (list.isNullOrEmpty()) {
        if (defaultDeptId != null) {
          result[emp.id] =
            EmployeeCarryForward(
              employeeId = emp.id,
              departmentId = defaultDeptId,
              dutyTypeId = null,
              sourceDate = "default",
            )
        }
        continue
      }
      val pool =
        if (!beforeDate.isNullOrBlank()) {
          val earlier = list.filter { it.date < beforeDate }
          earlier.ifEmpty { list }
        } else {
          list
        }
      val best = pool.sortedWith(comparator).firstOrNull() ?: continue
      result[emp.id] =
        EmployeeCarryForward(
          employeeId = emp.id,
          departmentId = best.departmentId ?: defaultDeptId,
          dutyTypeId = best.dutyTypeId,
          sourceDate = best.date,
        )
    }
    return result
  }

  fun evaluateAndBuildAssignments(
    request: RosterAssignmentRequest,
    employees: List<Employee>,
    shifts: List<Shift>,
    duties: List<DutyType>,
    existingAssignments: List<RosterAssignment>,
    nowMillis: Long = System.currentTimeMillis(),
  ): RosterApplyOutcome {
    val targetDates = DateUtils.expandDateRange(request.startDate, request.endDate)
    if (targetDates.isEmpty() || request.employeeIds.isEmpty() || request.shiftId.isBlank()) {
      return RosterApplyOutcome(
        assignmentsToUpsert = emptyList(),
        conflictsFound = emptyList(),
        skippedDueToConflictCount = 0,
        appliedCount = 0,
        requiresConfirmation = false,
      )
    }

    val empMap = employees.associateBy { it.id }
    val shiftMap = shifts.associateBy { it.id }
    val dutyMap = duties.associateBy { it.id }
    val shiftName = shiftMap[request.shiftId]?.name ?: request.shiftId

    val existingByKey =
      existingAssignments.associateBy { "${it.date}_${it.employeeId}_${it.shiftId}" }

    val conflicts = mutableListOf<RosterConflict>()
    val toUpsert = mutableListOf<RosterAssignment>()
    var skipped = 0

    for (date in targetDates) {
      for (empId in request.employeeIds.distinct()) {
        val emp = empMap[empId] ?: continue
        val key = "${date}_${empId}_${request.shiftId}"
        val existing = existingByKey[key]

        if (existing != null) {
          conflicts.add(
            RosterConflict(
              date = date,
              employeeId = empId,
              employeeName = emp.name,
              employeeCode = emp.code,
              shiftId = request.shiftId,
              shiftName = shiftName,
              existingDutyTitle = existing.dutyTypeId?.let { dutyMap[it]?.title },
            )
          )
        }

        if (existing != null && !request.overwriteConflicts) {
          skipped++
          continue
        }

        val mergedDutyId = request.dutyTypeId ?: existing?.dutyTypeId
        val resolvedDeptId =
          request.departmentId?.takeIf { it.isNotBlank() }
            ?: request.dutyTypeId?.let { dId -> dutyMap[dId]?.departmentId?.takeIf { it.isNotBlank() } }
            ?: existing?.departmentId
            ?: mergedDutyId?.let { dId -> dutyMap[dId]?.departmentId?.takeIf { it.isNotBlank() } }
        val mergedNotes =
          if (request.notes.isNotBlank()) request.notes.trim() else existing?.notes.orEmpty()

        toUpsert.add(
          RosterAssignment(
            id = existing?.id ?: key,
            date = date,
            employeeId = empId,
            shiftId = request.shiftId,
            dutyTypeId = mergedDutyId,
            notes = mergedNotes,
            createdAt = existing?.createdAt ?: nowMillis,
            updatedAt = nowMillis,
            departmentId = resolvedDeptId,
          )
        )
      }
    }

    val needsConfirmation = conflicts.isNotEmpty() && !request.overwriteConflicts
    return RosterApplyOutcome(
      assignmentsToUpsert = if (needsConfirmation) emptyList() else toUpsert,
      conflictsFound = conflicts,
      skippedDueToConflictCount = skipped,
      appliedCount = if (needsConfirmation) 0 else toUpsert.size,
      requiresConfirmation = needsConfirmation,
    )
  }

  fun evaluateAndBuildBatchAssignments(
    request: BatchRosterAssignmentRequest,
    employees: List<Employee>,
    shifts: List<Shift>,
    duties: List<DutyType>,
    existingAssignments: List<RosterAssignment>,
    nowMillis: Long = System.currentTimeMillis(),
  ): RosterApplyOutcome {
    if (!DateUtils.isValidIsoDate(request.date) || request.batches.isEmpty()) {
      return RosterApplyOutcome(
        assignmentsToUpsert = emptyList(),
        conflictsFound = emptyList(),
        skippedDueToConflictCount = 0,
        appliedCount = 0,
        requiresConfirmation = false,
      )
    }

    val empMap = employees.associateBy { it.id }
    val shiftMap = shifts.associateBy { it.id }
    val dutyMap = duties.associateBy { it.id }
    val existingByKey =
      existingAssignments.associateBy { "${it.date}_${it.employeeId}_${it.shiftId}" }

    val conflicts = mutableListOf<RosterConflict>()
    val toUpsertMap = linkedMapOf<String, RosterAssignment>()
    var skipped = 0

    for (batch in request.batches) {
      if (batch.shiftId.isBlank() || batch.employeeIds.isEmpty()) continue
      val shiftName = shiftMap[batch.shiftId]?.name ?: batch.shiftId

      for (empId in batch.employeeIds.distinct()) {
        val emp = empMap[empId] ?: continue
        val key = "${request.date}_${empId}_${batch.shiftId}"
        val existing = existingByKey[key]

        if (existing != null) {
          conflicts.add(
            RosterConflict(
              date = request.date,
              employeeId = empId,
              employeeName = emp.name,
              employeeCode = emp.code,
              shiftId = batch.shiftId,
              shiftName = shiftName,
              existingDutyTitle = existing.dutyTypeId?.let { dutyMap[it]?.title },
            )
          )
        }

        if (existing != null && !request.overwriteConflicts) {
          skipped++
          continue
        }

        val mergedDutyId = batch.dutyTypeId ?: existing?.dutyTypeId
        val resolvedDeptId =
          batch.departmentId?.takeIf { it.isNotBlank() }
            ?: batch.dutyTypeId?.let { dId -> dutyMap[dId]?.departmentId?.takeIf { it.isNotBlank() } }
            ?: existing?.departmentId
            ?: mergedDutyId?.let { dId -> dutyMap[dId]?.departmentId?.takeIf { it.isNotBlank() } }
        val mergedNotes =
          if (batch.notes.isNotBlank()) batch.notes.trim() else existing?.notes.orEmpty()

        toUpsertMap[key] =
          RosterAssignment(
            id = existing?.id ?: key,
            date = request.date,
            employeeId = empId,
            shiftId = batch.shiftId,
            dutyTypeId = mergedDutyId,
            notes = mergedNotes,
            createdAt = existing?.createdAt ?: nowMillis,
            updatedAt = nowMillis,
            departmentId = resolvedDeptId,
          )
      }
    }

    val needsConfirmation = conflicts.isNotEmpty() && !request.overwriteConflicts
    val toUpsert = toUpsertMap.values.toList()
    return RosterApplyOutcome(
      assignmentsToUpsert = if (needsConfirmation) emptyList() else toUpsert,
      conflictsFound = conflicts,
      skippedDueToConflictCount = skipped,
      appliedCount = if (needsConfirmation) 0 else toUpsert.size,
      requiresConfirmation = needsConfirmation,
    )
  }

  fun buildUpcomingCoverage(
    startDate: String,
    dayCount: Int,
    employees: List<Employee>,
    shifts: List<Shift>,
    duties: List<DutyType>,
    assignments: List<RosterAssignment>,
    departments: List<Department> = emptyList(),
  ): List<UpcomingCoverageDay> {
    val activeEmployees = employees.filter { it.isActive }.sortedBy { it.code }
    val empMap = employees.associateBy { it.id }
    val shiftMap = shifts.associateBy { it.id }
    val dutyMap = duties.associateBy { it.id }
    val deptMap = departments.associateBy { it.id }
    val assignmentsByDate = assignments.groupBy { it.date }

    return (0 until dayCount).map { offset ->
      val date = DateUtils.addDays(startDate, offset)
      val dayAssignments = assignmentsByDate[date].orEmpty()
      val assignedEmpIds = dayAssignments.map { it.employeeId }.toSet()

      val entries =
        dayAssignments
          .mapNotNull { assignment ->
            val emp = empMap[assignment.employeeId] ?: return@mapNotNull null
            val dutyObj = assignment.dutyTypeId?.let { dutyMap[it] }
            val deptId =
              assignment.departmentId?.takeIf { it.isNotBlank() }
                ?: dutyObj?.departmentId?.takeIf { it.isNotBlank() }
            UpcomingCoverageEntry(
              assignment = assignment,
              employee = emp,
              shift = shiftMap[assignment.shiftId],
              duty = dutyObj,
              department = deptId?.let { deptMap[it] },
            )
          }
          .sortedWith(compareBy({ it.shift?.startTime ?: "99:99" }, { it.employee.code }))

      val unassigned = activeEmployees.filter { it.id !in assignedEmpIds }
      val coveredActive = activeEmployees.count { it.id in assignedEmpIds }

      UpcomingCoverageDay(
        date = date,
        totalActiveEmployees = activeEmployees.size,
        coveredEmployeesCount = coveredActive,
        totalShiftSlotsCount = entries.size,
        unassignedEmployeesCount = unassigned.size,
        entries = entries,
        unassignedEmployees = unassigned,
      )
    }
  }

  fun buildEmployeeSchedules(
    fromDate: String,
    employees: List<Employee>,
    shifts: List<Shift>,
    duties: List<DutyType>,
    assignments: List<RosterAssignment>,
    departments: List<Department> = emptyList(),
  ): List<EmployeeScheduleGroup> {
    val shiftMap = shifts.associateBy { it.id }
    val dutyMap = duties.associateBy { it.id }
    val deptMap = departments.associateBy { it.id }
    val activeEmployees = employees.filter { it.isActive }.sortedBy { it.code }
    val upcomingByEmployee =
      assignments
        .filter { it.date >= fromDate }
        .groupBy { it.employeeId }

    return activeEmployees.map { emp ->
      val entries =
        upcomingByEmployee[emp.id]
          .orEmpty()
          .map { assignment ->
            val dutyObj = assignment.dutyTypeId?.let { dutyMap[it] }
            val deptId =
              assignment.departmentId?.takeIf { it.isNotBlank() }
                ?: dutyObj?.departmentId?.takeIf { it.isNotBlank() }
            UpcomingCoverageEntry(
              assignment = assignment,
              employee = emp,
              shift = shiftMap[assignment.shiftId],
              duty = dutyObj,
              department = deptId?.let { deptMap[it] },
            )
          }
          .sortedWith(compareBy({ it.assignment.date }, { it.shift?.startTime ?: "99:99" }))
      EmployeeScheduleGroup(
        employee = emp,
        upcomingEntries = entries,
      )
    }
  }
}
