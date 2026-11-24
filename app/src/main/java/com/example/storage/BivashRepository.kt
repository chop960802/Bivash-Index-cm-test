package com.example.storage

import androidx.room.withTransaction
import com.example.logic.AttendanceCalculator
import com.example.logic.BackupManager
import com.example.logic.DateUtils
import com.example.logic.OfficeCaseEngine
import com.example.logic.OfflineUpdateEngine
import com.example.logic.RosterConflictDetector
import com.example.models.APP_VERSION_NAME
import com.example.models.AppMeta
import com.example.models.AttendanceRecord
import com.example.models.AttendanceStatus
import com.example.models.BackupBundle
import com.example.models.BackupValidationResult
import com.example.models.BatchRosterAssignmentRequest
import com.example.models.CURRENT_SCHEMA_VERSION
import com.example.models.CaseSectionAttachment
import com.example.models.DEFAULT_DEPARTMENT_ID
import com.example.models.DEFAULT_DEPARTMENT_NAME
import com.example.models.DailyAttendanceRow
import com.example.models.Department
import com.example.models.DutyType
import com.example.models.Employee
import com.example.models.IctCategory
import com.example.models.IctEntry
import com.example.models.IctSubCategory
import com.example.models.NoteEntry
import com.example.models.OfficeCaseRecord
import com.example.models.OfficeIndexEntry
import com.example.models.OfflineUpdateApplyResult
import com.example.models.OfflineUpdatePack
import com.example.models.OfflineUpdatePreview
import com.example.models.RegisteredOffice
import com.example.models.RestoreExecutionResult
import com.example.models.RosterApplyOutcome
import com.example.models.RosterAssignment
import com.example.models.RosterAssignmentRequest
import com.example.models.SafetySnapshotInfo
import com.example.models.Shift
import com.example.models.TaskCategory
import com.example.models.TaskInputParser
import com.example.models.TaskItem
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BivashRepository(private val database: BivashDatabase) {
  private val dao = database.dao()

  val employees: Flow<List<Employee>> =
    dao.observeEmployees().map { list -> list.map { it.toDomain() } }

  val shifts: Flow<List<Shift>> =
    dao.observeShifts().map { list -> list.map { it.toDomain() } }

  val departments: Flow<List<Department>> =
    dao.observeDepartments().map { list -> list.map { it.toDomain() } }

  val dutyTypes: Flow<List<DutyType>> =
    dao.observeDutyTypes().map { list -> list.map { it.toDomain() } }

  val rosterAssignments: Flow<List<RosterAssignment>> =
    dao.observeRosterAssignments().map { list -> list.map { it.toDomain() } }

  val attendanceRecords: Flow<List<AttendanceRecord>> =
    dao.observeAttendanceRecords().map { list -> list.map { it.toDomain() } }

  val appMeta: Flow<AppMeta?> =
    dao.observeAppMeta().map { it?.toDomain() }

  val safetySnapshots: Flow<List<SafetySnapshotInfo>> =
    dao.observeSafetySnapshots().map { list -> list.map { it.toDomain() } }

  val officeIndex: Flow<List<OfficeIndexEntry>> =
    dao.observeOfficeIndex().map { list -> list.map { it.toDomain() } }

  val registeredOffices: Flow<List<RegisteredOffice>> =
    dao.observeRegisteredOffices().map { list -> list.map { it.toDomain() } }

  val officeCaseRecords: Flow<List<OfficeCaseRecord>> =
    dao.observeOfficeCaseRecords().map { list -> list.map { it.toDomain() } }

  val ictCategories: Flow<List<IctCategory>> =
    dao.observeIctCategories().map { list -> list.map { it.toDomain() } }

  val ictSubCategories: Flow<List<IctSubCategory>> =
    dao.observeIctSubCategories().map { list -> list.map { it.toDomain() } }

  val ictEntries: Flow<List<IctEntry>> =
    dao.observeIctEntries().map { list -> list.map { it.toDomain() } }

  val taskCategories: Flow<List<TaskCategory>> =
    dao.observeTaskCategories().map { list -> list.map { it.toDomain() } }

  val taskItems: Flow<List<TaskItem>> =
    dao.observeTaskItems().map { list -> list.map { it.toDomain() } }

  val notes: Flow<List<NoteEntry>> =
    dao.observeNotes().map { list -> list.map { it.toDomain() } }

  suspend fun ensureInitialized() {
    database.withTransaction {
      if (dao.getAppMeta() == null) {
        val now = System.currentTimeMillis()
        dao.upsertAppMeta(
          AppMeta(
              id = "singleton",
              schemaVersion = CURRENT_SCHEMA_VERSION,
              appVersion = APP_VERSION_NAME,
              lastBackupAt = null,
              lastUpdatePackId = null,
              lastUpdatePackVersion = null,
              referenceRulesJson = "[]",
              seededAt = now,
            )
            .toEntity()
        )
        if (dao.getAllDepartments().isEmpty()) {
          dao.upsertDepartment(
            DepartmentEntity(
              id = DEFAULT_DEPARTMENT_ID,
              name = DEFAULT_DEPARTMENT_NAME,
              description = "Default department (editable & deletable)",
              isActive = true,
              createdAt = now,
              updatedAt = now,
            )
          )
        }
      }
    }
  }

  suspend fun saveOfficeIndexEntry(
    existingId: String?,
    integerValue: Int,
    textValue: String,
    category: String,
    subCategory: String,
    notes: String,
  ) {
    val now = System.currentTimeMillis()
    val existing = existingId?.let { id -> dao.getAllOfficeIndex().firstOrNull { it.id == id } }
    val entity =
      OfficeIndexEntity(
        id = existing?.id ?: "idx_${UUID.randomUUID().toString().take(8)}",
        integerValue = integerValue,
        textValue = textValue.trim(),
        category = category.trim(),
        subCategory = subCategory.trim(),
        notes = notes.trim(),
        createdAt = existing?.createdAt ?: now,
        updatedAt = now,
      )
    dao.upsertOfficeIndexEntry(entity)
  }

  suspend fun deleteOfficeIndexEntry(id: String) {
    dao.deleteOfficeIndexById(id)
  }

  suspend fun saveRegisteredOffice(
    existingId: String?,
    officeName: String,
    shortCode: String,
    isActive: Boolean,
  ) {
    val cleanName = officeName.trim()
    val cleanCode = shortCode.trim().uppercase()
    if (cleanName.isBlank() || cleanCode.isBlank()) return
    val now = System.currentTimeMillis()
    database.withTransaction {
      val existing =
        existingId?.let { id -> dao.getAllRegisteredOffices().firstOrNull { it.id == id } }
      val officeId = existing?.id ?: "off_${UUID.randomUUID().toString().take(8)}"
      dao.upsertRegisteredOffice(
        RegisteredOfficeEntity(
          id = officeId,
          officeName = cleanName,
          shortCode = cleanCode,
          isActive = isActive,
          createdAt = existing?.createdAt ?: now,
          updatedAt = now,
        )
      )
      if (existing != null) {
        dao.updateOfficeCaseSnapshots(officeId, cleanName, cleanCode)
      }
    }
  }

  suspend fun toggleRegisteredOfficeActive(office: RegisteredOffice) {
    dao.upsertRegisteredOffice(
      office.copy(isActive = !office.isActive, updatedAt = System.currentTimeMillis()).toEntity()
    )
  }

  suspend fun deleteRegisteredOffice(officeId: String) {
    dao.deleteRegisteredOfficeById(officeId)
  }

  suspend fun saveNewOfficeCaseRecord(
    officeId: String,
    recordDate: String,
    firNumber: Int,
    ioName: String,
    actType: String,
    customActName: String,
    sections: List<CaseSectionAttachment>,
    notes: String,
  ) {
    val cleanIo = ioName.trim()
    if (officeId.isBlank() || firNumber <= 0 || cleanIo.isBlank() || !DateUtils.isValidIsoDate(recordDate)) {
      return
    }
    val now = System.currentTimeMillis()
    database.withTransaction {
      val office = dao.getAllRegisteredOffices().firstOrNull { it.id == officeId }
      val record =
        OfficeCaseRecord(
          id = "case_${UUID.randomUUID().toString().take(8)}",
          officeId = officeId,
          officeNameSnapshot = office?.officeName ?: "Office",
          officeShortCodeSnapshot = office?.shortCode ?: "",
          recordDate = recordDate,
          firNumber = firNumber,
          currentIoName = cleanIo,
          actType = actType,
          customActName = customActName.trim(),
          sections = sections,
          ioHistory = OfficeCaseEngine.buildInitialIoHistory(cleanIo, recordDate),
          notes = notes.trim(),
          createdAt = now,
          updatedAt = now,
        )
      dao.upsertOfficeCaseRecord(record.toEntity())
    }
  }

  suspend fun updateOfficeCaseRecord(
    existing: OfficeCaseRecord,
    officeId: String,
    recordDate: String,
    firNumber: Int,
    editedCurrentIoName: String,
    actType: String,
    customActName: String,
    sections: List<CaseSectionAttachment>,
    notes: String,
    newAssignedIoName: String?,
    newIoAssignedDate: String?,
    newIoRemarks: String,
  ) {
    if (firNumber <= 0 || !DateUtils.isValidIsoDate(recordDate)) return
    database.withTransaction {
      val office = dao.getAllRegisteredOffices().firstOrNull { it.id == officeId }?.toDomain()
      val updated =
        OfficeCaseEngine.applyEditWithOptionalIoReassignment(
          existing = existing,
          office = office,
          recordDate = recordDate,
          firNumber = firNumber,
          editedCurrentIoName = editedCurrentIoName,
          actType = actType,
          customActName = customActName,
          sections = sections,
          notes = notes,
          newAssignedIoName = newAssignedIoName,
          newIoAssignedDate = newIoAssignedDate,
          newIoRemarks = newIoRemarks,
        )
      dao.upsertOfficeCaseRecord(updated.toEntity())
    }
  }

  suspend fun deleteOfficeCaseRecord(recordId: String) {
    dao.deleteOfficeCaseRecordById(recordId)
  }

  suspend fun saveIctCategory(
    existingId: String?,
    name: String,
    description: String,
  ) {
    val cleanName = name.trim()
    if (cleanName.isBlank()) return
    val now = System.currentTimeMillis()
    database.withTransaction {
      val existing = existingId?.let { id -> dao.getAllIctCategories().firstOrNull { it.id == id } }
      val catId = existing?.id ?: "ict_cat_${UUID.randomUUID().toString().take(8)}"
      dao.upsertIctCategory(
        IctCategoryEntity(
          id = catId,
          name = cleanName,
          description = description.trim(),
          createdAt = existing?.createdAt ?: now,
          updatedAt = now,
        )
      )
      if (existing != null && existing.name != cleanName) {
        dao.updateIctEntryCategorySnapshot(catId, cleanName)
      }
    }
  }

  suspend fun deleteIctCategory(categoryId: String) {
    database.withTransaction {
      dao.deleteIctEntriesByCategoryId(categoryId)
      dao.deleteIctSubCategoriesByCategoryId(categoryId)
      dao.deleteIctCategoryById(categoryId)
    }
  }

  suspend fun saveIctSubCategory(
    existingId: String?,
    categoryId: String,
    name: String,
    unitLabel: String,
  ) {
    val cleanName = name.trim()
    if (categoryId.isBlank() || cleanName.isBlank()) return
    val now = System.currentTimeMillis()
    database.withTransaction {
      val existing =
        existingId?.let { id -> dao.getAllIctSubCategories().firstOrNull { it.id == id } }
      val subId = existing?.id ?: "ict_sub_${UUID.randomUUID().toString().take(8)}"
      dao.upsertIctSubCategory(
        IctSubCategoryEntity(
          id = subId,
          categoryId = categoryId,
          name = cleanName,
          unitLabel = unitLabel.trim(),
          createdAt = existing?.createdAt ?: now,
          updatedAt = now,
        )
      )
      if (existing != null && existing.name != cleanName) {
        dao.updateIctEntrySubCategorySnapshot(subId, cleanName)
      }
    }
  }

  suspend fun deleteIctSubCategory(subCategoryId: String) {
    database.withTransaction {
      dao.clearIctEntrySubCategory(subCategoryId)
      dao.deleteIctSubCategoryById(subCategoryId)
    }
  }

  suspend fun saveIctEntry(
    existingId: String?,
    categoryId: String,
    subCategoryId: String?,
    valueText: String,
    note: String,
    recordedAt: Long = System.currentTimeMillis(),
  ) {
    val cleanValue = valueText.trim()
    if (categoryId.isBlank() || cleanValue.isBlank()) return
    val now = System.currentTimeMillis()
    database.withTransaction {
      val existing = existingId?.let { id -> dao.getAllIctEntries().firstOrNull { it.id == id } }
      val catName = dao.getAllIctCategories().firstOrNull { it.id == categoryId }?.name.orEmpty()
      val subName =
        subCategoryId?.let { sid ->
          dao.getAllIctSubCategories().firstOrNull { it.id == sid }?.name
        }.orEmpty()
      dao.upsertIctEntry(
        IctEntryEntity(
          id = existing?.id ?: "ict_ent_${UUID.randomUUID().toString().take(8)}",
          categoryId = categoryId,
          subCategoryId = subCategoryId,
          valueText = cleanValue,
          numericValue = cleanValue.toDoubleOrNull(),
          note = note.trim(),
          categoryNameSnapshot = catName,
          subCategoryNameSnapshot = subName,
          recordedAt = existing?.recordedAt ?: recordedAt,
          updatedAt = now,
        )
      )
    }
  }

  suspend fun deleteIctEntry(entryId: String) {
    dao.deleteIctEntryById(entryId)
  }

  suspend fun saveTaskCategories(rawInput: String, useCommaSeparator: Boolean): Int {
    val entries = TaskInputParser.parseEntries(rawInput, useCommaSeparator)
    if (entries.isEmpty()) return 0
    val now = System.currentTimeMillis()
    val entities =
      entries.mapIndexed { idx, name ->
        TaskCategoryEntity(
          id = "task_cat_${UUID.randomUUID().toString().take(8)}",
          name = name,
          createdAt = now + idx,
          updatedAt = now + idx,
        )
      }
    dao.upsertTaskCategories(entities)
    return entities.size
  }

  suspend fun renameTaskCategory(category: TaskCategory, newName: String) {
    val clean = newName.trim()
    if (clean.isBlank()) return
    dao.upsertTaskCategory(
      category.copy(name = clean, updatedAt = System.currentTimeMillis()).toEntity()
    )
  }

  suspend fun deleteTaskCategory(categoryId: String) {
    database.withTransaction {
      dao.deleteTaskItemsByCategoryId(categoryId)
      dao.deleteTaskCategoryById(categoryId)
    }
  }

  suspend fun saveTaskItems(
    categoryId: String,
    rawInput: String,
    useCommaSeparator: Boolean,
  ): Int {
    if (categoryId.isBlank()) return 0
    val entries = TaskInputParser.parseEntries(rawInput, useCommaSeparator)
    if (entries.isEmpty()) return 0
    val now = System.currentTimeMillis()
    val entities =
      entries.mapIndexed { idx, title ->
        TaskItemEntity(
          id = "task_item_${UUID.randomUUID().toString().take(8)}",
          categoryId = categoryId,
          title = title,
          isCompleted = false,
          createdAt = now + idx,
          updatedAt = now + idx,
        )
      }
    dao.upsertTaskItems(entities)
    return entities.size
  }

  suspend fun toggleTaskItemCompleted(item: TaskItem) {
    dao.upsertTaskItem(
      item.copy(isCompleted = !item.isCompleted, updatedAt = System.currentTimeMillis()).toEntity()
    )
  }

  suspend fun renameTaskItem(item: TaskItem, newTitle: String) {
    val clean = newTitle.trim()
    if (clean.isBlank()) return
    dao.upsertTaskItem(
      item.copy(title = clean, updatedAt = System.currentTimeMillis()).toEntity()
    )
  }

  suspend fun deleteTaskItem(itemId: String) {
    dao.deleteTaskItemById(itemId)
  }

  suspend fun saveNote(existingId: String?, title: String, body: String) {
    val cleanTitle = title.trim()
    val cleanBody = body.trim()
    if (cleanTitle.isBlank() && cleanBody.isBlank()) return
    val now = System.currentTimeMillis()
    val existing = existingId?.let { id -> dao.getAllNotes().firstOrNull { it.id == id } }
    val entity =
      NoteEntity(
        id = existing?.id ?: "note_${UUID.randomUUID().toString().take(8)}",
        title = cleanTitle.ifBlank { "Untitled Note" },
        body = cleanBody,
        createdAt = existing?.createdAt ?: now,
        updatedAt = now,
      )
    dao.upsertNote(entity)
  }

  suspend fun deleteNote(noteId: String) {
    dao.deleteNoteById(noteId)
  }

  suspend fun saveEmployee(
    existingId: String?,
    code: String,
    name: String,
    designation: String,
    isActive: Boolean,
    defaultDepartmentId: String? = null,
  ) {
    val now = System.currentTimeMillis()
    val existing = existingId?.let { id -> dao.getAllEmployees().firstOrNull { it.id == id } }
    val entity =
      EmployeeEntity(
        id = existing?.id ?: "emp_${UUID.randomUUID().toString().take(8)}",
        code = code.trim().uppercase(),
        name = name.trim(),
        designation = designation.trim(),
        isActive = isActive,
        createdAt = existing?.createdAt ?: now,
        updatedAt = now,
        defaultDepartmentId = defaultDepartmentId?.takeIf { it.isNotBlank() },
      )
    dao.upsertEmployee(entity)
  }

  suspend fun toggleEmployeeActive(employee: Employee) {
    dao.upsertEmployee(
      employee.copy(isActive = !employee.isActive, updatedAt = System.currentTimeMillis()).toEntity()
    )
  }

  suspend fun deleteEmployeePreservingHistory(employeeId: String) {
    val today = DateUtils.todayIso()
    database.withTransaction {
      dao.deleteFutureRosterByEmployeeId(employeeId, today)
      dao.deleteEmployeeById(employeeId)
    }
  }

  suspend fun saveShift(
    existingId: String?,
    name: String,
    startTime: String,
    endTime: String,
    isNightShift: Boolean,
    isActive: Boolean,
  ) {
    val now = System.currentTimeMillis()
    val existing = existingId?.let { id -> dao.getAllShifts().firstOrNull { it.id == id } }
    val entity =
      ShiftEntity(
        id = existing?.id ?: "shift_${UUID.randomUUID().toString().take(8)}",
        name = name.trim(),
        startTime = startTime.trim(),
        endTime = endTime.trim(),
        isNightShift = isNightShift || name.contains("night", ignoreCase = true),
        isActive = isActive,
        createdAt = existing?.createdAt ?: now,
        updatedAt = now,
      )
    dao.upsertShift(entity)
  }

  suspend fun toggleShiftActive(shift: Shift) {
    dao.upsertShift(
      shift.copy(isActive = !shift.isActive, updatedAt = System.currentTimeMillis()).toEntity()
    )
  }

  suspend fun deleteShiftPreservingHistory(shiftId: String) {
    val today = DateUtils.todayIso()
    database.withTransaction {
      dao.deleteFutureRosterByShiftId(shiftId, today)
      dao.deleteShiftById(shiftId)
    }
  }

  suspend fun saveDepartment(
    existingId: String?,
    name: String,
    description: String,
    isActive: Boolean,
  ) {
    val cleanName = name.trim()
    if (cleanName.isBlank()) return
    val now = System.currentTimeMillis()
    val existing = existingId?.let { id -> dao.getAllDepartments().firstOrNull { it.id == id } }
    val entity =
      DepartmentEntity(
        id = existing?.id ?: "dept_${UUID.randomUUID().toString().take(8)}",
        name = cleanName,
        description = description.trim(),
        isActive = isActive,
        createdAt = existing?.createdAt ?: now,
        updatedAt = now,
      )
    dao.upsertDepartment(entity)
  }

  suspend fun toggleDepartmentActive(department: Department) {
    dao.upsertDepartment(
      department.copy(isActive = !department.isActive, updatedAt = System.currentTimeMillis())
        .toEntity()
    )
  }

  suspend fun deleteDepartmentPreservingHistory(departmentId: String) {
    database.withTransaction {
      val deptDuties = dao.getAllDutyTypes().filter { it.departmentId == departmentId }
      deptDuties.forEach { duty -> dao.clearRosterDuty(duty.id) }
      dao.deleteDutiesByDepartmentId(departmentId)
      dao.clearRosterDepartment(departmentId)
      dao.clearEmployeeDefaultDepartment(departmentId)
      dao.deleteDepartmentById(departmentId)
    }
  }

  suspend fun saveDutyType(
    existingId: String?,
    title: String,
    description: String,
    isActive: Boolean,
    departmentId: String = "",
    defaultShiftId: String? = null,
  ) {
    val cleanTitle = title.trim()
    if (cleanTitle.isBlank()) return
    val now = System.currentTimeMillis()
    val existing = existingId?.let { id -> dao.getAllDutyTypes().firstOrNull { it.id == id } }
    val entity =
      DutyTypeEntity(
        id = existing?.id ?: "duty_${UUID.randomUUID().toString().take(8)}",
        title = cleanTitle,
        description = description.trim(),
        isActive = isActive,
        createdAt = existing?.createdAt ?: now,
        updatedAt = now,
        departmentId = departmentId.trim(),
        defaultShiftId = defaultShiftId?.takeIf { it.isNotBlank() },
      )
    dao.upsertDutyType(entity)
  }

  suspend fun toggleDutyActive(duty: DutyType) {
    dao.upsertDutyType(
      duty.copy(isActive = !duty.isActive, updatedAt = System.currentTimeMillis()).toEntity()
    )
  }

  suspend fun deleteDutyTypePreservingHistory(dutyId: String) {
    database.withTransaction {
      dao.clearRosterDuty(dutyId)
      dao.deleteDutyById(dutyId)
    }
  }

  suspend fun markAttendance(
    date: String,
    row: DailyAttendanceRow,
    status: AttendanceStatus,
    remarks: String,
  ) {
    database.withTransaction {
      val existing = dao.getAttendanceByKey(date, row.employee.id, row.shiftId)?.toDomain()
      val updated =
        AttendanceCalculator.createUpsertedRecord(
          date = date,
          row = row.copy(existingRecord = existing ?: row.existingRecord),
          status = status,
          remarks = remarks,
        )
      dao.upsertAttendanceRecord(updated.toEntity())
    }
  }

  suspend fun markRemainingAsPresent(
    date: String,
    rows: List<DailyAttendanceRow>,
    onlyScheduled: Boolean,
  ): Int {
    val toInsert =
      AttendanceCalculator.buildBulkMarkPresentRecords(
        date = date,
        rows = rows,
        onlyScheduled = onlyScheduled,
      )
    if (toInsert.isEmpty()) return 0
    database.withTransaction {
      val deduped =
        toInsert.map { rec ->
          val existing = dao.getAttendanceByKey(rec.date, rec.employeeId, rec.shiftId)
          existing ?: rec.toEntity()
        }
      dao.upsertAttendanceRecords(deduped)
    }
    return toInsert.size
  }

  suspend fun evaluateAndApplyRoster(request: RosterAssignmentRequest): RosterApplyOutcome {
    return database.withTransaction {
      val employees = dao.getAllEmployees().map { it.toDomain() }
      val shifts = dao.getAllShifts().map { it.toDomain() }
      val duties = dao.getAllDutyTypes().map { it.toDomain() }
      val existingAssignments = dao.getAllRosterAssignments().map { it.toDomain() }

      val outcome =
        RosterConflictDetector.evaluateAndBuildAssignments(
          request = request,
          employees = employees,
          shifts = shifts,
          duties = duties,
          existingAssignments = existingAssignments,
        )

      if (!outcome.requiresConfirmation && outcome.assignmentsToUpsert.isNotEmpty()) {
        val entities =
          outcome.assignmentsToUpsert.map { item ->
            val current = dao.getRosterByKey(item.date, item.employeeId, item.shiftId)
            item.copy(id = current?.id ?: item.id).toEntity()
          }
        dao.upsertRosterAssignments(entities)
        syncAutoOffForDates(outcome.assignmentsToUpsert.map { it.date }.toSet(), employees)
      }
      outcome
    }
  }

  suspend fun evaluateAndApplyBatchRoster(
    request: BatchRosterAssignmentRequest
  ): RosterApplyOutcome {
    return database.withTransaction {
      val employees = dao.getAllEmployees().map { it.toDomain() }
      val shifts = dao.getAllShifts().map { it.toDomain() }
      val duties = dao.getAllDutyTypes().map { it.toDomain() }
      val existingAssignments = dao.getAllRosterAssignments().map { it.toDomain() }

      val outcome =
        RosterConflictDetector.evaluateAndBuildBatchAssignments(
          request = request,
          employees = employees,
          shifts = shifts,
          duties = duties,
          existingAssignments = existingAssignments,
        )

      if (!outcome.requiresConfirmation && outcome.assignmentsToUpsert.isNotEmpty()) {
        val entities =
          outcome.assignmentsToUpsert.map { item ->
            val current = dao.getRosterByKey(item.date, item.employeeId, item.shiftId)
            item.copy(id = current?.id ?: item.id).toEntity()
          }
        dao.upsertRosterAssignments(entities)
        syncAutoOffForDates(outcome.assignmentsToUpsert.map { it.date }.toSet(), employees)
      }
      outcome
    }
  }

  private suspend fun syncAutoOffForDates(dates: Set<String>, employees: List<Employee>) {
    for (date in dates) {
      val dayRoster = dao.getRosterAssignmentsByDate(date).map { it.toDomain() }
      val dayAttendance = dao.getAttendanceByDate(date).map { it.toDomain() }
      val sync =
        AttendanceCalculator.buildAutoOffSyncForDate(
          date = date,
          employees = employees,
          assignmentsForDate = dayRoster,
          existingAttendanceForDate = dayAttendance,
        )
      if (sync.staleAutoOffIdsToDelete.isNotEmpty()) {
        dao.deleteAttendanceByIds(sync.staleAutoOffIdsToDelete)
      }
      if (sync.recordsToUpsert.isNotEmpty()) {
        dao.upsertAttendanceRecords(sync.recordsToUpsert.map { it.toEntity() })
      }
    }
  }

  suspend fun removeRosterAssignment(assignmentId: String) {
    dao.deleteRosterAssignmentById(assignmentId)
  }

  suspend fun exportBackupJson(recordTimestamp: Boolean = true): Pair<BackupBundle, String> {
    val now = System.currentTimeMillis()
    val bundle =
      database.withTransaction {
        val employees = dao.getAllEmployees().map { it.toDomain() }
        val shifts = dao.getAllShifts().map { it.toDomain() }
        val departments = dao.getAllDepartments().map { it.toDomain() }
        val duties = dao.getAllDutyTypes().map { it.toDomain() }
        val roster = dao.getAllRosterAssignments().map { it.toDomain() }
        val attendance = dao.getAllAttendanceRecords().map { it.toDomain() }
        val officeIndexEntries = dao.getAllOfficeIndex().map { it.toDomain() }
        val registeredOffices = dao.getAllRegisteredOffices().map { it.toDomain() }
        val officeCaseRecords = dao.getAllOfficeCaseRecords().map { it.toDomain() }
        val meta = (dao.getAppMeta()?.toDomain() ?: AppMeta()).let {
          if (recordTimestamp) it.copy(lastBackupAt = now) else it
        }
        if (recordTimestamp) {
          dao.upsertAppMeta(meta.toEntity())
        }
        BackupManager.createBackupBundle(
          employees = employees,
          shifts = shifts,
          dutyTypes = duties,
          rosterAssignments = roster,
          attendanceRecords = attendance,
          appMeta = meta,
          officeIndex = officeIndexEntries,
          departments = departments,
          registeredOffices = registeredOffices,
          officeCaseRecords = officeCaseRecords,
          exportedAt = now,
        )
      }
    return bundle to BackupManager.serializeBundle(bundle)
  }

  fun validateBackupJson(rawJson: String): BackupValidationResult {
    return BackupManager.validateAndParseBackup(rawJson)
  }

  suspend fun saveManualSafetySnapshot(reason: String = "Manual safety snapshot"): SafetySnapshotInfo {
    val (_, rawJson) = exportBackupJson(recordTimestamp = false)
    val bundle = BackupManager.validateAndParseBackup(rawJson).bundle!!
    val now = System.currentTimeMillis()
    val summary =
      "${bundle.recordCounts.employees} employees · ${bundle.recordCounts.rosterAssignments} roster · ${bundle.recordCounts.attendanceRecords} attendance"
    val snapshot =
      SafetySnapshotEntity(
        id = "snap_${now}_${UUID.randomUUID().toString().take(4)}",
        createdAt = now,
        reason = reason,
        summary = summary,
        rawJson = rawJson,
      )
    dao.upsertSafetySnapshot(snapshot)
    return snapshot.toDomain()
  }

  suspend fun restoreFromBackupJson(rawJson: String): RestoreExecutionResult {
    val validation = BackupManager.validateAndParseBackup(rawJson)
    if (!validation.isValid || validation.bundle == null) {
      return RestoreExecutionResult(
        success = false,
        message = validation.errors.firstOrNull() ?: "Backup validation failed.",
        safetySnapshotId = null,
        restoredCounts = null,
      )
    }

    val snapshot =
      try {
        saveManualSafetySnapshot(reason = "Pre-restore automatic snapshot")
      } catch (ex: Exception) {
        return RestoreExecutionResult(
          success = false,
          message = "Aborted: could not create pre-restore safety snapshot (${ex.localizedMessage}).",
          safetySnapshotId = null,
          restoredCounts = null,
        )
      }

    val bundle = validation.bundle
    return try {
      database.withTransaction {
        dao.clearAttendanceRecords()
        dao.clearRosterAssignments()
        dao.clearDutyTypes()
        dao.clearDepartments()
        dao.clearShifts()
        dao.clearEmployees()
        dao.clearAppMeta()
        dao.clearOfficeIndex()
        dao.clearOfficeCaseRecords()
        dao.clearRegisteredOffices()

        dao.upsertShifts(bundle.payload.shifts.map { it.toEntity() })
        dao.upsertDepartments(bundle.payload.departments.map { it.toEntity() })
        dao.upsertDutyTypes(bundle.payload.dutyTypes.map { it.toEntity() })
        dao.upsertEmployees(bundle.payload.employees.map { it.toEntity() })
        dao.upsertRosterAssignments(bundle.payload.rosterAssignments.map { it.toEntity() })
        dao.upsertAttendanceRecords(bundle.payload.attendanceRecords.map { it.toEntity() })
        dao.upsertAppMeta(bundle.payload.appMeta.toEntity())
        dao.upsertOfficeIndex(bundle.payload.officeIndex.map { it.toEntity() })
        dao.upsertRegisteredOffices(bundle.payload.registeredOffices.map { it.toEntity() })
        dao.upsertOfficeCaseRecords(bundle.payload.officeCaseRecords.map { it.toEntity() })
      }
      RestoreExecutionResult(
        success = true,
        message =
          "Restored ${bundle.recordCounts.employees} employees and ${bundle.recordCounts.attendanceRecords} attendance records cleanly.",
        safetySnapshotId = snapshot.id,
        restoredCounts = bundle.recordCounts,
      )
    } catch (ex: Exception) {
      RestoreExecutionResult(
        success = false,
        message =
          "Restore failed inside atomic transaction and rolled back safely: ${ex.localizedMessage ?: "Unknown error"}",
        safetySnapshotId = snapshot.id,
        restoredCounts = null,
      )
    }
  }

  suspend fun restoreFromSafetySnapshot(snapshotId: String): RestoreExecutionResult {
    val entity = dao.getSafetySnapshotById(snapshotId)
      ?: return RestoreExecutionResult(
        success = false,
        message = "Safety snapshot not found.",
        safetySnapshotId = null,
        restoredCounts = null,
      )
    return restoreFromBackupJson(entity.rawJson)
  }

  suspend fun previewOfflineUpdate(rawJson: String): OfflineUpdatePreview {
    val shifts = dao.getAllShifts().map { it.toDomain() }
    val duties = dao.getAllDutyTypes().map { it.toDomain() }
    return OfflineUpdateEngine.previewUpdatePack(
      rawJson = rawJson,
      existingShifts = shifts,
      existingDuties = duties,
    )
  }

  suspend fun applyOfflineUpdate(pack: OfflineUpdatePack): OfflineUpdateApplyResult {
    return try {
      database.withTransaction {
        val existingShifts = dao.getAllShifts().associateBy { it.id }
        val existingDuties = dao.getAllDutyTypes().associateBy { it.id }
        val existingEmployeesCount = dao.getAllEmployees().size
        val existingAttendanceCount = dao.getAllAttendanceRecords().size

        var added = 0
        var updated = 0
        for (shift in pack.payload.shiftsToUpsert) {
          if (existingShifts.containsKey(shift.id)) updated++ else added++
        }
        for (duty in pack.payload.dutyTypesToUpsert) {
          if (existingDuties.containsKey(duty.id)) updated++ else added++
        }

        if (pack.payload.shiftsToUpsert.isNotEmpty()) {
          dao.upsertShifts(pack.payload.shiftsToUpsert.map { it.toEntity() })
        }
        if (pack.payload.dutyTypesToUpsert.isNotEmpty()) {
          dao.upsertDutyTypes(pack.payload.dutyTypesToUpsert.map { it.toEntity() })
        }

        val currentMeta = dao.getAppMeta()?.toDomain() ?: AppMeta()
        val updatedMeta = OfflineUpdateEngine.buildUpdatedAppMeta(currentMeta, pack)
        dao.upsertAppMeta(updatedMeta.toEntity())

        OfflineUpdateApplyResult(
          success = true,
          message =
            "Applied pack ${pack.packVersion} ($added new, $updated updated). All $existingEmployeesCount employees & $existingAttendanceCount attendance records preserved.",
          addedCount = added,
          updatedCount = updated,
          preservedAttendanceCount = existingAttendanceCount,
          preservedEmployeeCount = existingEmployeesCount,
        )
      }
    } catch (ex: Exception) {
      OfflineUpdateApplyResult(
        success = false,
        message = "Offline update rolled back: ${ex.localizedMessage ?: "Write error"}",
        addedCount = 0,
        updatedCount = 0,
        preservedAttendanceCount = 0,
        preservedEmployeeCount = 0,
      )
    }
  }
}
