package com.example.models

data class IctCategory(
  val id: String,
  val name: String,
  val description: String = "",
  val createdAt: Long = 0L,
  val updatedAt: Long = 0L,
)

data class IctSubCategory(
  val id: String,
  val categoryId: String,
  val name: String,
  val unitLabel: String = "",
  val createdAt: Long = 0L,
  val updatedAt: Long = 0L,
)

data class IctEntry(
  val id: String,
  val categoryId: String,
  val subCategoryId: String?,
  val valueText: String,
  val numericValue: Double?,
  val note: String = "",
  val categoryNameSnapshot: String = "",
  val subCategoryNameSnapshot: String = "",
  val recordedAt: Long = 0L,
  val updatedAt: Long = 0L,
)

data class IctCategoryGroup(
  val category: IctCategory,
  val subCategories: List<IctSubCategory>,
)
