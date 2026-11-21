package com.sek789102.formulabook.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.sek789102.formulabook.data.model.ExamCountdownEntity
import com.sek789102.formulabook.data.model.NoteEntity
import com.sek789102.formulabook.data.model.NoteTagCrossRef
import com.sek789102.formulabook.data.model.StudySessionEntity
import com.sek789102.formulabook.data.model.SubjectCategory
import com.sek789102.formulabook.data.model.SubjectEntity
import com.sek789102.formulabook.data.model.TagEntity

@Database(
    entities = [
        NoteEntity::class,
        TagEntity::class,
        NoteTagCrossRef::class,
        ExamCountdownEntity::class,
        StudySessionEntity::class,
        SubjectEntity::class
    ],
    version = 3,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `exam_countdowns` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `subject` TEXT NOT NULL,
                        `examDateMillis` INTEGER NOT NULL,
                        `note` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `study_sessions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `subject` TEXT NOT NULL,
                        `durationMinutes` INTEGER NOT NULL,
                        `completedAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `notes` ADD COLUMN `imagePaths` TEXT NOT NULL DEFAULT '[]'")
                db.execSQL("ALTER TABLE `notes` ADD COLUMN `deletedAt` INTEGER DEFAULT NULL")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `subjects` (
                        `id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `colorHex` INTEGER NOT NULL,
                        `isDefault` INTEGER NOT NULL,
                        `sortOrder` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                seedDefaultSubjects(db)
            }
        }

        fun seedDefaultSubjects(db: SupportSQLiteDatabase) {
            SubjectCategory.entries.forEach { cat ->
                val values = ContentValues().apply {
                    put("id", cat.name)
                    put("name", cat.defaultThaiName)
                    put("colorHex", cat.colorHex)
                    put("isDefault", 1)
                    put("sortOrder", cat.ordinal)
                    put("createdAt", 0L)
                }
                db.insert("subjects", SQLiteDatabase.CONFLICT_IGNORE, values)
            }
            db.execSQL(
                "UPDATE `subjects` SET `sortOrder` = ? WHERE `id` = ? AND `isDefault` = 1 AND `sortOrder` < ?",
                arrayOf<Any>(
                    SubjectCategory.GENERAL.ordinal,
                    SubjectCategory.GENERAL.name,
                    SubjectCategory.GENERAL.ordinal
                )
            )
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "student_formula_notes.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            seedDefaultSubjects(db)
                        }

                        override fun onOpen(db: SupportSQLiteDatabase) {
                            super.onOpen(db)
                            seedDefaultSubjects(db)
                        }
                    })
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
