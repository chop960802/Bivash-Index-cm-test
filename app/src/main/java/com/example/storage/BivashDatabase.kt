package com.example.storage

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
  entities =
    [
      EmployeeEntity::class,
      ShiftEntity::class,
      DepartmentEntity::class,
      DutyTypeEntity::class,
      RosterAssignmentEntity::class,
      AttendanceRecordEntity::class,
      AppMetaEntity::class,
      SafetySnapshotEntity::class,
      OfficeIndexEntity::class,
      RegisteredOfficeEntity::class,
      OfficeCaseRecordEntity::class,
      IctCategoryEntity::class,
      IctSubCategoryEntity::class,
      IctEntryEntity::class,
      TaskCategoryEntity::class,
      TaskItemEntity::class,
      NoteEntity::class,
    ],
  version = 7,
  exportSchema = false,
)
abstract class BivashDatabase : RoomDatabase() {
  abstract fun dao(): BivashDao

  companion object {
    @Volatile private var instance: BivashDatabase? = null

    val MIGRATION_1_2 =
      object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
          db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `office_index` (
              `id` TEXT NOT NULL,
              `integerValue` INTEGER NOT NULL,
              `textValue` TEXT NOT NULL,
              `category` TEXT NOT NULL,
              `subCategory` TEXT NOT NULL,
              `notes` TEXT NOT NULL,
              `createdAt` INTEGER NOT NULL,
              `updatedAt` INTEGER NOT NULL,
              PRIMARY KEY(`id`)
            )
            """.trimIndent()
          )
        }
      }

    val MIGRATION_2_3 =
      object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
          db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `ict_categories` (
              `id` TEXT NOT NULL,
              `name` TEXT NOT NULL,
              `description` TEXT NOT NULL,
              `createdAt` INTEGER NOT NULL,
              `updatedAt` INTEGER NOT NULL,
              PRIMARY KEY(`id`)
            )
            """.trimIndent()
          )
          db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `ict_sub_categories` (
              `id` TEXT NOT NULL,
              `categoryId` TEXT NOT NULL,
              `name` TEXT NOT NULL,
              `unitLabel` TEXT NOT NULL,
              `createdAt` INTEGER NOT NULL,
              `updatedAt` INTEGER NOT NULL,
              PRIMARY KEY(`id`)
            )
            """.trimIndent()
          )
          db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_ict_sub_categories_categoryId` ON `ict_sub_categories` (`categoryId`)"
          )
          db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `ict_entries` (
              `id` TEXT NOT NULL,
              `categoryId` TEXT NOT NULL,
              `subCategoryId` TEXT,
              `valueText` TEXT NOT NULL,
              `numericValue` REAL,
              `note` TEXT NOT NULL,
              `categoryNameSnapshot` TEXT NOT NULL,
              `subCategoryNameSnapshot` TEXT NOT NULL,
              `recordedAt` INTEGER NOT NULL,
              `updatedAt` INTEGER NOT NULL,
              PRIMARY KEY(`id`)
            )
            """.trimIndent()
          )
          db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_ict_entries_categoryId` ON `ict_entries` (`categoryId`)"
          )
          db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_ict_entries_subCategoryId` ON `ict_entries` (`subCategoryId`)"
          )
        }
      }

    val MIGRATION_3_4 =
      object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
          db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `departments` (
              `id` TEXT NOT NULL,
              `name` TEXT NOT NULL,
              `description` TEXT NOT NULL,
              `isActive` INTEGER NOT NULL,
              `createdAt` INTEGER NOT NULL,
              `updatedAt` INTEGER NOT NULL,
              PRIMARY KEY(`id`)
            )
            """.trimIndent()
          )
          db.execSQL("ALTER TABLE `duty_types` ADD COLUMN `departmentId` TEXT NOT NULL DEFAULT ''")
          db.execSQL("ALTER TABLE `duty_types` ADD COLUMN `defaultShiftId` TEXT")
          db.execSQL("ALTER TABLE `roster_assignments` ADD COLUMN `departmentId` TEXT")
          db.execSQL("ALTER TABLE `attendance_records` ADD COLUMN `departmentId` TEXT")
          db.execSQL(
            "ALTER TABLE `attendance_records` ADD COLUMN `departmentNameSnapshot` TEXT NOT NULL DEFAULT ''"
          )
          val now = System.currentTimeMillis()
          db.execSQL(
            "INSERT OR IGNORE INTO `departments` (`id`, `name`, `description`, `isActive`, `createdAt`, `updatedAt`) VALUES ('dept_computer', 'Computer', 'Default department (editable & deletable)', 1, $now, $now)"
          )
        }
      }

    val MIGRATION_4_5 =
      object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
          db.execSQL("ALTER TABLE `employees` ADD COLUMN `defaultDepartmentId` TEXT")
        }
      }

    val MIGRATION_5_6 =
      object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
          db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `shifts_new` (
              `id` TEXT NOT NULL,
              `name` TEXT NOT NULL,
              `startTime` TEXT NOT NULL,
              `endTime` TEXT NOT NULL,
              `isNightShift` INTEGER NOT NULL,
              `isActive` INTEGER NOT NULL,
              `createdAt` INTEGER NOT NULL,
              `updatedAt` INTEGER NOT NULL,
              PRIMARY KEY(`id`)
            )
            """.trimIndent()
          )
          db.execSQL(
            """
            INSERT INTO `shifts_new` (`id`, `name`, `startTime`, `endTime`, `isNightShift`, `isActive`, `createdAt`, `updatedAt`)
            SELECT `id`, `name`, `startTime`, `endTime`, `isNightShift`, `isActive`, `createdAt`, `updatedAt` FROM `shifts`
            """.trimIndent()
          )
          db.execSQL("DROP TABLE `shifts`")
          db.execSQL("ALTER TABLE `shifts_new` RENAME TO `shifts`")

          db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `task_categories` (
              `id` TEXT NOT NULL,
              `name` TEXT NOT NULL,
              `createdAt` INTEGER NOT NULL,
              `updatedAt` INTEGER NOT NULL,
              PRIMARY KEY(`id`)
            )
            """.trimIndent()
          )
          db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `task_items` (
              `id` TEXT NOT NULL,
              `categoryId` TEXT NOT NULL,
              `title` TEXT NOT NULL,
              `isCompleted` INTEGER NOT NULL,
              `createdAt` INTEGER NOT NULL,
              `updatedAt` INTEGER NOT NULL,
              PRIMARY KEY(`id`)
            )
            """.trimIndent()
          )
          db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_task_items_categoryId` ON `task_items` (`categoryId`)"
          )
          db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `notes` (
              `id` TEXT NOT NULL,
              `title` TEXT NOT NULL,
              `body` TEXT NOT NULL,
              `createdAt` INTEGER NOT NULL,
              `updatedAt` INTEGER NOT NULL,
              PRIMARY KEY(`id`)
            )
            """.trimIndent()
          )
        }
      }

    val MIGRATION_6_7 =
      object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
          db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `registered_offices` (
              `id` TEXT NOT NULL,
              `officeName` TEXT NOT NULL,
              `shortCode` TEXT NOT NULL,
              `isActive` INTEGER NOT NULL,
              `createdAt` INTEGER NOT NULL,
              `updatedAt` INTEGER NOT NULL,
              PRIMARY KEY(`id`)
            )
            """.trimIndent()
          )
          db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `office_case_records` (
              `id` TEXT NOT NULL,
              `officeId` TEXT NOT NULL,
              `officeNameSnapshot` TEXT NOT NULL,
              `officeShortCodeSnapshot` TEXT NOT NULL,
              `recordDate` TEXT NOT NULL,
              `firNumber` INTEGER NOT NULL,
              `currentIoName` TEXT NOT NULL,
              `actType` TEXT NOT NULL,
              `customActName` TEXT NOT NULL,
              `sectionsJson` TEXT NOT NULL,
              `ioHistoryJson` TEXT NOT NULL,
              `notes` TEXT NOT NULL,
              `createdAt` INTEGER NOT NULL,
              `updatedAt` INTEGER NOT NULL,
              PRIMARY KEY(`id`)
            )
            """.trimIndent()
          )
          db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_office_case_records_officeId` ON `office_case_records` (`officeId`)"
          )
          db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_office_case_records_recordDate` ON `office_case_records` (`recordDate`)"
          )
          db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_office_case_records_firNumber` ON `office_case_records` (`firNumber`)"
          )
        }
      }

    fun getInstance(context: Context): BivashDatabase {
      return instance
        ?: synchronized(this) {
          instance
            ?: Room.databaseBuilder(
                context.applicationContext,
                BivashDatabase::class.java,
                "bivash_index.db",
              )
              .addMigrations(
                MIGRATION_1_2,
                MIGRATION_2_3,
                MIGRATION_3_4,
                MIGRATION_4_5,
                MIGRATION_5_6,
                MIGRATION_6_7,
              )
              .build()
              .also { instance = it }
        }
    }
  }
}
