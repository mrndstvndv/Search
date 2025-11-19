package com.mrndstvndv.search.provider.files.index

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [IndexedDocumentEntity::class],
    version = 1,
    exportSchema = false
)
abstract class FileSearchDatabase : RoomDatabase() {
    abstract fun indexedDocumentDao(): IndexedDocumentDao

    companion object {
        @Volatile
        private var INSTANCE: FileSearchDatabase? = null

        fun get(context: Context): FileSearchDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    FileSearchDatabase::class.java,
                    "file_search_index.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
