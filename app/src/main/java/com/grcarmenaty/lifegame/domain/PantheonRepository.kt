package com.grcarmenaty.lifegame.domain

import com.grcarmenaty.lifegame.data.dao.DaemonDao
import com.grcarmenaty.lifegame.data.dao.QuestDao
import com.grcarmenaty.lifegame.data.entities.Daemon
import com.grcarmenaty.lifegame.data.entities.MajorQuest
import com.grcarmenaty.lifegame.data.entities.MinorQuest
import kotlinx.coroutines.flow.Flow
import java.util.Calendar
import java.util.TimeZone

/**
 * Single facade over Daemon + Quest DAOs. Encapsulates the apotheosis
 * rule (major-quest completion → daemon level-up + wish accrual) so the
 * UI never has to know how the loop closes.
 */
class PantheonRepository(
    private val daemonDao: DaemonDao,
    private val questDao: QuestDao,
) {
    fun observeDaemons(): Flow<List<Daemon>> = daemonDao.observeAll()

    suspend fun daemonCount(): Int = daemonDao.count()

    suspend fun getDaemon(id: Long): Daemon? = daemonDao.getById(id)

    fun observeMajors(daemonId: Long): Flow<List<MajorQuest>> =
        questDao.observeMajorsForDaemon(daemonId)

    fun observeMinors(majorId: Long): Flow<List<MinorQuest>> =
        questDao.observeMinorsForMajor(majorId)

    /** Derived per design v2: level starts at 1, +1 per completed major. */
    suspend fun levelOf(daemonId: Long): Int =
        1 + questDao.countCompletedMajorsForDaemon(daemonId)

    suspend fun summonDaemon(
        name: String,
        archetype: String,
        voicePreset: VoicePreset,
        boonText: String,
        firstMajorTitle: String,
        firstMinorTitles: List<String>,
    ): Long {
        val daemonId = daemonDao.insert(
            Daemon(
                name = name,
                archetype = archetype,
                voicePreset = voicePreset.name,
                boonText = boonText,
            )
        )
        val majorId = questDao.insertMajor(
            MajorQuest(
                daemonId = daemonId,
                title = firstMajorTitle,
                thresholdCount = firstMinorTitles.size.coerceAtLeast(1),
            )
        )
        firstMinorTitles.forEach { title ->
            questDao.insertMinor(
                MinorQuest(
                    majorQuestId = majorId,
                    title = title,
                    cadence = MinorQuest.CADENCE_ONE_OFF,
                )
            )
        }
        return daemonId
    }

    /**
     * Mark a minor quest complete and propagate progress to its major.
     * Returns an [ApotheosisEvent] if this completion tipped the major
     * over its threshold — the UI uses it to show the apotheosis dialog.
     */
    suspend fun completeMinor(minorId: Long): ApotheosisEvent? {
        val minor = questDao.getMinorById(minorId) ?: return null
        if (minor.completed && minor.cadence == MinorQuest.CADENCE_ONE_OFF) return null
        if (minor.cadence == MinorQuest.CADENCE_DAILY &&
            minor.lastCompletedAt?.let { sameLocalDay(it, System.currentTimeMillis()) } == true) {
            return null
        }

        questDao.updateMinor(
            minor.copy(
                completed = minor.cadence == MinorQuest.CADENCE_ONE_OFF,
                lastCompletedAt = System.currentTimeMillis(),
            )
        )

        val major = questDao.getMajorById(minor.majorQuestId) ?: return null
        if (major.completed) return null

        val newProgress = major.progressCount + minor.weight
        val nowComplete = newProgress >= major.thresholdCount

        questDao.updateMajor(
            major.copy(progressCount = newProgress, completed = nowComplete)
        )

        if (!nowComplete) return null

        val daemon = daemonDao.getById(major.daemonId) ?: return null
        daemonDao.update(daemon.copy(wishesAvailable = daemon.wishesAvailable + 1))
        return ApotheosisEvent(
            daemonId = daemon.id,
            daemonName = daemon.name,
            voicePreset = VoicePreset.fromKey(daemon.voicePreset),
            completedMajorTitle = major.title,
            newLevel = levelOf(daemon.id),
        )
    }

    suspend fun spendWish(daemonId: Long): String? {
        val daemon = daemonDao.getById(daemonId) ?: return null
        if (daemon.wishesAvailable <= 0) return null
        daemonDao.update(daemon.copy(wishesAvailable = daemon.wishesAvailable - 1))
        return daemon.boonText
    }

    private fun sameLocalDay(a: Long, b: Long): Boolean {
        val tz = TimeZone.getDefault()
        val ca = Calendar.getInstance(tz).apply { timeInMillis = a }
        val cb = Calendar.getInstance(tz).apply { timeInMillis = b }
        return ca.get(Calendar.YEAR) == cb.get(Calendar.YEAR) &&
            ca.get(Calendar.DAY_OF_YEAR) == cb.get(Calendar.DAY_OF_YEAR)
    }
}

data class ApotheosisEvent(
    val daemonId: Long,
    val daemonName: String,
    val voicePreset: VoicePreset,
    val completedMajorTitle: String,
    val newLevel: Int,
)
