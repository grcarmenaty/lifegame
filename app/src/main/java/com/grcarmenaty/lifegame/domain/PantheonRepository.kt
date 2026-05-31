package com.grcarmenaty.lifegame.domain

import com.grcarmenaty.lifegame.data.dao.BoonDao
import com.grcarmenaty.lifegame.data.dao.DaemonDao
import com.grcarmenaty.lifegame.data.dao.DialogueDao
import com.grcarmenaty.lifegame.data.dao.QuestDao
import com.grcarmenaty.lifegame.data.entities.Boon
import com.grcarmenaty.lifegame.data.entities.Daemon
import com.grcarmenaty.lifegame.data.entities.MajorQuest
import com.grcarmenaty.lifegame.data.entities.MinorQuest
import com.grcarmenaty.lifegame.domain.dialogue.ConversationContext
import com.grcarmenaty.lifegame.domain.dialogue.DialogueEngine
import com.grcarmenaty.lifegame.domain.dialogue.DialogueLine
import com.grcarmenaty.lifegame.domain.dialogue.DialogueStateStore
import com.grcarmenaty.lifegame.domain.dialogue.LineCategory
import com.grcarmenaty.lifegame.domain.dialogue.Surface
import com.grcarmenaty.lifegame.domain.dialogue.TimeOfDay
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
    private val dialogueDao: DialogueDao,
    private val dialogueEngine: DialogueEngine,
    private val dialogueStateStore: DialogueStateStore,
) {
    private val defaultThresholdForAddedMajor = 3

    // ---- Dialogue engine integration ----

    /**
     * Build a per-pick conversation context for [daemonId]. Many fields
     * are simplified for v0.0.6 (streak / weekly aggregates default to
     * cheap-to-compute values, foreground tracking unwired) — predicates
     * that need richer context fail-closed and the engine falls back to
     * lower tiers. The fields v0.0.6 *does* compute are exactly the ones
     * the shipped corpus reads.
     */
    private suspend fun buildContext(daemon: Daemon): ConversationContext {
        val state = dialogueStateStore.ensureDaemonState(daemon.id)
        val majors = questDao.observeMajorsForDaemon(daemon.id).let {
            // Use a non-suspending one-shot read via DAO suspend variant
            questDao.getMajorsForDaemon(daemon.id)
        }
        val openMajors = majors.filter { !it.completed }
        val recentlyClosedMajor = majors
            .filter { it.completed }
            .maxByOrNull { it.createdAt }
            ?.takeIf { System.currentTimeMillis() - it.createdAt < 24L * 3600_000L }
        val boons = boonDao.getForDaemon(daemon.id)
        val totalWishes = boons.sumOf { it.count }
        val level = 1 + questDao.countCompletedMajorsForDaemon(daemon.id)
        val daysSinceFirst = state.firstConversationAt?.let {
            ((System.currentTimeMillis() - it) / (24L * 3600_000L)).toInt()
        }
        val daysSinceLast = state.lastConversationAt?.let {
            ((System.currentTimeMillis() - it) / (24L * 3600_000L)).toInt()
        }
        return ConversationContext(
            daemonId = daemon.id,
            daemonName = daemon.name,
            archetypeKey = daemon.voicePreset,
            level = level,
            openMajors = openMajors,
            recentlyClosedMajor = recentlyClosedMajor,
            recentlyCompletedMinors = emptyList(),       // v0.0.6.1
            minorsCompletedToday = 0,                    // v0.0.6.1
            minorsCompletedThisWeek = 0,                 // v0.0.6.1
            dailyMinorsLapsedCount = 0,                  // v0.0.6.1
            streakDays = 0,                              // v0.0.6.1
            totalWishesAvailable = totalWishes,
            recentlySpentBoonText = null,                // v0.0.6.1
            conversationsHad = state.conversationsHad,
            daysSinceLastConversation = daysSinceLast,
            daysSinceFirstConversation = daysSinceFirst,
            majorsClosedTotal = state.majorsClosedTotal,
            wishesSpentTotal = state.wishesSpentTotal,
            minutesSinceLastForeground = null,           // v0.0.6.1
            dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK),
            timeOfDay = currentTimeOfDay(),
        )
    }

    // ---- Nudge worker support (v0.0.7) ----

    data class DaemonNudgeRow(
        val daemonId: Long,
        val daemonName: String,
        val notificationsEnabled: Boolean,
        val lastNudgeAt: Long?,
    )

    /**
     * Daemons the nudge worker should consider. Lazy-creates the
     * `daemon_state` row for daemons that don't have one yet so the
     * default `notificationsEnabled = true` lights up.
     */
    suspend fun allDaemonsForNudge(): List<DaemonNudgeRow> {
        return daemonDao.getAll().map { d ->
            val state = dialogueStateStore.ensureDaemonState(d.id)
            DaemonNudgeRow(
                daemonId = d.id,
                daemonName = d.name,
                notificationsEnabled = state.notificationsEnabled,
                lastNudgeAt = state.lastNudgeAt,
            )
        }
    }

    /** Engine pick for a NUDGE line, mark-played, returns text or null. */
    suspend fun pickNudgeLine(daemonId: Long): String? =
        pickInline(daemonId, LineCategory.NUDGE)

    suspend fun recordNudgeShown(daemonId: Long) {
        dialogueDao.recordNudge(daemonId, System.currentTimeMillis())
    }

    suspend fun setNotificationsEnabled(daemonId: Long, enabled: Boolean) {
        dialogueStateStore.ensureDaemonState(daemonId)
        dialogueDao.setNotificationsEnabled(daemonId, enabled)
    }

    /**
     * Engine pick + mark-played. Returns the rendered text or null if no
     * eligible line. Callers fall back to [VoicePreset] templated lines.
     */
    suspend fun pickInline(daemonId: Long, category: LineCategory): String? {
        val daemon = daemonDao.getById(daemonId) ?: return null
        val ctx = buildContext(daemon)
        val state = dialogueStateStore.loadState(daemonId)
        val line = dialogueEngine.pickFor(category, Surface.INLINE, ctx, state) ?: return null
        dialogueStateStore.markPlayed(daemonId, line, Surface.INLINE)
        return line.text
    }

    private fun currentTimeOfDay(): TimeOfDay {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 5..11 -> TimeOfDay.MORNING
            in 12..16 -> TimeOfDay.AFTERNOON
            in 17..20 -> TimeOfDay.EVENING
            else -> TimeOfDay.NIGHT
        }
    }

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
        firstMinorCadences: List<String> = emptyList(),
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
        firstMinorTitles.forEachIndexed { index, title ->
            val cadence = firstMinorCadences.getOrElse(index) { MinorQuest.CADENCE_ONE_OFF }
            questDao.insertMinor(
                MinorQuest(
                    majorQuestId = majorId,
                    title = title,
                    cadence = cadence,
                )
            )
        }
        return daemonId
    }

    /**
     * Mark a minor quest complete. The minor's contribution accumulates
     * onto its parent major's [MajorQuest.progressCount] for tracking,
     * but **never** auto-closes the major — closing a major is a user
     * decision (see [completeMajor]). Minors are small repeatable or
     * one-off acts; the major is a month-scale goal whose completion
     * the user controls.
     */
    suspend fun completeMinor(minorId: Long) {
        val minor = questDao.getMinorById(minorId) ?: return
        if (minor.completed && minor.cadence == MinorQuest.CADENCE_ONE_OFF) return
        if (minor.cadence == MinorQuest.CADENCE_DAILY &&
            minor.lastCompletedAt?.let { sameLocalDay(it, System.currentTimeMillis()) } == true) {
            return
        }

        questDao.updateMinor(
            minor.copy(
                completed = minor.cadence == MinorQuest.CADENCE_ONE_OFF,
                lastCompletedAt = System.currentTimeMillis(),
            )
        )

        val major = questDao.getMajorById(minor.majorQuestId) ?: return
        if (major.completed) return
        // Track-only: progressCount accumulates as informational signal,
        // but the major's `completed` flag is never flipped here.
        questDao.updateMajor(major.copy(progressCount = major.progressCount + minor.weight))
    }

    /**
     * User-driven close of a major. This is the only path that triggers
     * apotheosis (daemon level-up + boon deposit). Returns the event so
     * the UI can show the in-voice dialog; returns null if the major
     * doesn't exist or is already closed.
     */
    suspend fun completeMajor(majorId: Long): ApotheosisEvent? {
        val major = questDao.getMajorById(majorId) ?: return null
        if (major.completed) return null
        questDao.updateMajor(major.copy(completed = true))

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
        dialogueStateStore.ensureDaemonState(daemon.id)
        dialogueDao.incrementMajorsClosed(daemon.id)
        val engineLine = pickInline(daemon.id, LineCategory.APOTHEOSIS)
        return ApotheosisEvent(
            daemonId = daemon.id,
            daemonName = daemon.name,
            voicePreset = VoicePreset.fromKey(daemon.voicePreset),
            completedMajorTitle = major.title,
            newLevel = levelOf(daemon.id),
            grantedBoonText = grantedText,
            grantedBoonCount = if (grantedText != null) major.wishRewardCount else 0,
            engineLine = engineLine,
        )
    }

    /**
     * Reopen a previously-closed major (e.g., to undo a major that
     * auto-closed under pre-v0.0.10 behavior, or to walk back a wrong
     * tap). No wish refund — the boon deposit history is intentionally
     * permanent.
     */
    suspend fun reopenMajor(majorId: Long) {
        val major = questDao.getMajorById(majorId) ?: return
        if (!major.completed) return
        questDao.updateMajor(major.copy(completed = false))
    }

    /**
     * Spend one of [boonId]. Transactional via the DAO: if another
     * caller raced us to the last wish, the decrement is a no-op and
     * this returns null. Also increments the `wishesSpentTotal`
     * counter on `daemon_state` so the `WishesSpentAtLeast` earned
     * predicate can gate vulnerability lines.
     */
    suspend fun spendBoon(boonId: Long): String? {
        val before = boonDao.getById(boonId) ?: return null
        val spent = boonDao.spend(boonId) ?: return null
        dialogueStateStore.ensureDaemonState(before.daemonId)
        dialogueDao.incrementWishesSpent(before.daemonId)
        return spent.text
    }

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
        // v2 (Skeptic round 4): dialogue state travels with the daemons
        // so restore preserves voice continuity. "The play log IS the
        // relationship."
        val lineSeen = dialogueDao.allLineSeen()
        val cooldowns = dialogueDao.allCooldowns()
        val states = dialogueDao.allDaemonState()
        return json.encodeToString(
            PantheonBackup(
                exportedAt = System.currentTimeMillis(),
                appVersion = appVersion,
                daemons = daemons,
                lineSeen = lineSeen,
                cooldownPlay = cooldowns,
                daemonState = states,
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
        if (backup.formatVersion < PantheonBackup.MIN_SUPPORTED_FORMAT_VERSION ||
            backup.formatVersion > PantheonBackup.CURRENT_FORMAT_VERSION) {
            return ImportResult.Error(
                "Backup format v${backup.formatVersion} not supported by this build " +
                    "(supported: v${PantheonBackup.MIN_SUPPORTED_FORMAT_VERSION} to " +
                    "v${PantheonBackup.CURRENT_FORMAT_VERSION})."
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
        // v2 restore: dialogue state. v1 backups have empty lists and
        // the daemons start fresh on dialogue (acceptable degradation).
        if (backup.daemonState.isNotEmpty()) {
            dialogueDao.insertAllDaemonState(backup.daemonState)
        }
        if (backup.lineSeen.isNotEmpty()) {
            dialogueDao.insertAllLineSeen(backup.lineSeen)
        }
        if (backup.cooldownPlay.isNotEmpty()) {
            dialogueDao.insertAllCooldowns(backup.cooldownPlay)
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
    /** Engine-selected apotheosis line, or null → caller falls back to voice preset. */
    val engineLine: String? = null,
)
