package com.example.logic

import com.example.models.APP_VERSION_NAME
import com.example.models.AppMeta
import com.example.models.AttendanceRecord
import com.example.models.BackupBundle
import com.example.models.BackupPayload
import com.example.models.BackupRecordCounts
import com.example.models.BackupValidationResult
import com.example.models.CURRENT_SCHEMA_VERSION
import com.example.models.Department
import com.example.models.DutyType
import com.example.models.Employee
import com.example.models.OfficeCaseRecord
import com.example.models.OfficeIndexEntry
import com.example.models.RegisteredOffice
import com.example.models.RosterAssignment
import com.example.models.Shift
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class LegacyBackupPayloadV1(
  val employees: List<Employee>,
  val shifts: List<Shift>,
  val dutyTypes: List<DutyType>,
  val rosterAssignments: List<RosterAssignment>,
  val attendanceRecords: List<AttendanceRecord>,
  val appMeta: AppMeta,
)

@JsonClass(generateAdapter = true)
internal data class LegacyBackupPayloadV2(
  val employees: List<Employee>,
  val shifts: List<Shift>,
  val dutyTypes: List<DutyType>,
  val rosterAssignments: List<RosterAssignment>,
  val attendanceRecords: List<AttendanceRecord>,
  val appMeta: AppMeta,
  val officeIndex: List<OfficeIndexEntry> = emptyList(),
)

@JsonClass(generateAdapter = true)
internal data class LegacyBackupPayloadV3(
  val employees: List<Employee>,
  val shifts: List<Shift>,
  val dutyTypes: List<DutyType>,
  val rosterAssignments: List<RosterAssignment>,
  val attendanceRecords: List<AttendanceRecord>,
  val appMeta: AppMeta,
  val officeIndex: List<OfficeIndexEntry> = emptyList(),
  val departments: List<Department> = emptyList(),
)

object BackupManager {
  private val bundleAdapter by lazy {
    ChecksumUtil.moshi.adapter(BackupBundle::class.java).indent("  ")
  }

  fun createBackupBundle(
    employees: List<Employee>,
    shifts: List<Shift>,
    dutyTypes: List<DutyType>,
    rosterAssignments: List<RosterAssignment>,
    attendanceRecords: List<AttendanceRecord>,
    appMeta: AppMeta,
    officeIndex: List<OfficeIndexEntry> = emptyList(),
    departments: List<Department> = emptyList(),
    registeredOffices: List<RegisteredOffice> = emptyList(),
    officeCaseRecords: List<OfficeCaseRecord> = emptyList(),
    exportedAt: Long = System.currentTimeMillis(),
  ): BackupBundle {
    val sortedPayload =
      BackupPayload(
        employees = employees.sortedBy { it.id },
        shifts = shifts.sortedBy { it.id },
        dutyTypes = dutyTypes.sortedBy { it.id },
        rosterAssignments = rosterAssignments.sortedBy { it.id },
        attendanceRecords = attendanceRecords.sortedBy { it.id },
        appMeta = appMeta.copy(schemaVersion = CURRENT_SCHEMA_VERSION),
        officeIndex = officeIndex.sortedBy { it.id },
        departments = departments.sortedBy { it.id },
        registeredOffices = registeredOffices.sortedBy { it.id },
        officeCaseRecords = officeCaseRecords.sortedBy { it.id },
      )
    val counts =
      BackupRecordCounts(
        employees = sortedPayload.employees.size,
        shifts = sortedPayload.shifts.size,
        dutyTypes = sortedPayload.dutyTypes.size,
        rosterAssignments = sortedPayload.rosterAssignments.size,
        attendanceRecords = sortedPayload.attendanceRecords.size,
        officeIndex = sortedPayload.officeIndex.size,
        departments = sortedPayload.departments.size,
        registeredOffices = sortedPayload.registeredOffices.size,
        officeCaseRecords = sortedPayload.officeCaseRecords.size,
      )
    val checksum = ChecksumUtil.sha256Of(sortedPayload)
    return BackupBundle(
      schemaVersion = CURRENT_SCHEMA_VERSION,
      appVersion = APP_VERSION_NAME,
      exportedAt = exportedAt,
      recordCounts = counts,
      checksum = checksum,
      payload = sortedPayload,
    )
  }

  fun serializeBundle(bundle: BackupBundle): String = bundleAdapter.toJson(bundle)

  fun validateAndParseBackup(rawJson: String): BackupValidationResult {
    if (rawJson.isBlank()) {
      return BackupValidationResult(
        isValid = false,
        bundle = null,
        errors = listOf("Backup file is empty."),
        warnings = emptyList(),
      )
    }

    val bundle =
      try {
        bundleAdapter.fromJson(rawJson)
      } catch (ex: Exception) {
        return BackupValidationResult(
          isValid = false,
          bundle = null,
          errors = listOf("Malformed JSON backup file: ${ex.localizedMessage ?: "Parse error"}"),
          warnings = emptyList(),
        )
      }

    if (bundle == null) {
      return BackupValidationResult(
        isValid = false,
        bundle = null,
        errors = listOf("Unable to decode backup bundle."),
        warnings = emptyList(),
      )
    }

    val errors = mutableListOf<String>()
    val warnings = mutableListOf<String>()

    if (bundle.schemaVersion != CURRENT_SCHEMA_VERSION) {
      errors.add(
        "Unsupported schema_version=${bundle.schemaVersion}. Expected schema_version=$CURRENT_SCHEMA_VERSION."
      )
    }

    val payload = bundle.payload
    val expectedCounts =
      BackupRecordCounts(
        employees = payload.employees.size,
        shifts = payload.shifts.size,
        dutyTypes = payload.dutyTypes.size,
        rosterAssignments = payload.rosterAssignments.size,
        attendanceRecords = payload.attendanceRecords.size,
        officeIndex = payload.officeIndex.size,
        departments = payload.departments.size,
        registeredOffices = payload.registeredOffices.size,
        officeCaseRecords = payload.officeCaseRecords.size,
      )
    if (bundle.recordCounts != expectedCounts) {
      errors.add("Record counts header does not match payload contents.")
    }

    val normalizedPayload =
      payload.copy(
        employees = payload.employees.sortedBy { it.id },
        shifts = payload.shifts.sortedBy { it.id },
        dutyTypes = payload.dutyTypes.sortedBy { it.id },
        rosterAssignments = payload.rosterAssignments.sortedBy { it.id },
        attendanceRecords = payload.attendanceRecords.sortedBy { it.id },
        officeIndex = payload.officeIndex.sortedBy { it.id },
        departments = payload.departments.sortedBy { it.id },
        registeredOffices = payload.registeredOffices.sortedBy { it.id },
        officeCaseRecords = payload.officeCaseRecords.sortedBy { it.id },
      )
    val computedChecksum = ChecksumUtil.sha256Of(normalizedPayload)
    val legacyChecksumMatches =
      when {
        !rawJson.contains("\"officeIndex\"") &&
          normalizedPayload.officeIndex.isEmpty() &&
          normalizedPayload.departments.isEmpty() &&
          normalizedPayload.registeredOffices.isEmpty() &&
          normalizedPayload.officeCaseRecords.isEmpty() -> {
          val legacyPayload =
            LegacyBackupPayloadV1(
              employees = normalizedPayload.employees,
              shifts = normalizedPayload.shifts,
              dutyTypes = normalizedPayload.dutyTypes,
              rosterAssignments = normalizedPayload.rosterAssignments,
              attendanceRecords = normalizedPayload.attendanceRecords,
              appMeta = normalizedPayload.appMeta,
            )
          bundle.checksum.equals(ChecksumUtil.sha256Of(legacyPayload), ignoreCase = true)
        }
        !rawJson.contains("\"departments\"") &&
          normalizedPayload.departments.isEmpty() &&
          normalizedPayload.registeredOffices.isEmpty() &&
          normalizedPayload.officeCaseRecords.isEmpty() -> {
          val legacyPayload =
            LegacyBackupPayloadV2(
              employees = normalizedPayload.employees,
              shifts = normalizedPayload.shifts,
              dutyTypes = normalizedPayload.dutyTypes,
              rosterAssignments = normalizedPayload.rosterAssignments,
              attendanceRecords = normalizedPayload.attendanceRecords,
              appMeta = normalizedPayload.appMeta,
              officeIndex = normalizedPayload.officeIndex,
            )
          bundle.checksum.equals(ChecksumUtil.sha256Of(legacyPayload), ignoreCase = true)
        }
        !rawJson.contains("\"registeredOffices\"") &&
          !rawJson.contains("\"officeCaseRecords\"") &&
          normalizedPayload.registeredOffices.isEmpty() &&
          normalizedPayload.officeCaseRecords.isEmpty() -> {
          val legacyPayload =
            LegacyBackupPayloadV3(
              employees = normalizedPayload.employees,
              shifts = normalizedPayload.shifts,
              dutyTypes = normalizedPayload.dutyTypes,
              rosterAssignments = normalizedPayload.rosterAssignments,
              attendanceRecords = normalizedPayload.attendanceRecords,
              appMeta = normalizedPayload.appMeta,
              officeIndex = normalizedPayload.officeIndex,
              departments = normalizedPayload.departments,
            )
          bundle.checksum.equals(ChecksumUtil.sha256Of(legacyPayload), ignoreCase = true)
        }
        else -> false
      }
    if (!bundle.checksum.equals(computedChecksum, ignoreCase = true) && !legacyChecksumMatches) {
      errors.add("Checksum verification failed. Backup file may be corrupted or modified.")
    }

    val attendanceKeys = mutableSetOf<String>()
    for (rec in payload.attendanceRecords) {
      if (!DateUtils.isValidIsoDate(rec.date)) {
        errors.add("Invalid attendance date format '${rec.date}' for employee ${rec.employeeId}.")
      }
      val key = "${rec.date}_${rec.employeeId}_${rec.shiftId}"
      if (!attendanceKeys.add(key)) {
        errors.add("Duplicate attendance record found for ($key).")
      }
    }

    val rosterKeys = mutableSetOf<String>()
    for (assign in payload.rosterAssignments) {
      if (!DateUtils.isValidIsoDate(assign.date)) {
        errors.add("Invalid roster date format '${assign.date}' for employee ${assign.employeeId}.")
      }
      val key = "${assign.date}_${assign.employeeId}_${assign.shiftId}"
      if (!rosterKeys.add(key)) {
        errors.add("Duplicate roster assignment found for ($key).")
      }
    }

    for (caseRec in payload.officeCaseRecords) {
      if (!DateUtils.isValidIsoDate(caseRec.recordDate)) {
        errors.add("Invalid Office Index record date '${caseRec.recordDate}' for FIR #${caseRec.firNumber}.")
      }
      if (caseRec.firNumber <= 0) {
        errors.add("Invalid FIR number '${caseRec.firNumber}' in Office Index record '${caseRec.id}'.")
      }
    }

    val empIds = payload.employees.map { it.id }.toSet()
    val historicalWithSnapshotOrMissing =
      payload.attendanceRecords.count {
        it.employeeId !in empIds && it.employeeNameSnapshot.isBlank()
      }
    if (historicalWithSnapshotOrMissing > 0) {
      warnings.add(
        "$historicalWithSnapshotOrMissing attendance record(s) reference deleted employees without snapshot names."
      )
    }

    return BackupValidationResult(
      isValid = errors.isEmpty(),
      bundle = bundle,
      errors = errors,
      warnings = warnings,
    )
  }
}
