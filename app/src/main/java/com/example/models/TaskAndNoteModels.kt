package com.example.models

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TaskCategory(
  val id: String,
  val name: String,
  val createdAt: Long = 0L,
  val updatedAt: Long = 0L,
)

@JsonClass(generateAdapter = true)
data class TaskItem(
  val id: String,
  val categoryId: String,
  val title: String,
  val isCompleted: Boolean = false,
  val createdAt: Long = 0L,
  val updatedAt: Long = 0L,
)

@JsonClass(generateAdapter = true)
data class NoteEntry(
  val id: String,
  val title: String,
  val body: String,
  val createdAt: Long = 0L,
  val updatedAt: Long = 0L,
)

data class TaskCategoryGroup(
  val category: TaskCategory,
  val items: List<TaskItem>,
) {
  val completedCount: Int
    get() = items.count { it.isCompleted }
}

object TaskInputParser {
  fun parseEntries(rawInput: String, useCommaSeparator: Boolean): List<String> {
    val trimmed = rawInput.trim()
    if (trimmed.isEmpty()) return emptyList()
    if (!useCommaSeparator) return listOf(trimmed)
    return rawInput
      .split(',')
      .map { it.trim() }
      .filter { it.isNotEmpty() }
  }
}
