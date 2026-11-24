package com.example.storage

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.logic.ChecksumUtil
import com.example.models.AppMeta
import com.example.models.AttendanceRecord
import com.example.models.AttendanceStatus
import com.example.models.CURRENT_SCHEMA_VERSION
import com.example.models.CaseSectionAttachment
import com.example.models.Department
import com.example.models.DutyType
import com.example.models.Employee
import com.example.models.IctCategory
import com.example.models.IctEntry
import com.example.models.IctSubCategory
import com.example.models.IoAssignmentHistoryEntry
import com.example.models.NoteEntry
import com.example.models.OfficeCaseRecord
import com.example.models.OfficeIndexEntry
import com.example.models.RegisteredOffice
import com.example.models.RosterAssignment
import com.example.models.SafetySnapshotInfo
import com.example.models.Shift
import com.example.models.TaskCategory
import com.example.models.TaskItem
import com.squareup.moshi.Types

@Entity(tableName = "employees")
data class EmployeeEntity(
  @PrimaryKey val id: String,
  val code: String,
  val name: String,
  val designation: String,
  val isActive: Boolean,
  val createdAt: Long,
  val updatedAt: Long,
  val defaultDepartmentId: String? = null,
)

@Entity(tableName = "shifts")
data class ShiftEntity(
  @PrimaryKey val id: String,
  val name: String,
  val startTime: String,
  val endTime: String,
  val isNightShift: Boolean,
  val isActive: Boolean,
  val createdAt: Long,
  val updatedAt: Long,
)

@Entity(tableName = "departments")
data class DepartmentEntity(
  @PrimaryKey val id: String,
  val name: String,
  val description: String,
  val isActive: Boolean,
  val createdAt: Long,
  val updatedAt: Long,
)

@Entity(tableName = "duty_types")
data class DutyTypeEntity(
  @PrimaryKey val id: String,
  val title: String,
  val description: String,
  val isActive: Boolean,
  val createdAt: Long,
  val updatedAt: Long,
  val departmentId: String = "",
  val defaultShiftId: String? = null,
)

@Entity(
  tableName = "roster_assignments",
  indices = [Index(value = ["date", "employeeId", "shiftId"], unique = true)],
)
data class RosterAssignmentEntity(
  @PrimaryKey val id: String,
  val date: String,
  val employeeId: String,
  val shiftId: String,
  val dutyTypeId: String?,
  val notes: String,
  val createdAt: Long,
  val updatedAt: Long,
  val departmentId: String? = null,
)

@Entity(
  tableName = "attendance_records",
  indices = [Index(value = ["date", "employeeId", "shiftId"], unique = true)],
)
data class AttendanceRecordEntity(
  @PrimaryKey val id: String,
  val date: String,
  val employeeId: String,
  val shiftId: String,
  val dutyTypeId: String?,
  val statusName: String,
  val remarks: String,
  val employeeCodeSnapshot: String,
  val employeeNameSnapshot: String,
  val designationSnapshot: String,
  val shiftNameSnapshot: String,
  val isNightShiftSnapshot: Boolean,
  val dutyTitleSnapshot: String,
  val markedAt: Long,
  val updatedAt: Long,
  val departmentId: String? = null,
  val departmentNameSnapshot: String = "",
)

@Entity(tableName = "app_meta")
data class AppMetaEntity(
  @PrimaryKey val id: String = "singleton",
  val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
  val appVersion: String,
  val lastBackupAt: Long?,
  val lastUpdatePackId: String?,
  val lastUpdatePackVersion: String?,
  val referenceRulesJson: String,
  val seededAt: Long,
)

@Entity(tableName = "safety_snapshots")
data class SafetySnapshotEntity(
  @PrimaryKey val id: String,
  val createdAt: Long,
  val reason: String,
  val summary: String,
  val rawJson: String,
)

fun EmployeeEntity.toDomain() =
  Employee(id, code, name, designation, isActive, createdAt, updatedAt, defaultDepartmentId)

fun Employee.toEntity() =
  EmployeeEntity(id, code, name, designation, isActive, createdAt, updatedAt, defaultDepartmentId)

fun ShiftEntity.toDomain() =
  Shift(id, name, startTime, endTime, isNightShift, isActive, createdAt, updatedAt)

fun Shift.toEntity() =
  ShiftEntity(id, name, startTime, endTime, isNightShift, isActive, createdAt, updatedAt)

fun DepartmentEntity.toDomain() =
  Department(id, name, description, isActive, createdAt, updatedAt)

fun Department.toEntity() =
  DepartmentEntity(id, name, description, isActive, createdAt, updatedAt)

fun DutyTypeEntity.toDomain() =
  DutyType(id, title, description, isActive, createdAt, updatedAt, departmentId, defaultShiftId)

fun DutyType.toEntity() =
  DutyTypeEntity(id, title, description, isActive, createdAt, updatedAt, departmentId, defaultShiftId)

fun RosterAssignmentEntity.toDomain() =
  RosterAssignment(id, date, employeeId, shiftId, dutyTypeId, notes, createdAt, updatedAt, departmentId)

fun RosterAssignment.toEntity() =
  RosterAssignmentEntity(id, date, employeeId, shiftId, dutyTypeId, notes, createdAt, updatedAt, departmentId)

fun AttendanceRecordEntity.toDomain(): AttendanceRecord {
  val parsedStatus =
    AttendanceStatus.entries.firstOrNull { it.name == statusName } ?: AttendanceStatus.PRESENT
  return AttendanceRecord(
    id = id,
    date = date,
    employeeId = employeeId,
    shiftId = shiftId,
    dutyTypeId = dutyTypeId,
    status = parsedStatus,
    remarks = remarks,
    employeeCodeSnapshot = employeeCodeSnapshot,
    employeeNameSnapshot = employeeNameSnapshot,
    designationSnapshot = designationSnapshot,
    shiftNameSnapshot = shiftNameSnapshot,
    isNightShiftSnapshot = isNightShiftSnapshot,
    dutyTitleSnapshot = dutyTitleSnapshot,
    markedAt = markedAt,
    updatedAt = updatedAt,
    departmentId = departmentId,
    departmentNameSnapshot = departmentNameSnapshot,
  )
}

fun AttendanceRecord.toEntity() =
  AttendanceRecordEntity(
    id = id,
    date = date,
    employeeId = employeeId,
    shiftId = shiftId,
    dutyTypeId = dutyTypeId,
    statusName = status.name,
    remarks = remarks,
    employeeCodeSnapshot = employeeCodeSnapshot,
    employeeNameSnapshot = employeeNameSnapshot,
    designationSnapshot = designationSnapshot,
    shiftNameSnapshot = shiftNameSnapshot,
    isNightShiftSnapshot = isNightShiftSnapshot,
    dutyTitleSnapshot = dutyTitleSnapshot,
    markedAt = markedAt,
    updatedAt = updatedAt,
    departmentId = departmentId,
    departmentNameSnapshot = departmentNameSnapshot,
  )

fun AppMetaEntity.toDomain() =
  AppMeta(
    id = id,
    schemaVersion = schemaVersion,
    appVersion = appVersion,
    lastBackupAt = lastBackupAt,
    lastUpdatePackId = lastUpdatePackId,
    lastUpdatePackVersion = lastUpdatePackVersion,
    referenceRulesJson = referenceRulesJson,
    seededAt = seededAt,
  )

fun AppMeta.toEntity() =
  AppMetaEntity(
    id = id,
    schemaVersion = schemaVersion,
    appVersion = appVersion,
    lastBackupAt = lastBackupAt,
    lastUpdatePackId = lastUpdatePackId,
    lastUpdatePackVersion = lastUpdatePackVersion,
    referenceRulesJson = referenceRulesJson,
    seededAt = seededAt,
  )

fun SafetySnapshotEntity.toDomain() =
  SafetySnapshotInfo(id, createdAt, reason, summary, rawJson)

@Entity(tableName = "office_index")
data class OfficeIndexEntity(
  @PrimaryKey val id: String,
  val integerValue: Int,
  val textValue: String,
  val category: String,
  val subCategory: String,
  val notes: String,
  val createdAt: Long,
  val updatedAt: Long,
)

fun OfficeIndexEntity.toDomain() =
  OfficeIndexEntry(
    id = id,
    integerValue = integerValue,
    textValue = textValue,
    category = category,
    subCategory = subCategory,
    notes = notes,
    createdAt = createdAt,
    updatedAt = updatedAt,
  )

fun OfficeIndexEntry.toEntity() =
  OfficeIndexEntity(
    id = id,
    integerValue = integerValue,
    textValue = textValue,
    category = category,
    subCategory = subCategory,
    notes = notes,
    createdAt = createdAt,
    updatedAt = updatedAt,
  )

@Entity(tableName = "registered_offices")
data class RegisteredOfficeEntity(
  @PrimaryKey val id: String,
  val officeName: String,
  val shortCode: String,
  val isActive: Boolean,
  val createdAt: Long,
  val updatedAt: Long,
)

@Entity(
  tableName = "office_case_records",
  indices = [Index(value = ["officeId"]), Index(value = ["recordDate"]), Index(value = ["firNumber"])],
)
data class OfficeCaseRecordEntity(
  @PrimaryKey val id: String,
  val officeId: String,
  val officeNameSnapshot: String,
  val officeShortCodeSnapshot: String,
  val recordDate: String,
  val firNumber: Int,
  val currentIoName: String,
  val actType: String,
  val customActName: String,
  val sectionsJson: String,
  val ioHistoryJson: String,
  val notes: String,
  val createdAt: Long,
  val updatedAt: Long,
)

private object OfficeCaseJsonHelper {
  private val sectionsAdapter by lazy {
    val type = Types.newParameterizedType(List::class.java, CaseSectionAttachment::class.java)
    ChecksumUtil.moshi.adapter<List<CaseSectionAttachment>>(type)
  }

  private val ioHistoryAdapter by lazy {
    val type = Types.newParameterizedType(List::class.java, IoAssignmentHistoryEntry::class.java)
    ChecksumUtil.moshi.adapter<List<IoAssignmentHistoryEntry>>(type)
  }

  fun encodeSections(items: List<CaseSectionAttachment>): String =
    sectionsAdapter.toJson(items)

  fun decodeSections(raw: String): List<CaseSectionAttachment> =
    try {
      if (raw.isBlank()) emptyList() else sectionsAdapter.fromJson(raw).orEmpty()
    } catch (_: Exception) {
      emptyList()
    }

  fun encodeIoHistory(items: List<IoAssignmentHistoryEntry>): String =
    ioHistoryAdapter.toJson(items)

  fun decodeIoHistory(raw: String): List<IoAssignmentHistoryEntry> =
    try {
      if (raw.isBlank()) emptyList() else ioHistoryAdapter.fromJson(raw).orEmpty()
    } catch (_: Exception) {
      emptyList()
    }
}

fun RegisteredOfficeEntity.toDomain() =
  RegisteredOffice(
    id = id,
    officeName = officeName,
    shortCode = shortCode,
    isActive = isActive,
    createdAt = createdAt,
    updatedAt = updatedAt,
  )

fun RegisteredOffice.toEntity() =
  RegisteredOfficeEntity(
    id = id,
    officeName = officeName,
    shortCode = shortCode,
    isActive = isActive,
    createdAt = createdAt,
    updatedAt = updatedAt,
  )

fun OfficeCaseRecordEntity.toDomain() =
  OfficeCaseRecord(
    id = id,
    officeId = officeId,
    officeNameSnapshot = officeNameSnapshot,
    officeShortCodeSnapshot = officeShortCodeSnapshot,
    recordDate = recordDate,
    firNumber = firNumber,
    currentIoName = currentIoName,
    actType = actType,
    customActName = customActName,
    sections = OfficeCaseJsonHelper.decodeSections(sectionsJson),
    ioHistory = OfficeCaseJsonHelper.decodeIoHistory(ioHistoryJson),
    notes = notes,
    createdAt = createdAt,
    updatedAt = updatedAt,
  )

fun OfficeCaseRecord.toEntity() =
  OfficeCaseRecordEntity(
    id = id,
    officeId = officeId,
    officeNameSnapshot = officeNameSnapshot,
    officeShortCodeSnapshot = officeShortCodeSnapshot,
    recordDate = recordDate,
    firNumber = firNumber,
    currentIoName = currentIoName,
    actType = actType,
    customActName = customActName,
    sectionsJson = OfficeCaseJsonHelper.encodeSections(sections),
    ioHistoryJson = OfficeCaseJsonHelper.encodeIoHistory(ioHistory),
    notes = notes,
    createdAt = createdAt,
    updatedAt = updatedAt,
  )

@Entity(tableName = "ict_categories")
data class IctCategoryEntity(
  @PrimaryKey val id: String,
  val name: String,
  val description: String,
  val createdAt: Long,
  val updatedAt: Long,
)

@Entity(
  tableName = "ict_sub_categories",
  indices = [Index(value = ["categoryId"])],
)
data class IctSubCategoryEntity(
  @PrimaryKey val id: String,
  val categoryId: String,
  val name: String,
  val unitLabel: String,
  val createdAt: Long,
  val updatedAt: Long,
)

@Entity(
  tableName = "ict_entries",
  indices = [Index(value = ["categoryId"]), Index(value = ["subCategoryId"])],
)
data class IctEntryEntity(
  @PrimaryKey val id: String,
  val categoryId: String,
  val subCategoryId: String?,
  val valueText: String,
  val numericValue: Double?,
  val note: String,
  val categoryNameSnapshot: String,
  val subCategoryNameSnapshot: String,
  val recordedAt: Long,
  val updatedAt: Long,
)

fun IctCategoryEntity.toDomain() =
  IctCategory(id, name, description, createdAt, updatedAt)

fun IctCategory.toEntity() =
  IctCategoryEntity(id, name, description, createdAt, updatedAt)

fun IctSubCategoryEntity.toDomain() =
  IctSubCategory(id, categoryId, name, unitLabel, createdAt, updatedAt)

fun IctSubCategory.toEntity() =
  IctSubCategoryEntity(id, categoryId, name, unitLabel, createdAt, updatedAt)

fun IctEntryEntity.toDomain() =
  IctEntry(
    id = id,
    categoryId = categoryId,
    subCategoryId = subCategoryId,
    valueText = valueText,
    numericValue = numericValue,
    note = note,
    categoryNameSnapshot = categoryNameSnapshot,
    subCategoryNameSnapshot = subCategoryNameSnapshot,
    recordedAt = recordedAt,
    updatedAt = updatedAt,
  )

fun IctEntry.toEntity() =
  IctEntryEntity(
    id = id,
    categoryId = categoryId,
    subCategoryId = subCategoryId,
    valueText = valueText,
    numericValue = numericValue,
    note = note,
    categoryNameSnapshot = categoryNameSnapshot,
    subCategoryNameSnapshot = subCategoryNameSnapshot,
    recordedAt = recordedAt,
    updatedAt = updatedAt,
  )

@Entity(tableName = "task_categories")
data class TaskCategoryEntity(
  @PrimaryKey val id: String,
  val name: String,
  val createdAt: Long,
  val updatedAt: Long,
)

@Entity(
  tableName = "task_items",
  indices = [Index(value = ["categoryId"])],
)
data class TaskItemEntity(
  @PrimaryKey val id: String,
  val categoryId: String,
  val title: String,
  val isCompleted: Boolean,
  val createdAt: Long,
  val updatedAt: Long,
)

@Entity(tableName = "notes")
data class NoteEntity(
  @PrimaryKey val id: String,
  val title: String,
  val body: String,
  val createdAt: Long,
  val updatedAt: Long,
)

fun TaskCategoryEntity.toDomain() = TaskCategory(id, name, createdAt, updatedAt)

fun TaskCategory.toEntity() = TaskCategoryEntity(id, name, createdAt, updatedAt)

fun TaskItemEntity.toDomain() = TaskItem(id, categoryId, title, isCompleted, createdAt, updatedAt)

fun TaskItem.toEntity() = TaskItemEntity(id, categoryId, title, isCompleted, createdAt, updatedAt)

fun NoteEntity.toDomain() = NoteEntry(id, title, body, createdAt, updatedAt)

fun NoteEntry.toEntity() = NoteEntity(id, title, body, createdAt, updatedAt)


