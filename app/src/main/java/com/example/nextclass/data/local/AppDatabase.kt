package com.example.nextclass.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [ClassSessionEntity::class, ClassOverrideEntity::class, CourseEntity::class],
    version = 2,
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

        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "nextclass.db"
                ).addMigrations(MIGRATION_1_2).build().also { instance = it }
            }
    }
}
