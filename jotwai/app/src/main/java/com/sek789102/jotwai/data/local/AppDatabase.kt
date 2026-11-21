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
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao

    companion object {
        const val DB_NAME = "jotwai.db"

        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, DB_NAME)
                    // Future schema changes must ship a Migration; never wipe user notes.
                    .build()
                    .also { instance = it }
            }
    }
}
