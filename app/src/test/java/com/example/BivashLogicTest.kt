package com.example

import com.example.logic.AttendanceCalculator
import com.example.logic.BackupManager
import com.example.logic.DateUtils
import com.example.logic.HistorySearchEngine
import com.example.logic.LegalActSectionCatalog
import com.example.logic.ModuleRegistry
import com.example.logic.OfficeCaseEngine
import com.example.logic.OfflineUpdateEngine
import com.example.logic.RosterConflictDetector
import com.example.logic.UniversalSearchEngine
import com.example.models.AppMeta
import com.example.models.AppModuleId
import com.example.models.AttendanceRecord
import com.example.models.AttendanceStatus
import com.example.models.BatchRosterAssignmentRequest
import com.example.models.BatchShiftAssignmentItem
import com.example.models.CaseSectionAttachment
import com.example.models.CURRENT_SCHEMA_VERSION
import com.example.models.DEFAULT_DEPARTMENT_ID
import com.example.models.DEFAULT_DEPARTMENT_NAME
import com.example.models.Department
import com.example.models.DutyType
import com.example.models.Employee
import com.example.models.HistoryFilterQuery
import com.example.models.IctCategory
import com.example.models.IctEntry
import com.example.models.IctSubCategory
import com.example.models.LegalActType
import com.example.models.NoteEntry
import com.example.models.OfficeCaseRecord
import com.example.models.OfficeCaseSearchQuery
import com.example.models.OfficeIndexEntry
import com.example.models.RegisteredOffice
import com.example.models.RosterAssignment
import com.example.models.RosterAssignmentRequest
import com.example.models.Shift
import com.example.models.TaskCategory
import com.example.models.TaskInputParser
import com.example.models.TaskItem
import com.example.models.UniversalSearchModule
import com.example.models.UniversalSearchQuery
import com.example.ui.components.SyncedTextFieldBuffer
import com.example.ui.components.isHardwareEchoDuplicate
import com.example.ui.theme.FloralVariant
import com.example.ui.theme.GradientPaletteRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BivashLogicTest {

  private data class TestFixtures(
    val shifts: List<Shift>,
    val duties: List<DutyType>,
    val employees: List<Employee>,
    val roster: List<RosterAssignment>,
    val attendance: List<AttendanceRecord>,
    val meta: AppMeta,
  )

  private fun createFixtures(now: Long = 1700000000000L): TestFixtures {
    val today = DateUtils.todayIso(now)
    val twoDaysAgo = DateUtils.addDays(today, -2)
    val shifts =
      listOf(
        Shift("shift_morning", "Morning Shift", "08:00", "16:00", false, true, now, now),
        Shift("shift_afternoon", "Afternoon Shift", "14:00", "22:00", false, true, now, now),
        Shift("shift_night", "Night Shift", "22:00", "06:00", true, true, now, now),
      )
    val duties =
      listOf(
        DutyType("duty_gate", "Gate Control", "Entry logs", true, now, now),
        DutyType("duty_safety", "Safety Inspection", "Perimeter walk", true, now, now),
        DutyType("duty_dispatch", "Floor Dispatch", "Manifests", true, now, now),
      )
    val employees =
      listOf(
        Employee("emp_1", "EMP-101", "Bivash Sen", "Operations Lead", true, now, now),
        Employee("emp_2", "EMP-102", "Ananya Roy", "Shift Supervisor", true, now, now),
        Employee("emp_3", "EMP-103", "Priya Verma", "Dispatch Clerk", true, now, now),
      )
    val roster =
      listOf(
        RosterAssignment("${today}_emp_1_m", today, "emp_1", "shift_morning", "duty_gate", "", now, now),
        RosterAssignment("${today}_emp_1_n", today, "emp_1", "shift_night", "duty_safety", "", now, now),
        RosterAssignment("${today}_emp_2_m", today, "emp_2", "shift_morning", "duty_dispatch", "", now, now),
      )
    val attendance =
      listOf(
        AttendanceRecord(
          id = "${today}_emp_1_m",
          date = today,
          employeeId = "emp_1",
          shiftId = "shift_morning",
          dutyTypeId = "duty_gate",
          status = AttendanceStatus.PRESENT,
          remarks = "Opened register",
          employeeCodeSnapshot = "EMP-101",
          employeeNameSnapshot = "Bivash Sen",
          designationSnapshot = "Operations Lead",
          shiftNameSnapshot = "Morning Shift",
          isNightShiftSnapshot = false,
          dutyTitleSnapshot = "Gate Control",
          markedAt = now,
          updatedAt = now,
        ),
        AttendanceRecord(
          id = "${twoDaysAgo}_emp_2_n",
          date = twoDaysAgo,
          employeeId = "emp_2",
          shiftId = "shift_night",
          dutyTypeId = "duty_safety",
          status = AttendanceStatus.LATE,
          remarks = "Handover delay",
          employeeCodeSnapshot = "EMP-102",
          employeeNameSnapshot = "Ananya Roy",
          designationSnapshot = "Shift Supervisor",
          shiftNameSnapshot = "Night Shift",
          isNightShiftSnapshot = true,
          dutyTitleSnapshot = "Safety Inspection",
          markedAt = now,
          updatedAt = now,
        ),
        AttendanceRecord(
          id = "${today}_emp_3_",
          date = today,
          employeeId = "emp_3",
          shiftId = "",
          dutyTypeId = null,
          status = AttendanceStatus.OFF,
          remarks = "Weekly rotational off",
          employeeCodeSnapshot = "EMP-103",
          employeeNameSnapshot = "Priya Verma",
          designationSnapshot = "Dispatch Clerk",
          shiftNameSnapshot = "Unscheduled",
          isNightShiftSnapshot = false,
          dutyTitleSnapshot = "",
          markedAt = now,
          updatedAt = now,
        ),
      )
    return TestFixtures(shifts, duties, employees, roster, attendance, AppMeta(seededAt = now))
  }

  @Test
  fun `module registry enables v1 attendance and reserves Index and Counter`() {
    assertTrue(ModuleRegistry.isModuleEnabled(AppModuleId.ATTENDANCE_DUTY))
    assertFalse(ModuleRegistry.isModuleEnabled(AppModuleId.INDEX))
    assertFalse(ModuleRegistry.isModuleEnabled(AppModuleId.COUNTER))
    assertEquals(3, ModuleRegistry.getAllModules().size)
  }

  @Test
  fun `date range expansion is inclusive and deterministic`() {
    val range = DateUtils.expandDateRange("2026-11-20", "2026-11-22")
    assertEquals(listOf("2026-11-20", "2026-11-21", "2026-11-22"), range)
  }

  @Test
  fun `attendance summary and multi-shift daily rows compute accurately`() {
    val fx = createFixtures()
    val today = DateUtils.todayIso(1700000000000L)
    val assignmentsForToday = fx.roster.filter { it.date == today }
    val attendanceForToday = fx.attendance.filter { it.date == today }

    val summary =
      AttendanceCalculator.calculateSummary(
        date = today,
        employees = fx.employees,
        assignmentsForDate = assignmentsForToday,
        attendanceForDate = attendanceForToday,
      )
    assertEquals(3, summary.totalActiveEmployees)
    assertEquals(3, summary.scheduledSlotsCount)
    assertEquals(1, summary.presentCount)
    assertEquals(1, summary.offCount)
    assertEquals(2, summary.unmarkedSlotsCount)

    val rows =
      AttendanceCalculator.buildDailyRows(
        date = today,
        employees = fx.employees,
        shifts = fx.shifts,
        duties = fx.duties,
        assignmentsForDate = assignmentsForToday,
        attendanceForDate = attendanceForToday,
      )
    val bivashRows = rows.filter { it.employee.id == "emp_1" }
    assertEquals(2, bivashRows.size)
    assertTrue(bivashRows.any { it.shiftId == "shift_morning" && !it.isNightShift })
    assertTrue(bivashRows.any { it.shiftId == "shift_night" && it.isNightShift })
  }

  @Test
  fun `multi-shift on different shift has no conflict while same shift triggers conflict`() {
    val fx = createFixtures()
    val today = DateUtils.todayIso(1700000000000L)

    val differentShiftOutcome =
      RosterConflictDetector.evaluateAndBuildAssignments(
        request =
          RosterAssignmentRequest(
            employeeIds = listOf("emp_1"),
            startDate = today,
            endDate = today,
            shiftId = "shift_afternoon",
            dutyTypeId = "duty_dispatch",
            notes = "Extra afternoon cover",
            overwriteConflicts = false,
          ),
        employees = fx.employees,
        shifts = fx.shifts,
        duties = fx.duties,
        existingAssignments = fx.roster,
      )
    assertFalse(differentShiftOutcome.requiresConfirmation)
    assertEquals(1, differentShiftOutcome.appliedCount)

    val sameShiftOutcome =
      RosterConflictDetector.evaluateAndBuildAssignments(
        request =
          RosterAssignmentRequest(
            employeeIds = listOf("emp_1"),
            startDate = today,
            endDate = today,
            shiftId = "shift_morning",
            dutyTypeId = "duty_safety",
            notes = "Overwrite test",
            overwriteConflicts = false,
          ),
        employees = fx.employees,
        shifts = fx.shifts,
        duties = fx.duties,
        existingAssignments = fx.roster,
      )
    assertTrue(sameShiftOutcome.requiresConfirmation)
    assertEquals(1, sameShiftOutcome.conflictsFound.size)
  }

  @Test
  fun `history search filters by date range, designation, night shift, status, and text`() {
    val fx = createFixtures()
    val today = DateUtils.todayIso(1700000000000L)
    val twoDaysAgo = DateUtils.addDays(today, -2)

    val allRecent =
      HistorySearchEngine.search(
        query = HistoryFilterQuery(startDate = DateUtils.addDays(today, -5), endDate = today),
        attendanceRecords = fx.attendance,
        employees = fx.employees,
        shifts = fx.shifts,
        duties = fx.duties,
      )
    assertEquals(fx.attendance.size, allRecent.size)

    val nightShiftOnly =
      HistorySearchEngine.search(
        query =
          HistoryFilterQuery(
            startDate = twoDaysAgo,
            endDate = today,
            shiftId = "shift_night",
          ),
        attendanceRecords = fx.attendance,
        employees = fx.employees,
        shifts = fx.shifts,
        duties = fx.duties,
      )
    assertEquals(1, nightShiftOnly.size)
    assertTrue(nightShiftOnly.all { it.isNightShift })

    val supervisorLate =
      HistorySearchEngine.search(
        query =
          HistoryFilterQuery(
            startDate = twoDaysAgo,
            endDate = today,
            designation = "Shift Supervisor",
            status = AttendanceStatus.LATE,
          ),
        attendanceRecords = fx.attendance,
        employees = fx.employees,
        shifts = fx.shifts,
        duties = fx.duties,
      )
    assertEquals(1, supervisorLate.size)
    assertEquals("EMP-102", supervisorLate.first().employeeCode)

    val offSearch =
      HistorySearchEngine.search(
        query =
          HistoryFilterQuery(
            startDate = twoDaysAgo,
            endDate = today,
            searchText = "rotational",
            status = AttendanceStatus.OFF,
          ),
        attendanceRecords = fx.attendance,
        employees = fx.employees,
        shifts = fx.shifts,
        duties = fx.duties,
      )
    assertEquals(1, offSearch.size)
  }

  @Test
  fun `backup rejects unsupported schema version`() {
    val fx = createFixtures()
    val bundle =
      BackupManager.createBackupBundle(
        employees = fx.employees,
        shifts = fx.shifts,
        dutyTypes = fx.duties,
        rosterAssignments = fx.roster,
        attendanceRecords = fx.attendance,
        appMeta = fx.meta,
      )
    val raw =
      BackupManager.serializeBundle(bundle).replace("\"schemaVersion\": 1", "\"schemaVersion\": 99")
    val validation = BackupManager.validateAndParseBackup(raw)
    assertFalse(validation.isValid)
    assertTrue(validation.errors.any { it.contains("Unsupported schema_version") })
  }

  @Test
  fun `offline update rejects incompatible schema range`() {
    val fx = createFixtures()
    val sampleJson =
      OfflineUpdateEngine.createSampleUpdatePackJson()
        .replace("\"minSchemaVersion\": 1", "\"minSchemaVersion\": 2")
    val preview = OfflineUpdateEngine.previewUpdatePack(sampleJson, fx.shifts, fx.duties)
    assertFalse(preview.isCompatible)
  }

  @Test
  fun `gradient palette registry defines 30 unique 3-stop palettes and assigns distinct cards`() {
    assertEquals(30, GradientPaletteRegistry.all.size)
    assertTrue(GradientPaletteRegistry.all.all { it.colors.size == 3 })

    val assignedPalettes = (0 until 30).map { i -> GradientPaletteRegistry.paletteForCard("card_$i", i).id }
    assertEquals(30, assignedPalettes.distinct().size)

    val firstSixVariants =
      (0 until FloralVariant.entries.size).map { i ->
        GradientPaletteRegistry.floralVariantForCard("card_$i", i)
      }
    assertEquals(FloralVariant.entries.size, firstSixVariants.distinct().size)
  }

  @Test
  fun `batch multi-shift single date evaluation stages multiple shifts cleanly and detects conflicts`() {
    val fx = createFixtures()
    val today = DateUtils.todayIso(1700000000000L)

    // Assign emp_3 to Morning and emp_2 to Night in one combined batch (no conflicts)
    val cleanBatch =
      RosterConflictDetector.evaluateAndBuildBatchAssignments(
        request =
          BatchRosterAssignmentRequest(
            date = today,
            batches =
              listOf(
                BatchShiftAssignmentItem("shift_morning", listOf("emp_3"), "duty_gate", "Gate A"),
                BatchShiftAssignmentItem("shift_night", listOf("emp_2"), "duty_safety", "Perimeter"),
              ),
            overwriteConflicts = false,
          ),
        employees = fx.employees,
        shifts = fx.shifts,
        duties = fx.duties,
        existingAssignments = fx.roster,
      )
    assertFalse(cleanBatch.requiresConfirmation)
    assertEquals(2, cleanBatch.appliedCount)
    assertEquals(1, cleanBatch.assignmentsToUpsert.count { it.shiftId == "shift_morning" })
    assertEquals(1, cleanBatch.assignmentsToUpsert.count { it.shiftId == "shift_night" })

    // Now attempt batch where emp_1 already has shift_morning assigned -> triggers conflict
    val conflictBatch =
      RosterConflictDetector.evaluateAndBuildBatchAssignments(
        request =
          BatchRosterAssignmentRequest(
            date = today,
            batches =
              listOf(
                BatchShiftAssignmentItem("shift_morning", listOf("emp_1"), "duty_safety", "Replace"),
                BatchShiftAssignmentItem("shift_afternoon", listOf("emp_3"), "duty_dispatch", "Cover"),
              ),
            overwriteConflicts = false,
          ),
        employees = fx.employees,
        shifts = fx.shifts,
        duties = fx.duties,
        existingAssignments = fx.roster,
      )
    assertTrue(conflictBatch.requiresConfirmation)
    assertEquals(1, conflictBatch.conflictsFound.size)
    assertEquals("emp_1", conflictBatch.conflictsFound.first().employeeId)
  }

  @Test
  fun `calendar month grid populates date summaries and night shift indicators`() {
    val fx = createFixtures()
    val today = DateUtils.todayIso(1700000000000L)
    val (year, month) = DateUtils.yearMonthOf(today)

    val cells =
      AttendanceCalculator.buildCalendarMonthCells(
        year = year,
        month1Based = month,
        employees = fx.employees,
        shifts = fx.shifts,
        allRoster = fx.roster,
        allAttendance = fx.attendance,
      )
    assertTrue(cells.size in listOf(28, 35, 42))
    val todayCell = cells.first { it.dateIso == today }
    assertTrue(todayCell.isInCurrentMonth)
    assertTrue(todayCell.hasNightShift)
    assertEquals(1, todayCell.summary.presentCount)
    assertEquals(1, todayCell.summary.offCount)
    assertEquals(3, todayCell.summary.scheduledSlotsCount)
  }

  @Test
  fun `carry-forward resolves employee most recent department and duty and allows manual override`() {
    val now = 1700000000000L
    val day1 = "2026-11-18"
    val day2 = "2026-11-19"
    val day3 = "2026-11-20"
    val departments =
      listOf(
        Department(DEFAULT_DEPARTMENT_ID, DEFAULT_DEPARTMENT_NAME, "", true, now, now),
        Department("dept_ops", "Operations", "", true, now, now),
      )
    val duties =
      listOf(
        DutyType("duty_helpdesk", "Helpdesk", "", true, now, now, DEFAULT_DEPARTMENT_ID),
        DutyType("duty_dispatch", "Dispatch", "", true, now, now, "dept_ops"),
      )
    val employees =
      listOf(
        Employee("emp_1", "EMP-101", "Bivash Sen", "Lead", true, now, now),
        Employee("emp_2", "EMP-102", "Ananya Roy", "Analyst", true, now, now),
      )
    val shifts =
      listOf(Shift("shift_m", "Morning", "08:00", "16:00", false, true, now, now))

    val assignments =
      listOf(
        RosterAssignment(
          id = "${day1}_emp_1_shift_m",
          date = day1,
          employeeId = "emp_1",
          shiftId = "shift_m",
          dutyTypeId = "duty_helpdesk",
          notes = "",
          createdAt = now,
          updatedAt = now,
          departmentId = DEFAULT_DEPARTMENT_ID,
        ),
        RosterAssignment(
          id = "${day2}_emp_1_shift_m",
          date = day2,
          employeeId = "emp_1",
          shiftId = "shift_m",
          dutyTypeId = "duty_dispatch",
          notes = "",
          createdAt = now + 1000L,
          updatedAt = now + 1000L,
          departmentId = "dept_ops",
        ),
        RosterAssignment(
          id = "${day2}_emp_2_shift_m",
          date = day2,
          employeeId = "emp_2",
          shiftId = "shift_m",
          dutyTypeId = "duty_deleted",
          notes = "",
          createdAt = now + 1000L,
          updatedAt = now + 1000L,
          departmentId = DEFAULT_DEPARTMENT_ID,
        ),
      )

    val carryForward =
      RosterConflictDetector.resolveCarryForwardDefaults(
        employees = employees,
        departments = departments,
        duties = duties,
        assignments = assignments,
        beforeDate = day3,
      )

    assertEquals("dept_ops", carryForward["emp_1"]?.departmentId)
    assertEquals("duty_dispatch", carryForward["emp_1"]?.dutyTypeId)
    assertEquals(day2, carryForward["emp_1"]?.sourceDate)

    // Deleted duty is cleared gracefully while valid department remains
    assertEquals(DEFAULT_DEPARTMENT_ID, carryForward["emp_2"]?.departmentId)
    assertNull(carryForward["emp_2"]?.dutyTypeId)

    // Employee is never permanently locked: switching back to Computer/Helpdesk on day3 succeeds
    val overrideOutcome =
      RosterConflictDetector.evaluateAndBuildAssignments(
        request =
          RosterAssignmentRequest(
            employeeIds = listOf("emp_1"),
            startDate = day3,
            endDate = day3,
            shiftId = "shift_m",
            dutyTypeId = "duty_helpdesk",
            notes = "Manual override back to Computer",
            overwriteConflicts = false,
            departmentId = DEFAULT_DEPARTMENT_ID,
          ),
        employees = employees,
        shifts = shifts,
        duties = duties,
        existingAssignments = assignments,
      )
    assertFalse(overrideOutcome.requiresConfirmation)
    assertEquals(1, overrideOutcome.appliedCount)
    assertEquals(DEFAULT_DEPARTMENT_ID, overrideOutcome.assignmentsToUpsert.first().departmentId)
    assertEquals("duty_helpdesk", overrideOutcome.assignmentsToUpsert.first().dutyTypeId)
  }

  @Test
  fun `calendar breakdown groups working rows by shift then department then duty with separate absence section`() {
    val now = 1700000000000L
    val today = "2026-11-20"
    val departments =
      listOf(
        Department(DEFAULT_DEPARTMENT_ID, DEFAULT_DEPARTMENT_NAME, "", true, now, now),
        Department("dept_sec", "Security", "", true, now, now),
      )
    val shifts =
      listOf(
        Shift("shift_m", "Morning Shift", "08:00", "16:00", false, true, now, now),
        Shift("shift_n", "Night Shift", "22:00", "06:00", true, true, now, now),
      )
    val duties =
      listOf(
        DutyType("duty_pc", "Lab Desk", "", true, now, now, DEFAULT_DEPARTMENT_ID),
        DutyType("duty_noc", "NOC Watch", "", true, now, now, "dept_sec", "shift_n"),
      )
    val employees =
      listOf(
        Employee("emp_1", "EMP-01", "Asha", "Tech", true, now, now),
        Employee("emp_2", "EMP-02", "Ravi", "Guard", true, now, now),
        Employee("emp_3", "EMP-03", "Meera", "Clerk", true, now, now),
        Employee("emp_4", "EMP-04", "Kiran", "Support", true, now, now),
      )
    val assignments =
      listOf(
        RosterAssignment("${today}_emp_1_m", today, "emp_1", "shift_m", "duty_pc", "", now, now, DEFAULT_DEPARTMENT_ID),
        RosterAssignment("${today}_emp_2_n", today, "emp_2", "shift_n", "duty_noc", "", now, now, "dept_sec"),
      )
    val attendance =
      listOf(
        AttendanceRecord(
          id = "${today}_emp_3_",
          date = today,
          employeeId = "emp_3",
          shiftId = "",
          dutyTypeId = null,
          status = AttendanceStatus.OFF,
          remarks = "Rest day",
          employeeCodeSnapshot = "EMP-03",
          employeeNameSnapshot = "Meera",
          designationSnapshot = "Clerk",
          shiftNameSnapshot = "Unscheduled",
          isNightShiftSnapshot = false,
          dutyTitleSnapshot = "",
          markedAt = now,
          updatedAt = now,
        )
      )

    val rows =
      AttendanceCalculator.buildDailyRows(
        date = today,
        employees = employees,
        shifts = shifts,
        duties = duties,
        assignmentsForDate = assignments,
        attendanceForDate = attendance,
        departments = departments,
      )
    val breakdown = AttendanceCalculator.buildCalendarDateBreakdown(rows)

    assertEquals(2, breakdown.workingShiftGroups.size)
    val morningGroup = breakdown.workingShiftGroups[0]
    val nightGroup = breakdown.workingShiftGroups[1]
    assertFalse(morningGroup.isNightShift)
    assertTrue(nightGroup.isNightShift)
    assertEquals(DEFAULT_DEPARTMENT_NAME, morningGroup.departmentGroups.single().departmentName)
    assertEquals("Lab Desk", morningGroup.departmentGroups.single().dutyGroups.single().dutyTitle)
    assertEquals("Security", nightGroup.departmentGroups.single().departmentName)
    assertEquals("NOC Watch", nightGroup.departmentGroups.single().dutyGroups.single().dutyTitle)
    assertEquals(1, breakdown.offOrAbsenceRows.size)
    assertEquals("emp_3", breakdown.offOrAbsenceRows.single().employee.id)
    assertEquals(1, breakdown.unmarkedUnscheduledRows.size)
    assertEquals("emp_4", breakdown.unmarkedUnscheduledRows.single().employee.id)
  }

  @Test
  fun `history search filters by department combined with date range and shift`() {
    val now = 1700000000000L
    val departments =
      listOf(
        Department(DEFAULT_DEPARTMENT_ID, DEFAULT_DEPARTMENT_NAME, "", true, now, now),
        Department("dept_ops", "Field Ops", "", true, now, now),
      )
    val shifts =
      listOf(Shift("shift_n", "Night Shift", "22:00", "06:00", true, true, now, now))
    val duties =
      listOf(
        DutyType("duty_pc", "Systems", "", true, now, now, DEFAULT_DEPARTMENT_ID),
        DutyType("duty_gate", "Gate", "", true, now, now, "dept_ops"),
      )
    val employees = listOf(Employee("emp_1", "EMP-01", "Asha", "Engineer", true, now, now))
    val records =
      listOf(
        AttendanceRecord(
          id = "r1",
          date = "2026-11-19",
          employeeId = "emp_1",
          shiftId = "shift_n",
          dutyTypeId = "duty_pc",
          status = AttendanceStatus.PRESENT,
          remarks = "Server audit",
          employeeCodeSnapshot = "EMP-01",
          employeeNameSnapshot = "Asha",
          designationSnapshot = "Engineer",
          shiftNameSnapshot = "Night Shift",
          isNightShiftSnapshot = true,
          dutyTitleSnapshot = "Systems",
          markedAt = now,
          updatedAt = now,
          departmentId = DEFAULT_DEPARTMENT_ID,
          departmentNameSnapshot = DEFAULT_DEPARTMENT_NAME,
        ),
        AttendanceRecord(
          id = "r2",
          date = "2026-11-20",
          employeeId = "emp_1",
          shiftId = "shift_n",
          dutyTypeId = "duty_gate",
          status = AttendanceStatus.LATE,
          remarks = "Gate relief",
          employeeCodeSnapshot = "EMP-01",
          employeeNameSnapshot = "Asha",
          designationSnapshot = "Engineer",
          shiftNameSnapshot = "Night Shift",
          isNightShiftSnapshot = true,
          dutyTitleSnapshot = "Gate",
          markedAt = now,
          updatedAt = now,
          departmentId = "dept_ops",
          departmentNameSnapshot = "Field Ops",
        ),
      )

    val computerOnly =
      HistorySearchEngine.search(
        query =
          HistoryFilterQuery(
            startDate = "2026-11-18",
            endDate = "2026-11-21",
            departmentId = DEFAULT_DEPARTMENT_ID,
            shiftId = "shift_n",
          ),
        attendanceRecords = records,
        employees = employees,
        shifts = shifts,
        duties = duties,
        departments = departments,
      )
    assertEquals(1, computerOnly.size)
    assertEquals("Computer", computerOnly.single().departmentName)
    assertEquals("r1", computerOnly.single().id)

    val textSearchByDept =
      HistorySearchEngine.search(
        query =
          HistoryFilterQuery(
            startDate = "2026-11-18",
            endDate = "2026-11-21",
            searchText = "Field Ops",
          ),
        attendanceRecords = records,
        employees = employees,
        shifts = shifts,
        duties = duties,
        departments = departments,
      )
    assertEquals(1, textSearchByDept.size)
    assertEquals("r2", textSearchByDept.single().id)
  }

  @Test
  fun `employee registered with default department pre-fills carry-forward without locking later assignments`() {
    val now = 1700000000000L
    val departments =
      listOf(
        Department(DEFAULT_DEPARTMENT_ID, DEFAULT_DEPARTMENT_NAME, "", true, now, now),
        Department("dept_ops", "Operations", "", true, now, now),
      )
    val duties =
      listOf(
        DutyType("duty_ops", "Floor Patrol", "", true, now, now, "dept_ops"),
      )
    val employees =
      listOf(
        Employee("emp_new", "EMP-201", "Neha Das", "Analyst", true, now, now, DEFAULT_DEPARTMENT_ID),
        Employee("emp_flex", "EMP-202", "Karan Sen", "Trainee", true, now, now, null),
      )

    val initialCf =
      RosterConflictDetector.resolveCarryForwardDefaults(
        employees = employees,
        departments = departments,
        duties = duties,
        assignments = emptyList(),
        beforeDate = "2026-11-21",
      )
    assertEquals(DEFAULT_DEPARTMENT_ID, initialCf["emp_new"]?.departmentId)
    assertEquals("default", initialCf["emp_new"]?.sourceDate)
    assertNull(initialCf["emp_flex"])

    // Employee can still be flexibly assigned to a different department on a working day
    val shiftedAssignment =
      RosterAssignment(
        id = "2026-11-20_emp_new_shift_m",
        date = "2026-11-20",
        employeeId = "emp_new",
        shiftId = "shift_m",
        dutyTypeId = "duty_ops",
        notes = "",
        createdAt = now,
        updatedAt = now,
        departmentId = "dept_ops",
      )
    val updatedCf =
      RosterConflictDetector.resolveCarryForwardDefaults(
        employees = employees,
        departments = departments,
        duties = duties,
        assignments = listOf(shiftedAssignment),
        beforeDate = "2026-11-21",
      )
    assertEquals("dept_ops", updatedCf["emp_new"]?.departmentId)
    assertEquals("duty_ops", updatedCf["emp_new"]?.dutyTypeId)
    assertEquals("2026-11-20", updatedCf["emp_new"]?.sourceDate)
  }

  @Test
  fun `build date version formats as DD dot Mon dot YYYY and BuildConfig matches`() {
    assertEquals("01. Jan. 2027", DateUtils.formatBuildDateVersion(2027, 1, 1))
    assertEquals("05. Sep. 2026", DateUtils.formatBuildDateVersion(2026, 9, 5))
    assertTrue(
      "BuildConfig.APP_BUILD_DATE_VERSION='${BuildConfig.APP_BUILD_DATE_VERSION}' must match DD. Mon. YYYY",
      Regex("""^\d{2}\. [A-Z][a-z]{2}\. \d{4}$""").matches(BuildConfig.APP_BUILD_DATE_VERSION),
    )
  }

  @Test
  fun `universal search spans attendance roster ict data and office index with deep filters`() {
    val now = 1700000000000L
    val today = DateUtils.todayIso(now)
    val twoDaysAgo = DateUtils.addDays(today, -2)
    val fx = createFixtures(now)
    val ictCats = listOf(IctCategory("ict_c1", "Network", "Switch health", now, now))
    val ictSubs = listOf(IctSubCategory("ict_s1", "ict_c1", "Latency", "ms", now, now))
    val ictEntries =
      listOf(
        IctEntry(
          id = "ict_e1",
          categoryId = "ict_c1",
          subCategoryId = "ict_s1",
          valueText = "12",
          numericValue = 12.0,
          note = "Core uplink nominal",
          categoryNameSnapshot = "Network",
          subCategoryNameSnapshot = "Latency",
          recordedAt = now,
          updatedAt = now,
        )
      )
    val officeEntries =
      listOf(
        OfficeIndexEntry(
          id = "off_1",
          integerValue = 7,
          textValue = "Backup UPS Battery",
          category = "Hardware",
          subCategory = "Power",
          notes = "Server closet",
          createdAt = now,
          updatedAt = now,
        )
      )

    // 1. All modules & all dates returns attendance (3) + roster (3) + employees (3) + shifts (3) + duties (3) + ict (1) + office (1) = 17
    val allOut =
      UniversalSearchEngine.search(
        query = UniversalSearchQuery(startDate = null, endDate = null),
        employees = fx.employees,
        shifts = fx.shifts,
        departments = emptyList(),
        duties = fx.duties,
        rosterAssignments = fx.roster,
        attendanceRecords = fx.attendance,
        ictCategories = ictCats,
        ictSubCategories = ictSubs,
        ictEntries = ictEntries,
        officeEntries = officeEntries,
      )
    assertEquals(17, allOut.results.size)
    assertTrue(allOut.results.any { it.module == UniversalSearchModule.ICT_DATA })
    assertTrue(allOut.results.any { it.module == UniversalSearchModule.OFFICE_INDEX })
    assertTrue(allOut.results.any { it.module == UniversalSearchModule.EMPLOYEES })
    assertTrue(allOut.results.any { it.module == UniversalSearchModule.SHIFTS_DUTIES })

    // 2. Night shift only matches Night Shift Attendance (1) + Night Shift Roster (1) + Night Shift def (1) = 3
    val nightOut =
      UniversalSearchEngine.search(
        query =
          UniversalSearchQuery(
            startDate = twoDaysAgo,
            endDate = today,
            nightShiftOnly = true,
          ),
        employees = fx.employees,
        shifts = fx.shifts,
        departments = emptyList(),
        duties = fx.duties,
        rosterAssignments = fx.roster,
        attendanceRecords = fx.attendance,
        ictCategories = ictCats,
        ictSubCategories = ictSubs,
        ictEntries = ictEntries,
        officeEntries = officeEntries,
      )
    assertEquals(3, nightOut.results.size)
    assertTrue(nightOut.results.all { it.isNightShift })

    // 3. ICT Category filter narrows exclusively to ICT Data
    val ictOut =
      UniversalSearchEngine.search(
        query =
          UniversalSearchQuery(
            startDate = twoDaysAgo,
            endDate = today,
            ictCategoryId = "ict_c1",
            ictSubCategoryId = "ict_s1",
          ),
        employees = fx.employees,
        shifts = fx.shifts,
        departments = emptyList(),
        duties = fx.duties,
        rosterAssignments = fx.roster,
        attendanceRecords = fx.attendance,
        ictCategories = ictCats,
        ictSubCategories = ictSubs,
        ictEntries = ictEntries,
        officeEntries = officeEntries,
      )
    assertEquals(1, ictOut.results.size)
    assertEquals(UniversalSearchModule.ICT_DATA, ictOut.results.single().module)

    // 4. Office Index category + numeric search
    val officeOut =
      UniversalSearchEngine.search(
        query =
          UniversalSearchQuery(
            startDate = twoDaysAgo,
            endDate = today,
            officeCategory = "Hardware",
            officeSubCategory = "Power",
            searchText = "Battery",
          ),
        employees = fx.employees,
        shifts = fx.shifts,
        departments = emptyList(),
        duties = fx.duties,
        rosterAssignments = fx.roster,
        attendanceRecords = fx.attendance,
        ictCategories = ictCats,
        ictSubCategories = ictSubs,
        ictEntries = ictEntries,
        officeEntries = officeEntries,
      )
    assertEquals(1, officeOut.results.size)
    assertEquals("#7", officeOut.results.single().valueOrBadge)
  }

  @Test
  fun `auto-OFF marks unassigned active employees OFF, preserves overrides, and clears stale auto-OFF`() {
    val now = 1700000000000L
    val date = "2026-11-22"
    val employees =
      listOf(
        Employee("emp_1", "EMP-1", "Asha", "Lead", true, now, now),
        Employee("emp_2", "EMP-2", "Ravi", "Clerk", true, now, now),
        Employee("emp_3", "EMP-3", "Meera", "Guard", true, now, now),
        Employee("emp_inactive", "EMP-4", "Old Staff", "Guard", false, now, now),
      )
    val assignments =
      listOf(
        RosterAssignment("r1", date, "emp_1", "shift_m", null, "", now, now)
      )
    val existingAttendance =
      listOf(
        AttendanceRecord(
          id = "${date}_emp_3_",
          date = date,
          employeeId = "emp_3",
          shiftId = "",
          dutyTypeId = null,
          status = AttendanceStatus.LEAVE,
          remarks = "Approved leave",
          employeeCodeSnapshot = "EMP-3",
          employeeNameSnapshot = "Meera",
          designationSnapshot = "Guard",
          shiftNameSnapshot = "Unscheduled",
          isNightShiftSnapshot = false,
          dutyTitleSnapshot = "",
          markedAt = now,
          updatedAt = now,
        ),
        AttendanceRecord(
          id = "${date}_emp_1_",
          date = date,
          employeeId = "emp_1",
          shiftId = "",
          dutyTypeId = null,
          status = AttendanceStatus.OFF,
          remarks = AttendanceCalculator.AUTO_OFF_REMARK,
          employeeCodeSnapshot = "EMP-1",
          employeeNameSnapshot = "Asha",
          designationSnapshot = "Lead",
          shiftNameSnapshot = "Unscheduled",
          isNightShiftSnapshot = false,
          dutyTitleSnapshot = "",
          markedAt = now,
          updatedAt = now,
        ),
      )

    val sync =
      AttendanceCalculator.buildAutoOffSyncForDate(
        date = date,
        employees = employees,
        assignmentsForDate = assignments,
        existingAttendanceForDate = existingAttendance,
        nowMillis = now,
      )
    assertEquals(1, sync.recordsToUpsert.size)
    assertEquals("emp_2", sync.recordsToUpsert.single().employeeId)
    assertEquals(AttendanceStatus.OFF, sync.recordsToUpsert.single().status)
    assertEquals(1, sync.staleAutoOffIdsToDelete.size)
    assertEquals("${date}_emp_1_", sync.staleAutoOffIdsToDelete.single())
  }

  @Test
  fun `task comma separator trims entries and ignores blanks, while date normalization enforces order`() {
    assertEquals(
      listOf("Audit", "Verify logs", "Lock gate"),
      TaskInputParser.parseEntries(" Audit , , Verify logs,Lock gate , ", useCommaSeparator = true),
    )
    assertEquals(
      listOf("Audit, Verify logs"),
      TaskInputParser.parseEntries("  Audit, Verify logs  ", useCommaSeparator = false),
    )
    assertTrue(TaskInputParser.parseEntries(" , , ", useCommaSeparator = true).isEmpty())

    val utcMillis = DateUtils.isoToUtcMillis("2026-11-23")!!
    assertEquals("2026-11-23", DateUtils.utcMillisToIso(utcMillis))
    assertEquals("2026-11-10" to "2026-11-20", DateUtils.normalizeDateRange("2026-11-20", "2026-11-10"))
    assertEquals("2026-11-10" to null, DateUtils.normalizeDateRange("2026-11-10", null))
  }

  @Test
  fun `universal search queries Tasks and Notes with date range and status filters`() {
    val now = 1700000000000L
    val today = DateUtils.todayIso(now)
    val taskCats = listOf(TaskCategory("tc_1", "Daily Ops", now, now))
    val taskItems =
      listOf(
        TaskItem("ti_1", "tc_1", "Check UPS load", isCompleted = false, now, now),
        TaskItem("ti_2", "tc_1", "Sign handover book", isCompleted = true, now, now),
      )
    val notes =
      listOf(
        NoteEntry("n_1", "Generator Check", "Fuel level 85%", now, now),
      )

    val pendingTasks =
      UniversalSearchEngine.search(
        query =
          UniversalSearchQuery(
            module = UniversalSearchModule.TASKS,
            startDate = today,
            endDate = today,
            taskCategoryId = "tc_1",
            taskCompletedFilter = false,
          ),
        employees = emptyList(),
        shifts = emptyList(),
        departments = emptyList(),
        duties = emptyList(),
        rosterAssignments = emptyList(),
        attendanceRecords = emptyList(),
        ictCategories = emptyList(),
        ictSubCategories = emptyList(),
        ictEntries = emptyList(),
        officeEntries = emptyList(),
        taskCategories = taskCats,
        taskItems = taskItems,
        notes = notes,
      )
    assertEquals(1, pendingTasks.results.size)
    assertEquals("Check UPS load", pendingTasks.results.single().title)

    val noteHits =
      UniversalSearchEngine.search(
        query =
          UniversalSearchQuery(
            module = UniversalSearchModule.ALL,
            searchText = "Fuel",
          ),
        employees = emptyList(),
        shifts = emptyList(),
        departments = emptyList(),
        duties = emptyList(),
        rosterAssignments = emptyList(),
        attendanceRecords = emptyList(),
        ictCategories = emptyList(),
        ictSubCategories = emptyList(),
        ictEntries = emptyList(),
        officeEntries = emptyList(),
        taskCategories = taskCats,
        taskItems = taskItems,
        notes = notes,
      )
    assertEquals(1, noteHits.results.size)
    assertEquals(UniversalSearchModule.NOTES, noteHits.results.single().module)
  }

  @Test
  fun `legal act catalog contains all 358 BNS sections and all 47 POCSO sections with exact-number-first search`() {
    val bns = LegalActSectionCatalog.bnsSections
    val pocso = LegalActSectionCatalog.pocsoSections
    assertEquals(358, bns.size)
    assertEquals((1..358).map { it.toString() }, bns.map { it.sectionNumber })
    assertEquals(47, pocso.size)
    val expectedPocsoNums = (1..42).map { it.toString() } + listOf("42A", "43", "44", "45", "46")
    assertEquals(expectedPocsoNums, pocso.map { it.sectionNumber })

    val bns103 = LegalActSectionCatalog.searchSections(LegalActType.BNS, "103")
    assertTrue(bns103.isNotEmpty())
    assertEquals("103", bns103.first().sectionNumber)

    val pocso42A = LegalActSectionCatalog.searchSections(LegalActType.POCSO, "42A")
    assertTrue(pocso42A.isNotEmpty())
    assertEquals("42A", pocso42A.first().sectionNumber)

    val murderHits = LegalActSectionCatalog.searchSections(LegalActType.BNS, "murder")
    assertTrue(murderHits.any { it.sectionNumber == "103" })
  }

  @Test
  fun `office case engine tracks I-O transfer history, autofill names, and filters by current or historical I-O`() {
    val now = 1700000000000L
    val office1 = RegisteredOffice("off_1", "Sadar Police Station", "SPS", true, now, now)
    val office2 = RegisteredOffice("off_2", "Town Thana", "TTPS", true, now, now)

    val initialRecord =
      OfficeCaseRecord(
        id = "case_1",
        officeId = office1.id,
        officeNameSnapshot = office1.officeName,
        officeShortCodeSnapshot = office1.shortCode,
        recordDate = "2026-11-10",
        firNumber = 245,
        currentIoName = "SI Ramesh Verma",
        actType = LegalActType.BNS.code,
        sections =
          listOf(
            CaseSectionAttachment("BNS", "BNS 2023", "103", "Punishment for murder"),
            CaseSectionAttachment("POCSO", "POCSO Act", "6", "Aggravated penetrative sexual assault"),
          ),
        ioHistory = OfficeCaseEngine.buildInitialIoHistory("SI Ramesh Verma", "2026-11-10"),
        notes = "Initial FIR",
        createdAt = now,
        updatedAt = now,
      )

    val transferredRecord =
      OfficeCaseEngine.applyEditWithOptionalIoReassignment(
        existing = initialRecord,
        office = office1,
        recordDate = "2026-11-10",
        firNumber = 245,
        editedCurrentIoName = "SI Ramesh Verma",
        actType = LegalActType.BNS.code,
        customActName = "",
        sections = initialRecord.sections,
        notes = "Transferred to crime branch",
        newAssignedIoName = "Insp. Priya Nair",
        newIoAssignedDate = "2026-11-18",
        newIoRemarks = "Special investigation",
        nowMillis = now + 5000L,
      )

    assertEquals("Insp. Priya Nair", transferredRecord.currentIoName)
    assertEquals(2, transferredRecord.ioHistory.size)
    assertEquals("SI Ramesh Verma", transferredRecord.ioHistory[0].ioName)
    assertEquals("2026-11-10", transferredRecord.ioHistory[0].assignedDate)
    assertEquals("Insp. Priya Nair", transferredRecord.ioHistory[1].ioName)
    assertEquals("2026-11-18", transferredRecord.ioHistory[1].assignedDate)

    val secondRecord =
      OfficeCaseRecord(
        id = "case_2",
        officeId = office2.id,
        officeNameSnapshot = office2.officeName,
        officeShortCodeSnapshot = office2.shortCode,
        recordDate = "2026-11-20",
        firNumber = 312,
        currentIoName = "ASI Mohan Roy",
        actType = LegalActType.OTHERS.code,
        customActName = "NDPS Act",
        sections = listOf(CaseSectionAttachment("OTHERS", "NDPS Act", "20", "Cannabis possession")),
        ioHistory = OfficeCaseEngine.buildInitialIoHistory("ASI Mohan Roy", "2026-11-20"),
        createdAt = now,
        updatedAt = now + 10000L,
      )

    val allRecords = listOf(transferredRecord, secondRecord)
    val knownIos = OfficeCaseEngine.collectKnownIoNames(allRecords)
    assertEquals(listOf("ASI Mohan Roy", "Insp. Priya Nair", "SI Ramesh Verma"), knownIos)

    val prevIoFilter =
      OfficeCaseEngine.filterRecords(
        records = allRecords,
        query = OfficeCaseSearchQuery(ioNameQuery = "Ramesh"),
        offices = listOf(office1, office2),
      )
    assertEquals(1, prevIoFilter.size)
    assertEquals(245, prevIoFilter.single().firNumber)

    val pocsoFilter =
      OfficeCaseEngine.filterRecords(
        records = allRecords,
        query = OfficeCaseSearchQuery(actFilter = LegalActType.POCSO.code, sectionQuery = "6"),
        offices = listOf(office1, office2),
      )
    assertEquals(1, pocsoFilter.size)
    assertEquals("case_1", pocsoFilter.single().id)

    val ndpsFilter =
      OfficeCaseEngine.filterRecords(
        records = allRecords,
        query = OfficeCaseSearchQuery(startDate = "2026-11-15", endDate = "2026-11-23", officeId = office2.id),
        offices = listOf(office1, office2),
      )
    assertEquals(1, ndpsFilter.size)
    assertEquals(312, ndpsFilter.single().firNumber)
  }

  @Test
  fun `backup validates and preserves registered offices and office case records with I-O history`() {
    val now = 1700000000000L
    val office = RegisteredOffice("off_1", "Central Desk", "CD", true, now, now)
    val caseRec =
      OfficeCaseRecord(
        id = "case_1",
        officeId = office.id,
        officeNameSnapshot = office.officeName,
        officeShortCodeSnapshot = office.shortCode,
        recordDate = "2026-11-21",
        firNumber = 108,
        currentIoName = "Insp. Kiran",
        actType = LegalActType.BNS.code,
        sections = listOf(CaseSectionAttachment("BNS", "BNS 2023", "303", "Theft")),
        ioHistory = OfficeCaseEngine.buildInitialIoHistory("Insp. Kiran", "2026-11-21"),
        createdAt = now,
        updatedAt = now,
      )

    val bundle =
      BackupManager.createBackupBundle(
        employees = emptyList(),
        shifts = emptyList(),
        dutyTypes = emptyList(),
        rosterAssignments = emptyList(),
        attendanceRecords = emptyList(),
        appMeta = AppMeta(schemaVersion = CURRENT_SCHEMA_VERSION),
        registeredOffices = listOf(office),
        officeCaseRecords = listOf(caseRec),
        exportedAt = now,
      )
    val json = BackupManager.serializeBundle(bundle)
    val validation = BackupManager.validateAndParseBackup(json)
    assertTrue(validation.errors.joinToString(), validation.isValid)
    assertEquals(1, validation.bundle?.payload?.registeredOffices?.size)
    assertEquals("CD", validation.bundle?.payload?.registeredOffices?.single()?.shortCode)
    assertEquals(108, validation.bundle?.payload?.officeCaseRecords?.single()?.firNumber)

    // Invalid FIR number in backup should fail validation
    val invalidBundle =
      BackupManager.createBackupBundle(
        employees = emptyList(),
        shifts = emptyList(),
        dutyTypes = emptyList(),
        rosterAssignments = emptyList(),
        attendanceRecords = emptyList(),
        appMeta = AppMeta(schemaVersion = CURRENT_SCHEMA_VERSION),
        officeCaseRecords = listOf(caseRec.copy(firNumber = 0)),
        exportedAt = now,
      )
    val invalidValidation = BackupManager.validateAndParseBackup(BackupManager.serializeBundle(invalidBundle))
    assertFalse(invalidValidation.isValid)
    assertTrue(invalidValidation.errors.any { it.contains("Invalid FIR number") })
  }

  @Test
  fun `universal search indexes office case records by FIR, short code, current and previous I-O`() {
    val now = 1700000000000L
    val office = RegisteredOffice("off_1", "North Station", "NPS", true, now, now)
    val caseRec =
      OfficeCaseRecord(
        id = "case_99",
        officeId = office.id,
        officeNameSnapshot = office.officeName,
        officeShortCodeSnapshot = office.shortCode,
        recordDate = "2026-11-22",
        firNumber = 777,
        currentIoName = "Insp. Neha Sen",
        actType = LegalActType.POCSO.code,
        sections = listOf(CaseSectionAttachment("POCSO", "POCSO Act", "42A", "Act not in derogation of any other law")),
        ioHistory =
          listOf(
            com.example.models.IoAssignmentHistoryEntry("SI Alok Das", "2026-11-15", "Initial I/O"),
            com.example.models.IoAssignmentHistoryEntry("Insp. Neha Sen", "2026-11-22", "Reassigned I/O"),
          ),
        createdAt = now,
        updatedAt = now,
      )

    val byPrevIo =
      UniversalSearchEngine.search(
        query = UniversalSearchQuery(module = UniversalSearchModule.OFFICE_INDEX, searchText = "Alok Das"),
        employees = emptyList(),
        shifts = emptyList(),
        departments = emptyList(),
        duties = emptyList(),
        rosterAssignments = emptyList(),
        attendanceRecords = emptyList(),
        ictCategories = emptyList(),
        ictSubCategories = emptyList(),
        ictEntries = emptyList(),
        officeEntries = emptyList(),
        registeredOffices = listOf(office),
        officeCaseRecords = listOf(caseRec),
      )
    assertEquals(1, byPrevIo.results.size)
    assertEquals("FIR #777 · I/O: Insp. Neha Sen", byPrevIo.results.single().title)
    assertEquals("NPS", byPrevIo.results.single().valueOrBadge)
    assertTrue(byPrevIo.results.single().subtitle.contains("North Station"))
    assertTrue(byPrevIo.results.single().subtitle.contains("42A"))
  }

  @Test
  fun `synced text field buffer ignores delayed async echoes while honoring external resets`() {
    val buffer = SyncedTextFieldBuffer("")
    assertEquals("a", buffer.onLocalEdit("a"))
    assertEquals("ab", buffer.onLocalEdit("ab"))
    assertEquals("abc", buffer.onLocalEdit("abc"))

    // Delayed async echo of earlier edits must NOT revert currentText back to "a" or "ab"
    assertFalse(buffer.onExternalValueChanged("a"))
    assertEquals("abc", buffer.currentText)

    assertFalse(buffer.onExternalValueChanged("ab"))
    assertEquals("abc", buffer.currentText)

    // External Reset button sets "" -> MUST sync immediately
    assertTrue(buffer.onExternalValueChanged(""))
    assertEquals("", buffer.currentText)

    // Sanitizer test (e.g., digits only)
    assertEquals("42", buffer.onLocalEdit("42x") { raw -> raw.filter(Char::isDigit) })
  }

  @Test
  fun `hardware key echo guard drops sub-28ms duplicate keydowns while preserving normal double letters and delete`() {
    val keyA = android.view.KeyEvent.KEYCODE_A
    val keyDel = android.view.KeyEvent.KEYCODE_DEL

    // Rapid < 28ms synthetic echo of identical printable key with repeatCount == 0 -> dropped
    assertTrue(
      isHardwareEchoDuplicate(
        keyCode = keyA,
        repeatCount = 0,
        eventTimeMs = 1015L,
        lastKeyCode = keyA,
        lastEventTimeMs = 1000L,
      )
    )

    // Deliberate double letter (e.g. "book" or "100") > 28ms apart -> allowed
    assertFalse(
      isHardwareEchoDuplicate(
        keyCode = keyA,
        repeatCount = 0,
        eventTimeMs = 1090L,
        lastKeyCode = keyA,
        lastEventTimeMs = 1000L,
      )
    )

    // Deleting characters quickly via Backspace/DEL -> never dropped
    assertFalse(
      isHardwareEchoDuplicate(
        keyCode = keyDel,
        repeatCount = 0,
        eventTimeMs = 1010L,
        lastKeyCode = keyDel,
        lastEventTimeMs = 1000L,
      )
    )
  }
}
