package com.example.logic

import com.example.models.AppMeta
import com.example.models.CURRENT_SCHEMA_VERSION
import com.example.models.DutyType
import com.example.models.OfflineUpdatePack
import com.example.models.OfflineUpdatePayload
import com.example.models.OfflineUpdatePreview
import com.example.models.ReferenceRule
import com.example.models.Shift
import com.example.models.UpdateItemAction
import com.example.models.UpdatePreviewItem

object OfflineUpdateEngine {
  private val packAdapter by lazy {
    ChecksumUtil.moshi.adapter(OfflineUpdatePack::class.java).indent("  ")
  }
  private val rulesListType by lazy {
    com.squareup.moshi.Types.newParameterizedType(List::class.java, ReferenceRule::class.java)
  }
  private val rulesAdapter by lazy {
    ChecksumUtil.moshi.adapter<List<ReferenceRule>>(rulesListType)
  }

  fun previewUpdatePack(
    rawJson: String,
    existingShifts: List<Shift>,
    existingDuties: List<DutyType>,
  ): OfflineUpdatePreview {
    if (rawJson.isBlank()) {
      return invalidPreview("Update pack file is empty.")
    }

    val pack =
      try {
        packAdapter.fromJson(rawJson)
      } catch (ex: Exception) {
        return invalidPreview("Invalid update pack JSON: ${ex.localizedMessage ?: "Parse error"}")
      }

    if (pack == null) {
      return invalidPreview("Unable to decode offline update pack.")
    }

    val errors = mutableListOf<String>()
    val warnings = mutableListOf<String>()

    if (pack.packId.isBlank() || pack.packVersion.isBlank()) {
      errors.add("Update pack is missing packId or packVersion.")
    }
    if (
      CURRENT_SCHEMA_VERSION < pack.minSchemaVersion ||
        CURRENT_SCHEMA_VERSION > pack.maxSchemaVersion
    ) {
      errors.add(
        "Incompatible schema version. App uses v$CURRENT_SCHEMA_VERSION, pack supports v${pack.minSchemaVersion}..v${pack.maxSchemaVersion}."
      )
    }

    val computedChecksum = ChecksumUtil.sha256Of(pack.payload)
    if (!pack.checksum.equals(computedChecksum, ignoreCase = true)) {
      errors.add("Update pack checksum mismatch. Pack integrity check failed.")
    }

    val existingShiftMap = existingShifts.associateBy { it.id }
    val existingDutyMap = existingDuties.associateBy { it.id }
    val items = mutableListOf<UpdatePreviewItem>()

    var newShifts = 0
    var updatedShifts = 0
    var newDuties = 0
    var updatedDuties = 0

    for (shift in pack.payload.shiftsToUpsert) {
      if (shift.id.isBlank() || shift.name.isBlank()) {
        errors.add("Shift entry in update pack has empty id or name.")
        continue
      }
      if (!DateUtils.isValidTime24h(shift.startTime) || !DateUtils.isValidTime24h(shift.endTime)) {
        errors.add("Shift '${shift.name}' has invalid 24h time (${shift.startTime}-${shift.endTime}).")
      }
      val isExisting = existingShiftMap.containsKey(shift.id)
      if (isExisting) updatedShifts++ else newShifts++
      val nightLabel = if (shift.isNightShiftVisual()) " · Night Shift" else ""
      items.add(
        UpdatePreviewItem(
          category = "Shift",
          identifier = shift.id,
          title = shift.name,
          detail = "${shift.startTime}–${shift.endTime}$nightLabel",
          action = if (isExisting) UpdateItemAction.UPDATE else UpdateItemAction.ADD,
        )
      )
    }

    for (duty in pack.payload.dutyTypesToUpsert) {
      if (duty.id.isBlank() || duty.title.isBlank()) {
        errors.add("Duty template in update pack has empty id or title.")
        continue
      }
      val isExisting = existingDutyMap.containsKey(duty.id)
      if (isExisting) updatedDuties++ else newDuties++
      items.add(
        UpdatePreviewItem(
          category = "Duty Template",
          identifier = duty.id,
          title = duty.title,
          detail = duty.description,
          action = if (isExisting) UpdateItemAction.UPDATE else UpdateItemAction.ADD,
        )
      )
    }

    if (items.isEmpty() && pack.payload.referenceRules.isEmpty()) {
      warnings.add("Update pack contains no shift, duty, or rule changes.")
    }

    return OfflineUpdatePreview(
      isCompatible = errors.isEmpty(),
      pack = pack,
      errors = errors,
      warnings = warnings,
      items = items,
      newShiftsCount = newShifts,
      updatedShiftsCount = updatedShifts,
      newDutiesCount = newDuties,
      updatedDutiesCount = updatedDuties,
      rulesCount = pack.payload.referenceRules.size,
    )
  }

  fun buildUpdatedAppMeta(
    currentMeta: AppMeta,
    pack: OfflineUpdatePack,
  ): AppMeta {
    val existingRules =
      parseReferenceRules(currentMeta.referenceRulesJson).associateBy { it.key }.toMutableMap()
    for (rule in pack.payload.referenceRules) {
      existingRules[rule.key] = rule
    }
    val mergedRulesJson = rulesAdapter.toJson(existingRules.values.sortedBy { it.key })
    return currentMeta.copy(
      lastUpdatePackId = pack.packId,
      lastUpdatePackVersion = pack.packVersion,
      referenceRulesJson = mergedRulesJson,
    )
  }

  fun parseReferenceRules(json: String): List<ReferenceRule> {
    if (json.isBlank()) return emptyList()
    return try {
      rulesAdapter.fromJson(json).orEmpty()
    } catch (_: Exception) {
      emptyList()
    }
  }

  fun createSampleUpdatePackJson(nowMillis: Long = System.currentTimeMillis()): String {
    val payload =
      OfflineUpdatePayload(
        shiftsToUpsert =
          listOf(
            Shift(
              id = "shift_morning",
              name = "Morning Shift",
              startTime = "08:00",
              endTime = "16:00",
              isNightShift = false,
              isActive = true,
              createdAt = nowMillis,
              updatedAt = nowMillis,
            ),
            Shift(
              id = "shift_twilight",
              name = "Twilight Dispatch",
              startTime = "18:00",
              endTime = "22:00",
              isNightShift = false,
              isActive = true,
              createdAt = nowMillis,
              updatedAt = nowMillis,
            ),
          ),
        dutyTypesToUpsert =
          listOf(
            DutyType(
              id = "duty_gate",
              title = "Gate & Visitor Control",
              description = "Verify entry badges, delivery manifests, and shift handover log.",
              isActive = true,
              createdAt = nowMillis,
              updatedAt = nowMillis,
            ),
            DutyType(
              id = "duty_cold_audit",
              title = "Cold-Zone Audit",
              description = "Hourly temperature log and seal verification before dispatch.",
              isActive = true,
              createdAt = nowMillis,
              updatedAt = nowMillis,
            ),
          ),
        referenceRules =
          listOf(
            ReferenceRule(
              key = "shift_schedule_policy",
              title = "Updated Shift Windows",
              value = "Morning Shift (08:00–16:00) refreshed; Twilight Dispatch added (18:00–22:00).",
            ),
            ReferenceRule(
              key = "multi_shift_rule",
              title = "Multi-Shift Handover Check",
              value = "Staff working double shifts must log separate attendance entries per shift.",
            ),
          ),
      )

    val checksum = ChecksumUtil.sha256Of(payload)
    val pack =
      OfflineUpdatePack(
        packId = "bivash-ops-pack-1.1",
        packVersion = "1.1.0",
        minSchemaVersion = CURRENT_SCHEMA_VERSION,
        maxSchemaVersion = CURRENT_SCHEMA_VERSION,
        releaseNotes =
          "Adds Twilight Dispatch shift & Cold-Zone Audit duty template, and refreshes operational rules without altering employee or attendance history.",
        checksum = checksum,
        payload = payload,
      )
    return packAdapter.toJson(pack)
  }

  private fun invalidPreview(error: String): OfflineUpdatePreview =
    OfflineUpdatePreview(
      isCompatible = false,
      pack = null,
      errors = listOf(error),
      warnings = emptyList(),
      items = emptyList(),
      newShiftsCount = 0,
      updatedShiftsCount = 0,
      newDutiesCount = 0,
      updatedDutiesCount = 0,
      rulesCount = 0,
    )
}
