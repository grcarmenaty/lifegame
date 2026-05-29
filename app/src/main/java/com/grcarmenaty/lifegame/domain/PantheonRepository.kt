package com.grcarmenaty.lifegame.domain

import com.grcarmenaty.lifegame.data.dao.BoonDao
import com.grcarmenaty.lifegame.data.dao.DaemonDao
import com.grcarmenaty.lifegame.data.dao.QuestDao
import com.grcarmenaty.lifegame.data.entities.Boon
import com.grcarmenaty.lifegame.data.entities.Daemon
import com.grcarmenaty.lifegame.data.entities.MajorQuest
import com.grcarmenaty.lifegame.data.entities.MinorQuest
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Calendar
import java.util.TimeZone

/**
 * Single facade over Daemon + Quest + Boon DAOs. Encapsulates the
 * apotheosis rule (major-quest completion → daemon level-up + wish
 * accrual on the configured boon) so the UI never has to know how the
 * loop closes.
 *
 * Invariant: `boons.count` is the source of truth for available wishes.
 * Each major is a write-once trigger that increments its `wishBoonId`'s
 * count by `wishRewardCount` on completion. We never recompute wishes
 * from historical majors; deleting a boon nulls the FK on any major
 * still pointing at it (SET NULL) and historical deposits stay intact.
 */
class PantheonRepository(
    private val daemonDao: DaemonDao,
    private val questDao: QuestDao,
    private val boonDao: BoonDao,
) {
    private val defaultThresholdForAddedMajor = 3

    fun observeDaemons(): Flow<List<Daemon>> = daemonDao.observeAll()
    fun observeDaemon(id: Long): Flow<Daemon?> = daemonDao.observe(id)
    suspend fun daemonCount(): Int = daemonDao.count()
    suspend fun getDaemon(id: Long): Daemon? = daemonDao.getById(id)

    fun observeMajors(daemonId: Long): Flow<List<MajorQuest>> =
        questDao.observeMajorsForDaemon(daemonId)
    fun observeMinors(majorId: Long): Flow<List<MinorQuest>> =
        questDao.observeMinorsForMajor(majorId)
    fun observeBoons(daemonId: Long): Flow<List<Boon>> =
        boonDao.observeForDaemon(daemonId)

    suspend fun updateDaemon(
        id: Long,
        name: String,
        archetype: String,
        voicePreset: VoicePreset,
    ) {
        val existing = daemonDao.getById(id) ?: return
        daemonDao.update(
            existing.copy(
                name = name,
                archetype = archetype,
                voicePreset = voicePreset.name,
            )
        )
    }

    suspend fun vanishDaemon(id: Long) = daemonDao.deleteById(id)

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
            Daemon(name = name, archetype = archetype, voicePreset = voicePreset.name)
        )
        val boonId = boonDao.insert(
            Boon(daemonId = daemonId, text = boonText, count = 0)
        )
        val majorId = questDao.insertMajor(
            MajorQuest(
                daemonId = daemonId,
                title = firstMajorTitle,
                thresholdCount = firstMinorTitles.size.coerceAtLeast(1),
                wishBoonId = boonId,
                wishRewardCount = 1,
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
        // Apotheosis: deposit the configured reward on the boon. If
        // wishBoonId is null (boon was deleted), level-up only.
        val grantedText = major.wishBoonId?.let { boonId ->
            val boon = boonDao.getById(boonId) ?: return@let null
            if (major.wishRewardCount > 0) {
                boonDao.incrementCount(boonId, major.wishRewardCount)
            }
            boon.text
        }
        return ApotheosisEvent(
            daemonId = daemon.id,
            daemonName = daemon.name,
            voicePreset = VoicePreset.fromKey(daemon.voicePreset),
            completedMajorTitle = major.title,
            newLevel = levelOf(daemon.id),
            grantedBoonText = grantedText,
            grantedBoonCount = if (grantedText != null) major.wishRewardCount else 0,
        )
    }

    /**
     * Spend one of [boonId]. Transactional via the DAO: if another
     * caller raced us to the last wish, the decrement is a no-op and
     * this returns null.
     */
    suspend fun spendBoon(boonId: Long): String? = boonDao.spend(boonId)?.text

    // ---- CRUD: quests ----

    suspend fun addMajor(daemonId: Long, title: String) {
        // Default `wishBoonId` to the daemon's first boon; if there are
        // none somehow, the column is nullable and the major is just
        // level-only.
        val firstBoon = boonDao.getForDaemon(daemonId).firstOrNull()
        questDao.insertMajor(
            MajorQuest(
                daemonId = daemonId,
                title = title,
                thresholdCount = defaultThresholdForAddedMajor,
                wishBoonId = firstBoon?.id,
                wishRewardCount = 1,
            )
        )
    }

    /**
     * Add a minor under the given major. Returns false if the major is
     * already completed (UI should guard this too — see the Architect's
     * loop-hole note in the v0.0.3 design doc).
     */
    suspend fun addMinor(
        majorId: Long,
        title: String,
        cadence: String,
        weight: Int,
    ): Boolean {
        val major = questDao.getMajorById(majorId) ?: return false
        if (major.completed) return false
        questDao.insertMinor(
            MinorQuest(
                majorQuestId = majorId,
                title = title,
                cadence = cadence,
                weight = weight.coerceAtLeast(1),
            )
        )
        return true
    }

    suspend fun deleteMajor(majorId: Long) = questDao.deleteMajorById(majorId)
    suspend fun deleteMinor(minorId: Long) = questDao.deleteMinorById(minorId)

    suspend fun progressLossPreview(majorId: Long): Pair<Int, Int> {
        val done = questDao.countCompletedMinorsForMajor(majorId)
        val total = questDao.countMinorsForMajor(majorId)
        return done to total
    }

    // ---- CRUD: boons ----

    suspend fun addBoon(daemonId: Long, text: String, initialCount: Int) {
        boonDao.insert(
            Boon(daemonId = daemonId, text = text, count = initialCount.coerceAtLeast(0))
        )
    }

    suspend fun deleteBoon(boonId: Long) = boonDao.deleteById(boonId)

    // ---- Backup / restore / reset ----

    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    suspend fun exportToJson(appVersion: String): String {
        val daemons = daemonDao.getAll().map { d ->
            val boons = boonDao.getForDaemon(d.id)
            val majors = questDao.getMajorsForDaemon(d.id).map { m ->
                MajorBackup(major = m, minors = questDao.getMinorsForMajor(m.id))
            }
            DaemonBackup(daemon = d, boons = boons, majorQuests = majors)
        }
        return json.encodeToString(
            PantheonBackup(
                exportedAt = System.currentTimeMillis(),
                appVersion = appVersion,
                daemons = daemons,
            )
        )
    }

    /**
     * Replace semantics: wipe the current pantheon, then load the
     * backup. IDs are preserved so `wishBoonId` references survive
     * the round-trip; SQLite's autoincrement sequence updates to
     * `max(id)` so subsequent insertions don't collide.
     */
    suspend fun importFromJson(jsonText: String): ImportResult {
        val backup = try {
            json.decodeFromString<PantheonBackup>(jsonText)
        } catch (e: SerializationException) {
            return ImportResult.Error("Backup is malformed: ${e.message ?: "parse failed"}")
        } catch (e: IllegalArgumentException) {
            return ImportResult.Error("Backup is malformed: ${e.message ?: "invalid"}")
        }
        if (backup.formatVersion != PantheonBackup.CURRENT_FORMAT_VERSION) {
            return ImportResult.Error(
                "Backup format v${backup.formatVersion} not supported by this build " +
                    "(expected v${PantheonBackup.CURRENT_FORMAT_VERSION})."
            )
        }
        reset()
        backup.daemons.forEach { dwc ->
            daemonDao.insert(dwc.daemon)
            dwc.boons.forEach { boonDao.insert(it) }
            dwc.majorQuests.forEach { mwm ->
                questDao.insertMajor(mwm.major)
                mwm.minors.forEach { questDao.insertMinor(it) }
            }
        }
        return ImportResult.Success(daemonCount = backup.daemons.size)
    }

    suspend fun reset() {
        // FK CASCADE deletes boons, major_quests, and minor_quests for us.
        daemonDao.deleteAll()
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
    val grantedBoonText: String?,
    val grantedBoonCount: Int,
)
