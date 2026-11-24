package com.sek789102.jotwai.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.sek789102.jotwai.data.model.FolderEntity
import com.sek789102.jotwai.data.model.LabelEntity
import com.sek789102.jotwai.data.model.NoteEntity
import com.sek789102.jotwai.data.model.NoteLabelCrossRef
import com.sek789102.jotwai.data.model.NoteVersionEntity

@Database(
    entities = [
        NoteEntity::class,
        FolderEntity::class,
        LabelEntity::class,
        NoteLabelCrossRef::class,
        NoteVersionEntity::class
    ],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao

    companion object {
        const val DB_NAME = "jotwai.db"

        /** 1.2.0: folders get a colour. */
        val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE folders ADD COLUMN color INTEGER NOT NULL DEFAULT 0")
            }
        }

        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, DB_NAME)
                    // Every schema change ships a Migration; never wipe user notes.
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { instance = it }
            }
    }
}
