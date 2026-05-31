package com.grcarmenaty.lifegame.domain

import com.grcarmenaty.lifegame.data.dao.BoonDao
import com.grcarmenaty.lifegame.data.dao.DaemonDao
import com.grcarmenaty.lifegame.data.dao.DialogueDao
import com.grcarmenaty.lifegame.data.dao.EpicChapterDao
import com.grcarmenaty.lifegame.data.dao.QuestDao
import com.grcarmenaty.lifegame.data.entities.Boon
import com.grcarmenaty.lifegame.data.entities.Daemon
import com.grcarmenaty.lifegame.data.entities.DaemonState
import com.grcarmenaty.lifegame.data.entities.EpicChapter
import com.grcarmenaty.lifegame.data.entities.MajorQuest
import com.grcarmenaty.lifegame.data.entities.MinorQuest
import com.grcarmenaty.lifegame.domain.attention.AttentionConfig
import com.grcarmenaty.lifegame.domain.attention.AttentionDecay
import com.grcarmenaty.lifegame.domain.attention.AttentionMath
import com.grcarmenaty.lifegame.domain.dialogue.ConversationContext
import com.grcarmenaty.lifegame.domain.dialogue.DialogueEngine
import com.grcarmenaty.lifegame.domain.dialogue.DialogueLine
import com.grcarmenaty.lifegame.domain.dialogue.DialogueStateStore
import com.grcarmenaty.lifegame.domain.dialogue.LineCategory
import com.grcarmenaty.lifegame.domain.dialogue.Surface
import com.grcarmenaty.lifegame.domain.dialogue.TimeOfDay
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
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
    private val epicChapterDao: EpicChapterDao,
    private val attentionDecay: AttentionDecay,
) {
    private val defaultThresholdForAddedMajor = 3

    /**
     * Emits when a daemon's computed level rises above its
     * `lastSeenLevel`. UI listens via [LaunchedEffect] and queues the
     * boon-level-up review banner. `extraBufferCapacity = 16` so a
     * burst of completions across daemons doesn't drop events.
     */
    private val _levelUps = MutableSharedFlow<LevelUpEvent>(
        replay = 0,
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val levelUps: SharedFlow<LevelUpEvent> = _levelUps.asSharedFlow()

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

    /**
     * v0.0.10: level is now attention-derived. Falls back to the
     * pre-migration formula (1 + completed-majors) only if the
     * `daemon_state` row hasn't been created yet (shouldn't happen
     * post-migration — every daemon gets a row via INSERT OR IGNORE).
     */
    suspend fun levelOf(daemonId: Long): Int {
        val state = dialogueDao.daemonState(daemonId)
        return if (state == null) {
            1 + questDao.countCompletedMajorsForDaemon(daemonId)
        } else {
            AttentionMath.levelFor(state.attentionPoints)
        }
    }

    /** Read accessor for UI to display the attention-point counter. */
    suspend fun attentionOf(daemonId: Long): Int =
        dialogueDao.daemonState(daemonId)?.attentionPoints ?: 0

    /** Observable per-daemon state — used by detail screen to react to
     *  attention/decay changes without per-tick polling. */
    fun observeDaemonState(daemonId: Long): Flow<DaemonState?> =
        dialogueDao.observeDaemonState(daemonId)

    /** All daemon_state rows, observable — for the Daily screen banner
     *  that surfaces pending level-ups. */
    fun observeAllDaemonState(): Flow<List<DaemonState>> =
        dialogueDao.observeAllDaemonState()

    /**
     * Returns the resolved decay/accrual config for [daemonId] — used
     * by the detail screen edit form to display the effective values.
     */
    suspend fun resolvedAttentionConfig(daemonId: Long) =
        getDaemon(daemonId)?.let { daemon ->
            val state = dialogueStateStore.ensureDaemonState(daemonId)
            AttentionConfig.resolve(state, VoicePreset.fromKey(daemon.voicePreset))
        }

    suspend fun setDecayOverride(daemonId: Long, decayPerDay: Int?, graceDays: Int?) {
        dialogueStateStore.ensureDaemonState(daemonId)
        dialogueDao.setDecayOverride(daemonId, decayPerDay, graceDays)
    }

    suspend fun setDecayDisabled(daemonId: Long, disabled: Boolean) {
        dialogueStateStore.ensureDaemonState(daemonId)
        dialogueDao.setDecayDisabled(daemonId, disabled)
    }

    suspend fun setMinorsPerBoonOverride(daemonId: Long, minorsPerBoon: Int?) {
        dialogueStateStore.ensureDaemonState(daemonId)
        dialogueDao.setMinorsPerBoonOverride(daemonId, minorsPerBoon)
    }

    /**
     * Pending level-up addressables for the Daily banner. A daemon is
     * pending if its computed level exceeds its `lastSeenLevel`.
     */
    suspend fun pendingLevelUpDaemons(): List<DaemonState> =
        dialogueDao.allDaemonState().filter {
            AttentionMath.levelFor(it.attentionPoints) > it.lastSeenLevel
        }

    /** Bump `lastSeenLevel` after the user acknowledges a level-up. */
    suspend fun acknowledgeLevelUp(daemonId: Long) {
        val state = dialogueDao.daemonState(daemonId) ?: return
        val current = AttentionMath.levelFor(state.attentionPoints)
        if (current > state.lastSeenLevel) {
            dialogueDao.bumpLastSeenLevel(daemonId, current)
        }
    }

    // ---- Epic chapters ----

    fun observeEpicChapters(daemonId: Long): Flow<List<EpicChapter>> =
        epicChapterDao.observeForDaemon(daemonId)

    suspend fun addEpicChapter(daemonId: Long, text: String) {
        if (text.isBlank()) return
        val position = epicChapterDao.countForDaemon(daemonId)
        epicChapterDao.insert(
            EpicChapter(daemonId = daemonId, position = position, text = text.trim())
        )
    }

    suspend fun deleteEpicChapter(chapterId: Long) = epicChapterDao.deleteById(chapterId)

    // ---- Decay (called by workers) ----

    /** Run decay for every daemon. Used by the periodic decay worker
     *  and as the first step of [NudgeWorker] (Architect's "decay
     *  before nudge" rule). */
    suspend fun runDecay() {
        attentionDecay.applyForAll()
    }

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
     * Mark a minor quest complete. Side effects:
     *  - the minor row updates (one-off → completed; daily → today
     *    flagged as done)
     *  - progressCount on the parent major bumps (informational only;
     *    the major **never** auto-closes, see [completeMajor])
     *  - the daemon's `attentionPoints` grows by `minor.weight`
     *  - the daemon's `minorsCompletedSinceAccrual` increments; if it
     *    reaches the resolved threshold, +1 to the daemon's first
     *    boon and counter resets
     *  - if attention crosses a level threshold, [levelUps] emits
     */
    suspend fun completeMinor(minorId: Long) {
        val minor = questDao.getMinorById(minorId) ?: return
        if (minor.completed && minor.cadence == MinorQuest.CADENCE_ONE_OFF) return
        val now = System.currentTimeMillis()
        if (minor.cadence == MinorQuest.CADENCE_DAILY &&
            minor.lastCompletedAt?.let { sameLocalDay(it, now) } == true) {
            return
        }

        questDao.updateMinor(
            minor.copy(
                completed = minor.cadence == MinorQuest.CADENCE_ONE_OFF,
                lastCompletedAt = now,
            )
        )

        val major = questDao.getMajorById(minor.majorQuestId) ?: return
        if (!major.completed) {
            // Track-only: progressCount accumulates as informational
            // signal, but the major's `completed` flag is never flipped
            // here.
            questDao.updateMajor(major.copy(progressCount = major.progressCount + minor.weight))
        }

        val daemonId = major.daemonId
        val daemon = daemonDao.getById(daemonId) ?: return
        val state = dialogueStateStore.ensureDaemonState(daemonId)
        val voice = VoicePreset.fromKey(daemon.voicePreset)
        val cfg = AttentionConfig.resolve(state, voice)

        val before = state.attentionPoints
        val after = before + minor.weight
        dialogueDao.addAttention(daemonId, minor.weight, now)
        emitLevelUpIfCrossed(daemon, before, after, state.lastSeenLevel)

        // Boon-from-minors accrual: bump counter, fire on threshold.
        val nextCounter = state.minorsCompletedSinceAccrual + 1
        if (nextCounter >= cfg.minorsPerBoonAccrual) {
            val firstBoon = boonDao.getForDaemon(daemonId).firstOrNull()
            if (firstBoon != null) {
                boonDao.incrementCount(firstBoon.id, 1)
            }
            dialogueDao.setMinorsCompletedSinceAccrual(daemonId, 0)
        } else {
            dialogueDao.setMinorsCompletedSinceAccrual(daemonId, nextCounter)
        }
    }

    /**
     * User-driven close of a major. Deposits a flat +25 attention
     * bonus (per v0.0.10 plan §3.1) and emits a [LevelUpEvent] if the
     * deposit tips through a level threshold.
     *
     * NOTE: boon deposit is GONE under the new model — boons accrue
     * from ongoing minor work, not from discrete major closures. The
     * `wishBoonId` / `wishRewardCount` columns on `major_quests` are
     * vestigial and will be dropped in a future schema-cleanup pass.
     */
    suspend fun completeMajor(majorId: Long): ApotheosisEvent? {
        val major = questDao.getMajorById(majorId) ?: return null
        if (major.completed) return null
        questDao.updateMajor(major.copy(completed = true))

        val daemon = daemonDao.getById(major.daemonId) ?: return null
        val state = dialogueStateStore.ensureDaemonState(daemon.id)
        dialogueDao.incrementMajorsClosed(daemon.id)

        val before = state.attentionPoints
        val after = before + AttentionMath.MAJOR_CLOSURE_ATTENTION
        val now = System.currentTimeMillis()
        dialogueDao.addAttention(daemon.id, AttentionMath.MAJOR_CLOSURE_ATTENTION, now)
        emitLevelUpIfCrossed(daemon, before, after, state.lastSeenLevel)

        val engineLine = pickInline(daemon.id, LineCategory.APOTHEOSIS)
        return ApotheosisEvent(
            daemonId = daemon.id,
            daemonName = daemon.name,
            voicePreset = VoicePreset.fromKey(daemon.voicePreset),
            completedMajorTitle = major.title,
            newLevel = AttentionMath.levelFor(after),
            grantedBoonText = null,   // boons no longer come from major closure
            grantedBoonCount = 0,
            engineLine = engineLine,
        )
    }

    private suspend fun emitLevelUpIfCrossed(
        daemon: Daemon,
        attentionBefore: Int,
        attentionAfter: Int,
        lastSeenLevel: Int,
    ) {
        val newLevel = AttentionMath.levelFor(attentionAfter)
        if (newLevel > lastSeenLevel) {
            // Ally polish: multi-level jump fires ONCE with final level;
            // lastSeenLevel never rewinds on decay.
            _levelUps.tryEmit(
                LevelUpEvent(
                    daemonId = daemon.id,
                    daemonName = daemon.name,
                    voicePreset = VoicePreset.fromKey(daemon.voicePreset),
                    newLevel = newLevel,
                    previousLevel = AttentionMath.levelFor(attentionBefore),
                )
            )
        }
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
        // v3 (v0.0.10): epic chapters added; DaemonState gains the
        // attention columns automatically via @Serializable.
        val lineSeen = dialogueDao.allLineSeen()
        val cooldowns = dialogueDao.allCooldowns()
        val states = dialogueDao.allDaemonState()
        val chapters = epicChapterDao.all()
        return json.encodeToString(
            PantheonBackup(
                exportedAt = System.currentTimeMillis(),
                appVersion = appVersion,
                daemons = daemons,
                lineSeen = lineSeen,
                cooldownPlay = cooldowns,
                daemonState = states,
                epicChapters = chapters,
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
        // v3: epic chapters.
        if (backup.epicChapters.isNotEmpty()) {
            epicChapterDao.insertAll(backup.epicChapters)
        }

        // Architect (round 1 v0.0.10): on v1/v2 import, the loaded
        // DaemonState (if any) lacks the v0.0.10 attention columns and
        // gets defaults — meaning restored daemons land at level 0
        // unless we re-run the migration backfill. Ensure a row exists
        // for every daemon, then backfill attention from the imported
        // majors using the same helper the migration uses.
        if (backup.formatVersion < 3) {
            backup.daemons.forEach { dwc ->
                dialogueStateStore.ensureDaemonState(dwc.daemon.id)
                val allMinorsForDaemon = mutableListOf<MinorQuest>()
                val majors = dwc.majorQuests.map { mwm ->
                    allMinorsForDaemon += mwm.minors
                    mwm.major
                }
                val backfilled = com.grcarmenaty.lifegame.domain.attention.AttentionBackfill.compute(majors)
                if (backfilled > 0) {
                    dialogueDao.addAttention(dwc.daemon.id, backfilled, System.currentTimeMillis())
                    val newLevel = AttentionMath.levelFor(backfilled)
                    if (newLevel > 0) dialogueDao.bumpLastSeenLevel(dwc.daemon.id, newLevel)
                }
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
    /** Engine-selected apotheosis line, or null → caller falls back to voice preset. */
    val engineLine: String? = null,
)

/** Emitted when a daemon's computed level rises above [lastSeenLevel]. */
data class LevelUpEvent(
    val daemonId: Long,
    val daemonName: String,
    val voicePreset: VoicePreset,
    val newLevel: Int,
    val previousLevel: Int,
)
