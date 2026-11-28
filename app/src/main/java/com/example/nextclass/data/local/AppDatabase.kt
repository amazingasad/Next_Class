package com.example.nextclass.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ClassSessionEntity::class,
        ClassOverrideEntity::class,
        CourseEntity::class,
        AttendanceEntity::class,
        SemesterEntity::class,
        TaskEntity::class,
        NoteEntity::class,
        BreakEntity::class,
        StudyLogEntity::class,
    ],
    version = 6,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun routineDao(): RoutineDao

    abstract fun plannerDao(): PlannerDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `class_overrides` (`sessionId` INTEGER NOT NULL, " +
                        "`date` TEXT NOT NULL, `cancelled` INTEGER NOT NULL, `room` TEXT NOT NULL, " +
                        "PRIMARY KEY(`sessionId`, `date`))",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `courses` (`codeKey` TEXT NOT NULL, `code` TEXT NOT NULL, " +
                        "`title` TEXT NOT NULL, PRIMARY KEY(`codeKey`))",
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `attendance` (`courseKey` TEXT NOT NULL, `date` TEXT NOT NULL, " +
                        "`startTime` TEXT NOT NULL, `status` TEXT NOT NULL, " +
                        "PRIMARY KEY(`courseKey`, `date`, `startTime`))",
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `semesters` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`name` TEXT NOT NULL, `active` INTEGER NOT NULL)",
                )
                db.execSQL("INSERT INTO `semesters` (`id`, `name`, `active`) VALUES (1, 'Semester 1', 1)")
                db.execSQL("ALTER TABLE `class_sessions` ADD COLUMN `semesterId` INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE `courses` ADD COLUMN `semesterId` INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE `attendance` ADD COLUMN `semesterId` INTEGER NOT NULL DEFAULT 1")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `tasks` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`date` TEXT NOT NULL, `title` TEXT NOT NULL, `done` INTEGER NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL)",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `notes` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`title` TEXT NOT NULL, `body` TEXT NOT NULL, `pinned` INTEGER NOT NULL, " +
                        "`updatedAt` INTEGER NOT NULL)",
                )
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `breaks` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`name` TEXT NOT NULL, `startDate` TEXT NOT NULL, `endDate` TEXT NOT NULL)",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `study_log` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`courseKey` TEXT NOT NULL, `courseCode` TEXT NOT NULL, `date` TEXT NOT NULL, " +
                        "`minutes` INTEGER NOT NULL)",
                )
            }
        }

        private val FIRST_SEMESTER = object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                db.execSQL("INSERT INTO `semesters` (`id`, `name`, `active`) VALUES (1, 'Semester 1', 1)")
            }
        }

        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "nextclass.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                    .addCallback(FIRST_SEMESTER).build().also { instance = it }
            }
    }
}
