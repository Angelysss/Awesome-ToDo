package com.awesometodo.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [TodoEntity::class, FocusSessionEntity::class, ActiveTimerEntity::class], version = 2, exportSchema = true)
@TypeConverters(AppTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao

    companion object {
        fun create(context: Context): AppDatabase = Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "awesome-todo.db",
        ).addMigrations(MIGRATION_1_2).build()

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE todos ADD COLUMN timerMode TEXT NOT NULL DEFAULT 'COUNTDOWN'")
                database.execSQL("ALTER TABLE focus_sessions ADD COLUMN timerMode TEXT NOT NULL DEFAULT 'COUNTDOWN'")
                database.execSQL("ALTER TABLE active_timer ADD COLUMN timerMode TEXT NOT NULL DEFAULT 'COUNTDOWN'")
            }
        }
    }
}
