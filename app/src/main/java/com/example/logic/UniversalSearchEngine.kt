package com.example.logic

import com.example.models.AttendanceRecord
import com.example.models.Department
import com.example.models.DutyType
import com.example.models.Employee
import com.example.models.IctCategory
import com.example.models.IctEntry
import com.example.models.IctSubCategory
import com.example.models.NoteEntry
import com.example.models.OfficeCaseRecord
import com.example.models.OfficeIndexEntry
import com.example.models.RegisteredOffice
import com.example.models.RosterAssignment
import com.example.models.Shift
import com.example.models.TaskCategory
import com.example.models.TaskItem
import com.example.models.UniversalSearchFacets
import com.example.models.UniversalSearchModule
import com.example.models.UniversalSearchQuery
import com.example.models.UniversalSearchResult

object UniversalSearchEngine {

  data class Output(
    val results: List<UniversalSearchResult>,
    val facets: UniversalSearchFacets,
  )

  fun search(
    query: UniversalSearchQuery,
    employees: List<Employee>,
    shifts: List<Shift>,
    departments: List<Department>,
    duties: List<DutyType>,
    rosterAssignments: List<RosterAssignment>,
    attendanceRecords: List<AttendanceRecord>,
    ictCategories: List<IctCategory>,
    ictSubCategories: List<IctSubCategory>,
    ictEntries: List<IctEntry>,
    officeEntries: List<OfficeIndexEntry>,
    taskCategories: List<TaskCategory> = emptyList(),
    taskItems: List<TaskItem> = emptyList(),
    notes: List<NoteEntry> = emptyList(),
    registeredOffices: List<RegisteredOffice> = emptyList(),
    officeCaseRecords: List<OfficeCaseRecord> = emptyList(),
  ): Output {
    val empMap = employees.associateBy { it.id }
    val shiftMap = shifts.associateBy { it.id }
    val deptMap = departments.associateBy { it.id }
    val dutyMap = duties.associateBy { it.id }
    val ictCatMap = ictCategories.associateBy { it.id }
    val ictSubMap = ictSubCategories.associateBy { it.id }
    val taskCatMap = taskCategories.associateBy { it.id }
    val taskItemsByCat = taskItems.groupBy { it.categoryId }

    val (normStart, normEnd) = DateUtils.normalizeDateRange(query.startDate, query.endDate)
    fun matchesDate(dateIso: String): Boolean {
      if (normStart != null && dateIso < normStart) return false
      if (normEnd != null && dateIso > normEnd) return false
      return true
    }

    val needle = query.searchText.trim().lowercase()
    val hasEmployeeFilters = query.employeeId != null || query.designation != null
    val hasShiftFilters = query.shiftId != null || query.nightShiftOnly
    val hasOrgFilters = query.departmentId != null || query.dutyTypeId != null
    val hasAttendanceFilters = hasEmployeeFilters || hasShiftFilters || hasOrgFilters
    val hasStatusFilter = query.status != null
    val hasIctFilters = query.ictCategoryId != null || query.ictSubCategoryId != null
    val hasOfficeFilters = query.officeCategory != null || query.officeSubCategory != null
    val hasTaskFilters = query.taskCategoryId != null || query.taskCompletedFilter != null

    val results = mutableListOf<UniversalSearchResult>()

    // 1. Attendance records
    if (
      (query.module == UniversalSearchModule.ALL ||
        query.module == UniversalSearchModule.ATTENDANCE) &&
        !hasIctFilters &&
        !hasOfficeFilters &&
        !hasTaskFilters
    ) {
      attendanceRecords.forEach { rec ->
        if (!matchesDate(rec.date)) return@forEach
        if (query.employeeId != null && rec.employeeId != query.employeeId) return@forEach
        if (query.shiftId != null && rec.shiftId != query.shiftId) return@forEach
        if (query.dutyTypeId != null && rec.dutyTypeId != query.dutyTypeId) return@forEach
        if (query.status != null && rec.status != query.status) return@forEach

        val emp = empMap[rec.employeeId]
        val shift = if (rec.shiftId.isNotBlank()) shiftMap[rec.shiftId] else null
        val duty = rec.dutyTypeId?.let { dutyMap[it] }
        val effectiveDeptId =
          rec.departmentId?.takeIf { it.isNotBlank() }
            ?: duty?.departmentId?.takeIf { it.isNotBlank() }
        val dept = effectiveDeptId?.let { deptMap[it] }

        if (query.departmentId != null && effectiveDeptId != query.departmentId) return@forEach

        val code = emp?.code ?: rec.employeeCodeSnapshot.ifBlank { "DELETED" }
        val name = emp?.name ?: rec.employeeNameSnapshot.ifBlank { "Removed Employee" }
        val designation = emp?.designation ?: rec.designationSnapshot.ifBlank { "—" }
        if (
          query.designation != null && !designation.equals(query.designation, ignoreCase = true)
        ) {
          return@forEach
        }

        val shiftName =
          shift?.name
            ?: rec.shiftNameSnapshot.ifBlank {
              if (rec.shiftId.isBlank()) "Unscheduled" else "Removed Shift"
            }
        val isNight = shift?.isNightShiftVisual() ?: rec.isNightShiftSnapshot
        if (query.nightShiftOnly && !isNight) return@forEach

        val dutyTitle = duty?.title ?: rec.dutyTitleSnapshot.ifBlank { "No Duty" }
        val deptName = dept?.name ?: rec.departmentNameSnapshot.ifBlank { "—" }

        val matchesText =
          needle.isBlank() ||
            listOf(
                rec.date,
                code,
                name,
                designation,
                shiftName,
                deptName,
                dutyTitle,
                rec.status.label,
                rec.remarks,
              )
              .any { it.lowercase().contains(needle) }

        if (!matchesText) return@forEach

        val isDeletedRef =
          emp == null ||
            (rec.shiftId.isNotBlank() && shift == null) ||
            (rec.dutyTypeId != null && duty == null) ||
            (effectiveDeptId != null && dept == null)

        results.add(
          UniversalSearchResult(
            id = "att_${rec.id}",
            module = UniversalSearchModule.ATTENDANCE,
            date = rec.date,
            title = "$name ($code)",
            subtitle = "$designation · $shiftName",
            detailLine = rec.remarks,
            valueOrBadge = rec.status.label,
            tags = listOf(deptName, dutyTitle).filter { it.isNotBlank() && it != "—" },
            attendanceStatus = rec.status,
            isNightShift = isNight,
            isDeletedReference = isDeletedRef,
          )
        )
      }
    }

    // 2. Roster assignments
    if (
      (query.module == UniversalSearchModule.ALL || query.module == UniversalSearchModule.ROSTER) &&
        !hasStatusFilter &&
        !hasIctFilters &&
        !hasOfficeFilters &&
        !hasTaskFilters
    ) {
      rosterAssignments.forEach { assignment ->
        if (!matchesDate(assignment.date)) return@forEach
        if (query.employeeId != null && assignment.employeeId != query.employeeId) return@forEach
        if (query.shiftId != null && assignment.shiftId != query.shiftId) return@forEach
        if (query.dutyTypeId != null && assignment.dutyTypeId != query.dutyTypeId) return@forEach

        val emp = empMap[assignment.employeeId] ?: return@forEach
        if (
          query.designation != null &&
            !emp.designation.equals(query.designation, ignoreCase = true)
        ) {
          return@forEach
        }
        val shift = shiftMap[assignment.shiftId]
        val duty = assignment.dutyTypeId?.let { dutyMap[it] }
        val effectiveDeptId =
          assignment.departmentId?.takeIf { it.isNotBlank() }
            ?: duty?.departmentId?.takeIf { it.isNotBlank() }
        val dept = effectiveDeptId?.let { deptMap[it] }

        if (query.departmentId != null && effectiveDeptId != query.departmentId) return@forEach

        val isNight = shift?.isNightShiftVisual() ?: false
        if (query.nightShiftOnly && !isNight) return@forEach

        val shiftName = shift?.name ?: "Assigned Shift"
        val deptName = dept?.name ?: "Unassigned Dept"
        val dutyTitle = duty?.title ?: "No Duty"

        val matchesText =
          needle.isBlank() ||
            listOf(
                assignment.date,
                emp.code,
                emp.name,
                emp.designation,
                shiftName,
                deptName,
                dutyTitle,
                assignment.notes,
              )
              .any { it.lowercase().contains(needle) }

        if (!matchesText) return@forEach

        results.add(
          UniversalSearchResult(
            id = "roster_${assignment.id}",
            module = UniversalSearchModule.ROSTER,
            date = assignment.date,
            title = "${emp.name} (${emp.code})",
            subtitle = "${emp.designation} · $shiftName",
            detailLine = assignment.notes,
            valueOrBadge = shift?.let { "${it.startTime}–${it.endTime}" } ?: "Roster",
            tags = listOf(deptName, dutyTitle).filter { it.isNotBlank() },
            isNightShift = isNight,
          )
        )
      }
    }

    // 3. Employees
    if (
      (query.module == UniversalSearchModule.ALL ||
        query.module == UniversalSearchModule.EMPLOYEES) &&
        !hasShiftFilters &&
        !hasStatusFilter &&
        !hasIctFilters &&
        !hasOfficeFilters &&
        !hasTaskFilters &&
        query.dutyTypeId == null
    ) {
      employees.forEach { emp ->
        val dateIso = DateUtils.todayIso(emp.updatedAt.takeIf { it > 0L } ?: emp.createdAt)
        if (!matchesDate(dateIso)) return@forEach
        if (query.employeeId != null && emp.id != query.employeeId) return@forEach
        if (
          query.designation != null &&
            !emp.designation.equals(query.designation, ignoreCase = true)
        ) {
          return@forEach
        }
        if (query.departmentId != null && emp.defaultDepartmentId != query.departmentId) {
          return@forEach
        }

        val deptName = emp.defaultDepartmentId?.let { deptMap[it]?.name }.orEmpty()
        val activeLabel = if (emp.isActive) "Active" else "Inactive"
        val matchesText =
          needle.isBlank() ||
            listOf(dateIso, emp.code, emp.name, emp.designation, deptName, activeLabel)
              .any { it.lowercase().contains(needle) }

        if (!matchesText) return@forEach

        results.add(
          UniversalSearchResult(
            id = "emp_${emp.id}",
            module = UniversalSearchModule.EMPLOYEES,
            date = dateIso,
            title = "${emp.name} (${emp.code})",
            subtitle = emp.designation.ifBlank { "Team Member" },
            detailLine =
              if (deptName.isNotBlank()) "Default Dept: $deptName" else "Flexible department",
            valueOrBadge = activeLabel,
            tags = listOfNotNull(deptName.takeIf { it.isNotBlank() }),
          )
        )
      }
    }

    // 4. Shifts, Departments & Duties
    if (
      (query.module == UniversalSearchModule.ALL ||
        query.module == UniversalSearchModule.SHIFTS_DUTIES) &&
        !hasEmployeeFilters &&
        !hasStatusFilter &&
        !hasIctFilters &&
        !hasOfficeFilters &&
        !hasTaskFilters
    ) {
      if (query.departmentId == null && query.dutyTypeId == null) {
        shifts.forEach { shift ->
          if (query.shiftId != null && shift.id != query.shiftId) return@forEach
          val isNight = shift.isNightShiftVisual()
          if (query.nightShiftOnly && !isNight) return@forEach

          val dateIso = DateUtils.todayIso(shift.updatedAt.takeIf { it > 0L } ?: shift.createdAt)
          if (!matchesDate(dateIso)) return@forEach

          val shiftWindow = "${shift.startTime}–${shift.endTime}"
          val matchesText =
            needle.isBlank() ||
              listOf(
                  dateIso,
                  shift.name,
                  shiftWindow,
                  if (isNight) "night shift" else "day shift",
                )
                .any { it.lowercase().contains(needle) }

          if (!matchesText) return@forEach

          results.add(
            UniversalSearchResult(
              id = "shift_${shift.id}",
              module = UniversalSearchModule.SHIFTS_DUTIES,
              date = dateIso,
              title = shift.name,
              subtitle = "Shift Window · $shiftWindow",
              valueOrBadge = if (shift.isActive) "Active" else "Inactive",
              tags = listOf(if (isNight) "Night Shift" else "Day Shift"),
              isNightShift = isNight,
            )
          )
        }
      }

      if (query.shiftId == null && !query.nightShiftOnly && query.dutyTypeId == null) {
        departments.forEach { dept ->
          if (query.departmentId != null && dept.id != query.departmentId) return@forEach
          val dateIso = DateUtils.todayIso(dept.updatedAt.takeIf { it > 0L } ?: dept.createdAt)
          if (!matchesDate(dateIso)) return@forEach

          val matchesText =
            needle.isBlank() ||
              listOf(dateIso, dept.name, dept.description, "department")
                .any { it.lowercase().contains(needle) }

          if (!matchesText) return@forEach

          results.add(
            UniversalSearchResult(
              id = "dept_${dept.id}",
              module = UniversalSearchModule.SHIFTS_DUTIES,
              date = dateIso,
              title = dept.name,
              subtitle = "Department",
              detailLine = dept.description,
              valueOrBadge = if (dept.isActive) "Active" else "Inactive",
              tags = listOf("Department"),
            )
          )
        }
      }

      duties.forEach { duty ->
        if (query.dutyTypeId != null && duty.id != query.dutyTypeId) return@forEach
        if (query.departmentId != null && duty.departmentId != query.departmentId) return@forEach
        if (query.shiftId != null && duty.defaultShiftId != query.shiftId) return@forEach

        val linkedShift = duty.defaultShiftId?.let { shiftMap[it] }
        val isNight = linkedShift?.isNightShiftVisual() ?: false
        if (query.nightShiftOnly && !isNight) return@forEach

        val dateIso = DateUtils.todayIso(duty.updatedAt.takeIf { it > 0L } ?: duty.createdAt)
        if (!matchesDate(dateIso)) return@forEach

        val deptName = deptMap[duty.departmentId]?.name.orEmpty()
        val shiftName = linkedShift?.name.orEmpty()
        val matchesText =
          needle.isBlank() ||
            listOf(dateIso, duty.title, duty.description, deptName, shiftName, "duty")
              .any { it.lowercase().contains(needle) }

        if (!matchesText) return@forEach

        results.add(
          UniversalSearchResult(
            id = "duty_${duty.id}",
            module = UniversalSearchModule.SHIFTS_DUTIES,
            date = dateIso,
            title = duty.title,
            subtitle = if (deptName.isNotBlank()) "Duty · $deptName" else "Duty Template",
            detailLine = duty.description,
            valueOrBadge = if (duty.isActive) "Active" else "Inactive",
            tags = listOfNotNull("Duty", deptName.takeIf { it.isNotBlank() }, shiftName.takeIf { it.isNotBlank() }),
            isNightShift = isNight,
          )
        )
      }
    }

    // 5. ICT Data entries
    if (
      (query.module == UniversalSearchModule.ALL || query.module == UniversalSearchModule.ICT_DATA) &&
        !hasAttendanceFilters &&
        !hasStatusFilter &&
        !hasOfficeFilters &&
        !hasTaskFilters
    ) {
      ictEntries.forEach { entry ->
        val dateIso = DateUtils.todayIso(entry.recordedAt.takeIf { it > 0L } ?: entry.updatedAt)
        if (!matchesDate(dateIso)) return@forEach
        if (query.ictCategoryId != null && entry.categoryId != query.ictCategoryId) return@forEach
        if (query.ictSubCategoryId != null && entry.subCategoryId != query.ictSubCategoryId) {
          return@forEach
        }

        val cat = ictCatMap[entry.categoryId]
        val sub = entry.subCategoryId?.let { ictSubMap[it] }
        val catName = cat?.name ?: entry.categoryNameSnapshot.ifBlank { "ICT" }
        val subName = sub?.name ?: entry.subCategoryNameSnapshot
        val unitLabel = sub?.unitLabel.orEmpty()
        val displayValue =
          if (unitLabel.isNotBlank()) "${entry.valueText} $unitLabel" else entry.valueText

        val matchesText =
          needle.isBlank() ||
            listOf(
                dateIso,
                catName,
                subName,
                entry.valueText,
                entry.numericValue?.toString().orEmpty(),
                unitLabel,
                entry.note,
              )
              .any { it.lowercase().contains(needle) }

        if (!matchesText) return@forEach

        results.add(
          UniversalSearchResult(
            id = "ict_${entry.id}",
            module = UniversalSearchModule.ICT_DATA,
            date = dateIso,
            title = if (subName.isNotBlank()) "$catName · $subName" else catName,
            subtitle = "Recorded value: $displayValue",
            detailLine = entry.note,
            valueOrBadge = displayValue,
            tags = listOfNotNull(catName, subName.takeIf { it.isNotBlank() }),
            isDeletedReference = cat == null,
            targetCategoryId = entry.categoryId,
          )
        )
      }
    }

    // 6. Office Index entries
    if (
      (query.module == UniversalSearchModule.ALL ||
        query.module == UniversalSearchModule.OFFICE_INDEX) &&
        !hasAttendanceFilters &&
        !hasStatusFilter &&
        !hasIctFilters &&
        !hasTaskFilters
    ) {
      officeEntries.forEach { entry ->
        val dateIso = DateUtils.todayIso(entry.updatedAt.takeIf { it > 0L } ?: entry.createdAt)
        if (!matchesDate(dateIso)) return@forEach
        if (
          query.officeCategory != null &&
            !entry.category.equals(query.officeCategory, ignoreCase = true)
        ) {
          return@forEach
        }
        if (
          query.officeSubCategory != null &&
            !entry.subCategory.equals(query.officeSubCategory, ignoreCase = true)
        ) {
          return@forEach
        }

        val matchesText =
          needle.isBlank() ||
            listOf(
                dateIso,
                entry.textValue,
                entry.integerValue.toString(),
                entry.category,
                entry.subCategory,
                entry.notes,
              )
              .any { it.lowercase().contains(needle) }

        if (!matchesText) return@forEach

        results.add(
          UniversalSearchResult(
            id = "office_${entry.id}",
            module = UniversalSearchModule.OFFICE_INDEX,
            date = dateIso,
            title = entry.textValue,
            subtitle =
              listOf(entry.category, entry.subCategory)
                .filter { it.isNotBlank() }
                .joinToString(" · ")
                .ifBlank { "Office Index" },
            detailLine = entry.notes,
            valueOrBadge = "#${entry.integerValue}",
            tags =
              listOf(entry.category, entry.subCategory).filter { it.isNotBlank() },
          )
        )
      }

      if (!hasOfficeFilters) {
        val officesById = registeredOffices.associateBy { it.id }
        officeCaseRecords.forEach { caseRec ->
          if (!matchesDate(caseRec.recordDate)) return@forEach
          val officeName = OfficeCaseEngine.resolveOfficeName(caseRec, officesById)
          val shortCode = OfficeCaseEngine.resolveOfficeShortCode(caseRec, officesById)
          val secLabels = caseRec.sections.map { it.displayLabel() }
          val ioHistoryText = caseRec.ioHistory.map { "${it.ioName} ${it.assignedDate} ${it.remarks}" }
          val matchesText =
            needle.isBlank() ||
              (listOf(
                  caseRec.recordDate,
                  caseRec.firNumber.toString(),
                  "fir ${caseRec.firNumber}",
                  "fir#${caseRec.firNumber}",
                  officeName,
                  shortCode,
                  caseRec.currentIoName,
                  caseRec.actType,
                  caseRec.customActName,
                  caseRec.notes,
                ) + secLabels + ioHistoryText)
                .any { it.lowercase().contains(needle) }
          if (!matchesText) return@forEach

          val secSummary =
            caseRec.sections.joinToString(", ") { "${it.actCode} Sec ${it.sectionNumber}" }
              .ifBlank { caseRec.customActName }
          results.add(
            UniversalSearchResult(
              id = "office_case_${caseRec.id}",
              module = UniversalSearchModule.OFFICE_INDEX,
              date = caseRec.recordDate,
              title = "FIR #${caseRec.firNumber} · I/O: ${caseRec.currentIoName}",
              subtitle = listOf(officeName, secSummary).filter { it.isNotBlank() }.joinToString(" · "),
              detailLine = caseRec.notes,
              valueOrBadge = shortCode.ifBlank { caseRec.primaryActBadge() },
              tags =
                listOfNotNull(
                  shortCode.takeIf { it.isNotBlank() },
                  caseRec.primaryActBadge().takeIf { it.isNotBlank() },
                  if (caseRec.ioHistory.size > 1) "${caseRec.ioHistory.size} I/Os" else null,
                ),
            )
          )
        }
      }
    }

    // 7. Task Manager categories & items
    if (
      (query.module == UniversalSearchModule.ALL || query.module == UniversalSearchModule.TASKS) &&
        !hasAttendanceFilters &&
        !hasStatusFilter &&
        !hasIctFilters &&
        !hasOfficeFilters
    ) {
      taskItems.forEach { item ->
        if (query.taskCategoryId != null && item.categoryId != query.taskCategoryId) return@forEach
        if (query.taskCompletedFilter != null && item.isCompleted != query.taskCompletedFilter) {
          return@forEach
        }
        val dateIso = DateUtils.todayIso(item.updatedAt.takeIf { it > 0L } ?: item.createdAt)
        if (!matchesDate(dateIso)) return@forEach

        val catName = taskCatMap[item.categoryId]?.name.orEmpty().ifBlank { "Tasks" }
        val statusLabel = if (item.isCompleted) "Done" else "Pending"
        val matchesText =
          needle.isBlank() ||
            listOf(dateIso, item.title, catName, statusLabel, "task")
              .any { it.lowercase().contains(needle) }

        if (!matchesText) return@forEach

        results.add(
          UniversalSearchResult(
            id = "task_${item.id}",
            module = UniversalSearchModule.TASKS,
            date = dateIso,
            title = item.title,
            subtitle = "Task Category: $catName",
            valueOrBadge = statusLabel,
            tags = listOf(catName),
            targetCategoryId = item.categoryId,
          )
        )
      }

      taskCategories.forEach { cat ->
        if (query.taskCategoryId != null && cat.id != query.taskCategoryId) return@forEach
        if (query.taskCompletedFilter == true) return@forEach
        if (!taskItemsByCat[cat.id].isNullOrEmpty()) return@forEach

        val dateIso = DateUtils.todayIso(cat.updatedAt.takeIf { it > 0L } ?: cat.createdAt)
        if (!matchesDate(dateIso)) return@forEach

        val matchesText =
          needle.isBlank() ||
            listOf(dateIso, cat.name, "task category")
              .any { it.lowercase().contains(needle) }

        if (!matchesText) return@forEach

        results.add(
          UniversalSearchResult(
            id = "task_cat_${cat.id}",
            module = UniversalSearchModule.TASKS,
            date = dateIso,
            title = cat.name,
            subtitle = "Task Category · 0 items",
            valueOrBadge = "Category",
            tags = listOf("Empty Category"),
            targetCategoryId = cat.id,
          )
        )
      }
    }

    // 8. Notes
    if (
      (query.module == UniversalSearchModule.ALL || query.module == UniversalSearchModule.NOTES) &&
        !hasAttendanceFilters &&
        !hasStatusFilter &&
        !hasIctFilters &&
        !hasOfficeFilters &&
        !hasTaskFilters
    ) {
      notes.forEach { note ->
        val dateIso = DateUtils.todayIso(note.updatedAt.takeIf { it > 0L } ?: note.createdAt)
        if (!matchesDate(dateIso)) return@forEach

        val matchesText =
          needle.isBlank() ||
            listOf(dateIso, note.title, note.body, "note")
              .any { it.lowercase().contains(needle) }

        if (!matchesText) return@forEach

        results.add(
          UniversalSearchResult(
            id = "note_${note.id}",
            module = UniversalSearchModule.NOTES,
            date = dateIso,
            title = note.title,
            subtitle = "Note · $dateIso",
            detailLine = note.body,
            valueOrBadge = "Note",
          )
        )
      }
    }

    val designations =
      (employees.map { it.designation } +
          attendanceRecords.mapNotNull { it.designationSnapshot.takeIf { d -> d.isNotBlank() } })
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()
        .sorted()

    val officeCategories =
      officeEntries.map { it.category.trim() }.filter { it.isNotBlank() }.distinct().sorted()

    val officeSubCategories =
      officeEntries
        .asSequence()
        .filter {
          query.officeCategory == null || it.category.equals(query.officeCategory, ignoreCase = true)
        }
        .map { it.subCategory.trim() }
        .filter { it.isNotBlank() }
        .distinct()
        .sorted()
        .toList()

    val sorted =
      results.sortedWith(
        compareByDescending<UniversalSearchResult> { it.date }
          .thenBy { it.module.ordinal }
          .thenBy { it.title }
      )

    return Output(
      results = sorted,
      facets =
        UniversalSearchFacets(
          designations = designations,
          officeCategories = officeCategories,
          officeSubCategories = officeSubCategories,
        ),
    )
  }
}
