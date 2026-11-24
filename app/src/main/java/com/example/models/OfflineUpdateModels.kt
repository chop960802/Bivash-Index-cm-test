package com.example.models

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OfflineUpdatePayload(
  val shiftsToUpsert: List<Shift> = emptyList(),
  val dutyTypesToUpsert: List<DutyType> = emptyList(),
  val referenceRules: List<ReferenceRule> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class OfflineUpdatePack(
  val packId: String,
  val packVersion: String,
  val minSchemaVersion: Int,
  val maxSchemaVersion: Int,
  val releaseNotes: String,
  val checksum: String,
  val payload: OfflineUpdatePayload,
)

data class UpdatePreviewItem(
  val category: String,
  val identifier: String,
  val title: String,
  val detail: String,
  val action: UpdateItemAction,
)

data class OfflineUpdatePreview(
  val isCompatible: Boolean,
  val pack: OfflineUpdatePack?,
  val errors: List<String>,
  val warnings: List<String>,
  val items: List<UpdatePreviewItem>,
  val newShiftsCount: Int,
  val updatedShiftsCount: Int,
  val newDutiesCount: Int,
  val updatedDutiesCount: Int,
  val rulesCount: Int,
)

data class OfflineUpdateApplyResult(
  val success: Boolean,
  val message: String,
  val addedCount: Int,
  val updatedCount: Int,
  val preservedAttendanceCount: Int,
  val preservedEmployeeCount: Int,
)
