package com.example.models

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BackupPayload(
  val employees: List<Employee>,
  val shifts: List<Shift>,
  val dutyTypes: List<DutyType>,
  val rosterAssignments: List<RosterAssignment>,
  val attendanceRecords: List<AttendanceRecord>,
  val appMeta: AppMeta,
  val officeIndex: List<OfficeIndexEntry> = emptyList(),
  val departments: List<Department> = emptyList(),
  val registeredOffices: List<RegisteredOffice> = emptyList(),
  val officeCaseRecords: List<OfficeCaseRecord> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class BackupRecordCounts(
  val employees: Int,
  val shifts: Int,
  val dutyTypes: Int,
  val rosterAssignments: Int,
  val attendanceRecords: Int,
  val officeIndex: Int = 0,
  val departments: Int = 0,
  val registeredOffices: Int = 0,
  val officeCaseRecords: Int = 0,
)

@JsonClass(generateAdapter = true)
data class BackupBundle(
  val schemaVersion: Int,
  val appVersion: String,
  val exportedAt: Long,
  val recordCounts: BackupRecordCounts,
  val checksum: String,
  val payload: BackupPayload,
)

data class BackupValidationResult(
  val isValid: Boolean,
  val bundle: BackupBundle?,
  val errors: List<String>,
  val warnings: List<String>,
)

data class RestoreExecutionResult(
  val success: Boolean,
  val message: String,
  val safetySnapshotId: String?,
  val restoredCounts: BackupRecordCounts?,
)
