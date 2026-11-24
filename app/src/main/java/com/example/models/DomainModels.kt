package com.example.models

import com.squareup.moshi.JsonClass

const val CURRENT_SCHEMA_VERSION = 1
const val APP_VERSION_NAME = "1.0.0"
const val DEFAULT_DEPARTMENT_ID = "dept_computer"
const val DEFAULT_DEPARTMENT_NAME = "Computer"

@JsonClass(generateAdapter = true)
data class Department(
  val id: String,
  val name: String,
  val description: String = "",
  val isActive: Boolean = true,
  val createdAt: Long,
  val updatedAt: Long,
)

@JsonClass(generateAdapter = true)
data class Employee(
  val id: String,
  val code: String,
  val name: String,
  val designation: String,
  val isActive: Boolean,
  val createdAt: Long,
  val updatedAt: Long,
  val defaultDepartmentId: String? = null,
)

@JsonClass(generateAdapter = true)
data class Shift(
  val id: String,
  val name: String,
  val startTime: String,
  val endTime: String,
  val isNightShift: Boolean,
  val isActive: Boolean,
  val createdAt: Long,
  val updatedAt: Long,
) {
  fun isNightShiftVisual(): Boolean =
    isNightShift || name.contains("night", ignoreCase = true)
}

@JsonClass(generateAdapter = true)
data class DutyType(
  val id: String,
  val title: String,
  val description: String,
  val isActive: Boolean,
  val createdAt: Long,
  val updatedAt: Long,
  val departmentId: String = "",
  val defaultShiftId: String? = null,
)

@JsonClass(generateAdapter = true)
data class RosterAssignment(
  val id: String,
  val date: String,
  val employeeId: String,
  val shiftId: String,
  val dutyTypeId: String?,
  val notes: String,
  val createdAt: Long,
  val updatedAt: Long,
  val departmentId: String? = null,
)

@JsonClass(generateAdapter = true)
data class AttendanceRecord(
  val id: String,
  val date: String,
  val employeeId: String,
  val shiftId: String,
  val dutyTypeId: String?,
  val status: AttendanceStatus,
  val remarks: String,
  val employeeCodeSnapshot: String = "",
  val employeeNameSnapshot: String = "",
  val designationSnapshot: String = "",
  val shiftNameSnapshot: String = "",
  val isNightShiftSnapshot: Boolean = false,
  val dutyTitleSnapshot: String = "",
  val markedAt: Long,
  val updatedAt: Long,
  val departmentId: String? = null,
  val departmentNameSnapshot: String = "",
)

@JsonClass(generateAdapter = true)
data class AppMeta(
  val id: String = "singleton",
  val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
  val appVersion: String = APP_VERSION_NAME,
  val lastBackupAt: Long? = null,
  val lastUpdatePackId: String? = null,
  val lastUpdatePackVersion: String? = null,
  val referenceRulesJson: String = "[]",
  val seededAt: Long = 0L,
)

data class ModuleDefinition(
  val id: AppModuleId,
  val title: String,
  val subtitle: String,
  val description: String,
  val isEnabled: Boolean,
  val versionLabel: String,
  val badgeText: String,
)

data class SafetySnapshotInfo(
  val id: String,
  val createdAt: Long,
  val reason: String,
  val summary: String,
  val rawJson: String,
)
