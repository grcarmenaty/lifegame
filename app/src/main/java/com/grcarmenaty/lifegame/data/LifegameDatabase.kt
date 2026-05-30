package com.grcarmenaty.lifegame.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.grcarmenaty.lifegame.data.dao.BoonDao
import com.grcarmenaty.lifegame.data.dao.DaemonDao
import com.grcarmenaty.lifegame.data.dao.DialogueDao
import com.grcarmenaty.lifegame.data.dao.QuestDao
import com.grcarmenaty.lifegame.data.entities.Boon
import com.grcarmenaty.lifegame.data.entities.CooldownPlay
import com.grcarmenaty.lifegame.data.entities.Daemon
import com.grcarmenaty.lifegame.data.entities.DaemonState
import com.grcarmenaty.lifegame.data.entities.LineSeen
import com.grcarmenaty.lifegame.data.entities.MajorQuest
import com.grcarmenaty.lifegame.data.entities.MinorQuest

@Database(
    entities = [
        Daemon::class, MajorQuest::class, MinorQuest::class, Boon::class,
        LineSeen::class, CooldownPlay::class, DaemonState::class,
    ],
    version = 4,
    exportSchema = true,
)
abstract class LifegameDatabase : RoomDatabase() {
    abstract fun daemonDao(): DaemonDao
    abstract fun questDao(): QuestDao
    abstract fun boonDao(): BoonDao
    abstract fun dialogueDao(): DialogueDao

    companion object {
        @Volatile private var instance: LifegameDatabase? = null

        fun get(context: Context): LifegameDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    LifegameDatabase::class.java,
                    "lifegame.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                    .also { instance = it }
            }
    }
}

/**
 * v1 → v2: introduce the [Boon] table (one boon per daemon, seeded from
 * the legacy `daemons.boonText` / `daemons.wishesAvailable` columns),
 * add `wishBoonId` + `wishRewardCount` to `major_quests`, and drop the
 * legacy boon columns from `daemons`. (See v0.0.3 migration notes.)
 */
internal val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("PRAGMA foreign_keys = OFF")

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

        db.execSQL(
            """
            INSERT INTO `boons` (`daemonId`, `text`, `count`, `createdAt`)
            SELECT `id`, `boonText`, `wishesAvailable`, `createdAt` FROM `daemons`
            """.trimIndent()
        )

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
        db.query("PRAGMA foreign_key_check").use { /* throws if any row violates */ }
    }
}

/**
 * v2 → v3: pure additive — three new tables for the dialogue engine.
 * No recreation, no FK-add on existing rows.
 *   * `line_seen` — per-(daemon, line) play history
 *   * `cooldown_play` — surface-scoped cooldown rows
 *   * `daemon_state` — sibling table for per-daemon dialogue / relationship
 *     state and v0.0.7 cutdown-trigger instrumentation
 *
 * Existing daemons get no `daemon_state` row yet; the engine treats a
 * missing row as zero-everything. The repository inserts a row lazily on
 * the first dialogue event.
 */
/**
 * v3 → v4: pure additive — adds `notificationsEnabled` and
 * `lastNudgeAt` to `daemon_state` for v0.0.7 notifications.
 *
 * `ALTER TABLE ... ADD COLUMN` is safe here (no FK additions on
 * existing rows); existing rows take the defaults.
 */
internal val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE `daemon_state` ADD COLUMN `notificationsEnabled` INTEGER NOT NULL DEFAULT 1"
        )
        db.execSQL(
            "ALTER TABLE `daemon_state` ADD COLUMN `lastNudgeAt` INTEGER"
        )
    }
}

internal val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `line_seen` (
              `daemonId` INTEGER NOT NULL,
              `lineId` TEXT NOT NULL,
              `lastPlayedAt` INTEGER NOT NULL,
              `playCount` INTEGER NOT NULL,
              PRIMARY KEY (`daemonId`, `lineId`),
              FOREIGN KEY(`daemonId`) REFERENCES `daemons`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_line_seen_daemonId` ON `line_seen` (`daemonId`)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `cooldown_play` (
              `daemonId` INTEGER NOT NULL,
              `cooldownGroup` TEXT NOT NULL,
              `surface` TEXT NOT NULL,
              `expiresAtPicks` INTEGER NOT NULL,
              PRIMARY KEY (`daemonId`, `cooldownGroup`, `surface`),
              FOREIGN KEY(`daemonId`) REFERENCES `daemons`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_cooldown_play_daemonId` ON `cooldown_play` (`daemonId`)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `daemon_state` (
              `daemonId` INTEGER PRIMARY KEY NOT NULL,
              `lastConversationAt` INTEGER,
              `firstConversationAt` INTEGER,
              `conversationsHad` INTEGER NOT NULL DEFAULT 0,
              `majorsClosedTotal` INTEGER NOT NULL DEFAULT 0,
              `wishesSpentTotal` INTEGER NOT NULL DEFAULT 0,
              `screenOpenCount` INTEGER NOT NULL DEFAULT 0,
              `lastScreenOpenAt` INTEGER,
              FOREIGN KEY(`daemonId`) REFERENCES `daemons`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
    }
}
