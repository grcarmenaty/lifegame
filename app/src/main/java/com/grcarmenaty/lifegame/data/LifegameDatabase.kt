package com.grcarmenaty.lifegame.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.grcarmenaty.lifegame.data.dao.BoonDao
import com.grcarmenaty.lifegame.data.dao.DaemonDao
import com.grcarmenaty.lifegame.data.dao.QuestDao
import com.grcarmenaty.lifegame.data.entities.Boon
import com.grcarmenaty.lifegame.data.entities.Daemon
import com.grcarmenaty.lifegame.data.entities.MajorQuest
import com.grcarmenaty.lifegame.data.entities.MinorQuest

@Database(
    entities = [Daemon::class, MajorQuest::class, MinorQuest::class, Boon::class],
    version = 2,
    exportSchema = true,
)
abstract class LifegameDatabase : RoomDatabase() {
    abstract fun daemonDao(): DaemonDao
    abstract fun questDao(): QuestDao
    abstract fun boonDao(): BoonDao

    companion object {
        @Volatile private var instance: LifegameDatabase? = null

        fun get(context: Context): LifegameDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    LifegameDatabase::class.java,
                    "lifegame.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { instance = it }
            }
    }
}

/**
 * v1 → v2: introduce the [Boon] table (one boon per daemon, seeded from
 * the legacy `daemons.boonText` / `daemons.wishesAvailable` columns),
 * add `wishBoonId` + `wishRewardCount` to `major_quests`, and drop the
 * legacy boon columns from `daemons`. Both column-add-with-FK and
 * column-drop require full table recreation under SQLite < 3.35, which
 * we're targeting per minSdk 26.
 *
 * Invariant pinned here: `boons.count` is the source of truth for
 * available wishes. Majors are write-once triggers — on completion the
 * configured reward is added to `boons.count`. We never recompute from
 * historical majors. Deleting a boon nulls `major_quests.wishBoonId` for
 * any major still referencing it (ON DELETE SET NULL); historical
 * deposits remain.
 */
internal val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Room normally turns FKs on at connect time. During table
        // recreation we need them off so the intermediate state doesn't
        // fail FK validation.
        db.execSQL("PRAGMA foreign_keys = OFF")

        // 1. Create boons.
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `boons` (
              `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
              `daemonId` INTEGER NOT NULL,
              `text` TEXT NOT NULL,
              `count` INTEGER NOT NULL,
              `createdAt` INTEGER NOT NULL,
              FOREIGN KEY(`daemonId`) REFERENCES `daemons`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_boons_daemonId` ON `boons` (`daemonId`)")

        // 2. Seed one boon per existing daemon from the legacy columns.
        db.execSQL(
            """
            INSERT INTO `boons` (`daemonId`, `text`, `count`, `createdAt`)
            SELECT `id`, `boonText`, `wishesAvailable`, `createdAt` FROM `daemons`
            """.trimIndent()
        )

        // 3. Recreate major_quests with wishBoonId + wishRewardCount.
        db.execSQL(
            """
            CREATE TABLE `major_quests_new` (
              `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
              `daemonId` INTEGER NOT NULL,
              `title` TEXT NOT NULL,
              `description` TEXT,
              `thresholdCount` INTEGER NOT NULL,
              `progressCount` INTEGER NOT NULL,
              `completed` INTEGER NOT NULL,
              `wishBoonId` INTEGER,
              `wishRewardCount` INTEGER NOT NULL,
              `createdAt` INTEGER NOT NULL,
              FOREIGN KEY(`daemonId`) REFERENCES `daemons`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
              FOREIGN KEY(`wishBoonId`) REFERENCES `boons`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
            )
            """.trimIndent()
        )
        // Copy with each major's wishBoonId pointing at its daemon's
        // first boon, wishRewardCount = 1 by default.
        db.execSQL(
            """
            INSERT INTO `major_quests_new`
              (`id`, `daemonId`, `title`, `description`, `thresholdCount`,
               `progressCount`, `completed`, `wishBoonId`, `wishRewardCount`,
               `createdAt`)
            SELECT
              mq.`id`, mq.`daemonId`, mq.`title`, mq.`description`, mq.`thresholdCount`,
              mq.`progressCount`, mq.`completed`,
              (SELECT b.`id` FROM `boons` b WHERE b.`daemonId` = mq.`daemonId`
               ORDER BY b.`createdAt` ASC LIMIT 1),
              1,
              mq.`createdAt`
            FROM `major_quests` mq
            """.trimIndent()
        )
        db.execSQL("DROP TABLE `major_quests`")
        db.execSQL("ALTER TABLE `major_quests_new` RENAME TO `major_quests`")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_major_quests_daemonId` ON `major_quests` (`daemonId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_major_quests_wishBoonId` ON `major_quests` (`wishBoonId`)")

        // 4. Recreate daemons without boonText / wishesAvailable.
        db.execSQL(
            """
            CREATE TABLE `daemons_new` (
              `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
              `name` TEXT NOT NULL,
              `archetype` TEXT NOT NULL,
              `voicePreset` TEXT NOT NULL,
              `createdAt` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO `daemons_new` (`id`, `name`, `archetype`, `voicePreset`, `createdAt`)
            SELECT `id`, `name`, `archetype`, `voicePreset`, `createdAt` FROM `daemons`
            """.trimIndent()
        )
        db.execSQL("DROP TABLE `daemons`")
        db.execSQL("ALTER TABLE `daemons_new` RENAME TO `daemons`")

        db.execSQL("PRAGMA foreign_keys = ON")
        // Documented Room recipe: catch FK breaks the recreation might have introduced.
        db.query("PRAGMA foreign_key_check").use { /* throws if any row violates */ }
    }
}
