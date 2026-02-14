package com.xaaav.mozukutsuchikey.clipboard

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ClipboardEntity::class], version = 1, exportSchema = false)
abstract class ClipboardDatabase : RoomDatabase() {
    abstract fun clipboardDao(): ClipboardDao

    companion object {
        @Volatile
        private var instance: ClipboardDatabase? = null

        fun getInstance(context: Context): ClipboardDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    ClipboardDatabase::class.java,
                    "clipboard.db"
                ).build().also { instance = it }
            }
    }
}
