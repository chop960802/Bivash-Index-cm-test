package com.example.models

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OfficeIndexEntry(
  val id: String,
  val integerValue: Int,
  val textValue: String,
  val category: String,
  val subCategory: String,
  val notes: String = "",
  val createdAt: Long,
  val updatedAt: Long,
)

@JsonClass(generateAdapter = true)
data class RegisteredOffice(
  val id: String,
  val officeName: String,
  val shortCode: String,
  val isActive: Boolean = true,
  val createdAt: Long,
  val updatedAt: Long,
)

enum class LegalActType(val code: String, val displayName: String, val shortLabel: String) {
  BNS("BNS", "Bharatiya Nyaya Sanhita, 2023 (BNS)", "BNS 2023"),
  POCSO("POCSO", "Protection of Children from Sexual Offences Act (POCSO)", "POCSO Act"),
  OTHERS("OTHERS", "Others (Custom Act / Section)", "Others");

  companion object {
    fun fromCode(code: String): LegalActType =
      entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: OTHERS
  }
}

data class LegalSectionItem(
  val actCode: String,
  val sectionNumber: String,
  val shortTitle: String,
) {
  fun displayLabel(): String = "Sec $sectionNumber — $shortTitle"
}

@JsonClass(generateAdapter = true)
data class CaseSectionAttachment(
  val actCode: String,
  val actDisplayName: String,
  val sectionNumber: String,
  val shortTitle: String,
) {
  fun key(): String = "${actCode.uppercase()}:${sectionNumber.trim().uppercase()}"
  fun displayLabel(): String =
    if (sectionNumber.isBlank()) shortTitle
    else "${actDisplayName.ifBlank { actCode }} Sec $sectionNumber — $shortTitle"
}

@JsonClass(generateAdapter = true)
data class IoAssignmentHistoryEntry(
  val ioName: String,
  val assignedDate: String,
  val remarks: String = "",
)

@JsonClass(generateAdapter = true)
data class OfficeCaseRecord(
  val id: String,
  val officeId: String,
  val officeNameSnapshot: String,
  val officeShortCodeSnapshot: String,
  val recordDate: String,
  val firNumber: Int,
  val currentIoName: String,
  val actType: String,
  val customActName: String = "",
  val sections: List<CaseSectionAttachment> = emptyList(),
  val ioHistory: List<IoAssignmentHistoryEntry> = emptyList(),
  val notes: String = "",
  val createdAt: Long,
  val updatedAt: Long,
) {
  fun primaryActBadge(): String {
    val distinctActs = sections.map { it.actCode }.distinct()
    return when {
      distinctActs.size > 1 -> distinctActs.joinToString(" + ") {
        if (it == LegalActType.OTHERS.code && customActName.isNotBlank()) customActName else it
      }
      actType == LegalActType.OTHERS.code && customActName.isNotBlank() -> customActName
      distinctActs.size == 1 -> {
        val code = distinctActs.first()
        if (code == LegalActType.OTHERS.code && customActName.isNotBlank()) customActName else code
      }
      else -> actType
    }
  }
}

data class OfficeCaseSearchQuery(
  val startDate: String? = null,
  val endDate: String? = null,
  val officeId: String? = null,
  val firNumberQuery: String = "",
  val ioNameQuery: String = "",
  val actFilter: String? = null,
  val sectionQuery: String = "",
  val searchText: String = "",
) {
  val activeFilterCount: Int
    get() =
      listOfNotNull(
          startDate ?: endDate,
          officeId,
          firNumberQuery.trim().takeIf { it.isNotEmpty() },
          ioNameQuery.trim().takeIf { it.isNotEmpty() },
          actFilter,
          sectionQuery.trim().takeIf { it.isNotEmpty() },
          searchText.trim().takeIf { it.isNotEmpty() },
        )
        .size
}

data class OfficeIndexSummary(
  val totalEntries: Int = 0,
  val distinctCategories: Int = 0,
  val integerTotal: Int = 0,
  val recentEntries: List<OfficeIndexEntry> = emptyList(),
  val totalCaseRecords: Int = 0,
  val totalOffices: Int = 0,
)
