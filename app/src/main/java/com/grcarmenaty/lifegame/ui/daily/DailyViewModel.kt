package com.grcarmenaty.lifegame.ui.daily

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
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
    val greeting: String,
    val openMinors: List<MinorEntry>,
)

data class MinorEntry(
    val minor: MinorQuest,
    val parentMajorTitle: String,
)

class DailyViewModel(
    private val repository: PantheonRepository,
) : ViewModel() {

    private val _apotheosis = MutableStateFlow<ApotheosisEvent?>(null)
    val apotheosis: StateFlow<ApotheosisEvent?> = _apotheosis

    private val _boonGranted = MutableStateFlow<String?>(null)
    val boonGranted: StateFlow<String?> = _boonGranted

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
        return repository.observeMajors(daemon.id).flatMapLatest { majors ->
            val openMajors = majors.filter { !it.completed }
            if (openMajors.isEmpty()) {
                flow {
                    emit(
                        DaemonDailyBlock(
                            daemon = daemon,
                            level = repository.levelOf(daemon.id),
                            greeting = voice.greeting(greetingSeed),
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
                    DaemonDailyBlock(
                        daemon = daemon,
                        level = repository.levelOf(daemon.id),
                        greeting = voice.greeting(greetingSeed),
                        openMinors = lists.flatMap { it },
                    )
                }
            }
        }
    }

    fun completeMinor(minorId: Long) {
        viewModelScope.launch {
            val event = repository.completeMinor(minorId)
            if (event != null) _apotheosis.value = event
        }
    }

    fun dismissApotheosis() { _apotheosis.value = null }

    fun spendWish(daemonId: Long) {
        viewModelScope.launch {
            val boon = repository.spendWish(daemonId)
            if (boon != null) _boonGranted.value = boon
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

    companion object {
        fun factory(repository: PantheonRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                DailyViewModel(repository) as T
        }
    }
}
