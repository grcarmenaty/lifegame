package com.grcarmenaty.lifegame.domain

import com.grcarmenaty.lifegame.data.entities.Boon
import com.grcarmenaty.lifegame.data.entities.Daemon
import com.grcarmenaty.lifegame.data.entities.MajorQuest
import com.grcarmenaty.lifegame.data.entities.MinorQuest
import kotlinx.serialization.Serializable

/**
 * Backup wire format. Versioned via [formatVersion] so future schema
 * shifts can refuse a too-old or too-new file with a clear error
 * instead of crashing the deserializer.
 *
 * The hierarchy mirrors the FK graph: daemons own boons + major quests,
 * majors own minor quests. IDs are preserved on import so wishBoonId
 * references survive a round-trip.
 */
@Serializable
data class PantheonBackup(
    val formatVersion: Int = CURRENT_FORMAT_VERSION,
    val exportedAt: Long,
    val appVersion: String,
    val daemons: List<DaemonBackup>,
) {
    companion object {
        const val CURRENT_FORMAT_VERSION = 1
    }
}

@Serializable
data class DaemonBackup(
    val daemon: Daemon,
    val boons: List<Boon>,
    val majorQuests: List<MajorBackup>,
)

@Serializable
data class MajorBackup(
    val major: MajorQuest,
    val minors: List<MinorQuest>,
)

sealed class ImportResult {
    data class Success(val daemonCount: Int) : ImportResult()
    data class Error(val message: String) : ImportResult()
}
