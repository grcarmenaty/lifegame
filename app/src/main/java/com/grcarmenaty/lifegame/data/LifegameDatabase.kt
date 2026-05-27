package com.grcarmenaty.lifegame.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.grcarmenaty.lifegame.data.dao.DaemonDao
import com.grcarmenaty.lifegame.data.dao.QuestDao
import com.grcarmenaty.lifegame.data.entities.Daemon
import com.grcarmenaty.lifegame.data.entities.MajorQuest
import com.grcarmenaty.lifegame.data.entities.MinorQuest

@Database(
    entities = [Daemon::class, MajorQuest::class, MinorQuest::class],
    version = 1,
    exportSchema = false
)
abstract class LifegameDatabase : RoomDatabase() {
    abstract fun daemonDao(): DaemonDao
    abstract fun questDao(): QuestDao

    companion object {
        @Volatile private var instance: LifegameDatabase? = null

        fun get(context: Context): LifegameDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    LifegameDatabase::class.java,
                    "lifegame.db"
                ).build().also { instance = it }
            }
    }
}
