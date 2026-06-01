package com.grcarmenaty.lifegame.ui.daily

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.grcarmenaty.lifegame.data.entities.Boon
import com.grcarmenaty.lifegame.data.entities.Daemon
import com.grcarmenaty.lifegame.data.entities.DaemonState
import com.grcarmenaty.lifegame.data.entities.MinorQuest
import com.grcarmenaty.lifegame.domain.ApotheosisEvent
import com.grcarmenaty.lifegame.domain.PantheonRepository
import com.grcarmenaty.lifegame.domain.VoicePreset
import com.grcarmenaty.lifegame.domain.attention.AttentionMath
import java.util.Calendar
import java.util.TimeZone
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DailyState(
    val daemons: List<DaemonDailyBlock> = emptyList(),
    val pendingLevelUps: List<PendingLevelUp> = emptyList(),
)

data class DaemonDailyBlock(
    val daemon: Daemon,
    val level: Int,
    val attentionPoints: Int,
    val levelProgress: Float,
    /** True at level 4 — UI shows a shimmer pip on the bar (Ally polish). */
    val atMaxLevel: Boolean,
    val greeting: String,
    /** Minors grouped under their parent major. v0.0.11: minors live
     *  under their major as a heading, not as a sub-label per card. */
    val majorGroups: List<MajorGroup>,
    val availableBoons: List<Boon>,
) {
    val totalWishes: Int get() = availableBoons.sumOf { it.count }
    /** Total open minors across all groups, for empty-state rendering. */
    val openMinorCount: Int get() = majorGroups.sumOf { it.openMinors.size }
}

data class MajorGroup(
    val majorId: Long,
    val majorTitle: String,
    val openMinors: List<MinorQuest>,
)

/** Daemons whose computed level has risen above their `lastSeenLevel`. */
data class PendingLevelUp(
    val daemonId: Long,
    val daemonName: String,
    val newLevel: Int,
)

/** Open picker state — non-null means the spend dialog is showing. */
data class SpendPickerState(
    val daemonId: Long,
    val daemonName: String,
    val boons: List<Boon>,
)

class DailyViewModel(
    private val repository: PantheonRepository,
) : ViewModel() {

    private val _apotheosis = MutableStateFlow<ApotheosisEvent?>(null)
    val apotheosis: StateFlow<ApotheosisEvent?> = _apotheosis

    private val _boonGranted = MutableStateFlow<String?>(null)
    val boonGranted: StateFlow<String?> = _boonGranted

    private val _spendPicker = MutableStateFlow<SpendPickerState?>(null)
    val spendPicker: StateFlow<SpendPickerState?> = _spendPicker

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<DailyState> = run {
        val daemonsBlocksFlow: Flow<List<DaemonDailyBlock>> = repository.observeDaemons()
            .flatMapLatest { daemons ->
                if (daemons.isEmpty()) flowOf(emptyList())
                else combine(daemons.map { buildDaemonBlockFlow(it) }) { it.toList() }
            }
        combine(daemonsBlocksFlow, repository.observeAllDaemonState()) { blocks, allStates ->
            val byId = blocks.associateBy { it.daemon.id }
            val pending = allStates.mapNotNull { s ->
                val level = AttentionMath.levelFor(s.attentionPoints)
                if (level > s.lastSeenLevel) {
                    val name = byId[s.daemonId]?.daemon?.name ?: return@mapNotNull null
                    PendingLevelUp(s.daemonId, name, level)
                } else null
            }
            DailyState(daemons = blocks, pendingLevelUps = pending)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DailyState())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun buildDaemonBlockFlow(daemon: Daemon): Flow<DaemonDailyBlock> {
        val voice = VoicePreset.fromKey(daemon.voicePreset)
        val greetingSeed = daemon.id + todaySeed()

        val majorGroupsFlow: Flow<List<MajorGroup>> =
            repository.observeMajors(daemon.id).flatMapLatest { majors ->
                val openMajors = majors.filter { !it.completed }
                if (openMajors.isEmpty()) {
                    flowOf(emptyList())
                } else {
                    val perMajor: List<Flow<MajorGroup>> = openMajors.map { major ->
                        repository.observeMinors(major.id).map { minors ->
                            MajorGroup(
                                majorId = major.id,
                                majorTitle = major.title,
                                openMinors = minors.filter { isOpenNow(it) },
                            )
                        }
                    }
                    // Skip empty groups — a major with no open minors today
                    // (e.g., everything daily-completed already) shouldn't
                    // show as a heading with nothing under it.
                    combine(perMajor) { groups ->
                        groups.toList().filter { it.openMinors.isNotEmpty() }
                    }
                }
            }

        return combine(
            majorGroupsFlow,
            repository.observeBoons(daemon.id),
            repository.observeDaemonState(daemon.id),
        ) { majorGroups, boons, ds ->
            val attention = ds?.attentionPoints ?: 0
            val level = AttentionMath.levelFor(attention)
            DaemonDailyBlock(
                daemon = daemon,
                level = level,
                attentionPoints = attention,
                levelProgress = AttentionMath.levelProgress(attention),
                atMaxLevel = level >= AttentionMath.MAX_LEVEL,
                greeting = voice.greeting(greetingSeed),
                majorGroups = majorGroups,
                availableBoons = boons.filter { it.count > 0 },
            )
        }
    }

    fun completeMinor(minorId: Long) {
        viewModelScope.launch {
            repository.completeMinor(minorId)
            // No apotheosis here — closing a major is user-driven
            // (see DaemonDetailViewModel.completeMajor). Minor
            // completions track progress but never auto-close.
            // Boon-from-minors accrual happens inside repo.
            // Level-up emission also inside repo (via SharedFlow).
        }
    }
    fun dismissApotheosis() { _apotheosis.value = null }

    fun acknowledgeLevelUp(daemonId: Long) {
        viewModelScope.launch { repository.acknowledgeLevelUp(daemonId) }
    }

    fun openSpendPicker(daemonId: Long) {
        val daemon = state.value.daemons.firstOrNull { it.daemon.id == daemonId } ?: return
        if (daemon.availableBoons.isEmpty()) return
        _spendPicker.value = SpendPickerState(
            daemonId = daemonId,
            daemonName = daemon.daemon.name,
            boons = daemon.availableBoons,
        )
    }

    fun cancelSpendPicker() { _spendPicker.value = null }

    fun confirmSpend(boonId: Long) {
        viewModelScope.launch {
            val boonText = repository.spendBoon(boonId)
            _spendPicker.value = null
            if (boonText != null) _boonGranted.value = boonText
        }
    }

    fun dismissBoon() { _boonGranted.value = null }

    private fun isOpenNow(m: MinorQuest): Boolean {
        val last = m.lastCompletedAt ?: return !m.completed
        val now = System.currentTimeMillis()
        return when (m.cadence) {
            MinorQuest.CADENCE_ONE_OFF -> !m.completed
            MinorQuest.CADENCE_DAILY -> !sameLocalDay(last, now)
            MinorQuest.CADENCE_WEEKLY -> !sameLocalWeek(last, now)
            MinorQuest.CADENCE_MONTHLY -> !sameLocalMonth(last, now)
            else -> !m.completed
        }
    }

    private fun sameLocalDay(a: Long, b: Long): Boolean {
        val tz = TimeZone.getDefault()
        val ca = Calendar.getInstance(tz).apply { timeInMillis = a }
        val cb = Calendar.getInstance(tz).apply { timeInMillis = b }
        return ca.get(Calendar.YEAR) == cb.get(Calendar.YEAR) &&
            ca.get(Calendar.DAY_OF_YEAR) == cb.get(Calendar.DAY_OF_YEAR)
    }

    private fun sameLocalWeek(a: Long, b: Long): Boolean {
        val tz = TimeZone.getDefault()
        val ca = Calendar.getInstance(tz).apply { timeInMillis = a }
        val cb = Calendar.getInstance(tz).apply { timeInMillis = b }
        return ca.getWeekYear() == cb.getWeekYear() &&
            ca.get(Calendar.WEEK_OF_YEAR) == cb.get(Calendar.WEEK_OF_YEAR)
    }

    private fun sameLocalMonth(a: Long, b: Long): Boolean {
        val tz = TimeZone.getDefault()
        val ca = Calendar.getInstance(tz).apply { timeInMillis = a }
        val cb = Calendar.getInstance(tz).apply { timeInMillis = b }
        return ca.get(Calendar.YEAR) == cb.get(Calendar.YEAR) &&
            ca.get(Calendar.MONTH) == cb.get(Calendar.MONTH)
    }

    private fun todaySeed(): Long {
        val c = Calendar.getInstance()
        return (c.get(Calendar.YEAR) * 1000L) + c.get(Calendar.DAY_OF_YEAR)
    }

    companion object {
        fun factory(repository: PantheonRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                DailyViewModel(repository) as T
        }
    }
}
