package com.example.logic

import com.example.models.CaseSectionAttachment
import com.example.models.IoAssignmentHistoryEntry
import com.example.models.OfficeCaseRecord
import com.example.models.OfficeCaseSearchQuery
import com.example.models.RegisteredOffice

object OfficeCaseEngine {

  fun buildInitialIoHistory(
    initialIoName: String,
    recordDate: String,
  ): List<IoAssignmentHistoryEntry> {
    val cleanName = initialIoName.trim()
    if (cleanName.isBlank()) return emptyList()
    return listOf(
      IoAssignmentHistoryEntry(
        ioName = cleanName,
        assignedDate = recordDate,
        remarks = "Initial I/O",
      )
    )
  }

  fun applyEditWithOptionalIoReassignment(
    existing: OfficeCaseRecord,
    office: RegisteredOffice?,
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
    nowMillis: Long = System.currentTimeMillis(),
  ): OfficeCaseRecord {
    val baseHistory =
      if (existing.ioHistory.isNotEmpty()) existing.ioHistory.toMutableList()
      else buildInitialIoHistory(existing.currentIoName, existing.recordDate).toMutableList()

    val cleanEditedIo = editedCurrentIoName.trim().ifBlank { existing.currentIoName }
    val cleanReassignedIo = newAssignedIoName?.trim().orEmpty()
    val effectiveAssignDate =
      newIoAssignedDate?.takeIf { DateUtils.isValidIsoDate(it) } ?: DateUtils.todayIso(nowMillis)

    val finalCurrentIo: String
    if (cleanReassignedIo.isNotBlank()) {
      // Ensure previous active I/O is preserved in history before appending the reassigned I/O
      val lastHistoricalName = baseHistory.lastOrNull()?.ioName
      if (lastHistoricalName == null || !lastHistoricalName.equals(cleanEditedIo, ignoreCase = true)) {
        if (cleanEditedIo.isNotBlank()) {
          baseHistory.add(
            IoAssignmentHistoryEntry(
              ioName = cleanEditedIo,
              assignedDate = existing.recordDate,
              remarks = "Previous I/O",
            )
          )
        }
      }
      baseHistory.add(
        IoAssignmentHistoryEntry(
          ioName = cleanReassignedIo,
          assignedDate = effectiveAssignDate,
          remarks = newIoRemarks.trim().ifBlank { "Reassigned I/O" },
        )
      )
      finalCurrentIo = cleanReassignedIo
    } else {
      finalCurrentIo = cleanEditedIo
      if (baseHistory.isEmpty() && cleanEditedIo.isNotBlank()) {
        baseHistory.add(
          IoAssignmentHistoryEntry(
            ioName = cleanEditedIo,
            assignedDate = recordDate,
            remarks = "Initial I/O",
          )
        )
      } else if (baseHistory.isNotEmpty()) {
        val last = baseHistory.last()
        if (!last.ioName.equals(cleanEditedIo, ignoreCase = true)) {
          baseHistory[baseHistory.lastIndex] = last.copy(ioName = cleanEditedIo)
        }
      }
    }

    return existing.copy(
      officeId = office?.id ?: existing.officeId,
      officeNameSnapshot = office?.officeName ?: existing.officeNameSnapshot,
      officeShortCodeSnapshot = office?.shortCode ?: existing.officeShortCodeSnapshot,
      recordDate = recordDate,
      firNumber = firNumber,
      currentIoName = finalCurrentIo,
      actType = actType,
      customActName = customActName.trim(),
      sections = sections,
      ioHistory = baseHistory,
      notes = notes.trim(),
      updatedAt = nowMillis,
    )
  }

  fun collectKnownIoNames(records: List<OfficeCaseRecord>): List<String> {
    val seen = LinkedHashMap<String, String>()
    records
      .sortedByDescending { it.updatedAt }
      .forEach { rec ->
        rec.currentIoName.trim().takeIf { it.isNotEmpty() }?.let { name ->
          seen.putIfAbsent(name.lowercase(), name)
        }
        rec.ioHistory.asReversed().forEach { h ->
          h.ioName.trim().takeIf { it.isNotEmpty() }?.let { name ->
            seen.putIfAbsent(name.lowercase(), name)
          }
        }
      }
    return seen.values.sortedBy { it.lowercase() }
  }

  fun resolveOfficeShortCode(
    record: OfficeCaseRecord,
    officesById: Map<String, RegisteredOffice>,
  ): String = officesById[record.officeId]?.shortCode ?: record.officeShortCodeSnapshot

  fun resolveOfficeName(
    record: OfficeCaseRecord,
    officesById: Map<String, RegisteredOffice>,
  ): String = officesById[record.officeId]?.officeName ?: record.officeNameSnapshot

  fun filterRecords(
    records: List<OfficeCaseRecord>,
    query: OfficeCaseSearchQuery,
    offices: List<RegisteredOffice>,
  ): List<OfficeCaseRecord> {
    val officesById = offices.associateBy { it.id }
    val (normStart, normEnd) = DateUtils.normalizeDateRange(query.startDate, query.endDate)
    val firNeedle = query.firNumberQuery.trim()
    val ioNeedle = query.ioNameQuery.trim().lowercase()
    val actNeedle = query.actFilter?.trim()?.takeIf { it.isNotBlank() }
    val secNeedle = query.sectionQuery.trim().lowercase()
    val textNeedle = query.searchText.trim().lowercase()

    return records
      .filter { rec ->
        if (normStart != null && rec.recordDate < normStart) return@filter false
        if (normEnd != null && rec.recordDate > normEnd) return@filter false
        if (query.officeId != null && rec.officeId != query.officeId) return@filter false

        if (firNeedle.isNotEmpty() && !rec.firNumber.toString().contains(firNeedle)) {
          return@filter false
        }

        if (ioNeedle.isNotEmpty()) {
          val matchesCurrentIo = rec.currentIoName.lowercase().contains(ioNeedle)
          val matchesPreviousIo = rec.ioHistory.any { it.ioName.lowercase().contains(ioNeedle) }
          if (!matchesCurrentIo && !matchesPreviousIo) return@filter false
        }

        if (actNeedle != null) {
          val matchesPrimaryAct = rec.actType.equals(actNeedle, ignoreCase = true)
          val matchesSectionAct = rec.sections.any { it.actCode.equals(actNeedle, ignoreCase = true) }
          val matchesCustomAct = rec.customActName.equals(actNeedle, ignoreCase = true)
          if (!matchesPrimaryAct && !matchesSectionAct && !matchesCustomAct) return@filter false
        }

        if (secNeedle.isNotEmpty()) {
          val matchesSection =
            rec.sections.any { sec ->
              sec.sectionNumber.lowercase().contains(secNeedle) ||
                sec.shortTitle.lowercase().contains(secNeedle) ||
                sec.displayLabel().lowercase().contains(secNeedle)
            }
          if (!matchesSection) return@filter false
        }

        if (textNeedle.isNotEmpty()) {
          val officeName = resolveOfficeName(rec, officesById)
          val shortCode = resolveOfficeShortCode(rec, officesById)
          val fields =
            listOf(
              rec.recordDate,
              rec.firNumber.toString(),
              "fir ${rec.firNumber}",
              officeName,
              shortCode,
              rec.currentIoName,
              rec.actType,
              rec.customActName,
              rec.notes,
            ) +
              rec.ioHistory.map { "${it.ioName} ${it.assignedDate} ${it.remarks}" } +
              rec.sections.map { it.displayLabel() }
          if (fields.none { it.lowercase().contains(textNeedle) }) return@filter false
        }

        true
      }
      .sortedWith(
        compareByDescending<OfficeCaseRecord> { it.recordDate }
          .thenByDescending { it.updatedAt }
          .thenByDescending { it.firNumber }
      )
  }
}
