package com.example.storage

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface BivashDao {

  @Query("SELECT * FROM employees ORDER BY isActive DESC, code ASC, name ASC")
  fun observeEmployees(): Flow<List<EmployeeEntity>>

  @Query("SELECT * FROM shifts ORDER BY isActive DESC, startTime ASC, name ASC")
  fun observeShifts(): Flow<List<ShiftEntity>>

  @Query("SELECT * FROM departments ORDER BY isActive DESC, name ASC, createdAt ASC")
  fun observeDepartments(): Flow<List<DepartmentEntity>>

  @Query("SELECT * FROM duty_types ORDER BY isActive DESC, title ASC")
  fun observeDutyTypes(): Flow<List<DutyTypeEntity>>

  @Query("SELECT * FROM roster_assignments ORDER BY date ASC, employeeId ASC, shiftId ASC")
  fun observeRosterAssignments(): Flow<List<RosterAssignmentEntity>>

  @Query("SELECT * FROM attendance_records ORDER BY date DESC, employeeId ASC, shiftId ASC")
  fun observeAttendanceRecords(): Flow<List<AttendanceRecordEntity>>

  @Query("SELECT * FROM app_meta WHERE id = 'singleton'")
  fun observeAppMeta(): Flow<AppMetaEntity?>

  @Query("SELECT * FROM safety_snapshots ORDER BY createdAt DESC")
  fun observeSafetySnapshots(): Flow<List<SafetySnapshotEntity>>

  @Query("SELECT * FROM office_index ORDER BY updatedAt DESC, category ASC, textValue ASC")
  fun observeOfficeIndex(): Flow<List<OfficeIndexEntity>>

  @Query("SELECT * FROM registered_offices ORDER BY isActive DESC, officeName ASC, shortCode ASC")
  fun observeRegisteredOffices(): Flow<List<RegisteredOfficeEntity>>

  @Query("SELECT * FROM office_case_records ORDER BY recordDate DESC, updatedAt DESC, firNumber DESC")
  fun observeOfficeCaseRecords(): Flow<List<OfficeCaseRecordEntity>>

  @Query("SELECT * FROM ict_categories ORDER BY name ASC, createdAt ASC")
  fun observeIctCategories(): Flow<List<IctCategoryEntity>>

  @Query("SELECT * FROM ict_sub_categories ORDER BY name ASC, createdAt ASC")
  fun observeIctSubCategories(): Flow<List<IctSubCategoryEntity>>

  @Query("SELECT * FROM ict_entries ORDER BY recordedAt DESC, updatedAt DESC")
  fun observeIctEntries(): Flow<List<IctEntryEntity>>

  @Query("SELECT * FROM task_categories ORDER BY name ASC, createdAt ASC")
  fun observeTaskCategories(): Flow<List<TaskCategoryEntity>>

  @Query("SELECT * FROM task_items ORDER BY isCompleted ASC, createdAt ASC")
  fun observeTaskItems(): Flow<List<TaskItemEntity>>

  @Query("SELECT * FROM notes ORDER BY updatedAt DESC, createdAt DESC")
  fun observeNotes(): Flow<List<NoteEntity>>

  @Query("SELECT * FROM employees")
  suspend fun getAllEmployees(): List<EmployeeEntity>

  @Query("SELECT * FROM shifts")
  suspend fun getAllShifts(): List<ShiftEntity>

  @Query("SELECT * FROM departments")
  suspend fun getAllDepartments(): List<DepartmentEntity>

  @Query("SELECT * FROM duty_types")
  suspend fun getAllDutyTypes(): List<DutyTypeEntity>

  @Query("SELECT * FROM roster_assignments")
  suspend fun getAllRosterAssignments(): List<RosterAssignmentEntity>

  @Query("SELECT * FROM attendance_records")
  suspend fun getAllAttendanceRecords(): List<AttendanceRecordEntity>

  @Query("SELECT * FROM app_meta WHERE id = 'singleton'")
  suspend fun getAppMeta(): AppMetaEntity?

  @Query("SELECT * FROM safety_snapshots WHERE id = :id")
  suspend fun getSafetySnapshotById(id: String): SafetySnapshotEntity?

  @Query("SELECT * FROM office_index")
  suspend fun getAllOfficeIndex(): List<OfficeIndexEntity>

  @Query("SELECT * FROM registered_offices")
  suspend fun getAllRegisteredOffices(): List<RegisteredOfficeEntity>

  @Query("SELECT * FROM office_case_records")
  suspend fun getAllOfficeCaseRecords(): List<OfficeCaseRecordEntity>

  @Query("SELECT * FROM ict_categories")
  suspend fun getAllIctCategories(): List<IctCategoryEntity>

  @Query("SELECT * FROM ict_sub_categories")
  suspend fun getAllIctSubCategories(): List<IctSubCategoryEntity>

  @Query("SELECT * FROM ict_entries")
  suspend fun getAllIctEntries(): List<IctEntryEntity>

  @Query("SELECT * FROM task_categories")
  suspend fun getAllTaskCategories(): List<TaskCategoryEntity>

  @Query("SELECT * FROM task_items")
  suspend fun getAllTaskItems(): List<TaskItemEntity>

  @Query("SELECT * FROM notes")
  suspend fun getAllNotes(): List<NoteEntity>

  @Query("SELECT * FROM roster_assignments WHERE date = :date")
  suspend fun getRosterAssignmentsByDate(date: String): List<RosterAssignmentEntity>

  @Query("SELECT * FROM attendance_records WHERE date = :date")
  suspend fun getAttendanceByDate(date: String): List<AttendanceRecordEntity>

  @Query(
    "SELECT * FROM attendance_records WHERE date = :date AND employeeId = :employeeId AND shiftId = :shiftId LIMIT 1"
  )
  suspend fun getAttendanceByKey(
    date: String,
    employeeId: String,
    shiftId: String,
  ): AttendanceRecordEntity?

  @Query(
    "SELECT * FROM roster_assignments WHERE date = :date AND employeeId = :employeeId AND shiftId = :shiftId LIMIT 1"
  )
  suspend fun getRosterByKey(
    date: String,
    employeeId: String,
    shiftId: String,
  ): RosterAssignmentEntity?

  @Upsert
  suspend fun upsertEmployees(items: List<EmployeeEntity>)

  @Upsert
  suspend fun upsertEmployee(item: EmployeeEntity)

  @Upsert
  suspend fun upsertShifts(items: List<ShiftEntity>)

  @Upsert
  suspend fun upsertShift(item: ShiftEntity)

  @Upsert
  suspend fun upsertDepartments(items: List<DepartmentEntity>)

  @Upsert
  suspend fun upsertDepartment(item: DepartmentEntity)

  @Upsert
  suspend fun upsertDutyTypes(items: List<DutyTypeEntity>)

  @Upsert
  suspend fun upsertDutyType(item: DutyTypeEntity)

  @Upsert
  suspend fun upsertRosterAssignments(items: List<RosterAssignmentEntity>)

  @Upsert
  suspend fun upsertAttendanceRecords(items: List<AttendanceRecordEntity>)

  @Upsert
  suspend fun upsertAttendanceRecord(item: AttendanceRecordEntity)

  @Upsert
  suspend fun upsertAppMeta(meta: AppMetaEntity)

  @Upsert
  suspend fun upsertSafetySnapshot(snapshot: SafetySnapshotEntity)

  @Upsert
  suspend fun upsertOfficeIndex(items: List<OfficeIndexEntity>)

  @Upsert
  suspend fun upsertOfficeIndexEntry(item: OfficeIndexEntity)

  @Upsert
  suspend fun upsertRegisteredOffices(items: List<RegisteredOfficeEntity>)

  @Upsert
  suspend fun upsertRegisteredOffice(item: RegisteredOfficeEntity)

  @Upsert
  suspend fun upsertOfficeCaseRecords(items: List<OfficeCaseRecordEntity>)

  @Upsert
  suspend fun upsertOfficeCaseRecord(item: OfficeCaseRecordEntity)

  @Query(
    "UPDATE office_case_records SET officeNameSnapshot = :officeName, officeShortCodeSnapshot = :shortCode WHERE officeId = :officeId"
  )
  suspend fun updateOfficeCaseSnapshots(officeId: String, officeName: String, shortCode: String)

  @Query("DELETE FROM roster_assignments WHERE id = :id")
  suspend fun deleteRosterAssignmentById(id: String)

  @Query("DELETE FROM employees WHERE id = :employeeId")
  suspend fun deleteEmployeeById(employeeId: String)

  @Query("DELETE FROM roster_assignments WHERE employeeId = :employeeId AND date >= :fromDate")
  suspend fun deleteFutureRosterByEmployeeId(employeeId: String, fromDate: String)

  @Query("DELETE FROM shifts WHERE id = :shiftId")
  suspend fun deleteShiftById(shiftId: String)

  @Query("DELETE FROM roster_assignments WHERE shiftId = :shiftId AND date >= :fromDate")
  suspend fun deleteFutureRosterByShiftId(shiftId: String, fromDate: String)

  @Query("DELETE FROM departments WHERE id = :departmentId")
  suspend fun deleteDepartmentById(departmentId: String)

  @Query("DELETE FROM duty_types WHERE departmentId = :departmentId")
  suspend fun deleteDutiesByDepartmentId(departmentId: String)

  @Query("UPDATE roster_assignments SET departmentId = NULL WHERE departmentId = :departmentId")
  suspend fun clearRosterDepartment(departmentId: String)

  @Query("UPDATE employees SET defaultDepartmentId = NULL WHERE defaultDepartmentId = :departmentId")
  suspend fun clearEmployeeDefaultDepartment(departmentId: String)

  @Query("DELETE FROM duty_types WHERE id = :dutyId")
  suspend fun deleteDutyById(dutyId: String)

  @Query("UPDATE roster_assignments SET dutyTypeId = NULL WHERE dutyTypeId = :dutyId")
  suspend fun clearRosterDuty(dutyId: String)

  @Query("DELETE FROM employees")
  suspend fun clearEmployees()

  @Query("DELETE FROM shifts")
  suspend fun clearShifts()

  @Query("DELETE FROM departments")
  suspend fun clearDepartments()

  @Query("DELETE FROM duty_types")
  suspend fun clearDutyTypes()

  @Query("DELETE FROM roster_assignments")
  suspend fun clearRosterAssignments()

  @Query("DELETE FROM attendance_records")
  suspend fun clearAttendanceRecords()

  @Query("DELETE FROM app_meta")
  suspend fun clearAppMeta()

  @Query("DELETE FROM office_index WHERE id = :id")
  suspend fun deleteOfficeIndexById(id: String)

  @Query("DELETE FROM office_index")
  suspend fun clearOfficeIndex()

  @Query("DELETE FROM registered_offices WHERE id = :id")
  suspend fun deleteRegisteredOfficeById(id: String)

  @Query("DELETE FROM registered_offices")
  suspend fun clearRegisteredOffices()

  @Query("DELETE FROM office_case_records WHERE id = :id")
  suspend fun deleteOfficeCaseRecordById(id: String)

  @Query("DELETE FROM office_case_records")
  suspend fun clearOfficeCaseRecords()

  @Upsert
  suspend fun upsertIctCategory(item: IctCategoryEntity)

  @Upsert
  suspend fun upsertIctSubCategory(item: IctSubCategoryEntity)

  @Upsert
  suspend fun upsertIctEntry(item: IctEntryEntity)

  @Query("UPDATE ict_entries SET categoryNameSnapshot = :name WHERE categoryId = :categoryId")
  suspend fun updateIctEntryCategorySnapshot(categoryId: String, name: String)

  @Query("UPDATE ict_entries SET subCategoryNameSnapshot = :name WHERE subCategoryId = :subCategoryId")
  suspend fun updateIctEntrySubCategorySnapshot(subCategoryId: String, name: String)

  @Query("DELETE FROM ict_categories WHERE id = :categoryId")
  suspend fun deleteIctCategoryById(categoryId: String)

  @Query("DELETE FROM ict_sub_categories WHERE categoryId = :categoryId")
  suspend fun deleteIctSubCategoriesByCategoryId(categoryId: String)

  @Query("DELETE FROM ict_entries WHERE categoryId = :categoryId")
  suspend fun deleteIctEntriesByCategoryId(categoryId: String)

  @Query("DELETE FROM ict_sub_categories WHERE id = :subCategoryId")
  suspend fun deleteIctSubCategoryById(subCategoryId: String)

  @Query("UPDATE ict_entries SET subCategoryId = NULL WHERE subCategoryId = :subCategoryId")
  suspend fun clearIctEntrySubCategory(subCategoryId: String)

  @Query("DELETE FROM ict_entries WHERE id = :entryId")
  suspend fun deleteIctEntryById(entryId: String)

  @Query("DELETE FROM attendance_records WHERE id IN (:ids)")
  suspend fun deleteAttendanceByIds(ids: List<String>)

  @Upsert
  suspend fun upsertTaskCategories(items: List<TaskCategoryEntity>)

  @Upsert
  suspend fun upsertTaskCategory(item: TaskCategoryEntity)

  @Query("DELETE FROM task_categories WHERE id = :categoryId")
  suspend fun deleteTaskCategoryById(categoryId: String)

  @Upsert
  suspend fun upsertTaskItems(items: List<TaskItemEntity>)

  @Upsert
  suspend fun upsertTaskItem(item: TaskItemEntity)

  @Query("DELETE FROM task_items WHERE id = :itemId")
  suspend fun deleteTaskItemById(itemId: String)

  @Query("DELETE FROM task_items WHERE categoryId = :categoryId")
  suspend fun deleteTaskItemsByCategoryId(categoryId: String)

  @Upsert
  suspend fun upsertNote(item: NoteEntity)

  @Query("DELETE FROM notes WHERE id = :noteId")
  suspend fun deleteNoteById(noteId: String)
}
