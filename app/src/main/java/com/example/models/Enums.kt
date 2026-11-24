package com.example.models

import com.squareup.moshi.JsonClass

enum class AttendanceStatus(val label: String, val shortCode: String) {
  PRESENT("Present", "P"),
  ABSENT("Absent", "A"),
  LATE("Late", "L"),
  LEAVE("Leave", "LV"),
  OFF("Off", "OFF"),
}

enum class AppModuleId {
  ATTENDANCE_DUTY,
  INDEX,
  COUNTER,
}

enum class RosterViewMode(val label: String) {
  BY_DATE("By Date"),
  BY_EMPLOYEE("By Employee"),
}

enum class UpdateItemAction(val label: String) {
  ADD("New"),
  UPDATE("Update"),
}

enum class AppThemeMode(val label: String) {
  SYSTEM("System"),
  DAY("Day"),
  NIGHT("Night"),
}

@JsonClass(generateAdapter = true)
data class ReferenceRule(
  val key: String,
  val title: String,
  val value: String,
)
