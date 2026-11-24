package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.logic.AttendanceCalculator
import com.example.logic.BackupManager
import com.example.logic.DateUtils
import com.example.logic.HistorySearchEngine
import com.example.logic.OfflineUpdateEngine
import com.example.models.AppThemeMode
import com.example.models.AttendanceStatus
import com.example.models.BatchRosterAssignmentRequest
import com.example.models.BatchShiftAssignmentItem
import com.example.models.CURRENT_SCHEMA_VERSION
import com.example.models.CaseSectionAttachment
import com.example.models.DEFAULT_DEPARTMENT_ID
import com.example.models.DEFAULT_DEPARTMENT_NAME
import com.example.models.HistoryFilterQuery
import com.example.models.LegalActType
import com.example.models.RosterAssignmentRequest
import com.example.storage.BivashDatabase
import com.example.storage.BivashRepository
import com.example.storage.ThemePreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  private lateinit var db: BivashDatabase
  private lateinit var repo: BivashRepository

  @Before
  fun setUp() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    db = Room.inMemoryDatabaseBuilder(context, BivashDatabase::class.java).allowMainThreadQueries().build()
    repo = BivashRepository(db)
  }

  @After
  fun tearDown() {
    db.close()
  }

  @Test
  fun `app name matches Bivash Index`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    assertEquals("Bivash Index", context.getString(R.string.app_name))
  }

  @Test
  fun `fresh install initializes clean empty collections with schema version 1 and clean backup`() =
    runBlocking {
      repo.ensureInitialized()

      assertEquals(0, repo.employees.first().size)
      assertEquals(0, repo.shifts.first().size)
      assertEquals(0, repo.dutyTypes.first().size)
      assertEquals(1, repo.departments.first().size)
      assertEquals(DEFAULT_DEPARTMENT_ID, repo.departments.first().single().id)
      assertEquals(DEFAULT_DEPARTMENT_NAME, repo.departments.first().single().name)
      assertEquals(0, repo.rosterAssignments.first().size)
      assertEquals(0, repo.attendanceRecords.first().size)
      assertEquals(0, repo.officeIndex.first().size)
      assertEquals(0, repo.ictCategories.first().size)
      assertEquals(0, repo.ictSubCategories.first().size)
      assertEquals(0, repo.ictEntries.first().size)
      assertEquals(CURRENT_SCHEMA_VERSION, repo.appMeta.first()?.schemaVersion)

      val (bundle, json) = repo.exportBackupJson()
      assertEquals(0, bundle.recordCounts.employees)
      assertEquals(0, bundle.recordCounts.shifts)
      assertEquals(0, bundle.recordCounts.dutyTypes)
      assertEquals(1, bundle.recordCounts.departments)
      assertEquals(0, bundle.recordCounts.rosterAssignments)
      assertEquals(0, bundle.recordCounts.attendanceRecords)
      assertEquals(0, bundle.recordCounts.officeIndex)
      assertTrue(BackupManager.validateAndParseBackup(json).isValid)
    }

  private suspend fun insertUserCreatedBaseData(today: String) {
    repo.saveEmployee(null, "EMP-101", "Bivash Sen", "Operations Lead", true)
    repo.saveEmployee(null, "EMP-099", "Kabir Bose", "Field Officer", true)
    repo.saveShift(null, "Morning Shift", "08:00", "16:00", false, true)
    repo.saveShift(null, "Afternoon Shift", "14:00", "22:00", false, true)
    repo.saveShift(null, "Night Shift", "22:00", "06:00", true, true)
    repo.saveDutyType(null, "Gate Control", "Entry checks", true)
    repo.saveDutyType(null, "Safety Inspection", "Perimeter walk", true)

    val emp101 = repo.employees.first().first { it.code == "EMP-101" }
    val morning = repo.shifts.first().first { it.name == "Morning Shift" }
    val night = repo.shifts.first().first { it.name == "Night Shift" }
    val gate = repo.dutyTypes.first().first { it.title == "Gate Control" }
    val safety = repo.dutyTypes.first().first { it.title == "Safety Inspection" }

    repo.evaluateAndApplyRoster(
      RosterAssignmentRequest(
        employeeIds = listOf(emp101.id),
        startDate = today,
        endDate = today,
        shiftId = morning.id,
        dutyTypeId = gate.id,
        notes = "Morning cover",
        overwriteConflicts = false,
      )
    )
    repo.evaluateAndApplyRoster(
      RosterAssignmentRequest(
        employeeIds = listOf(emp101.id),
        startDate = today,
        endDate = today,
        shiftId = night.id,
        dutyTypeId = safety.id,
        notes = "Night cover",
        overwriteConflicts = false,
      )
    )
  }

  @Test
  fun `user data enforces multi-shift attendance uniqueness per date employee and shift`() =
    runBlocking {
      repo.ensureInitialized()
      val today = DateUtils.todayIso()
      insertUserCreatedBaseData(today)

      val employees = repo.employees.first()
      val shifts = repo.shifts.first()
      val morning = shifts.first { it.name == "Morning Shift" }
      val night = shifts.first { it.name == "Night Shift" }
      val emp101 = employees.first { it.code == "EMP-101" }

      val rows =
        AttendanceCalculator.buildDailyRows(
          date = today,
          employees = employees,
          shifts = shifts,
          duties = repo.dutyTypes.first(),
          assignmentsForDate = repo.rosterAssignments.first().filter { it.date == today },
          attendanceForDate = repo.attendanceRecords.first().filter { it.date == today },
        )

      val bivashMorning = rows.first { it.employee.id == emp101.id && it.shiftId == morning.id }
      val bivashNight = rows.first { it.employee.id == emp101.id && it.shiftId == night.id }

      repo.markAttendance(today, bivashMorning, AttendanceStatus.LATE, "Updated morning remark")
      repo.markAttendance(today, bivashMorning, AttendanceStatus.PRESENT, "Corrected morning")
      repo.markAttendance(today, bivashNight, AttendanceStatus.PRESENT, "Double-shift night log")

      val records = repo.attendanceRecords.first()
      val bivashToday = records.filter { it.date == today && it.employeeId == emp101.id }
      assertEquals(2, bivashToday.size)
      assertEquals("Corrected morning", bivashToday.first { it.shiftId == morning.id }.remarks)
      assertEquals("Double-shift night log", bivashToday.first { it.shiftId == night.id }.remarks)
      assertTrue(bivashToday.first { it.shiftId == night.id }.isNightShiftSnapshot)
    }

  @Test
  fun `backup export and atomic restore creates pre-restore safety snapshot`() = runBlocking {
    repo.ensureInitialized()
    repo.saveEmployee(null, "EMP-101", "Bivash Sen", "Operations Lead", true)
    val (_, backupJson) = repo.exportBackupJson()

    repo.saveEmployee(null, "EMP-777", "Test Extra", "Night Coordinator", true)
    assertEquals(2, repo.employees.first().size)

    val result = repo.restoreFromBackupJson(backupJson)
    assertTrue(result.success)
    assertNotNull(result.safetySnapshotId)
    assertEquals(1, repo.employees.first().size)
    assertEquals(1, repo.safetySnapshots.first().size)

    val badResult = repo.restoreFromBackupJson(backupJson.replace("Bivash Sen", "Tampered"))
    assertFalse(badResult.success)
    assertEquals(1, repo.employees.first().size)
  }

  @Test
  fun `offline update pack applies shifts and duties without wiping attendance or employees`() =
    runBlocking {
      repo.ensureInitialized()
      repo.saveEmployee(null, "EMP-101", "Bivash Sen", "Operations Lead", true)
      val initialEmpCount = repo.employees.first().size
      val initialAttCount = repo.attendanceRecords.first().size

      val packJson = OfflineUpdateEngine.createSampleUpdatePackJson()
      val preview = repo.previewOfflineUpdate(packJson)
      assertTrue(preview.isCompatible)

      val applyResult = repo.applyOfflineUpdate(preview.pack!!)
      assertTrue(applyResult.success)
      assertEquals(initialEmpCount, repo.employees.first().size)
      assertEquals(initialAttCount, repo.attendanceRecords.first().size)
      assertTrue(repo.shifts.first().isNotEmpty())
      assertEquals("bivash-ops-pack-1.1", repo.appMeta.first()?.lastUpdatePackId)
    }

  @Test
  fun `roster allows multiple distinct shifts per day and warns before overwriting same shift`() =
    runBlocking {
      repo.ensureInitialized()
      val today = DateUtils.todayIso()
      insertUserCreatedBaseData(today)

      val emp101 = repo.employees.first().first { it.code == "EMP-101" }
      val afternoon = repo.shifts.first().first { it.name == "Afternoon Shift" }
      val gate = repo.dutyTypes.first().first { it.title == "Gate Control" }
      val safety = repo.dutyTypes.first().first { it.title == "Safety Inspection" }

      val multiShiftAttempt =
        repo.evaluateAndApplyRoster(
          RosterAssignmentRequest(
            employeeIds = listOf(emp101.id),
            startDate = today,
            endDate = today,
            shiftId = afternoon.id,
            dutyTypeId = gate.id,
            notes = "Extra shift",
            overwriteConflicts = false,
          )
        )
      assertFalse(multiShiftAttempt.requiresConfirmation)
      assertEquals(1, multiShiftAttempt.appliedCount)

      val conflictAttempt =
        repo.evaluateAndApplyRoster(
          RosterAssignmentRequest(
            employeeIds = listOf(emp101.id),
            startDate = today,
            endDate = today,
            shiftId = afternoon.id,
            dutyTypeId = safety.id,
            notes = "Updated override",
            overwriteConflicts = false,
          )
        )
      assertTrue(conflictAttempt.requiresConfirmation)
      assertEquals(1, conflictAttempt.conflictsFound.size)

      val overwriteAttempt =
        repo.evaluateAndApplyRoster(
          RosterAssignmentRequest(
            employeeIds = listOf(emp101.id),
            startDate = today,
            endDate = today,
            shiftId = afternoon.id,
            dutyTypeId = safety.id,
            notes = "Updated override",
            overwriteConflicts = true,
          )
        )
      assertFalse(overwriteAttempt.requiresConfirmation)
      assertEquals(1, overwriteAttempt.appliedCount)
    }

  @Test
  fun `deleting employee shift and duty clears future roster while preserving historical attendance`() =
    runBlocking {
      repo.ensureInitialized()
      val today = DateUtils.todayIso()
      insertUserCreatedBaseData(today)

      val emp099 = repo.employees.first().first { it.code == "EMP-099" }
      val morning = repo.shifts.first().first { it.name == "Morning Shift" }
      val night = repo.shifts.first().first { it.name == "Night Shift" }
      val gate = repo.dutyTypes.first().first { it.title == "Gate Control" }

      val rows =
        AttendanceCalculator.buildDailyRows(
          date = today,
          employees = repo.employees.first(),
          shifts = repo.shifts.first(),
          duties = repo.dutyTypes.first(),
          assignmentsForDate = repo.rosterAssignments.first().filter { it.date == today },
          attendanceForDate = emptyList(),
        )
      val kabirRow = rows.first { it.employee.id == emp099.id }
      val bivashNight = rows.first { it.employee.code == "EMP-101" && it.shiftId == night.id }
      val bivashMorning = rows.first { it.employee.code == "EMP-101" && it.shiftId == morning.id }

      repo.markAttendance(today, kabirRow, AttendanceStatus.ABSENT, "Before deletion")
      repo.markAttendance(today, bivashNight, AttendanceStatus.PRESENT, "Night worked")
      repo.markAttendance(today, bivashMorning, AttendanceStatus.PRESENT, "Gate covered")

      repo.deleteEmployeePreservingHistory(emp099.id)
      assertFalse(repo.employees.first().any { it.id == emp099.id })
      assertTrue(repo.attendanceRecords.first().any { it.employeeId == emp099.id })

      repo.deleteShiftPreservingHistory(night.id)
      assertFalse(repo.shifts.first().any { it.id == night.id })
      assertFalse(
        repo.rosterAssignments.first().any { it.shiftId == night.id && it.date >= today }
      )
      assertTrue(repo.attendanceRecords.first().any { it.shiftId == night.id })

      repo.deleteDutyTypePreservingHistory(gate.id)
      assertFalse(repo.dutyTypes.first().any { it.id == gate.id })
      assertFalse(repo.rosterAssignments.first().any { it.dutyTypeId == gate.id })
      assertTrue(repo.attendanceRecords.first().any { it.dutyTypeId == gate.id })

      val history =
        HistorySearchEngine.search(
          query = HistoryFilterQuery(startDate = DateUtils.addDays(today, -10), endDate = today),
          attendanceRecords = repo.attendanceRecords.first(),
          employees = repo.employees.first(),
          shifts = repo.shifts.first(),
          duties = repo.dutyTypes.first(),
        )
      val archivedEmp = history.firstOrNull { it.employeeCode == "EMP-099" }
      assertNotNull(archivedEmp)
      assertTrue(archivedEmp!!.isDeletedReference)
      assertEquals("Kabir Bose", archivedEmp.employeeName)
    }

  @Test
  fun `office index persists multi-field entries and participates in backup and restore`() =
    runBlocking {
      repo.ensureInitialized()
      assertEquals(0, repo.officeIndex.first().size)

      repo.saveOfficeIndexEntry(
        existingId = null,
        integerValue = 42,
        textValue = "Thermal Printer Rolls",
        category = "Supplies",
        subCategory = "Dispatch",
        notes = "Rack B3",
      )
      val afterAdd = repo.officeIndex.first()
      assertEquals(1, afterAdd.size)
      val added = afterAdd.first()
      assertEquals(42, added.integerValue)
      assertEquals("Supplies", added.category)
      assertEquals("Dispatch", added.subCategory)

      val (bundle, backupJson) = repo.exportBackupJson()
      assertEquals(1, bundle.recordCounts.officeIndex)

      repo.deleteOfficeIndexEntry(added.id)
      assertEquals(0, repo.officeIndex.first().size)

      val restoreResult = repo.restoreFromBackupJson(backupJson)
      assertTrue(restoreResult.success)
      assertEquals(1, repo.officeIndex.first().size)
      assertTrue(repo.officeIndex.first().any { it.textValue == "Thermal Printer Rolls" && it.integerValue == 42 })
    }

  @Test
  fun `theme preference defaults to SYSTEM and persists DAY and NIGHT selections`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefs = ThemePreferences(context)
    assertEquals(AppThemeMode.SYSTEM, prefs.getThemeMode())

    prefs.setThemeMode(AppThemeMode.NIGHT)
    assertEquals(AppThemeMode.NIGHT, ThemePreferences(context).getThemeMode())

    prefs.setThemeMode(AppThemeMode.DAY)
    assertEquals(AppThemeMode.DAY, ThemePreferences(context).getThemeMode())

    prefs.setThemeMode(AppThemeMode.SYSTEM)
    assertEquals(AppThemeMode.SYSTEM, ThemePreferences(context).getThemeMode())
  }

  @Test
  fun `ict categories sub-categories and entries support full CRUD, rename snapshots, and cascades`() =
    runBlocking {
      repo.ensureInitialized()
      assertEquals(0, repo.ictCategories.first().size)

      repo.saveIctCategory(null, "Server Metrics", "Core rack telemetry")
      val cat = repo.ictCategories.first().first()
      assertEquals("Server Metrics", cat.name)

      repo.saveIctSubCategory(null, cat.id, "CPU Load", "%")
      val sub = repo.ictSubCategories.first().first()
      assertEquals(cat.id, sub.categoryId)

      repo.saveIctEntry(null, cat.id, sub.id, "48.5", "Nominal morning reading")
      var entry = repo.ictEntries.first().first()
      assertEquals(48.5, entry.numericValue!!, 0.001)
      assertEquals("Server Metrics", entry.categoryNameSnapshot)
      assertEquals("CPU Load", entry.subCategoryNameSnapshot)

      // Rename category & sub-category -> snapshots propagate to existing entries
      repo.saveIctCategory(cat.id, "Infrastructure Telemetry", "Updated desc")
      repo.saveIctSubCategory(sub.id, cat.id, "Processor Load", "%")
      entry = repo.ictEntries.first().first()
      assertEquals("Infrastructure Telemetry", entry.categoryNameSnapshot)
      assertEquals("Processor Load", entry.subCategoryNameSnapshot)

      // Delete sub-category -> clears subCategoryId on entry while keeping entry intact
      repo.deleteIctSubCategory(sub.id)
      assertEquals(0, repo.ictSubCategories.first().size)
      entry = repo.ictEntries.first().first()
      assertEquals(null, entry.subCategoryId)

      // Delete category -> cleans up category and linked entries
      repo.deleteIctCategory(cat.id)
      assertEquals(0, repo.ictCategories.first().size)
      assertEquals(0, repo.ictEntries.first().size)
    }

  @Test
  fun `batch single date roster commits multiple shifts atomically and handles conflict overwrite`() =
    runBlocking {
      repo.ensureInitialized()
      val today = DateUtils.todayIso()
      insertUserCreatedBaseData(today)

      val emp099 = repo.employees.first().first { it.code == "EMP-099" }
      val emp101 = repo.employees.first().first { it.code == "EMP-101" }
      val morning = repo.shifts.first().first { it.name == "Morning Shift" }
      val afternoon = repo.shifts.first().first { it.name == "Afternoon Shift" }
      val night = repo.shifts.first().first { it.name == "Night Shift" }
      val gate = repo.dutyTypes.first().first { it.title == "Gate Control" }
      val safety = repo.dutyTypes.first().first { it.title == "Safety Inspection" }

      val outcome =
        repo.evaluateAndApplyBatchRoster(
          BatchRosterAssignmentRequest(
            date = today,
            batches =
              listOf(
                BatchShiftAssignmentItem(morning.id, listOf(emp099.id), gate.id, "Batch morning"),
                BatchShiftAssignmentItem(afternoon.id, listOf(emp101.id), safety.id, "Batch afternoon"),
                BatchShiftAssignmentItem(night.id, listOf(emp099.id), safety.id, "Batch night"),
              ),
            overwriteConflicts = false,
          )
        )
      assertFalse(outcome.requiresConfirmation)
      assertEquals(3, outcome.appliedCount)
      assertEquals(5, repo.rosterAssignments.first().count { it.date == today })
    }

  @Test
  fun `default Computer department supports rename unlimited departments nested duties and permanent deletion`() =
    runBlocking {
      repo.ensureInitialized()
      val initialDept = repo.departments.first().single()
      assertEquals(DEFAULT_DEPARTMENT_ID, initialDept.id)
      assertEquals("Computer", initialDept.name)

      // Rename & edit default Computer department
      repo.saveDepartment(initialDept.id, "ICT & Computer", "Primary technical wing", true)
      assertEquals("ICT & Computer", repo.departments.first().single().name)

      // Add multiple extra departments and nested duties
      repo.saveDepartment(null, "Logistics", "Fleet & gate desk", true)
      repo.saveDepartment(null, "Security", "Perimeter patrol", true)
      val departments = repo.departments.first()
      assertEquals(3, departments.size)

      val ictDept = departments.first { it.id == DEFAULT_DEPARTMENT_ID }
      val logDept = departments.first { it.name == "Logistics" }
      repo.saveShift(null, "Night Shift", "22:00", "06:00", true, true)
      val nightShift = repo.shifts.first().single()

      repo.saveDutyType(null, "Server Watch", "Rack alerts", true, ictDept.id, nightShift.id)
      repo.saveDutyType(null, "Dispatch", "Gate manifests", true, logDept.id, null)
      val duties = repo.dutyTypes.first()
      assertEquals(2, duties.size)
      assertEquals(ictDept.id, duties.first { it.title == "Server Watch" }.departmentId)
      assertEquals(nightShift.id, duties.first { it.title == "Server Watch" }.defaultShiftId)

      // Delete default Computer department -> nested duties cleared and ensureInitialized never recreates it
      repo.deleteDepartmentPreservingHistory(ictDept.id)
      assertFalse(repo.departments.first().any { it.id == DEFAULT_DEPARTMENT_ID })
      assertFalse(repo.dutyTypes.first().any { it.title == "Server Watch" })

      repo.ensureInitialized()
      assertFalse(repo.departments.first().any { it.id == DEFAULT_DEPARTMENT_ID })
      assertEquals(2, repo.departments.first().size)
    }

  @Test
  fun `deleting department preserves historical departmentNameSnapshot and restores via backup`() =
    runBlocking {
      repo.ensureInitialized()
      val today = DateUtils.todayIso()
      val computerDept = repo.departments.first().single { it.id == DEFAULT_DEPARTMENT_ID }

      repo.saveEmployee(null, "EMP-101", "Bivash Sen", "Lead", true)
      repo.saveShift(null, "Morning Shift", "08:00", "16:00", false, true)
      repo.saveDutyType(null, "Helpdesk", "IT support", true, computerDept.id, null)

      val emp = repo.employees.first().single()
      val shift = repo.shifts.first().single()
      val duty = repo.dutyTypes.first().single()

      repo.evaluateAndApplyRoster(
        RosterAssignmentRequest(
          employeeIds = listOf(emp.id),
          startDate = today,
          endDate = today,
          shiftId = shift.id,
          dutyTypeId = duty.id,
          notes = "Assigned to Computer dept",
          overwriteConflicts = false,
          departmentId = computerDept.id,
        )
      )

      val rows =
        AttendanceCalculator.buildDailyRows(
          date = today,
          employees = repo.employees.first(),
          shifts = repo.shifts.first(),
          duties = repo.dutyTypes.first(),
          assignmentsForDate = repo.rosterAssignments.first(),
          attendanceForDate = emptyList(),
          departments = repo.departments.first(),
        )
      repo.markAttendance(today, rows.single(), AttendanceStatus.PRESENT, "On desk")

      val (_, backupJson) = repo.exportBackupJson()

      repo.deleteDepartmentPreservingHistory(computerDept.id)
      assertTrue(repo.departments.first().isEmpty())
      assertTrue(repo.dutyTypes.first().isEmpty())
      assertEquals(1, repo.rosterAssignments.first().size)
      assertEquals(null, repo.rosterAssignments.first().single().departmentId)
      assertEquals(null, repo.rosterAssignments.first().single().dutyTypeId)

      val history =
        HistorySearchEngine.search(
          query =
            HistoryFilterQuery(
              startDate = DateUtils.addDays(today, -1),
              endDate = today,
              departmentId = computerDept.id,
            ),
          attendanceRecords = repo.attendanceRecords.first(),
          employees = repo.employees.first(),
          shifts = repo.shifts.first(),
          duties = repo.dutyTypes.first(),
          departments = repo.departments.first(),
        )
      assertEquals(1, history.size)
      assertEquals("Computer", history.single().departmentName)
      assertTrue(history.single().isDeletedReference)

      val restore = repo.restoreFromBackupJson(backupJson)
      assertTrue(restore.success)
      assertEquals(1, repo.departments.first().size)
      assertEquals("Computer", repo.departments.first().single().name)
      assertEquals(1, repo.dutyTypes.first().size)
    }

  @Test
  fun `employee registration persists optional defaultDepartmentId, survives backup, and clears when dept deleted`() =
    runBlocking {
      repo.ensureInitialized()
      val computerDept = repo.departments.first().single { it.id == DEFAULT_DEPARTMENT_ID }

      repo.saveEmployee(
        existingId = null,
        code = "EMP-301",
        name = "Riya Paul",
        designation = "SysAdmin",
        isActive = true,
        defaultDepartmentId = computerDept.id,
      )
      repo.saveEmployee(
        existingId = null,
        code = "EMP-302",
        name = "Arjun Das",
        designation = "Floater",
        isActive = true,
        defaultDepartmentId = null,
      )

      val riya = repo.employees.first().first { it.code == "EMP-301" }
      val arjun = repo.employees.first().first { it.code == "EMP-302" }
      assertEquals(computerDept.id, riya.defaultDepartmentId)
      assertEquals(null, arjun.defaultDepartmentId)

      val (_, backupJson) = repo.exportBackupJson()

      // Deleting the department clears defaultDepartmentId while keeping the employee
      repo.deleteDepartmentPreservingHistory(computerDept.id)
      val riyaAfterDeptDelete = repo.employees.first().first { it.id == riya.id }
      assertEquals(null, riyaAfterDeptDelete.defaultDepartmentId)

      // Restoring from backup restores department and employee defaultDepartmentId
      val restore = repo.restoreFromBackupJson(backupJson)
      assertTrue(restore.success)
      assertEquals(computerDept.id, repo.employees.first().first { it.code == "EMP-301" }.defaultDepartmentId)
    }

  @Test
  fun `settings exposes About section and BuildConfig version uses Android Studio DD Mon YYYY format`() {
    assertTrue(com.example.ui.state.SettingsSection.entries.any { it.name == "ABOUT" && it.title == "About" })
    assertTrue(
      Regex("""^\d{2}\. [A-Z][a-z]{2}\. \d{4}$""").matches(BuildConfig.APP_BUILD_DATE_VERSION)
    )
  }

  @Test
  fun `roster assignment auto-marks remaining active employees OFF and clears stale auto-OFF when assigned`() =
    runBlocking {
      repo.ensureInitialized()
      val today = DateUtils.todayIso()
      repo.saveEmployee(null, "EMP-1", "Asha", "Lead", true)
      repo.saveEmployee(null, "EMP-2", "Ravi", "Clerk", true)
      repo.saveShift(null, "Morning", "08:00", "16:00", false, true)

      val asha = repo.employees.first().first { it.code == "EMP-1" }
      val ravi = repo.employees.first().first { it.code == "EMP-2" }
      val shift = repo.shifts.first().single()

      repo.evaluateAndApplyRoster(
        RosterAssignmentRequest(
          employeeIds = listOf(asha.id),
          startDate = today,
          endDate = today,
          shiftId = shift.id,
          dutyTypeId = null,
          notes = "",
          overwriteConflicts = false,
        )
      )

      val raviAutoOff =
        repo.attendanceRecords.first().firstOrNull { it.date == today && it.employeeId == ravi.id }
      assertNotNull(raviAutoOff)
      assertEquals(AttendanceStatus.OFF, raviAutoOff!!.status)
      assertEquals(AttendanceCalculator.AUTO_OFF_REMARK, raviAutoOff.remarks)

      // Later assign Ravi to Morning shift -> stale auto-OFF is cleaned up automatically
      repo.evaluateAndApplyRoster(
        RosterAssignmentRequest(
          employeeIds = listOf(ravi.id),
          startDate = today,
          endDate = today,
          shiftId = shift.id,
          dutyTypeId = null,
          notes = "",
          overwriteConflicts = false,
        )
      )
      assertFalse(
        repo.attendanceRecords.first().any {
          it.date == today &&
            it.employeeId == ravi.id &&
            it.remarks == AttendanceCalculator.AUTO_OFF_REMARK
        }
      )
    }

  @Test
  fun `task manager and notes support full persistence, comma bulk creation, and cascade delete`() =
    runBlocking {
      repo.ensureInitialized()
      assertEquals(0, repo.taskCategories.first().size)
      assertEquals(0, repo.notes.first().size)

      val addedCats = repo.saveTaskCategories("Ops, Hardware, ", useCommaSeparator = true)
      assertEquals(2, addedCats)
      val ops = repo.taskCategories.first().first { it.name == "Ops" }

      val addedItems = repo.saveTaskItems(ops.id, "Check UPS, Verify Rack", useCommaSeparator = true)
      assertEquals(2, addedItems)
      val ups = repo.taskItems.first().first { it.title == "Check UPS" }
      repo.toggleTaskItemCompleted(ups)
      assertTrue(repo.taskItems.first().first { it.id == ups.id }.isCompleted)

      repo.deleteTaskCategory(ops.id)
      assertFalse(repo.taskCategories.first().any { it.id == ops.id })
      assertFalse(repo.taskItems.first().any { it.categoryId == ops.id })

      repo.saveNote(null, "Shift Memo", "Test UPS monthly")
      val note = repo.notes.first().single()
      assertEquals("Shift Memo", note.title)
      repo.deleteNote(note.id)
      assertTrue(repo.notes.first().isEmpty())
    }

  @Test
  fun `registered offices and office case records support CRUD, I-O reassignment history, snapshot rename propagation, and backup restore`() =
    runBlocking {
      repo.ensureInitialized()
      assertTrue(repo.registeredOffices.first().isEmpty())
      assertTrue(repo.officeCaseRecords.first().isEmpty())

      repo.saveRegisteredOffice(null, "Sadar Thana", "STPS", isActive = true)
      val office = repo.registeredOffices.first().single()
      assertEquals("STPS", office.shortCode)

      val today = DateUtils.todayIso()
      repo.saveNewOfficeCaseRecord(
        officeId = office.id,
        recordDate = today,
        firNumber = 404,
        ioName = "SI Vivek Rao",
        actType = LegalActType.BNS.code,
        customActName = "",
        sections = listOf(CaseSectionAttachment("BNS", "BNS 2023", "103", "Punishment for murder")),
        notes = "Case opened",
      )

      val savedCase = repo.officeCaseRecords.first().single()
      assertEquals(404, savedCase.firNumber)
      assertEquals("SI Vivek Rao", savedCase.currentIoName)
      assertEquals(1, savedCase.ioHistory.size)

      // Rename office & short code -> snapshot on existing case records updates automatically
      repo.saveRegisteredOffice(office.id, "Sadar Central Station", "SCS", isActive = true)
      val afterOfficeRename = repo.officeCaseRecords.first().single()
      assertEquals("Sadar Central Station", afterOfficeRename.officeNameSnapshot)
      assertEquals("SCS", afterOfficeRename.officeShortCodeSnapshot)

      // Reassign new I/O -> preserves prior I/O in history
      repo.updateOfficeCaseRecord(
        existing = afterOfficeRename,
        officeId = office.id,
        recordDate = today,
        firNumber = 404,
        editedCurrentIoName = "SI Vivek Rao",
        actType = LegalActType.BNS.code,
        customActName = "",
        sections =
          afterOfficeRename.sections +
            CaseSectionAttachment("POCSO", "POCSO Act", "4", "Punishment for penetrative sexual assault"),
        notes = "Transferred to Special Cell",
        newAssignedIoName = "Insp. Ananya Roy",
        newIoAssignedDate = today,
        newIoRemarks = "Special Cell",
      )

      val reassigned = repo.officeCaseRecords.first().single()
      assertEquals("Insp. Ananya Roy", reassigned.currentIoName)
      assertEquals(2, reassigned.ioHistory.size)
      assertEquals("SI Vivek Rao", reassigned.ioHistory.first().ioName)
      assertEquals(2, reassigned.sections.size)

      val (_, backupJson) = repo.exportBackupJson()
      repo.deleteOfficeCaseRecord(reassigned.id)
      repo.deleteRegisteredOffice(office.id)
      assertTrue(repo.officeCaseRecords.first().isEmpty())
      assertTrue(repo.registeredOffices.first().isEmpty())

      val restore = repo.restoreFromBackupJson(backupJson)
      assertTrue(restore.success)
      assertEquals(1, repo.registeredOffices.first().size)
      assertEquals("SCS", repo.registeredOffices.first().single().shortCode)
      assertEquals(1, repo.officeCaseRecords.first().size)
      assertEquals("Insp. Ananya Roy", repo.officeCaseRecords.first().single().currentIoName)
      assertEquals(2, repo.officeCaseRecords.first().single().ioHistory.size)
    }
}
