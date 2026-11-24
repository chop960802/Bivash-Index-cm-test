package com.example.logic

import com.example.models.AppModuleId
import com.example.models.ModuleDefinition

object ModuleRegistry {
  private val modules: List<ModuleDefinition> =
    listOf(
      ModuleDefinition(
        id = AppModuleId.ATTENDANCE_DUTY,
        title = "Attendance & Daily Duty Register",
        subtitle = "Active v1 Module",
        description =
          "Daily team attendance tracking, shift management, duty templates, and upcoming roster planning.",
        isEnabled = true,
        versionLabel = "v1.0",
        badgeText = "ACTIVE",
      ),
      ModuleDefinition(
        id = AppModuleId.INDEX,
        title = "Index Ledger",
        subtitle = "Reserved Extension Slot",
        description =
          "Structured personal and team reference index for structured items, tags, and offline lookups.",
        isEnabled = false,
        versionLabel = "v2.0 Planned",
        badgeText = "PLANNED",
      ),
      ModuleDefinition(
        id = AppModuleId.COUNTER,
        title = "Shift & Ops Counter",
        subtitle = "Reserved Extension Slot",
        description =
          "Tactile offline tally counters for batch counts, dispatch tallies, and shift handovers.",
        isEnabled = false,
        versionLabel = "v2.0 Planned",
        badgeText = "PLANNED",
      ),
    )

  fun getAllModules(): List<ModuleDefinition> = modules

  fun getEnabledModules(): List<ModuleDefinition> = modules.filter { it.isEnabled }

  fun isModuleEnabled(id: AppModuleId): Boolean =
    modules.firstOrNull { it.id == id }?.isEnabled == true
}
