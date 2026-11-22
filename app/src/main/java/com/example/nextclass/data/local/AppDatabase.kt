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
    ],
    version = 4,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun routineDao(): RoutineDao

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
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .addCallback(FIRST_SEMESTER).build().also { instance = it }
            }
    }
}
