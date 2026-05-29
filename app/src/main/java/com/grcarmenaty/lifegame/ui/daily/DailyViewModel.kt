package com.grcarmenaty.lifegame.ui.daily

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.grcarmenaty.lifegame.data.entities.Boon
import com.grcarmenaty.lifegame.data.entities.Daemon
import com.grcarmenaty.lifegame.data.entities.MinorQuest
import com.grcarmenaty.lifegame.domain.ApotheosisEvent
import com.grcarmenaty.lifegame.domain.PantheonRepository
import com.grcarmenaty.lifegame.domain.VoicePreset
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

data class DailyState(val daemons: List<DaemonDailyBlock> = emptyList())

data class DaemonDailyBlock(
    val daemon: Daemon,
    val level: Int,
    val levelProgress: Float,
    val leadingMajorTitle: String?,
    val greeting: String,
    val openMinors: List<MinorEntry>,
    val availableBoons: List<Boon>,
) {
    val totalWishes: Int get() = availableBoons.sumOf { it.count }
}

data class MinorEntry(
    val minor: MinorQuest,
    val parentMajorTitle: String,
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
    val state: StateFlow<DailyState> = repository.observeDaemons()
        .flatMapLatest { daemons ->
            if (daemons.isEmpty()) {
                flowOf(DailyState(emptyList()))
            } else {
                val blockFlows = daemons.map { buildDaemonBlockFlow(it) }
                combine(blockFlows) { blocks -> DailyState(blocks.toList()) }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DailyState())

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun buildDaemonBlockFlow(daemon: Daemon): Flow<DaemonDailyBlock> {
        val voice = VoicePreset.fromKey(daemon.voicePreset)
        val greetingSeed = daemon.id + todaySeed()

        val coreFlow: Flow<CoreBlock> = repository.observeMajors(daemon.id).flatMapLatest { majors ->
            val openMajors = majors.filter { !it.completed }
            val leading = openMajors.maxByOrNull {
                it.progressCount.toFloat() / it.thresholdCount.coerceAtLeast(1)
            }
            val levelProgress = leading?.let {
                (it.progressCount.toFloat() / it.thresholdCount.coerceAtLeast(1))
                    .coerceIn(0f, 1f)
            } ?: 0f
            val leadingTitle = leading?.title

            if (openMajors.isEmpty()) {
                flow {
                    emit(
                        CoreBlock(
                            level = repository.levelOf(daemon.id),
                            levelProgress = levelProgress,
                            leadingTitle = leadingTitle,
                            openMinors = emptyList(),
                        )
                    )
                }
            } else {
                val minorFlows: List<Flow<List<MinorEntry>>> = openMajors.map { major ->
                    repository.observeMinors(major.id).map { minors ->
                        minors.filter { isOpenToday(it) }
                            .map { MinorEntry(it, major.title) }
                    }
                }
                combine(minorFlows) { lists ->
                    CoreBlock(
                        level = repository.levelOf(daemon.id),
                        levelProgress = levelProgress,
                        leadingTitle = leadingTitle,
                        openMinors = lists.flatMap { it },
                    )
                }
            }
        }

        return combine(coreFlow, repository.observeBoons(daemon.id)) { core, boons ->
            DaemonDailyBlock(
                daemon = daemon,
                level = core.level,
                levelProgress = core.levelProgress,
                leadingMajorTitle = core.leadingTitle,
                greeting = voice.greeting(greetingSeed),
                openMinors = core.openMinors,
                availableBoons = boons.filter { it.count > 0 },
            )
        }
    }

    fun completeMinor(minorId: Long) {
        viewModelScope.launch {
            val event = repository.completeMinor(minorId)
            if (event != null) _apotheosis.value = event
        }
    }
    fun dismissApotheosis() { _apotheosis.value = null }

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

    private fun isOpenToday(m: MinorQuest): Boolean = when (m.cadence) {
        MinorQuest.CADENCE_ONE_OFF -> !m.completed
        MinorQuest.CADENCE_DAILY ->
            m.lastCompletedAt?.let { !sameLocalDay(it, System.currentTimeMillis()) } ?: true
        else -> !m.completed
    }

    private fun sameLocalDay(a: Long, b: Long): Boolean {
        val tz = TimeZone.getDefault()
        val ca = Calendar.getInstance(tz).apply { timeInMillis = a }
        val cb = Calendar.getInstance(tz).apply { timeInMillis = b }
        return ca.get(Calendar.YEAR) == cb.get(Calendar.YEAR) &&
            ca.get(Calendar.DAY_OF_YEAR) == cb.get(Calendar.DAY_OF_YEAR)
    }

    private fun todaySeed(): Long {
        val c = Calendar.getInstance()
        return (c.get(Calendar.YEAR) * 1000L) + c.get(Calendar.DAY_OF_YEAR)
    }

    private data class CoreBlock(
        val level: Int,
        val levelProgress: Float,
        val leadingTitle: String?,
        val openMinors: List<MinorEntry>,
    )

    companion object {
        fun factory(repository: PantheonRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                DailyViewModel(repository) as T
        }
    }
}
